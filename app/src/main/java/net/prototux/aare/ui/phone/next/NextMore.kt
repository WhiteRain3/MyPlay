package net.prototux.aare.ui.phone.next

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.GppGood
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.prototux.aare.BuildConfig
import net.prototux.aare.DiagLog
import net.prototux.aare.PinnedHeadUnits
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.SettingsBackup
import net.prototux.aare.UpdateChecker
import net.prototux.aare.service.ProjectionService
import net.prototux.aare.ui.car.myplay.KEY_ENGINE_LANGUAGE
import net.prototux.aare.ui.car.myplay.myplayEngineLanguage
import net.prototux.aare.ui.car.myplay.myplayEnginePreview
import net.prototux.aare.ui.phone.SpeedLimitDownload
import net.prototux.aare.ui.phone.SpeedLimitFiles
import net.prototux.aare.ui.phone.backupFileName
import net.prototux.aare.ui.phone.lastSavedText
import net.prototux.aare.ui.phone.myplayBackupLastSaved
import net.prototux.aare.ui.phone.myplaySpeedLimitRegion

@Composable
private fun Page(route: String, content: LazyListScope.() -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().testTag("next-page-$route"),
        state = rememberNextListState(),
        contentPadding = PaddingValues(start = NextSpace.Gutter, end = NextSpace.Gutter, bottom = 24.dp),
        content = content,
    )
}

// ---------------------------------------------------------------- the root

/** More (frame 43): rare things, each one tap away, with its value. */
@Composable
fun NextMoreRoot() {
    val ctx = LocalContext.current
    val nav = LocalNextNavigator.current
    val update by UpdateChecker.available.collectAsState()
    val check by NextUpdateCheck.result.collectAsState()
    val found = foundUpdate(check, update)
    val known by rememberSetting("known_cars") { knownCars }
    val pins = rememberPins()
    val wireless by rememberSetting("wireless") { wireless }
    val saved by rememberSetting("myplay_backup_last_saved") { myplayBackupLastSaved }
    val video by rememberSetting("max_video_height", "prefer_60fps", "force_h264", "constant_bitrate_video") { videoChanged(this) }
    val preview by rememberSetting("myplay_engine_preview", "myplay_orbit_default_done") { myplayEnginePreview }
    val language by rememberSetting(KEY_ENGINE_LANGUAGE) { myplayEngineLanguage }
    val design = designInUse(preview, language)
    val carCount = carEntries(known, pins) { headUnitName(ctx, it) }.size
    Page("more") {
        item {
            NextGroup(listOf(
                {
                    NextValueRow(stringResource(R.string.next_more_updates_label), icon = Icons.Outlined.SystemUpdate,
                        value = if (found == null) stringResource(R.string.next_upd_up_to_date_label) else null,
                        tag = found?.let { { NextTag(stringResource(R.string.next_more_ready_label, it.version), TagKind.CAUTION) } }) { nav.open("more/updates") }
                },
                {
                    NextValueRow(stringResource(R.string.next_more_cars_label), pluralCars(carCount), Icons.Outlined.DirectionsCar) { nav.open("more/cars") }
                },
                {
                    NextValueRow(stringResource(R.string.next_more_connection_label),
                        stringResource(if (wireless) R.string.next_more_wireless_on_label else R.string.next_more_wireless_off_label),
                        Icons.Outlined.Wifi) { nav.open("more/connection") }
                },
                {
                    NextValueRow(stringResource(R.string.next_more_backup_label),
                        lastSavedText(saved)?.let { stringResource(R.string.next_more_saved_label, it) } ?: stringResource(R.string.next_more_never_label),
                        Icons.Outlined.History) { nav.open("more/backup") }
                },
                { NextValueRow(stringResource(R.string.next_more_diagnostics_label), icon = Icons.Outlined.MonitorHeart) { nav.open("more/diagnostics") } },
            ))
            Spacer(Modifier.height(12.dp))
        }
        item {
            NextGroup(listOf(
                {
                    NextValueRow(stringResource(R.string.next_more_advanced_label),
                        stringResource(if (video) R.string.next_more_changed_label else R.string.next_more_normal_label), Icons.Outlined.Tune, changed = video) {
                        nav.open("more/advanced")
                    }
                },
                {
                    NextValueRow(stringResource(R.string.next_more_archive_label), stringResource(when (design) {
                        DesignInUse.ORBIT -> R.string.next_more_not_used_label
                        DesignInUse.DECK -> R.string.next_att_deck_label
                        DesignInUse.OLDER -> R.string.next_att_older_label
                    }), Icons.Outlined.Archive) { nav.open("more/archive") }
                },
                { NextValueRow(stringResource(R.string.next_more_about_label), BuildConfig.VERSION_NAME, Icons.Outlined.Info) { nav.open("more/about") } },
            ))
        }
    }
}

