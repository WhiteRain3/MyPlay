package net.prototux.aare.ui.phone

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import net.prototux.aare.car.myplayHomeAddress
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.car.myplayGestureLine
import net.prototux.aare.ui.car.myplay.HomePad
import net.prototux.aare.ui.car.myplay.myplayHomePad
import net.prototux.aare.ui.car.myplay.myplayInfoHome
import net.prototux.aare.car.ButtonAction
import net.prototux.aare.car.ButtonTable
import net.prototux.aare.car.ButtonTable.Control
import net.prototux.aare.car.ButtonTable.Gesture
import net.prototux.aare.car.DOUBLE_RANGE
import net.prototux.aare.car.GestureEngine
import net.prototux.aare.car.HOLD_RANGE
import net.prototux.aare.car.myplayButtons
import net.prototux.aare.car.myplayGestureDoubleMs
import net.prototux.aare.car.myplayGestureHoldMs

/** The gestures the page offers for [control]. Back has no double press: two quick Backs already leave an app that ignores Back. */
internal fun gesturesFor(control: Control): List<Gesture> = when (control) {
    Control.KNOB_PUSH -> listOf(Gesture.PRESS, Gesture.HOLD, Gesture.DOUBLE, Gesture.TRIPLE, Gesture.HOLD_TURN)
    Control.WHEEL_CLICK -> listOf(Gesture.PRESS, Gesture.HOLD, Gesture.DOUBLE, Gesture.TRIPLE)
    Control.MENU -> listOf(Gesture.PRESS, Gesture.HOLD, Gesture.DOUBLE)
    Control.BACK -> listOf(Gesture.PRESS, Gesture.HOLD)
}

/** The actions offered for [gesture]: skipping songs only goes with hold and turn, which can do nothing else. */
internal fun actionsFor(gesture: Gesture): List<ButtonAction> =
    if (gesture == Gesture.HOLD_TURN) listOf(ButtonAction.SKIP_SONGS, ButtonAction.NOTHING)
    else ButtonAction.entries - ButtonAction.SKIP_SONGS

/** The default action of [control] and [gesture] (nothing when the table has none). */
internal fun defaultAction(control: Control, gesture: Gesture): ButtonAction = ButtonTable.DEFAULTS[control to gesture] ?: ButtonAction.NOTHING

/**
 * The stored overrides after setting [control] and [gesture] to [action]: an action equal to the
 * default is stored as no override, so the setting holds only real changes.
 */
internal fun withAction(stored: String, control: Control, gesture: Gesture, action: ButtonAction): String {
    val overrides = ButtonTable.decodeOverrides(stored).toMutableMap()
    if (action == defaultAction(control, gesture)) overrides.remove(control to gesture) else overrides[control to gesture] = action
    return ButtonTable.encodeOverrides(overrides)
}

/**
 * Whether the buttons in [stored] still leave a way out: Back's press or hold goes back or home
 * (so an app that ignores the screen can still be left), and some gesture somewhere goes home.
 */
internal fun leavesAWayOut(stored: String): Boolean {
    val t = ButtonTable(ButtonTable.decodeOverrides(stored))
    val backOut = listOf(Gesture.PRESS, Gesture.HOLD).any { t.action(Control.BACK, it) in setOf(ButtonAction.BACK, ButtonAction.HOME) }
    val home = Control.entries.any { c -> Gesture.entries.any { g -> t.action(c, g) in setOf(ButtonAction.HOME, ButtonAction.HOME_OR_APPS) } }
    return backOut && home
}

/** The actions offered for [control] and [gesture] given the other choices in [stored]: none that would leave no way out. */
internal fun allowedActions(stored: String, control: Control, gesture: Gesture): List<ButtonAction> =
    actionsFor(gesture).filter { it == ButtonTable(ButtonTable.decodeOverrides(stored)).action(control, gesture) || leavesAWayOut(withAction(stored, control, gesture, it)) }

/** How many gestures differ from the defaults, for the Buttons tile. */
fun changedCount(stored: String): Int =
    ButtonTable.decodeOverrides(stored).count { (k, v) -> v != defaultAction(k.first, k.second) }

internal fun actionName(a: ButtonAction): Int = when (a) {
    ButtonAction.SELECT -> R.string.phone_action_select
    ButtonAction.BACK -> R.string.phone_action_back
    ButtonAction.HOME -> R.string.phone_action_home
    ButtonAction.HOME_OR_APPS -> R.string.phone_action_home_or_apps
    ButtonAction.ASSISTANT -> R.string.phone_action_assistant
    ButtonAction.PLAY_PAUSE -> R.string.phone_action_play_pause
    ButtonAction.NEXT -> R.string.phone_action_next
    ButtonAction.PREVIOUS -> R.string.phone_action_previous
    ButtonAction.SKIP_SONGS -> R.string.phone_action_skip_songs
    ButtonAction.OPEN_NAVIGATION -> R.string.phone_action_open_navigation
    ButtonAction.OPEN_MEDIA -> R.string.phone_action_open_media
    ButtonAction.OPEN_PHONE -> R.string.phone_action_open_phone
    ButtonAction.NAVIGATE_HOME -> R.string.phone_action_navigate_home
    ButtonAction.CALL_BACK -> R.string.phone_action_call_back
    ButtonAction.CAR_SCREENSHOT -> R.string.myplay_screenshot_action
    ButtonAction.NOTHING -> R.string.phone_action_nothing
}

internal fun gestureName(g: Gesture): Int = when (g) {
    Gesture.PRESS -> R.string.phone_gesture_press
    Gesture.HOLD -> R.string.phone_gesture_hold
    Gesture.DOUBLE -> R.string.phone_gesture_double
    Gesture.HOLD_TURN -> R.string.phone_gesture_hold_turn
    Gesture.TRIPLE -> R.string.phone_gesture_triple
}

internal fun controlName(c: Control): Int = when (c) {
    Control.KNOB_PUSH -> R.string.phone_control_knob
    Control.WHEEL_CLICK -> R.string.phone_control_wheel
    Control.MENU -> R.string.phone_control_menu
    Control.BACK -> R.string.phone_control_back
}

