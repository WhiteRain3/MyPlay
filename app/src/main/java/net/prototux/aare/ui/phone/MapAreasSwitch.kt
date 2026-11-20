package net.prototux.aare.ui.phone

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.host.templates.myplayDebugMapAreas

/** A check for the owner: outline on the car screen the part of the map MyPlay tells the maps app is free (setting myplay_debug_map_areas). */
@Composable
fun MapAreasSwitch() {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    var on by remember { mutableStateOf(settings.myplayDebugMapAreas) }
    RowGroup(rows = listOf {
        SwitchRow(Icons.Rounded.CropFree, stringResource(R.string.phone_map_areas_title), on, TileTone.AMBER,
            stringResource(R.string.phone_map_areas_hint)) { on = it; settings.myplayDebugMapAreas = it }
    })
}
