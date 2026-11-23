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

