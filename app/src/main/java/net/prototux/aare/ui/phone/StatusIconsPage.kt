package net.prototux.aare.ui.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.rounded.Battery5Bar
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.ui.car.BatteryIcons
import net.prototux.aare.ui.car.Clock
import net.prototux.aare.ui.car.LocalStatusStyle
import net.prototux.aare.ui.car.NetworkStatusIcons
import net.prototux.aare.ui.car.PhoneStatusInfo
import net.prototux.aare.ui.car.StatusSize
import net.prototux.aare.ui.car.StatusStyle
import net.prototux.aare.ui.car.myplayStatusBatteryIcon
import net.prototux.aare.ui.car.myplayStatusBatteryNumber
import net.prototux.aare.ui.car.myplayStatusColor
import net.prototux.aare.ui.car.myplayStatusColorByLevel
import net.prototux.aare.ui.car.myplayStatusPercentSign
import net.prototux.aare.ui.car.myplayStatusSignal
import net.prototux.aare.ui.car.myplayStatusSize
import net.prototux.aare.ui.car.rememberPhoneStatus
import net.prototux.aare.ui.car.statusStyle
import net.prototux.aare.ui.theme.CarTheme
import net.prototux.aare.ui.theme.CarThemes
import net.prototux.aare.ui.theme.forPhone
import net.prototux.aare.ui.theme.withAccent

/** The colours offered for the status icons and the clock, as stored ARGB values. Null ("Default") is the theme's own. */
val STATUS_COLOURS: List<Color> = listOf(
    Color(0xFFFFFFFF), Color(0xFF6EE7A8), Color(0xFF4CC9F0), Color(0xFFFFC24A), Color(0xFFFF5A5F), Color(0xFFC7B0FF),
)

/** A stored ARGB colour as a dot colour, or null for the theme's own. */
fun statusDotColour(argb: Int?): Color? = argb?.let { Color(it) }

/** A tapped dot as the ARGB value to store. */
fun statusArgbOf(colour: Color): Int = colour.toArgb()

/** The three bands of "colour by level", as fractions of a full battery: red up to LOW, amber up to MEDIUM, green above. */
fun levelBands(): Triple<Float, Float, Float> = Triple(StatusStyle.LOW / 100f, StatusStyle.MEDIUM / 100f, 1f)

/**
 * The clock and status icons drawn large by the car's own code, in the look and status style the
 * settings choose now, as on the Battery picture of the design. The phone's real battery and
 * signal are shown (87 % until they are read).
 */
@Composable
fun StatusZoom(style: StatusStyle) {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    val version = rememberSettingsChanges()
    val theme = remember(version) { CarThemes.byId(settings.carThemeId).forPhone(ctx).withAccent(settings.carAccent, ctx) }
    val night = remember(version) { pictureNight(settings.carTheme) }
    val status = rememberPhoneStatus() ?: PhoneStatusInfo(signal = 3, wifi = false, battery = 87, charging = false)
    val density = LocalDensity.current
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(116.dp).clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF17306B), Color(0xFF5B2A86))))
            .border(1.dp, Color(0xFF2F3650), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center,
    ) {
        // Drawn larger than on the car so the details can be seen: half as large again, a little less
        // on a narrow phone so the largest clock and icons still fit side by side.
        val zoom = zoomFor(maxWidth.value)
        CompositionLocalProvider(LocalDensity provides Density(density.density * zoom, density.fontScale)) {
            CarTheme(night = night, theme = theme) {
                CompositionLocalProvider(LocalStatusStyle provides style) {
                    Row(
                        Modifier.padding(horizontal = 10.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f)).padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Clock()
                        Spacer(Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            NetworkStatusIcons(status)
                            BatteryIcons(status)
                        }
                    }
                }
            }
        }
    }
}

/** How much larger than on the car the status strip is drawn, for a strip [widthDp] wide. */
fun zoomFor(widthDp: Float): Float = if (widthDp < 380f) 1.2f else 1.5f

