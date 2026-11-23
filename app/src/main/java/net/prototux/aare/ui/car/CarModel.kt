package net.prototux.aare.ui.car

import android.graphics.Bitmap
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.prototux.aare.car.NextEvent
import net.prototux.aare.car.Weather
import net.prototux.aare.notify.PhoneGuidance
import net.prototux.aare.ui.theme.AareTheme

/**
 * What kind of app an entry in the launcher is, which decides where its UI goes. [EXIT] has no UI:
 * it hands the screen back to the car's own.
 */
enum class CarAppKind { NAVIGATION, MEDIA, TEMPLATED, ASSISTANT, MESSAGING, PHONE_SCREEN, BROWSER, CAR_DATA, STANDALONE, PHONE, SETTINGS, NOTIFICATIONS, EXIT }

/** One launchable app on the car screen. */
@Stable
class CarAppEntry(
    val id: String,
    val packageName: String,
    val label: String,
    val icon: Bitmap?,
    val kind: CarAppKind,
)

@Stable
class NowPlaying(
    val appId: String,
    val title: String,
    val subtitle: String,
    val art: Bitmap?,
    val playing: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAtMs: Long,
)

/** The car UI's navigation: the dashboard, the app grid, or one app full screen. */
sealed interface CarScreen {
    data object Dashboard : CarScreen
    data object Launcher : CarScreen
    data class App(val appId: String) : CarScreen
}

/** A transient card, e.g. an incoming message or a toast from a car app. */
@Stable
class CarNotice(
    val key: String,
    val title: String,
    val text: String,
    val icon: Bitmap?,
    val actions: List<Pair<String, () -> Unit>> = emptyList(),
    val timeMs: Long = System.currentTimeMillis(),
    /** A message from a contact: its text is hidden while driving, like in the messaging screens. */
    val message: Boolean = false,
    /**
     * Polish #4: the driver asked for it this moment (Call back's "Call"): the ring goes onto its
     * first button, so one push answers. A notice nobody asked for puts the ring on Dismiss instead.
     */
    val asked: Boolean = false,
)

/** Head-unit geometry: the drawable area inside the video margins, and the rail side. */
data class CarGeometry(
    val widthPx: Int,
    val heightPx: Int,
    val dpi: Int,
    /** Rail on the left (left-hand drive) or right. */
    val railStart: Boolean = true,
)

/**
 * All state the car UI reads. The session and hosts write it from the main thread.
 */
@Stable
class CarUiState {
    /**
     * MyPlay (Preview): the phone's interactive Preview's pretend speed (CarRuntime.pretendSpeed);
     * null in a car session, which never pretends. Set once, when the runtime is made.
     */
    var pretendSpeed: net.prototux.aare.car.PretendSpeed? = null
        internal set

