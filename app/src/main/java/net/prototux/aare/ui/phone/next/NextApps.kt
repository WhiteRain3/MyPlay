package net.prototux.aare.ui.phone.next

import android.content.Intent
import android.graphics.Bitmap
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.car.MirroredKeys
import net.prototux.aare.host.AndroidAutoApps
import net.prototux.aare.host.CarAppCatalog
import net.prototux.aare.host.CarAppInfo
import net.prototux.aare.host.CarRuntime
import net.prototux.aare.host.MyPlayAppList
import net.prototux.aare.host.PhoneApp
import net.prototux.aare.host.UnsupportedCarApp
import net.prototux.aare.launch.AppLauncher
import net.prototux.aare.car.myplayMirrorTurns
import net.prototux.aare.ui.phone.withMirrorTurn
import net.prototux.aare.ui.phone.withYouTubeApp
import net.prototux.aare.ui.phone.swappedVideoApp
import net.prototux.aare.host.myplayVideoApp
import net.prototux.aare.ui.phone.youTubeAppOnCar

/** Everything the car's launcher lists, as it groups it (the same rules as CarRuntime.applyApps). */
private class Lists(val carApps: List<CarAppInfo>, val media: List<PhoneApp>, val messaging: List<PhoneApp>)

private suspend fun loadLists(ctx: android.content.Context): Lists = withContext(Dispatchers.IO) {
    runCatching {
        val car = CarAppCatalog.query(ctx)
        val carPkgs = car.mapTo(HashSet()) { it.packageName }
        val messaging = AndroidAutoApps.messagingApps(ctx)
        val messagingOnly = AndroidAutoApps.messagingOnly(messaging)
        val media = AndroidAutoApps.mediaApps(ctx).filter { it.packageName !in carPkgs && it.packageName !in messagingOnly }
        val shown = carPkgs + media.map { it.packageName }
        // Leave out what the car's grid leaves out (Google's Android Auto).
        Lists(car.filter { MyPlayAppList.canShow(it.packageName) }, media.filter { MyPlayAppList.canShow(it.packageName) },
            messaging.filter { it.packageName !in shown && MyPlayAppList.canShow(it.packageName) })
    }.getOrDefault(Lists(emptyList(), emptyList(), emptyList()))
}

@Composable
private fun AppIcon(icon: Bitmap?, label: String, faded: Boolean = false) {
    val m = Modifier.size(NextSpace.IconDisc).clip(CircleShape).then(if (faded) Modifier.alpha(0.5f) else Modifier)
    if (icon != null) Image(icon.asImageBitmap(), null, m)
    else Box(m.background(NextTab.APPS.world), contentAlignment = Alignment.Center) {
        Text(label.take(1), style = NextType.Button, color = NextColors.On)
    }
}

@Composable
private fun BuiltInIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(Modifier.size(NextSpace.IconDisc).clip(CircleShape).background(NextTab.APPS.world), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = NextColors.On, modifier = Modifier.size(20.dp))
    }
}

