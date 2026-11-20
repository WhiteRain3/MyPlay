package net.prototux.aare.ui.car

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.prototux.aare.R
import net.prototux.aare.car.CarData
import net.prototux.aare.car.Weather
import net.prototux.aare.car.usesFahrenheit
import kotlin.math.roundToInt

/** A status item the rail or taskbar can show, chosen and ordered on the phone. */
enum class RailWidget(
    /** Stored in the settings: never change it once released. */
    val id: String,
) {
    CLOCK("clock"),
    TEMPERATURE("temperature"),
    BATTERY("battery"),
    WEATHER("weather");

    companion object {
        /** The widgets of a saved list of ids, in order: unknown ids dropped, each widget once. */
        fun parse(ids: List<String>): List<RailWidget> = ids.mapNotNull { id -> entries.firstOrNull { it.id == id } }.distinct()

        /** Before the rail was configurable: the clock, the phone's battery and the weather (Settings.railWidgets' own default). */
        val DEFAULT = listOf(CLOCK, BATTERY, WEATHER)
    }
}

/** This order with [widget] shown (added at the end) or hidden. */
internal fun List<RailWidget>.withWidget(widget: RailWidget, shown: Boolean): List<RailWidget> = when {
    shown -> if (widget in this) this else this + widget
    else -> this - widget
}

/**
 * The widgets actually drawn. Without a car temperature sensor the temperature widget falls back
 * to the fetched weather, so when the weather widget is chosen too it would show the same chip
 * twice: then the temperature widget gives way.
 */
internal fun List<RailWidget>.forDisplay(carHasTemperature: Boolean): List<RailWidget> =
    if (!carHasTemperature && RailWidget.WEATHER in this) this - RailWidget.TEMPERATURE else this

/**
 * The car's outside temperature for the rail, in the unit the dashboard's weather uses: the car
 * reports Celsius (AAP's Environment sensor carries no display unit), so it follows the phone's
 * locale like the fetched weather does ([usesFahrenheit]), and matches its "°" / "°F" style.
 */
internal fun outsideTemperatureLabel(celsius: Float, fahrenheit: Boolean): String =
    if (fahrenheit) "${(celsius * 9 / 5 + 32).roundToInt()}°F" else "${celsius.roundToInt()}°"

/**
 * The rail's chosen status widgets, laid out for the layout they're in: stacked vertically on a
 * side rail, in a row on a bottom bar (horizontal), each [spacing] apart. [phone] is the rail's
 * one polled phone status (rememberPhoneStatus), shared with its network icons.
 */
@Composable
internal fun RailWidgets(widgets: List<RailWidget>, weather: Weather?, phone: PhoneStatusInfo?, horizontal: Boolean, spacing: Dp, weatherOn: Boolean = weather != null) {
    val car by CarData.state.collectAsState()
    val carTemperature = car.environment?.value?.temperatureC
    val content: @Composable () -> Unit = {
        widgets.forDisplay(carHasTemperature = carTemperature != null).forEach { widget ->
            when (widget) {
                RailWidget.CLOCK -> Clock()
                RailWidget.TEMPERATURE -> OutsideTemperatureWidget(carTemperature, weather)
                RailWidget.BATTERY -> BatteryWidget(phone)
                // MyPlay (P1, P1-1): a dimmed placeholder when the weather is on but has no reading yet
                // (first fetch, no location); with the weather switched off, nothing, as before
                // (the weather item is in the default list, so most bars would show it otherwise).
                RailWidget.WEATHER -> if (weather != null) WeatherChip(weather) else if (weatherOn) NoReading(Icons.Rounded.Cloud)
            }
        }
    }
    if (horizontal) Row(horizontalArrangement = Arrangement.spacedBy(spacing), verticalAlignment = Alignment.CenterVertically) { content() }
    else Column(verticalArrangement = Arrangement.spacedBy(spacing), horizontalAlignment = Alignment.CenterHorizontally) { content() }
}

/**
 * The temperature outside, from the car's own sensor when it reports one; else the weather the
 * dashboard already fetches (same source as the weather widget, with its icon too).
 */
@Composable
internal fun OutsideTemperatureWidget(carTemperature: Float?, weather: Weather?) {
    if (carTemperature != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Thermostat, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
            Text(outsideTemperatureLabel(carTemperature, usesFahrenheit()), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    } else if (weather != null) {
        WeatherChip(weather)
    } else {
        NoReading(Icons.Rounded.Thermostat) // MyPlay (P1)
    }
}

/**
 * MyPlay (P1): a chosen bar item with no reading (no car sensor, weather off or not fetched yet):
 * its icon and "–°", dimmed, so the driver sees the item is there but has nothing to show. The
 * phone's Bar page says why.
 */
@Composable
private fun NoReading(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val dim = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = dim, modifier = Modifier.size(18.dp))
        Text(NO_READING, style = MaterialTheme.typography.labelLarge, color = dim)
    }
}

/** What a bar item without a reading shows (MyPlay, P1). */
internal const val NO_READING = "–°"

/** Just the phone's battery (gauge, charging bolt or percent): one of the rail's status widgets. */
@Composable
internal fun BatteryWidget(status: PhoneStatusInfo?) {
    val s = status ?: return
    // MyPlay (F4-5): with the icon and the number both off, only charging still shows something.
    val look = LocalStatusStyle.current
    if (!look.batteryIcon && look.batteryText(s.battery) == null && !s.charging) return
    // MyPlay (F4): drawn as the status style says, like PhoneStatusIcons.
    Row(Modifier.carPart(CarPart.Status), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        BatteryIcons(s)
    }
}

/**
 * The rail's status widgets, for the car's and the phone's settings: each shown or hidden, and the
 * shown ones moved up or down. Mirrors DashboardCardsEditor, for the dashboard's own cards.
 */
@Composable
internal fun RailWidgetsEditor(order: List<RailWidget>, rowHeight: Dp, onChange: (List<RailWidget>) -> Unit) {
    Column {
        Text(stringResource(R.string.rail_widgets_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.rail_widgets_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        (order + RailWidget.entries.filter { it !in order }).forEach { widget ->
            val i = order.indexOf(widget)
            val shown = i >= 0
            Row(Modifier.fillMaxWidth().heightIn(min = rowHeight), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).heightIn(min = rowHeight).carFocusHighlight(MaterialTheme.shapes.small).clickable { onChange(order.withWidget(widget, !shown)) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = shown, onCheckedChange = null)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(widget.label), style = MaterialTheme.typography.bodyLarge)
                }
                if (shown) {
                    IconButton(onClick = { onChange(order.movedBy(i, -1)) }, enabled = i > 0, modifier = Modifier.size(rowHeight).carFocusHighlight(CircleShape)) {
                        Icon(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.common_up))
                    }
                    IconButton(onClick = { onChange(order.movedBy(i, 1)) }, enabled = i < order.lastIndex, modifier = Modifier.size(rowHeight).carFocusHighlight(CircleShape)) {
                        Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.common_down))
                    }
                }
            }
        }
    }
}

private val RailWidget.label: Int
    get() = when (this) {
        RailWidget.CLOCK -> R.string.rail_widget_clock
        RailWidget.TEMPERATURE -> R.string.rail_widget_temperature
        RailWidget.BATTERY -> R.string.rail_widget_battery
        RailWidget.WEATHER -> R.string.rail_widget_weather
    }
