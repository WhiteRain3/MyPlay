package net.prototux.aare.ui.car.myplay

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.prototux.aare.car.SpeedReading
import androidx.car.app.model.Distance
import androidx.car.app.navigation.model.Maneuver
import net.prototux.aare.host.templates.CarFormat
import net.prototux.aare.media.LibraryRow
import net.prototux.aare.media.MediaTransport
import net.prototux.aare.ui.car.CarAppEntry
import net.prototux.aare.ui.car.CarCall
import net.prototux.aare.ui.car.CarNotice
import net.prototux.aare.ui.car.CarUiState
import net.prototux.aare.ui.theme.MapColourFilter

/**
 * MyPlay (plan phase N, N1): a skin draws MyPlay's car screen in one design language (ORBIT, DECK,
 * RACE). It gets its colours and sizes ([SkinTokens]) and one composable per slot, a place the
 * engine offers; it never decides what a push does. The engine gives each slot its data ready to
 * show ([HomeSlots]), so a skin is drawing only.
 */
data class SkinTokens(
    val background: Color,
    val surface: Color,
    val onSurface: Color,
    val dim: Color,
    val accent: Color,
    val onAccent: Color,
    val corner: Dp,
    /** Room between a slot and the screen's edge. */
    val edge: Dp = 16.dp,
    /**
     * The map colour filter this skin suggests (N3), used only while the chosen look sets none;
     * null leaves the map as the app draws it.
     */
    val mapFilter: MapColourFilter? = null,
    /** N5: whether these are the night colours, for skins that draw more than the tokens. */
    val night: Boolean = true,
)

/**
 * N5: whether the slot being drawn holds the knob's ring, for a skin that draws its own focus ring
 * ([MyPlaySkin.drawsFocus]) on the part it chooses (ORBIT: the round part only).
 */
val LocalStopRinged = staticCompositionLocalOf { false }

/** The places the engine offers a skin. */
enum class Slot { TURN, STATUS, SPEED, HUB, SONG, CHOOSER, WORLD }

/**
 * The next turn: the distance, then the road or instruction. Null fields are not known. [arrow] is
 * the maps app's own arrow picture when it sends one (phone guidance); else [direction], from the
 * route step's manoeuvre, when it is one MyPlay can draw; else neither (draw no direction at all).
 */
data class TurnSlot(val distance: String?, val road: String?, val arrow: Bitmap? = null, val direction: TurnDirection? = null)

/** The turns a skin can draw an arrow for; anything else gets no arrow (never a guessed one). */
enum class TurnDirection { STRAIGHT, SLIGHT_LEFT, LEFT, SHARP_LEFT, SLIGHT_RIGHT, RIGHT, SHARP_RIGHT, U_TURN_LEFT, U_TURN_RIGHT;
    companion object {
        /** From androidx.car.app.navigation.model.Maneuver's type. */
        fun of(type: Int?): TurnDirection? = when (type) {
            Maneuver.TYPE_STRAIGHT, Maneuver.TYPE_DEPART -> STRAIGHT
            Maneuver.TYPE_TURN_SLIGHT_LEFT, Maneuver.TYPE_KEEP_LEFT -> SLIGHT_LEFT
            Maneuver.TYPE_TURN_NORMAL_LEFT -> LEFT
            Maneuver.TYPE_TURN_SHARP_LEFT -> SHARP_LEFT
            Maneuver.TYPE_TURN_SLIGHT_RIGHT, Maneuver.TYPE_KEEP_RIGHT -> SLIGHT_RIGHT
            Maneuver.TYPE_TURN_NORMAL_RIGHT -> RIGHT
            Maneuver.TYPE_TURN_SHARP_RIGHT -> SHARP_RIGHT
            Maneuver.TYPE_U_TURN_LEFT -> U_TURN_LEFT
            Maneuver.TYPE_U_TURN_RIGHT -> U_TURN_RIGHT
            else -> null
        }
    }
}

/**
 * The trip: the arrival time, formatted by the engine in the phone's 12 or 24 hour style (N1-2),
 * the distance and time left, and how much of the route is behind (0..1; null until known).
 */
data class TripSlot(val arrival: String?, val distanceLeft: String?, val timeLeft: String? = null, val progress: Float? = null)

/**
 * The song; its position as the player last told it ([positionMs] at [updatedAtMs], moving while
 * [playing]). [updatedAtMs] is on SystemClock.elapsedRealtime(), as the player samples it.
 */
