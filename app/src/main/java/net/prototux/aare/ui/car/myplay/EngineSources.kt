package net.prototux.aare.ui.car.myplay

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import net.prototux.aare.car.SpeedReading
import kotlinx.coroutines.flow.StateFlow
import net.prototux.aare.car.CarData
import net.prototux.aare.car.PassiveFixes
import net.prototux.aare.car.PhoneFixes
import net.prototux.aare.ui.car.CarActions
import net.prototux.aare.ui.car.CarCall
import net.prototux.aare.ui.car.CarNotice
import net.prototux.aare.ui.car.CarUiState
import net.prototux.aare.ui.car.NowPlaying

/**
 * MyPlay (plan phase N, N2): what the engine's screens read and do, behind small interfaces, so
 * every screen can be drawn and walked in tests with fakes (which R4-2 could not do with AARE's
 * screens). The car's real ones read [CarUiState] and call [CarActions]; the phone list and the
 * mirror get their real sources with their worlds (N8, N9).
 */
interface MusicSource {
    val nowPlaying: NowPlaying?
    fun playPause()
    fun next()
    fun previous()
    fun open()
}

interface CallSource {
    val call: CarCall?
    fun answer()
    fun end()
}

interface NotificationSource {
    val notices: List<CarNotice>
    fun dismiss(notice: CarNotice)
}

/** A person or number in the phone's recent calls or contacts. */
data class PhoneEntry(val name: String, val number: String)

interface PhoneSource {
    val recents: List<PhoneEntry>
    fun call(entry: PhoneEntry)
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
class SpeedSource(val car: StateFlow<CarData.State>, val fixes: () -> PhoneFixes, val fixed: SpeedReading? = null)

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
    /** The app grid, until the Apps world draws its own (N6). */
    val openApps: () -> Unit,
    val speed: SpeedSource,
    /** The phone's picture of the car screen: the engine takes no focus and reads no speed. */
    val picture: Boolean = false,
) {
    companion object {
        /** The car's sources: the car state and actions; no phone list or mirror yet (N8, N9). */
        fun of(state: CarUiState, actions: CarActions, context: Context?) = EngineSources(
            music = object : MusicSource {
                override val nowPlaying get() = state.nowPlaying
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
            },
            phone = object : PhoneSource {
                override val recents = emptyList<PhoneEntry>()
                override fun call(entry: PhoneEntry) = Unit
            },
            mirror = object : MirrorSource {
                override val running = false
                override fun start() = Unit
                override fun key(action: Int, keyCode: Int) = Unit
            },
            openMaps = { state.app(state.homeNavAppId ?: state.activeNavAppId)?.let(actions::openApp) ?: actions.openLauncher() },
            openApps = { actions.openLauncher() },
            speed = SpeedSource(CarData.state, { context?.let(::PassiveFixes) ?: NoFixes }),
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
    override fun start(onFix: (Float, Long) -> Unit) = false
    override fun stop() = Unit
}
