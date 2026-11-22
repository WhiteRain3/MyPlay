package net.prototux.aare.ui.phone.next

import net.prototux.aare.ui.car.CarPart
import net.prototux.aare.ui.phone.Look
import net.prototux.aare.ui.phone.MyPlayLooks
import net.prototux.aare.ui.theme.MapColourFilter

/**
 * The page a tapped part of the car picture opens, per design (plan §3.5): ORBIT's parts lead to
 * what they show; with DECK or an older layout, their own parts lead to Archive. Null (the whole
 * picture) means "stay on Screen".
 */
fun routeForPart(part: CarPart?, design: DesignInUse): String? {
    val archived = if (design == DesignInUse.DECK) "more/archive/deck" else "more/archive/layouts"
    return when (part) {
        null -> null
        CarPart.OrbitHub -> if (design == DesignInUse.ORBIT) "screen/design" else archived
        CarPart.OrbitSong -> if (design == DesignInUse.ORBIT) "car/sound" else "more/archive/layouts"
        CarPart.OrbitTurn -> if (design == DesignInUse.ORBIT) "car/maps" else null
        CarPart.OrbitSpeed -> "car/speed"
        CarPart.Wallpaper -> "screen/wallpaper"
        CarPart.AppsButton -> "apps"
        CarPart.Bar, CarPart.Clock, CarPart.Status, is CarPart.DashboardCard, is CarPart.MapBehindCard -> "more/archive/layouts"
    }
}

/** The look in use by name (built-in or saved), or null when the settings are no saved look (plan §9.3). */
fun lookInUse(all: Map<String, *>, saved: List<Look>): String? =
    (MyPlayLooks.BUILT_IN + saved).firstOrNull { MyPlayLooks.isCurrent(all, it) }?.name

/** Map colours' value: a preset's key, "custom", or "none" when the filter changes nothing. */
fun mapColoursKind(filter: MapColourFilter): String {
    val c = filter.clamped()
    if (c == MapColourFilter()) return "none"
    return MapColourFilter.PRESETS.entries.firstOrNull { it.value == c }?.key ?: "custom"
}

/** The fixed densities the Size page offers (dpi); 0 is Auto. Same list as today's. */
val FIXED_DENSITIES: List<Int> = net.prototux.aare.ui.phone.DENSITY_CHOICES.filter { it != 0 }
