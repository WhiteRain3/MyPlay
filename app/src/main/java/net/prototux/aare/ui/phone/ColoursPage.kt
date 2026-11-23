package net.prototux.aare.ui.phone

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.ui.theme.CarAccents
import net.prototux.aare.ui.theme.CarThemeSpec
import net.prototux.aare.ui.theme.CarThemes
import net.prototux.aare.ui.theme.label
import net.prototux.aare.ui.theme.ColourRole
import net.prototux.aare.ui.theme.CustomColours
import net.prototux.aare.ui.theme.contrastRatio
import net.prototux.aare.ui.theme.forPhone
import net.prototux.aare.ui.theme.myplayLookColours
import net.prototux.aare.ui.theme.withAccent
import net.prototux.aare.ui.theme.withCustomColours
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.IconButton

/** The accent colours as dots: each accent's colour on dark surfaces, in the settings' order. */
fun accentDotColours(): List<Color> = CarAccents.all.map { it.dark.primary }

/** The dot of the stored accent [id]; null for the theme's own or the phone's. */
fun accentDotColour(id: String): Color? = CarAccents.byId(id)?.dark?.primary

/** The stored accent id of a tapped dot. */
fun accentIdOf(colour: Color): String? = CarAccents.all.firstOrNull { it.dark.primary == colour }?.id

/** The name of a colour role on the Colours page. */
val ColourRole.label: Int
    get() = when (this) {
        ColourRole.BACKGROUND -> R.string.phone_role_background
        ColourRole.CARD -> R.string.phone_role_card
        ColourRole.BAR -> R.string.phone_role_bar
        ColourRole.ACCENT -> R.string.phone_role_accent
        ColourRole.ON_ACCENT -> R.string.phone_role_on_accent
        ColourRole.TEXT -> R.string.phone_role_text
        ColourRole.TEXT_DIM -> R.string.phone_role_text_dim
    }

/** What a colour role paints on the car screen (the car shares some colours between parts). */
val ColourRole.what: Int
    get() = when (this) {
        ColourRole.BACKGROUND -> R.string.phone_role_background_what
        ColourRole.CARD -> R.string.phone_role_card_what
        ColourRole.BAR -> R.string.phone_role_bar_what
        ColourRole.ACCENT -> R.string.phone_role_accent_what
        ColourRole.ON_ACCENT -> R.string.phone_role_on_accent_what
        ColourRole.TEXT -> R.string.phone_role_text_what
        ColourRole.TEXT_DIM -> R.string.phone_role_text_dim_what
    }

