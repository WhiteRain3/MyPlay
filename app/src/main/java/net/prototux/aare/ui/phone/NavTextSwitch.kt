package net.prototux.aare.ui.phone

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Subject
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.host.myplayDebugNavText

/** N3's Waze check: log the texts the maps app sends to the car screen (myplay_debug_nav_text, not backed up). */
@Composable
fun NavTextSwitch() {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    var on by remember { mutableStateOf(settings.myplayDebugNavText) }
    RowGroup(rows = listOf {
        SwitchRow(Icons.AutoMirrored.Rounded.Subject, stringResource(R.string.phone_nav_text_title), on, TileTone.AMBER,
            stringResource(R.string.phone_nav_text_hint)) { on = it; settings.myplayDebugNavText = it }
    })
}