/** Whether any of the video settings differs from its default (the Advanced row's value). */
internal fun videoChanged(s: Settings): Boolean = runCatching {
    val p = s.prefs
    p.contains("max_video_height") || p.getBoolean("prefer_60fps", false) || p.getBoolean("force_h264", false) || p.getBoolean("constant_bitrate_video", false)
}.getOrDefault(false)

@Composable
private fun pluralCars(n: Int): String = LocalContext.current.resources.getQuantityString(R.plurals.next_more_cars_count_label, n, n)

// ---------------------------------------------------------------- Cars

/** A car in the one list (plan §3.2): a known car, its trusted certificates, or both when the names match. */
data class CarEntry(val id: String, val name: String, val lastMs: Long?, val pinKey: String?, val fingerprints: Set<String>)

/**
 * The one list of cars: today's two stores (known_cars by name and time; head_unit_pins by key) merged
 * where a trusted key's name is a known car's name, newest first, trusted-only ones after.
 */
fun carEntries(known: Map<String, Long>, pins: Map<String, Set<String>>, labelOf: (String) -> String): List<CarEntry> {
    val byLabel = pins.keys.associateBy(labelOf)
    val cars = known.entries.sortedByDescending { it.value }.map { (name, time) ->
        val key = byLabel[name]
        CarEntry("car:" + Uri.encode(name), name, time, key, key?.let { pins[it] }.orEmpty())
    }
    val used = cars.mapNotNull { it.pinKey }.toSet()
    val rest = pins.keys.filter { it !in used }.sorted().map { key -> CarEntry("pin:" + Uri.encode(key), labelOf(key), null, key, pins[key].orEmpty()) }
    return cars + rest
}

/** Cars forgotten but still in their undo window: hidden from the lists until the time is up. */
internal object PendingForget {
    val ids = MutableStateFlow<Set<String>>(emptySet())
}

@Composable
private fun rememberPins(): Map<String, Set<String>> {
    val ctx = LocalContext.current
    val pinned = remember { PinnedHeadUnits(ctx) }
    var pins by remember { mutableStateOf(runCatching { pinned.all }.getOrDefault(emptyMap())) }
    // A car trusted from the notification while the page is open shows at once.
    androidx.compose.runtime.DisposableEffect(pinned) { onDispose(pinned.listen { pins = runCatching { pinned.all }.getOrDefault(emptyMap()) }) }
    return pins
}

/** A trusted key as the owner knows it: the paired car's name, the USB accessory, or the developer server. */
@android.annotation.SuppressLint("MissingPermission")
internal fun headUnitName(ctx: android.content.Context, key: String): String = when {
    key == "tcp" -> ctx.getString(R.string.huserver_label)
    key.startsWith("usb:") -> key.removePrefix("usb:").replace('|', ' ').trim().ifBlank { key }
    else -> runCatching { ctx.getSystemService(android.bluetooth.BluetoothManager::class.java)?.adapter?.getRemoteDevice(key)?.name }.getOrNull() ?: key
}

@Composable
private fun linkOfKey(key: String?): String? = when {
    key == null -> null
    key == "tcp" -> stringResource(R.string.next_more_wifi_label)
    key.startsWith("usb:") -> stringResource(R.string.next_home_usb_label)
    else -> stringResource(R.string.next_home_wireless_label)
}

@Composable
private fun whenWords(ms: Long?): String? = ms?.let { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(it) }

