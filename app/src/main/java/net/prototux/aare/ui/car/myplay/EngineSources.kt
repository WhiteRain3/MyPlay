package net.prototux.aare.ui.car.myplay

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
) {
    companion object {
        /** The car's sources: the car state and actions; no phone list or mirror yet (N8, N9). */
        fun of(state: CarUiState, actions: CarActions) = EngineSources(
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
        )
    }
}
