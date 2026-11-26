package net.prototux.aare.ui.phone

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.prototux.aare.R
import net.prototux.aare.host.CarRuntime

/** The allowed phone apps with the YouTube app ([pkg]: the resolved one, a patched build included, VA-4) added. */
fun withYouTubeApp(allowed: Set<String>, pkg: String = CarRuntime.YOUTUBE_PACKAGE): Set<String> = allowed + pkg

/**
 * VA-14: the allowed phone apps after the Video app changed from [old] to [now]: when the old one
 * was on the car, the new one takes its place; otherwise nothing changes.
 */
fun swappedVideoApp(allowed: Set<String>, old: String?, now: String?): Set<String> =
    if (old == null || now == null || old == now || old !in allowed) allowed else allowed - old + now

/** Whether the YouTube app [pkg] is on the car screen: phone apps switched on and it among them. */
fun youTubeAppOnCar(standaloneApps: Boolean, allowed: Set<String>, pkg: String = CarRuntime.YOUTUBE_PACKAGE): Boolean = standaloneApps && pkg in allowed

