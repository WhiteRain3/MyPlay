package net.prototux.aare.ui.phone

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.ui.theme.CarControlSize
import net.prototux.aare.ui.theme.CarTextSize

/** The density choices: 0 is Auto, then fixed dpi as upstream offers (120 to 240), with the Audi's 213 among them. */
val DENSITY_CHOICES: List<Int> = listOf(0, 120, 160, 200, 213, 240)

/** The density pills: [DENSITY_CHOICES], plus a stored value in its place when it is not one of them, so it shows as it is. */
fun densityOptions(stored: Int): List<Int> =
    if (stored in DENSITY_CHOICES) DENSITY_CHOICES else (DENSITY_CHOICES + stored).sorted()

/**
 * How big things are on the car screen (plan D3 on the phone): text size and control size, which
 * show in the picture at once, and the car screen's density, which applies from the next
 * connection. Same settings as upstream AARE's (car_text_size, car_control_size, car_density).
 */
@Composable
fun SizePage() {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    var text by remember { mutableStateOf(CarTextSize.byId(settings.carTextSize).id) }
    var controls by remember { mutableStateOf(CarControlSize.byId(settings.carControlSize).id) }
    var density by remember { mutableIntStateOf(settings.carDensity) }
    Column {
        PartPicture()
        SectionHeading(stringResource(R.string.phone_size_text_controls))
        PartCard(padding = 16.dp) {
            Column {
                PhoneChoices(stringResource(R.string.setup_choice_text_size), CarTextSize.entries.map { it.id to stringResource(it.labelRes) }, text) {
                    text = it; settings.carTextSize = it
                }
                PhoneChoices(stringResource(R.string.setup_choice_control_size), CarControlSize.entries.map { it.id to stringResource(it.labelRes) }, controls) {
                    controls = it; settings.carControlSize = it
                }
            }
        }
        HintLine(Icons.Rounded.Info, stringResource(R.string.phone_size_text_controls_hint))
        SectionHeading(stringResource(R.string.setup_choice_density))
        PartCard(padding = 16.dp) {
            Column {
                val auto = stringResource(R.string.setup_choice_density_auto)
                PhoneChoices(
                    stringResource(R.string.phone_size_density_title),
                    densityOptions(density).map { it.toString() to if (it == 0) auto else stringResource(R.string.phone_size_dpi, it) },
                    density.toString(),
                ) { v -> v.toIntOrNull()?.let { density = it; settings.carDensity = it } }
            }
        }
        HintLine(Icons.Rounded.Info, stringResource(R.string.phone_size_density_hint))
        HintLine(Icons.Rounded.Info, stringResource(R.string.setup_hint_density))
    }
}
