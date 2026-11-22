package net.prototux.aare.ui.phone

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HighlightOff
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import net.prototux.aare.R

private val CheckStep.title: Int
    get() = when (this) {
        CheckStep.KNOB_RIGHT -> R.string.phone_check_knob_right
        CheckStep.KNOB_LEFT -> R.string.phone_check_knob_left
        CheckStep.KNOB_PUSH -> R.string.phone_check_knob_push
        CheckStep.KNOB_HOLD -> R.string.phone_check_knob_hold
        CheckStep.KNOB_BUTTONS -> R.string.phone_check_knob_buttons
        CheckStep.PAD_UP -> R.string.phone_check_pad_up
        CheckStep.PAD_DOWN -> R.string.phone_check_pad_down
        CheckStep.PAD_LEFT -> R.string.phone_check_pad_left
        CheckStep.PAD_RIGHT -> R.string.phone_check_pad_right
        CheckStep.MENU -> R.string.phone_check_menu
        CheckStep.BACK -> R.string.phone_check_back
        CheckStep.BACK_HOLD -> R.string.phone_check_back_hold
        CheckStep.WHEEL_SCROLL -> R.string.phone_check_wheel_scroll
        CheckStep.WHEEL_CLICK -> R.string.phone_check_wheel_click
        CheckStep.WHEEL_HOLD -> R.string.phone_check_wheel_hold
    }

/**
 * The Car check (for R8): walks the owner through each of the car's controls, one at a time, and
 * records whatever the car sends while that step is on screen (the codes are what is not known, so
 * nothing is matched by code). Then one button shares the findings, what the car reported about
 * itself and the connection log, without private details ([CarCheckLog]).
 *
 * [inputs]: the car's input as it arrives (any thread); [report]: what the car said about itself
 * this session; [log]: the connection log's lines now; [connected]: whether a car is connected.
 */
@Composable
fun CarCheckPage(inputs: Flow<CheckInput>, report: List<String>, log: () -> List<String>, connected: Boolean, now: () -> Long) {
    val ctx = LocalContext.current
    val settings = remember { net.prototux.aare.Settings(ctx) }
    var current by rememberSaveable { mutableIntStateOf(0) }
    val captures = remember { mutableStateMapOf<CheckStep, StepCapture>() }
    val step = CheckStep.entries.getOrNull(current)
    // Everything that arrives goes to the step on screen; nothing is recorded once all are done.
    LaunchedEffect(inputs) {
        inputs.flowOn(Dispatchers.Default).collect { input ->
            val s = CheckStep.entries.getOrNull(current) ?: return@collect
            captures[s] = (captures[s] ?: StepCapture()).with(input)
        }
    }
    // A key still down shows its hold growing.
    var clock by remember { mutableLongStateOf(now()) }
    LaunchedEffect(Unit) { while (true) { clock = now(); delay(250) } }
    val titles = CheckStep.entries.associateWith { stringResource(it.title) }

    Column {
        if (!connected) HintLine(Icons.Rounded.LinkOff, stringResource(R.string.phone_check_not_connected))
        HintLine(Icons.Rounded.Info, stringResource(R.string.phone_check_how))
        if (step != null) {
            SectionHeading(stringResource(R.string.phone_check_step_of, current + 1, CheckStep.entries.size))
            PartCard {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.TouchApp, null, tint = PhoneColors.Accent, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(titles.getValue(step), color = PhoneColors.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    if (step.hold) Text(stringResource(R.string.phone_check_hold_hint), color = PhoneColors.Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
                    if (step == CheckStep.KNOB_BUTTONS) Text(stringResource(R.string.phone_check_knob_buttons_seen, distinctKeys(captures[step])),
                        color = PhoneColors.Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
                    Spacer(Modifier.height(12.dp))
                    val lines = captures[step]?.lines(clock).orEmpty()
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(PhoneColors.Page).padding(12.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite }) {
                        if (lines.isEmpty()) Text(stringResource(R.string.phone_check_waiting), color = PhoneColors.Muted, fontSize = 14.sp)
                        lines.forEach { Text(it, color = PhoneColors.Text, fontSize = 13.sp, fontFamily = FontFamily.Monospace) }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val got = captures[step]?.isEmpty == false
                        PillButton(stringResource(if (got) R.string.phone_check_next else R.string.phone_check_nothing), main = got) {
                            if (captures[step] == null) captures[step] = StepCapture()
                            current++
                            // #1: what the hold steps saw, for the Buttons page's note.
                            carSendsHolds(captures.toMap())?.let { settings.myplayCarSendsHolds = it }
                        }
                        if (got) PillButton(stringResource(R.string.phone_check_again), main = false) { captures[step] = StepCapture() }
                    }
                }
            }
        } else {
            SectionHeading(stringResource(R.string.phone_check_done))
        }
        SectionHeading(stringResource(R.string.phone_check_steps))
        RowGroup(rows = CheckStep.entries.map { s ->
            {
                val c = captures[s]
                Row(
                    Modifier.fillMaxWidth().clickable { current = s.ordinal }.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val (icon, tint) = when {
                        c == null -> Icons.Rounded.RadioButtonUnchecked to PhoneColors.Muted
                        c.isEmpty -> Icons.Rounded.HighlightOff to PhoneColors.Muted
                        else -> Icons.Rounded.CheckCircle to PhoneColors.Accent
                    }
                    Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(titles.getValue(s), color = PhoneColors.Text, fontSize = 15.sp, fontWeight = if (s == step) FontWeight.Bold else FontWeight.Normal)
                        val detail = when {
                            c == null -> null
                            c.isEmpty -> stringResource(R.string.phone_check_nothing_arrived)
                            else -> c.lines(clock).first()
                        }
                        detail?.let { Text(it, color = PhoneColors.Muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace, maxLines = 2) }
                    }
                }
            }
        })
        Spacer(Modifier.height(14.dp))
        WideButton(stringResource(R.string.phone_check_share), Icons.Rounded.Share) {
            val text = CarCheckLog.shareText(checkSummary(captures.toMap(), { titles.getValue(it) }, now()), report, log())
            val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, "MyPlay car check").putExtra(Intent.EXTRA_TEXT, text)
            ctx.startActivity(Intent.createChooser(send, null))
        }
        HintLine(Icons.Rounded.Info, stringResource(R.string.phone_check_private))
    }
}
