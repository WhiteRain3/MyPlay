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

/** The accent colours as dots: each accent's colour on dark surfaces, in the settings' order. */
fun accentDotColours(): List<Color> = CarAccents.all.map { it.dark.primary }

/** The dot of the stored accent [id]; null for the theme's own or the phone's. */
fun accentDotColour(id: String): Color? = CarAccents.byId(id)?.dark?.primary

/** The stored accent id of a tapped dot. */
fun accentIdOf(colour: Color): String? = CarAccents.all.firstOrNull { it.dark.primary == colour }?.id

/**
 * The car screen's colours (plan E5): the look, the accent colour, day and night, and whether
 * map apps follow the car's lights. The same settings as the Car screen page (style_theme,
 * style_accent, car_theme, myplay_maps_follow_car_lights); the layout stays where it is.
 */
@Composable
fun ColoursPage() {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    var themeId by remember { mutableStateOf(CarThemes.byId(settings.carThemeId).id) }
    var accent by remember { mutableStateOf(settings.carAccent) }
    var dayNight by remember { mutableStateOf(settings.carTheme) }
    var mapsFollow by remember { mutableStateOf(settings.mapsFollowCarLights) }
    Column {
        PartPicture()
        SectionHeading(stringResource(R.string.setup_choice_theme))
        RowGroup(rows = CarThemes.available.map { theme ->
            { ThemeRow(theme, theme.id == themeId) { themeId = theme.id; settings.carThemeId = theme.id } }
        })
        SectionHeading(stringResource(R.string.setup_choice_accent))
        ColourDots(
            accentDotColours(), CarAccents.all.map { stringResource(it.nameRes) }, accentDotColour(accent),
            onSelect = { c -> accentIdOf(c)?.let { accent = it; settings.carAccent = it } },
            endText = stringResource(R.string.accent_theme),
            onEnd = { accent = CarAccents.THEME; settings.carAccent = CarAccents.THEME },
            endSelected = accent == CarAccents.THEME,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Spacer(Modifier.height(10.dp))
            PillButton(stringResource(R.string.phone_colours_phone_accent), main = accent == CarAccents.PHONE) {
                accent = CarAccents.PHONE; settings.carAccent = CarAccents.PHONE
            }
        }
        SectionHeading(stringResource(R.string.phone_colours_day_night))
        ChoiceButtons(
            listOf(
                Triple(Settings.THEME_DARK, Icons.Rounded.DarkMode, stringResource(R.string.setup_choice_dark)),
                Triple(Settings.THEME_AUTO, Icons.Rounded.BrightnessAuto, stringResource(R.string.setup_choice_colors_auto)),
                Triple(Settings.THEME_LIGHT, Icons.Rounded.LightMode, stringResource(R.string.setup_choice_light)),
            ),
            dayNight,
        ) { dayNight = it; settings.carTheme = it }
        Spacer(Modifier.height(12.dp))
        RowGroup(rows = listOf {
            SwitchRow(Icons.Rounded.Map, stringResource(R.string.myplay_toggle_maps_follow_car_title), mapsFollow, TileTone.GREEN,
                stringResource(R.string.myplay_toggle_maps_follow_car_subtitle)) { mapsFollow = it; settings.mapsFollowCarLights = it }
        })
    }
}

/** One look: three dots of its night colours (background, card, accent), its name, and a tick when chosen. */
@Composable
private fun ThemeRow(theme: CarThemeSpec, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.RadioButton, onClick = onClick).semantics { this.selected = selected }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.clip(RoundedCornerShape(14.dp)).background(theme.night.background).border(1.dp, PhoneColors.Border, RoundedCornerShape(14.dp))
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf(theme.night.surfaceContainer, theme.night.primary, theme.night.onSurface).forEach {
                Box(Modifier.size(12.dp).clip(CircleShape).background(it))
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(theme.label(), Modifier.weight(1f), color = PhoneColors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        if (selected) Icon(Icons.Rounded.CheckCircle, null, tint = PhoneColors.Accent)
    }
}
