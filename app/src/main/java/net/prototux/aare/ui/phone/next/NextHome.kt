package net.prototux.aare.ui.phone.next

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.GppGood
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.text.DateFormat
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.UpdateChecker
import net.prototux.aare.audio.CaptureMethods
import net.prototux.aare.cert.CertificateUpdates
import net.prototux.aare.service.ProjectionService
import net.prototux.aare.ui.CarPreviewActivity
import net.prototux.aare.ui.car.myplay.KEY_ENGINE_LANGUAGE
import net.prototux.aare.ui.car.myplay.myplayEngineLanguage
import net.prototux.aare.ui.car.myplay.myplayEnginePreview
import net.prototux.aare.ui.permissionBlocked
import net.prototux.aare.ui.phone.CarPicture

/** What Home needs from the world, so a picture or a test can hand it in instead. */
data class HomeInputs(
    val hero: Hero,
    val attention: Set<Attention>,
    val design: DesignInUse,
    val update: UpdateChecker.Update? = null,
    val certificate: CertificateUpdates.Expiring? = null,
    val setupLeft: List<SetupItem> = emptyList(),
    /** When the hero's error was first seen, for "What happened". */
    val errorAt: Long? = null,
)

/** What "Finish setup" counts: only what connecting needs (plan §10); the rest is asked in context. */
enum class SetupItem { PERMISSIONS, NOTIFICATION_ACCESS, CAR_AUDIO }

/** What Home's buttons do; the real ones talk to the service, a test's record the taps. */
interface HomeActions {
    fun trust(hero: Hero.Trust)
    fun notNow(hero: Hero.Trust)
    fun cancel()
    fun disconnect()
    fun tryAgain()
    fun fix(why: Why)
    fun preview()
    fun setUp(item: SetupItem)
    fun dismissUpdate()
    fun dismissCertificate()
}

/** Home (plan §16 chunk 2): the hero, what needs attention, the small car picture. */
@Composable
fun NextHome() {
    val ctx = LocalContext.current
    val state by ProjectionService.state.collectAsState()
    val update by UpdateChecker.available.collectAsState()
    val expiring by CertificateUpdates.expiring.collectAsState()
    val wireless by rememberSetting("wireless") { wireless }
    val preview by rememberSetting("myplay_engine_preview", "myplay_orbit_default_done") { myplayEnginePreview }
    val language by rememberSetting(KEY_ENGINE_LANGUAGE) { myplayEngineLanguage }
    var notNow by rememberSaveable { mutableStateOf<String?>(null) }
    val resumed = rememberResumeCount()
    val radios = rememberRadios(wireless, resumed)
    val wirelessConnecting = state.status == stringResource(R.string.notif_status_connecting_wirelessly)
    val hero = heroOf(state, radios, wirelessConnecting, notNow)
    var errorAt by rememberSaveable { mutableLongStateOf(0L) }
    LaunchedEffect(state.lastError) { errorAt = if (state.lastError != null) System.currentTimeMillis() else 0L }
    val setupLeft = remember(resumed, state.audioCapture) { setupLeft(ctx, state.audioCapture) }
    val design = designInUse(preview, language)
    val attention = buildSet {
        expiring?.let { add(if (it.expired) Attention.CERT_EXPIRED else Attention.CERT_EXPIRING) }
        if (setupLeft.isNotEmpty()) add(Attention.FINISH_SETUP)
        if (update != null) add(Attention.UPDATE)
        if (design != DesignInUse.ORBIT) add(Attention.ARCHIVED)
    }
    val permissions = rememberConnectPermissions()
    val actions = remember(permissions) {
        object : HomeActions {
            override fun trust(hero: Hero.Trust) = ProjectionService.trust(ctx, hero.peerKey, hero.fingerprint)
            override fun notNow(hero: Hero.Trust) { notNow = hero.fingerprint }
            override fun cancel() = ProjectionService.stop(ctx)
            override fun disconnect() = ProjectionService.stop(ctx)
            override fun tryAgain() { ProjectionService.probeUsb(ctx); ProjectionService.probeWireless(ctx) }
            override fun fix(why: Why) = fixRadio(ctx, why)
            override fun preview() = ctx.startActivity(Intent(ctx, CarPreviewActivity::class.java))
            override fun setUp(item: SetupItem) = when (item) {
                SetupItem.PERMISSIONS -> permissions()
                SetupItem.NOTIFICATION_ACCESS -> ctx.startSafely(Intent(AndroidSettings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                SetupItem.CAR_AUDIO -> Unit // a link: the page opens it
            }
            override fun dismissUpdate() = UpdateChecker.dismiss(ctx)
            override fun dismissCertificate() = CertificateUpdates.dismiss(ctx)
        }
    }
    NextHomeContent(
        HomeInputs(hero, attention, design, update, expiring, setupLeft, errorAt.takeIf { it > 0 }),
        actions,
        picture = { CarPicture(stringResource(R.string.next_home_car_screen_label), Modifier.fillMaxWidth(), onPart = null) },
    )
}

/** Home drawn from [inputs]; [picture] is the live car picture (a plain box in tests). */
@Composable
fun NextHomeContent(inputs: HomeInputs, actions: HomeActions, picture: @Composable () -> Unit) {
    val nav = LocalNextNavigator.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    val cards = attentionCards(inputs.attention, expanded)
    LazyColumn(
        Modifier.fillMaxSize().testTag("next-page-home"),
        state = rememberNextListState(),
        contentPadding = PaddingValues(start = NextSpace.Gutter, end = NextSpace.Gutter, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { HeroBlock(inputs.hero, actions, inputs.errorAt) }
        items(cards.shown.size) { i -> AttentionCard(cards.shown[i], inputs, actions) }
        if (cards.more.isNotEmpty()) item {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                NextButton(stringResource(R.string.next_att_more_label, cards.more.size), { expanded = true }, kind = ButtonKind.TEXT, small = true)
            }
        }
        item { PictureCard(inputs.design, picture) { nav.open("screen") } }
        if (inputs.hero is Hero.Connected) item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NextButton(stringResource(R.string.next_home_preview_label), actions::preview, Modifier.weight(1f), icon = Icons.Rounded.Tv)
                NextButton(stringResource(R.string.next_home_disconnect_label), actions::disconnect, Modifier.weight(1f), ButtonKind.GHOST)
            }
        } else item {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                NextButton(stringResource(R.string.next_home_preview_car_label), actions::preview, kind = ButtonKind.TEXT, icon = Icons.Rounded.Tv)
            }
        }
    }
}

