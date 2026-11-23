package net.prototux.aare.ui.phone.next

import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.audio.CaptureMethods
import net.prototux.aare.car.ButtonAction
import net.prototux.aare.car.ButtonTable
import net.prototux.aare.car.ButtonTable.Control
import net.prototux.aare.car.ButtonTable.Gesture
import net.prototux.aare.car.CarInputLog
import net.prototux.aare.car.DOUBLE_RANGE
import net.prototux.aare.car.GestureEngine
import net.prototux.aare.car.HOLD_RANGE
import net.prototux.aare.car.LearnedButtons
import net.prototux.aare.car.SpeedSourceMode
import net.prototux.aare.car.myplayButtons
import net.prototux.aare.car.myplayGestureDoubleMs
import net.prototux.aare.car.myplayGestureHoldMs
import net.prototux.aare.car.myplayGestureLine
import net.prototux.aare.car.myplayHomeAddress
import net.prototux.aare.car.myplayPlaces
import net.prototux.aare.car.myplayLearnButton
import net.prototux.aare.car.myplayLearnButtonAtMs
import net.prototux.aare.car.myplayLearnedButtons
import net.prototux.aare.car.myplaySpeedSource
import net.prototux.aare.service.ProjectionService
import net.prototux.aare.ui.car.HomeMaps
import net.prototux.aare.ui.car.myplay.HomePad
import net.prototux.aare.ui.car.myplay.HomePadAction
import net.prototux.aare.ui.car.myplay.HomePadKey
import net.prototux.aare.ui.car.myplay.myplayHomePad
import net.prototux.aare.ui.car.myplay.myplayInfoHome
import net.prototux.aare.ui.car.myplayHomeMaps
import net.prototux.aare.ui.phone.HOME_PAD_ACTION_NAMES
import net.prototux.aare.ui.phone.SPEED_SOURCE_NAMES
import net.prototux.aare.ui.phone.SpeedLimitFiles
import net.prototux.aare.ui.phone.TEACHABLE_ACTIONS
import net.prototux.aare.ui.phone.actionName
import net.prototux.aare.ui.phone.allowedActions
import net.prototux.aare.ui.phone.changedCount
import net.prototux.aare.ui.phone.controlName
import net.prototux.aare.ui.phone.defaultAction
import net.prototux.aare.ui.phone.gestureName
import net.prototux.aare.ui.phone.gesturesFor
import net.prototux.aare.ui.phone.learnedRows
import net.prototux.aare.ui.phone.myplayCarSendsHolds
import net.prototux.aare.ui.phone.myplaySpeedLimitRegion
import net.prototux.aare.ui.phone.withAction

/** The list every Car page sits in. */
@Composable
private fun CarPage(route: String, content: LazyListScope.() -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().testTag("next-page-$route"),
        state = rememberNextListState(),
        contentPadding = PaddingValues(start = NextSpace.Gutter, end = NextSpace.Gutter, bottom = 24.dp),
        content = content,
    )
}

/** The action's words; pressing the ring is "highlight" in the new app (review "Second pass" item 5). */
@Composable
internal fun actionWords(a: ButtonAction): String =
    if (a == ButtonAction.SELECT) stringResource(R.string.next_car_select_label) else stringResource(actionName(a))

/** How many of the button settings differ from their defaults (the Buttons row's value). */
fun buttonsChanged(stored: String, pad: Map<HomePadKey, HomePadAction>, infoHome: Boolean, holdMs: Long, doubleMs: Long): Int =
    changedCount(stored) + HomePadKey.entries.count { (pad[it] ?: HomePadAction.NOTHING) != (HomePad.DEFAULT[it] ?: HomePadAction.NOTHING) } +
        (if (infoHome) 1 else 0) + (if (holdMs != GestureEngine.HOLD_MS) 1 else 0) + (if (doubleMs != GestureEngine.DOUBLE_MS) 1 else 0)

// ---------------------------------------------------------------- the root

