package net.prototux.aare.car

import net.prototux.aare.car.GestureEngine.Gesture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** MyPlay (plan R2). */
class GestureEngineTest {
    private val ENTER = 66
    private val BACK = 4
    private val got = ArrayList<Gesture>()
    private var time = 0L
    private val timers = ArrayList<Pair<Long, () -> Unit>>()

    private val clock = object : GestureEngine.Clock {
        override fun now() = time
        override fun after(delayMs: Long, task: () -> Unit): () -> Unit {
            val t = (time + delayMs) to task
            timers += t
            return { timers.remove(t) }
        }
    }

    /** Moves the clock on, running the timers that fall due, in order. */
    private fun advance(ms: Long) {
        val end = time + ms
        while (true) {
            val next = timers.filter { it.first <= end }.minByOrNull { it.first } ?: break
            timers.remove(next)
            time = next.first
            next.second()
        }
        time = end
    }

    private val engine = GestureEngine({ GestureEngine.Timings() }, doubleCapable = { it == ENTER }, clock = clock) { got += it }

    private fun press(code: Int, holdFor: Long = 50) {
        engine.onKey(code, true)
        advance(holdFor)
        engine.onKey(code, false)
    }

    @Test fun aShortPressOfAPlainKeyIsAPressAtOnce() {
        press(BACK)
        assertEquals(listOf(Gesture.Press(BACK)), got)
    }

    @Test fun aDoubleCapableKeysPressWaitsForASecond() {
        press(ENTER)
        assertTrue(got.isEmpty())
        advance(GestureEngine.DOUBLE_MS)
        assertEquals(listOf(Gesture.Press(ENTER)), got)
    }

    @Test fun twoQuickPressesAreADouble() {
        press(ENTER)
        advance(100)
        press(ENTER)
        advance(1_000)
        assertEquals(listOf(Gesture.Double(ENTER)), got)
    }

    @Test fun twoSlowPressesAreTwoPresses() {
        press(ENTER)
        advance(GestureEngine.DOUBLE_MS + 10)
        press(ENTER)
        advance(1_000)
        assertEquals(listOf(Gesture.Press(ENTER), Gesture.Press(ENTER)), got)
    }

    @Test fun holdingFiresTheHoldWhileStillDownAndTheUpDoesNothing() {
        engine.onKey(ENTER, true)
        advance(GestureEngine.HOLD_MS)
        assertEquals(listOf(Gesture.Hold(ENTER)), got)
        advance(2_000)
        engine.onKey(ENTER, false)
        advance(1_000)
        assertEquals(listOf(Gesture.Hold(ENTER)), got)
    }

    @Test fun theCarsLongPressFlagIsAHoldAtOnce() {
        engine.onKey(BACK, true)
        advance(100)
        engine.onKey(BACK, true, carLongPress = true)
        engine.onKey(BACK, true, carLongPress = true)
        engine.onKey(BACK, false, carLongPress = true)
        advance(2_000)
        assertEquals(listOf(Gesture.Hold(BACK)), got)
    }

    @Test fun aFlagOnlyOnTheUpIsAHold() {
        engine.onKey(BACK, true)
        engine.onKey(BACK, false, carLongPress = true)
        advance(2_000)
        assertEquals(listOf(Gesture.Hold(BACK)), got)
    }

    @Test fun aFlaggedUpWithNoDownIsAHold() {
        engine.onKey(ENTER, false, carLongPress = true)
        advance(2_000)
        assertEquals(listOf(Gesture.Hold(ENTER)), got)
    }

    @Test fun repeatedDownsKeepTheFirstDownsTime() {
        engine.onKey(BACK, true)
        advance(300)
        engine.onKey(BACK, true)
        advance(300)
        assertEquals(listOf(Gesture.Hold(BACK)), got)
    }

    @Test fun aLostUpIsForgotten() {
        engine.onKey(BACK, true)
        advance(GestureEngine.HOLD_MS) // a hold fires
        advance(GestureEngine.STALE_MS + 1) // the up never came
        got.clear()
        press(BACK)
        assertEquals(listOf(Gesture.Press(BACK)), got)
    }

    @Test fun aTurnWhileAKeyIsDownIsAHoldAndTurn() {
        engine.onKey(ENTER, true)
        advance(100)
        assertTrue(engine.onTurn(2))
        assertTrue(engine.onTurn(-1))
        engine.onKey(ENTER, false)
        advance(1_000)
        assertEquals(listOf(Gesture.HoldTurn(ENTER, 2), Gesture.HoldTurn(ENTER, -1)), got)
    }

    @Test fun aTurnWithNoKeyDownMovesTheRing() {
        assertFalse(engine.onTurn(1))
        assertTrue(got.isEmpty())
    }

    @Test fun aTurnAfterTheHoldFiredStillBelongsToTheHold() {
        engine.onKey(ENTER, true)
        advance(GestureEngine.HOLD_MS + 100)
        assertTrue(engine.onTurn(1))
        engine.onKey(ENTER, false)
        assertEquals(listOf(Gesture.Hold(ENTER), Gesture.HoldTurn(ENTER, 1)), got)
    }

    @Test fun timingsComeFromTheSettings() {
        val fast = GestureEngine({ GestureEngine.Timings(holdMs = 200, doubleMs = 100) }, { it == ENTER }, clock) { got += it }
        fast.onKey(ENTER, true)
        advance(200)
        assertEquals(listOf(Gesture.Hold(ENTER)), got)
    }

    @Test fun keysAreIndependent() {
        engine.onKey(BACK, true)
        press(ENTER)
        advance(GestureEngine.DOUBLE_MS)
        assertEquals(listOf(Gesture.Press(ENTER)), got)
        advance(GestureEngine.HOLD_MS)
        assertEquals(listOf(Gesture.Press(ENTER), Gesture.Hold(BACK)), got)
    }
}
