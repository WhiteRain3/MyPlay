package net.prototux.aare.host.templates

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Look C, step 3: an app's lists (Waze's destinations, its settings, any ListTemplate) in the
 * orbit wheel's look, where MyPlay's frame asks for it (a maps app under ORBIT). The app's rows and
 * the knob's focus stay as they are; the focused row is brought to the middle instead of just into
 * view, and every row fades, shrinks and bends left with its distance from the middle, read in the
 * draw phase only (OW-1), so a turn never recomposes the list.
 */
internal val LocalWheelLists = staticCompositionLocalOf { false }

internal object WheelLook {
    /** About one row's height, for the space that lets the first and last rows reach the middle. */
    val ROW = 88.dp
    /** The arc's radius in rows' heights, and how far a row a whole viewport-half away moves left. */
    private const val SHRINK = 0.15f
    private const val FADE = 0.55f
    private val BEND = 60.dp

    /** The focused row goes to the middle of the list, not just into view. */
    @OptIn(ExperimentalFoundationApi::class)
    val Centre = object : BringIntoViewSpec {
        override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float =
            offset - (containerSize - size) / 2f
    }

    /**
     * How far from the middle a row is, -1..1 over half the viewport (0 in the middle), from where
     * the list has laid it out; null when it is not laid out.
     */
    fun distance(state: LazyListState, index: Int): Float? {
        val info = state.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return null
        val half = (info.viewportEndOffset - info.viewportStartOffset) / 2f
        if (half <= 0f) return null
        val middle = info.viewportStartOffset + half
        val centre = item.offset + item.size / 2f
        return ((centre - middle) / half).coerceIn(-1f, 1f)
    }

    /** The bend left on the arc for a row [d] (-1..1) from the middle, as a fraction of [BEND]. */
    fun bend(d: Float): Float {
        val x = abs(d).coerceAtMost(1f)
        return 1f - sqrt(1f - x * x)
    }

    @Composable
    fun Item(state: LazyListState, index: Int, content: @Composable () -> Unit) {
        Box(
            Modifier.graphicsLayer {
                val d = distance(state, index) ?: 0f
                val far = abs(d)
                val s = 1f - SHRINK * far
                scaleX = s
                scaleY = s
                alpha = (1f - FADE * far).coerceAtLeast(0.2f)
                translationX = -BEND.toPx() * bend(d)
            },
        ) { content() }
    }
}
