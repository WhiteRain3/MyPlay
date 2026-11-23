package net.prototux.aare.ui.car.myplay.orbit

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.prototux.aare.ui.car.myplay.SkinTokens
import net.prototux.aare.ui.car.myplay.World

/**
 * MyPlay (plan phase N, N4): ORBIT's tokens, from the Design reviewer's sheet (AA-Experiments,
 * branch claude/design-options, docs/design-options/car-screen/new-language/, language 1).
 *
 * Circles, rings and arcs; near-black glass over the map; each world has its own accent, and the
 * accent of the world you are in tints the rings. Sizes are the sheet's pixels at 1280x720, as dp.
 *
 * The sheet draws night only. The day colours here are MyPlay's own reading of it (light glass,
 * the same accents, darker text) until the owner sees them on the car.
 */
/**
 * TH-1 (the owner: "our theming doesn't work"): the owner's theme, accent and own colours, taken by
 * ORBIT. Null (the default theme, its own accent, no own colours): ORBIT's designed colours. TH-1a:
 * provided by each engine (MyPlayEngineShell) from its own state, so the car's screen and the
 * phone's picture of it never overwrite each other, and the first frame already has it.
 */
val LocalOrbitThemed = androidx.compose.runtime.compositionLocalOf<Orbit.Themed?> { null }

object Orbit {
    /**
     * Each world's accent (the sheet's dial). By night the sheet's colours, each at least 7:1 on
     * the night glass. By day (O-1) the same hues darkened, each at least 4.9:1 on the day glass
     * (the sheet's pale ones were 1.4 to 2.5:1, under the 3:1 needed for icons and rings).
     */
    @androidx.compose.runtime.Composable @androidx.compose.runtime.ReadOnlyComposable
    fun accent(world: World, night: Boolean): Color = accent(world, night, LocalOrbitThemed.current)

    /** [accent] for [themed] given (outside composition). */
    fun accent(world: World, night: Boolean, themed: Themed?): Color = themed?.accentFor(world, night) ?: designAccent(world, night)

    /** The sheet's own accent for [world] (no theme chosen). */
    private fun designAccent(world: World, night: Boolean): Color = if (night) when (world) {
        World.NAVIGATE -> Color(0xFF6EE7A8)
        World.MUSIC -> Color(0xFFA78BFA)
        World.CALLS -> Color(0xFF5FD39A)
        World.APPS -> Color(0xFFFF8FB1)
        World.CAR -> Color(0xFFFFC24A)
        World.SETTINGS -> Color(0xFF9DB4E8)
    } else when (world) {
        World.NAVIGATE -> Color(0xFF0E7A4C)
        World.MUSIC -> Color(0xFF6A3FD1)
        World.CALLS -> Color(0xFF17734A)
        World.APPS -> Color(0xFFC2185B)
        World.CAR -> Color(0xFF8A5A00)
        World.SETTINGS -> Color(0xFF3A5AA6)
    }

    /** A low battery and other warnings: the sheet's decline red by night, a darker red by day (5.6:1 on the day glass). */
    fun warning(night: Boolean): Color = if (night) Color(0xFFFF6B6B) else Color(0xFFC62828)

    /** N20: a calm amber, for the speed over the limit (not the red of a warning). */
    fun caution(night: Boolean): Color = if (night) Color(0xFFFFC857) else Color(0xFF8A5A00)

    /** Text and icons on a filled accent disc: dark on the pale night accents, white on the dark day ones. */
    @androidx.compose.runtime.Composable @androidx.compose.runtime.ReadOnlyComposable
    fun onAccent(night: Boolean): Color = onAccent(night, LocalOrbitThemed.current)

    fun onAccent(night: Boolean, themed: Themed?): Color = themed?.let { if (night) it.onAccentNight else it.onAccentDay }
        ?: if (night) Color(0xFF06251A) else Color.White

    fun icon(world: World): ImageVector = when (world) {
        World.NAVIGATE -> Icons.Rounded.Navigation
        World.MUSIC -> Icons.Rounded.MusicNote
        World.CALLS -> Icons.Rounded.Phone
        World.APPS -> Icons.Rounded.Apps
        World.CAR -> Icons.Rounded.DirectionsCar
        World.SETTINGS -> Icons.Rounded.Settings
    }


    /** The type scale (system fonts only). */
    object Type {
        /** Hero numbers: the distance to the turn. */
        val hero = TextStyle(fontSize = 62.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 62.sp)
        /** The clock and the hub's arrival time. */
        val clock = TextStyle(fontSize = 46.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 46.sp)
        /** Names: the song, a road, a person. */
        val name = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
        /** Secondary lines. */
        val secondary = TextStyle(fontSize = 26.sp)
        /** The words on the dial. */
        val dial = TextStyle(fontSize = 27.sp, fontWeight = FontWeight.SemiBold)
    }

