package net.prototux.aare.ui.phone

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.car.SpeedSourceMode
import net.prototux.aare.car.myplaySpeedSource

/** The words for each speed source, in the order shown. */
val SPEED_SOURCE_NAMES: List<Pair<SpeedSourceMode, Int>> = listOf(
    SpeedSourceMode.AUTO to R.string.phone_speed_source_auto,
    SpeedSourceMode.CAR to R.string.phone_speed_source_car,
    SpeedSourceMode.OFF to R.string.phone_speed_source_off,
)

/** N3: where the car screen's speed comes from (myplay_speed_source, the car side's setting). */
@Composable
fun SpeedSourceChoice(settings: Settings) {
    var mode by remember { mutableStateOf(settings.myplaySpeedSource) }
    SectionHeading(stringResource(R.string.phone_speed_source_title))
    PhoneChoices(
        stringResource(R.string.phone_speed_source_which),
        SPEED_SOURCE_NAMES.map { (m, res) -> m.id to stringResource(res) },
        mode.id,
    ) { id -> SpeedSourceMode.byId(id).let { mode = it; settings.myplaySpeedSource = it } }
    HintLine(Icons.Rounded.Info, stringResource(when (mode) {
        SpeedSourceMode.AUTO -> R.string.phone_speed_source_auto_hint
        SpeedSourceMode.CAR -> R.string.phone_speed_source_car_hint
        SpeedSourceMode.OFF -> R.string.phone_speed_source_off_hint
    }))
}