/** Car (design frame 20): every page with its value; Passenger mode with its warning line. */
@Composable
fun NextCarRoot() {
    val ctx = LocalContext.current
    val nav = LocalNextNavigator.current
    val settings = remember { Settings(ctx) }
    val changed by rememberSetting("myplay_buttons", "myplay_home_pad", "myplay_info_home", "myplay_gesture_hold_ms", "myplay_gesture_double_ms") {
        buttonsChanged(myplayButtons, myplayHomePad, myplayInfoHome, myplayGestureHoldMs, myplayGestureDoubleMs)
    }
    val homeMaps by rememberSetting("myplay_home_maps") { myplayHomeMaps }
    val source by rememberSetting("myplay_speed_source") { myplaySpeedSource }
    val region by rememberSetting("myplay_speed_limit_region") { myplaySpeedLimitRegion }
    val carAudio by rememberSetting("car_audio") { carAudio }
    val banners by rememberSetting("message_banners") { messageBanners }
    val assistant by rememberSetting("assistant_pkg") { assistantPackage }
    val passenger by rememberSetting("ignore_driving") { ignoreDriving }
    val state by ProjectionService.state.collectAsState()
    val audioReady = remember(state.audioCapture) {
        runCatching { CaptureMethods.audioPolicyAvailable(ctx) || CaptureMethods.shizukuAvailable() || CaptureMethods.rootAvailable(ctx) }
            .getOrDefault(false) || state.audioCapture
    }
    val sourceWord = stringResource(SPEED_SOURCE_NAMES.first { it.first == source }.second)
    val regionName = SpeedLimitFiles.REGIONS.firstOrNull { it.id == region }?.name
    CarPage("car") {
        item {
            NextGroup(listOf(
                {
                    NextValueRow(stringResource(R.string.next_car_buttons_label),
                        if (changed == 0) stringResource(R.string.next_car_default_label) else stringResource(R.string.next_car_changed_count_label, changed),
                        Icons.Outlined.RadioButtonChecked, changed = changed > 0) { nav.open("car/buttons") }
                },
                { NextValueRow(stringResource(R.string.next_car_maps_label), homeMapsWord(homeMaps), Icons.Outlined.Map) { nav.open("car/maps") } },
                {
                    NextValueRow(stringResource(R.string.next_car_speed_label),
                        if (source == SpeedSourceMode.OFF || regionName == null) sourceWord else "$sourceWord · $regionName",
                        Icons.Outlined.Speed) { nav.open("car/speed") }
                },
                {
                    // The Sound page lives in More › Advanced (the owner's car plays over its own Bluetooth);
                    // this switch stays here because it also carries the maps app's voice (guidance channel).
                    NextSwitchRow(stringResource(R.string.next_sound_switch_label), carAudio, { settings.carAudio = it },
                        Icons.AutoMirrored.Outlined.VolumeUp,
                        note = if (carAudio && !audioReady) RowNote.Warn(stringResource(R.string.next_car_sound_needs_help))
                            else RowNote.Help(stringResource(R.string.next_car_sound_voice_help)),
                        info = stringResource(R.string.next_car_sound_info), tag = { NextConnectionTag() })
                },
                {
                    NextValueRow(stringResource(R.string.next_car_messages_label),
                        stringResource(if (banners) R.string.next_car_banners_on_label else R.string.next_car_off_label),
                        Icons.Outlined.Chat) { nav.open("car/messages") }
                },
                {
                    NextValueRow(stringResource(R.string.next_car_voice_label),
                        assistant?.let { appLabel(ctx, it) } ?: stringResource(R.string.next_car_same_as_phone_label),
                        Icons.Outlined.Mic) { nav.open("car/voice") }
                },
            ))
            Spacer(Modifier.height(12.dp))
        }
        item {
            NextCard {
                NextSwitchRow(
                    stringResource(R.string.next_car_passenger_label), passenger, { settings.ignoreDriving = it }, Icons.Outlined.People,
                    note = RowNote.Warn(stringResource(R.string.next_car_passenger_warn_help)),
                    info = stringResource(R.string.next_car_passenger_info),
                )
            }
        }
    }
}

@Composable
private fun homeMapsWord(setting: String): String = when {
    setting == HomeMaps.LAST -> stringResource(R.string.next_car_last_used_label)
    setting == HomeMaps.ASK -> stringResource(R.string.next_car_always_ask_label)
    setting.startsWith(HomeMaps.APP_PREFIX) -> appLabel(LocalContext.current, setting.removePrefix(HomeMaps.APP_PREFIX))
    else -> setting
}

/** An app's name from the phone, or its package when it is not installed. */
internal fun appLabel(ctx: android.content.Context, pkg: String): String = runCatching {
    val pm = ctx.packageManager
    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
}.getOrDefault(pkg)

// ---------------------------------------------------------------- Buttons

/** What the drawing's parts are: the four controls in the table, and the pad, which always moves the highlight. */
private const val PAD = "pad"

