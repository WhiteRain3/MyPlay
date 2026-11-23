package net.prototux.aare.ui.phone

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SmartButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import net.prototux.aare.car.myplayLearnButtonAtMs
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.car.ButtonAction
import net.prototux.aare.car.CarInputLog
import net.prototux.aare.car.LearnedButtons
import net.prototux.aare.car.myplayLearnButton
import net.prototux.aare.car.myplayLearnedButtons

/**
 * The actions a taught button can do: everything but pressing the ring (that is the knob's), skipping
 * songs by hold and turn (needs the knob) and nothing (forgetting is the list's delete).
 */
val TEACHABLE_ACTIONS: List<ButtonAction> =
    ButtonAction.entries - setOf(ButtonAction.SELECT, ButtonAction.SKIP_SONGS, ButtonAction.NOTHING)

/** The learned buttons in the order shown: by key code. */
fun learnedRows(learned: Map<Int, ButtonAction>): List<Pair<Int, ButtonAction>> = learned.entries.sortedBy { it.key }.map { it.key to it.value }

