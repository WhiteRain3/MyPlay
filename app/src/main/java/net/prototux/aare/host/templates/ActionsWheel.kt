package net.prototux.aare.host.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.compose.ui.res.stringResource
import net.prototux.aare.R

/**
 * Look C, steps 1 and 2 (the owner's chosen Waze design, claude/waze-design): on the maps app's
 * full-screen map under ORBIT, Waze's action strip and map buttons are not drawn; they are one
 * press away in a wheel. The rule (the dispatcher, 2026-11-23; the box never sends holds):
 * - the pad's centre (the knob's push) opens the wheel;
 * - Back opens the wheel; Back with the wheel open closes it;
 * - a second Back within [SECOND_BACK_MS] of closing it goes to MyPlay's home (K3: Back never traps);
 * - "MyPlay home" is also the wheel's last row.
 * A push runs the ringed action and closes the wheel; "Move map" (Action.PAN) starts moving the map.
 *
 * Provided by MyPlay's app frame for a maps app under ORBIT; null everywhere else (the strips as before).
 */
internal class ActionsWheelHost(
    val home: () -> Unit,
    val homeTitle: String,
    val moveMapTitle: String,
    val now: () -> Long = { android.os.SystemClock.uptimeMillis() },
    /** True while something over the app holds the knob (a ringing call, a notice with buttons): Back and the push are its. */
    val held: () -> Boolean = { false },
) {
    companion object {
        const val SECOND_BACK_MS = 2_000L
    }
}

internal val LocalActionsWheel = staticCompositionLocalOf<ActionsWheelHost?> { null }

/** One row of the wheel. [icon] (the app's) or [vector] (ours), or neither. */
internal class WheelEntry(val key: String, val title: String, val icon: CarIcon?, val vector: ImageVector?, val run: () -> Unit)

internal const val ACTIONS_WHEEL_TAG = "actions_wheel"
internal fun wheelRowTag(key: String) = "wheel_$key"
internal const val WHEEL_HOME = "home"

/**
 * The wheel's rows: the action strip's actions, then the map buttons, then MyPlay home. The app
 * icon and Back are left out (Back is the wheel's own), and so is an action that is disabled or
 * shows nothing. Pure, apart from the clicks it wires.
 */
internal fun wheelEntries(
    strip: ActionStrip?, mapStrip: ActionStrip?, wheel: ActionsWheelHost,
    click: (Action) -> Unit, movePan: () -> Unit,
): List<WheelEntry> = buildList {
    var i = 0
    for (a in strip?.actions.orEmpty() + mapStrip?.actions.orEmpty()) {
        val key = "a${i++}"
        if (!a.isEnabled) continue
        when (a.type) {
            Action.TYPE_APP_ICON, Action.TYPE_BACK -> Unit
            Action.TYPE_PAN -> add(WheelEntry(key, wheel.moveMapTitle, null, Icons.Rounded.OpenWith, movePan))
            else -> {
                val title = a.title.plain()
                if (title.isEmpty() && a.icon == null) continue
                if (a.onClickDelegate == null) continue
                add(WheelEntry(key, title, a.icon, null) { click(a) })
            }
        }
    }
    add(WheelEntry(WHEEL_HOME, wheel.homeTitle, null, Icons.Rounded.Home, wheel.home))
}

/**
 * The wheel over the map: the ringed row in the middle and biggest, one row either side, faded;
 * the knob (Tab), the pad's up and down move it; a push runs the row. Takes the focus while open.
 */
@Composable
internal fun BoxScope.ActionsWheelView(entries: List<WheelEntry>, ringed: Int, move: (Int) -> Unit, run: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier.align(Alignment.Center).width(560.dp)
            .background(scheme.surface.copy(alpha = 0.94f), RoundedCornerShape(40.dp))
            .padding(vertical = 20.dp, horizontal = 24.dp)
            .focusRequester(focus).focusable()
            .onKeyEvent { e ->
                val ours = e.key in WHEEL_KEYS
                if (e.type == KeyEventType.KeyDown) when (e.key) {
                    Key.Tab -> move(if (e.isShiftPressed) -1 else 1)
                    Key.DirectionUp, Key.DirectionLeft -> move(-1)
                    Key.DirectionDown, Key.DirectionRight -> move(1)
                    Key.Enter, Key.DirectionCenter, Key.NumPadEnter -> run()
                    else -> Unit
                }
                ours
            }
            .testTag(ACTIONS_WHEEL_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(if (ringed > 0) stringResource(R.string.myplay_wheel_more, ringed) else "", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, modifier = Modifier.height(20.dp))
        for (d in -1..1) {
            val e = entries.getOrNull(ringed + d)
            if (e == null) { Spacer(Modifier.height(if (d == 0) 84.dp else 60.dp)); continue }
            val on = d == 0
            Row(
                Modifier.padding(vertical = 6.dp).alpha(if (on) 1f else 0.55f)
                    .then(if (on) Modifier.border(5.dp, scheme.primary, RoundedCornerShape(36.dp)) else Modifier)
                    .padding(horizontal = 24.dp, vertical = if (on) 14.dp else 8.dp)
                    .semantics { selected = on }.testTag(wheelRowTag(e.key)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val iconSize = if (on) 44.dp else 32.dp
                Box(Modifier.size(iconSize + 12.dp).background(scheme.surfaceContainerHigh, CircleShape), contentAlignment = Alignment.Center) {
                    when {
                        e.vector != null -> Icon(e.vector, null, Modifier.size(iconSize), tint = scheme.primary)
                        e.icon != null -> CarIconView(e.icon, iconSize, monochromeTint = scheme.primary)
                    }
                }
                if (e.title.isNotEmpty()) {
                    Spacer(Modifier.width(18.dp))
                    Text(e.title, fontSize = if (on) 32.sp else 27.sp, fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        val after = entries.size - ringed - 1
        Text(if (after > 0) stringResource(R.string.myplay_wheel_more, after) else "", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, modifier = Modifier.height(20.dp))
    }
}

private val WHEEL_KEYS = setOf(Key.Tab, Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight, Key.Enter, Key.DirectionCenter, Key.NumPadEnter)
