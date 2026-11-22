package net.prototux.aare.ui.phone

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.prototux.aare.R
import net.prototux.aare.host.CarRuntime

/** The allowed phone apps with the YouTube app added. */
fun withYouTubeApp(allowed: Set<String>): Set<String> = allowed + CarRuntime.YOUTUBE_PACKAGE

/** Whether the YouTube app is on the car screen: phone apps switched on and YouTube among them. */
fun youTubeAppOnCar(standaloneApps: Boolean, allowed: Set<String>): Boolean = standaloneApps && CarRuntime.YOUTUBE_PACKAGE in allowed

/**
 * The owner: the real YouTube app on the car screen, not the website. One switch on the existing
 * "phone apps on the car screen" path (root or Shizuku, parked only): on, after the same warning,
 * it switches phone apps on and adds YouTube; off, it takes YouTube off again. [available] is
 * whether root or Shizuku is there.
 */
@Composable
fun YouTubeAppRow(onCar: Boolean, available: Boolean, onToggle: () -> Unit) {
    RowGroup(rows = listOf {
        SwitchRow(
            Icons.Rounded.SmartDisplay, stringResource(R.string.phone_youtube_app_title), onCar, TileTone.CORAL,
            stringResource(if (available) R.string.phone_youtube_app_hint else R.string.phone_youtube_app_needs_root),
        ) { onToggle() }
    })
}
