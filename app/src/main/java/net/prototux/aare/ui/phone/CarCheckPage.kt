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
import androidx.compose.runtime.mutableStateOf
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

internal val CheckStep.title: Int
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

