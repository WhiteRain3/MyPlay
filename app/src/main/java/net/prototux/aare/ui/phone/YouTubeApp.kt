package net.prototux.aare.ui.phone

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.prototux.aare.R
import net.prototux.aare.host.CarRuntime

/** The allowed phone apps with the YouTube app ([pkg]: the resolved one, a patched build included, VA-4) added. */
fun withYouTubeApp(allowed: Set<String>, pkg: String = CarRuntime.YOUTUBE_PACKAGE): Set<String> = allowed + pkg

/** Whether the YouTube app [pkg] is on the car screen: phone apps switched on and it among them. */
fun youTubeAppOnCar(standaloneApps: Boolean, allowed: Set<String>, pkg: String = CarRuntime.YOUTUBE_PACKAGE): Boolean = standaloneApps && pkg in allowed