// ---------------------------------------------------------------- the hero


@Composable
private fun HeroBlock(hero: Hero, actions: HomeActions, errorAt: Long?) {
    val nav = LocalNextNavigator.current
    var details by rememberSaveable { mutableStateOf(false) }
    var whatHappened by rememberSaveable { mutableStateOf(false) }
    val green = NextTab.HOME.world
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        when (hero) {
            is Hero.Connected -> {
                NextHeroRing(RingStyle.FULL, green, Icons.Rounded.DirectionsCar)
                HeroTitle(stringResource(R.string.next_home_connected_label), carAndLink(hero.car, hero.how))
                Spacer(Modifier.height(16.dp))
                NextCard {
                    NextValueRow(stringResource(R.string.next_home_details_label), hero.video.ifBlank { null }, Icons.Outlined.Info) { details = true }
                }
            }
            is Hero.Connecting -> {
                NextHeroRing(RingStyle.SWEEP, green, Icons.Outlined.DirectionsCar)
                HeroTitle(
                    hero.car?.let { stringResource(R.string.next_home_connecting_label, it) } ?: stringResource(R.string.next_home_connecting_any_label),
                    hero.how?.let { linkWord(it) },
                )
                Spacer(Modifier.height(16.dp))
                NextButton(stringResource(R.string.next_home_cancel_label), actions::cancel, kind = ButtonKind.GHOST)
            }
            is Hero.Trust -> {
                NextHeroRing(RingStyle.CAUTION, NextColors.Caution, Icons.Outlined.GppGood)
                HeroTitle(
                    stringResource(if (hero.changed) R.string.next_home_trust_changed_label else R.string.next_home_trust_label),
                    carAndLink(stringResource(R.string.next_home_new_car_label), hero.how),
                )
                Spacer(Modifier.height(16.dp))
                NextCard {
                    NextRow(
                        stringResource(R.string.next_home_fingerprint_label, hero.fingerprint.take(16).uppercase().chunked(4).joinToString(" ")),
                        icon = Icons.Outlined.Key,
                        note = RowNote.Warn(stringResource(R.string.next_home_trust_warn_help)),
                        info = stringResource(R.string.next_home_fingerprint_info, hero.fingerprint.uppercase().chunked(4).joinToString(" ")),
                    )
                }
                Spacer(Modifier.height(16.dp))
                // The one security decision: both answers the same size (review "Second pass" item 3).
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NextButton(stringResource(R.string.next_home_not_now_label), { actions.notNow(hero) }, Modifier.weight(1f), ButtonKind.GHOST)
                    NextButton(stringResource(R.string.next_home_trust_button_label), { actions.trust(hero) }, Modifier.weight(1f), icon = Icons.Outlined.VerifiedUser)
                }
                NextNote(stringResource(R.string.next_home_then_reconnects_help), Icons.Rounded.Refresh)
            }
            is Hero.Failed -> {
                NextHeroRing(RingStyle.ERROR, NextColors.Warn, Icons.Outlined.DirectionsCar)
                HeroTitle(stringResource(R.string.next_home_failed_label), oneLine(hero.reason))
                Spacer(Modifier.height(16.dp))
                NextButton(stringResource(R.string.next_home_try_again_label), actions::tryAgain, Modifier.fillMaxWidth(), icon = Icons.Rounded.Refresh)
                Spacer(Modifier.height(12.dp))
                val at = errorAt?.let { DateFormat.getTimeInstance(DateFormat.SHORT).format(it) }
                NextGroup(listOf(
                    { NextValueRow(stringResource(R.string.next_home_what_happened_label), at, Icons.Outlined.ErrorOutline) { whatHappened = true } },
                    { NextValueRow(stringResource(R.string.next_home_car_check_label), icon = Icons.Outlined.MonitorHeart) { nav.open("more/diagnostics/car-check") } },
                ))
                if (whatHappened) NextSheet(stringResource(R.string.next_home_what_happened_label), { whatHappened = false }) {
                    Text(hero.reason, style = NextType.Value, color = NextColors.Text2)
                    Spacer(Modifier.height(16.dp))
                    NextButton(stringResource(R.string.next_home_log_label), { whatHappened = false; nav.open("more/diagnostics/log") }, kind = ButtonKind.GHOST)
                }
            }
            is Hero.NotConnected -> {
                NextHeroRing(RingStyle.DASHED, NextColors.Text3, Icons.Outlined.DirectionsCar)
                val (line, action, icon) = whyWords(hero.why)
                HeroTitle(stringResource(R.string.next_home_not_connected_label), line)
                if (action != null) {
                    Spacer(Modifier.height(16.dp))
                    NextButton(action, { actions.fix(hero.why) }, Modifier.fillMaxWidth(), icon = icon)
                    Text(stringResource(R.string.next_home_or_usb_help), style = NextType.Help, color = NextColors.Text3,
                        modifier = Modifier.padding(top = 10.dp))
                }
            }
        }
    }
    if (details && hero is Hero.Connected) NextSheet(stringResource(R.string.next_home_details_label), { details = false }) {
        NextCard {
            if (hero.video.isNotBlank()) NextRow(stringResource(R.string.next_home_video_label), trailing = { NextValue(hero.video) })
            NextDivider(16.dp)
            NextRow(stringResource(R.string.next_home_certificate_label), trailing = { NextValue(hero.certificate) })
            hero.how?.let {
                NextDivider(16.dp)
                NextRow(stringResource(R.string.next_home_link_label), trailing = { NextValue(linkWord(it)) })
            }
        }
    }
}

