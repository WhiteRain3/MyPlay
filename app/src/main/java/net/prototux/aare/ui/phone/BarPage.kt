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

/** The bar on the car screen (plan E5): which side it is on, dark or not, and the weather next to the clock. */
@Composable
fun BarPage() {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    var side by remember { mutableStateOf(settings.railPosition) }
    var darkBar by remember { mutableStateOf(settings.darkBar) }
    var weather by remember { mutableStateOf(settings.showWeather) }
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
        SectionHeading(stringResource(R.string.phone_bar_more))
        RowGroup(rows = listOf(
            { SwitchRow(Icons.Rounded.Contrast, stringResource(R.string.setup_toggle_dark_bar_title), darkBar, TileTone.VIOLET,
                stringResource(R.string.setup_toggle_dark_bar_subtitle)) { darkBar = it; settings.darkBar = it } },
            { SwitchRow(Icons.Rounded.WbSunny, stringResource(R.string.setup_toggle_weather_title), weather, TileTone.AMBER,
                stringResource(R.string.setup_toggle_weather_subtitle)) { weather = it; settings.showWeather = it } },
        ))
    }
}
