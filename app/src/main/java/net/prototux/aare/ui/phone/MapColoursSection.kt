package net.prototux.aare.ui.phone

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.ui.theme.MapColourFilter
import net.prototux.aare.ui.theme.myplayMapFilter

/** The map filter's setting, as the car side stores it (Settings.myplayMapFilter). */
private const val MAP_FILTER_KEY = "myplay_map_filter"

/** The made-up map the preview draws: land, water, park, small and main roads, the route, a label. */
enum class SampleMapPart { LAND, WATER, PARK, ROAD, MAIN_ROAD, ROUTE, LABEL }

/** The sample map's colours (0xRRGGBB) by day and by night, roughly as maps apps draw them. */
fun sampleMapColour(part: SampleMapPart, night: Boolean): Int = when (part) {
    SampleMapPart.LAND -> if (night) 0x242F3E else 0xF1EFE8
    SampleMapPart.WATER -> if (night) 0x17263C else 0xA8CCF0
    SampleMapPart.PARK -> if (night) 0x263C3F else 0xC8E6C0
    SampleMapPart.ROAD -> if (night) 0x38414E else 0xFFFFFF
    SampleMapPart.MAIN_ROAD -> if (night) 0x746855 else 0xF7D774
    SampleMapPart.ROUTE -> if (night) 0x669DF6 else 0x4285F4
    SampleMapPart.LABEL -> if (night) 0x9CA5B3 else 0x5F6368
}

/** The sample map's colours through [filter] ([night] for night's darker step), as the car would show them. */
fun filteredSampleMap(filter: MapColourFilter, night: Boolean): Map<SampleMapPart, Int> {
    val m = filter.matrix(night)
    return SampleMapPart.entries.associateWith { p -> sampleMapColour(p, night).let { if (m == null) it else MapColourFilter.apply(m, it) } }
}

/** The preset [filter] is exactly, or null for the owner's own values. */
fun mapPresetOf(filter: MapColourFilter): String? = MapColourFilter.PRESETS.entries.firstOrNull { it.value == filter.clamped() }?.key

private val PRESET_NAMES = mapOf(
    "none" to R.string.phone_map_preset_none, "muted" to R.string.phone_map_preset_muted, "cool" to R.string.phone_map_preset_cool,
    "warm" to R.string.phone_map_preset_warm, "dark_night" to R.string.phone_map_preset_dark_night, "grey" to R.string.phone_map_preset_grey,
)