/** Car › Buttons (frames 22, 23): the knob and wheel drawn; each gesture's action; the pages under it. */
@Composable
fun NextButtonsPage() {
    val ctx = LocalContext.current
    val nav = LocalNextNavigator.current
    val settings = remember { Settings(ctx) }
    val stored by rememberSetting("myplay_buttons") { myplayButtons }
    val pad by rememberSetting("myplay_home_pad") { myplayHomePad }
    val infoHome by rememberSetting("myplay_info_home") { myplayInfoHome }
    val holdMs by rememberSetting("myplay_gesture_hold_ms") { myplayGestureHoldMs }
    val doubleMs by rememberSetting("myplay_gesture_double_ms") { myplayGestureDoubleMs }
    val line by rememberSetting("myplay_gesture_line") { myplayGestureLine }
    val learned by rememberSetting("myplay_learned_buttons") { myplayLearnedButtons }
    val noHolds by rememberSetting("myplay_car_sends_holds") { myplayCarSendsHolds == false }
    var part by rememberSaveable { mutableStateOf(Control.KNOB_PUSH.id) }
    var picking by rememberSaveable { mutableStateOf<String?>(null) } // a gesture id
    val table = ButtonTable(ButtonTable.decodeOverrides(stored))
    val control = Control.byId(part)
    val padChanged = HomePadKey.entries.count { (pad[it] ?: HomePadAction.NOTHING) != (HomePad.DEFAULT[it] ?: HomePadAction.NOTHING) } + (if (infoHome) 1 else 0)
    CarPage("car/buttons") {
        item { ControlsDrawing(part) { part = it } }
        item { NextNote(stringResource(R.string.next_car_tap_control_help), Icons.Outlined.TouchApp) }
        if (control == null) item {
            NextSection(stringResource(R.string.phone_control_pad))
            NextCard { NextRow(stringResource(R.string.next_car_pad_moves_label), note = RowNote.Help(stringResource(R.string.next_car_pad_moves_help))) }
        } else {
            item {
                NextSection(stringResource(controlName(control)))
                if (noHolds && gesturesFor(control).any { it == Gesture.HOLD || it == Gesture.HOLD_TURN }) {
                    NextNote(stringResource(R.string.next_car_no_holds_help), Icons.Outlined.Info)
                    Spacer(Modifier.height(8.dp))
                }
            }
            item {
                NextCard {
                    gesturesFor(control).forEachIndexed { i, g ->
                        if (i > 0) NextDivider(16.dp)
                        val action = table.action(control, g)
                        val isHold = g == Gesture.HOLD || g == Gesture.HOLD_TURN
                        if (noHolds && isHold) {
                            // Shown, but disabled: this car sends no holds (design override).
                            NextRow(stringResource(gestureName(g)), tag = { NextTag(stringResource(R.string.next_car_needs_holds_label)) }, enabled = false) {
                                Text("–", style = NextType.Value, color = NextColors.Text3)
                            }
                        } else {
                            NextValueRow(stringResource(gestureName(g)), actionWords(action), changed = action != defaultAction(control, g)) { picking = g.id }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        item {
            NextGroup(listOf(
                {
                    NextValueRow(stringResource(R.string.next_car_pad_shortcuts_label),
                        if (padChanged == 0) stringResource(R.string.next_car_default_label) else stringResource(R.string.next_car_changed_count_label, padChanged),
                        Icons.Outlined.Apps, changed = padChanged > 0) { nav.open("car/buttons/pad-shortcuts") }
                },
                {
                    NextValueRow(stringResource(R.string.next_car_teach_label),
                        if (learned.isEmpty()) null else stringResource(R.string.next_car_taught_label, learned.size),
                        Icons.Outlined.TouchApp) { nav.open("car/buttons/teach") }
                },
                {
                    NextValueRow(stringResource(R.string.next_car_timing_label), stringResource(R.string.next_car_ms_label, doubleMs.toInt()),
                        Icons.Outlined.Timer, changed = doubleMs != GestureEngine.DOUBLE_MS || holdMs != GestureEngine.HOLD_MS) { nav.open("car/buttons/timing") }
                },
                {
                    NextSwitchRow(stringResource(R.string.next_car_line_label), line, { settings.myplayGestureLine = it }, Icons.Outlined.ViewInAr,
                        tag = { AlsoOnCarTag() })
                },
            ))
        }
        item {
            NextSection(stringResource(R.string.next_car_home_address_label))
            HomeAddress(settings)
            Spacer(Modifier.height(12.dp))
            PlacesCard(settings)
        }
        item {
            NextResetPage({
                val before = listOf(settings.myplayButtons, settings.myplayGestureHoldMs, settings.myplayGestureDoubleMs, settings.myplayHomePad, settings.myplayInfoHome)
                settings.myplayButtons = ""
                settings.myplayGestureHoldMs = GestureEngine.HOLD_MS
                settings.myplayGestureDoubleMs = GestureEngine.DOUBLE_MS
                settings.myplayHomePad = HomePad.DEFAULT
                settings.myplayInfoHome = false
                return@NextResetPage {
                    settings.myplayButtons = before[0] as String
                    settings.myplayGestureHoldMs = before[1] as Long
                    settings.myplayGestureDoubleMs = before[2] as Long
                    @Suppress("UNCHECKED_CAST") settings.myplayHomePad = before[3] as Map<HomePadKey, HomePadAction>
                    settings.myplayInfoHome = before[4] as Boolean
                }
            }, enabled = buttonsChanged(stored, pad, infoHome, holdMs, doubleMs) > 0)
        }
    }
    val g = picking?.let { Gesture.byId(it) }
    if (control != null && g != null) {
        val current = table.action(control, g)
        NextSheet("${stringResource(controlName(control))} · ${stringResource(gestureName(g))}", { picking = null }) {
            NextCard {
                // Choices that would leave no way out are not offered (the code's rule, allowedActions).
                allowedActions(stored, control, g).forEachIndexed { i, a ->
                    if (i > 0) NextDivider(16.dp)
                    NextRow(actionWords(a), onClick = {
                        settings.myplayButtons = withAction(stored, control, g, a); picking = null
                    }, role = Role.RadioButton) { if (a == current) Icon(Icons.Rounded.Check, null, tint = NextColors.Int) }
                }
            }
        }
    }
}

/**
 * The saved places (ideas #5) next to the home address: up to five, each a name and an address; tap to
 * edit, × to forget with undo. Kept on this phone only (not in backups), as the car side decided (PL-2).
 */
@Composable
private fun PlacesCard(settings: Settings) {
    val undo = LocalNextUndo.current
    val places by rememberSetting(net.prototux.aare.car.KEY_PLACES) { myplayPlaces }
    var editing by rememberSaveable { mutableStateOf<Int?>(null) } // index; places.size = a new one
    val forgotten = stringResource(R.string.next_car_place_forgotten_label)
    NextCard {
        places.forEachIndexed { i, p ->
            if (i > 0) NextDivider(16.dp)
            val forget = stringResource(R.string.next_car_forget_label, p.name.ifEmpty { p.address })
            NextRow(p.name.ifEmpty { p.address }, note = if (p.name.isNotEmpty()) RowNote.Help(p.address) else null,
                onClick = { editing = i }, role = Role.Button) {
                Box(
                    Modifier.minimumInteractiveComponentSize().clip(CircleShape).clickable(role = Role.Button) {
                        val before = settings.myplayPlaces
                        settings.myplayPlaces = before.filterIndexed { j, _ -> j != i }
                        undo.show(forgotten) { settings.myplayPlaces = before }
                    }.semantics { contentDescription = forget },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.Close, null, tint = NextColors.Text2) }
            }
        }
        if (places.size < net.prototux.aare.car.Places.MAX) {
            if (places.isNotEmpty()) NextDivider(16.dp)
            NextRow(stringResource(R.string.next_car_add_place_label), onClick = { editing = places.size }, role = Role.Button,
                note = if (places.isEmpty()) RowNote.Help(stringResource(R.string.next_car_places_help)) else null)
        }
    }
    editing?.let { i ->
        val p = places.getOrNull(i) ?: net.prototux.aare.car.SavedPlace("", "")
        PlaceDialog(p, onDismiss = { editing = null }) { edited ->
            editing = null
            settings.myplayPlaces = if (i < places.size) places.mapIndexed { j, old -> if (j == i) edited else old } else places + edited
        }
    }
}

/** A place's name and address in one dialog; the address is needed, the name is not. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun PlaceDialog(place: net.prototux.aare.car.SavedPlace, onDismiss: () -> Unit, onSave: (net.prototux.aare.car.SavedPlace) -> Unit) {
    var name by rememberSaveable { mutableStateOf(place.name) }
    var address by rememberSaveable { mutableStateOf(place.address) }
    androidx.compose.material3.BasicAlertDialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.clip(RoundedCornerShape(NextSpace.SheetRadius)).background(NextColors.Sheet)
                .border(1.dp, NextColors.Edge2, RoundedCornerShape(NextSpace.SheetRadius)).padding(24.dp),
        ) {
            Text(stringResource(R.string.next_car_place_label), style = NextType.SheetTitle, color = NextColors.Text)
            Spacer(Modifier.height(12.dp))
            PlaceField(name, { name = it.take(net.prototux.aare.car.Places.NAME_MAX) }, stringResource(R.string.next_car_place_name_label))
            Spacer(Modifier.height(8.dp))
            PlaceField(address, { address = it.take(net.prototux.aare.car.Places.ADDRESS_MAX) }, stringResource(R.string.next_car_address_field_label))
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NextButton(stringResource(android.R.string.cancel), onDismiss, Modifier.weight(1f), ButtonKind.GHOST)
                NextButton(stringResource(R.string.next_looks_save_button_label), { onSave(net.prototux.aare.car.SavedPlace(name.trim(), address.trim())) },
                    Modifier.weight(1f), enabled = address.isNotBlank())
            }
        }
    }
}

@Composable
private fun PlaceField(value: String, onChange: (String) -> Unit, label: String) {
    androidx.compose.material3.OutlinedTextField(
        value, onChange, singleLine = true, modifier = Modifier.fillMaxWidth(), label = { Text(label) },
        shape = RoundedCornerShape(14.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NextColors.Int, unfocusedBorderColor = NextColors.Outline,
            focusedTextColor = NextColors.Text, unfocusedTextColor = NextColors.Text,
            focusedLabelColor = NextColors.Int, unfocusedLabelColor = NextColors.Text2, cursorColor = NextColors.Int,
        ),
    )
}

/** The home address for "Navigate home", saved as it is typed (plan §3.4 rule 7). */
@Composable
private fun HomeAddress(settings: Settings) {
    var text by rememberSaveable { mutableStateOf(settings.myplayHomeAddress) }
    NextCard {
        androidx.compose.material3.OutlinedTextField(
            text, { v -> text = v.take(200); settings.myplayHomeAddress = text }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            label = { Text(stringResource(R.string.next_car_address_field_label)) },
            supportingText = { Text(stringResource(R.string.next_car_address_help)) },
            shape = RoundedCornerShape(14.dp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NextColors.Int, unfocusedBorderColor = NextColors.Outline,
                focusedTextColor = NextColors.Text, unfocusedTextColor = NextColors.Text,
                focusedLabelColor = NextColors.Int, unfocusedLabelColor = NextColors.Text2, cursorColor = NextColors.Int,
                focusedSupportingTextColor = NextColors.Text3, unfocusedSupportingTextColor = NextColors.Text3,
            ),
        )
    }
}

/** The dash knob with its pad, Menu and Back, and the steering wheel's click (frame 22); tap one to choose it. */
@Composable
private fun ControlsDrawing(selected: String, onSelect: (String) -> Unit) {
    NextCard {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.next_car_dash_knob_label).uppercase(), style = NextType.Tag, color = NextColors.Text2)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallControl(Icons.Rounded.Menu, stringResource(R.string.phone_control_menu), selected == Control.MENU.id) { onSelect(Control.MENU.id) }
                    SmallControl(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.phone_control_back), selected == Control.BACK.id) { onSelect(Control.BACK.id) }
                }
                Knob(selected == Control.KNOB_PUSH.id, selected == PAD, onPush = { onSelect(Control.KNOB_PUSH.id) }, onPad = { onSelect(PAD) })
            }
            Text(stringResource(R.string.next_car_knob_pad_label), style = NextType.Help, color = NextColors.Text2, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.next_car_wheel_label).uppercase(), style = NextType.Tag, color = NextColors.Text2)
            Spacer(Modifier.height(10.dp))
            val wheel = stringResource(R.string.phone_control_wheel)
            val on = selected == Control.WHEEL_CLICK.id
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier.size(120.dp, 40.dp).clip(RoundedCornerShape(20.dp))
                        .border(if (on) 3.dp else 1.5.dp, if (on) NextColors.Int else NextColors.Outline, RoundedCornerShape(20.dp))
                        .clickable(role = Role.RadioButton) { onSelect(Control.WHEEL_CLICK.id) }
                        .semantics { contentDescription = wheel; this.selected = on },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { repeat(8) { Box(Modifier.size(2.dp, 18.dp).background(NextColors.Text2)) } }
                }
                Text(wheel, style = NextType.Value, color = NextColors.Text)
            }
        }
    }
}