/** Apps (frames 39, 40): the car screen's app list itself; Reorder is a mode of the same list. */
@Composable
fun NextAppsRoot() {
    val ctx = LocalContext.current
    val nav = LocalNextNavigator.current
    val settings = remember { Settings(ctx) }
    val hidden by rememberSetting("hidden_apps") { hiddenApps }
    val order by rememberSetting("app_order") { appOrder }
    val phoneScreen by rememberSetting("phone_screen") { phoneScreen }
    val browser by rememberSetting("browser") { browser }
    val carData by rememberSetting("car_data_app") { carDataApp }
    val standalone by rememberSetting("standalone_apps") { standaloneApps }
    val allowed by rememberSetting("standalone_app_pkgs") { standaloneAppPackages }
    val lists by produceState<Lists?>(null) { value = loadLists(ctx) }
    val unsupported by produceState(emptyList<UnsupportedCarApp>(), lists) {
        lists?.let { l -> value = withContext(Dispatchers.IO) { runCatching { AndroidAutoApps.unsupported(ctx, l.carApps.map { it.packageName }) }.getOrDefault(emptyList()) } }
    }
    var reordering by rememberSaveable { mutableStateOf(false) }
    var showUnsupported by rememberSaveable { mutableStateOf(false) }
    fun toggle(key: String, on: Boolean) { settings.hiddenApps = withHidden(hidden, key, on) }
    val l = lists
    if (reordering && l != null) {
        ReorderList(l, order, hidden, phoneScreen, browser, onDone = { reordering = false }) { listed, i, by ->
            settings.appOrder = movedOrder(order, listed, i, by)
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize().testTag("next-page-apps"),
        state = rememberNextListState(),
        contentPadding = PaddingValues(start = NextSpace.Gutter, end = NextSpace.Gutter, bottom = 24.dp),
    ) {
        item {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                NextButton(stringResource(R.string.next_apps_reorder_label), { reordering = true }, kind = ButtonKind.TEXT, small = true, enabled = l != null)
            }
            NextCard {
                NextValueRow(
                    stringResource(R.string.next_apps_phone_apps_label),
                    if (!standalone) stringResource(R.string.next_car_off_label) else stringResource(R.string.next_apps_count_label, allowed.size),
                    Icons.Outlined.Smartphone, note = RowNote.Help(stringResource(R.string.next_apps_phone_apps_help)),
                ) { nav.open("apps/phone-apps") }
            }
        }
        if (l == null) item { NextNote(stringResource(R.string.next_apps_loading_help)) }
        else {
            if (l.carApps.isNotEmpty()) group(R.string.next_apps_car_apps_label, l.carApps.map { Triple(it.packageName, it.label, it.icon) }, hidden, ::toggle)
            if (l.media.isNotEmpty()) group(R.string.next_apps_media_label, l.media.map { Triple(it.packageName, it.label, it.icon) }, hidden, ::toggle)
            if (l.messaging.isNotEmpty()) group(R.string.next_apps_messaging_label, l.messaging.map { Triple(it.packageName, it.label, it.icon) }, hidden, ::toggle)
        }
        item { NextSection(stringResource(R.string.next_apps_myplay_label)) }
        item {
            NextCard {
                // Phone and Notifications are built in, so hidden_apps takes their fixed ids. Settings
                // is never offered: hiding it would lock the car-side settings out of the car.
                NextSwitchRow(stringResource(R.string.setup_app_entry_phone), CarRuntime.PHONE_ID !in hidden, { toggle(CarRuntime.PHONE_ID, it) },
                    lead = { BuiltInIcon(androidx.compose.material.icons.Icons.Outlined.Smartphone) })
                NextDivider()
                NextSwitchRow(stringResource(R.string.setup_app_entry_notifications), CarRuntime.NOTIFICATIONS_ID !in hidden, { toggle(CarRuntime.NOTIFICATIONS_ID, it) },
                    lead = { BuiltInIcon(androidx.compose.material.icons.Icons.Outlined.NotificationsNone) })
                NextDivider()
                GearSwitchRow(stringResource(R.string.next_apps_phone_screen_label), phoneScreen, { settings.phoneScreen = it }, Icons.Outlined.Tv) { nav.open("apps/phone-screen") }
                NextDivider()
                GearSwitchRow(stringResource(R.string.next_apps_browser_label), browser, { settings.browser = it }, Icons.Outlined.Language) { nav.open("apps/browser") }
                if (browser) {
                    NextDivider()
                    NextSwitchRow(stringResource(R.string.next_apps_youtube_web_label), CarRuntime.YOUTUBE_ID !in hidden, { toggle(CarRuntime.YOUTUBE_ID, it) },
                        note = RowNote.Help(stringResource(R.string.next_apps_youtube_web_help)), lead = { BuiltInIcon(Icons.Outlined.Tv) })
                }
                NextDivider()
                NextSwitchRow(stringResource(R.string.next_more_car_data_label), carData, { settings.carDataApp = it },
                    lead = { BuiltInIcon(Icons.Outlined.TouchApp) })
            }
        }
        if (unsupported.isNotEmpty()) item {
            Spacer(Modifier.height(12.dp))
            NextCard {
                NextRow(
                    stringResource(R.string.next_apps_cant_show_label), onClick = { showUnsupported = !showUnsupported }, role = Role.Button,
                    lead = { Box(Modifier.size(NextSpace.IconDisc), contentAlignment = Alignment.Center) {
                        Icon(if (showUnsupported) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = NextColors.Text2)
                    } },
                ) { NextValue(unsupported.size.toString()) }
                if (showUnsupported) unsupported.forEach { app ->
                    NextDivider()
                    NextRow(app.label, note = RowNote.Help(app.reason), lead = { AppIcon(app.icon, app.label) })
                }
            }
        }
    }
}