/**
 * Map colours (the car side's map colour filter): a filter on the hosted maps app's picture, with a
 * before/after preview on a made-up map ([night] or day), presets and the filter's own values.
 * Stored in myplay_map_filter, one of a whole look's settings: saving or choosing a look keeps it
 * with the look.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapColoursSection(settings: Settings, night: Boolean) {
    var filter by remember { mutableStateOf(settings.myplayMapFilter) }
    // Follows a change made elsewhere while the page stays open (a look chosen, a backup restored).
    DisposableEffect(settings) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == MAP_FILTER_KEY) filter = settings.myplayMapFilter
        }
        settings.prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { settings.prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    var pickingTint by remember { mutableStateOf(false) }
    fun save(f: MapColourFilter) { filter = f.clamped(); settings.myplayMapFilter = filter }

    SectionHeading(stringResource(R.string.phone_map_colours))
    Text(stringResource(R.string.phone_map_colours_for), color = PhoneColors.Muted, fontSize = 14.sp, modifier = Modifier.padding(bottom = 10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SampleMap(filteredSampleMap(MapColourFilter(), night), stringResource(R.string.phone_map_before), Modifier.weight(1f))
        SampleMap(filteredSampleMap(filter, night), stringResource(R.string.phone_map_after), Modifier.weight(1f))
    }
    Spacer(Modifier.height(12.dp))
    val preset = mapPresetOf(filter)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MapColourFilter.PRESETS.forEach { (key, f) ->
            PillButton(stringResource(PRESET_NAMES[key] ?: R.string.phone_map_preset_none), main = key == preset) { save(f) }
        }
    }
    Spacer(Modifier.height(8.dp))
    PartCard {
        Column(Modifier.padding(16.dp)) {
            FilterSlider(stringResource(R.string.phone_map_hue, filter.hueShift), filter.hueShift, -180..180, { filter = filter.copy(hueShift = it) }) { save(filter) }
            FilterSlider(stringResource(R.string.phone_map_saturation, filter.saturation), filter.saturation, 0..200, { filter = filter.copy(saturation = it) }) { save(filter) }
            FilterSlider(stringResource(R.string.phone_map_brightness, filter.brightness), filter.brightness, -100..100, { filter = filter.copy(brightness = it) }) { save(filter) }
            FilterSlider(stringResource(R.string.phone_map_contrast, filter.contrast), filter.contrast, 0..200, { filter = filter.copy(contrast = it) }) { save(filter) }
            val tintLabel = stringResource(R.string.phone_map_tint_colour)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    FilterSlider(stringResource(R.string.phone_map_tint, filter.tintStrength), filter.tintStrength, 0..100, { filter = filter.copy(tintStrength = it) }) { save(filter) }
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF000000.toInt() or filter.tint))
                        .border(1.dp, PhoneColors.Border, RoundedCornerShape(10.dp))
                        .clickable(onClickLabel = tintLabel, role = Role.Button) { pickingTint = true }
                        .semantics { contentDescription = tintLabel },
                )
            }
            FilterSlider(stringResource(R.string.phone_map_night_darker, filter.nightDarker), filter.nightDarker, 0..90, { filter = filter.copy(nightDarker = it) }) { save(filter) }
        }
    }
    Spacer(Modifier.height(10.dp))
    HintLine(Icons.Rounded.Speed, stringResource(R.string.phone_map_what))
    HintLine(Icons.Rounded.Info, stringResource(R.string.phone_map_with_look))
    if (pickingTint) {
        ColourPickerDialog(stringResource(R.string.phone_map_tint_colour), 0xFF000000.toInt() or filter.tint, onDismiss = { pickingTint = false }) { c ->
            pickingTint = false
            save(filter.copy(tint = c and 0xFFFFFF, tintStrength = if (filter.tintStrength == 0) 20 else filter.tintStrength))
        }
    }
}

@Composable
private fun FilterSlider(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit, onDone: () -> Unit) {
    Text(label, color = PhoneColors.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    Slider(
        value.toFloat(), { onChange(Math.round(it)) }, valueRange = range.first.toFloat()..range.last.toFloat(),
        onValueChangeFinished = onDone,
        colors = SliderDefaults.colors(thumbColor = PhoneColors.Accent, activeTrackColor = PhoneColors.Accent, inactiveTrackColor = PhoneColors.SwitchOff),
    )
}

/** The made-up map in [colours], with [caption] under it. */
@Composable
private fun SampleMap(colours: Map<SampleMapPart, Int>, caption: String, modifier: Modifier) {
    fun c(p: SampleMapPart) = Color(0xFF000000.toInt() or colours.getValue(p))
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(1.4f).clip(RoundedCornerShape(12.dp)).border(1.dp, PhoneColors.Border, RoundedCornerShape(12.dp))) {
            val w = size.width
            val h = size.height
            drawRect(c(SampleMapPart.LAND))
            drawPath(Path().apply { moveTo(w * 0.6f, 0f); lineTo(w, 0f); lineTo(w, h * 0.55f); quadraticTo(w * 0.75f, h * 0.45f, w * 0.6f, 0f) }, c(SampleMapPart.WATER))
            drawRect(c(SampleMapPart.PARK), Offset(w * 0.08f, h * 0.55f), androidx.compose.ui.geometry.Size(w * 0.3f, h * 0.3f))
            val thin = 4.dp.toPx()
            drawLine(c(SampleMapPart.ROAD), Offset(0f, h * 0.35f), Offset(w * 0.7f, h * 0.35f), thin)
            drawLine(c(SampleMapPart.ROAD), Offset(w * 0.45f, h * 0.35f), Offset(w * 0.45f, h), thin)
            drawLine(c(SampleMapPart.MAIN_ROAD), Offset(0f, h * 0.9f), Offset(w, h * 0.5f), thin * 1.8f)
            drawLine(c(SampleMapPart.ROUTE), Offset(w * 0.45f, h), Offset(w * 0.45f, h * 0.35f), thin * 1.4f, cap = StrokeCap.Round)
            drawLine(c(SampleMapPart.ROUTE), Offset(w * 0.45f, h * 0.35f), Offset(w * 0.15f, h * 0.35f), thin * 1.4f, cap = StrokeCap.Round)
            drawCircle(c(SampleMapPart.LABEL), 3.dp.toPx(), Offset(w * 0.22f, h * 0.18f))
            drawLine(c(SampleMapPart.LABEL), Offset(w * 0.26f, h * 0.18f), Offset(w * 0.42f, h * 0.18f), 2.dp.toPx())
        }
        Text(caption, color = PhoneColors.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}