@Composable
private fun whyWords(why: Why): Triple<String, String?, ImageVector?> = when (why) {
    Why.BLUETOOTH_OFF -> Triple(stringResource(R.string.next_home_bt_off_help), stringResource(R.string.next_home_turn_on_bt_label), Icons.Rounded.Bluetooth)
    Why.WIFI_OFF -> Triple(stringResource(R.string.next_home_wifi_off_help), stringResource(R.string.next_home_turn_on_wifi_label), Icons.Rounded.Wifi)
    Why.WIRELESS_OFF -> Triple(stringResource(R.string.next_home_wireless_off_help), stringResource(R.string.next_home_turn_on_wireless_label), Icons.Rounded.Wifi)
    Why.WAITING -> Triple(stringResource(R.string.next_home_waiting_help), null, null)
}

@Composable
private fun linkWord(link: Link): String = stringResource(if (link == Link.USB) R.string.next_home_usb_label else R.string.next_home_wireless_label)

@Composable
private fun carAndLink(car: String, link: Link?): String = if (link == null) car else "$car · ${linkWord(link)}"

@Composable
private fun HeroTitle(title: String, sub: String?) {
    Text(title, style = NextType.Hero, color = NextColors.Text, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp).testTag("next-hero-title"))
    if (sub != null) Text(sub, style = NextType.Value.copy(fontSize = NextType.Label.fontSize), color = NextColors.Text2,
        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
}

// ---------------------------------------------------------------- attention