@Composable
private fun SmallControl(icon: ImageVector, name: String, on: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).border(if (on) 3.dp else 1.5.dp, if (on) NextColors.Int else NextColors.Outline, CircleShape)
                .clickable(role = Role.RadioButton, onClick = onClick).semantics { contentDescription = name; selected = on },
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = NextColors.Text) }
        Text(name, style = NextType.Help, color = NextColors.Text2, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun Knob(pushOn: Boolean, padOn: Boolean, onPush: () -> Unit, onPad: () -> Unit) {
    val push = stringResource(R.string.phone_control_knob)
    val pad = stringResource(R.string.phone_control_pad)
    Box(
        Modifier.size(140.dp).clip(CircleShape).border(if (padOn) 3.dp else 1.5.dp, if (padOn) NextColors.Int else NextColors.Outline, CircleShape)
            .clickable(role = Role.RadioButton, onClick = onPad).semantics { contentDescription = pad; selected = padOn },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // The pad's four arrows.
            val c = center
            val r = size.minDimension / 2 - 10.dp.toPx()
            for (a in listOf(0f, 90f, 180f, 270f)) {
                val rad = Math.toRadians(a.toDouble())
                drawCircle(NextColors.Text2, 3.dp.toPx(), Offset(c.x + (r * Math.cos(rad)).toFloat(), c.y + (r * Math.sin(rad)).toFloat()))
            }
        }
        Box(
            Modifier.size(92.dp).clip(CircleShape).background(NextColors.Surface2)
                .border(if (pushOn) 3.dp else 2.dp, if (pushOn) NextColors.Int else NextColors.Text2, CircleShape)
                .clickable(role = Role.RadioButton, onClick = onPush).semantics { contentDescription = push; selected = pushOn },
            contentAlignment = Alignment.Center,
        ) { Box(Modifier.size(44.dp).border(2.dp, NextColors.Text, CircleShape)) }
    }
}

