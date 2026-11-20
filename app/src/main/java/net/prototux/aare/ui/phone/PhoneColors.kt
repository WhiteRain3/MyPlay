package net.prototux.aare.ui.phone

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * MyPlay's phone colours, taken from the owner's chosen design (the "merged" pictures in
 * AA-Experiments, `docs/design-options/`, branch `claude/design-options`). Always dark: the
 * owner does not want a light phone app.
 */
object PhoneColors {
    val Page = Color(0xFF10131B)
    val Card = Color(0xFF171B27)
    val Border = Color(0xFF262B39)
    val Divider = Color(0xFF222838)
    val Circle = Color(0xFF1B2030)
    val CircleIcon = Color(0xFFAAB3C8)
    val Text = Color(0xFFEEF1F7)
    val Muted = Color(0xFF8B93A7)
    val Dim = Color(0xFF6B7390)
    val Accent = Color(0xFF6EE7A8)
    val OnAccent = Color(0xFF06251A)
    val AccentCard = Color(0xFF14291F)
    val AccentSoftText = Color(0xFF9FE9C3)
    val SwitchOff = Color(0xFF2B3144)
    val QuietButton = Color(0xFF222838)
    val QuietButtonText = Color(0xFFE3E8F5)
    val SavedBar = Color(0xFF1D3A2C)
    val SavedText = Color(0xFFBFF3D6)
    val Warning = Color(0xFFFFC24A)
    val Error = Color(0xFFFF5A5F)
}

/** The six tile colours of the design: a dark background and a light symbol and text colour. */
enum class TileTone(val background: Color, val content: Color) {
    CORAL(Color(0xFF33201F), Color(0xFFFFB199)),
    GREEN(Color(0xFF1D3226), Color(0xFF8FE3B0)),
    BLUE(Color(0xFF1C2A3F), Color(0xFF8EC5FF)),
    AMBER(Color(0xFF352E1B), Color(0xFFFFD77A)),
    VIOLET(Color(0xFF2A2340), Color(0xFFC7B0FF)),
    PINK(Color(0xFF36202C), Color(0xFFFF9FC0)),
}

/** Material colours for the parts of the phone app that still use Material's own controls. */
val PhonePalette = darkColorScheme(
    primary = PhoneColors.Accent,
    onPrimary = PhoneColors.OnAccent,
    primaryContainer = PhoneColors.AccentCard,
    onPrimaryContainer = PhoneColors.AccentSoftText,
    secondary = PhoneColors.Accent,
    onSecondary = PhoneColors.OnAccent,
    secondaryContainer = PhoneColors.SavedBar,
    onSecondaryContainer = PhoneColors.SavedText,
    tertiary = TileTone.VIOLET.content,
    onTertiary = PhoneColors.Page,
    tertiaryContainer = TileTone.VIOLET.background,
    onTertiaryContainer = TileTone.VIOLET.content,
    background = PhoneColors.Page,
    onBackground = PhoneColors.Text,
    surface = PhoneColors.Page,
    onSurface = PhoneColors.Text,
    surfaceVariant = PhoneColors.Card,
    onSurfaceVariant = PhoneColors.Muted,
    surfaceContainerLowest = PhoneColors.Page,
    surfaceContainerLow = PhoneColors.Card,
    surfaceContainer = PhoneColors.Card,
    surfaceContainerHigh = PhoneColors.Circle,
    surfaceContainerHighest = PhoneColors.QuietButton,
    outline = PhoneColors.Border,
    outlineVariant = PhoneColors.Divider,
    error = PhoneColors.Error,
    onError = PhoneColors.Page,
    errorContainer = TileTone.CORAL.background,
    onErrorContainer = TileTone.CORAL.content,
)

/** The phone app's theme: always dark, whatever the phone itself is set to. */
@Composable
fun MyPlayPhoneTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PhonePalette, content = content)
}
