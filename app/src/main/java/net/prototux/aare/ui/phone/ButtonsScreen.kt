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

/** What the drawing's parts are: the four controls in the table, and the pad, which always moves the ring. */
private sealed interface Part {
    data class Of(val control: Control) : Part
    data object Pad : Part
}

/**
 * The car's buttons (plan B1): a drawing of the steering wheel's scroll wheel and the dash knob
 * with its pad, Menu and Back; tap a part to see what each gesture on it does and change it.
 * Reads and writes only the car side's settings (myplay_buttons, myplay_gesture_*); the car
 * applies a change at once. Which keys the box really sends is not known until the owner's
 * connection log (R8): the drawing is the controls, not the codes.
 */
@Composable
fun ButtonsScreen() {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    var stored by remember { mutableStateOf(settings.myplayButtons) }
    var holdMs by remember { mutableFloatStateOf(settings.myplayGestureHoldMs.toFloat()) }
    var doubleMs by remember { mutableFloatStateOf(settings.myplayGestureDoubleMs.toFloat()) }
    var line by remember { mutableStateOf(settings.myplayGestureLine) }
    var homePad by remember { mutableStateOf(settings.myplayHomePad) }
    var infoHome by remember { mutableStateOf(settings.myplayInfoHome) }
    var part by rememberSaveable { mutableStateOf("knob_push") }
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    val selected: Part = Control.byId(part)?.let { Part.Of(it) } ?: Part.Pad
    val table = ButtonTable(ButtonTable.decodeOverrides(stored))

    Column {
        ControlsDrawing(selected) { p -> part = (p as? Part.Of)?.control?.id ?: "pad"; open = null }
        HintLine(Icons.Rounded.TouchApp, stringResource(R.string.phone_buttons_hint))
        // #1: the Car check saw this car send every hold as an instant press.
        if (settings.myplayCarSendsHolds == false) HintLine(Icons.Rounded.Info, stringResource(R.string.phone_buttons_no_holds))

        when (selected) {
            Part.Pad -> {
                SectionHeading(stringResource(R.string.phone_control_pad))
                PartCard(padding = 16.dp) { Text(stringResource(R.string.phone_buttons_pad_text), color = PhoneColors.Text, fontSize = 14.sp) }
            }
            is Part.Of -> {
                val control = selected.control
                SectionHeading(stringResource(controlName(control)))
                RowGroup(rows = gesturesFor(control).map { g ->
                    {
                        val action = table.action(control, g)
                        val isDefault = action == defaultAction(control, g)
                        Column {
                            SimpleRow(
                                Icons.Rounded.TouchApp, stringResource(gestureName(g)), if (isDefault) TileTone.BLUE else TileTone.GREEN,
                                stringResource(if (isDefault) R.string.phone_buttons_default else R.string.phone_buttons_changed),
                                onClick = { open = if (open == g.id) null else g.id },
                            ) { RowValue(stringResource(actionName(action))) }
                            if (open == g.id) Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                                Column {
                                    PhoneChoices(
                                        stringResource(R.string.phone_buttons_choose),
                                        allowedActions(stored, control, g).map { it.id to stringResource(actionName(it)) },
                                        action.id,
                                    ) { id ->
                                        ButtonAction.byId(id)?.let { a ->
                                            stored = withAction(stored, control, g, a); settings.myplayButtons = stored
                                        }
                                        open = null
                                    }
                                }
                            }
                        }
                    }
                })
                // Any control with a double press set makes its single press wait (B1-3).
                if (Gesture.DOUBLE in gesturesFor(control)) HintLine(Icons.Rounded.Info, stringResource(R.string.phone_buttons_double_wait))
                if (control == Control.KNOB_PUSH) HintLine(Icons.Rounded.Info, stringResource(R.string.phone_buttons_hold_turn_unknown))
                if (control == Control.BACK || control == Control.MENU) HintLine(Icons.Rounded.Info, stringResource(R.string.phone_buttons_way_out))
                if (control == Control.BACK) HintLine(Icons.Rounded.Info, stringResource(R.string.phone_buttons_back_double))
            }
        }

        SectionHeading(stringResource(R.string.phone_buttons_timing))
        PartCard(padding = 16.dp) {
            Column {
                Text(stringResource(R.string.phone_buttons_hold_time, holdMs.toInt()), color = PhoneColors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Slider(
                    holdMs, { holdMs = (Math.round(it / 50f) * 50).toFloat() },
                    valueRange = HOLD_RANGE.first.toFloat()..HOLD_RANGE.last.toFloat(),
                    onValueChangeFinished = { settings.myplayGestureHoldMs = holdMs.toLong() },
                    colors = sliderColours(),
                )
                Text(stringResource(R.string.phone_buttons_double_time, doubleMs.toInt()), color = PhoneColors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Slider(
                    doubleMs, { doubleMs = (Math.round(it / 25f) * 25).toFloat() },
                    valueRange = DOUBLE_RANGE.first.toFloat()..DOUBLE_RANGE.last.toFloat(),
                    onValueChangeFinished = { settings.myplayGestureDoubleMs = doubleMs.toLong() },
                    colors = sliderColours(),
                )
            }
        }
        HintLine(Icons.Rounded.Info, stringResource(R.string.phone_buttons_codes_unknown))
        Spacer(Modifier.height(8.dp))
        HomePadSection(homePad, { homePad = it; settings.myplayHomePad = it }, infoHome) { infoHome = it; settings.myplayInfoHome = it }
        TeachButtonSection(settings)
        HomeAddressField(settings)
        PlacesSection(settings) // #5: the saved places, beside Home address
        Spacer(Modifier.height(8.dp))
        RowGroup(rows = listOf {
            SwitchRow(Icons.Rounded.Info, stringResource(R.string.phone_buttons_line_title), line, TileTone.BLUE,
                stringResource(R.string.phone_buttons_line_hint)) { line = it; settings.myplayGestureLine = it }
        })
        Spacer(Modifier.height(8.dp))
        WideButton(stringResource(R.string.phone_buttons_reset), Icons.Rounded.Refresh) {
            stored = ""; settings.myplayButtons = ""
            holdMs = GestureEngine.HOLD_MS.toFloat(); settings.myplayGestureHoldMs = GestureEngine.HOLD_MS
            doubleMs = GestureEngine.DOUBLE_MS.toFloat(); settings.myplayGestureDoubleMs = GestureEngine.DOUBLE_MS
            homePad = HomePad.DEFAULT; settings.myplayHomePad = HomePad.DEFAULT
            infoHome = false; settings.myplayInfoHome = false
            open = null
        }
    }
}

@Composable
private fun sliderColours() = SliderDefaults.colors(thumbColor = PhoneColors.Accent, activeTrackColor = PhoneColors.Accent, inactiveTrackColor = PhoneColors.SwitchOff)

/** The two controls drawn side by side; each tappable part outlined green when chosen. */
@Composable
private fun ControlsDrawing(selected: Part, onSelect: (Part) -> Unit) {
    PartCard(padding = 16.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            // The steering wheel's scroll wheel with its click.
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val on = selected == Part.Of(Control.WHEEL_CLICK)
                DrawnPart(stringResource(R.string.phone_control_wheel), on, { onSelect(Part.Of(Control.WHEEL_CLICK)) }, Modifier.size(width = 56.dp, height = 120.dp), RoundedCornerShape(28.dp)) {
                    Canvas(Modifier.fillMaxSize().padding(10.dp)) {
                        val ridge = if (on) PhoneColors.Accent else PhoneColors.Muted
                        for (i in 0 until 9) {
                            val y = size.height * (i + 0.5f) / 9
                            drawRoundRect(ridge, Offset(size.width * 0.15f, y - 2f), Size(size.width * 0.7f, 4f), CornerRadius(2f))
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.phone_buttons_steering), color = PhoneColors.Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
            // The dash knob: pad around the push, Menu and Back below.
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                    DrawnPart(stringResource(R.string.phone_control_pad), selected == Part.Pad, { onSelect(Part.Pad) }, Modifier.size(140.dp), CircleShape) {
                        Canvas(Modifier.fillMaxSize()) {
                            val c = Offset(size.width / 2, size.height / 2)
                            val r = size.minDimension * 0.42f
                            val col = if (selected == Part.Pad) PhoneColors.Accent else PhoneColors.Muted
                            listOf(Offset(0f, -r), Offset(0f, r), Offset(-r, 0f), Offset(r, 0f)).forEach { d ->
                                drawCircle(col, 5.dp.toPx(), c + d)
                            }
                        }
                    }
                    DrawnPart(stringResource(R.string.phone_control_knob), selected == Part.Of(Control.KNOB_PUSH), { onSelect(Part.Of(Control.KNOB_PUSH)) },
                        Modifier.size(76.dp), CircleShape) {}
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DrawnPart(stringResource(R.string.phone_control_menu), selected == Part.Of(Control.MENU), { onSelect(Part.Of(Control.MENU)) },
                        Modifier.size(width = 64.dp, height = 36.dp), RoundedCornerShape(12.dp), label = stringResource(R.string.phone_control_menu))
                    DrawnPart(stringResource(R.string.phone_control_back), selected == Part.Of(Control.BACK), { onSelect(Part.Of(Control.BACK)) },
                        Modifier.size(width = 64.dp, height = 36.dp), RoundedCornerShape(12.dp), label = stringResource(R.string.phone_control_back))
                }
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.phone_buttons_dash), color = PhoneColors.Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun DrawnPart(
    description: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier,
    shape: androidx.compose.ui.graphics.Shape, label: String? = null, content: @Composable () -> Unit = {},
) {
    Box(
        modifier
            .clip(shape)
            .background(if (selected) PhoneColors.AccentCard else PhoneColors.Circle)
            .border(if (selected) 2.5.dp else 1.dp, if (selected) PhoneColors.Accent else PhoneColors.Border, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description; this.selected = selected },
        contentAlignment = Alignment.Center,
    ) {
        content()
        if (label != null) Text(label, color = if (selected) PhoneColors.Accent else PhoneColors.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * HN-1: where a button taught "Navigate home" goes: an address or "lat,lng", saved as typed (the
 * car side reads it at each press). Empty: the car says to set it here.
 */
@Composable
private fun HomeAddressField(settings: net.prototux.aare.Settings) {
    var address by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(settings.myplayHomeAddress) }
    androidx.compose.material3.OutlinedTextField(
        address, { v -> address = v.take(200); settings.myplayHomeAddress = address },
        singleLine = true,
        label = { androidx.compose.material3.Text(stringResource(R.string.phone_home_address_title)) },
        supportingText = { androidx.compose.material3.Text(stringResource(R.string.phone_home_address_hint)) },
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedBorderColor = PhoneColors.Accent, unfocusedBorderColor = PhoneColors.Border),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag(HOME_ADDRESS_TAG),
    )
}

internal const val HOME_ADDRESS_TAG = "phone_home_address"
