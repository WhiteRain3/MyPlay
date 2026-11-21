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
}

/**
 * The speed limit file (N21): choose the region, download it on Wi-Fi (looked for again monthly,
 * when the app or this page opens), see its size and date, delete it. The car screen reads the
 * limit from it (N20). Says what the data cannot know.
 */
@Composable
fun SpeedLimitPage() {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    val scope = rememberCoroutineScope()
    var region by remember { mutableStateOf(settings.myplaySpeedLimitRegion) }
    val state by SpeedLimitDownload.state.collectAsState()
    // Read again after each download or delete.
    var changes by remember { mutableIntStateOf(0) }
    val kept = remember(region, state, changes) { if (region.isEmpty()) null else SpeedLimitDownload.kept(ctx, region) }
    LaunchedEffect(Unit) { SpeedLimitDownload.checkIfDue(ctx) }

    Column {
        SpeedSourceChoice(settings)
        SectionHeading(stringResource(R.string.phone_speed_region))
        PhoneChoices(
            stringResource(R.string.phone_speed_region_which),
            listOf("" to stringResource(R.string.phone_speed_region_none)) + SpeedLimitFiles.REGIONS.map { it.id to it.name },
            region,
        ) { id ->
            region = id; settings.myplaySpeedLimitRegion = id
            // Another region's file is of no use any more: it goes (SL-3).
            SpeedLimitDownload.deleteOthers(ctx, id)
            if (id.isNotEmpty()) scope.launch { SpeedLimitDownload.update(ctx, force = true) }
        }
        if (region.isNotEmpty()) {
            SectionHeading(stringResource(R.string.phone_speed_file))
            RowGroup(rows = listOf {
                SimpleRow(
                    Icons.Rounded.Map, SpeedLimitFiles.REGIONS.firstOrNull { it.id == region }?.name ?: region, TileTone.GREEN,
                    if (kept == null) stringResource(R.string.phone_speed_not_downloaded)
                    else stringResource(R.string.phone_speed_kept, Formatter.formatShortFileSize(ctx, kept.bytes), kept.built),
                ) {}
            })
            speedLimitStatus(state)?.let { res ->
                val percent = (state as? SpeedLimitDownload.State.Downloading)?.percent
                HintLine(if (state == SpeedLimitDownload.State.NeedsWifi) Icons.Rounded.Wifi else Icons.Rounded.Download,
                    stringResource(res) + (percent?.let { " $it %" } ?: ""))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(stringResource(if (kept == null) R.string.phone_speed_download else R.string.phone_speed_look_now)) {
                    scope.launch { SpeedLimitDownload.update(ctx, force = true) }
                }
                if (kept != null) PillButton(stringResource(R.string.phone_speed_delete), main = false) {
                    SpeedLimitDownload.delete(ctx, region); changes++
                }
            }
            HintLine(Icons.Rounded.Wifi, stringResource(R.string.phone_speed_wifi_only))
        }
        HintLine(Icons.Rounded.Speed, stringResource(R.string.phone_speed_hint_only))
        HintLine(Icons.Rounded.Info, stringResource(R.string.phone_speed_offline))
        Text(stringResource(R.string.phone_osm_notice), color = PhoneColors.Muted, fontSize = 12.sp)
    }
}
