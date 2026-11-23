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

