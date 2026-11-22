package net.prototux.aare.ui.phone.next

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.audio.CaptureMethod
import net.prototux.aare.audio.CaptureMethods
import net.prototux.aare.car.DrivingMode
import net.prototux.aare.car.KEY_SPEED_LIMIT_MARGIN
import net.prototux.aare.car.PhoneLocationFeed
import net.prototux.aare.car.SpeedRules
import net.prototux.aare.car.SpeedSourceMode
import net.prototux.aare.car.myplaySpeedLimitMargin
import net.prototux.aare.car.myplaySpeedSource
import net.prototux.aare.car.myplayDriveWarnings
import net.prototux.aare.host.Assistants
import net.prototux.aare.host.CarAppCatalog
import net.prototux.aare.service.ProjectionService
import net.prototux.aare.ui.car.HomeMaps
import net.prototux.aare.ui.car.myplay.KEY_MAPS_IDLE_HOME
import net.prototux.aare.ui.car.myplay.MapsIdleHome
import net.prototux.aare.ui.car.myplay.myplayMapsIdleHome
import net.prototux.aare.ui.car.myplayHomeMaps
import net.prototux.aare.ui.phone.KEY_SPEED_LIMIT_MOBILE
import net.prototux.aare.ui.phone.KEY_SPEED_LIMIT_REGION
import net.prototux.aare.host.MyPlayAppList
import net.prototux.aare.ui.phone.SPEED_SOURCE_NAMES
import net.prototux.aare.ui.phone.SpeedLimitDownload
import net.prototux.aare.ui.phone.SpeedLimitFiles
import net.prototux.aare.ui.phone.homeMapsOptions
import net.prototux.aare.ui.phone.homeMapsUnavailable
import net.prototux.aare.ui.phone.myplaySpeedLimitMobile
import net.prototux.aare.ui.phone.myplaySpeedLimitRegion
import net.prototux.aare.ui.phone.speedLimitStatus
import net.prototux.aare.ui.theme.myplayMapFilter
import rikka.shizuku.Shizuku

@Composable
private fun Page(route: String, content: LazyListScope.() -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().testTag("next-page-$route"),
        state = rememberNextListState(),
        contentPadding = PaddingValues(start = NextSpace.Gutter, end = NextSpace.Gutter, bottom = 24.dp),
        content = content,
    )
}

