package net.prototux.aare.host.templates

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
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
 *  - [mayCoverMap] for a place where something may appear (the slot of the music card while no
 *    music is playing), so the stable area already leaves it out and the map does not jump when
 *    the card appears.
 *
 * Nothing here knows any particular layout: it only collects rectangles. Without a registry (all
 * of today's layouts), both modifiers do nothing and the areas are as before.
 */
internal class MapCoverRegistry {
    /** Drawn over the map now. */
    val shown = mutableStateMapOf<Any, Rect>()
    /** May be drawn over the map (also everything in [shown]). */
    val reserved = mutableStateMapOf<Any, Rect>()
}

internal val LocalMapCovers = staticCompositionLocalOf<MapCoverRegistry?> { null }

/** Marks this as drawn over the map now; see [MapCoverRegistry]. Bounds in root coordinates, as the map's. */
@Composable
internal fun Modifier.coversMap(): Modifier = markCover(shownNow = true)

/** Marks this place as one where something may be drawn over the map; see [MapCoverRegistry]. */
@Composable
internal fun Modifier.mayCoverMap(): Modifier = markCover(shownNow = false)

@Composable
private fun Modifier.markCover(shownNow: Boolean): Modifier {
    val registry = LocalMapCovers.current ?: return this
    val key = remember { Any() }
    DisposableEffect(registry, shownNow) {
        onDispose {
            registry.shown.remove(key)
            registry.reserved.remove(key)
        }
    }
    return onGloballyPositioned {
        val r = it.boundsInRoot()
        if (shownNow) registry.shown[key] = r else registry.shown.remove(key)
        registry.reserved[key] = r
    }
}

/** MyPlay (M2): the two areas a maps app is told, worked out from what covers its map. Pure. */
internal object MapAreas {
    data class Areas(val visible: MapGeometry.Box, val stable: MapGeometry.Box)

    /**
     * [map] is the map's own rectangle; [shown] covers it now; [reserved] may cover it (whatever
     * is in [shown] counts as reserved too). The visible area is the largest free rectangle
     * around [shown]; the stable area the largest free one around both. The stable area is never
     * larger than the visible one: where the two would not nest, it is cut to fit inside it.
     */
    fun compute(map: MapGeometry.Box, shown: List<MapGeometry.Box>, reserved: List<MapGeometry.Box>): Areas {
        val visible = MapGeometry.unobstructed(map, shown)
        val stable = MapGeometry.unobstructed(map, shown + reserved).intersect(visible) ?: visible
        return Areas(visible, stable)
    }

    /** [box] in the map surface's own pixels (0,0 at the map's top left), as the app expects. */
    fun inSurface(map: MapGeometry.Box, box: MapGeometry.Box): List<Int> = listOf(
        (box.left - map.left).roundToInt(), (box.top - map.top).roundToInt(),
        (box.right - map.left).roundToInt(), (box.bottom - map.top).roundToInt(),
    )
}
