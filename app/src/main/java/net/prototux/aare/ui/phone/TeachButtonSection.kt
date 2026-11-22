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

/**
 * O11: teach MyPlay a car button (the four round the MMI knob): choose what it should do, then press
 * it in the car; the car binds whatever code it sends (myplay_learn_button, cleared by the car once
 * learned). Below, the buttons taught so far with their codes, each removable.
 */
@Composable
fun TeachButtonSection(settings: Settings) {
    val ctx = LocalContext.current
    var chosen by remember { mutableStateOf(ButtonAction.OPEN_NAVIGATION) }
    var armed by remember { mutableStateOf(settings.myplayLearnButton) }
    var learned by remember { mutableStateOf(settings.myplayLearnedButtons) }
    // The car clears the armed action and adds the key while this page is open.
    // O11-1: an arm lapses after LearnedButtons.ARM_MS; read it again each second so the page clears by itself.
    var secondsLeft by remember { mutableStateOf(0L) }
    LaunchedEffect(armed) {
        while (armed != null) {
            armed = settings.myplayLearnButton
            secondsLeft = ((LearnedButtons.ARM_MS - (System.currentTimeMillis() - settings.myplayLearnButtonAtMs)) / 1000).coerceAtLeast(0)
            delay(1_000)
        }
    }
    DisposableEffect(settings) {
        val prefs = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == "myplay_learn_button" || key == "myplay_learned_buttons") {
                armed = settings.myplayLearnButton; learned = settings.myplayLearnedButtons
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    SectionHeading(stringResource(R.string.phone_teach_title))
    PartCard(padding = 16.dp) {
        Column {
            val waiting = armed
            if (waiting == null) {
                PhoneChoices(
                    stringResource(R.string.phone_teach_choose),
                    TEACHABLE_ACTIONS.map { it.id to stringResource(actionName(it)) },
                    chosen.id,
                ) { id -> ButtonAction.byId(id)?.let { chosen = it } }
                Spacer(Modifier.height(12.dp))
                PillButton(stringResource(R.string.phone_teach_start)) { settings.myplayLearnButton = chosen; armed = chosen }
            } else {
                Text(stringResource(R.string.phone_teach_waiting, stringResource(actionName(waiting))), color = PhoneColors.Text, fontSize = 16.sp)
                Text(stringResource(R.string.phone_teach_seconds, secondsLeft), color = PhoneColors.Muted, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                PillButton(stringResource(R.string.common_cancel), main = false) { settings.myplayLearnButton = null; armed = null }
            }
        }
    }
    HintLine(Icons.Rounded.Info, stringResource(R.string.phone_teach_hint))
    val rows = learnedRows(learned)
    if (rows.isNotEmpty()) {
        SectionHeading(stringResource(R.string.phone_teach_learned))
        RowGroup(rows = rows.map { (code, action) ->
            {
                Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.SmartButton, null, tint = PhoneColors.Muted)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(actionName(action)), color = PhoneColors.Text, fontSize = 16.sp)
                        Text(CarInputLog.keyName(code), color = PhoneColors.Muted, fontSize = 13.sp)
                    }
                    IconButton(onClick = {
                        learned = LearnedButtons.learn(learned, code, ButtonAction.NOTHING); settings.myplayLearnedButtons = learned
                    }) { Icon(Icons.Rounded.Delete, stringResource(R.string.phone_teach_forget, CarInputLog.keyName(code)), tint = PhoneColors.Muted) }
                }
            }
        })
    }
}
