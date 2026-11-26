package net.prototux.aare.ui.car

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import net.prototux.aare.R
import net.prototux.aare.car.MirroredKeys

/**
 * MyPlay (plan G1): over a mirrored phone app, the knob's stop that hands the knob and the pad to
 * the app ([MirroredKeys.forward]): every key down it maps goes to [send] as
 * (KeyEvent.ACTION_DOWN, key code), and its up as ACTION_UP, only for a key whose down it sent
 * (G1-2: not the up of the turn that brought the ring here). It takes no touches.
 *
 * G1-1: while it holds the ring it shows a thin ring round the app, and the first time a line
 * saying the knob now works the app and how to leave (holding Back, or Back three times quickly).
 */
@Composable
internal fun MirroredKeyStop(turn: MirroredKeys.TurnMode, send: (action: Int, keyCode: Int) -> Unit, modifier: Modifier = Modifier, takeRing: Boolean = false) {
    // VA-3: an app that runs on the car screen gets the knob and the pad from the start, not after a first turn onto it.
    val ring = remember { androidx.compose.ui.focus.FocusRequester() }
    if (takeRing) LaunchedEffect(Unit) { runCatching { ring.requestFocus() } }
    val down = remember { HashSet<Int>() }
    var focused by remember { mutableStateOf(false) }
    var hintShown by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf(false) }
    LaunchedEffect(focused) {
        if (focused && !hintShown) {
            hintShown = true
            hint = true
            delay(HINT_MS)
            hint = false
        }
    }
    Box(
        modifier.fillMaxSize()
            .onFocusChanged { focused = it.isFocused; if (!it.isFocused) down.clear() }
            .onPreviewKeyEvent { e ->
                val code = MirroredKeys.forward(e.key.nativeKeyCode, e.isShiftPressed, turn) ?: return@onPreviewKeyEvent false
                when (e.type) {
                    KeyEventType.KeyDown -> { down += code; send(android.view.KeyEvent.ACTION_DOWN, code) }
                    KeyEventType.KeyUp -> if (down.remove(code)) send(android.view.KeyEvent.ACTION_UP, code)
                    else -> return@onPreviewKeyEvent false
                }
                true
            }
            .carFocusHighlight(MaterialTheme.shapes.small)
            .focusRequester(ring)
            .focusable(),
    ) {
        if (hint) {
            Text(
                stringResource(R.string.myplay_mirror_knob_hint), style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag(MIRROR_HINT_TAG),
            )
        }
    }
}

internal const val MIRROR_HINT_TAG = "mirror_knob_hint"
private const val HINT_MS = 4_000L
