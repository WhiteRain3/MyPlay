package net.prototux.aare.ui.phone

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.car.MirroredKeys
import net.prototux.aare.car.myplayMirrorTurns

/** [all] (myplay_mirror_turns) with [packageName]'s knob turn set to [mode]; the default is not stored. */
fun withMirrorTurn(all: String?, packageName: String, mode: MirroredKeys.TurnMode): String =
    MirroredKeys.encodeTurns(MirroredKeys.decodeTurns(all) + (packageName to mode))