// ---------------------------------------------------------------- Pad shortcuts

private val PAD_ROWS: List<Pair<HomePadKey, Int>> = listOf(
    HomePadKey.CENTER to R.string.next_car_pad_center_label,
    HomePadKey.UP to R.string.next_car_pad_up_label,
    HomePadKey.LEFT to R.string.next_car_pad_left_label,
    HomePadKey.RIGHT to R.string.next_car_pad_right_label,
    HomePadKey.DOWN to R.string.next_car_pad_down_label,
)

private fun padIcon(a: HomePadAction): androidx.compose.ui.graphics.vector.ImageVector = when (a) {
    HomePadAction.MUSIC -> Icons.Outlined.MusicNote
    HomePadAction.MAPS -> Icons.Outlined.Map
    HomePadAction.DIAL -> Icons.Outlined.RadioButtonChecked
    HomePadAction.APPS -> Icons.Outlined.Apps
    HomePadAction.CALLS, HomePadAction.CALL_BACK -> Icons.Outlined.Call
    HomePadAction.PLACES -> Icons.Outlined.Place
    HomePadAction.NEXT_SONG -> Icons.Outlined.SkipNext
    HomePadAction.NAVIGATE_HOME -> Icons.Outlined.Home
    else -> Icons.Outlined.Block
}

