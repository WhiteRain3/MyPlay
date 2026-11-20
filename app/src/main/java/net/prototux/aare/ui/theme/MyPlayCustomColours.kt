package net.prototux.aare.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.core.content.edit
import net.prototux.aare.Settings
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * MyPlay (F3): the owner's own colours over a look, night and day apart. Each role paints a part of
 * the car screen; the phone's Colours page ties each to the part of the car picture it paints.
 * Applied after the accent, so the owner's colours win. Stored as text (setting
 * "myplay_look_colours"); the format is kept stable once released, as whole looks (F7) reuse it.
 */
enum class ColourRole(val id: String) {
    /** The screen behind everything. */
    BACKGROUND("background"),
    /** Cards and tiles. */
    CARD("card"),
    /** The bar (rail, ribbon, dock). */
    BAR("bar"),
    /** Buttons that stand out, the focus ring, progress. */
    ACCENT("accent"),
    /** Text and icons on the accent colour. */
    ON_ACCENT("on_accent"),
    TEXT("text"),
    /** Secondary text. */
    TEXT_DIM("text_dim"),
    ;

    companion object {
        fun byId(id: String) = entries.firstOrNull { it.id == id }
    }
}

/** The owner's colours (ARGB) for night and for day; a role not set keeps the look's own. */
data class CustomColours(val night: Map<ColourRole, Int> = emptyMap(), val day: Map<ColourRole, Int> = emptyMap()) {
    val isEmpty: Boolean get() = night.isEmpty() && day.isEmpty()

    /** "night.accent=FF6EE7A8;day.card=FF202430", night first, roles in their order. */
    fun encode(): String = (night.toSortedMap().map { (r, c) -> "night.${r.id}=${hex(c)}" } +
        day.toSortedMap().map { (r, c) -> "day.${r.id}=${hex(c)}" }).joinToString(";")

    companion object {
        val NONE = CustomColours()

        /** The text form back; unknown or broken parts are skipped. */
        fun decode(s: String?): CustomColours {
            val night = HashMap<ColourRole, Int>()
            val day = HashMap<ColourRole, Int>()
            for (part in s.orEmpty().split(';')) {
                val (key, value) = part.split('=', limit = 2).takeIf { it.size == 2 } ?: continue
                val (side, roleId) = key.trim().split('.', limit = 2).takeIf { it.size == 2 } ?: continue
                val role = ColourRole.byId(roleId) ?: continue
                val colour = value.trim().takeIf { it.length == 8 }?.toLongOrNull(16)?.toInt() ?: continue
                when (side) {
                    "night" -> night[role] = colour
                    "day" -> day[role] = colour
                }
            }
            return CustomColours(night, day)
        }

        private fun hex(c: Int) = "%08X".format(c)
    }
}

/** The look with the owner's colours over it (after the accent); nothing set leaves it as it is. */
fun CarThemeSpec.withCustomColours(c: CustomColours): CarThemeSpec =
    if (c.isEmpty) this else copy(night = night.withColours(c.night), day = day.withColours(c.day))

private fun ColorScheme.withColours(m: Map<ColourRole, Int>): ColorScheme {
    if (m.isEmpty()) return this
    fun get(role: ColourRole, current: Color) = m[role]?.let { Color(it) } ?: current
    val background = get(ColourRole.BACKGROUND, background)
    val card = m[ColourRole.CARD]?.let { Color(it) }
    val bar = m[ColourRole.BAR]?.let { Color(it) }
    val text = m[ColourRole.TEXT]?.let { Color(it) }
    return copy(
        background = background,
        surface = get(ColourRole.BACKGROUND, surface),
        surfaceContainer = card ?: surfaceContainer,
        surfaceContainerHigh = card ?: surfaceContainerHigh,
        surfaceContainerHighest = card ?: surfaceContainerHighest,
        surfaceContainerLowest = bar ?: surfaceContainerLowest,
        surfaceContainerLow = bar ?: surfaceContainerLow,
        primary = get(ColourRole.ACCENT, primary),
        onPrimary = get(ColourRole.ON_ACCENT, onPrimary),
        onSurface = text ?: onSurface,
        onBackground = text ?: onBackground,
        onSurfaceVariant = get(ColourRole.TEXT_DIM, onSurfaceVariant),
    )
}

/** WCAG 2.x contrast ratio of two ARGB colours (alpha ignored), from 1 to 21. */
fun contrastRatio(fg: Int, bg: Int): Double {
    val a = relativeLuminance(fg)
    val b = relativeLuminance(bg)
    return (max(a, b) + 0.05) / (min(a, b) + 0.05)
}

private fun relativeLuminance(argb: Int): Double {
    fun channel(v: Int): Double {
        val c = v / 255.0
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel((argb shr 16) and 0xFF) + 0.7152 * channel((argb shr 8) and 0xFF) + 0.0722 * channel(argb and 0xFF)
}

/** The owner's colours as text ("" = none), see [CustomColours]. */
var Settings.myplayLookColours: String
    get() = prefs.getString("myplay_look_colours", "") ?: ""
    set(v) = prefs.edit { putString("myplay_look_colours", v) }
