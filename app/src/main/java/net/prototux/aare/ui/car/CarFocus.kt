package net.prototux.aare.ui.car

import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp

/**
 * A clear focus highlight for non-touch head units driven by a rotary controller or D-pad: a thick
 * border on the element's shape while it holds focus, so the driver can see where they are.
 *
 * Place it directly before the element's own `.clickable`, whose focus target it observes (a
 * clickable is focusable by default, so Compose's D-pad traversal already reaches it; this only
 * makes the focus visible). It draws nothing until something is focused, so touch head units,
 * which never move focus, are unaffected; and nothing after a touch, see [CarFocusVisibility].
 *
 * MyPlay: an outer border in the accent colour alone cannot be seen on a button that is itself
 * filled with the accent colour (the play button). So there is also a light veil under the
 * content and a second ring just inside the border, in the colour that contrasts with the accent.
 * The veil and the inner ring are taken from JVKeller's fork of AARE (commit a08c2073).
 */
fun Modifier.carFocusHighlight(shape: Shape): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val color = MaterialTheme.colorScheme.primary
    val inner = MaterialTheme.colorScheme.onPrimary
    val shown = focused && CarFocusVisibility.keyInput
    onFocusChanged { focused = it.isFocused }
        .drawWithContent {
            if (shown) drawOutline(shape.createOutline(size, layoutDirection, this), inner.copy(alpha = FOCUS_VEIL_ALPHA))
            drawContent()
            if (!shown) return@drawWithContent
            val w = FOCUS_INNER_RING.toPx()
            val at = FOCUS_OUTER_RING.toPx() + w / 2
            // Too small for two rings (a tiny badge): the outer border alone.
            if (size.minDimension > at * 4) {
                val outline = shape.createOutline(Size(size.width - at * 2, size.height - at * 2), layoutDirection, this)
                translate(at, at) { drawOutline(outline, inner, style = Stroke(w)) }
            }
        }
        .border(width = if (shown) FOCUS_OUTER_RING else 0.dp, color = if (shown) color else Color.Transparent, shape = shape)
}

private val FOCUS_OUTER_RING = 3.dp
private val FOCUS_INNER_RING = 4.dp
private const val FOCUS_VEIL_ALPHA = 0.3f

/**
 * Whether the car's last input was a navigation key rather than a touch (main thread). Car touches
 * are injected straight into the window, so they never put it back in touch mode once a knob took
 * it out: on a unit with both, the highlight would otherwise stay up and follow the taps.
 */
object CarFocusVisibility {
    var keyInput by mutableStateOf(false)
}
