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

/**
 * G1: what turning the knob does inside one mirrored phone app: move up and down (lists, most
 * apps) or left and right (rows). Shown under an app the owner allowed on the car screen.
 */
@Composable
fun MirrorTurnChoice(settings: Settings, packageName: String) {
    var mode by remember(packageName) { mutableStateOf(MirroredKeys.turnFor(settings.myplayMirrorTurns, packageName)) }
    Box(Modifier.padding(start = 56.dp, bottom = 8.dp)) {
        PhoneChoices(
            stringResource(R.string.phone_mirror_turn),
            listOf(
                MirroredKeys.TurnMode.UP_DOWN.id to stringResource(R.string.phone_mirror_turn_up_down),
                MirroredKeys.TurnMode.LEFT_RIGHT.id to stringResource(R.string.phone_mirror_turn_left_right),
            ),
            mode.id,
        ) { id ->
            MirroredKeys.TurnMode.byId(id)?.let { m -> mode = m; settings.myplayMirrorTurns = withMirrorTurn(settings.myplayMirrorTurns, packageName, m) }
        }
    }
}