    var night by mutableStateOf(true)
    var screen by mutableStateOf<CarScreen>(CarScreen.Dashboard)
    /**
     * Every app the car can open, in the user's order: also the built-ins hidden from the app grid,
     * since the bell, the call card, the rail's phone button and the steering-wheel keys open them
     * from here. The grids show [launcherApps].
     */
    val apps = mutableStateListOf<CarAppEntry>()
    /** Ids of built-in apps (CarRuntime.HIDEABLE_BUILTIN_IDS) the user hid from the app grid only. */
    var launcherHidden by mutableStateOf<Set<String>>(emptySet())
    /** What the app grid and launcher list show: [apps] without the ones in [launcherHidden]. */
    val launcherApps: List<CarAppEntry> get() =
        apps.withoutLauncherHidden(launcherHidden).onlyKinds(launcherKinds.takeIf { screen == CarScreen.Launcher })
    /**
     * MyPlay (K10): when set, the opened app grid lists only apps of these kinds (the Music button's
     * list). Only while the screen is the grid: the grid drawn as the home screen always shows all.
     */
    var launcherKinds by mutableStateOf<Set<CarAppKind>?>(null)
    var nowPlaying by mutableStateOf<NowPlaying?>(null)
    /** The navigation app whose map is shown on the dashboard. */
    var activeNavAppId by mutableStateOf<String?>(null)
    /** The media app the dashboard's media card controls. */
    var activeMediaAppId by mutableStateOf<String?>(null)
    val notices = mutableStateListOf<CarNotice>()
    var drivingRestricted by mutableStateOf(false)
    var headUnitName by mutableStateOf("")
    var listening by mutableStateOf(false)
    /** Recent notifications (messages), newest first, for the notification centre. */
    val history = mutableStateListOf<CarNotice>()
    /** Notifications not seen in the notification centre yet. */
    var unseen by mutableStateOf(0)
    /** The dashboard's wallpaper: a built-in name, or "custom" with [wallpaperImage]. */
    var wallpaper by mutableStateOf("none")
    var wallpaperImage by mutableStateOf<Bitmap?>(null)
    /** The hosted navigation app's trip (next step, distances), for the cluster's turn card. */
    var trip by mutableStateOf<androidx.car.app.navigation.model.Trip?>(null)
    /** The package that sent [trip]: its maneuver icons are opened on that app's behalf. */
    var tripPackage by mutableStateOf<String?>(null)
    /** The weather next to the clock, if known and wanted. */
    var weather by mutableStateOf<Weather?>(null)
    /** The next calendar event, for the dashboard. */
    var nextEvent by mutableStateOf<NextEvent?>(null)
    /** The phone call ringing or on, if any. */
    var call by mutableStateOf<CarCall?>(null)
    /** MyPlay (R6): the last button gesture's line (GestureLine.kt). */
    var gestureLine by mutableStateOf<GestureLine?>(null)
    /** MyPlay (N1): draw with MyPlay's own engine (ui/car/myplay/), a hidden preview while phase N is built. */
    var enginePreview by mutableStateOf(false)
    /** MyPlay (N13): the new screen's design language ("orbit", "deck"; MyPlaySkins). */
    var engineLanguage by mutableStateOf("orbit")
    /** MyPlay (O2): seconds before a panel over the map closes by itself; 0 never (PanelTimeout). */
    var panelTimeoutS by mutableStateOf(net.prototux.aare.ui.car.myplay.PanelTimeout.DEFAULT)
    /** MyPlay (O3): Home was asked for (not Back): the new screen comes back to its plain home, not where it was. */
    var engineHomeAsked by mutableStateOf(false)
    /** MyPlay (O2b): minutes before a maps app full screen goes home without input; 0 never. */
    var mapsIdleHomeMin by mutableStateOf(0)
    /** MyPlay (O11): ORBIT's home shows information only, no knob ring (with the home pad on). */
    var infoHome by mutableStateOf(false)
    /**
     * MyPlay (G-4): something holds the knob that a push must answer at once (the music panel, a
     * map alert's button); with a call or a notice with buttons, the car side then takes the
     * knob's push as a plain press (no double or triple, no wait).
     */
    var knobBusy by mutableStateOf(false)

    /** TH-1: the owner chose a theme, an accent or own colours, so ORBIT takes [theme]'s colours. */
    var orbitThemed by mutableStateOf(false)