data class SongSlot(
    val title: String, val subtitle: String, val playing: Boolean, val art: Bitmap?,
    val positionMs: Long = 0, val durationMs: Long = 0, val updatedAtMs: Long = 0,
)

/**
 * N5: a route's progress from its distance left: the longest distance left seen on this route is
 * its length (the engine starts again when the route ends). Pure.
 */
object TripProgress {
    /** The new longest distance and the progress. */
    fun update(longest: Double?, left: Double?): Pair<Double?, Float?> {
        if (left == null || left < 0) return longest to null
        val l = maxOf(longest ?: 0.0, left)
        return l to if (l <= 0.0) null else (1 - left / l).toFloat().coerceIn(0f, 1f)
    }

    /** A Distance's value in metres (androidx.car.app.model.Distance's units). */
    fun meters(value: Double, unit: Int): Double? = when (unit) {
        Distance.UNIT_METERS -> value
        Distance.UNIT_KILOMETERS, Distance.UNIT_KILOMETERS_P1 -> value * 1000
        Distance.UNIT_MILES, Distance.UNIT_MILES_P1 -> value * 1609.344
        Distance.UNIT_FEET -> value * 0.3048
        Distance.UNIT_YARDS -> value * 0.9144
        else -> null
    }
}

/** N7: what the Music world shows. */
data class MusicPage(val song: SongSlot?, val transport: MediaTransport)

/** N7: one level of a music app's library, as the skin draws it. */
data class LibraryPage(val title: String, val rows: List<LibraryRow>, val loading: Boolean, val error: String?)

/** N8: one row of the Calls world. */
data class PhoneRow(val title: String, val subtitle: String, val kind: Kind) {
    enum class Kind { RECENT, MISSED, FAVOURITE, PHONE_APP }
}

/** N8: what the Calls world shows; [permitted] false: the phone's permissions are missing. */
data class PhonePage(val rows: List<PhoneRow>, val loading: Boolean, val permitted: Boolean)

/** N8: one row of the notifications list; [action] is the label a push runs, if any. Null [notice]: Clear all. */
data class NoticeRow(val notice: CarNotice?, val text: String, val time: String, val action: String?)

/** N8: what the notifications list shows. */
data class NoticeListPage(val rows: List<NoticeRow>)

/** N9: one row of the Settings world: its title, the value shown ("On", "Dark"), and whether it is a switch that is on. */
data class SettingRow(val title: String, val value: String, val switch: Boolean, val on: Boolean, val note: String? = null)

data class SettingsPage(val rows: List<SettingRow>)

/** N8: a notice's stops (also their test tags). */
const val NOTICE_DISMISS = "notice_dismiss"
fun noticeActionStop(i: Int) = "notice_action_$i"

/** N8: the stops of a ringing call (also their test tags). */
const val CALL_ANSWER = "call_answer"
const val CALL_DECLINE = "call_decline"

/** N7: the Music world's stops (also their test tags). */
const val MUSIC_ART = "music_art"
const val MUSIC_PREV = "music_prev"
const val MUSIC_PLAY = "music_play"
const val MUSIC_NEXT = "music_next"
const val MUSIC_LIBRARY = "music_library"
fun upNextStop(queueId: Long) = "music_up_$queueId"

/** O1: the music panel's stops (and their test tags). */
const val PANEL_SEEK = "panel_seek"
const val PANEL_PREV = "panel_prev"
const val PANEL_PLAY = "panel_play"
const val PANEL_NEXT = "panel_next"
const val PANEL_APP = "panel_app"

/** What the home screen's slots show; null where there is nothing (no route, no song). */
data class HomeSlots(val turn: TurnSlot?, val trip: TripSlot?, val song: SongSlot?) {
    companion object {
        /** [time] formats an epoch time as the phone shows times (12 or 24 hours). */
        fun from(state: CarUiState, time: (Long) -> String): HomeSlots {
            val trip = state.trip
            val turn = if (trip != null) {
                val step = trip.steps.firstOrNull()
                val distance = trip.stepTravelEstimates.firstOrNull()?.remainingDistance?.let { CarFormat.distance(it.displayDistance, it.displayUnit) }
                val road = (step?.road ?: step?.cue ?: trip.currentRoad)?.toString()?.takeIf { it.isNotBlank() }
                if (distance == null && road == null) null else TurnSlot(distance, road, direction = TurnDirection.of(step?.maneuver?.type))
            } else state.phoneGuidance?.let { g ->
                TurnSlot(g.primary.takeIf { it.isNotBlank() }, g.secondary.takeIf { it.isNotBlank() }, arrow = g.arrow)
            }
            val est = trip?.destinationTravelEstimates?.firstOrNull()
            val tripSlot = est?.let { e ->
                TripSlot(e.arrivalTimeAtDestination?.timeSinceEpochMillis?.let(time),
                    e.remainingDistance?.let { CarFormat.distance(it.displayDistance, it.displayUnit) },
                    e.remainingTimeSeconds.takeIf { it >= 0 }?.let { CarFormat.duration(it) })
            }
            val song = state.nowPlaying?.let { SongSlot(it.title, it.subtitle, it.playing, it.art, it.positionMs, it.durationMs, it.updatedAtMs) }
            return HomeSlots(turn, tripSlot, song)
        }
    }
}

