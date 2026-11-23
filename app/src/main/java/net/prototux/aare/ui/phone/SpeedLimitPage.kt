package net.prototux.aare.ui.phone

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.car.SpeedRules
import net.prototux.aare.car.myplaySpeedLimitMargin

/** What the page says about the download now; null when there is nothing to say. */
fun speedLimitStatus(state: SpeedLimitDownload.State): Int? = when (state) {
    SpeedLimitDownload.State.Idle -> null
    SpeedLimitDownload.State.Checking -> R.string.phone_speed_checking
    is SpeedLimitDownload.State.Downloading -> R.string.phone_speed_downloading
    SpeedLimitDownload.State.UpToDate -> R.string.phone_speed_up_to_date
    SpeedLimitDownload.State.Done -> R.string.phone_speed_done
    SpeedLimitDownload.State.NeedsWifi -> R.string.phone_speed_needs_wifi
    SpeedLimitDownload.State.Failed -> R.string.phone_speed_failed
    SpeedLimitDownload.State.NotPublished -> R.string.phone_speed_not_published
    SpeedLimitDownload.State.NoInternet -> R.string.phone_speed_no_internet
}

