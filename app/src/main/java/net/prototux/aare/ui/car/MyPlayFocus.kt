package net.prototux.aare.ui.car

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * MyPlay: keeping the knob's ring inside the screen that was just opened.
 *
 * On the test bench, opening Settings or a maps app and turning the knob once put the ring on the
 * bar, not on the screen's first item. The shell asked for focus once, the moment the screen
 * changed: the old screen is still there for a moment (it fades out), a hosted app has nothing to
 * focus until its first page arrives, and when the focused item goes away with its page nothing
 * holds the focus any more. The next turn then starts from the top, which is the bar.
 *
 * Two rules, both without the knob's tilt:
 *  - after a screen opens, ask again until the new screen holds the focus and has kept it for a
 *    moment ([FocusSettle]);
 *  - whenever nothing at all holds the focus while the knob is in use, give it to the content.
 */
object CarFocusState {
    /** Whether anything on the car screen holds the focus (main thread), see [trackCarFocus]. */
    var anyFocused by mutableStateOf(false)
}

/** On the car screen's root: records whether anything in it holds the focus. */
fun Modifier.trackCarFocus(): Modifier = onFocusChanged { CarFocusState.anyFocused = it.hasFocus }

/**
 * What to do at each check after a screen opened. Ask while the content does not hold the focus;
 * stop once it has held it at two checks in a row after [settleAfterMs] (the old screen has faded
 * out by then), or when [giveUpAfterMs] have passed (a screen with nothing to focus, like a map).
 */
internal class FocusSettle(private val settleAfterMs: Long = SETTLE_AFTER_MS, private val giveUpAfterMs: Long = GIVE_UP_AFTER_MS) {
    enum class Step { ASK, WAIT, DONE }

    private var heldInARow = 0

    fun step(elapsedMs: Long, contentFocused: Boolean): Step {
        if (elapsedMs >= giveUpAfterMs) return Step.DONE
        if (!contentFocused) { heldInARow = 0; return Step.ASK }
        if (elapsedMs < settleAfterMs) return Step.WAIT
        heldInARow++
        return if (heldInARow >= 2) Step.DONE else Step.WAIT
    }

    companion object {
        const val STEP_MS = 100L
        const val SETTLE_AFTER_MS = 300L
        const val GIVE_UP_AFTER_MS = 4000L
    }
}

/** Whether to hand the focus to the content because nothing holds it: only while the knob or D-pad is in use. */
internal fun shouldRecoverFocus(keyInput: Boolean, anyFocused: Boolean): Boolean = keyInput && !anyFocused

/**
 * In the content area: the two rules above. [screen] is what is shown, [contentFocused] whether the
 * content holds the focus now, [entry] the requester that puts the focus on the content's first item.
 */
@Composable
internal fun KeepFocusInContent(screen: Any, contentFocused: () -> Boolean, entry: FocusRequester) {
    LaunchedEffect(screen) {
        // At once, as before: also when the content already holds the focus, so that it moves from
        // the item that was pressed to the new screen's first item.
        runCatching { entry.requestFocus() }
        val settle = FocusSettle()
        var elapsed = 0L
        while (true) {
            delay(FocusSettle.STEP_MS)
            elapsed += FocusSettle.STEP_MS
            when (settle.step(elapsed, contentFocused())) {
                FocusSettle.Step.ASK -> runCatching { entry.requestFocus() }
                FocusSettle.Step.WAIT -> Unit
                FocusSettle.Step.DONE -> break
            }
        }
    }
    // The focused item went away with its page (a sub-page opened, a list reloaded): nothing holds
    // the focus, and the next turn would start from the bar. Tried for a few seconds, in case the
    // new page has nothing to focus yet.
    val lost = shouldRecoverFocus(CarFocusVisibility.keyInput, CarFocusState.anyFocused)
    LaunchedEffect(lost) {
        if (!lost) return@LaunchedEffect
        repeat(RECOVER_TRIES) {
            delay(FocusSettle.STEP_MS)
            if (!shouldRecoverFocus(CarFocusVisibility.keyInput, CarFocusState.anyFocused)) return@LaunchedEffect
            runCatching { entry.requestFocus() }
        }
    }
}

private const val RECOVER_TRIES = 40

/**
 * MyPlay: when the knob moves the ring to this element, scroll its list or grid so the whole
 * element, ring included, is on screen with a little room around it.
 *
 * Compose already scrolls a focused element into view, but only just, and it asks at the moment
 * focus arrives; an entry of a lazy list that was only composed for this focus move may not have
 * its final place yet. This asks once more, one frame later, for a slightly larger area.
 * Part of [carFocusHighlight], so everything that shows the ring has it.
 */
internal fun Modifier.keepFocusedVisible(): Modifier = composed {
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val margin = with(LocalDensity.current) { FOCUS_VIEW_MARGIN.toPx() }
    var size by remember { mutableStateOf(IntSize.Zero) }
    bringIntoViewRequester(requester)
        .onSizeChanged { size = it }
        .onFocusChanged {
            if (!it.isFocused || !CarFocusVisibility.keyInput) return@onFocusChanged
            scope.launch {
                withFrameNanos { }
                requester.bringIntoView(focusViewArea(size.width, size.height, margin))
            }
        }
}

/** Room kept between the focus ring and the edge of the list when scrolling it into view. */
private val FOCUS_VIEW_MARGIN = 12.dp

/** The area to scroll into view for an element of this size: the element and [margin] around it. */
internal fun focusViewArea(width: Int, height: Int, margin: Float): Rect =
    Rect(0f - margin, 0f - margin, width + margin, height + margin)
