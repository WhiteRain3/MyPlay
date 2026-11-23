package net.prototux.aare.host.templates

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
 * its pan mode for the time, when it offers one ([panOffered]); R5-7: a pan mode the driver had
 * already turned on ([panning]) is left on when move mode ends.
 */
@Composable
internal fun BoxScope.MapMoveStop(
    host: TemplateHost, panOffered: Boolean, panning: () -> Boolean, setPan: (Boolean) -> Unit,
    /** Look C: false when the push is the actions wheel's (move mode then starts only from [moveAsked]). */
    pushEnters: Boolean = true,
    /** Look C: each new value (above 0) starts move mode, as "Move map" in the actions wheel. */
    moveAsked: Int = 0,
    focus: FocusRequester? = null,
    /** Look C (design 17): ORBIT's look while moving: a glass hint pill at the top and a crosshair, no buttons. */
    orbitLook: Boolean = false,
) {
    var moving by remember { mutableStateOf(false) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    /** Whether entering move mode turned the app's pan mode on (so leaving turns it off again). */
    var panTurnedOn by remember { mutableStateOf(false) }
    fun enter() {
        moving = true
        panTurnedOn = panOffered && !panning()
        if (panTurnedOn) setPan(true)
    }
    fun leave() {
        moving = false
        if (panTurnedOn) setPan(false)
        panTurnedOn = false
    }
    BackHandler(enabled = moving) { leave() }
    LaunchedEffect(moveAsked) {
        if (moveAsked > 0) { focus?.let { runCatching { it.requestFocus() } }; enter() }
    }
    Box(
        Modifier.fillMaxSize().onSizeChanged { size = it }
            .onFocusChanged { if (!it.isFocused && moving) leave() }
            .onKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onKeyEvent moving && MapMoveKeys.action(e.key, e.isShiftPressed, true) != null
                if (!moving && !pushEnters) return@onKeyEvent false
                when (val a = MapMoveKeys.action(e.key, e.isShiftPressed, moving) ?: return@onKeyEvent false) {
                    MapMoveKeys.Action.Enter -> enter()
                    MapMoveKeys.Action.Leave -> leave()
                    is MapMoveKeys.Action.Pan -> safeCall("mapScroll") { host.mapScroll(a.dx * size.width, a.dy * size.height) }
                    is MapMoveKeys.Action.Zoom -> safeCall("mapScale") { host.mapScale(size.width / 2f, size.height / 2f, a.factor) }
                }
                true
            }
            .then(if (focus != null) Modifier.focusRequester(focus) else Modifier)
            .focusable().carFocusHighlight(MaterialTheme.shapes.large),
    )
    if (moving && orbitLook) {
        MapMoveOrbitLook()
    } else if (moving) {
        Text(
            stringResource(R.string.myplay_map_move_hint),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

/** Look C (design 17): the hint pill at the top and the crosshair with its four arrows in the middle. */
@Composable
private fun BoxScope.MapMoveOrbitLook() {
    val scheme = MaterialTheme.colorScheme
    val pill = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
    androidx.compose.foundation.layout.Row(
        Modifier.align(Alignment.TopCenter).padding(top = 40.dp).clip(pill).background(scheme.surface.copy(alpha = 0.92f))
            .border(2.dp, scheme.outlineVariant, pill).padding(horizontal = 22.dp, vertical = 12.dp)
            .testTag(MAP_MOVE_PILL_TAG),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp),
    ) {
        androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.RadioButtonChecked, null,
            Modifier.padding(end = 0.dp).size(22.dp), tint = scheme.onSurfaceVariant)
        Text(stringResource(R.string.myplay_map_move_pad), style = MaterialTheme.typography.titleMedium, color = scheme.onSurfaceVariant)
        Text(stringResource(R.string.myplay_map_move_back), style = MaterialTheme.typography.titleMedium, color = scheme.onSurface,
            modifier = Modifier.padding(start = 12.dp))
    }
    // The crosshair: a ring in the middle, an arrow each way.
    androidx.compose.foundation.Canvas(Modifier.align(Alignment.Center).size(170.dp).testTag(MAP_MOVE_CROSS_TAG)) {
        val c = center
        val white = androidx.compose.ui.graphics.Color.White
        val halo = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f)
        drawCircle(halo, 13.dp.toPx(), c, style = androidx.compose.ui.graphics.drawscope.Stroke(6.dp.toPx()))
        drawCircle(white, 11.dp.toPx(), c, style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
        val d = 70.dp.toPx()
        val a = 12.dp.toPx()
        for ((dx, dy) in listOf(0f to -1f, 0f to 1f, -1f to 0f, 1f to 0f)) {
            val tip = androidx.compose.ui.geometry.Offset(c.x + dx * (d + a), c.y + dy * (d + a))
            val base = androidx.compose.ui.geometry.Offset(c.x + dx * d, c.y + dy * d)
            val px = -dy * a
            val py = dx * a
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(tip.x, tip.y); lineTo(base.x + px, base.y + py); lineTo(base.x - px, base.y - py); close()
            }
            drawPath(path, halo, style = androidx.compose.ui.graphics.drawscope.Stroke(4.dp.toPx()))
            drawPath(path, white)
        }
    }
}

internal const val MAP_MOVE_PILL_TAG = "map_move_pill"
internal const val MAP_MOVE_CROSS_TAG = "map_move_cross"
