package net.prototux.aare.ui.phone

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import net.prototux.aare.ui.theme.ColourRole
import net.prototux.aare.ui.theme.CustomColours
import net.prototux.aare.ui.theme.contrastRatio

/** The owner's colours (F3), with one role on one side set to [argb], or back to the look's own when null. */
fun CustomColours.with(night: Boolean, role: ColourRole, argb: Int?): CustomColours {
    val side = (if (night) this.night else day).toMutableMap()
    if (argb == null) side.remove(role) else side[role] = argb or 0xFF000000.toInt()
    return if (night) copy(night = side) else copy(day = side)
}

/** The owner's colours for one side. */
fun CustomColours.side(night: Boolean): Map<ColourRole, Int> = if (night) this.night else day

/** The colour [role] paints, as the car reads it from the finished [scheme] (see withCustomColours). */
fun roleColour(scheme: ColorScheme, role: ColourRole): Int = when (role) {
    ColourRole.BACKGROUND -> scheme.background
    ColourRole.CARD -> scheme.surfaceContainer
    ColourRole.BAR -> scheme.surfaceContainerLowest
    ColourRole.ACCENT -> scheme.primary
    ColourRole.ON_ACCENT -> scheme.onPrimary
    ColourRole.TEXT -> scheme.onSurface
    ColourRole.TEXT_DIM -> scheme.onSurfaceVariant
}.toArgb()

/** The role a tap on the picture's [part] changes on the Colours page: what that part is mostly painted with. */
fun colourRoleOf(part: PicturePart): ColourRole = when (part) {
    PicturePart.BAR, PicturePart.APPS -> ColourRole.BAR
    PicturePart.CLOCK, PicturePart.CARDS, PicturePart.HUB, PicturePart.SPEED -> ColourRole.CARD
    PicturePart.STATUS -> ColourRole.TEXT
    PicturePart.WALLPAPER, PicturePart.WHOLE -> ColourRole.BACKGROUND
}

/** Text over what it is read on, the pairs the contrast warning checks. */
val CONTRAST_PAIRS: List<Pair<ColourRole, ColourRole>> = listOf(
    ColourRole.TEXT to ColourRole.BACKGROUND,
    ColourRole.TEXT to ColourRole.CARD,
    ColourRole.TEXT to ColourRole.BAR,
    ColourRole.TEXT_DIM to ColourRole.CARD,
    ColourRole.ON_ACCENT to ColourRole.ACCENT,
)

/** The lowest contrast text should have (WCAG AA for normal text). */
const val MIN_CONTRAST = 4.5

/**
 * The pairs that read badly (below [MIN_CONTRAST]) with the colours [colourOf] gives, counting only
 * pairs where the owner set at least one of the two ([set]): the look's own pairs are not theirs to fix.
 */
fun hardToRead(colourOf: (ColourRole) -> Int, set: Set<ColourRole>): List<Pair<ColourRole, ColourRole>> =
    CONTRAST_PAIRS.filter { (fg, bg) -> (fg in set || bg in set) && contrastRatio(colourOf(fg), colourOf(bg)) < MIN_CONTRAST }
