package net.prototux.aare.host.templates

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import kotlin.math.roundToInt

/**
 * MyPlay (M2): what MyPlay's own screen draws over a hosted maps app's picture, for any layout.
 *
 * A maps app is told two areas (the car library's "visible area" and "stable area"), and a well
 * made one centres its position arrow in them. Until now only the map template's own cards and
 * strips counted. With the map behind everything (plan phase M), MyPlay's bar and cards also lie
 * over it. A screen that draws them provides a [MapCoverRegistry] through [LocalMapCovers] and
 * marks each of them:
 *  - [coversMap] for something drawn over the map now (a card that is showing, the bar);
 *  - [coversMap] with `now = false` (or [mayCoverMap]) for a place where something may appear (the
 *    slot of the music card while no music is playing), so the stable area already leaves it out
 *    and the map does not jump when the card appears. A card that comes and goes in one place
 *    passes whether it shows as `now`, from the same call, so its rectangle moves between the two
 *    lists even when the card does not move on screen (M2-1).
 *
 * Nothing here knows any particular layout: it only collects rectangles. Without a registry (all
 * of today's layouts), both modifiers do nothing and the areas are as before.
 */
internal class MapCoverRegistry {
    /** Drawn over the map now. */
    val shown = mutableStateMapOf<Any, Rect>()
    /** May be drawn over the map (also everything in [shown]). */
    val reserved = mutableStateMapOf<Any, Rect>()

    /** [key] is at [rect]; it covers the map now when [now]. */
    fun place(key: Any, rect: Rect, now: Boolean) {
        reserved[key] = rect
        if (now) shown[key] = rect else shown.remove(key)
    }

    /** [key] now covers the map or not, at the place it was last given ([place]); nothing if it has none yet. */
    fun setNow(key: Any, now: Boolean) {
        val rect = reserved[key] ?: return
        if (now) shown[key] = rect else shown.remove(key)
    }

    /**
     * MC-1: the skin wants the free area centred on the screen ([MapGeometry.centred]): only the
     * hard parts it marks count, and the app's arrow sits in the middle.
     */
    var centred by androidx.compose.runtime.mutableStateOf(false)

    fun remove(key: Any) {
        shown.remove(key)
        reserved.remove(key)
    }
}

internal val LocalMapCovers = staticCompositionLocalOf<MapCoverRegistry?> { null }

/**
 * Marks this as a place over the map: drawn over it now when [now], else only a place where
 * something may appear. See [MapCoverRegistry]. Bounds in root coordinates, as the map's.
 */
@Composable
internal fun Modifier.coversMap(now: Boolean = true): Modifier {
    val registry = LocalMapCovers.current ?: return this
    val key = remember { Any() }
    val nowState = rememberUpdatedState(now)
    DisposableEffect(registry) { onDispose { registry.remove(key) } }
    // M2-1: when only [now] changes, the place does not move and is not positioned again, so the
    // last known rectangle is moved between the two lists here.
    LaunchedEffect(registry, now) { registry.setNow(key, now) }
    return onGloballyPositioned { registry.place(key, it.boundsInRoot(), nowState.value) }
}

/** Marks this place as one where something may be drawn over the map: `coversMap(now = false)`. */
@Composable
internal fun Modifier.mayCoverMap(): Modifier = coversMap(now = false)

/** MyPlay (M2): the two areas a maps app is told, worked out from what covers its map. Pure. */
internal object MapAreas {
    data class Areas(val visible: MapGeometry.Box, val stable: MapGeometry.Box)

    /**
     * [map] is the map's own rectangle; [shown] covers it now; [reserved] may cover it (whatever
     * is in [shown] counts as reserved too). The visible area is the largest free rectangle
     * around [shown]; the stable area is the largest free rectangle inside the visible area that
     * is clear of every reserved place (M2-2), so it always nests in the visible area.
     */
    fun compute(map: MapGeometry.Box, shown: List<MapGeometry.Box>, reserved: List<MapGeometry.Box>, centred: Boolean = false, minGutter: Float = 0f): Areas {
        if (centred) {
            // MC-1: both centred on the screen; the stable one inside the visible one (M2-2).
            val gutter = maxOf(minGutter, map.width * MapGeometry.CENTRED_GUTTER)
            val visible = MapGeometry.centred(map, shown, gutter)
            val stable = MapGeometry.centred(map, shown + reserved, gutter).intersect(visible) ?: visible
            return Areas(visible, stable)
        }
        val visible = MapGeometry.unobstructed(map, shown)
        val stable = MapGeometry.unobstructed(visible, reserved)
        return Areas(visible, stable)
    }

    /** [box] in the map surface's own pixels (0,0 at the map's top left), as the app expects. */
    fun inSurface(map: MapGeometry.Box, box: MapGeometry.Box): List<Int> = listOf(
        (box.left - map.left).roundToInt(), (box.top - map.top).roundToInt(),
        (box.right - map.left).roundToInt(), (box.bottom - map.top).roundToInt(),
    )
}