/**
 * The pad drawn round (frame 24): the knob push in the middle, pad up above, down below, left or
 * right beside, each with what it opens. A picture only: the rows below say the same for TalkBack.
 */
@Composable
private fun PadDrawing(pad: Map<HomePadKey, HomePadAction>) {
    val names = HOME_PAD_ACTION_NAMES.associate { (a, res) -> a to stringResource(res) }
    val keyNames = PAD_ROWS.associate { (k, res) -> k to stringResource(res) }
    @Composable
    fun Spot(key: HomePadKey, modifier: Modifier) {
        val a = pad[key] ?: HomePadAction.NOTHING
        Column(modifier.width(120.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(NextColors.Surface).border(2.dp, NextColors.Outline, CircleShape),
                contentAlignment = Alignment.Center) { Icon(padIcon(a), null, tint = NextTab.CAR.world, modifier = Modifier.size(28.dp)) }
            Text(names[a] ?: a.id, style = NextType.Label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight(620)), color = NextColors.Text,
                maxLines = 1, modifier = Modifier.padding(top = 4.dp))
            Text(keyNames[key].orEmpty(), style = NextType.Help, color = NextColors.Text3, maxLines = 1)
        }
    }
    Box(Modifier.fillMaxWidth().height(330.dp).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Box(Modifier.size(270.dp).clip(CircleShape).border(1.dp, NextColors.Edge2, CircleShape))
        Spot(HomePadKey.UP, Modifier.align(Alignment.TopCenter))
        Spot(HomePadKey.CENTER, Modifier.align(Alignment.Center))
        Spot(HomePadKey.LEFT, Modifier.align(Alignment.CenterStart).padding(start = 8.dp))
        Spot(HomePadKey.RIGHT, Modifier.align(Alignment.CenterEnd).padding(end = 8.dp))
        Spot(HomePadKey.DOWN, Modifier.align(Alignment.BottomCenter))
    }
}

