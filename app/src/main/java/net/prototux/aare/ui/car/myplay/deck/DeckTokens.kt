package net.prototux.aare.ui.car.myplay.deck

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import net.prototux.aare.ui.car.myplay.SkinTokens
import net.prototux.aare.ui.car.myplay.World

/**
 * MyPlay (plan phase N, N13): DECK's tokens, from the Design reviewer's sheet (AA-Experiments,
 * branch claude/design-options, docs/design-options/car-screen/new-language/, language 2).
 *
 * Tall index cards with small corners and a thick coloured top edge; one flat hue per world, used
 * as a tinted surface with a bright edge; the map stays dark and neutral. Sizes are the sheet's
 * pixels at 1280x720, as dp.
 *
 * The sheet draws night only and five worlds. The day colours and Settings' hue (blue: the sheet's
 * five are taken) are MyPlay's own reading of it, until the owner sees them in the car.
 */
object Deck {
    /** Each world's hue. By night the sheet's; by day the same hues darkened to read on light cards. */
    fun hue(world: World, night: Boolean): Color = if (night) when (world) {
        World.NAVIGATE -> Color(0xFF2DD4BF)
        World.MUSIC -> Color(0xFFA78BFA)
        World.CALLS -> Color(0xFF4ADE80)
        World.CAR -> Color(0xFFFBBF24)
        World.APPS -> Color(0xFFFB7185)
        World.SETTINGS -> Color(0xFF60A5FA)
    } else when (world) {
        World.NAVIGATE -> Color(0xFF0F766E)
        World.MUSIC -> Color(0xFF6D28D9)
        World.CALLS -> Color(0xFF15803D)
        World.CAR -> Color(0xFF8A5A00)
        World.APPS -> Color(0xFFBE123C)
        World.SETTINGS -> Color(0xFF1D4ED8)
    }

    fun icon(world: World): ImageVector = when (world) {
        World.NAVIGATE -> Icons.Rounded.Navigation
        World.MUSIC -> Icons.Rounded.MusicNote
        World.CALLS -> Icons.Rounded.Phone
        World.CAR -> Icons.Rounded.Speed
        World.APPS -> Icons.Rounded.Apps
        World.SETTINGS -> Icons.Rounded.Settings
    }

    /** The colours that change between night and day. */
    data class Paper(
        /** A card's base, under its world's tint. */
        val card: Color,
        val text: Color,
        val secondary: Color,
        /** Behind everything when there is no map. */
        val background: Color,
        /** The shadow under text over the map and the halo round the focus outline. */
        val halo: Color,
        /** How much of a world's hue tints its card. */
        val tint: Float,
    )

    val NIGHT = Paper(card = Color(0xFF14171E), text = Color(0xFFF1F3F8), secondary = Color(0xFFB4BCCB), background = Color(0xFF0D1016), halo = Color(0xFF05070B), tint = 0.16f)
    val DAY = Paper(card = Color(0xFFF7F8FA), text = Color(0xFF10131A), secondary = Color(0xFF465063), background = Color(0xFFE6EAF0), halo = Color(0xFFFFFFFF), tint = 0.10f)

    fun paper(night: Boolean) = if (night) NIGHT else DAY

    /** A world's card: its hue as a flat tint over the card colour (opaque, so the map never shows through). */
    fun cardColour(world: World, night: Boolean): Color = paper(night).let { p -> hue(world, night).copy(alpha = p.tint).compositeOver(p.card) }

    /** The type scale (system fonts only). */
    object Type {
        /** Hero numbers: the distance to the turn. */
        val hero = TextStyle(fontSize = 92.sp, fontWeight = FontWeight.Bold, lineHeight = 92.sp)
        /** Card titles. */
        val title = TextStyle(fontSize = 38.sp, fontWeight = FontWeight.Bold, lineHeight = 42.sp)
        /** The clock and the arrival time. */
        val clock = TextStyle(fontSize = 46.sp, fontWeight = FontWeight.Bold, lineHeight = 48.sp)
        val secondary = TextStyle(fontSize = 27.sp, lineHeight = 31.sp)
        /** Labels: caps, wide spacing. */
        val label = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.14.em)
    }

    /** A card's corner, and its coloured top edge. */
    val CORNER = 14.dp
    val EDGE = 7.dp

    /** The engine's tokens for DECK; the accent is Navigate's. */
    fun tokens(night: Boolean): SkinTokens = paper(night).let { p ->
        SkinTokens(
            background = p.background, surface = p.card, onSurface = p.text, dim = p.secondary,
            accent = hue(World.NAVIGATE, night), onAccent = if (night) Color(0xFF04221D) else Color.White,
            corner = CORNER, edge = 40.dp, night = night,
        )
    }
}
