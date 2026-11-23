package net.prototux.aare.ui.car.myplay.orbit

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt
import net.prototux.aare.R

/**
 * Look C, step 3: ORBIT's one wheel for every list (the owner's chosen design, claude/waze-design
 * "The orbit wheel"). Rows sit on an arc round a centre off the left of the screen, the way the
 * dial turns. The ringed row is always in the middle and biggest, one row either side, smaller
 * and faded; a knob click turns the wheel one row, so the focus never travels down the screen. A
 * thin arc left of the rows carries a dot at the ringed row's place in the whole list, and "N more"
 * above and below say how far it goes. Sizes are the sheet's, at 1280x720 (inside ORBIT's Scaled).
 */
internal object OrbitWheelSpec {
    /** The middle row's centre on the sheet (inside the free area, x 288-992, y 43-677). */
    val MIDDLE_X = 300.dp
    val MIDDLE_Y = 380.dp
    /** One row's step along the wheel, and the arc's radius (its centre off the left). */
    val STEP = 150.dp
    val RADIUS = 340.dp
    val ROW_WIDTH = 660.dp
    /** How far each row either side of the middle shrinks and fades. */
    const val SHRINK = 0.15f
    const val FADE = 0.45f
    const val TAG = "orbit_wheel"

    /** How far left of the middle row a row [dy] above or below it sits, on the arc (its centre is off the left). */
    fun arcX(dy: Dp): Dp {
        val r = RADIUS.value
        val y = dy.value.coerceIn(-r, r)
        return (r - sqrt(r * r - y * y)).dp
    }

    /** How many rows are out of sight above and below the ringed one (its neighbours show). */
    fun hidden(ringed: Int, count: Int): Pair<Int, Int> = (ringed - 1).coerceAtLeast(0) to (count - ringed - 2).coerceAtLeast(0)

    /** The rows composed for a wheel turned to [at] (fractional while it turns): one either side. */
    fun window(at: Float, count: Int): IntRange =
        if (count <= 0) IntRange.EMPTY else (floor(at).toInt() - 1).coerceAtLeast(0)..(ceil(at).toInt() + 1).coerceAtMost(count - 1)
}

/**
 * The wheel: [count] rows, [ringed] in the middle. [row] draws row i (its modifier is the wheel's
 * place for it, to be applied first); [on] is whether it is the ringed one.
 */
@Composable
internal fun OrbitWheel(
    count: Int, ringed: Int, accent: Color, night: Boolean, modifier: Modifier = Modifier,
    row: @Composable (i: Int, on: Boolean, place: Modifier) -> Unit,
) {
    val g = Orbit.glass(night)
    val target = ringed.coerceIn(0, (count - 1).coerceAtLeast(0)).toFloat()
    val at by animateFloatAsState(target, tween(180), label = "wheel")
    Box(modifier.fillMaxSize().testTag(OrbitWheelSpec.TAG)) {
        // Where you are: a thin arc left of the rows, a dot at the ringed row's place in the list.
        if (count > 1) Canvas(Modifier.fillMaxSize()) {
            val r = OrbitWheelSpec.RADIUS.toPx()
            val cx = OrbitWheelSpec.MIDDLE_X.toPx() - 40.dp.toPx() - r
            val cy = OrbitWheelSpec.MIDDLE_Y.toPx()
            val half = Math.toDegrees(asin((1.6f * OrbitWheelSpec.STEP.toPx() / r).toDouble())).toFloat()
            drawArc(g.track, -half, 2 * half, useCenter = false, topLeft = Offset(cx - r, cy - r),
                size = androidx.compose.ui.geometry.Size(2 * r, 2 * r), style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
            val f = at / (count - 1)
            val a = Math.toRadians((-half + 2 * half * f).toDouble())
            drawCircle(accent, 9.dp.toPx(), Offset(cx + r * kotlin.math.cos(a).toFloat(), cy + r * kotlin.math.sin(a).toFloat()))
        }
        // Rows out of sight: the neighbour either side is on screen.
        val (above, below) = OrbitWheelSpec.hidden(ringed, count)
        val more = TextStyle(fontSize = 22.sp, color = g.secondary)
        if (above > 0) Text(stringResource(R.string.myplay_wheel_more, above), style = more,
            modifier = Modifier.align(Alignment.TopStart).offset(OrbitWheelSpec.MIDDLE_X + 40.dp, OrbitWheelSpec.MIDDLE_Y - OrbitWheelSpec.STEP * 1.75f))
        if (below > 0) Text(stringResource(R.string.myplay_wheel_more, below), style = more,
            modifier = Modifier.align(Alignment.TopStart).offset(OrbitWheelSpec.MIDDLE_X + 40.dp, OrbitWheelSpec.MIDDLE_Y + OrbitWheelSpec.STEP * 1.5f))
        for (i in OrbitWheelSpec.window(at, count)) {
            val d = i - at
            val dy = OrbitWheelSpec.STEP * d
            val far = abs(d).coerceAtMost(1.5f)
            val place = Modifier.align(Alignment.TopStart)
                .offset(OrbitWheelSpec.MIDDLE_X - OrbitWheelSpec.arcX(dy), OrbitWheelSpec.MIDDLE_Y + dy - 52.dp)
                .requiredWidth(OrbitWheelSpec.ROW_WIDTH)
                .scale(1f - OrbitWheelSpec.SHRINK * far)
                .alpha((1f - OrbitWheelSpec.FADE * far).coerceAtLeast(0f))
            row(i, i == ringed, place)
        }
    }
}