    /** The colours that change between night and day. */
    data class Glass(
        /** The glass under a disc or the dial. */
        val glass: Color,
        /** A thin edge on the glass. */
        val edge: Color,
        /** A ring's track, under its progress arc. */
        val track: Color,
        val text: Color,
        val secondary: Color,
        /** Behind everything when there is no map. */
        val background: Color,
        /** The halo around the focus ring, so it reads on any map. */
        val halo: Color,
    )

    val NIGHT = Glass(
        glass = Color(0xDB080C14), edge = Color(0x24FFFFFF), track = Color(0xFF24323A),
        text = Color(0xFFEEF1F7), secondary = Color(0xFFAAB3C8), background = Color(0xFF0B1318), halo = Color(0xFF06080D),
    )
    val DAY = Glass(
        glass = Color(0xE6F4F6FA), edge = Color(0x330E121B), track = Color(0xFFD3DAE3),
        text = Color(0xFF0E121B), secondary = Color(0xFF4A5468), background = Color(0xFFE9EEF2), halo = Color(0xFFFFFFFF),
    )

    @androidx.compose.runtime.Composable @androidx.compose.runtime.ReadOnlyComposable
    fun glass(night: Boolean): Glass = glass(night, LocalOrbitThemed.current)

    fun glass(night: Boolean, themed: Themed?): Glass = themed?.let { if (night) it.night else it.day } ?: if (night) NIGHT else DAY

    /** TH-1: ORBIT's colours from a car theme (after its accent and the owner's colours). */
    class Themed(
        val night: Glass, val day: Glass,
        val accentNight: Color, val accentDay: Color,
        val onAccentNight: Color, val onAccentDay: Color,
        /** A single-colour theme: every world in its one colour, not only Navigate. */
        val single: Boolean,
    ) {
        /** Navigate (home's world and the focus ring) takes the theme's accent; the other worlds keep theirs, unless single-colour. */
        fun accentFor(world: World, night: Boolean): Color? =
            if (single || world == World.NAVIGATE) (if (night) accentNight else accentDay) else null

        companion object {
            fun from(theme: net.prototux.aare.ui.theme.CarThemeSpec): Themed {
                fun g(s: androidx.compose.material3.ColorScheme, base: Glass) = Glass(
                    glass = s.surface.copy(alpha = base.glass.alpha), edge = s.outline.copy(alpha = 0.3f), track = s.surfaceVariant,
                    text = s.onSurface, secondary = s.onSurfaceVariant, background = s.background, halo = s.background,
                )
                return Themed(g(theme.night, NIGHT), g(theme.day, DAY), theme.night.primary, theme.day.primary,
                    theme.night.onPrimary, theme.day.onPrimary, single = theme.singleColor != null)
            }
        }
    }

    /** The engine's tokens for ORBIT; the accent is Navigate's (home's world). */
    @androidx.compose.runtime.Composable @androidx.compose.runtime.ReadOnlyComposable
    fun tokens(night: Boolean): SkinTokens = tokens(night, LocalOrbitThemed.current)

    fun tokens(night: Boolean, themed: Themed?): SkinTokens = glass(night, themed).let { g ->
        SkinTokens(
            background = g.background, surface = g.glass, onSurface = g.text, dim = g.secondary,
            accent = accent(World.NAVIGATE, night, themed), onAccent = onAccent(night, themed),
            // ORBIT is round: a corner larger than any part makes every shape a circle or a pill.
            corner = 999.dp, edge = 44.dp, night = night,
        )
    }

    /**
     * Look C (23, 24): the maps app's screens under ORBIT in ORBIT's own colours, by night and by
     * day as the car is: the glass for its cards and rows, the text colours, and Navigate's accent
     * (darkened by day, at least 4.9:1 on the day glass) for its primary buttons, Go now and rings.
     * [base] (the car theme's scheme) keeps everything else.
     */
    fun appScheme(base: androidx.compose.material3.ColorScheme, night: Boolean, themed: Themed?): androidx.compose.material3.ColorScheme {
        val g = glass(night, themed)
        val accent = accent(World.NAVIGATE, night, themed)
        val on = onAccent(night, themed)
        val solid = g.glass.copy(alpha = 1f)
        return base.copy(
            primary = accent, onPrimary = on, primaryContainer = accent, onPrimaryContainer = on,
            secondaryContainer = solid, onSecondaryContainer = g.text,
            background = g.background, onBackground = g.text,
            surface = solid, onSurface = g.text, surfaceVariant = g.track, onSurfaceVariant = g.secondary,
            surfaceContainerLowest = solid, surfaceContainerLow = solid, surfaceContainer = solid,
            surfaceContainerHigh = solid, surfaceContainerHighest = solid,
            outline = g.edge, outlineVariant = g.edge,
        )
    }

    /** The focus ring: an accent outline [FOCUS_WIDTH] wide, [FOCUS_GAP] away from the shape, in a dark halo. */
    val FOCUS_WIDTH = 7.dp
    val FOCUS_GAP = 5.dp
    val FOCUS_HALO = 16.dp
}
