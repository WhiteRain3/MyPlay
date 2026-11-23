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

