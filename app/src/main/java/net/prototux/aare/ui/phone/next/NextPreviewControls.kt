package net.prototux.aare.ui.phone.next

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RotateLeft
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.prototux.aare.R

/**
 * What the Preview's controls drive (dispatcher, 2026-11-23): the car's own input path (learned
 * buttons, remaps, the button table, the gesture engine, mirrored keys), run on the Preview's
 * CarRuntime. The car side provides it; the controls only send what the car would send.
 */
interface PreviewInputs {
    /** A raw Android key code, as CarKey.code carries it from the car: down, then up. */
    fun key(code: Int, down: Boolean)
    /** The knob turned: + clockwise. */
    fun rotary(steps: Int)
    /** A pretend car speed for the speed and limit visuals, Preview only; null: off. */
    fun pretendSpeed(kmh: Float?)
}

/** One button of the bar and the code it sends (the car's defaults, ButtonTable.DEFAULT_CODES). */
data class PreviewKey(val label: Int, val icon: ImageVector, val code: Int)

val PREVIEW_KEYS: List<PreviewKey> = listOf(
    PreviewKey(R.string.next_prev_push_label, Icons.Rounded.RadioButtonChecked, KeyEvent.KEYCODE_DPAD_CENTER),
    PreviewKey(R.string.next_car_pad_up_label, Icons.Rounded.KeyboardArrowUp, KeyEvent.KEYCODE_DPAD_UP),
    PreviewKey(R.string.next_car_pad_down_label, Icons.Rounded.KeyboardArrowDown, KeyEvent.KEYCODE_DPAD_DOWN),
    PreviewKey(R.string.next_car_pad_left_label, Icons.Rounded.KeyboardArrowLeft, KeyEvent.KEYCODE_DPAD_LEFT),
    PreviewKey(R.string.next_car_pad_right_label, Icons.Rounded.KeyboardArrowRight, KeyEvent.KEYCODE_DPAD_RIGHT),
    PreviewKey(R.string.next_prev_back_label, Icons.AutoMirrored.Rounded.ArrowBack, KeyEvent.KEYCODE_BACK),
    PreviewKey(R.string.next_prev_menu_label, Icons.Rounded.Menu, KeyEvent.KEYCODE_MENU),
)

/** A key that sends down when the finger lands and up when it lifts, so holds and quick double/triple pushes are real. */
@Composable
private fun KeyButton(label: String, icon: ImageVector?, text: String? = null, onDown: () -> Unit, onUp: () -> Unit) {
    val down by rememberUpdatedState(onDown)
    val up by rememberUpdatedState(onUp)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(56.dp)) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(NextColors.Surface2).border(1.5.dp, NextColors.Outline, CircleShape)
                .semantics { contentDescription = label; role = Role.Button }
                .testTag("preview-key-$label")
                .pointerInput(Unit) { detectTapGestures(onPress = { down(); tryAwaitRelease(); up() }) },
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) Icon(icon, null, tint = NextColors.Text, modifier = Modifier.size(22.dp))
            else Text(text.orEmpty(), style = NextType.Tag, color = NextColors.Text)
        }
        Text(label, style = NextType.Tag, color = NextColors.Text3, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
    }
}

/**
 * The Preview's control bar (dispatcher request): the knob (turns and push), the pad, Back and Menu,
 * the taught corner buttons ([learnedCodes]), and a pretend speed. Marked "Testing aid": it sends
 * what the car would, and says what it cannot pretend.
 */
@Composable
fun NextPreviewControls(
    inputs: PreviewInputs, learnedCodes: List<Int>, modifier: Modifier = Modifier,
    /** This car's code for a default one (the Buttons page's choice for the knob push, Back, Menu). */
    codeFor: (Int) -> Int = { it },
) {
    var speed by rememberSaveable { mutableFloatStateOf(0f) }
    Column(modifier.fillMaxWidth().background(NextColors.TabBar).padding(horizontal = 12.dp, vertical = 8.dp).testTag("preview-controls")) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NextTag(stringResource(R.string.next_prev_testing_label), TagKind.CAUTION, Icons.Rounded.Science)
            KeyButton(stringResource(R.string.next_prev_turn_left_label), Icons.Rounded.RotateLeft, onDown = { inputs.rotary(-1) }, onUp = {})
            KeyButton(stringResource(R.string.next_prev_turn_right_label), Icons.Rounded.RotateRight, onDown = { inputs.rotary(1) }, onUp = {})
            PREVIEW_KEYS.forEach { k ->
                val code = codeFor(k.code)
                KeyButton(stringResource(k.label), k.icon, onDown = { inputs.key(code, true) }, onUp = { inputs.key(code, false) })
            }
            learnedCodes.take(2).forEachIndexed { i, code ->
                KeyButton(stringResource(R.string.next_prev_corner_label, i + 1), null, "${i + 1}",
                    onDown = { inputs.key(code, true) }, onUp = { inputs.key(code, false) })
            }
        }
        NextSlider(
            stringResource(R.string.next_prev_pretend_label),
            if (speed < 1f) stringResource(R.string.next_prev_speed_off_label) else stringResource(R.string.next_prev_speed_label, speed.toInt()),
            speed, 0f..200f, { speed = it; inputs.pretendSpeed(if (it < 1f) null else it) },
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.next_prev_cannot_help), style = NextType.Help, color = NextColors.Text3, modifier = Modifier.weight(1f, fill = false))
            NextInfoButton(stringResource(R.string.next_prev_cannot_help), stringResource(R.string.next_prev_cannot_info))
        }
    }
}
