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
    HomePadKey.SIDE to R.string.phone_home_pad_side,
    HomePadKey.DOWN to R.string.phone_home_pad_down,
)

/** The words for each action, in the order offered. */
val HOME_PAD_ACTION_NAMES: List<Pair<HomePadAction, Int>> = listOf(
    HomePadAction.MUSIC to R.string.phone_home_pad_music,
    HomePadAction.MAPS to R.string.phone_home_pad_maps,
    HomePadAction.DIAL to R.string.phone_home_pad_dial,
    HomePadAction.APPS to R.string.phone_home_pad_apps,
    HomePadAction.CALLS to R.string.phone_home_pad_calls,
    HomePadAction.NOTHING to R.string.phone_home_pad_nothing,
)

/** [mapping] with [key] set to [action]. */
fun withHomePad(mapping: Map<HomePadKey, HomePadAction>, key: HomePadKey, action: HomePadAction) = mapping + (key to action)

/**
 * O6: what the pad's keys open on ORBIT's home while the knob's ring is hidden
 * (myplay_home_pad, the car side's setting, applied live).
 */
@Composable
fun HomePadSection(mapping: Map<HomePadKey, HomePadAction>, onChange: (Map<HomePadKey, HomePadAction>) -> Unit) {
    SectionHeading(stringResource(R.string.phone_home_pad_title))
    PartCard(padding = 16.dp) {
        Column {
            HOME_PAD_KEY_NAMES.forEachIndexed { i, (key, name) ->
                if (i > 0) Spacer(Modifier.height(14.dp))
                PhoneChoices(
                    stringResource(name),
                    HOME_PAD_ACTION_NAMES.map { (a, res) -> a.id to stringResource(res) },
                    (mapping[key] ?: HomePadAction.NOTHING).id,
                ) { id -> HomePadAction.byId(id)?.let { onChange(withHomePad(mapping, key, it)) } }
            }
        }
    }
    HintLine(Icons.Rounded.Info, stringResource(R.string.phone_home_pad_hint))
}
