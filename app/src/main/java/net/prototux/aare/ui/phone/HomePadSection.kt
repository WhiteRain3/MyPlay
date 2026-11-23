package net.prototux.aare.ui.phone

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.prototux.aare.R
import net.prototux.aare.ui.car.myplay.HomePadAction
import net.prototux.aare.ui.car.myplay.HomePadKey

/** The words for each pad key, in the order shown. */
val HOME_PAD_KEY_NAMES: List<Pair<HomePadKey, Int>> = listOf(
    HomePadKey.CENTER to R.string.phone_home_pad_center,
    HomePadKey.UP to R.string.phone_home_pad_up,
    HomePadKey.LEFT to R.string.phone_home_pad_left,
    HomePadKey.RIGHT to R.string.phone_home_pad_right,
    HomePadKey.DOWN to R.string.phone_home_pad_down,
)

/** The words for each action, in the order offered. */
val HOME_PAD_ACTION_NAMES: List<Pair<HomePadAction, Int>> = listOf(
    HomePadAction.MUSIC to R.string.phone_home_pad_music,
    HomePadAction.MAPS to R.string.phone_home_pad_maps,
    HomePadAction.DIAL to R.string.phone_home_pad_dial,
    HomePadAction.APPS to R.string.phone_home_pad_apps,
    HomePadAction.CALLS to R.string.phone_home_pad_calls,
    HomePadAction.CALL_BACK to R.string.phone_action_call_back,
    HomePadAction.PLACES to R.string.phone_home_pad_places,
    HomePadAction.NOTHING to R.string.phone_home_pad_nothing,
)

/** [mapping] with [key] set to [action]. */
fun withHomePad(mapping: Map<HomePadKey, HomePadAction>, key: HomePadKey, action: HomePadAction) = mapping + (key to action)

