package net.prototux.aare.ui.car.myplay.orbit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhoneCallback
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.prototux.aare.car.ButtonAction
import net.prototux.aare.ui.car.myplay.HomePadAction
import net.prototux.aare.ui.car.myplay.PadHints
import net.prototux.aare.ui.car.myplay.World

/**
 * HN-2 (the owner's hints A, E and F): small icon discs on a thin ring round the hub, one per pad
 * direction that opens something (up, down, and left and right alike for the side), in the colour
 * of what it opens; and the buttons taught in the car on the left diagonals (the first up-left, the
 * second down-left; the up-right one is the limit sign's). Icons only. Everything stays left of the
 * map's free area (MC-1): the right disc ends 20 dp short of it on a 1280x720 screen.
 */
internal object OrbitHints {
    /** The hub's disc (OrbitSkin's Hub and HubSpeed): its size, and the box it is in when the speed is shown. */
    val HUB = 192.dp
    val HUB_WITH_SPEED = 212.dp
    /** The ring's radius from the disc's middle, and the hint discs' size. */
    val RING = 118.dp
    val DISC = 40.dp

    const val TAG_UP = "orbit_hint_up"
    const val TAG_DOWN = "orbit_hint_down"
    const val TAG_LEFT = "orbit_hint_left"
    const val TAG_RIGHT = "orbit_hint_right"
    fun cornerTag(i: Int) = "orbit_hint_corner_$i"

    /** Each hint's angle on the ring, in degrees (0 right, 90 down), for the diagonals of the taught buttons. */
    val CORNER_DEGREES = listOf(225f, 135f)

    fun icon(a: HomePadAction): ImageVector = when (a) {
        HomePadAction.MUSIC -> Icons.Rounded.MusicNote
        HomePadAction.MAPS -> Icons.Rounded.Navigation
        HomePadAction.DIAL -> Icons.Rounded.DonutLarge
        HomePadAction.APPS -> Icons.Rounded.Apps
        HomePadAction.CALLS -> Icons.Rounded.Phone
        HomePadAction.CALL_BACK -> Icons.Rounded.PhoneCallback
        HomePadAction.NOTHING -> Icons.Rounded.RadioButtonChecked
    }

    fun world(a: HomePadAction): World? = when (a) {
        HomePadAction.MUSIC -> World.MUSIC
        HomePadAction.MAPS -> World.NAVIGATE
        HomePadAction.APPS -> World.APPS
        HomePadAction.CALLS, HomePadAction.CALL_BACK -> World.CALLS
        else -> null
    }

    fun icon(a: ButtonAction): ImageVector = when (a) {
        ButtonAction.NAVIGATE_HOME -> Icons.Rounded.Home
        ButtonAction.OPEN_NAVIGATION -> Icons.Rounded.Navigation
        ButtonAction.OPEN_MEDIA -> Icons.Rounded.MusicNote
        ButtonAction.OPEN_PHONE -> Icons.Rounded.Phone
        ButtonAction.CALL_BACK -> Icons.Rounded.PhoneCallback
        ButtonAction.PLAY_PAUSE -> Icons.Rounded.PlayArrow
        ButtonAction.NEXT -> Icons.Rounded.SkipNext
        ButtonAction.PREVIOUS -> Icons.Rounded.SkipPrevious
        ButtonAction.SKIP_SONGS -> Icons.Rounded.FastForward
        ButtonAction.ASSISTANT -> Icons.Rounded.Mic
        ButtonAction.BACK -> Icons.Rounded.ArrowBack
        ButtonAction.HOME, ButtonAction.HOME_OR_APPS -> Icons.Rounded.Dashboard
        ButtonAction.SELECT, ButtonAction.NOTHING -> Icons.Rounded.RadioButtonChecked
    }

    fun world(a: ButtonAction): World? = when (a) {
        ButtonAction.NAVIGATE_HOME, ButtonAction.OPEN_NAVIGATION -> World.NAVIGATE
        ButtonAction.OPEN_MEDIA, ButtonAction.PLAY_PAUSE, ButtonAction.NEXT, ButtonAction.PREVIOUS, ButtonAction.SKIP_SONGS -> World.MUSIC
        ButtonAction.OPEN_PHONE, ButtonAction.CALL_BACK -> World.CALLS
        else -> null
    }
}

/** The hints round the hub, in ORBIT's scale (inside OrbitSkin's Scaled). [speedShown]: the hub's box holds the limit sign too. */
@Composable
internal fun OrbitPadHints(hints: PadHints, speedShown: Boolean, night: Boolean, modifier: Modifier) {
    val g = Orbit.glass(night)
    val box = if (speedShown) OrbitHints.HUB_WITH_SPEED else OrbitHints.HUB
    val half = OrbitHints.HUB / 2
    // The disc's middle in the box: bottom left aligned.
    val cx = half
    val cy = box - half
    class Hint(val degrees: Float, val icon: ImageVector, val colour: Color, val tag: String)
    fun padColour(a: HomePadAction) = OrbitHints.world(a)?.let { Orbit.accent(it, night) } ?: g.text
    fun keyColour(a: ButtonAction) = OrbitHints.world(a)?.let { Orbit.accent(it, night) } ?: g.text
    val all = buildList {
        hints.up?.let { add(Hint(270f, OrbitHints.icon(it), padColour(it), OrbitHints.TAG_UP)) }
        hints.down?.let { add(Hint(90f, OrbitHints.icon(it), padColour(it), OrbitHints.TAG_DOWN)) }
        hints.side?.let {
            add(Hint(180f, OrbitHints.icon(it), padColour(it), OrbitHints.TAG_LEFT))
            add(Hint(0f, OrbitHints.icon(it), padColour(it), OrbitHints.TAG_RIGHT))
        }
        hints.corners.forEachIndexed { i, a ->
            OrbitHints.CORNER_DEGREES.getOrNull(i)?.let { add(Hint(it, OrbitHints.icon(a), keyColour(a), OrbitHints.cornerTag(i))) }
        }
    }
    Box(modifier.size(box)) {
        // A thin arc of the ring by each hint (the ring is not drawn where nothing is).
        Canvas(Modifier.size(box)) {
            val c = Offset(cx.toPx(), cy.toPx())
            val r = OrbitHints.RING.toPx()
            for (h in all) {
                drawArc(h.colour.copy(alpha = 0.55f), h.degrees - 22f, 44f, useCenter = false,
                    topLeft = Offset(c.x - r, c.y - r), size = Size(2 * r, 2 * r), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            }
        }
        for (h in all) {
            val rad = Math.toRadians(h.degrees.toDouble())
            val dx: Dp = OrbitHints.RING * kotlin.math.cos(rad).toFloat()
            val dy: Dp = OrbitHints.RING * kotlin.math.sin(rad).toFloat()
            Box(
                Modifier.align(Alignment.TopStart).offset(cx + dx - OrbitHints.DISC / 2, cy + dy - OrbitHints.DISC / 2)
                    .size(OrbitHints.DISC).clip(CircleShape).background(g.glass).border(2.dp, h.colour, CircleShape).testTag(h.tag),
                contentAlignment = Alignment.Center,
            ) { Icon(h.icon, null, Modifier.size(22.dp), tint = h.colour) }
        }
    }
}
