package net.prototux.aare.ui

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.audio.MicRouter
import net.prototux.aare.car.ButtonTable
import net.prototux.aare.car.CarInputs
import net.prototux.aare.host.CarRuntime
import net.prototux.aare.media.MediaHub
import net.prototux.aare.ui.car.CarScreen
import net.prototux.aare.ui.phone.next.NextColors
import net.prototux.aare.ui.phone.next.NextPreviewControls
import net.prototux.aare.ui.phone.next.NextType
import net.prototux.aare.ui.phone.next.PreviewInputs

/**
 * The car screen on the phone itself, without a car: to try apps, check the layout, or test the car
 * UI (dispatcher, 2026-11-23). Full screen, landscape. The control bar sends the car's own inputs
 * through the same path as a car (CarInputs: learned buttons, remaps, the button table, the gesture
 * engine, mirrored keys); a tap or swipe on the screen is a finger on it; a pretend speed feeds the
 * speed visuals. What only a car can give (the limit, temperature, night mode, car data…) is listed
 * by the bar's (i). Hosted apps get the phone's microphone.
 */
class CarPreviewActivity : ComponentActivity() {
    private lateinit var runtime: CarRuntime
    private lateinit var inputs: CarInputs
    private val mic = MicRouter({ null }, {})

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Full screen: the bars come back with a swipe from the edge.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        val settings = Settings(this)
        runtime = CarRuntime(this, lifecycleScope, settings, MediaHub(this, lifecycleScope), primary = false)
        val dm = resources.displayMetrics
        runtime.densityDpi = dm.densityDpi
        runtime.screenWidthDp = (dm.widthPixels / dm.density).toInt()
        runtime.screenHeightDp = (dm.heightPixels / dm.density).toInt()
        runtime.microphone = { sink, stopped -> mic.open(sink, stopped) }
        runtime.start()
        inputs = CarInputs(runtime, settings, CarInputs.ActivityHost(this))
        inputs.start()
        val bridge = object : PreviewInputs {
            override fun key(code: Int, down: Boolean) = inputs.key(code, down, longPress = false)
            override fun rotary(steps: Int) = inputs.rotary(steps)
            override fun pretendSpeed(kmh: Float?) = runtime.pretendSpeed(kmh)
        }
        setContent {
            var controls by rememberSaveable { mutableStateOf(true) }
            Column(Modifier.fillMaxSize().background(NextColors.Bg)) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    runtime.Root(Modifier.fillMaxSize())
                    // Testing aid: the bar shows or hides; the car screen keeps the rest of the phone.
                    Text(
                        stringResource(if (controls) R.string.next_prev_hide_label else R.string.next_prev_show_label),
                        style = NextType.Tag, color = NextColors.On,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(12.dp))
                            .background(NextColors.Caution).clickable { controls = !controls }
                            .padding(horizontal = 10.dp, vertical = 6.dp).testTag("preview-toggle"),
                    )
                }
                if (controls) {
                    val codes = inputs.controlCodes()
                    NextPreviewControls(
                        bridge, learnedCodes = inputs.learnedCodes().keys.sorted(),
                        codeFor = { default ->
                            when (default) {
                                KeyEvent.KEYCODE_DPAD_CENTER -> codes[ButtonTable.Control.KNOB_PUSH]
                                KeyEvent.KEYCODE_BACK -> codes[ButtonTable.Control.BACK]
                                KeyEvent.KEYCODE_MENU -> codes[ButtonTable.Control.MENU]
                                else -> null
                            } ?: default
                        },
                    )
                }
            }
        }
        // Through the dispatcher: with predictive back (targetSdk 36+ on Android 16) onBackPressed
        // is no longer called. Back walks the car screen back to the dashboard, then leaves.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (runtime.ui.screen == CarScreen.Dashboard) finish() else runtime.back()
            }
        })
    }

    override fun onDestroy() {
        inputs.stop()
        inputs.clear()
        mic.shutdown()
        runtime.pretendSpeed(null)
        runtime.stop()
        super.onDestroy()
    }
}
