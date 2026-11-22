package net.prototux.aare.ui.phone.next

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The rebuilt phone app's colours, from the design's style sheet (docs/design/phone-app/src/style.css).
 * Dark only. World colours mark a tab's identity; [Int] is the one colour for anything you can touch.
 */
object NextColors {
    // Surfaces
    val Bg = Color(0xFF0A1015)
    val Surface = Color(0xFF121A21)
    val Surface2 = Color(0xFF1A242D)
    val Sheet = Color(0xFF151E26)
    val Edge = Color(0x12FFFFFF) // white at 7 %
    val Edge2 = Color(0x24FFFFFF) // white at 14 %
    val Track = Color(0xFF2A3540) // fills only, never the only edge of a control
    val Outline = Color(0xFF8A95A8) // off switch, slider rail
    val SliderRail = Color(0xFF67748A)
    val Disabled = Color(0xFF5A6676) // dashed outline of a disabled control
    val TabBar = Color(0xF00D1319)
    val Menu = Color(0xFF1F2932)
    val Scrim = Color(0xA8030609)

    /** The one interaction colour: switches, sliders, primary buttons, selection, focus. */
    val Int = Color(0xFFDCE3EF)

    // Text
    val Text = Color(0xFFEEF1F7)
    val Text2 = Color(0xFFAAB3C8)
    val Text3 = Color(0xFF8A95A8)

    // Signals
    val Warn = Color(0xFFFF7A7A)
    val Caution = Color(0xFFFF9A57) // attention only; no tab uses this hue
    val Ok = Color(0xFF6EE7A8)
    val On = Color(0xFF0A1015) // text on any light fill

    // Snackbar
    val Snack = Color(0xFFE6EAF2)
    val SnackAction = Color(0xFF3A2C8A)
}

/** The five tabs and their world colours (ORBIT's night world colours). Identity only, never a control. */
enum class NextTab(val world: Color) {
    HOME(Color(0xFF6EE7A8)),
    SCREEN(Color(0xFFA78BFA)),
    CAR(Color(0xFFFFC24A)),
    APPS(Color(0xFFFF8FB1)),
    MORE(Color(0xFF9DB4E8)),
}

/** A world colour at 14 % (soft fill) or 55 % (line), as the style sheet mixes them. */
fun Color.soft(): Color = copy(alpha = 0.14f)
fun Color.line(): Color = copy(alpha = 0.55f)

/** Shape and space, on a 4 dp grid. */
object NextSpace {
    val Gutter = 16.dp
    val CardRadius = 24.dp
    val SheetRadius = 28.dp
    val RowMin = 64.dp
    val RowTall = 72.dp
    val IconDisc = 40.dp
    val Target = 48.dp
    val Button = 52.dp
    val ButtonSmall = 40.dp
    val TabBar = 72.dp
}

/** Contrast between two opaque colours, by the WCAG formula. */
fun contrast(a: Color, b: Color): Double {
    fun lum(c: Color): Double {
        fun ch(v: Float): Double = if (v <= 0.03928f) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
        return 0.2126 * ch(c.red) + 0.7152 * ch(c.green) + 0.0722 * ch(c.blue)
    }
    val (hi, lo) = lum(a).let { la -> lum(b).let { lb -> if (la > lb) la to lb else lb to la } }
    return (hi + 0.05) / (lo + 0.05)
}
