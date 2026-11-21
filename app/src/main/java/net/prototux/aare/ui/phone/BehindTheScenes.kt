package net.prototux.aare.ui.phone

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Gamepad
import net.prototux.aare.car.myplayButtons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.prototux.aare.BuildConfig
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.UpdateChecker
import net.prototux.aare.audio.CaptureMethod
import net.prototux.aare.audio.CaptureMethods
import net.prototux.aare.cert.CertificateStore
import net.prototux.aare.host.AndroidAutoApps
import net.prototux.aare.host.Assistants
import net.prototux.aare.host.CarAppCatalog
import net.prototux.aare.host.MyPlayAppList

/** The "Behind the scenes" tiles on the start screen (plan E4), in the order they are shown. */
enum class BehindTile { UPDATES, CONNECTION, APPS, BUTTONS, CAR_CHECK, SPEED_LIMITS, SOUND, MESSAGES, VOICE, CERTIFICATES, BACKUP, ADVANCED, CAR_DATA, ABOUT }

/** What the Sound tile says. */
enum class SoundValue { OFF, NOT_SET_UP, BEST, ALL_SOUND, MUSIC_ONLY }

/** Sound to the car switched off, or the way it is captured (the best one the phone allows, as the Sound page picks it). */
fun soundValue(carAudio: Boolean, best: CaptureMethod?): SoundValue = when {
    !carAudio -> SoundValue.OFF
    best == null -> SoundValue.NOT_SET_UP
    best == CaptureMethod.AUDIO_POLICY || best == CaptureMethod.SHIZUKU_POLICY -> SoundValue.BEST
    best == CaptureMethod.SHIZUKU -> SoundValue.ALL_SOUND
    else -> SoundValue.MUSIC_ONLY
}

/** The best capture method the phone allows now: the same order as the Sound page. */
fun bestCaptureMethod(policy: Boolean, shizuku: Boolean, root: Boolean, playbackCapture: Boolean): CaptureMethod? = when {
    policy -> CaptureMethod.AUDIO_POLICY
    shizuku -> CaptureMethod.SHIZUKU_POLICY
    root -> CaptureMethod.SHIZUKU
    playbackCapture -> CaptureMethod.PLAYBACK_CAPTURE
    else -> null
}

/** What the Messages tile says. */
enum class MessagesValue { NO_ACCESS, ON, OFF }

/** Messages need notification access first; then "on" means message banners are shown on the car. */
fun messagesValue(access: Boolean, banners: Boolean): MessagesValue = when {
    !access -> MessagesValue.NO_ACCESS
    banners -> MessagesValue.ON
    else -> MessagesValue.OFF
}

/** What the Advanced tile says. */
enum class AdvancedValue { LOG_ON, SERVER_ON, NORMAL }

/** The detailed protocol log first (it is what the Advanced page is mostly opened for), then the head unit server. */
fun advancedValue(verboseLog: Boolean, headUnitServer: Boolean): AdvancedValue = when {
    verboseLog -> AdvancedValue.LOG_ON
    headUnitServer -> AdvancedValue.SERVER_ON
    else -> AdvancedValue.NORMAL
}

/**
 * How many phone apps the car screen lists, by the same rules as the car's app grid
 * (CarRuntime.applyApps and the Apps page): car apps, then media apps without a car app and not
 * messaging-only, then messaging apps not listed yet; hidden ones left out. Built-in entries
 * (Phone, Settings…) are not counted, nor Google's Android Auto, which the car's grid leaves out.
 */
fun carScreenAppCount(car: Set<String>, media: Set<String>, messaging: Set<String>, messagingOnly: Set<String>, hidden: Set<String>): Int {
    val listedMedia = media - car - messagingOnly
    val shown = car + listedMedia
    val listedMessaging = messaging - shown
    // Google's Android Auto is never in the car's grid (K8).
    return (shown + listedMessaging).count { it !in hidden && MyPlayAppList.canShow(it) }
}

private fun countCarScreenApps(context: Context, hidden: Set<String>): Int {
    val car = CarAppCatalog.query(context).mapTo(HashSet()) { it.packageName }
    val messaging = AndroidAutoApps.messagingApps(context)
    val media = AndroidAutoApps.mediaApps(context).mapTo(HashSet()) { it.packageName }
    return carScreenAppCount(car, media, messaging.mapTo(HashSet()) { it.packageName }, AndroidAutoApps.messagingOnly(messaging), hidden)
}

/** One tile's look and what it shows. */
internal class TileSpec(val tile: BehindTile, val icon: ImageVector, val tone: TileTone, val label: String, val value: String?, val attention: Boolean = false)

/**
 * The tiles, three to a row, each showing its own state. [connected] and [audioCapture] come from
 * the connection's state; [refresh] changes when the user comes back to the app, to re-read
 * permissions and settings changed elsewhere.
 */
