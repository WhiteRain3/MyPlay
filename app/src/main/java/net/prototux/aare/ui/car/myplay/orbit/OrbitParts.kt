package net.prototux.aare.ui.car.myplay.orbit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * MyPlay (plan phase N, N4): ORBIT's parts, drawn from [Orbit]'s tokens. Only circles, rings and
 * arcs; a ring shows progress (the trip, the song) instead of a bar.
 */

/** A round disc of glass (or [fill]) with a thin edge; content in the middle. */
@Composable
fun OrbitDisc(
    size: Dp,
    night: Boolean,
    modifier: Modifier = Modifier,
    fill: Color? = null,
    edge: Color? = null,
    edgeWidth: Dp = 2.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val g = Orbit.glass(night)
    Box(
        // AR-1: always a true circle: a row short of room cannot squeeze it into an oval.
        modifier.requiredSize(size).clip(CircleShape).background(fill ?: g.glass).border(edgeWidth, edge ?: g.edge, CircleShape),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/**
 * A progress ring: a [track] circle and an arc of [colour] from the top, clockwise, over
 * [progress] (0..1, clamped). Null progress draws the track only. Round line ends, as the sheet.
 */
@Composable
fun OrbitRing(
    progress: Float?,
    size: Dp,
    stroke: Dp,
    colour: Color,
    track: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    // AR-1 (the owner: the art sometimes an oval): always its full square size, whatever room the
    // row beside it leaves; the text gives way instead.
    Box(modifier.requiredSize(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.requiredSize(size)) {
            val w = stroke.toPx()
            val inset = w / 2
            val arcSize = Size(this.size.width - w, this.size.height - w)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w))
            val p = progress?.coerceIn(0f, 1f) ?: return@Canvas
            if (p > 0f) drawArc(colour, -90f, 360f * p, false, Offset(inset, inset), arcSize, style = Stroke(w, cap = StrokeCap.Round))
        }
        content()
    }
}

/**
 * ORBIT's focus ring around a round part: an accent circle [Orbit.FOCUS_WIDTH] wide,
 * [Orbit.FOCUS_GAP] outside the part, in a dark halo so it reads over any map. Drawn behind the
 * part's own drawing area and outside it, so it never moves the layout.
 */
fun Modifier.orbitFocus(on: Boolean, accent: Color, halo: Color): Modifier = if (!on) this else drawBehind {
    val r = minOf(size.width, size.height) / 2
    val c = Offset(size.width / 2, size.height / 2)
    val gap = Orbit.FOCUS_GAP.toPx()
    val w = Orbit.FOCUS_WIDTH.toPx()
    drawCircle(halo.copy(alpha = 0.6f), r + gap + w + Orbit.FOCUS_HALO.toPx() / 2, c, style = Stroke(Orbit.FOCUS_HALO.toPx()))
    drawCircle(halo, r + gap / 2, c, style = Stroke(gap))
    drawCircle(accent, r + gap + w / 2, c, style = Stroke(w))
}

/** ORBIT's focus ring around a rounded square of corner [corner] (the artwork box, "the one square"). */
fun Modifier.orbitFocusRounded(on: Boolean, accent: Color, halo: Color, corner: Dp): Modifier = if (!on) this else drawBehind {
    val gap = Orbit.FOCUS_GAP.toPx()
    val w = Orbit.FOCUS_WIDTH.toPx()
    val r = corner.toPx()
    fun ring(grow: Float, colour: Color, width: Float) = drawRoundRect(
        colour, Offset(-grow, -grow), Size(size.width + 2 * grow, size.height + 2 * grow),
        androidx.compose.ui.geometry.CornerRadius(r + grow), style = Stroke(width),
    )
    ring(gap + w + Orbit.FOCUS_HALO.toPx() / 2, halo.copy(alpha = 0.6f), Orbit.FOCUS_HALO.toPx())
    ring(gap / 2, halo, gap)
    ring(gap + w / 2, accent, w)
}

/** The ring-shaped sector of the dial between [startDeg] and [endDeg] (0 = right, clockwise), radii in px. */
internal fun sectorPath(center: Offset, inner: Float, outer: Float, startDeg: Float, endDeg: Float, steps: Int = 24): Path = Path().apply {
    fun at(r: Float, deg: Float) = Math.toRadians(deg.toDouble()).let { Offset(center.x + r * cos(it).toFloat(), center.y + r * sin(it).toFloat()) }
    val first = at(outer, startDeg)
    moveTo(first.x, first.y)
    for (i in 1..steps) at(outer, startDeg + (endDeg - startDeg) * i / steps).let { lineTo(it.x, it.y) }
    for (i in steps downTo 0) at(inner, startDeg + (endDeg - startDeg) * i / steps).let { lineTo(it.x, it.y) }
    close()
}

/**
 * One sector of the dial: glass, or the world's accent at 30 % with an accent edge when [lit]
 * (the sheet's dial: six sectors of 57 degrees with 3 degree gaps).
 */
@Composable
fun OrbitSector(index: Int, count: Int, lit: Boolean, accent: Color, night: Boolean, size: Dp, inner: Dp, modifier: Modifier = Modifier) {
    val g = Orbit.glass(night)
    // SM-5: the sector's path (50 points of trigonometry) and its stroke are made once per size and
    // look (drawWithCache), not again on every draw of the dial while it ripples open or the ring moves.
    androidx.compose.foundation.layout.Spacer(modifier.size(size).drawWithCache {
        val c = Offset(this.size.width / 2, this.size.height / 2)
        val span = 360f / count
        val mid = -90f + span * index
        val path = sectorPath(c, inner.toPx(), this.size.minDimension / 2 - 3.dp.toPx(), mid - span / 2 + 1.5f, mid + span / 2 - 1.5f)
        val fill = if (lit) accent.copy(alpha = 0.30f) else g.glass
        val edge = if (lit) accent else g.edge
        val stroke = Stroke((if (lit) 5 else 2).dp.toPx())
        onDrawBehind {
            drawPath(path, fill)
            drawPath(path, edge, style = stroke)
        }
    })
}

/** ORBIT's motion: everything opens out of the hub like a ripple; the dial turns in detents. */
object OrbitMotion {
    /** A ripple opening (or closing back into the hub). */
    const val RIPPLE_MS = 240
    /** One detent of the dial. */
    val detent = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
}

/**
 * Grows the content out of [from] (a point in its own fractions, e.g. the hub's corner) when it
 * appears, as a ripple: scale and fade in over [OrbitMotion.RIPPLE_MS].
 */
@Composable
fun Modifier.orbitRipple(fromX: Float, fromY: Float): Modifier {
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(OrbitMotion.RIPPLE_MS, easing = FastOutSlowInEasing)) }
    return graphicsLayer {
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(fromX, fromY)
        val v = grow.value
        scaleX = 0.6f + 0.4f * v
        scaleY = 0.6f + 0.4f * v
        alpha = v
    }
}

/**
 * The dial's turn to [index] of [count], in degrees, sprung so it settles into each detent.
 * Counts whole turns, so going from the last world to the first keeps turning the same way.
 */
@Composable
fun orbitDetentAngle(turns: Int, count: Int): Float {
    val angle by animateFloatAsState(-360f / count * turns, OrbitMotion.detent, label = "orbit dial")
    return angle
}