@Composable
private fun AttentionCard(kind: Attention, inputs: HomeInputs, actions: HomeActions) {
    val nav = LocalNextNavigator.current
    val dateOf = { e: CertificateUpdates.Expiring -> DateFormat.getDateInstance().format(e.notAfter) }
    when (kind) {
        Attention.CERT_EXPIRED -> inputs.certificate?.let { e ->
            NextCard(warn = true) {
                NextValueRow(stringResource(R.string.next_att_cert_expired_label), icon = Icons.Outlined.VerifiedUser,
                    note = RowNote.Help(stringResource(R.string.next_att_cert_help, e.name, dateOf(e)))) { nav.open("more/advanced/certificates") }
            }
        }
        Attention.CERT_EXPIRING -> inputs.certificate?.let { e ->
            NextCard {
                DismissibleRow(
                    stringResource(R.string.next_att_cert_expiring_label), stringResource(R.string.next_att_cert_help, e.name, dateOf(e)),
                    Icons.Outlined.VerifiedUser, actions::dismissCertificate,
                ) { nav.open("more/advanced/certificates") }
            }
        }
        Attention.FINISH_SETUP -> {
            var open by rememberSaveable { mutableStateOf(false) }
            val words = inputs.setupLeft.map { setupWord(it) }
            NextCard {
                NextRow(
                    stringResource(R.string.next_att_setup_label), icon = Icons.Outlined.AutoAwesome,
                    note = RowNote.Help(words.joinToString(", ")),
                    onClick = { open = true }, role = Role.Button,
                ) {
                    NextValue(stringResource(R.string.next_att_setup_left_label, inputs.setupLeft.size))
                    Box(Modifier.size(10.dp).clip(CircleShape).background(NextColors.Caution))
                }
            }
            if (open) NextSheet(stringResource(R.string.next_att_setup_label), { open = false }) {
                NextGroup(inputs.setupLeft.map { item ->
                    @Composable {
                        NextRow(setupWord(item), onClick = {
                            if (item == SetupItem.CAR_AUDIO) { open = false; nav.open("car/sound") } else actions.setUp(item)
                        }, role = Role.Button) { Text(stringResource(R.string.next_att_set_up_label), style = NextType.Button.copy(fontSize = NextType.Value.fontSize), color = NextColors.Int) }
                    }
                }, inset = 16.dp)
            }
        }
        Attention.UPDATE -> inputs.update?.let { u ->
            NextCard(highlight = true) {
                DismissibleRow(
                    stringResource(R.string.next_att_update_label, u.version),
                    stringResource(R.string.next_att_update_help, net.prototux.aare.BuildConfig.VERSION_NAME),
                    Icons.Outlined.SystemUpdate, actions::dismissUpdate, onClick = null,
                )
                Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Install lives on the Updates page (chunk 3), with its progress and errors.
                    NextButton(stringResource(R.string.next_att_install_label), { nav.open("more/updates") }, Modifier.weight(1f), icon = Icons.Rounded.Download, small = true)
                    NextButton(stringResource(R.string.next_att_whats_new_label), { nav.open("more/whats-new") }, Modifier.weight(1f), ButtonKind.GHOST, small = true)
                }
            }
        }
        Attention.ARCHIVED -> NextCard {
            NextValueRow(
                stringResource(if (inputs.design == DesignInUse.DECK) R.string.next_att_deck_label else R.string.next_att_older_label),
                icon = Icons.Outlined.Archive, note = RowNote.Help(stringResource(R.string.next_att_archived_help)),
            ) { nav.open("more/archive") }
        }
    }
}

@Composable
private fun DismissibleRow(label: String, help: String, icon: ImageVector, onDismiss: () -> Unit, onClick: (() -> Unit)?) {
    val dismiss = stringResource(R.string.next_att_dismiss_label)
    NextRow(label, icon = icon, note = RowNote.Help(help), onClick = onClick, role = Role.Button) {
        Box(
            Modifier.minimumInteractiveComponentSize().clip(CircleShape).clickable(role = Role.Button, onClick = onDismiss)
                .semantics { contentDescription = dismiss },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Close, null, tint = NextColors.Text2) }
    }
}

@Composable
private fun setupWord(item: SetupItem): String = stringResource(when (item) {
    SetupItem.PERMISSIONS -> R.string.next_att_setup_perms_label
    SetupItem.NOTIFICATION_ACCESS -> R.string.next_att_setup_notif_label
    SetupItem.CAR_AUDIO -> R.string.next_att_setup_audio_label
})

// ---------------------------------------------------------------- the picture

