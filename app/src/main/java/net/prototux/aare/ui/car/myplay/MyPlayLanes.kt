package net.prototux.aare.ui.car.myplay

import androidx.car.app.navigation.model.Lane
import androidx.car.app.navigation.model.LaneDirection
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * MyPlay (the owner's request, 2026-11-23): the lanes for the next turn on ORBIT, when the maps app
 * sends them (androidx.car.app Step.lanes, in its Trip or its navigation template's turn card).
 * Nothing is shown, and nothing changes, when it sends none. [shape] is the LaneDirection shape the
 * lane is drawn with (the recommended direction if it has one, else its first), [recommended]
 * whether the app recommends it.
 */
data class LaneHint(val shape: Int, val recommended: Boolean)

object LaneHints {
    /** More lanes than this are not drawn (a sign of something odd; the arrows would be too small). */
    const val MAX = 8

    fun from(lanes: List<Lane>?): List<LaneHint> {
        if (lanes.isNullOrEmpty() || lanes.size > MAX) return emptyList()
        return lanes.map { lane ->
            val dirs = lane.directions
            val rec = dirs.firstOrNull { it.isRecommended }
            LaneHint((rec ?: dirs.firstOrNull())?.shape ?: LaneDirection.SHAPE_UNKNOWN, rec != null)
        }.takeIf { hints -> hints.any { it.recommended } }.orEmpty()
    }

    /** The lanes to show: the trip's if it has some, else the turn card's. */
    fun pick(trip: List<LaneHint>, template: List<LaneHint>): List<LaneHint> = trip.ifEmpty { template }
}

/**
 * Where the hosted maps app's turn card hands its lanes while the skin draws the turn itself
 * (provided by the engine round the home map, as [LocalMapAlertSink]); unset, nothing is handed.
 */
val LocalLaneSink = staticCompositionLocalOf<((List<LaneHint>) -> Unit)?> { null }