/** A row of size buttons, each drawing [sample] at its size; the chosen one green. */
@Composable
fun SizeButtons(selected: StatusSize, onSelect: (StatusSize) -> Unit, sample: @Composable (StatusSize, Color) -> Unit) {
    PartCard(padding = 12.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            StatusSize.entries.forEach { size ->
                val on = size == selected
                val tint = if (on) PhoneColors.Accent else PhoneColors.Dim
                Column(
                    Modifier.clip(RoundedCornerShape(14.dp)).clickable(role = Role.RadioButton) { onSelect(size) }
                        .semantics { this.selected = on }.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    sample(size, tint)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(sizeName(size)), color = tint, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun sizeName(size: StatusSize): Int = when (size) {
    StatusSize.SMALL -> R.string.phone_size_small
    StatusSize.NORMAL -> R.string.phone_size_normal
    StatusSize.LARGE -> R.string.phone_size_large
    StatusSize.LARGER -> R.string.phone_size_larger
}

/** Colour dots for a status colour: [stored] ARGB or null for the theme's. */
@Composable
fun StatusColourDots(stored: Int?, onChange: (Int?) -> Unit) {
    ColourDots(
        STATUS_COLOURS, STATUS_COLOURS.indices.map { stringResource(R.string.phone_colour_n, it + 1) }, statusDotColour(stored),
        onSelect = { onChange(statusArgbOf(it)) },
        endText = stringResource(R.string.phone_parts_default), onEnd = { onChange(null) },
        endSelected = stored == null || statusDotColour(stored)?.let { selectedColourIndex(STATUS_COLOURS, it) } == null,
    )
}

/**
 * The status icons on the car screen (plan E5, the Battery picture of the design): icon, number
 * and % sign as big symbol buttons, the signal, size as growing batteries, colour as dots, and
 * colour by level with its three bands. Writes the car side's F4 settings (MyPlayStatusSettings).
 */
@Composable
fun StatusIconsPage() {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    val version = rememberSettingsChanges()
    val style = remember(version) { settings.statusStyle }
    Column {
        StatusZoom(style)
        SectionHeading(stringResource(R.string.phone_status_show))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigSymbolButton(Glyph.Symbol(Icons.Rounded.Battery5Bar), stringResource(R.string.phone_status_icon), style.batteryIcon, Modifier.weight(1f)) {
                settings.myplayStatusBatteryIcon = !style.batteryIcon }
            BigSymbolButton(Glyph.Big("87"), stringResource(R.string.phone_status_number), style.batteryNumber, Modifier.weight(1f)) {
                settings.myplayStatusBatteryNumber = !style.batteryNumber }
            BigSymbolButton(Glyph.Big("%"), stringResource(R.string.phone_status_sign), style.percentSign, Modifier.weight(1f)) {
                settings.myplayStatusPercentSign = !style.percentSign }
        }
        if (style.percentSign && !style.batteryNumber) HintLine(Icons.Rounded.Battery5Bar, stringResource(R.string.phone_status_sign_needs_number))
        Spacer(Modifier.height(10.dp))
        RowGroup(rows = listOf {
            SwitchRow(Icons.Rounded.SignalCellularAlt, stringResource(R.string.phone_status_signal), style.signal, TileTone.BLUE,
                stringResource(R.string.phone_status_signal_sub)) { settings.myplayStatusSignal = it }
        })
        SectionHeading(stringResource(R.string.phone_status_size))
        SizeButtons(style.size, { settings.myplayStatusSize = it }) { size, tint ->
            Icon(Icons.Rounded.Battery5Bar, null, tint = tint, modifier = Modifier.size(22.dp * size.scale))
        }
        SectionHeading(stringResource(R.string.phone_status_colour))
        StatusColourDots(style.color) { settings.myplayStatusColor = it }
        HintLine(Icons.Rounded.Battery5Bar, stringResource(R.string.phone_status_colour_hint))
        SectionHeading(stringResource(R.string.phone_status_by_level), end = {
            PhoneSwitch(style.colorByLevel) { settings.myplayStatusColorByLevel = it }
        })
        LevelBar(style.colorByLevel)
    }
}

/** The battery's colour bands, red, amber and green, with their limits; grey when colour by level is off (only red stays). */
@Composable
private fun LevelBar(on: Boolean) {
    val (low, mid, _) = levelBands()
    val red = PhoneColors.Error
    val amber = if (on) PhoneColors.Warning else PhoneColors.SwitchOff
    val green = if (on) PhoneColors.Accent else PhoneColors.SwitchOff
    PartCard(padding = 14.dp) {
        Column {
            Row(Modifier.fillMaxWidth().height(18.dp).clip(RoundedCornerShape(9.dp))) {
                Box(Modifier.weight(low).height(18.dp).background(red))
                Box(Modifier.weight(mid - low).height(18.dp).background(amber))
                Box(Modifier.weight(1f - mid).height(18.dp).background(green))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LevelLegend(red, stringResource(R.string.phone_status_low, StatusStyle.LOW))
                LevelLegend(amber, stringResource(R.string.phone_status_middle, StatusStyle.MEDIUM))
                LevelLegend(green, stringResource(R.string.phone_status_high))
            }
            Spacer(Modifier.height(6.dp))
            Text(stringResource(if (on) R.string.phone_status_by_level_on else R.string.phone_status_by_level_off), color = PhoneColors.Muted, fontSize = 12.5.sp)
        }
    }
}

@Composable
private fun LevelLegend(colour: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(colour))
        Spacer(Modifier.width(5.dp))
        Text(text, color = PhoneColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