@Composable
private fun PictureCard(design: DesignInUse, picture: @Composable () -> Unit, onOpen: () -> Unit) {
    NextCard(Modifier.clickable(role = Role.Button, onClick = onOpen)) {
        Box(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp)) { picture() }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.next_home_car_screen_label), style = NextType.Label, color = NextColors.Text, modifier = Modifier.weight(1f))
            NextValue(stringResource(when (design) {
                DesignInUse.ORBIT -> R.string.next_design_orbit_label
                DesignInUse.DECK -> R.string.next_design_deck_label
                DesignInUse.OLDER -> R.string.next_design_older_label
            }))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = NextColors.Text3, modifier = Modifier.size(20.dp))
        }
    }
}

// ---------------------------------------------------------------- the phone

/** Counts the times the app came back to the front, so checks of the phone's state run again. */
@Composable
private fun rememberResumeCount(): Int {
    val lifecycle = LocalLifecycleOwner.current
    var count by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) count++ }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    return count
}

/** Bluetooth and Wi-Fi on or off, following the system's broadcasts while Home shows. */
@Composable
private fun rememberRadios(wireless: Boolean, resumed: Int): Radios {
    val ctx = LocalContext.current
    var changes by remember { mutableIntStateOf(0) }
    DisposableEffect(ctx) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) { changes++ }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        }
        ContextCompat.registerReceiver(ctx, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { ctx.unregisterReceiver(receiver) }
    }
    return remember(wireless, resumed, changes) {
        val bt = runCatching { ctx.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled }.getOrNull() ?: true
        val wifi = runCatching { ctx.applicationContext.getSystemService(WifiManager::class.java)?.isWifiEnabled }.getOrNull() ?: true
        Radios(wireless, bt, wifi)
    }
}

private fun fixRadio(ctx: Context, why: Why) {
    when (why) {
        Why.BLUETOOTH_OFF -> {
            val canAsk = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            if (canAsk) ctx.startSafely(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            else ctx.startSafely(Intent(AndroidSettings.ACTION_BLUETOOTH_SETTINGS))
        }
        Why.WIFI_OFF -> ctx.startSafely(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Intent(AndroidSettings.Panel.ACTION_WIFI) else Intent(AndroidSettings.ACTION_WIFI_SETTINGS),
        )
        Why.WIRELESS_OFF -> {
            Settings(ctx).wireless = true
            ProjectionService.probeWireless(ctx)
        }
        Why.WAITING -> Unit
    }
}

/** The permissions connecting needs (plan §10 Setup step 1); calls, contacts and the rest are asked in context. */
internal fun connectPermissions(): List<String> = buildList {
    add(Manifest.permission.ACCESS_COARSE_LOCATION); add(Manifest.permission.ACCESS_FINE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { add(Manifest.permission.BLUETOOTH_CONNECT); add(Manifest.permission.BLUETOOTH_SCAN) }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { add(Manifest.permission.POST_NOTIFICATIONS); add(Manifest.permission.NEARBY_WIFI_DEVICES) }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) add(Manifest.permission.ACCESS_LOCAL_NETWORK)
}

private fun setupLeft(ctx: Context, audioCapture: Boolean): List<SetupItem> = buildList {
    if (!connectPermissions().all { ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED }) add(SetupItem.PERMISSIONS)
    if (!NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)) add(SetupItem.NOTIFICATION_ACCESS)
    val audio = runCatching {
        CaptureMethods.audioPolicyAvailable(ctx) || CaptureMethods.shizukuAvailable() || CaptureMethods.rootAvailable(ctx)
    }.getOrDefault(false) || audioCapture
    if (!audio) add(SetupItem.CAR_AUDIO)
}

/**
 * Asks for [connectPermissions]. One refused for good no longer shows a dialog, so then MyPlay's
 * system settings open instead (today's rule, [permissionBlocked]).
 */
@Composable
private fun rememberConnectPermissions(): () -> Unit {
    val ctx = LocalContext.current
    val activity = ctx as? android.app.Activity
    val settings = remember { Settings(ctx) }
    var before by remember { mutableStateOf(emptySet<String>()) }
    var asked by remember { mutableStateOf(emptySet<String>()) }
    fun rationale(p: String) = activity != null && androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, p)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val blocked = activity != null && result.filterValues { !it }.keys.any { permissionBlocked(it in before, rationale(it), it in asked) }
        if (blocked) ctx.startSafely(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", ctx.packageName, null)))
    }
    return {
        val wanted = connectPermissions()
        before = wanted.filter(::rationale).toSet()
        asked = settings.requestedPermissions
        settings.requestedPermissions = asked + wanted
        launcher.launch(wanted.toTypedArray())
    }
}

internal fun Context.startSafely(intent: Intent) {
    runCatching { startActivity(intent) }
}
