package net.prototux.aare.ui.car

import net.prototux.aare.ui.car.FocusSettle.Step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusSettleTest {
    /** Runs the checks every 100 ms with [focusedAt] saying whether the content holds the focus at that time. */
    private fun run(focusedAt: (Long) -> Boolean): List<Pair<Long, Step>> {
        val s = FocusSettle()
        val out = ArrayList<Pair<Long, Step>>()
        var t = 0L
        while (true) {
            t += FocusSettle.STEP_MS
            val step = s.step(t, focusedAt(t))
            out += t to step
            if (step == Step.DONE) return out
        }
    }

    @Test fun asksWhileTheContentHasNoFocus() {
        val steps = run { it >= 700 }
        assertTrue(steps.filter { it.first < 700 }.all { it.second == Step.ASK })
    }

    /** A hosted app whose first page comes after 700 ms: done soon after it took the focus. */
    @Test fun stopsOnceTheContentHasKeptTheFocus() {
        val steps = run { it >= 700 }
        assertEquals(800L, steps.last().first)
        assertEquals(Step.DONE, steps.last().second)
    }

    /** The old screen held the focus while it faded out, then went away: that early focus does not count. */
    @Test fun focusDuringTheFadeDoesNotEndIt() {
        val steps = run { it < 200 || it >= 500 }
        assertEquals(Step.WAIT, steps.first { it.first == 100L }.second)
        assertEquals(Step.ASK, steps.first { it.first == 200L }.second)
        assertEquals(Step.ASK, steps.first { it.first == 400L }.second)
        assertEquals(600L, steps.last().first)
    }

    /** Focus lost again right after the fade: keep asking, and count from the start again. */
    @Test fun aLossAfterSettlingStartsTheCountAgain() {
        val steps = run { it != 400L }
        assertEquals(Step.WAIT, steps.first { it.first == 300L }.second)
        assertEquals(Step.ASK, steps.first { it.first == 400L }.second)
        assertEquals(600L, steps.last().first)
    }

    /** A screen with nothing to focus (a map): give up, so the knob can go to the bar. */
    @Test fun givesUpOnAScreenWithNothingToFocus() {
        val steps = run { false }
        assertEquals(FocusSettle.GIVE_UP_AFTER_MS, steps.last().first)
        assertTrue(steps.dropLast(1).all { it.second == Step.ASK })
    }

    @Test fun focusIsRecoveredOnlyWhileTheKnobIsInUse() {
        assertTrue(shouldRecoverFocus(keyInput = true, anyFocused = false))
        assertFalse(shouldRecoverFocus(keyInput = true, anyFocused = true))
        assertFalse(shouldRecoverFocus(keyInput = false, anyFocused = false))
    }
}
