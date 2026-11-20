package net.prototux.aare.ui.phone

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.ui.car.myplayClockBold
import net.prototux.aare.ui.car.myplayClockColor
import net.prototux.aare.ui.car.myplayClockSize
import net.prototux.aare.ui.car.statusStyle

/**
 * The clock on the car screen (plan E5): its size as growing "12:34", bold or not, and its
 * colour. Writes the car side's F4 settings (MyPlayStatusSettings). The Retro look draws its own
 * seven-segment clock, which takes the size and colour but not bold.
 */
@Composable
fun ClockPage() {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    val version = rememberSettingsChanges()
    val style = remember(version) { settings.statusStyle }
    Column {
        StatusZoom(style)
        SectionHeading(stringResource(R.string.phone_status_size))
        SizeButtons(style.clockSize, { settings.myplayClockSize = it }) { size, tint ->
            Text("12:34", color = tint, fontSize = 15.sp * size.scale, fontWeight = if (style.clockBold) FontWeight.Bold else FontWeight.Medium)
        }
        SectionHeading(stringResource(R.string.phone_clock_style))
        RowGroup(rows = listOf {
            SwitchRow(Icons.Rounded.FormatBold, stringResource(R.string.phone_clock_bold), style.clockBold, TileTone.VIOLET,
                stringResource(R.string.phone_clock_bold_sub)) { settings.myplayClockBold = it }
        })
        SectionHeading(stringResource(R.string.phone_status_colour))
        StatusColourDots(style.clockColor) { settings.myplayClockColor = it }
    }
}
