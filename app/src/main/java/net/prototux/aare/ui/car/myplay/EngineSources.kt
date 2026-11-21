package net.prototux.aare.ui.car.myplay

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import android.content.pm.PackageManager
import android.provider.CallLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.prototux.aare.car.Calls
import net.prototux.aare.ui.car.PHONE_PERMISSIONS
import net.prototux.aare.ui.car.contacts
import net.prototux.aare.ui.car.recents
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import net.prototux.aare.car.SpeedReading
import kotlinx.coroutines.flow.StateFlow
import net.prototux.aare.car.CarData
import net.prototux.aare.media.MediaLibrary
import net.prototux.aare.media.MediaTransport
import net.prototux.aare.car.PassiveFixes
import net.prototux.aare.car.PhoneFixes
import net.prototux.aare.ui.car.CarActions
import net.prototux.aare.ui.car.CarAppEntry
import net.prototux.aare.ui.car.CarAppKind
import net.prototux.aare.ui.car.CarCall
import net.prototux.aare.ui.car.CarNotice
import net.prototux.aare.ui.car.CarUiState
import net.prototux.aare.ui.car.NowPlaying
import net.prototux.aare.Settings
import net.prototux.aare.car.FileSpeedLimits
import net.prototux.aare.car.PhoneFix
import net.prototux.aare.car.SpeedLimits
import net.prototux.aare.car.SpeedRules
import net.prototux.aare.car.myplaySpeedLimitMargin

/**
 * MyPlay (plan phase N, N2): what the engine's screens read and do, behind small interfaces, so
 * every screen can be drawn and walked in tests with fakes (which R4-2 could not do with AARE's
 * screens). The car's real ones read [CarUiState] and call [CarActions]; the phone list and the
 * mirror get their real sources with their worlds (N8, N9).
 */
interface MusicSource {
    val nowPlaying: NowPlaying?
    /** N7: what the playing source allows, and its next songs. */
    val transport: MediaTransport
    fun playPause()
    fun next()
    fun previous()
    /** N7: seek (only when [transport] allows it). */
    fun seekTo(positionMs: Long)
    /** N7: play a song of the queue. */
    fun skipTo(queueId: Long)
    /** The music app's own screen: its library, search and queue (the browser). */
    fun open()
    /** N7: the music app's library for the engine to draw, or null (then [open] is the way). */
    fun library(): MediaLibrary? = null
}

interface CallSource {
    val call: CarCall?
    fun answer()
    fun end()
}

interface NotificationSource {
    val notices: List<CarNotice>
    fun dismiss(notice: CarNotice)
    /** N8: the notifications of this drive, newest first (the list), and how many are not seen yet. */
    val history: List<CarNotice> get() = emptyList()
    val unseen: Int get() = 0
    fun markSeen() = Unit
    fun clearAll() = Unit
}

/** N6: the apps of the Apps world, in the owner's order, and opening one. */
interface AppsSource {
    val apps: List<CarAppEntry>
    fun open(app: CarAppEntry)
}

/** A person or number in the phone's recent calls or contacts. */
data class PhoneEntry(val name: String, val number: String, val detail: String = "", val missed: Boolean = false)

interface PhoneSource {
    /** MyPlay may read the calls and contacts and place calls (the phone's permissions). */
    val permitted: Boolean get() = false
    /** N8: the recent calls, newest first, one per number. */
    suspend fun recents(): List<PhoneEntry> = emptyList()
    /** N8: the starred contacts, or all of them if none is starred. */
    suspend fun favourites(): List<PhoneEntry> = emptyList()
    fun call(entry: PhoneEntry)
    /** N8: the phone app's own screen (calls, contacts, keypad). */
    fun open()
}

interface MirrorSource {
    val running: Boolean
    fun start()
    /** A key into the mirrored screen ([android.view.KeyEvent] action and code). */
    fun key(action: Int, keyCode: Int)
}

/**
 * N3: the speed's inputs: the car's data, and the phone's fixes when the rules allow them
 * (car/MyPlaySpeed.kt). With [fixed], that speed is shown and nothing is read or listened to
 * (the phone's picture of the car screen).
 */
class SpeedSource(
    val car: StateFlow<CarData.State>,
    val fixes: () -> PhoneFixes,
    val fixed: SpeedReading? = null,
    /** N20: the speed limit file's lookup (null: none), and the margin before the speed changes colour. */
    val limits: () -> SpeedLimits? = { null },
    val margin: () -> Int = { SpeedRules.MARGIN_DEFAULT },
)

/**
 * The sources a caller gives the engine instead of the car's own: the phone's picture of the car
 * screen provides [EngineSources.picture] here (N11).
 */
val LocalEngineSources = staticCompositionLocalOf<EngineSources?> { null }