private fun granted(ctx: android.content.Context, p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

// ---------------------------------------------------------------- Maps

/** Car › Maps (frames 29, 30). */
@Composable
fun NextMapsPage() {
    val ctx = LocalContext.current
    val nav = LocalNextNavigator.current
    val settings = remember { Settings(ctx) }
    val homeMaps by rememberSetting("myplay_home_maps") { myplayHomeMaps }
    val idle by rememberSetting(KEY_MAPS_IDLE_HOME) { myplayMapsIdleHome }
    val follow by rememberSetting("myplay_maps_follow_car_lights") { mapsFollowCarLights }
    val carGps by rememberSetting("car_gps") { carGpsForPhone }
    val mapKind by rememberSetting("myplay_map_filter") { mapColoursKind(myplayMapFilter) }
    val resumed = rememberResumeTicks()
    val mockAllowed = remember(resumed, carGps) { PhoneLocationFeed.allowed(ctx) }
    // The maps apps the car can host, by the car's own rules (as today's page).
    val apps by produceState<List<Pair<String, String>>?>(null) {
        val hidden = settings.hiddenApps
        value = withContext(Dispatchers.IO) {
            runCatching {
                CarAppCatalog.query(ctx).filter { it.isNavigation && MyPlayAppList.canShow(it.packageName) && it.packageName !in hidden }
                    .map { it.packageName to it.label }.distinctBy { it.first }.sortedBy { it.second.lowercase() }
            }.getOrDefault(emptyList())
        }
    }
    val last = stringResource(R.string.next_car_last_used_label)
    val ask = stringResource(R.string.next_car_always_ask_label)
    Page("car/maps") {
        item {
            NextGroup(listOf(
                {
                    NextPickerRow(
                        stringResource(R.string.next_maps_on_home_label),
                        homeMapsOptions(homeMaps, apps.orEmpty()).map { (value, label) ->
                            Choice(value, label ?: when (value) {
                                HomeMaps.LAST -> last
                                HomeMaps.ASK -> ask
                                else -> value.removePrefix(HomeMaps.APP_PREFIX)
                            })
                        },
                        homeMaps, { settings.myplayHomeMaps = it }, default = HomeMaps.LAST, icon = Icons.Outlined.Home,
                        tag = { AlsoOnCarTag() },
                        note = if (apps != null && homeMapsUnavailable(homeMaps, apps.orEmpty())) RowNote.Warn(stringResource(R.string.next_maps_unavailable_help)) else null,
                    )
                },
                {
                    NextPickerRow(
                        stringResource(R.string.next_maps_idle_label),
                        MapsIdleHome.CHOICES.map { m ->
                            Choice(m, if (m == MapsIdleHome.OFF) stringResource(R.string.next_car_off_label) else stringResource(R.string.next_maps_minutes_label, m))
                        },
                        idle, { settings.myplayMapsIdleHome = it }, default = MapsIdleHome.OFF, icon = Icons.Outlined.History,
                        tag = { AlsoOnCarTag() },
                    )
                },
                { NextSwitchRow(stringResource(R.string.next_maps_follow_label), follow, { settings.mapsFollowCarLights = it }, Icons.Outlined.LightMode) },
                {
                    NextSwitchRow(stringResource(R.string.next_maps_car_gps_label), carGps, { settings.carGpsForPhone = it }, Icons.Outlined.GpsFixed,
                        note = RowNote.Help(stringResource(R.string.next_maps_car_gps_help)))
                },
            ))
        }
        if (carGps && !mockAllowed) item {
            Spacer(Modifier.height(12.dp))
            NextCard(warn = true) {
                NextRow(stringResource(R.string.next_maps_mock_label), icon = Icons.Outlined.WarningAmber, disc = DiscTone.WARN,
                    note = RowNote.Help(stringResource(R.string.next_maps_mock_help)),
                    onClick = { ctx.startSafely(Intent(AndroidSettings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }, role = Role.Button)
            }
        }
        item {
            Spacer(Modifier.height(12.dp))
            // A link to the other tab (plan §3.4 rule 2): Map colours lives in Screen, because looks hold it.
            NextCard {
                NextLinkRow(stringResource(R.string.next_screen_map_colours_label), mapWord(mapKind), Icons.Outlined.Palette) { nav.open("screen/map-colours") }
            }
        }
    }
}

@Composable
private fun mapWord(kind: String): String = when (kind) {
    "none" -> stringResource(R.string.next_scr_none_label)
    "custom" -> stringResource(R.string.next_scr_custom_label)
    else -> MapPresetWords[kind]?.let { stringResource(it) } ?: kind
}

private val MapPresetWords = mapOf(
    "muted" to R.string.phone_map_preset_muted, "cool" to R.string.phone_map_preset_cool, "warm" to R.string.phone_map_preset_warm,
    "dark_night" to R.string.phone_map_preset_dark_night, "grey" to R.string.phone_map_preset_grey,
)

// ---------------------------------------------------------------- Speed

/** Car › Speed (frames 31–33): source, margin, region and its file, mobile data, the Waze slot. */
@Composable
fun NextSpeedPage() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { Settings(ctx) }
    val source by rememberSetting("myplay_speed_source") { myplaySpeedSource }
    val margin by rememberSetting(KEY_SPEED_LIMIT_MARGIN) { myplaySpeedLimitMargin }
    val region by rememberSetting(KEY_SPEED_LIMIT_REGION) { myplaySpeedLimitRegion }
    val mobile by rememberSetting(KEY_SPEED_LIMIT_MOBILE) { myplaySpeedLimitMobile }
    val warnings by rememberSetting(net.prototux.aare.car.KEY_DRIVE_WARNINGS) { myplayDriveWarnings }
    val state by SpeedLimitDownload.state.collectAsState()
    var changes by remember { mutableIntStateOf(0) }
    val kept = remember(region, state, changes) { if (region.isEmpty()) null else SpeedLimitDownload.kept(ctx, region) }
    var askMobile by rememberSaveable { mutableStateOf<Long?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { SpeedLimitDownload.checkIfDue(ctx) }
    val regionName = SpeedLimitFiles.REGIONS.firstOrNull { it.id == region }?.name
    Page("car/speed") {
        item {
            NextSection(stringResource(R.string.next_speed_source_label), first = true, extra = { AlsoOnCarTag() })
            NextSegmented(SPEED_SOURCE_NAMES.map { (m, res) -> Choice(m, stringResource(res)) }, source, { settings.myplaySpeedSource = it })
            NextNote(stringResource(when (source) {
                SpeedSourceMode.AUTO -> R.string.next_speed_auto_help
                SpeedSourceMode.CAR -> R.string.next_speed_car_help
                SpeedSourceMode.OFF -> R.string.next_speed_off_help
            }), Icons.Outlined.Info)
            Spacer(Modifier.height(12.dp))
        }
        item {
            NextGroup(listOf(
                {
                    NextPickerRow(
                        stringResource(R.string.next_speed_warn_label),
                        SpeedRules.MARGINS.map { m ->
                            Choice(m, if (m == SpeedRules.MARGIN_OFF) stringResource(R.string.next_car_off_label) else stringResource(R.string.next_speed_kmh_label, m))
                        },
                        margin, { settings.myplaySpeedLimitMargin = it }, default = SpeedRules.MARGIN_DEFAULT, icon = Icons.Outlined.WarningAmber,
                    )
                },
                {
                    NextPickerRow(
                        stringResource(R.string.next_speed_region_label),
                        listOf(Choice("", stringResource(R.string.next_scr_none_label))) + SpeedLimitFiles.REGIONS.map { Choice(it.id, it.name) },
                        region, { id ->
                            settings.myplaySpeedLimitRegion = id
                            // Nothing is deleted here (#5): another region's file goes only once the new one is kept.
                            if (id.isNotEmpty()) scope.launch { SpeedLimitDownload.update(ctx, force = true) }
                        }, default = "", icon = Icons.Outlined.Place,
                    )
                },
            ))
            Spacer(Modifier.height(12.dp))
        }
        if (region.isNotEmpty()) item {
            NextCard {
                val downloading = state as? SpeedLimitDownload.State.Downloading
                if (downloading != null) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center) {
                            NextHeroRing(RingStyle.PROGRESS, NextColors.Int, null, null, (downloading.percent ?: 0) / 100f, size = 56.dp)
                            Text("${downloading.percent ?: 0}%", style = NextType.Tag, color = NextColors.Text)
                        }
                        Spacer(Modifier.size(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.next_speed_downloading_label), style = NextType.Label, color = NextColors.Text)
                            Text(regionName ?: region, style = NextType.Help, color = NextColors.Text3)
                        }
                    }
                } else {
                    NextRow(
                        stringResource(R.string.next_speed_file_label), icon = Icons.Outlined.Description,
                        note = RowNote.Help(if (kept == null) stringResource(R.string.next_speed_not_downloaded_help, regionName ?: region)
                            else stringResource(R.string.next_speed_kept_help, Formatter.formatShortFileSize(ctx, kept.bytes), kept.built)),
                    )
                    speedLimitStatus(state)?.let { res ->
                        val failed = state == SpeedLimitDownload.State.Failed || state == SpeedLimitDownload.State.NoInternet
                        Text(stringResource(res), style = NextType.Help, color = if (failed) NextColors.Warn else NextColors.Text2,
                            modifier = Modifier.padding(start = 70.dp, end = 16.dp, bottom = 8.dp))
                    }
                    Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NextButton(
                            stringResource(if (kept == null) R.string.next_speed_download_label else R.string.next_speed_newer_label),
                            { scope.launch { SpeedLimitDownload.update(ctx, force = true) } }, kind = ButtonKind.GHOST, small = true,
                        )
                        if (kept != null) NextButton(stringResource(R.string.next_speed_delete_label), { confirmDelete = region }, kind = ButtonKind.DANGER, small = true)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            NextCard {
                NextSwitchRow(stringResource(R.string.next_speed_mobile_label), mobile, { on ->
                    if (!on) settings.myplaySpeedLimitMobile = false
                    else scope.launch { askMobile = SpeedLimitDownload.lookUp(region)?.bytes ?: -1L }
                }, Icons.Outlined.Wifi)
            }
        }
        if (region.isEmpty()) {
            // Files kept from before can still be deleted with no region chosen (SL-15).
            item {
                val leftOver = remember(changes) { SpeedLimitFiles.REGIONS.mapNotNull { r -> SpeedLimitDownload.kept(ctx, r.id)?.let { r to it } } }
                leftOver.forEach { (r, m) ->
                    NextCard {
                        NextRow(r.name, icon = Icons.Outlined.Description,
                            note = RowNote.Help(stringResource(R.string.next_speed_kept_help, Formatter.formatShortFileSize(ctx, m.bytes), m.built))) {
                            NextButton(stringResource(R.string.next_speed_delete_label), { confirmDelete = r.id }, kind = ButtonKind.DANGER, small = true)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
        item {
            // Ideas 2026-11-22: the ice sign and the hot-phone line on the car screen (on by default).
            NextCard {
                NextSwitchRow(stringResource(R.string.next_speed_warnings_label), warnings, { settings.myplayDriveWarnings = it },
                    Icons.Outlined.AcUnit, note = RowNote.Help(stringResource(R.string.next_speed_warnings_help)), tag = { AlsoOnCarTag() })
            }
            Spacer(Modifier.height(12.dp))
        }
        item {
            NextNote(stringResource(R.string.next_speed_offline_help), Icons.Outlined.Lock)
            Spacer(Modifier.height(12.dp))
            // Slot (plan §8): Limit from Waze, later.
            NextCard {
                NextRow(stringResource(R.string.next_speed_waze_label), note = RowNote.Help(stringResource(R.string.next_speed_waze_help)),
                    tag = { NextTag(stringResource(R.string.next_scr_coming_label)) }, enabled = false) { NextSwitch(false, enabled = false) }
            }
            NextNote(stringResource(R.string.phone_osm_notice))
        }
    }
    askMobile?.let { bytes ->
        NextConfirm(
            stringResource(R.string.next_speed_mobile_label),
            if (bytes > 0) stringResource(R.string.next_speed_mobile_size_info, Formatter.formatShortFileSize(ctx, bytes))
            else stringResource(R.string.next_speed_mobile_unknown_info),
            stringResource(R.string.next_upd_allow_button_label),
            onConfirm = { askMobile = null; settings.myplaySpeedLimitMobile = true }, onDismiss = { askMobile = null },
        )
    }
    confirmDelete?.let { id ->
        NextConfirm(
            stringResource(R.string.next_speed_delete_title_label), stringResource(R.string.next_speed_delete_info),
            stringResource(R.string.next_speed_delete_label),
            onConfirm = { confirmDelete = null; SpeedLimitDownload.delete(ctx, id, "the Delete button"); changes++ },
            onDismiss = { confirmDelete = null }, danger = true,
        )
    }
}

// ---------------------------------------------------------------- Sound

/** Car › Sound (frames 34, 35): the switch, whether sound reaches the car, the methods best first. */
@Composable
fun NextSoundPage() {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    val on by rememberSetting("car_audio") { carAudio }
    val resume by rememberSetting("resume_media") { resumeMedia }
    val state by ProjectionService.state.collectAsState()
    var recheck by remember { mutableIntStateOf(0) }
    val resumed = rememberResumeTicks()
    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == android.app.Activity.RESULT_OK && r.data != null) ProjectionService.giveAudioConsent(ctx, r.resultCode, r.data!!)
        recheck++
    }
    DisposableEffect(Unit) {
        val l = Shizuku.OnRequestPermissionResultListener { _, _ -> recheck++ }
        runCatching { Shizuku.addRequestPermissionResultListener(l) }
        onDispose { runCatching { Shizuku.removeRequestPermissionResultListener(l) } }
    }
    val methods = remember(recheck, resumed, state.audioCapture) { soundMethods(ctx, state.audioCapture) }
    val best = methods.best
    Page("car/sound") {
        item {
            NextCard {
                NextSwitchRow(stringResource(R.string.next_sound_switch_label), on, { settings.carAudio = it }, Icons.AutoMirrored.Outlined.VolumeUp)
            }
            Spacer(Modifier.height(12.dp))
        }
        item {
            // The words only (review "Second pass" item 2); the method names stay on the rows below.
            NextCard(warn = best == null) {
                NextRow(
                    stringResource(if (best != null) R.string.next_sound_ready_label else R.string.next_sound_none_label),
                    icon = if (best != null) Icons.Rounded.Check else Icons.Outlined.WarningAmber,
                    disc = if (best != null) DiscTone.WORLD else DiscTone.WARN,
                    note = RowNote.Help(stringResource(if (best != null) R.string.next_sound_ready_help else R.string.next_sound_none_help)),
                )
            }
        }
        item { NextSection(stringResource(R.string.next_sound_methods_label)) }
        item {
            NextCard {
                MethodRow(stringResource(R.string.next_sound_policy_label),
                    stringResource(if (methods.policy) R.string.next_sound_policy_ready_help else R.string.next_sound_policy_help),
                    inUse = best == CaptureMethod.AUDIO_POLICY, ready = methods.policy, action = R.string.next_car_set_up_label,
                    info = stringResource(R.string.next_sound_policy_info)) {
                    ctx.startSafely(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/WhiteRain3/MyPlay#car-audio")))
                }
                NextDivider(16.dp)
                MethodRow(stringResource(R.string.next_sound_shizuku_label),
                    stringResource(when {
                        methods.shizuku -> R.string.next_sound_shizuku_ready_help
                        methods.root -> R.string.next_sound_root_ready_help
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> R.string.next_sound_android12_help
                        methods.shizukuAsk -> R.string.next_sound_shizuku_allow_help
                        else -> R.string.next_sound_shizuku_help
                    }),
                    inUse = best == CaptureMethod.SHIZUKU_POLICY || best == CaptureMethod.SHIZUKU, ready = methods.shizuku || methods.root,
                    action = if (methods.shizukuAsk) R.string.next_upd_allow_button_label else R.string.next_car_set_up_label,
                    info = stringResource(R.string.next_sound_shizuku_info)) {
                    when {
                        methods.shizukuAsk -> runCatching { Shizuku.requestPermission(SHIZUKU_REQUEST) }
                        // Check for root (prompts su on a rooted phone); otherwise point to Shizuku.
                        else -> CaptureMethods.checkRoot { granted ->
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                if (granted) { settings.rootAudio = true; recheck++ }
                                else ctx.startSafely(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")))
                            }
                        }
                    }
                }
                NextDivider(16.dp)
                MethodRow(stringResource(R.string.next_sound_capture_label),
                    stringResource(if (state.audioCapture) R.string.next_sound_capture_ready_help else R.string.next_sound_capture_help),
                    inUse = best == CaptureMethod.PLAYBACK_CAPTURE, ready = state.audioCapture, action = R.string.next_car_set_up_label,
                    info = stringResource(R.string.next_sound_capture_info)) {
                    val mpm = ctx.getSystemService(MediaProjectionManager::class.java)
                    consent.launch(if (Build.VERSION.SDK_INT >= 34) mpm.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
                        else mpm.createScreenCaptureIntent())
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        item {
            NextCard {
                NextSwitchRow(stringResource(R.string.next_sound_resume_label), resume, { settings.resumeMedia = it }, Icons.Outlined.PlayCircle)
            }
        }
    }
}

private const val SHIZUKU_REQUEST = 42

/** Which capture methods this phone has now, and the best of them (today's order). */
internal class SoundMethods(val policy: Boolean, val shizuku: Boolean, val shizukuAsk: Boolean, val root: Boolean, val capture: Boolean) {
    val best: CaptureMethod? = when {
        policy -> CaptureMethod.AUDIO_POLICY
        shizuku -> CaptureMethod.SHIZUKU_POLICY
        root -> CaptureMethod.SHIZUKU
        capture -> CaptureMethod.PLAYBACK_CAPTURE
        else -> null
    }
}

internal fun soundMethods(ctx: android.content.Context, capture: Boolean): SoundMethods = runCatching {
    SoundMethods(CaptureMethods.audioPolicyAvailable(ctx), CaptureMethods.shizukuAvailable(), CaptureMethods.shizukuNeedsPermission(),
        CaptureMethods.rootAvailable(ctx), capture)
}.getOrDefault(SoundMethods(false, false, false, false, capture))

@Composable
private fun MethodRow(name: String, help: String, inUse: Boolean, ready: Boolean, action: Int, info: String, onSetUp: () -> Unit) {
    NextRow(name, note = RowNote.Help(help), info = info) {
        when {
            inUse -> NextTag(stringResource(R.string.next_scr_in_use_label), TagKind.INT)
            ready -> NextTag(stringResource(R.string.next_sound_ready_tag_label), TagKind.OK)
            else -> NextButton(stringResource(action), onSetUp, kind = if (action == R.string.next_upd_allow_button_label) ButtonKind.PRIMARY else ButtonKind.GHOST, small = true)
        }
    }
}

// ---------------------------------------------------------------- Messages and calls

private val CALL_PERMISSIONS = listOf(
    Manifest.permission.CALL_PHONE, Manifest.permission.ANSWER_PHONE_CALLS, Manifest.permission.READ_CALL_LOG,
    Manifest.permission.READ_CONTACTS, Manifest.permission.READ_PHONE_STATE,
)

/** Car › Messages and calls (frames 36, 37): permissions asked here, over only the rows that need them. */
@Composable
fun NextMessagesPage() {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    val resumed = rememberResumeTicks()
    var recheck by remember { mutableIntStateOf(0) }
    val access = remember(resumed, recheck) { NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName) }
    val calls = remember(resumed, recheck) { CALL_PERMISSIONS.all { granted(ctx, it) } }
    val dndAccess = remember(resumed, recheck) { runCatching { DrivingMode.accessGranted(ctx) }.getOrDefault(false) }
    val askCalls = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { recheck++ }
    val banners by rememberSetting("message_banners") { messageBanners }
    val groups by rememberSetting("group_messages") { groupMessages }
    val aloud by rememberSetting("read_messages") { readMessagesAloud }
    val dnd by rememberSetting("dnd_driving") { dndWhileDriving }
    val reply by rememberSetting("auto_reply") { autoReply }
    val speaker by rememberSetting("calls_on_phone_speaker") { callsOnPhoneSpeaker }
    var replyText by rememberSaveable { mutableStateOf(settings.autoReplyText) }
    Page("car/messages") {
        if (!access) item {
            PermissionCard(stringResource(R.string.next_msg_access_label), stringResource(R.string.next_msg_access_help), Icons.Outlined.NotificationsNone) {
                ctx.startSafely(Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            Spacer(Modifier.height(12.dp))
        }
        item {
            NextGroup(listOf(
                { NextSwitchRow(stringResource(R.string.next_msg_banners_label), banners, { settings.messageBanners = it }, Icons.Outlined.Chat, tag = { AlsoOnCarTag() }, enabled = access) },
                { NextSwitchRow(stringResource(R.string.next_msg_groups_label), groups, { settings.groupMessages = it }, Icons.Outlined.People, enabled = access) },
                { NextSwitchRow(stringResource(R.string.next_msg_aloud_label), aloud, { settings.readMessagesAloud = it }, Icons.AutoMirrored.Outlined.VolumeUp, tag = { AlsoOnCarTag() }, enabled = access) },
            ))
        }
        item { NextSection(stringResource(R.string.next_msg_driving_label)) }
        item {
            NextCard {
                NextSwitchRow(stringResource(R.string.next_msg_dnd_label), dnd, { settings.dndWhileDriving = it }, Icons.Outlined.DarkMode)
                if (dnd && !dndAccess) {
                    NextDivider()
                    NextValueRow(stringResource(R.string.next_msg_dnd_access_label), note = RowNote.Help(stringResource(R.string.next_msg_dnd_access_help))) {
                        ctx.startSafely(Intent(AndroidSettings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                    }
                }
                NextDivider()
                NextSwitchRow(stringResource(R.string.next_msg_reply_label), reply, { settings.autoReply = it }, Icons.Outlined.Description, enabled = access)
                if (reply) {
                    androidx.compose.material3.OutlinedTextField(
                        replyText, { replyText = it; settings.autoReplyText = it }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        label = { Text(stringResource(R.string.next_msg_reply_text_label)) },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NextColors.Int, unfocusedBorderColor = NextColors.Outline,
                            focusedTextColor = NextColors.Text, unfocusedTextColor = NextColors.Text,
                            focusedLabelColor = NextColors.Int, unfocusedLabelColor = NextColors.Text2, cursorColor = NextColors.Int,
                        ),
                    )
                }
            }
        }
        item { NextSection(stringResource(R.string.next_msg_calls_label)) }
        if (!calls) item {
            PermissionCard(stringResource(R.string.next_msg_calls_access_label), stringResource(R.string.next_msg_calls_access_help), Icons.Outlined.Contacts) {
                askCalls.launch(CALL_PERMISSIONS.toTypedArray())
            }
            Spacer(Modifier.height(12.dp))
        }
        item {
            NextCard {
                NextSwitchRow(stringResource(R.string.next_msg_speaker_label), speaker, { settings.callsOnPhoneSpeaker = it }, Icons.Outlined.Phone,
                    note = RowNote.Help(stringResource(R.string.next_msg_speaker_help)), tag = { NextConnectionTag() }, enabled = calls)
            }
        }
    }
}

/** A permission asked in context (plan §10): one card with one action, over the rows that need it. */
@Composable
private fun PermissionCard(title: String, help: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onAllow: () -> Unit) {
    NextCard(highlight = true) {
        NextRow(title, icon = icon, note = RowNote.Help(help))
        Box(Modifier.padding(start = 70.dp, bottom = 14.dp)) {
            NextButton(stringResource(R.string.next_upd_allow_button_label), onAllow, small = true)
        }
    }
}

// ---------------------------------------------------------------- Voice

/** Car › Voice assistant (frame 38): the phone's own, or a chosen app; each says where it opens. */
@Composable
fun NextVoicePage() {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    val chosen by rememberSetting("assistant_pkg") { assistantPackage }
    val choices by produceState(emptyList<net.prototux.aare.host.AssistantApp>()) {
        value = withContext(Dispatchers.IO) { runCatching { Assistants.query(ctx, CarAppCatalog.query(ctx)) }.getOrDefault(emptyList()) }
    }
    val phoneDefault = remember { runCatching { Assistants.phoneDefault(ctx) }.getOrNull() }
    val defaultLabel = phoneDefault?.let { pkg -> choices.firstOrNull { it.packageName == pkg }?.label ?: Assistants.label(ctx, pkg) ?: pkg }
    Page("car/voice") {
        item {
            NextCard {
                NextRow(
                    stringResource(R.string.next_car_same_as_phone_label), note = RowNote.Help(defaultLabel ?: stringResource(R.string.next_voice_no_default_help)),
                    onClick = { settings.assistantPackage = null }, role = Role.RadioButton,
                ) { if (chosen == null) androidx.compose.material3.Icon(Icons.Rounded.Check, null, tint = NextColors.Int) }
                choices.forEach { app ->
                    NextDivider()
                    NextRow(
                        app.label,
                        lead = {
                            app.icon?.let { Image(it.asImageBitmap(), null, Modifier.size(NextSpace.IconDisc).clip(CircleShape)) }
                                ?: NextIconDisc(Icons.Rounded.Mic)
                        },
                        note = RowNote.Help(stringResource(if (app.hasCarApp) R.string.next_voice_car_help else R.string.next_voice_phone_help)),
                        onClick = { settings.assistantPackage = app.packageName }, role = Role.RadioButton,
                    ) { if (chosen == app.packageName) androidx.compose.material3.Icon(Icons.Rounded.Check, null, tint = NextColors.Int) }
                }
            }
        }
    }
}

/** Counts the times the app came back to the front, so checks of the phone's state run again. */
@Composable
internal fun rememberResumeTicks(): Int {
    val lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var count by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, e -> if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) count++ }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    return count
}
