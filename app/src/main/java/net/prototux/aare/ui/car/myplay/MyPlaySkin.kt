package net.prototux.aare.ui.car.myplay

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.prototux.aare.host.templates.CarFormat
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
)

/** The places the engine offers a skin. */
enum class Slot { TURN, STATUS, HUB, SONG, CHOOSER, WORLD }

/** The next turn: the distance, then the road or instruction. Null fields are not known. */
data class TurnSlot(val distance: String?, val road: String?)

/** The trip: the arrival time, formatted by the engine in the phone's 12 or 24 hour style (N1-2), and the distance left. */
data class TripSlot(val arrival: String?, val distanceLeft: String?)

data class SongSlot(val title: String, val subtitle: String, val playing: Boolean, val art: Bitmap?)

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
                if (distance == null && road == null) null else TurnSlot(distance, road)
            } else state.phoneGuidance?.let { g ->
                TurnSlot(g.primary.takeIf { it.isNotBlank() }, g.secondary.takeIf { it.isNotBlank() })
            }
            val est = trip?.destinationTravelEstimates?.firstOrNull()
            val tripSlot = est?.let { e ->
                TripSlot(e.arrivalTimeAtDestination?.timeSinceEpochMillis?.let(time),
                    e.remainingDistance?.let { CarFormat.distance(it.displayDistance, it.displayUnit) })
            }
            val song = state.nowPlaying?.let { SongSlot(it.title, it.subtitle, it.playing, it.art) }
            return HomeSlots(turn, tripSlot, song)
        }
    }
}

interface MyPlaySkin {
    /** Stored in the settings once languages can be chosen (N15): never change it once released. */
    val id: String

    fun tokens(night: Boolean): SkinTokens

    @Composable fun Turn(turn: TurnSlot, modifier: Modifier)
    /** The hub: always there (the knob's way into the worlds), with the trip when there is a route. */
    @Composable fun Hub(trip: TripSlot?, modifier: Modifier)
    @Composable fun Song(song: SongSlot, modifier: Modifier)
    @Composable fun Status(modifier: Modifier)
    /** The six worlds, [selected] ringed. */
    @Composable fun Chooser(selected: World, modifier: Modifier)
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
