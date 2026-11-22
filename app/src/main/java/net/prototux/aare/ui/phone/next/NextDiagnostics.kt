package net.prototux.aare.ui.phone.next

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subject
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.DateFormat
import net.prototux.aare.DiagLog
import net.prototux.aare.R
import net.prototux.aare.SessionAapLog
import net.prototux.aare.Settings
import net.prototux.aare.car.CarData
import net.prototux.aare.car.CarInputEvents
import net.prototux.aare.service.ProjectionService
import net.prototux.aare.ui.phone.CarCheckLog
import net.prototux.aare.ui.phone.CheckStep
import net.prototux.aare.ui.phone.checkSummary
import net.prototux.aare.ui.phone.distinctKeys
import net.prototux.aare.ui.phone.title

@Composable
private fun Page(route: String, content: LazyListScope.() -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().testTag("next-page-$route"),
        state = rememberNextListState(),
        contentPadding = PaddingValues(start = NextSpace.Gutter, end = NextSpace.Gutter, bottom = 24.dp),
        content = content,
    )
}

/** More › Diagnostics (frame 57): three tools, each with its state. */
@Composable
fun NextDiagnosticsPage() {
    val nav = LocalNextNavigator.current
    val status = rememberCarCheckStatus()
    val connected = ProjectionService.state.collectAsState().value.connected
    val lines by DiagLog.lines.collectAsState()
    Page("more/diagnostics") {
        item {
            NextGroup(listOf(
                {
                    NextValueRow(stringResource(R.string.next_more_car_check_label), carCheckWords(status), Icons.Outlined.MonitorHeart) {
                        nav.open("more/diagnostics/car-check")
                    }
                },
                {
                    NextValueRow(stringResource(R.string.next_more_car_data_label),
                        stringResource(if (connected) R.string.next_diag_live_label else R.string.next_diag_connect_first_label),
                        Icons.Outlined.Speed) { nav.open("more/diagnostics/car-data") }
                },
                {
                    NextValueRow(stringResource(R.string.next_more_log_label), pluralStringResource(R.plurals.next_diag_lines_label, lines.size, lines.size),
                        Icons.Outlined.Subject) { nav.open("more/diagnostics/log") }
                },
            ))
        }
    }
}

// ---------------------------------------------------------------- Car check

/** The run's state as one value, read again whenever the run or the "was running" setting changes. */
@Composable
fun rememberCarCheckStatus(): CarCheckStatus {
    val step by CarCheckRun.step.collectAsState()
    val running by CarCheckRun.running.collectAsState()
    val captures by CarCheckRun.captures.collectAsState()
    val finishedAt by CarCheckRun.finishedAt.collectAsState()
    val wasRunning by rememberSetting(CarCheckRun.KEY_RUNNING) { prefs.getBoolean(CarCheckRun.KEY_RUNNING, false) }
    return carCheckStatus(step, CheckStep.entries.size, running, captures.isNotEmpty(), wasRunning && !running, finishedAt)
}

@Composable
fun carCheckWords(status: CarCheckStatus): String = when (status) {
    CarCheckStatus.NotRun -> stringResource(R.string.next_diag_not_run_label)
    is CarCheckStatus.InProgress -> stringResource(R.string.next_diag_in_progress_label)
    CarCheckStatus.Stopped -> stringResource(R.string.next_diag_stopped_label)
    is CarCheckStatus.Done -> status.at?.let { stringResource(R.string.next_diag_done_on_label, DateFormat.getDateInstance(DateFormat.MEDIUM).format(it)) }
        ?: stringResource(R.string.next_diag_done_label)
}

