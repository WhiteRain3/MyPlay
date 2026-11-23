package net.prototux.aare.ui.car.myplay

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap

/**
 * MyPlay (AL-1, the owner's photo: Waze's alert card covered ORBIT's turn): an alert from the maps
 * app on the home (a car-library Alert: "In 380 m · Reduced speed · Close"), handed to the engine
 * so the skin draws it in its own style and place instead of the default card. [actions] run the
 * app's action and dismiss the alert; [key] tells one alert from the next.
 */
data class MapAlert(
    val key: Int,
    val title: String,
    val subtitle: String?,
    val icon: ImageBitmap?,
    val actions: List<Pair<String, () -> Unit>>,
    /** Look C (design 20): how long the alert shows before it goes by itself (the app's durationMillis); 0: until dismissed. */
    val durationMs: Long = 0,
    /** When it was first shown (SystemClock.uptimeMillis), so its countdown is right even if it is drawn later. */
    val shownAtMs: Long = 0,
)

/**
 * Look C (design 20): the share of [alert]'s time still to run at [nowMs] (1 when it appears, 0
 * when it goes); null for an alert with no time limit, which has no countdown.
 */
fun alertTimeLeft(alert: MapAlert, nowMs: Long): Float? =
    if (alert.durationMs <= 0) null else (1f - (nowMs - alert.shownAtMs).toFloat() / alert.durationMs).coerceIn(0f, 1f)

/**
 * Where the home's map hands its app's alert (null: none now). Provided by the engine round the
 * home map while the skin draws alerts itself ([MyPlaySkin.drawsMapAlerts]); unset, the map draws
 * its own default card as before.
 */
val LocalMapAlertSink = staticCompositionLocalOf<((MapAlert?) -> Unit)?> { null }

/** A map alert's action button (its test tag and home stop). */
fun mapAlertStop(i: Int) = "map_alert_action_$i"

/** MC-2: where the alert pill goes: [bottom] px above the screen's bottom, at most [maxWidth] px wide, centred. */
internal data class AlertPlace(val bottom: Float, val maxWidth: Float)

/** MC-2: the share of the screen's height round its middle (as a radius) that the pill keeps clear: the maps app's arrow is there. */
internal const val ALERT_KEEP_CLEAR = 0.12f

/**
 * MC-2 (the dispatcher: the alert pill must never sit on Waze's arrow, which is now in the very
 * middle, MC-1): the pill goes at the bottom of the centred free area, just above its bottom
 * margin (MapGeometry's, a share of the height), far below the keep-clear zone round the middle;
 * centred, and no wider than the room between the bottom parts beside it ([left], the hub, and
 * [right], the song, whole with its text), less the same gutter as the free area. A part not given,
 * or above the middle, does not narrow it. On a 1280x720 screen: 43 px up, 428 px wide at most.
 */
internal fun alertPlace(w: Float, h: Float, left: FocusGraph.Box?, right: FocusGraph.Box?, minGutter: Float): AlertPlace {
    val g = net.prototux.aare.host.templates.MapGeometry
    val gutter = maxOf(minGutter, w * g.CENTRED_GUTTER)
    val cx = w / 2
    val cy = h / 2
    val l = left?.takeIf { it.bottom > cy }?.right ?: 0f
    val r = right?.takeIf { it.bottom > cy }?.left ?: w
    val room = (minOf(cx - l, r - cx) - gutter).coerceAtLeast(w * 0.15f)
    return AlertPlace(bottom = h * g.CENTRED_MARGIN, maxWidth = 2 * room)
}