private fun LazyListScope.group(title: Int, apps: List<Triple<String, String, Bitmap?>>, hidden: Set<String>, toggle: (String, Boolean) -> Unit) {
    item { NextSection(stringResource(title)) }
    item {
        NextCard {
            apps.forEachIndexed { i, (pkg, label, icon) ->
                if (i > 0) NextDivider()
                NextSwitchRow(label, pkg !in hidden, { toggle(pkg, it) }, lead = { AppIcon(icon, label) })
            }
        }
    }
}

/** A built-in app with its own page: ⚙ opens it, the switch shows or hides it. */
@Composable
private fun GearSwitchRow(label: String, on: Boolean, onChange: (Boolean) -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, onGear: () -> Unit) {
    val gear = stringResource(R.string.next_apps_gear_label, label)
    NextRow(label, lead = { BuiltInIcon(icon) }) {
        Box(
            Modifier.minimumInteractiveComponentSize().clip(CircleShape).clickable(role = Role.Button, onClick = onGear)
                .semantics { contentDescription = gear },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.Settings, null, tint = NextColors.Text2) }
        Box(Modifier.clickable(role = Role.Switch) { onChange(!on) }.semantics { contentDescription = label }) { NextSwitch(on) }
    }
}

/** Reorder (frame 40): every app in the car's order, hidden ones greyed in their place; drag handles and arrows. */
@Composable
private fun ReorderList(
    l: Lists, order: List<String>, hidden: Set<String>, phoneScreen: Boolean, browser: Boolean,
    onDone: () -> Unit, onMove: (List<String>, Int, Int) -> Unit,
) {
    val icons = remember(l) {
        (l.carApps.map { it.packageName to it.icon } + l.media.map { it.packageName to it.icon } + l.messaging.map { it.packageName to it.icon }).toMap()
    }
    val all = buildList {
        l.carApps.forEach { add(AppKey(it.packageName, it.label)) }
        l.media.forEach { add(AppKey(it.packageName, it.label)) }
        l.messaging.forEach { add(AppKey(it.packageName, it.label)) }
        add(AppKey(CarRuntime.PHONE_ID, stringResource(R.string.setup_app_entry_phone)))
        // Settings is always in the car's list (never hidden): always in this order too.
        add(AppKey(CarRuntime.SETTINGS_ID, stringResource(R.string.setup_app_entry_settings)))
        add(AppKey(CarRuntime.NOTIFICATIONS_ID, stringResource(R.string.setup_app_entry_notifications)))
        if (phoneScreen) add(AppKey(CarRuntime.PHONE_SCREEN_ID, stringResource(R.string.next_apps_phone_screen_label)))
        if (browser) add(AppKey(CarRuntime.BROWSER_ID, stringResource(R.string.next_apps_browser_label)))
        if (browser) add(AppKey(CarRuntime.YOUTUBE_ID, stringResource(R.string.next_apps_youtube_web_label)))
    }
    val sorted = inCarOrder(all, order)
    val listed = sorted.map { it.key }
    val closeWord = stringResource(R.string.next_scr_close_label)
    androidx.activity.compose.BackHandler(onBack = onDone)
    LazyColumn(
        Modifier.fillMaxSize().testTag("next-apps-reorder"),
        contentPadding = PaddingValues(start = NextSpace.Gutter, end = NextSpace.Gutter, bottom = 24.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(NextSpace.Target).clip(CircleShape).clickable(role = Role.Button, onClick = onDone)
                    .semantics { contentDescription = closeWord }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Close, null, tint = NextColors.Text2) }
                Text(stringResource(R.string.next_apps_order_label), style = NextType.PageTitle, color = NextColors.Text, modifier = Modifier.weight(1f))
                NextButton(stringResource(R.string.next_apps_done_label), onDone, small = true)
            }
            NextNote(stringResource(R.string.next_apps_drag_help), Icons.Rounded.DragIndicator)
            Spacer(Modifier.height(10.dp))
        }
        item {
            NextCard {
                sorted.forEachIndexed { i, app ->
                    if (i > 0) NextDivider()
                    val isHidden = app.key in hidden
                    val up = stringResource(R.string.next_apps_up_label, app.label)
                    val down = stringResource(R.string.next_apps_down_label, app.label)
                    NextRow(
                        app.label, note = if (isHidden) RowNote.Help(stringResource(R.string.next_apps_hidden_help)) else null,
                        lead = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.DragIndicator, null, tint = NextColors.Text3)
                                Spacer(Modifier.size(8.dp))
                                AppIcon(icons[app.key], app.label, faded = isHidden)
                            }
                        },
                    ) {
                        ArrowButton(Icons.Rounded.ArrowUpward, up, i > 0) { onMove(listed, i, -1) }
                        ArrowButton(Icons.Rounded.ArrowDownward, down, i < listed.lastIndex) { onMove(listed, i, 1) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArrowButton(icon: androidx.compose.ui.graphics.vector.ImageVector, name: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.minimumInteractiveComponentSize().clip(CircleShape).clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (enabled) NextColors.Text else NextColors.Disabled) }
}