/** More › Diagnostics › Car check (frames 58–60): one step at a time, then share. */
@Composable
fun NextCarCheckPage(now: () -> Long = SystemClock::elapsedRealtime) {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    val step by CarCheckRun.step.collectAsState()
    val running by CarCheckRun.running.collectAsState()
    val captures by CarCheckRun.captures.collectAsState()
    val connected = ProjectionService.state.collectAsState().value.connected
    val report by CarInputEvents.report.collectAsState()
    val total = CheckStep.entries.size
    val titles = CheckStep.entries.associateWith { stringResource(it.title) }
    // A run starts when the page is open and the car is connected; it then outlives the page (plan §4).
    LaunchedEffect(connected) {
        CarCheckRun.clearStopped(settings)
        if (connected && !CarCheckRun.done) CarCheckRun.start(settings)
    }
    var clock by remember { mutableLongStateOf(now()) }
    LaunchedEffect(Unit) { while (true) { clock = now(); delay(250) } }
    // Off each time the page opens: the maps app's alert texts can name streets.
    var alertTexts by remember { mutableStateOf(false) }
    var preview by rememberSaveable { mutableStateOf(false) }
    val shareText = { CarCheckLog.shareText(checkSummary(captures, { titles.getValue(it) }, now()), report, DiagLog.lines.value, alertTexts = alertTexts) }
    Page("more/diagnostics/car-check") {
        val current = CheckStep.entries.getOrNull(step)
        if (current == null) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    NextHeroRing(RingStyle.FULL, NextTab.MORE.world, Icons.Rounded.Check)
                    Text(stringResource(R.string.next_diag_all_done_label), style = NextType.Hero, color = NextColors.Text, modifier = Modifier.padding(top = 14.dp))
                    val empty = CheckStep.entries.count { captures[it]?.isEmpty != false }
                    Text(stringResource(R.string.next_diag_summary_help, total, empty), style = NextType.Value, color = NextColors.Text2, modifier = Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(16.dp))
                }
            }
            item {
                NextCard {
                    CheckStep.entries.forEachIndexed { i, s ->
                        if (i > 0) NextDivider()
                        val c = captures[s]
                        NextRow(titles.getValue(s), icon = Icons.Outlined.RadioButtonChecked,
                            onClick = { CarCheckRun.goTo(i); if (connected) CarCheckRun.start(settings) }, role = androidx.compose.ui.semantics.Role.Button) {
                            if (c == null || c.isEmpty) NextTag(stringResource(R.string.next_diag_nothing_tag_label), TagKind.MUTE)
                            else NextTag(stringResource(R.string.next_diag_heard_tag_label), TagKind.OK)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            item {
                NextCard {
                    NextCheckRow(stringResource(R.string.next_diag_alert_texts_label), alertTexts, { alertTexts = it },
                        RowNote.Help(stringResource(R.string.next_diag_street_names_help)))
                    NextDivider()
                    NextValueRow(stringResource(R.string.next_diag_what_shared_label), icon = Icons.Outlined.Description) { preview = true }
                }
                Spacer(Modifier.height(16.dp))
                NextButton(stringResource(R.string.next_diag_share_label), {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_SUBJECT, "MyPlay car check").putExtra(Intent.EXTRA_TEXT, shareText())
                    ctx.startSafely(Intent.createChooser(send, null))
                }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Share)
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    NextButton(stringResource(R.string.next_diag_start_again_label), { CarCheckRun.reset(); if (connected) CarCheckRun.start(settings) }, kind = ButtonKind.TEXT, small = true)
                }
                NextNote(stringResource(R.string.next_diag_private_help), Icons.Outlined.Lock)
            }
        } else {
            item {
                // The progress: one dash per step (frame 58).
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CheckStep.entries.forEachIndexed { i, _ ->
                        Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                            .background(if (i <= step) NextColors.Int else NextColors.Track))
                    }
                }
                Text(stringResource(R.string.next_diag_step_of_label, step + 1, total), style = NextType.Help, color = NextColors.Text3)
                Text(titles.getValue(current), style = NextType.Hero, color = NextColors.Text, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
                if (current.hold) Text(stringResource(R.string.next_diag_hold_help), style = NextType.Value, color = NextColors.Text2,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                Spacer(Modifier.height(16.dp))
            }
            if (!connected) item {
                // The check listens to the car: it starts only when connected (frame 60).
                NextNote(stringResource(R.string.next_car_connect_first_help), Icons.Rounded.Cable)
                Spacer(Modifier.height(12.dp))
            }
            item {
                val c = captures[current]
                NextCard {
                    NextRow(stringResource(R.string.next_diag_heard_label), icon = Icons.Outlined.Subject) {
                        if (current == CheckStep.KNOB_BUTTONS) NextValue(stringResource(R.string.next_diag_of_four_label, distinctKeys(c)))
                    }
                    val lines = c?.lines(clock).orEmpty()
                    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 14.dp).semantics { liveRegion = LiveRegionMode.Polite }) {
                        if (lines.isEmpty()) Text(stringResource(R.string.next_diag_waiting_help), style = NextType.Help, color = NextColors.Text3)
                        lines.forEach { Text(it, style = NextType.Help.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp), color = NextColors.Text2) }
                    }
                }
                Spacer(Modifier.height(16.dp))
                val got = c?.isEmpty == false
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (got) NextButton(stringResource(R.string.next_diag_again_label), { CarCheckRun.again() }, Modifier.weight(1f), ButtonKind.GHOST, enabled = connected)
                    else NextButton(stringResource(R.string.next_diag_nothing_label), { CarCheckRun.next(settings) }, Modifier.weight(1f), ButtonKind.GHOST, enabled = connected)
                    NextButton(stringResource(R.string.next_diag_next_label), { CarCheckRun.next(settings) }, Modifier.weight(1f), enabled = connected && got)
                }
            }
        }
    }
    if (preview) NextSheet(stringResource(R.string.next_diag_what_shared_label), { preview = false }) {
        LazyColumn(Modifier.height(420.dp)) {
            item { Text(shareText(), style = NextType.Help.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp), color = NextColors.Text2) }
        }
    }
}