/** Car › Buttons › ORBIT home: pad shortcuts (frame 24). */
@Composable
fun NextPadShortcutsPage() {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    val pad by rememberSetting("myplay_home_pad") { myplayHomePad }
    val infoHome by rememberSetting("myplay_info_home") { myplayInfoHome }
    CarPage("car/buttons/pad-shortcuts") {
        item {
            NextCard {
                NextSwitchRow(stringResource(R.string.next_car_info_home_label), infoHome, { settings.myplayInfoHome = it }, Icons.Outlined.Info,
                    note = RowNote.Help(stringResource(R.string.next_car_info_home_help)), changed = infoHome)
            }
            Spacer(Modifier.height(12.dp))
        }
        item {
            PadDrawing(pad)
            Spacer(Modifier.height(12.dp))
        }
        item {
            NextCard {
                PAD_ROWS.forEachIndexed { i, (key, name) ->
                    if (i > 0) NextDivider(16.dp)
                    val current = pad[key] ?: HomePadAction.NOTHING
                    NextPickerRow(
                        stringResource(name), HOME_PAD_ACTION_NAMES.map { (a, res) -> Choice(a, stringResource(res)) },
                        current, { settings.myplayHomePad = pad + (key to it) }, default = HomePad.DEFAULT[key] ?: HomePadAction.NOTHING,
                    )
                }
            }
            NextNote(stringResource(R.string.next_car_pad_works_help), Icons.Outlined.Info)
        }
        item {
            NextResetPage({
                val before = settings.myplayHomePad to settings.myplayInfoHome
                settings.myplayHomePad = HomePad.DEFAULT; settings.myplayInfoHome = false
                return@NextResetPage { settings.myplayHomePad = before.first; settings.myplayInfoHome = before.second }
            }, enabled = infoHome || pad != HomePad.DEFAULT)
        }
    }
}

// ---------------------------------------------------------------- Timing

/** Car › Buttons › Timing (frame 25): the two times; the hold disabled when this car sends no holds. */
@Composable
fun NextTimingPage() {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    val holdMs by rememberSetting("myplay_gesture_hold_ms") { myplayGestureHoldMs }
    val doubleMs by rememberSetting("myplay_gesture_double_ms") { myplayGestureDoubleMs }
    val noHolds by rememberSetting("myplay_car_sends_holds") { myplayCarSendsHolds == false }
    var dragDouble by remember { mutableStateOf<Float?>(null) }
    var dragHold by remember { mutableStateOf<Float?>(null) }
    CarPage("car/buttons/timing") {
        item {
            val v = dragDouble ?: doubleMs.toFloat()
            NextCard {
                NextSlider(
                    stringResource(R.string.next_car_double_within_label), stringResource(R.string.next_car_ms_label, v.toInt()),
                    v, DOUBLE_RANGE.first.toFloat()..DOUBLE_RANGE.last.toFloat(),
                    { dragDouble = (Math.round(it / 25f) * 25).toFloat() },
                    { dragDouble?.let { settings.myplayGestureDoubleMs = it.toLong() }; dragDouble = null },
                    ends = "${DOUBLE_RANGE.first} ms" to "${DOUBLE_RANGE.last} ms", changed = doubleMs != GestureEngine.DOUBLE_MS,
                )
            }
            NextNote(stringResource(R.string.next_car_double_help), Icons.Outlined.Info)
            Spacer(Modifier.height(12.dp))
        }
        item {
            val v = dragHold ?: holdMs.toFloat()
            NextCard {
                if (noHolds) {
                    NextRow(stringResource(R.string.next_car_hold_longer_label), tag = { NextTag(stringResource(R.string.next_car_needs_holds_label)) }, enabled = false) {
                        NextValue(stringResource(R.string.next_car_not_used_label))
                    }
                } else NextSlider(
                    stringResource(R.string.next_car_hold_longer_label), stringResource(R.string.next_car_ms_label, v.toInt()),
                    v, HOLD_RANGE.first.toFloat()..HOLD_RANGE.last.toFloat(),
                    { dragHold = (Math.round(it / 50f) * 50).toFloat() },
                    { dragHold?.let { settings.myplayGestureHoldMs = it.toLong() }; dragHold = null },
                    ends = "${HOLD_RANGE.first} ms" to "${HOLD_RANGE.last} ms", changed = holdMs != GestureEngine.HOLD_MS,
                )
            }
            if (noHolds) NextNote(stringResource(R.string.next_car_no_holds_help), Icons.Outlined.Info)
        }
        item {
            NextResetPage({
                val before = settings.myplayGestureDoubleMs to settings.myplayGestureHoldMs
                settings.myplayGestureDoubleMs = GestureEngine.DOUBLE_MS; settings.myplayGestureHoldMs = GestureEngine.HOLD_MS
                return@NextResetPage { settings.myplayGestureDoubleMs = before.first; settings.myplayGestureHoldMs = before.second }
            }, enabled = doubleMs != GestureEngine.DOUBLE_MS || holdMs != GestureEngine.HOLD_MS)
        }
    }
}

// ---------------------------------------------------------------- Teach

