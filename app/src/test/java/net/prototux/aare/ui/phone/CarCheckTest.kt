package net.prototux.aare.ui.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CarCheckTest {
    private fun capture(vararg inputs: CheckInput) = inputs.fold(StepCapture()) { c, i -> c.with(i) }

    @Test
    fun aPushIsOneDownAndOneUp() {
        val c = capture(CheckInput.Key(3, 23, true, false, 1000), CheckInput.Key(3, 23, false, false, 1120))
        assertEquals(listOf("DPAD_CENTER (23): 1 down, 1 up, held 120 ms, channel 3"), c.lines(5000))
    }

    @Test
    fun aHeldKeyShowsTheCarsRepeatsAndLongPress() {
        val c = capture(
            CheckInput.Key(3, 4, true, false, 0), CheckInput.Key(3, 4, true, false, 500), CheckInput.Key(3, 4, true, true, 600),
            CheckInput.Key(3, 4, false, false, 1500),
        )
        assertEquals(listOf("BACK (4): 3 down, 1 up, held 1500 ms, the car repeated the down 2 times while held, long press flag, channel 3"), c.lines(9000))
    }

    @Test
    fun aKeyStillDownIsSaid() {
        val c = capture(CheckInput.Key(3, 82, true, false, 1000))
        assertEquals(listOf("MENU (82): 1 down, 0 up, held 400 ms (still down), channel 3"), c.lines(1400))
    }

    @Test
    fun turnsAreAddedUpByDirection() {
        val c = capture(CheckInput.Turn(3, 65536, 1, 0), CheckInput.Turn(3, 65536, 2, 1), CheckInput.Turn(3, 65536, -1, 2), CheckInput.Turn(3, 65536, 0, 3))
        assertEquals(listOf("ROTARY_CONTROLLER (65536) turned: 3 forward, 1 back in 4 events, 1 with no movement, channel 3"), c.lines(0))
    }

    @Test
    fun differentCodesAndChannelsAreKeptApart() {
        val c = capture(CheckInput.Key(3, 19, true, false, 0), CheckInput.Key(4, 19, true, false, 0), CheckInput.Absolute(3, 7, 42, 0), CheckInput.Touchpad(5, 0))
        assertEquals(4, c.lines(0).size)
        assertTrue(c.lines(0)[2].contains("absolute value 42"))
    }

    @Test
    fun theSummarySaysWhatNeverArrived() {
        val summary = checkSummary(
            mapOf(CheckStep.KNOB_PUSH to capture(CheckInput.Key(3, 23, true, false, 0), CheckInput.Key(3, 23, false, false, 50)), CheckStep.MENU to StepCapture()),
            { it.id }, 100,
        )
        assertEquals("knob_right: not tried", summary[0])
        assertEquals("knob_push: DPAD_CENTER (23): 1 down, 1 up, held 50 ms, channel 3", summary[2])
        assertEquals("menu: nothing arrived", summary[CheckStep.MENU.ordinal])
        assertEquals(CheckStep.entries.size, summary.size)
    }
}