// ---------------------------------------------------------------- Phone apps

/** Apps › Phone apps (frames 41, 42): the risky switch, the allowed apps and their knob turn, YouTube. */
@Composable
fun NextPhoneAppsPage() {
    val ctx = LocalContext.current
    val nav = LocalNextNavigator.current
    val settings = remember { Settings(ctx) }
    val enabled by rememberSetting("standalone_apps") { standaloneApps }
    val awake by rememberSetting("standalone_keep_screen_awake") { standaloneKeepScreenAwake }
    val allowed by rememberSetting("standalone_app_pkgs") { standaloneAppPackages }
    val turns by rememberSetting("myplay_mirror_turns") { myplayMirrorTurns }
    val resumed = rememberResumeTicks()
    val shizuku = rememberShizukuStep()
    val available = remember(resumed, shizuku) { runCatching { AppLauncher.available(ctx) }.getOrDefault(false) }
    // VA-4: the YouTube app is the owner's "Video app", or the one MyPlay finds (a patched build first).
    val videoChoice by rememberSetting("myplay_video_app") { myplayVideoApp }
    val youTubePkg by produceState<String?>(null, videoChoice, resumed) {
        value = withContext(Dispatchers.IO) { runCatching { net.prototux.aare.host.VideoApps.youTubeApp(ctx, settings) }.getOrNull() }
    }
    val apps by produceState<List<Triple<String, String, Bitmap?>>?>(null) { value = withContext(Dispatchers.IO) { launchable(ctx) } }
    var adding by rememberSaveable { mutableStateOf(false) }
    var askYouTube by rememberSaveable { mutableStateOf(false) }
    val swapScope = androidx.compose.runtime.rememberCoroutineScope()
    // VA-14: a new Video app takes the old one's place on the car. Only the app that was the video app
    // is swapped (put on by its row or by the once-only add); other phone apps are left alone.
    fun swapVideoApp(choice: String?) {
        val old = youTubePkg
        settings.myplayVideoApp = choice
        swapScope.launch {
            val now = withContext(Dispatchers.IO) { runCatching { net.prototux.aare.host.VideoApps.youTubeApp(ctx, settings) }.getOrNull() }
            settings.standaloneAppPackages = swappedVideoApp(settings.standaloneAppPackages, old, now)
        }
    }
    var turning by rememberSaveable { mutableStateOf<String?>(null) }
    LazyColumn(
        Modifier.fillMaxSize().testTag("next-page-apps/phone-apps"),
        state = rememberNextListState(),
        contentPadding = PaddingValues(start = NextSpace.Gutter, end = NextSpace.Gutter, bottom = 24.dp),
    ) {
        // Only while neither root nor a running, allowed Shizuku is there: then say exactly what to do.
        // Root in use says so once; the rest shows only while phone apps can't launch yet.
        if (shizuku == ShizukuStep.ROOT_READY || (!available && shizuku != ShizukuStep.READY)) item {
            ShizukuCard(shizuku)
            Spacer(Modifier.height(12.dp))
        }
        item {
            NextCard {
                NextSwitchRow(
                    stringResource(R.string.next_apps_launch_label), enabled, { settings.standaloneApps = it }, Icons.Outlined.Smartphone,
                    note = RowNote.Warn(stringResource(R.string.next_apps_parked_warn_help)), info = stringResource(R.string.next_apps_launch_info),
                    enabled = available || enabled,
                )
            }
        }
        if (enabled) {
            item { NextSection(stringResource(R.string.next_apps_allowed_label), extra = { NextTag(allowed.size.toString(), TagKind.MUTE) }) }
            item {
                NextCard {
                    val byPkg = apps.orEmpty().associateBy { it.first }
                    allowed.sorted().forEach { pkg ->
                        val (_, label, icon) = byPkg[pkg] ?: Triple(pkg, appLabel(ctx, pkg), null)
                        val mode = MirroredKeys.turnFor(turns, pkg)
                        NextRow(label, onClick = { turning = pkg }, role = Role.Button, lead = { AppIcon(icon, label) }) {
                            NextValue(turnWord(mode), mode != MirroredKeys.TurnMode.UP_DOWN)
                        }
                        NextDivider()
                    }
                    NextRow(stringResource(R.string.next_apps_add_label), onClick = { adding = true }, role = Role.Button,
                        lead = { Box(Modifier.size(NextSpace.IconDisc), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Add, null, tint = NextColors.Text2) } })
                }
            }
            item { NextSection(stringResource(R.string.next_apps_while_open_label)) }
            item {
                NextCard { NextSwitchRow(stringResource(R.string.next_apps_no_sleep_label), awake, { settings.standaloneKeepScreenAwake = it }, Icons.Outlined.LightMode) }
            }
        }
        item { NextSection(stringResource(R.string.next_apps_youtube_app_section_label)) }
        item {
            val list = apps.orEmpty()
            val name: (String?) -> String? = { p -> p?.let { q -> list.firstOrNull { it.first == q }?.second ?: q } }
            val auto = if (videoChoice == null) name(youTubePkg) else null
            NextCard {
                // VA-4: which app counts as YouTube (a patched build such as Morphe); Automatic lets MyPlay find it.
                NextPickerRow(
                    stringResource(R.string.next_apps_video_app_label),
                    listOf(Choice("", auto?.let { stringResource(R.string.next_apps_video_auto_named_label, it) } ?: stringResource(R.string.next_apps_video_auto_label))) +
                        list.map { (p, label, _) -> Choice(p, label) },
                    videoChoice.orEmpty(), { swapVideoApp(it.ifEmpty { null }) }, default = "",
                    icon = Icons.Outlined.Tv, info = stringResource(R.string.next_apps_video_app_info),
                )
                val pkg = youTubePkg
                if (pkg != null) {
                    NextDivider()
                    val onCar = youTubeAppOnCar(enabled, allowed, pkg)
                    NextRow(stringResource(R.string.next_apps_youtube_app_label), note = RowNote.Help(stringResource(R.string.next_apps_youtube_parked_help)),
                        lead = { BuiltInIcon(Icons.Outlined.Tv) }) {
                        if (onCar) NextButton(stringResource(R.string.next_apps_take_off_label), {
                            settings.standaloneAppPackages = allowed - pkg
                        }, kind = ButtonKind.GHOST, small = true)
                        else NextButton(stringResource(R.string.next_apps_put_on_label), { askYouTube = true }, kind = ButtonKind.GHOST, small = true, enabled = available)
                    }
                }
            }
        }
    }
    if (askYouTube) NextConfirm(
        stringResource(R.string.next_apps_youtube_app_label), stringResource(R.string.next_apps_youtube_confirm_info),
        stringResource(R.string.next_apps_put_on_label),
        onConfirm = {
            askYouTube = false
            settings.standaloneApps = true
            youTubePkg?.let { settings.standaloneAppPackages = withYouTubeApp(allowed, it) }
        }, onDismiss = { askYouTube = false },
    )
    if (adding) NextSheet(stringResource(R.string.next_apps_add_label), { adding = false }) {
        val list = apps
        if (list == null) NextNote(stringResource(R.string.next_apps_loading_help))
        else androidx.compose.foundation.lazy.LazyColumn(Modifier.height(420.dp)) {
            items(list.size) { i ->
                val (pkg, label, icon) = list[i]
                NextSwitchRow(label, pkg in allowed, { on ->
                    settings.standaloneAppPackages = if (on) allowed + pkg else allowed - pkg
                }, lead = { AppIcon(icon, label) })
            }
        }
    }
    turning?.let { pkg ->
        NextSheet(appLabel(ctx, pkg), { turning = null }, sub = stringResource(R.string.next_apps_turn_help)) {
            NextCard {
                listOf(MirroredKeys.TurnMode.UP_DOWN, MirroredKeys.TurnMode.LEFT_RIGHT).forEachIndexed { i, m ->
                    if (i > 0) NextDivider(16.dp)
                    NextRow(turnWord(m), onClick = {
                        settings.myplayMirrorTurns = withMirrorTurn(turns, pkg, m); turning = null
                    }, role = Role.RadioButton) {
                        if (MirroredKeys.turnFor(turns, pkg) == m) Icon(androidx.compose.material.icons.Icons.Rounded.Check, null, tint = NextColors.Int)
                    }
                }
            }
        }
    }
}

