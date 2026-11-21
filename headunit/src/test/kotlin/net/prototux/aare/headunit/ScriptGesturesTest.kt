package net.prototux.aare.headunit

import net.prototux.aare.headunit.ScriptGestures.Step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** MyPlay (R7). */
class ScriptGesturesTest {
    @Test fun aHoldIsDownWaitUp() {
        assertEquals(listOf(Step.Down(66), Step.Wait(800), Step.Up(66)), ScriptGestures.steps("hold:66,800"))
    }

    @Test fun aDoubleIsTwoQuickPresses() {
        val s = ScriptGestures.steps("double:85")!!
        assertEquals(listOf(Step.Down(85), Step.Up(85), Step.Down(85), Step.Up(85)), s.filter { it !is Step.Wait })
        assertEquals(ScriptGestures.TAP_MS * 2 + ScriptGestures.DOUBLE_GAP_MS, s.filterIsInstance<Step.Wait>().sumOf { it.ms })
    }

    @Test fun holdAndTurnTurnsOneClickAtATimeWhileHeld() {
        val s = ScriptGestures.steps("holdturn:23,-3")!!.filter { it !is Step.Wait }
        assertEquals(listOf(Step.Down(23), Step.Turn(-1), Step.Turn(-1), Step.Turn(-1), Step.Up(23)), s)
    }

    @Test fun otherEventsAndJunkAreNotGestures() {
        for (w in listOf("key:4", "rotate:1", "tap:1,2", "hold:66", "hold:x,500", "hold:66,0", "holdturn:23,0", "double:", "night")) {
            assertNull(w, ScriptGestures.steps(w))
        }
    }
}