/** Car › Buttons › Teach a car button (frames 26–28): choose, teach, then the buttons taught with Forget. */
@Composable
fun NextTeachPage() {
    val ctx = LocalContext.current
    val undo = LocalNextUndo.current
    val settings = remember { Settings(ctx) }
    val armed by rememberSetting("myplay_learn_button", "myplay_learn_button_at") { myplayLearnButton }
    val learned by rememberSetting("myplay_learned_buttons") { myplayLearnedButtons }
    val connected = ProjectionService.state.collectAsState().value.connected
    var chosen by rememberSaveable { mutableStateOf(ButtonAction.OPEN_NAVIGATION.id) }
    val forgotten = stringResource(R.string.next_car_forgotten_label)
    // The arm lapses after LearnedButtons.ARM_MS: count down, and read it again each second.
    var secondsLeft by remember { mutableLongStateOf(0L) }
    var tick by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(armed) {
        while (armed != null) {
            val left = LearnedButtons.ARM_MS - (System.currentTimeMillis() - settings.myplayLearnButtonAtMs)
            secondsLeft = (left / 1000).coerceAtLeast(0)
            tick = (left.toFloat() / LearnedButtons.ARM_MS).coerceIn(0f, 1f)
            if (left <= 0) { settings.myplayLearnButton = null; break }
            delay(1_000)
        }
    }
    CarPage("car/buttons/teach") {
        val waiting = armed
        if (waiting != null) item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                BreathingRing(tick)
                Text(stringResource(R.string.next_car_press_in_car_label), style = NextType.Hero, color = NextColors.Text, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 14.dp))
                Text(stringResource(R.string.next_car_will_do_help, actionWords(waiting)), style = NextType.Value, color = NextColors.Text2, modifier = Modifier.padding(top = 4.dp))
                Text(stringResource(R.string.next_car_seconds_left_help, secondsLeft.toInt()), style = NextType.Value, color = NextColors.Text2, modifier = Modifier.padding(top = 6.dp))
                Spacer(Modifier.height(16.dp))
                NextButton(stringResource(R.string.next_home_cancel_label), { settings.myplayLearnButton = null }, kind = ButtonKind.GHOST, small = true)
            }
        } else {
            item {
                NextCard {
                    NextPickerRow(stringResource(R.string.next_car_what_it_does_label), TEACHABLE_ACTIONS.map { Choice(it.id, actionWordsPlain(it)) },
                        chosen, { chosen = it }, icon = Icons.Outlined.TouchApp)
                }
                Spacer(Modifier.height(12.dp))
                NextButton(stringResource(R.string.next_car_teach_it_label), {
                    ButtonAction.byId(chosen)?.let { settings.myplayLearnButton = it }
                }, Modifier.fillMaxWidth(), icon = Icons.Outlined.TouchApp, enabled = connected)
                // Teach listens to the car: nothing to tap until it connects (frame 28).
                if (!connected) NextNote(stringResource(R.string.next_car_connect_first_help), Icons.Rounded.Cable)
            }
            val rows = learnedRows(learned)
            if (rows.isNotEmpty()) {
                item { NextSection(stringResource(R.string.next_car_taught_title_label)) }
                item {
                    NextCard {
                        rows.forEachIndexed { i, (code, action) ->
                            if (i > 0) NextDivider()
                            val key = CarInputLog.keyName(code)
                            val forget = stringResource(R.string.next_car_forget_label, key)
                            NextRow(actionWords(action), icon = Icons.Outlined.RadioButtonChecked, note = RowNote.Help(key)) {
                                Box(
                                    Modifier.minimumInteractiveComponentSize().clip(CircleShape).clickable(role = Role.Button) {
                                        val before = settings.myplayLearnedButtons
                                        settings.myplayLearnedButtons = LearnedButtons.learn(before, code, ButtonAction.NOTHING)
                                        undo.show(forgotten) { settings.myplayLearnedButtons = before }
                                    }.semantics { contentDescription = forget },
                                    contentAlignment = Alignment.Center,
                                ) { Icon(Icons.Outlined.Close, null, tint = NextColors.Text2) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun actionWordsPlain(a: ButtonAction): String = actionWords(a)

/** The waiting ring (frame 27): the time left as an arc, breathing gently. */
@Composable
private fun BreathingRing(left: Float, size: Dp = 200.dp) {
    val breathe = rememberInfiniteTransition(label = "breathe")
    val s by breathe.animateFloat(1f, 1.04f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "s")
    Box(Modifier.padding(top = 16.dp).size(size).scale(s), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = 6.dp.toPx()
            drawCircle(NextColors.Track, radius = this.size.minDimension / 2 - w, style = Stroke(w))
            val inset = w
            drawArc(NextColors.Text, -90f, 360f * left, false, Offset(inset, inset),
                androidx.compose.ui.geometry.Size(this.size.width - 2 * inset, this.size.height - 2 * inset), style = Stroke(w))
        }
        Icon(Icons.Outlined.TouchApp, null, tint = NextColors.Text, modifier = Modifier.size(64.dp))
    }
}