@Composable
private fun turnWord(m: MirroredKeys.TurnMode) = stringResource(
    if (m == MirroredKeys.TurnMode.LEFT_RIGHT) R.string.next_apps_left_right_label else R.string.next_apps_up_down_label,
)

/** Every app with a launcher icon, except MyPlay itself (today's list for the allow-list). */
private fun launchable(ctx: android.content.Context): List<Triple<String, String, Bitmap?>> = runCatching {
    val pm = ctx.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    pm.queryIntentActivities(intent, 0).mapNotNull { it.activityInfo?.packageName }.filter { it != ctx.packageName }.distinct()
        .mapNotNull { pkg ->
            val info = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull() ?: return@mapNotNull null
            val icon = runCatching { pm.getApplicationIcon(info).toBitmap(96, 96) }.getOrNull()
            Triple(pkg, pm.getApplicationLabel(info).toString(), icon)
        }.sortedBy { it.second.lowercase() }
}.getOrDefault(emptyList())

// ---------------------------------------------------------------- Phone screen and Browser

/** Apps › Phone screen: the switch, and the touch permission it needs. */
@Composable
fun NextPhoneScreenPage() {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    val on by rememberSetting("phone_screen") { phoneScreen }
    val resumed = rememberResumeTicks()
    val touch = remember(resumed) {
        AndroidSettings.Secure.getString(ctx.contentResolver, AndroidSettings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty().contains(ctx.packageName)
    }
    LazyColumn(
        Modifier.fillMaxSize().testTag("next-page-apps/phone-screen"),
        contentPadding = PaddingValues(start = NextSpace.Gutter, end = NextSpace.Gutter, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            NextCard {
                NextSwitchRow(stringResource(R.string.next_apps_phone_screen_label), on, { settings.phoneScreen = it }, Icons.Outlined.Tv,
                    note = RowNote.Help(stringResource(R.string.next_apps_phone_screen_help)))
            }
        }
        if (on && !touch) item {
            NextCard(highlight = true) {
                NextRow(stringResource(R.string.next_apps_touch_label), icon = Icons.Outlined.TouchApp,
                    note = RowNote.Help(stringResource(R.string.next_apps_touch_help)))
                Box(Modifier.padding(start = 70.dp, bottom = 14.dp)) {
                    NextButton(stringResource(R.string.next_upd_allow_button_label), { ctx.startSafely(Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS)) }, small = true)
                }
            }
        }
    }
}

/** Apps › Browser: the start page, saved as typed. */
@Composable
fun NextBrowserPage() {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    val on by rememberSetting("browser") { browser }
    var url by rememberSaveable { mutableStateOf(settings.browserUrl) }
    LazyColumn(
        Modifier.fillMaxSize().testTag("next-page-apps/browser"),
        contentPadding = PaddingValues(start = NextSpace.Gutter, end = NextSpace.Gutter, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            NextCard {
                NextSwitchRow(stringResource(R.string.next_apps_browser_label), on, { settings.browser = it }, Icons.Outlined.Language,
                    note = RowNote.Warn(stringResource(R.string.next_apps_browser_parked_help)))
            }
        }
        item {
            NextCard {
                androidx.compose.material3.OutlinedTextField(
                    url, { url = it; settings.browserUrl = it }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    label = { Text(stringResource(R.string.next_apps_start_page_label)) },
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
}

