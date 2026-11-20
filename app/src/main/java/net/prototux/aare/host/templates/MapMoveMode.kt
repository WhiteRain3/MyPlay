package net.prototux.aare.host.templates

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import net.prototux.aare.R
import net.prototux.aare.ui.car.carFocusHighlight

/**
 * MyPlay (plan R5): moving a hosted maps app's map with the knob, after JVKeller's idea (fork
 * commit 570a67de), written anew.
 *
 * The map itself is a stop for the knob, after the app's own buttons (so the ring still starts on
 * the app's first button). Pushed, it enters "move the map": the pad moves the map a fifth of its
 * size per press, turning zooms in (forward) or out (back) around its middle, and a push or Back
 * leaves. A line at the bottom says so while it lasts. Touch is not affected: this draws only the
 * ring and the line, and reads no touches.
 */
internal object MapMoveKeys {
    sealed interface Action {
        data object Enter : Action
        data object Leave : Action
        /** Move the map so its content goes by (dx, dy) fractions of the map's size. */
        data class Pan(val dx: Float, val dy: Float) : Action
        data class Zoom(val factor: Float) : Action
    }

    const val PAN_FRACTION = 0.2f
    const val ZOOM_IN = 1.25f
    const val ZOOM_OUT = 0.8f

    /**
     * What a key down does on the map stop: null passes it on (outside move mode, everything but
     * the push; in move mode, keys it has no use for, such as Back, which [MapMoveStop] handles).
     * The knob's turns arrive as Tab and Shift+Tab.
     */
    fun action(key: Key, shift: Boolean, moving: Boolean): Action? = when {
        !moving -> if (key == Key.Enter || key == Key.DirectionCenter || key == Key.NumPadEnter) Action.Enter else null
        key == Key.Enter || key == Key.DirectionCenter || key == Key.NumPadEnter -> Action.Leave
        // Scroll distances as the app gets them (SurfaceCallback.onScroll): positive moves the view right/down.
        key == Key.DirectionLeft -> Action.Pan(-PAN_FRACTION, 0f)
        key == Key.DirectionRight -> Action.Pan(PAN_FRACTION, 0f)
        key == Key.DirectionUp -> Action.Pan(0f, -PAN_FRACTION)
        key == Key.DirectionDown -> Action.Pan(0f, PAN_FRACTION)
        key == Key.Tab -> Action.Zoom(if (shift) ZOOM_OUT else ZOOM_IN)
        else -> null
    }
}

/**
 * The map's knob stop and its move mode, over the map in [MapScaffold]. [setPan] puts the app in
 * its pan mode for the time, when it offers one ([panOffered]).
 */
@Composable
internal fun BoxScope.MapMoveStop(host: TemplateHost, panOffered: Boolean, setPan: (Boolean) -> Unit) {
    var moving by remember { mutableStateOf(false) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    fun leave() {
        moving = false
        if (panOffered) setPan(false)
    }
    BackHandler(enabled = moving) { leave() }
    Box(
        Modifier.fillMaxSize().onSizeChanged { size = it }
            .onFocusChanged { if (!it.isFocused && moving) leave() }
            .onKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onKeyEvent moving && MapMoveKeys.action(e.key, e.isShiftPressed, true) != null
                when (val a = MapMoveKeys.action(e.key, e.isShiftPressed, moving) ?: return@onKeyEvent false) {
                    MapMoveKeys.Action.Enter -> { moving = true; if (panOffered) setPan(true) }
                    MapMoveKeys.Action.Leave -> leave()
                    is MapMoveKeys.Action.Pan -> safeCall("mapScroll") { host.mapScroll(a.dx * size.width, a.dy * size.height) }
                    is MapMoveKeys.Action.Zoom -> safeCall("mapScale") { host.mapScale(size.width / 2f, size.height / 2f, a.factor) }
                }
                true
            }
            .focusable().carFocusHighlight(MaterialTheme.shapes.large),
    )
    if (moving) {
        Text(
            stringResource(R.string.myplay_map_move_hint),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
