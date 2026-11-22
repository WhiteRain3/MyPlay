package net.prototux.aare.ui.car.myplay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import net.prototux.aare.R
import net.prototux.aare.ui.car.CarAppKind

/**
 * MyPlay (O3): opening an app from MyPlay's own screen. The app grows out of the stop it was
 * chosen from (a disc in the honeycomb, the turn, the music panel) to the full screen, with no
 * rail beside it; Back shrinks it back into that stop, and the engine is where it was (the Apps
 * world with the ring on that disc, or the home). Home (the Menu button, Back held) goes to the
 * plain home instead. Only for a skin without a rail ([MyPlaySkin.appRail] false: ORBIT).
 */
internal class EngineMemory {
    /** The skin the rest was kept for; another skin starts afresh. */
    var skinId: String? = null

    /** Where the engine is (home, the dial, a world); the engine's own, kept here. */
    private var model: MyPlayScreenModel? = null
    fun model(hand: List<World>?): MyPlayScreenModel = model ?: MyPlayScreenModel(hand).also { model = it }

    /** The engine's state that Back from an app comes back to (the engine reads and writes these). */
    val chosen = mutableStateOf(STOP_HUB)
    val appRing = mutableStateOf(0)
    val appsKinds = mutableStateOf<Set<CarAppKind>?>(null)
    val fromApps = mutableStateOf(false)

    /** Where the app was opened from (in the screen's coordinates); null: the middle of the screen. */
    var origin by mutableStateOf<FocusGraph.Box?>(null)

    /** An app had the screen: the home shrinks it back into [origin] once. */
    var returning by mutableStateOf(false)

    /** Forget everything but the skin (a new skin, or Home asked for). */
    fun reset(skin: String?) {
        skinId = skin
        model = null
        chosen.value = STOP_HUB
        appRing.value = 0
        appsKinds.value = null
        fromApps.value = false
        origin = null
        returning = false
    }
}

/** O3: how long an app takes to grow out of its stop, and to shrink back. */
internal const val APP_OPEN_MS = 260
internal const val APP_CLOSE_MS = 220

/** O3: how many times a drive shows the way home over an app, and for how long. */
internal const val HOME_HINT_TIMES = 3
internal const val HOME_HINT_MS = 4_000L

/** The scale at [p] (0 at the stop, 1 full screen) for a stop of [box] on a [w]x[h] screen. */
internal fun openScale(box: FocusGraph.Box?, w: Float, h: Float, p: Float): Float {
    if (w <= 0f || h <= 0f) return 1f
    val start = if (box == null) 0.6f else maxOf((box.right - box.left) / w, (box.bottom - box.top) / h).coerceIn(0.05f, 1f)
    return start + (1f - start) * p.coerceIn(0f, 1f)
}

/** Draws this at [p] of the way from [box] (null: the middle) to the full screen. */
internal fun Modifier.grownFrom(box: FocusGraph.Box?, p: () -> Float): Modifier = graphicsLayer {
    val v = p()
    if (v >= 1f) return@graphicsLayer
    val w = size.width
    val h = size.height
    val s = openScale(box, w, h, v)
    scaleX = s
    scaleY = s
    transformOrigin = if (box == null || w <= 0f || h <= 0f) TransformOrigin.Center
    else TransformOrigin((box.cx / w).coerceIn(0f, 1f), (box.cy / h).coerceIn(0f, 1f))
    alpha = (v * 2.5f).coerceIn(0f, 1f)
    shape = RoundedCornerShape((48 * (1f - v)).dp)
    clip = true
}

/**
 * O3: the app shrinking back into the stop it came from, drawn once over the home when the engine
 * comes back from an app: a pane of the screen's background, to the stop, fading.
 */
@Composable
internal fun ShrinkBack(memory: EngineMemory, modifier: Modifier) {
    if (!memory.returning) return
    val back = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        back.animateTo(0f, tween(APP_CLOSE_MS, easing = FastOutSlowInEasing))
        memory.returning = false
        memory.origin = null
    }
    Box(
        modifier.fillMaxSize().testTag(SHRINK_TAG)
            .grownFrom(memory.origin) { back.value }
            .graphicsLayer { alpha = back.value }
            .background(LocalSkinTokens.current.surface),
    )
}

/** O3: "Hold Back to come home" over an app, for a few seconds, the first few times in a drive. */
@Composable
internal fun HomeHint(show: Boolean, modifier: Modifier) {
    var on by remember { mutableStateOf(show) }
    LaunchedEffect(Unit) { if (on) { delay(HOME_HINT_MS); on = false } }
    if (!on) return
    val t = LocalSkinTokens.current
    Text(
        stringResource(R.string.myplay_frame_home_hint), color = t.onSurface, fontSize = 22.sp,
        modifier = modifier.clip(RoundedCornerShape(24.dp)).background(t.surface.copy(alpha = 0.92f))
            .padding(horizontal = 22.dp, vertical = 10.dp).testTag(HOME_HINT_TAG),
    )
}

internal const val SHRINK_TAG = "app_shrink"
internal const val HOME_HINT_TAG = "app_home_hint"

/**
 * O3-3: the window an opening app shows through at [p] (0 the stop, 1 the whole [w]x[h] screen),
 * from [box] (null: the middle).
 */
internal fun revealRect(box: FocusGraph.Box?, w: Float, h: Float, p: Float): FocusGraph.Box {
    val b = box ?: FocusGraph.Box(w * 0.2f, h * 0.2f, w * 0.8f, h * 0.8f)
    val v = p.coerceIn(0f, 1f)
    fun lerp(a: Float, z: Float) = a + (z - a) * v
    return FocusGraph.Box(lerp(b.left, 0f), lerp(b.top, 0f), lerp(b.right, w), lerp(b.bottom, h))
}

/**
 * O3-3 (the Reviewer: a maps app's SurfaceView does not follow a scaled layer): the app is drawn
 * full size at once, and this cover, in the screen's background, hides all but a window that grows
 * from the stop to the whole screen. Drawn over the app, so it works over any surface.
 */
@Composable
internal fun RevealCover(box: FocusGraph.Box?, p: () -> Float, color: androidx.compose.ui.graphics.Color, modifier: Modifier) =
    androidx.compose.foundation.Canvas(modifier.fillMaxSize().testTag(REVEAL_TAG)) {
        val v = p()
        if (v >= 1f) return@Canvas
        val r = revealRect(box, size.width, size.height, v)
        val corner = (48 * (1f - v)).dp.toPx()
        val path = androidx.compose.ui.graphics.Path().apply {
            fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
            addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
            addRoundRect(androidx.compose.ui.geometry.RoundRect(r.left, r.top, r.right, r.bottom,
                androidx.compose.ui.geometry.CornerRadius(corner, corner)))
        }
        drawPath(path, color)
    }

internal const val REVEAL_TAG = "app_reveal"
