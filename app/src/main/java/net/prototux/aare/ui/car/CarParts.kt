package net.prototux.aare.ui.car

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * MyPlay: the parts of the car screen, tagged where each layout draws them, so the phone's small
 * picture of the car screen (plan E3) can find them and make each one a tappable region.
 *
 * In the car nothing is recorded: [LocalCarParts] is null there and [carPart] does nothing. The
 * phone's picture provides a [CarPartRegistry] and reads [CarPartRegistry.regions].
 */
sealed interface CarPart {
    data object Bar : CarPart
    data object Clock : CarPart
    /** The phone's battery, signal and Wi-Fi icons. */
    data object Status : CarPart
    data object Wallpaper : CarPart
    /** The button that opens the app grid. */
    data object AppsButton : CarPart
    /** A card of AARE's dashboard (taskbar layout). */
    data class DashboardCard(val card: DashCard) : CarPart
    /** A card of the "map behind, cards on top" layout. */
    data class MapBehindCard(val card: CarLayoutSpec.SpecCard) : CarPart
}

/** Where each tagged part was last drawn, in root coordinates of the composition that drew it. */
class CarPartRegistry {
    private val placed = mutableStateMapOf<Any, Pair<CarPart, Rect>>()

    /** Every part drawn now with its rectangle. A part drawn in two places (the clock in a bar and a card) is listed twice. */
    val regions: List<Pair<CarPart, Rect>> get() = placed.values.toList()

    internal fun put(key: Any, part: CarPart, rect: Rect) { placed[key] = part to rect }
    internal fun remove(key: Any) { placed.remove(key) }
}

val LocalCarParts = staticCompositionLocalOf<CarPartRegistry?> { null }

/** Tags this as [part] for the phone's picture; nothing in the car. */
@Composable
fun Modifier.carPart(part: CarPart): Modifier {
    val registry = LocalCarParts.current ?: return this
    val key = remember { Any() }
    DisposableEffect(registry) { onDispose { registry.remove(key) } }
    return onGloballyPositioned { registry.put(key, part, it.boundsInRoot()) }
}
