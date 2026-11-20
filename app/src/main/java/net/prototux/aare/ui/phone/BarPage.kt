package net.prototux.aare.ui.phone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
import net.prototux.aare.ui.car.CarLayout
import net.prototux.aare.ui.car.changeLayout
import net.prototux.aare.ui.car.myplayLayoutSpec
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

/**
 * Which of the Bar page's controls the car screen's [layout] uses. Read by hand from the layouts'
 * drawing code (CarShell, DockShell, ClassicShell, SpecShell); when a layout starts or stops
 * reading one of these settings, change it here too.
 */
data class BarControls(val taskbarSide: Boolean, val items: Boolean, val darkBar: Boolean, val weather: Boolean, val mapBehindBar: Boolean)

fun barControlsFor(layout: CarLayout): BarControls = when (layout) {
    CarLayout.TASKBAR -> BarControls(taskbarSide = true, items = true, darkBar = true, weather = true, mapBehindBar = false)
    CarLayout.MAP_BEHIND -> BarControls(taskbarSide = false, items = false, darkBar = false, weather = true, mapBehindBar = true)
    CarLayout.DOCK -> BarControls(taskbarSide = false, items = false, darkBar = false, weather = true, mapBehindBar = false)
    CarLayout.CLASSIC -> BarControls(taskbarSide = false, items = false, darkBar = false, weather = false, mapBehindBar = false)
}

/** The name of a layout as the Layout page shows it. */
@Composable
fun layoutName(layout: CarLayout): String = stringResource(when (layout) {
    CarLayout.TASKBAR -> R.string.setup_layout_taskbar
    CarLayout.DOCK -> R.string.setup_layout_dock
    CarLayout.CLASSIC -> R.string.setup_layout_classic
    CarLayout.MAP_BEHIND -> R.string.myplay_layout_map_behind
})

/** "You use the X layout", what this page changes for it, and a button to the Layout page. */
@Composable
fun LayoutNote(layout: CarLayout, text: String, openLayout: () -> Unit) {
    Spacer(Modifier.height(12.dp))
    PartCard(padding = 14.dp) {
        Column {
            Text(stringResource(R.string.phone_layout_in_use, layoutName(layout)), color = PhoneColors.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(text, color = PhoneColors.Muted, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            PillButton(stringResource(R.string.phone_layout_change), main = false, onClick = openLayout)
        }
    }
}

/**
 * The bar on the car screen (plan E5). Shows the controls the layout in use reads: with "map
 * behind" its own bar options (look and side); with the taskbar its side, items and dark bar;
 * the weather for the layouts that show it. A note says which layout is in use and what applies.
 */
@Composable
fun BarPage(openLayout: () -> Unit) {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    val version = rememberSettingsChanges()
    val layout = remember(version) { CarLayout.byId(settings.carLayoutId) }
    val spec = remember(version) { settings.myplayLayoutSpec }
    val controls = barControlsFor(layout)
    var side by remember { mutableStateOf(settings.railPosition) }
    var darkBar by remember { mutableStateOf(settings.darkBar) }
    var weather by remember { mutableStateOf(settings.showWeather) }
    var items by remember { mutableStateOf(RailWidget.parse(settings.railWidgets)) }
    fun changeItems(new: List<RailWidget>) { items = new; settings.railWidgets = new.map(RailWidget::id) }
    Column {
        PartPicture()
        LayoutNote(layout, stringResource(when (layout) {
            CarLayout.TASKBAR -> R.string.phone_bar_note_taskbar
            CarLayout.MAP_BEHIND -> R.string.phone_bar_note_map_behind
            CarLayout.DOCK -> R.string.phone_bar_note_dock
            CarLayout.CLASSIC -> R.string.phone_bar_note_classic
        }), openLayout)
        if (controls.mapBehindBar) {
            SectionHeading(stringResource(R.string.phone_layout_bar))
            MapBehindBar(spec) { f -> settings.changeLayout(f) }
        }
        if (controls.taskbarSide) {
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
        }
        if (controls.items) {
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
            HintLine(Icons.Rounded.WbSunny, stringResource(R.string.phone_bar_items_weather_hint))
        }
        val rows = buildList<@Composable () -> Unit> {
            if (controls.darkBar) add {
                SwitchRow(Icons.Rounded.Contrast, stringResource(R.string.setup_toggle_dark_bar_title), darkBar, TileTone.VIOLET,
                    stringResource(R.string.setup_toggle_dark_bar_subtitle)) { darkBar = it; settings.darkBar = it }
            }
            if (controls.weather) add {
                SwitchRow(Icons.Rounded.WbSunny, stringResource(R.string.setup_toggle_weather_title), weather, TileTone.AMBER,
                    stringResource(R.string.setup_toggle_weather_subtitle)) { weather = it; settings.showWeather = it }
            }
        }
        if (rows.isNotEmpty()) {
            SectionHeading(stringResource(R.string.phone_bar_more))
            RowGroup(rows = rows)
        }
    }
}