/** More › Cars (frames 49, 51, 52): one list; Forget is on a car's page. */
@Composable
fun NextCarsPage() {
    val ctx = LocalContext.current
    val nav = LocalNextNavigator.current
    val known by rememberSetting("known_cars") { knownCars }
    val pins = rememberPins()
    val pending by PendingForget.ids.collectAsState()
    val cars = carEntries(known, pins) { headUnitName(ctx, it) }.filter { it.id !in pending }
    Page("more/cars") {
        if (cars.isEmpty()) item { NextEmpty(stringResource(R.string.next_more_no_cars_help)) }
        else item {
            NextCard {
                cars.forEachIndexed { i, car ->
                    if (i > 0) NextDivider()
                    NextRow(
                        car.name, icon = Icons.Outlined.DirectionsCar,
                        note = listOfNotNull(whenWords(car.lastMs), linkOfKey(car.pinKey)).joinToString(" · ").takeIf { it.isNotEmpty() }?.let { RowNote.Help(it) },
                        tag = if (car.fingerprints.isNotEmpty()) ({ NextTag(stringResource(R.string.next_more_trusted_label), TagKind.OK) }) else null,
                        onClick = { nav.open(NextRoutes.car(car.id)) }, role = androidx.compose.ui.semantics.Role.Button,
                    ) {
                        androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = NextColors.Text3)
                    }
                }
            }
        }
    }
}

/** More › Cars › a car (frame 50): its trust, the per-car slot, and Forget with undo. */
@Composable
fun NextCarPage(id: String) {
    val ctx = LocalContext.current
    val nav = LocalNextNavigator.current
    val undo = LocalNextUndo.current
    val settings = remember { Settings(ctx) }
    val known by rememberSetting("known_cars") { knownCars }
    val pins = rememberPins()
    val car = carEntries(known, pins) { headUnitName(ctx, it) }.firstOrNull { it.id == id }
    val forgotten = stringResource(R.string.next_more_car_forgotten_label)
    Page(NextRoutes.car(id)) {
        if (car == null) { item { NextEmpty(stringResource(R.string.next_more_no_cars_help)) }; return@Page }
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                NextHeroRing(RingStyle.FULL, NextTab.MORE.world, Icons.Outlined.DirectionsCar, size = 140.dp)
                Text(car.name, style = NextType.Hero, color = NextColors.Text, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
                listOfNotNull(whenWords(car.lastMs)?.let { stringResource(R.string.next_more_last_connected_label, it) }, linkOfKey(car.pinKey))
                    .joinToString(" · ").takeIf { it.isNotEmpty() }?.let {
                        Text(it, style = NextType.Value, color = NextColors.Text2, modifier = Modifier.padding(top = 4.dp))
                    }
                Spacer(Modifier.height(16.dp))
            }
        }
        item {
            NextCard {
                if (car.fingerprints.isNotEmpty()) {
                    val fp = car.fingerprints.first().take(16).uppercase().chunked(4).joinToString(" ")
                    NextRow(stringResource(R.string.next_more_trusted_label), icon = Icons.Outlined.GppGood,
                        note = RowNote.Help(stringResource(R.string.next_home_fingerprint_label, fp)),
                        info = stringResource(R.string.next_more_trusted_info))
                } else {
                    NextRow(stringResource(R.string.next_more_not_trusted_label), icon = Icons.Outlined.GppGood,
                        note = RowNote.Help(stringResource(R.string.next_more_asks_help)))
                }
                NextDivider()
                // Slot (plan §8): per-car fixes, later.
                NextRow(stringResource(R.string.next_more_car_fixes_label), icon = Icons.Outlined.Build,
                    tag = { NextTag(stringResource(R.string.next_scr_coming_label)) }, enabled = false)
            }
            Spacer(Modifier.height(16.dp))
        }
        item {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                NextButton(stringResource(R.string.next_more_forget_car_label), {
                    // Hidden at once; forgotten for good only when the undo time is up (plan §10).
                    PendingForget.ids.value = PendingForget.ids.value + car.id
                    nav.back()
                    undo.show(forgotten, undo = { PendingForget.ids.value = PendingForget.ids.value - car.id }) {
                        PendingForget.ids.value = PendingForget.ids.value - car.id
                        if (car.lastMs != null) settings.forgetCar(car.name)
                        car.pinKey?.let { PinnedHeadUnits(ctx).forget(it) }
                    }
                }, kind = ButtonKind.DANGER, icon = Icons.Rounded.Close)
            }
        }
    }
}