// ---------------------------------------------------------------- Car data

/** More › Diagnostics › Car data (frame 61): the car-screen switch, who the car is, and live values. */
@Composable
fun NextCarDataPage() {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    val inCar by rememberSetting("car_data_app") { carDataApp }
    val car by CarData.state.collectAsState()
    val proj by ProjectionService.state.collectAsState()
    Page("more/diagnostics/car-data") {
        item {
            NextCard { NextSwitchRow(stringResource(R.string.next_diag_show_in_car_label), inCar, { settings.carDataApp = it }, Icons.Outlined.Tv) }
        }
        if (!car.connected) {
            item { NextNote(stringResource(R.string.next_car_connect_first_help), Icons.Rounded.Cable) }
            return@Page
        }
        item { NextSection(stringResource(R.string.next_diag_car_label)) }
        item {
            val id = car.identity
            DataCard(listOf(
                stringResource(R.string.car_data_identity) to id?.let { listOfNotNull(it.make.ifBlank { null }, it.model.ifBlank { null }, it.year?.toString()).joinToString(" ").ifBlank { null } },
                stringResource(R.string.car_data_head_unit) to proj.headUnit.ifBlank { null },
                stringResource(R.string.car_data_connection) to listOfNotNull(linkOf(proj.transport)?.let { stringResource(if (it == Link.USB) R.string.next_home_usb_label else R.string.next_home_wireless_label) }, proj.video.ifBlank { null }).joinToString(" · ").ifBlank { null },
            ))
        }
        item { NextSection(stringResource(R.string.next_diag_live_data_label), extra = { NextTag(stringResource(R.string.next_diag_live_label), TagKind.OK) }) }
        item {
            DataCard(listOf(
                stringResource(R.string.car_data_driving) to car.drivingStatus?.let { if (it.value == 0) stringResource(R.string.car_data_parked) else stringResource(R.string.car_data_moving) },
                stringResource(R.string.car_data_night) to car.nightMode?.let { if (it.value) stringResource(R.string.car_data_night_on) else stringResource(R.string.car_data_night_off) },
                stringResource(R.string.car_data_speed) to car.speed?.let { "%.0f km/h".format(it.value.metersPerSecond * 3.6f) },
                stringResource(R.string.car_data_gear) to car.gearName,
                stringResource(R.string.car_data_fuel) to car.fuel?.let { f -> listOfNotNull(f.value.levelPercent?.let { "$it %" }, f.value.rangeKm?.let { "$it km" }).joinToString(" · ").ifBlank { null } },
                stringResource(R.string.car_data_environment) to car.environment?.value?.temperatureC?.let { "%.1f °C".format(it) },
                stringResource(R.string.car_data_odometer) to car.odometer?.let { "%.0f km".format(it.value.km) },
                stringResource(R.string.car_data_tyres) to car.tirePressuresKpa?.let { t -> t.value.joinToString(" · ") { "%.0f".format(it) } + " kPa" },
                stringResource(R.string.car_data_doors) to car.doors?.let { d ->
                    if (d.value.doorsOpen.none { it } && d.value.hoodOpen != true && d.value.trunkOpen != true) stringResource(R.string.car_data_closed)
                    else stringResource(R.string.next_diag_open_label)
                },
            ).filter { it.second != null })
        }
    }
}

