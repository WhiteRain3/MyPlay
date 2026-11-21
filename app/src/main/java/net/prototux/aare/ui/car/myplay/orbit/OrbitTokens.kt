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
object Orbit {
    /** Each world's accent and icon (the sheet's dial). */
    fun accent(world: World): Color = when (world) {
        World.NAVIGATE -> Color(0xFF6EE7A8)
        World.MUSIC -> Color(0xFFA78BFA)
        World.CALLS -> Color(0xFF5FD39A)
        World.APPS -> Color(0xFFFF8FB1)
        World.CAR -> Color(0xFFFFC24A)
        World.SETTINGS -> Color(0xFF9DB4E8)
    }

    fun icon(world: World): ImageVector = when (world) {
        World.NAVIGATE -> Icons.Rounded.Navigation
        World.MUSIC -> Icons.Rounded.MusicNote
        World.CALLS -> Icons.Rounded.Phone
        World.APPS -> Icons.Rounded.Apps
        World.CAR -> Icons.Rounded.DirectionsCar
        World.SETTINGS -> Icons.Rounded.Settings
    }

    /** Dark text on a filled accent disc (the turn bubble, the big buttons). */
    val ON_ACCENT = Color(0xFF06251A)
    /** The song's ring and its track. */
    val SONG = Color(0xFFA78BFA)

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

    fun glass(night: Boolean) = if (night) NIGHT else DAY

    /** The engine's tokens for ORBIT; the accent is Navigate's (home's world). */
    fun tokens(night: Boolean): SkinTokens = glass(night).let { g ->
        SkinTokens(
            background = g.background, surface = g.glass, onSurface = g.text, dim = g.secondary,
            accent = accent(World.NAVIGATE), onAccent = ON_ACCENT,
            // ORBIT is round: a corner larger than any part makes every shape a circle or a pill.
            corner = 999.dp, edge = 44.dp,
        )
    }

    /** The focus ring: an accent outline [FOCUS_WIDTH] wide, [FOCUS_GAP] away from the shape, in a dark halo. */
    val FOCUS_WIDTH = 7.dp
    val FOCUS_GAP = 5.dp
    val FOCUS_HALO = 16.dp
}