    /** MyPlay (SH-1): soft plates behind ORBIT's parts (true), or none. */
    var orbitShadeSoft by mutableStateOf(true)
    /** MyPlay (drive warnings): quiet road and phone warnings wanted (myplay_drive_warnings). */
    var driveWarnings by mutableStateOf(true)
    /** MyPlay (drive warnings): the car reports 4 °C or less outside (IceWarning); shown only with [driveWarnings]. */
    var iceRisk by mutableStateOf(false)
    /** MyPlay (LP-1): the limit sign pulses once when the limit changes (myplay_limit_pulse). */
    var limitPulse by mutableStateOf(true)
    /** MyPlay (#5): Home and the saved places, for the Navigate world's list (Places.destinations); empty: none. */
    var navPlaces by mutableStateOf<List<net.prototux.aare.car.SavedPlace>>(emptyList())
    /** MyPlay (O6): what ORBIT's home pad opens while the ring is hidden; empty: the ring always (as in tests). */
    var homePad by mutableStateOf<Map<net.prototux.aare.ui.car.myplay.HomePadKey, net.prototux.aare.ui.car.myplay.HomePadAction>>(emptyMap())
    /** MyPlay (O3): how often this drive has shown the way home over an app (the first few times only). */
    var homeHints by mutableStateOf(0)
    /**
     * MyPlay (F4): a world for the new screen to open, once (its id, as World.id: "calls"); the
     * engine opens it and clears this. Set by the car runtime (a call placed from an app).
     */
    var engineOpenWorld by mutableStateOf<String?>(null)
    /** MyPlay: an app shows something fullscreen (the browser's video): MyPlay's frame hides its rail. */
    var appFullscreen by mutableStateOf(false)
    /** MyPlay (N7): what the playing source allows (seek, previous, next) and its next songs. */
    var transport by mutableStateOf(net.prototux.aare.media.MediaTransport())
    /** MyPlay (N3): where the engine's speed comes from (car/MyPlaySpeed.kt). */
    var speedSource by mutableStateOf(net.prototux.aare.car.SpeedSourceMode.AUTO)
    /** The apps opened last, newest first (ids): the rail shows the recent ones. */
    val recentAppIds = mutableStateListOf<String>()
    /** The voice assistant showing as a bar over the screen (an app entry id), or null. */
    var assistantAppId by mutableStateOf<String?>(null)
    /** Turn-by-turn from a navigation app running on the phone (Google Maps, Waze…). */
    var phoneGuidance by mutableStateOf<PhoneGuidance?>(null)
    /** Right-hand drive: the rail goes to the right, next to the driver. */
    var railOnRight by mutableStateOf(false)
    /** The driver sits on the right (right-hand drive): the voice button goes on that side. */
    var driverOnRight by mutableStateOf(false)
    /** The rail stays dark even with day colors (a setting). */
    var darkBar by mutableStateOf(false)
    /** The rail's status widgets, in order (a setting). */
    var railWidgets by mutableStateOf(RailWidget.DEFAULT)
    /** The dashboard's cards, in order (a setting). */
    var dashboardCards by mutableStateOf(DashCard.DEFAULT)
    /** The weather next to the clock (a setting; the weather card is one of [dashboardCards]). */
    var weatherInBar by mutableStateOf(true)
    /** The theme (a setting, with the phone's accent colours if asked). */
    var theme by mutableStateOf(AareTheme)
    /** Where things go: a taskbar and dashboard, or a dock and app pages (a setting). */
    var layout by mutableStateOf(CarLayout.TASKBAR)
    /** The rail runs along the bottom instead of a side. */
    var railBottom by mutableStateOf(false)
    /** MyPlay (K9): which maps app the home screen's map shows (a setting: see HomeMaps). */
    var homeMaps by mutableStateOf(HomeMaps.LAST)
    /** MyPlay (K9): maps apps that showed nothing on the home screen, until they are opened again. */
    var navShowsNothing by mutableStateOf<Set<String>>(emptySet())
    /** MyPlay (K9): the maps app the home screen's map shows, or null for the maps buttons. */
    val homeNavAppId: String? get() = HomeMaps.appId(homeMaps, activeNavAppId, apps, navShowsNothing)
    /** MyPlay (F4): how the status icons and the clock look (settings). */
    var statusStyle by mutableStateOf(StatusStyle())
    /** MyPlay (M1/M3): the options of the "map behind" layout (a setting). */
    var layoutSpec by mutableStateOf(CarLayoutSpec.DEFAULT)
    /** Font-scale multiplier for the car screen's text (a setting). Independent of [controlScale]. */
    var textScale by mutableStateOf(1f)
    /** Multiplier for [net.prototux.aare.ui.theme.CarDimens] (touch targets, icons…) (a setting). */
    var controlScale by mutableStateOf(1f)

    fun app(id: String?): CarAppEntry? = apps.firstOrNull { it.id == id }
}

/** These apps without the [hidden] ones, for the app grid: everything else still reaches them. */
/**
 * MyPlay (K10): only the entries of [kinds]; all of them when [kinds] is null, or when none is of
 * those kinds (an empty grid would leave the driver with nothing to choose).
 */
internal fun List<CarAppEntry>.onlyKinds(kinds: Set<CarAppKind>?): List<CarAppEntry> {
    if (kinds == null) return this
    return filter { it.kind in kinds }.ifEmpty { this }
}

internal fun List<CarAppEntry>.withoutLauncherHidden(hidden: Set<String>): List<CarAppEntry> =
    if (hidden.isEmpty()) this else filter { it.id !in hidden }

/** A phone call, ringing or on, for the car's call card. */
@Stable
data class CarCall(val ringing: Boolean, val name: String, val sinceMs: Long)

/** Actions the car UI asks for; implemented by the session. */
interface CarActions {
    fun openApp(app: CarAppEntry)
    fun goHome()
    fun openLauncher()
    /** MyPlay (K10): the app grid with music apps only (Music pressed with no music app remembered). */
    fun openMusicApps() = openLauncher()
    fun assistant()
    fun mediaPlayPause()
    fun mediaNext()
    fun mediaPrevious()
    /** MyPlay (N7): seek the playing song; nothing where the car cannot. */
    fun mediaSeekTo(positionMs: Long) = Unit
    /** MyPlay (N7): play a song of the queue. */
    fun mediaSkipTo(queueId: Long) = Unit
    /** MyPlay (N7): the playing (or last) music app's library, for MyPlay's own screen; null when there is none. */
    fun mediaLibrary(): net.prototux.aare.media.MediaLibrary? = null
    fun dismissNotice(notice: CarNotice)
    fun answerCall()
    fun endCall()
    /** Navigate to a place (an address or a name) in the car's navigation app. */
    fun navigateTo(place: String)
    /** MyPlay (#10): offer the last missed call on the car screen, to call back with one push. */
    fun callBackLastMissed() = Unit
    /** HN-3: the maps app to the home address set on the phone; without one, a notice says so. */
    fun navigateHome() = Unit
}