// ---------------------------------------------------------------- Connection

/** More › Connection (frame 53): only how the phone reaches the car. */
@Composable
fun NextConnectionPage() {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    val wireless by rememberSetting("wireless") { wireless }
    val address by rememberSetting("phone_bt_address") { phoneBluetoothAddress }
    var editing by rememberSaveable { mutableStateOf(false) }
    Page("more/connection") {
        item {
            NextGroup(listOf(
                {
                    NextSwitchRow(stringResource(R.string.next_more_wireless_label), wireless, {
                        settings.wireless = it
                        if (it) ProjectionService.probeWireless(ctx)
                    }, Icons.Outlined.Wifi)
                },
                {
                    NextValueRow(stringResource(R.string.next_more_bt_label), address.ifBlank { stringResource(R.string.next_scr_auto_label) },
                        Icons.Outlined.Bluetooth, changed = address.isNotBlank(),
                        note = RowNote.Help(stringResource(R.string.next_more_bt_help))) { editing = true }
                },
            ))
        }
    }
    if (editing) NextTextDialog(
        stringResource(R.string.next_more_bt_label), address, stringResource(R.string.next_more_bt_field_label),
        stringResource(R.string.next_looks_save_button_label), onDismiss = { editing = false },
        sub = stringResource(R.string.next_more_bt_dialog_help), maxLength = 17,
        error = { t -> if (t.isBlank() || Regex("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}").matches(t.trim())) null else ctx.getString(R.string.next_more_bt_error_help) },
    ) { t -> editing = false; settings.phoneBluetoothAddress = t.trim() }
}

// ---------------------------------------------------------------- Backup

/** What loading a backup left to set again on this phone (plan §10, frame 56). */
data class AfterLoad(val skipped: Int, val photo: Boolean, val region: String?)

