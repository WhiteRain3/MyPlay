package net.prototux.aare.ui.phone.next

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** How a hero ring is drawn (design frame 77, "Rings"). */
enum class RingStyle { FULL, SWEEP, DASHED, CAUTION, ERROR, PROGRESS }

/**
 * The hero ring of Home and Updates: full when done, sweeping while waiting, dashed when not
 * connected, a share of the circle for [progress] (0–1), with an icon or a [label] inside.
 */
@Composable
fun NextHeroRing(style: RingStyle, color: Color, icon: ImageVector?, label: String? = null, progress: Float = 0f, size: Dp = 184.dp) {
    val sweep = rememberInfiniteTransition(label = "sweep")
    val turn by sweep.animateFloat(0f, 360f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "turn")
    Box(Modifier.padding(top = 8.dp).size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = 10.dp.toPx()
            val inset = w / 2 + 8.dp.toPx()
            val box = Size(this.size.width - 2 * inset, this.size.height - 2 * inset)
            val tl = Offset(inset, inset)
            when (style) {
                RingStyle.FULL -> {
                    drawCircle(color.copy(alpha = 0.10f), radius = this.size.minDimension / 2)
                    drawArc(color, 0f, 360f, false, tl, box, style = Stroke(w))
                }
                RingStyle.SWEEP -> {
                    drawArc(NextColors.Track, 0f, 360f, false, tl, box, style = Stroke(w))
                    rotate(turn) { drawArc(color, -90f, 100f, false, tl, box, style = Stroke(w, cap = StrokeCap.Round)) }
                }
                RingStyle.DASHED, RingStyle.CAUTION -> drawArc(
                    color, 0f, 360f, false, tl, box,
                    style = Stroke(if (style == RingStyle.CAUTION) w * 0.6f else 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f))),
                )
                RingStyle.ERROR -> {
                    drawArc(NextColors.Track, 0f, 360f, false, tl, box, style = Stroke(w))
                    drawArc(color, -90f, 110f, false, tl, box, style = Stroke(w, cap = StrokeCap.Round))
                }
                RingStyle.PROGRESS -> {
                    drawArc(NextColors.Track, 0f, 360f, false, tl, box, style = Stroke(w))
                    drawArc(color, -90f, 360f * progress.coerceIn(0f, 1f), false, tl, box, style = Stroke(w, cap = StrokeCap.Round))
                }
            }
        }
        if (label != null) Text(label, style = NextType.Hero.copy(fontSize = NextType.RootTitle.fontSize), color = NextColors.Text)
        else if (icon != null) Icon(icon, null, tint = color, modifier = Modifier.size(56.dp))
    }
}