@Composable
private fun DataCard(rows: List<Pair<String, String?>>) {
    NextCard {
        rows.forEachIndexed { i, (label, value) ->
            if (i > 0) NextDivider(16.dp)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = NextType.Label, color = NextColors.Text2, modifier = Modifier.weight(1f))
                // Tabular numbers, so live values don't shift.
                Text(value ?: "–", style = NextType.Value.copy(fontFeatureSettings = "tnum"), color = NextColors.Text)
            }
        }
    }
}

// ---------------------------------------------------------------- Connection log

/** More › Diagnostics › Connection log (frame 62): the two log switches, the last lines, Copy / Save / Clear. */
@Composable
fun NextLogPage() {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    val log by DiagLog.lines.collectAsState()
    val verbose by rememberSetting("verbose_protocol_log") { verboseProtocolLog }
    val carInput by rememberSetting("log_car_input") { logCarInput }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val title = stringResource(R.string.next_more_log_label)
    // Writes every kept line to a file the owner picks, to attach to a bug report.
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching { ctx.contentResolver.openOutputStream(uri)?.use { it.write(DiagLog.lines.value.joinToString("\n").toByteArray()) } }
            .onFailure { Toast.makeText(ctx, it.message, Toast.LENGTH_SHORT).show() }
    }
    Page("more/diagnostics/log") {
        item {
            NextGroup(listOf(
                {
                    NextSwitchRow(stringResource(R.string.next_diag_verbose_label), verbose, {
                        settings.verboseProtocolLog = it; SessionAapLog.verbose = it
                    }, Icons.Outlined.Subject)
                },
                { NextSwitchRow(stringResource(R.string.next_diag_car_input_label), carInput, { settings.logCarInput = it }, Icons.Outlined.RadioButtonChecked) },
            ))
            Spacer(Modifier.height(12.dp))
        }
        item {
            NextCard {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    if (log.isEmpty()) Text(stringResource(R.string.next_diag_empty_help), style = NextType.Help, color = NextColors.Text3)
                    log.takeLast(40).forEach { Text(it, style = NextType.Help.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp), color = NextColors.Text2) }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Copy takes every kept line, not only those shown.
                NextButton(stringResource(R.string.next_diag_copy_label), {
                    ctx.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(title, log.joinToString("\n")))
                }, Modifier.weight(1f), ButtonKind.GHOST, small = true, enabled = log.isNotEmpty())
                NextButton(stringResource(R.string.next_looks_save_button_label), { save.launch("aare-log.txt") }, Modifier.weight(1f), ButtonKind.GHOST, small = true,
                    enabled = log.isNotEmpty())
                NextButton(stringResource(R.string.next_diag_clear_label), { confirmClear = true }, Modifier.weight(1f), ButtonKind.DANGER, small = true, enabled = log.isNotEmpty())
            }
            NextNote(stringResource(R.string.next_diag_log_private_help), Icons.Outlined.Lock)
        }
    }
    if (confirmClear) NextConfirm(
        stringResource(R.string.next_diag_clear_title_label), stringResource(R.string.next_diag_clear_info), stringResource(R.string.next_diag_clear_label),
        onConfirm = { confirmClear = false; DiagLog.clear() }, onDismiss = { confirmClear = false }, danger = true,
    )
}
