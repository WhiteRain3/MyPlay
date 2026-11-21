package net.prototux.aare.ui.car

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import net.prototux.aare.car.MirroredKeys

/**
 * MyPlay (plan G1): over a mirrored phone app, the knob's stop that hands the knob and the pad to
 * the app ([MirroredKeys.forward]): every key down and up it maps goes to [send] as
 * (KeyEvent.ACTION_DOWN or _UP, key code). It draws nothing and takes no touches. While it holds
 * the ring, turns go to the app, not to the bar; holding Back goes home as everywhere.
 */
@Composable
internal fun MirroredKeyStop(turn: MirroredKeys.TurnMode, send: (action: Int, keyCode: Int) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxSize()
            .onPreviewKeyEvent { e ->
                val code = MirroredKeys.forward(e.key.nativeKeyCode, e.isShiftPressed, turn) ?: return@onPreviewKeyEvent false
                when (e.type) {
                    KeyEventType.KeyDown -> send(android.view.KeyEvent.ACTION_DOWN, code)
                    KeyEventType.KeyUp -> send(android.view.KeyEvent.ACTION_UP, code)
                    else -> return@onPreviewKeyEvent false
                }
                true
            }
            .focusable(),
    )
}