interface MyPlaySkin {
    /** Stored in the settings once languages can be chosen (N15): never change it once released. */
    val id: String

    fun tokens(night: Boolean): SkinTokens

    @Composable fun Turn(turn: TurnSlot, modifier: Modifier)
    /**
     * N5: whether the skin draws the knob's ring itself (reading [LocalStopRinged] in each slot);
     * otherwise the engine draws a plain one around the whole slot.
     */
    val drawsFocus: Boolean get() = false

    /** N5: drawn over the map and under the slots, e.g. a shade in the corners so their text reads. */
    @Composable fun Backdrop(modifier: Modifier) = Unit

    /**
     * N13: a skin whose home is a hand of cards (DECK) instead of the hub and its dial (ORBIT): the
     * worlds it holds, in the order a knob turn walks them. At home a turn swings the hand (past
     * either end it goes on to the other home stops); a push opens the front card's world; Back
     * comes home with that card in front. Null: the hub and the dial.
     */
    val hand: List<World>? get() = null

    /**
     * N13: the hand at home, bottom right, with [front] in front; the music card carries [song].
     * Every card takes taps with [pick]: the front card opens, any other comes to the front (so a
     * tap is never caught by a card behind the one that was hit). Only for a skin with a [hand].
     */
    @Composable fun Hand(front: World, song: SongSlot?, ringed: Boolean, pick: (World) -> Unit, modifier: Modifier) = Unit

    /** N13: the trip under the turn read-out (a skin with a [hand]; ORBIT's hub shows it instead). */
    @Composable fun Trip(trip: TripSlot, modifier: Modifier) = Unit

    /**
     * F1: the narrow rail beside a full-screen app, on the driver's side, with the way home
     * ([home]). Its width is the skin's; the app takes the rest of the screen.
     */
    @Composable fun AppRail(home: () -> Unit, modifier: Modifier) = DefaultAppRail(home, modifier)

    /**
     * O3: whether an app has the rail beside it. Without it ([false], ORBIT) the app takes the
     * whole screen, grows out of the stop it was opened from, and Back shrinks it back there
     * ([EngineMemory]); the way home is Back, the car's Home (Menu) and holding Back.
     */
    val appRail: Boolean get() = true

    /** The hub: always there (the knob's way into the worlds), with the trip when there is a route. */
    @Composable fun Hub(trip: TripSlot?, modifier: Modifier)
    @Composable fun Song(song: SongSlot, modifier: Modifier)
    @Composable fun Status(modifier: Modifier)

    /**
     * N3: whether this skin shows the speed (under the status). Only while it is drawn does the
     * engine read a speed at all; the phone's location is used only then (car/MyPlaySpeed.kt).
     */
    val showsSpeed: Boolean get() = false

    /** The speed, or null when none is known (show a dash, never a guess). */
    @Composable fun Speed(reading: SpeedReading?, modifier: Modifier) = Unit
    /** The six worlds, [selected] ringed. */
    @Composable fun Chooser(selected: World, modifier: Modifier)
    /** N6: how many apps one page of the Apps world holds. */
    val appsPerPage: Int get() = 10

    /**
     * N6: the Apps world, one page of [apps]; [ringed] is the one holding the ring (an index into
     * [apps]). Each app's part takes [tile] (its index), which the engine uses for the knob, the
     * pad and taps; the skin draws the ring on [ringed].
     */
    @Composable fun AppsWorld(apps: List<CarAppEntry>, ringed: Int, page: Int, pages: Int, tile: (Int) -> Modifier, modifier: Modifier)

    /**
     * N7: the Music world: the song (null when nothing plays), its buttons, the next songs, and the
     * way to the music app's library. Each stop takes `stop(id)` with its id ([MUSIC_ART],
     * [MUSIC_PREV], [MUSIC_PLAY], [MUSIC_NEXT], [MUSIC_LIBRARY], [upNextStop]); the skin draws
     * the ring on [ringed]. The artwork carries the progress bar: left and right seek there (R5-5).
     */
    @Composable fun MusicWorld(page: MusicPage, ringed: String, stop: (String) -> Modifier, modifier: Modifier)