/** Everything the engine reads and does, in one place. */
data class EngineSources(
    val music: MusicSource,
    val calls: CallSource,
    val notifications: NotificationSource,
    val phone: PhoneSource,
    val mirror: MirrorSource,
    /** Open the maps app (the turn slot's push), or the app grid when there is none. */
    val openMaps: () -> Unit,
    val speed: SpeedSource,
    val apps: AppsSource,
    /** N9: the Car world's lines (label, value), as the car data screen shows them; empty while no car is connected. */
    val carLines: @Composable () -> List<Pair<String, String>> = {
        val car by CarData.state.collectAsState()
        val proj by net.prototux.aare.service.ProjectionService.state.collectAsState()
        // N20, the owner's drive: why the speed limit shows or not, while a speed is on the screen.
        val why = SpeedLimitDiag.why
        net.prototux.aare.ui.car.carDataLines(car, proj) +
            listOfNotNull(why?.let { androidx.compose.ui.res.stringResource(net.prototux.aare.R.string.myplay_car_line_speed_limit) to it.line })
    },
    /** N9: the settings of the Settings world. */
    val settings: SettingsSource = object : SettingsSource { override fun items() = emptyList<SettingItem>() },
    /** The phone's picture of the car screen: the engine takes no focus and reads no speed. */
    val picture: Boolean = false,
) {
    companion object {
        /** The car's sources: the car state and actions; no phone list or mirror yet (N8, N9). */
        fun of(state: CarUiState, actions: CarActions, context: Context?) = EngineSources(
            music = object : MusicSource {
                override val nowPlaying get() = state.nowPlaying
                override val transport get() = state.transport
                override fun seekTo(positionMs: Long) = actions.mediaSeekTo(positionMs)
                override fun skipTo(queueId: Long) = actions.mediaSkipTo(queueId)
                override fun library() = actions.mediaLibrary()
                override fun playPause() = actions.mediaPlayPause()
                override fun next() = actions.mediaNext()
                override fun previous() = actions.mediaPrevious()
                override fun open() {
                    state.app(state.nowPlaying?.appId ?: state.activeMediaAppId)?.let(actions::openApp) ?: actions.openMusicApps()
                }
            },
            calls = object : CallSource {
                override val call get() = state.call
                override fun answer() = actions.answerCall()
                override fun end() = actions.endCall()
            },
            notifications = object : NotificationSource {
                override val notices get() = state.notices
                override fun dismiss(notice: CarNotice) = actions.dismissNotice(notice)
                override val history get() = state.history
                override val unseen get() = state.unseen
                override fun markSeen() { state.unseen = 0 }
                override fun clearAll() = state.history.clear()
            },
            phone = object : PhoneSource {
                override val permitted get() = context != null &&
                    PHONE_PERMISSIONS.all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
                override suspend fun recents() = withContext(Dispatchers.IO) {
                    context?.let(::recents).orEmpty().map { PhoneEntry(it.name, it.number, it.detail, missed = it.type == CallLog.Calls.MISSED_TYPE) }
                }
                override suspend fun favourites() = withContext(Dispatchers.IO) {
                    context?.let(::contacts).orEmpty().map { PhoneEntry(it.name, it.number, it.detail) }
                }
                // Placed by Android's telephony, so the sound goes through the car's hands-free link as usual.
                override fun call(entry: PhoneEntry) { if (entry.number.isNotBlank()) context?.let { Calls.place(it, entry.number) } }
                override fun open() { state.apps.firstOrNull { it.kind == CarAppKind.PHONE }?.let(actions::openApp) }
            },
            mirror = object : MirrorSource {
                override val running = false
                override fun start() = Unit
                override fun key(action: Int, keyCode: Int) = Unit
            },
            openMaps = { state.app(state.homeNavAppId ?: state.activeNavAppId)?.let(actions::openApp) ?: actions.openLauncher() },
            speed = SpeedSource(
                CarData.state, { context?.let(::PassiveFixes) ?: NoFixes },
                limits = { context?.let(::FileSpeedLimits) },
                margin = { context?.let { Settings(it).myplaySpeedLimitMargin } ?: SpeedRules.MARGIN_DEFAULT },
            ),
            settings = context?.let { CarSettingsSource(it, state) } ?: object : SettingsSource { override fun items() = emptyList<SettingItem>() },
            apps = object : AppsSource {
                // Already in the owner's order, as the app grid shows them (CarRuntime.applyApps).
                override val apps get() = state.launcherApps
                override fun open(app: CarAppEntry) = actions.openApp(app)
            },
        )

        /**
         * For the phone's picture of the car screen (N11): [state]'s sample data, the caller's
         * actions (no-ops there), a fixed [speed], no location and no focus taken.
         */
        fun picture(state: CarUiState, actions: CarActions, speed: SpeedReading = SpeedReading(48, SpeedReading.From.CAR)): EngineSources =
            of(state, actions, null).copy(speed = SpeedSource(MutableStateFlow(CarData.State()), { NoFixes }, speed), picture = true)
    }
}

/** No phone fixes at all. */
private object NoFixes : PhoneFixes {
    override fun start(onFix: (PhoneFix) -> Unit) = false
    override fun stop() = Unit
}
