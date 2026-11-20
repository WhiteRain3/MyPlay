package net.prototux.aare.ui.phone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BorderBottom
import androidx.compose.material.icons.rounded.BorderLeft
import androidx.compose.material.icons.rounded.BorderRight
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Battery5Bar
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.ui.car.RailWidget
import net.prototux.aare.ui.car.movedBy
import net.prototux.aare.ui.car.withWidget

/** The live car picture at the top of every part page: it shows each change at once. Not tappable. */
@Composable
fun PartPicture() {
    CarPicture(stringResource(R.string.phone_part_picture_description), onPart = null)
}

/** A row of big symbol buttons where exactly one is chosen. [options]: stored value, symbol, label. */
@Composable
fun ChoiceButtons(options: List<Triple<String, ImageVector, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, icon, label) ->
            BigSymbolButton(Glyph.Symbol(icon), label, value == selected, Modifier.weight(1f)) { onSelect(value) }
        }
    }
}

/** The rows of the bar's items: the shown ones in their order, then the others. */
fun barItemRows(order: List<RailWidget>): List<RailWidget> = order + RailWidget.entries.filter { it !in order }

private val RailWidget.symbol: ImageVector
    get() = when (this) {
        RailWidget.CLOCK -> Icons.Rounded.Schedule
        RailWidget.TEMPERATURE -> Icons.Rounded.Thermostat
        RailWidget.BATTERY -> Icons.Rounded.Battery5Bar
        RailWidget.WEATHER -> Icons.Rounded.WbSunny
    }

private val RailWidget.title: Int
    get() = when (this) {
        RailWidget.CLOCK -> R.string.rail_widget_clock
        RailWidget.TEMPERATURE -> R.string.rail_widget_temperature
        RailWidget.BATTERY -> R.string.rail_widget_battery
        RailWidget.WEATHER -> R.string.rail_widget_weather
    }

/** The bar on the car screen (plan E5): which side it is on, what it shows, dark or not, and the weather. */
@Composable
fun BarPage() {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    var side by remember { mutableStateOf(settings.railPosition) }
    var darkBar by remember { mutableStateOf(settings.darkBar) }
    var weather by remember { mutableStateOf(settings.showWeather) }
    var items by remember { mutableStateOf(RailWidget.parse(settings.railWidgets)) }
    fun changeItems(new: List<RailWidget>) { items = new; settings.railWidgets = new.map(RailWidget::id) }
    Column {
        PartPicture()
        SectionHeading(stringResource(R.string.phone_bar_side))
        ChoiceButtons(
            listOf(
                Triple(Settings.RAIL_AUTO, Icons.Rounded.Person, stringResource(R.string.setup_choice_bar_auto)),
                Triple(Settings.RAIL_LEFT, Icons.Rounded.BorderLeft, stringResource(R.string.setup_choice_left)),
                Triple(Settings.RAIL_RIGHT, Icons.Rounded.BorderRight, stringResource(R.string.setup_choice_right)),
                Triple(Settings.RAIL_BOTTOM, Icons.Rounded.BorderBottom, stringResource(R.string.setup_choice_bottom)),
            ),
            side,
        ) { side = it; settings.railPosition = it }
        SectionHeading(stringResource(R.string.phone_bar_items))
        RowGroup(rows = barItemRows(items).map { item ->
            {
                val i = items.indexOf(item)
                val shown = i >= 0
                SimpleRow(item.symbol, stringResource(item.title), TileTone.BLUE) {
                    if (shown) {
                        RoundIconButton(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.common_up), enabled = i > 0, size = 36.dp) { changeItems(items.movedBy(i, -1)) }
                        RoundIconButton(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.common_down), enabled = i < items.lastIndex, size = 36.dp) { changeItems(items.movedBy(i, 1)) }
                    }
                    PhoneSwitch(shown) { on -> changeItems(items.withWidget(item, on)) }
                }
            }
        })
        HintLine(Icons.Rounded.Info, stringResource(R.string.phone_bar_items_hint))
        SectionHeading(stringResource(R.string.phone_bar_more))
        RowGroup(rows = listOf(
            { SwitchRow(Icons.Rounded.Contrast, stringResource(R.string.setup_toggle_dark_bar_title), darkBar, TileTone.VIOLET,
                stringResource(R.string.setup_toggle_dark_bar_subtitle)) { darkBar = it; settings.darkBar = it } },
            { SwitchRow(Icons.Rounded.WbSunny, stringResource(R.string.setup_toggle_weather_title), weather, TileTone.AMBER,
                stringResource(R.string.setup_toggle_weather_subtitle)) { weather = it; settings.showWeather = it } },
        ))
    }
}
