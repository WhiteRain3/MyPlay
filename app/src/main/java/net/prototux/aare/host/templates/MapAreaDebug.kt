package net.prototux.aare.host.templates

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.core.content.edit
import net.prototux.aare.Settings

/**
 * MyPlay: what MyPlay tells a hosted maps app about the free part of its map, made visible, for
 * the owner's report that Waze is not centred in some layouts and its speed sign is cut at an edge.
 *
 *  - Every change of the "visible area" or "stable area" goes into the connection log, one line
 *    each ([line]), in the map surface's own pixels as the app gets them, with the map's size and
 *    where it sits on the screen. The Car check's shared log carries them.
 *  - With the setting [myplayDebugMapAreas] on, the two areas are outlined over the map on the car
 *    screen ([MapAreaOutline]): the visible area in solid green, the stable area dashed yellow.
 */
internal object MapAreaDebug {
    /** Whether the outlines are drawn; set from the setting by the car runtime. */
    var show by mutableStateOf(false)

    /** At most this many lines per map, so a map whose cards keep moving does not fill the log. */
    const val MAX_LINES = 60

    /**
     * One line for the connection log. [mapRoot] is the map's rectangle on the screen (px);
     * [visible] and [stable] are in the map surface's pixels (left, top, right, bottom), [stable]
     * null when it was not sent (pan mode).
     */
    fun line(mapRoot: MapGeometry.Box, visible: List<Int>, stable: List<Int>?): String {
        val w = Math.round(mapRoot.width)
        val h = Math.round(mapRoot.height)
        return "map area: map ${w}x$h px at ${Math.round(mapRoot.left)},${Math.round(mapRoot.top)} on the screen; " +
            "visible ${rect(visible)}" + (stable?.let { "; stable ${rect(it)}" } ?: "; stable unchanged (moving the map)")
    }

    private fun rect(r: List<Int>) = "${r[0]},${r[1]} to ${r[2]},${r[3]} (${r[2] - r[0]}x${r[3] - r[1]})"
}

/** MyPlay: outline the areas last told to the app (map surface px, as this box's own). */
@Composable
internal fun MapAreaOutline(visible: List<Int>?, stable: List<Int>?) {
    if (!MapAreaDebug.show) return
    Canvas(Modifier.fillMaxSize()) {
        val width = 4f
        fun box(r: List<Int>?, color: Color, dashed: Boolean) {
            r ?: return
            drawRect(
                color, Offset(r[0] + width / 2, r[1] + width / 2),
                Size((r[2] - r[0] - width).coerceAtLeast(0f), (r[3] - r[1] - width).coerceAtLeast(0f)),
                style = Stroke(width, pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(24f, 16f)) else null),
            )
        }
        box(visible, Color(0xFF00E676), dashed = false)
        box(stable, Color(0xFFFFEA00), dashed = true)
    }
}

/** MyPlay: outline the map's visible and stable areas on the car screen (a check for the owner). */
var Settings.myplayDebugMapAreas: Boolean
    get() = prefs.getBoolean("myplay_debug_map_areas", false)
    set(v) = prefs.edit { putBoolean("myplay_debug_map_areas", v) }