    /**
     * O1: the music panel, opened by a push on the home's song and drawn above it: the music app
     * ([PANEL_APP]), previous, play/pause and next ([PANEL_PREV], [PANEL_PLAY], [PANEL_NEXT]) and,
     * when the player can seek, the progress bar ([PANEL_SEEK], left and right seek there). Each
     * stop takes `stop(id)`; the skin draws the ring on [ringed]. Only a skin with a song slot
     * (no [hand]) is asked to draw it.
     */
    @Composable fun MusicPanel(page: MusicPage, ringed: String, stop: (String) -> Modifier, modifier: Modifier) = Unit

    /**
     * N7: a music app's library: one level's rows ([LibraryPage]); [ringed] holds the ring. Each row
     * takes `row(i)`; [art] loads a row's artwork. The skin keeps the ringed row in view.
     */
    @Composable fun Library(page: LibraryPage, ringed: Int, art: suspend (LibraryRow, Int) -> Bitmap?, row: (Int) -> Modifier, modifier: Modifier)

    /**
     * N8: a call ringing, over everything: Answer ([CALL_ANSWER]) and, when MyPlay may end calls
     * ([canEnd]), Decline ([CALL_DECLINE]); each takes `stop(id)`; the ring is on [ringed].
     */
    @Composable fun Ringing(call: CarCall, canEnd: Boolean, ringed: String, stop: (String) -> Modifier, modifier: Modifier)

    /**
     * N8: a call in progress: a strip at the top of the home with the caller and the time, which
     * opens the phone ([STOP_CALL]), and Hang up ([STOP_HANG_UP]) when [canEnd]; [ringed] as on the home.
     */
    @Composable fun InCall(call: CarCall, canEnd: Boolean, ringed: String?, stop: (String) -> Modifier, modifier: Modifier)

    /**
     * N8: the Calls world: recent calls, favourites and, last, the phone app (keypad, all contacts).
     * Each row takes `row(i)`; the ring is on [ringed]; the skin keeps it in view.
     */
    @Composable fun PhoneWorld(page: PhonePage, ringed: Int, row: (Int) -> Modifier, modifier: Modifier)

    /**
     * N8: a notice (a message, a reminder) at the top for a few seconds: its icon, title and text
     * ([hideText] while driving for a message), its actions ([noticeActionStop]) and Dismiss
     * ([NOTICE_DISMISS]); each takes `stop(id)`; [ringed] as on the home.
     */
    @Composable fun Notice(notice: CarNotice, hideText: Boolean, ringed: String?, stop: (String) -> Modifier, modifier: Modifier)

    /** N8: the bell under the clock with the number of unseen notifications ([count] > 0; the engine hides it at 0). */
    @Composable fun Bell(count: Int, ringed: Boolean, modifier: Modifier)

    /**
     * N8: the notifications list over the home: one row per notification and, last, Clear all.
     * Each row takes `row(i)`; the ring is on [ringed]; the skin keeps it in view.
     */
    @Composable fun NoticeList(page: NoticeListPage, ringed: Int, row: (Int) -> Modifier, modifier: Modifier)

    /** N9: the Car world: the car's data, one row per line, to read (nothing to push); the ring is on [ringed]. */
    @Composable fun CarWorld(lines: List<Pair<String, String>>, ringed: Int, row: (Int) -> Modifier, modifier: Modifier)

    /** N9: the Settings world: one row per setting, its value shown; each row takes `row(i)`; the ring is on [ringed]. */
    @Composable fun SettingsWorld(page: SettingsPage, ringed: Int, row: (Int) -> Modifier, modifier: Modifier)

    /** A world not drawn yet by this skin (the chunks after N1 replace it world by world). */
    @Composable fun WorldPlaceholder(world: World, modifier: Modifier)

    /**
     * N3: whether [slot] covers the map, so the maps app keeps its arrow and its own signs out of
     * it. The corner slots do; the chooser and a world's page come and go over the middle, and
     * counting them would make the map jump each time, so by default they do not.
     */
    fun coversMap(slot: Slot): Boolean = slot != Slot.CHOOSER && slot != Slot.WORLD
}

internal val LocalSkinTokens = staticCompositionLocalOf<SkinTokens> { error("no MyPlay skin") }
