package net.prototux.aare.ui.car

import android.graphics.SurfaceTexture
import android.view.KeyEvent
import net.prototux.aare.car.myplayMirrorTurns
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import net.prototux.aare.R
import net.prototux.aare.host.CarRuntime
import net.prototux.aare.launch.AppLauncher
import net.prototux.aare.media.Placeholder

/**
 * Hosts an ordinary phone app on the car screen (NewPipe, an emulator, a browser app…). A
 * [TextureView] gives its surface to the privileged [AppLauncher], which puts a virtual display
 * there, launches [packageName] on it, and receives the car's touches we forward.
 *
 * Not an Android Auto app, so — like the phone-screen mirror — it is paused while driving. The
 * privileged core is device-tested; here it just wires the surface, touches and lifecycle.
 */
@Composable
internal fun StandaloneAppScreen(runtime: CarRuntime, driving: Boolean, packageName: String, modifier: Modifier) {
    val context = LocalContext.current
    // VA-8: the app kept from before, if it is this one (it carries on), else a new one.
    val hosted = remember(packageName) { runtime.hostedApps.take(packageName) }
    val launcher = hosted.launcher
    var started by remember(packageName) { mutableStateOf(hosted.started) }
    // VA-8: picture hidden, sound off: driving off pauses the app's playing video.
    LaunchedEffect(driving) {
        if (driving) runtime.media.playback.value?.takeIf { it.packageName == packageName && it.playing }?.let { runtime.media.pause() }
    }

    DisposableEffect(packageName) {
        // Hardware / head-unit Back goes to the launched app.
        val quickBacks = net.prototux.aare.car.QuickBacks()
        val back: () -> Boolean = back@{
            val t = android.os.SystemClock.uptimeMillis()
            // MyPlay (G1-3): Back three times quickly leaves the app, whatever the buttons do.
            if (quickBacks.press(t)) { runtime.actions.goHome(); return@back true }
            launcher.key(t, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)
            launcher.key(t, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK)
            launcher.connected
        }
        runtime.screenBack = back
        onDispose {
            if (runtime.screenBack === back) runtime.screenBack = null
            runtime.hostedApps.leave(hosted)
        }
    }

    // VA-5: a video link for this app ("Play on car", "Open on car") opens in it once it runs (parked).
    val link = runtime.ui.videoLink
    LaunchedEffect(link, started) {
        if (link == null || link.packageName != packageName) return@LaunchedEffect
        if (link.link == null) { runtime.ui.videoLink = null; return@LaunchedEffect }
        if (!started) return@LaunchedEffect
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { launcher.open(packageName, link.link) }
        if (runtime.ui.videoLink === link) runtime.ui.videoLink = null
    }

    Box(modifier.background(Color.Black)) {
        if (!driving) {
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    TextureView(ctx).apply {
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                                val surface = Surface(st)
                                // connect() blocks (binds Shizuku / spawns su), so do it off the main thread.
                                Thread {
                                    if (hosted.started) {
                                        // VA-8: the kept display gets the new surface; the app carries on.
                                        launcher.resize(surface, w, h, runtime.densityDpi)
                                        started = true
                                    } else if (launcher.connect()) {
                                        val id = launcher.start(
                                            surface, w, h, runtime.densityDpi, packageName,
                                            runtime.settings.standaloneKeepScreenAwake,
                                        )
                                        hosted.displayId = id
                                        started = id >= 0
                                    }
                                }.apply { isDaemon = true }.start()
                            }
                            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
                                launcher.resize(Surface(st), w, h, runtime.densityDpi)
                            }
                            // VA-8: hidden while driving: the display keeps the app, with no surface.
                            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean { launcher.detach(); return true }
                            override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            // Forward the car's touches to the launched app's display (single pointer for now).
            Box(Modifier.fillMaxSize().pointerInput(packageName) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val t0 = down.uptimeMillis
                    launcher.touch(t0, t0, MotionEvent.ACTION_DOWN, down.position.x, down.position.y, 0)
                    down.consume()
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (change.pressed) {
                            launcher.touch(t0, change.uptimeMillis, MotionEvent.ACTION_MOVE, change.position.x, change.position.y, 0)
                        } else {
                            launcher.touch(t0, change.uptimeMillis, MotionEvent.ACTION_UP, change.position.x, change.position.y, 0)
                            change.consume()
                            break
                        }
                        change.consume()
                    }
                }
            })
            // MyPlay (G1): the knob and the pad go into the app as keys.
            MirroredKeyStop(
                net.prototux.aare.car.MirroredKeys.turnFor(runtime.settings.myplayMirrorTurns, packageName),
                send = { action, code -> launcher.key(android.os.SystemClock.uptimeMillis(), action, code) },
                takeRing = started,
            )
            // A quiet reminder that this isn't an Android Auto app; VA-7: only for a few seconds, as it covers the video's top.
            var banner by remember(packageName) { mutableStateOf(true) }
            LaunchedEffect(packageName, started) { if (started) { kotlinx.coroutines.delay(BANNER_MS); banner = false } }
            if (banner) Text(
                stringResource(R.string.standalone_not_native),
                style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.35f)).padding(horizontal = 12.dp, vertical = 3.dp),
            )
            if (!started) Placeholder(stringResource(R.string.standalone_starting), loading = true)
        } else {
            Placeholder(stringResource(R.string.standalone_paused_driving), loading = false, icon = Icons.Rounded.PauseCircle)
        }
    }
}

/** VA-7: how long the "not an Android Auto app" line stays over a hosted app once it runs. */
internal const val BANNER_MS = 3_000L