@Composable
fun BehindTheScenesTiles(connected: Boolean, audioCapture: Boolean, refresh: Int, open: (BehindTile) -> Unit) {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    val update by UpdateChecker.available.collectAsState()
    @Suppress("UNUSED_EXPRESSION") refresh
    // Slow ones (package lists, certificate files) off the main thread.
    val apps by produceState<Int?>(null, refresh) {
        value = withContext(Dispatchers.IO) { runCatching { countCarScreenApps(ctx, settings.hiddenApps) }.getOrNull() }
    }
    val certificate by produceState<String?>(null, refresh) {
        value = withContext(Dispatchers.IO) {
            runCatching { CertificateStore(ctx).pick(settings.certificateIndex) }.getOrNull()?.let { e ->
                if (!e.removable && e.selfSigned) null else e.name
            }
        }
    }
    val certificateText = certificate ?: stringResource(R.string.phone_tile_cert_own)
    val speedLimitRegionName = remember(refresh) { SpeedLimitFiles.REGIONS.firstOrNull { it.id == settings.myplaySpeedLimitRegion }?.name }
    val voice = remember(refresh) {
        (settings.assistantPackage ?: Assistants.phoneDefault(ctx))?.let { Assistants.label(ctx, it) }
    }
    // Read again when the user comes back to the app ([refresh]), not on every redraw.
    val sound = remember(refresh, audioCapture) {
        soundValue(
            settings.carAudio,
            bestCaptureMethod(CaptureMethods.audioPolicyAvailable(ctx), CaptureMethods.shizukuAvailable(), CaptureMethods.rootAvailable(ctx), audioCapture),
        )
    }
    val messages = remember(refresh) { messagesValue(NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName), settings.messageBanners) }
    val advanced = remember(refresh) { advancedValue(settings.verboseProtocolLog, settings.headUnitServer) }

    val tiles = listOf(
        TileSpec(BehindTile.UPDATES, Icons.Rounded.SystemUpdate, TileTone.BLUE, stringResource(R.string.phone_tile_updates), update?.version ?: BuildConfig.VERSION_NAME, attention = update != null),
        TileSpec(BehindTile.CONNECTION, Icons.Rounded.Bluetooth, TileTone.GREEN, stringResource(R.string.phone_tile_connection),
            stringResource(if (connected) R.string.phone_tile_connected else R.string.phone_tile_waiting)),
        TileSpec(BehindTile.APPS, Icons.Rounded.Apps, TileTone.VIOLET, stringResource(R.string.phone_tile_apps),
            apps?.let { stringResource(R.string.phone_tile_apps_count, it) }),
        TileSpec(BehindTile.BUTTONS, Icons.Rounded.Gamepad, TileTone.PINK, stringResource(R.string.phone_tile_buttons),
            remember(refresh) { changedCount(settings.myplayButtons) }.let { n ->
                if (n == 0) stringResource(R.string.phone_tile_buttons_default) else stringResource(R.string.phone_tile_buttons_changed, n)
            }),
        TileSpec(BehindTile.CAR_CHECK, Icons.Rounded.Checklist, TileTone.CORAL, stringResource(R.string.phone_tile_car_check), stringResource(R.string.phone_check_start)),
        TileSpec(BehindTile.SPEED_LIMITS, Icons.Rounded.Speed, TileTone.GREEN, stringResource(R.string.phone_tile_speed),
            speedLimitRegionName ?: stringResource(R.string.phone_speed_region_none)),
        TileSpec(BehindTile.SOUND, Icons.AutoMirrored.Rounded.VolumeUp, TileTone.AMBER, stringResource(R.string.phone_tile_sound),
            stringResource(when (sound) {
                SoundValue.OFF -> R.string.phone_tile_off
                SoundValue.NOT_SET_UP -> R.string.phone_tile_set_up
                SoundValue.BEST -> R.string.phone_tile_sound_best
                SoundValue.ALL_SOUND -> R.string.phone_tile_sound_all
                SoundValue.MUSIC_ONLY -> R.string.phone_tile_sound_music
            }), attention = sound == SoundValue.NOT_SET_UP),
        TileSpec(BehindTile.MESSAGES, Icons.Rounded.Forum, TileTone.BLUE, stringResource(R.string.phone_tile_messages),
            stringResource(when (messages) {
                MessagesValue.NO_ACCESS -> R.string.phone_tile_set_up
                MessagesValue.ON -> R.string.phone_tile_on
                MessagesValue.OFF -> R.string.phone_tile_off
            }), attention = messages == MessagesValue.NO_ACCESS),
        TileSpec(BehindTile.VOICE, Icons.Rounded.Mic, TileTone.PINK, stringResource(R.string.phone_tile_voice), voice ?: stringResource(R.string.phone_tile_none)),
        TileSpec(BehindTile.CERTIFICATES, Icons.Rounded.VerifiedUser, TileTone.GREEN, stringResource(R.string.phone_tile_certificates), certificateText),
        TileSpec(BehindTile.BACKUP, Icons.Rounded.Backup, TileTone.GREEN, stringResource(R.string.phone_tile_backup),
            remember(refresh) { lastSavedText(settings.myplayBackupLastSaved) } ?: stringResource(R.string.phone_tile_file)),
        TileSpec(BehindTile.ADVANCED, Icons.Rounded.Tune, TileTone.CORAL, stringResource(R.string.phone_tile_advanced),
            stringResource(when (advanced) {
                AdvancedValue.LOG_ON -> R.string.phone_tile_log_on
                AdvancedValue.SERVER_ON -> R.string.phone_tile_server_on
                AdvancedValue.NORMAL -> R.string.phone_tile_normal
            })),
        TileSpec(BehindTile.CAR_DATA, Icons.Rounded.DirectionsCar, TileTone.AMBER, stringResource(R.string.phone_tile_car_data), stringResource(R.string.phone_tile_view)),
        TileSpec(BehindTile.ABOUT, Icons.Rounded.Info, TileTone.VIOLET, stringResource(R.string.phone_tile_about), stringResource(R.string.app_name)),
    )
    TileGrid(tiles, open)
}

@Composable
internal fun TileGrid(tiles: List<TileSpec>, open: (BehindTile) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tiles.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { t ->
                    SymbolTile(t.icon, t.value, t.label, t.tone, Modifier.weight(1f), attention = t.attention) { open(t.tile) }
                }
                // Keeps a short last row's tiles the same width as the others.
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