/** More › Backup (frames 54–56): save, load after a confirm, then what to set again here. */
@Composable
fun NextBackupPage() {
    val ctx = LocalContext.current
    val nav = LocalNextNavigator.current
    val settings = remember { Settings(ctx) }
    val scope = rememberCoroutineScope()
    val saved by rememberSetting("myplay_backup_last_saved") { myplayBackupLastSaved }
    var pendingImport by rememberSaveable { mutableStateOf<Uri?>(null) }
    var loaded by rememberSaveable { mutableStateOf<String?>(null) } // AfterLoad as "skipped|photo|region"
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val json = SettingsBackup.export(settings.all).toString(2)
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { ctx.contentResolver.openOutputStream(uri)!!.use { it.write(json.toByteArray()) } }
                    .onFailure { DiagLog.add("settings export failed: ${it.javaClass.simpleName}") }.isSuccess
            }
            if (ok) settings.myplayBackupLastSaved = System.currentTimeMillis()
            Toast.makeText(ctx, if (ok) R.string.phone_backup_saved else R.string.setup_backup_export_failed, Toast.LENGTH_LONG).show()
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) pendingImport = uri }
    val after = loaded?.split('|')?.let { AfterLoad(it[0].toIntOrNull() ?: 0, it.getOrNull(1) == "1", it.getOrNull(2)?.ifEmpty { null }) }
    Page("more/backup") {
        if (after != null) {
            item {
                NextCard(highlight = true) {
                    NextRow(stringResource(R.string.next_more_loaded_label), icon = Icons.Rounded.Check, disc = DiscTone.WORLD,
                        note = if (after.skipped > 0) RowNote.Help(stringResource(R.string.next_more_left_out_help, after.skipped)) else null)
                }
            }
            item { NextSection(stringResource(R.string.next_more_set_again_label)) }
            item {
                val rows = mutableListOf<@Composable () -> Unit>()
                if (after.photo) rows += { NextValueRow(stringResource(R.string.next_more_photo_label), stringResource(R.string.next_more_choose_label), Icons.Outlined.Image) { nav.open("screen/wallpaper") } }
                after.region?.let { r ->
                    rows += {
                        NextValueRow(stringResource(R.string.next_speed_not_downloaded_help, SpeedLimitFiles.REGIONS.firstOrNull { it.id == r }?.name ?: r),
                            icon = Icons.Outlined.Speed) { nav.open("car/speed") }
                    }
                }
                rows += { NextValueRow(stringResource(R.string.next_more_certificates_label), stringResource(R.string.next_more_import_label), Icons.Outlined.VerifiedUser) { nav.open("more/advanced/certificates") } }
                rows += { NextRow(stringResource(R.string.next_more_trusted_cars_label), icon = Icons.Outlined.DirectionsCar, note = RowNote.Help(stringResource(R.string.next_more_asks_help))) }
                NextGroup(rows)
                Spacer(Modifier.height(16.dp))
            }
        }
        item {
            NextGroup(listOf(
                {
                    NextRow(stringResource(R.string.next_more_save_label), icon = Icons.Outlined.Download,
                        note = RowNote.Help(lastSavedText(saved)?.let { stringResource(R.string.next_more_last_saved_help, it) } ?: stringResource(R.string.next_more_never_saved_help))) {
                        NextButton(stringResource(R.string.next_looks_save_button_label), { export.launch(backupFileName()) }, small = true)
                    }
                },
                {
                    NextRow(stringResource(R.string.next_more_load_label), icon = Icons.Outlined.UploadFile,
                        note = RowNote.Help(stringResource(R.string.next_more_load_help))) {
                        NextButton(stringResource(R.string.next_more_load_button_label), { import.launch(arrayOf("application/json", "text/plain", "*/*")) }, kind = ButtonKind.GHOST, small = true)
                    }
                },
            ))
            NextNote(stringResource(R.string.next_more_not_in_file_help), Icons.Outlined.Info)
        }
    }
    pendingImport?.let { uri ->
        // Loading can't be undone, so it asks (plan §10).
        NextConfirm(
            stringResource(R.string.next_more_replace_label), stringResource(R.string.next_more_replace_info),
            stringResource(R.string.next_more_replace_button_label),
            onConfirm = {
                pendingImport = null
                scope.launch {
                    val imported = withContext(Dispatchers.IO) {
                        // On failure only the kind is logged: a JSON error's message quotes the whole file.
                        runCatching {
                            val bytes = ctx.contentResolver.openInputStream(uri)!!.use { SettingsBackup.readLimited(it) }
                            SettingsBackup.import(org.json.JSONObject(bytes.toString(Charsets.UTF_8)))
                        }.onFailure { DiagLog.add("settings import failed: ${it.javaClass.simpleName}") }.getOrNull()
                    }
                    if (imported == null) {
                        Toast.makeText(ctx, R.string.setup_backup_import_failed, Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    settings.restore(imported.values)
                    if (imported.skipped.isNotEmpty()) DiagLog.add("settings import skipped ${imported.skipped.size}: ${imported.skipped.joinToString(limit = 10)}")
                    // What a file can't carry: the photo, the speed file, certificates, trusted cars.
                    val photo = settings.wallpaper == Settings.WALLPAPER_CUSTOM && !java.io.File(ctx.filesDir, Settings.WALLPAPER_FILE).isFile
                    val region = settings.myplaySpeedLimitRegion.takeIf { it.isNotEmpty() && SpeedLimitDownload.kept(ctx, it) == null }
                    loaded = "${imported.skipped.size}|${if (photo) 1 else 0}|${region.orEmpty()}"
                }
            },
            onDismiss = { pendingImport = null },
        )
    }
}


/** A car page's title: the car's name from its id ("car:<name>" or "pin:<key>"), or null for other routes. */
internal fun carTitle(ctx: android.content.Context, route: String): String? {
    if (!route.startsWith(NextRoutes.CAR_PREFIX)) return null
    val id = route.removePrefix(NextRoutes.CAR_PREFIX)
    return when {
        id.startsWith("car:") -> Uri.decode(id.removePrefix("car:"))
        id.startsWith("pin:") -> headUnitName(ctx, Uri.decode(id.removePrefix("pin:")))
        else -> id
    }
}
