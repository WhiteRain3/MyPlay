package net.prototux.aare.car

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** MyPlay (O11): buttons taught an action in the car. */
class LearnedButtonsTest {
    @Test fun aLearnedButtonIsStoredByItsCode() {
        val m = LearnedButtons.learn(emptyMap(), 1001, ButtonAction.OPEN_NAVIGATION)
        assertEquals(mapOf(1001 to ButtonAction.OPEN_NAVIGATION), m)
        assertEquals(m, LearnedButtons.decode(LearnedButtons.encode(m)))
        // Taught again: the new action wins; NOTHING forgets it.
        assertEquals(ButtonAction.OPEN_MEDIA, LearnedButtons.learn(m, 1001, ButtonAction.OPEN_MEDIA)[1001])
        assertTrue(LearnedButtons.learn(m, 1001, ButtonAction.NOTHING).isEmpty())
    }

    @Test fun unknownPartsAreSkipped() {
        assertEquals(mapOf(5 to ButtonAction.HOME), LearnedButtons.decode("x=home;5=home;6=nonsense;;7"))
        assertTrue(LearnedButtons.decode(null).isEmpty())
    }

    @Test fun backAndTheKnobAreNeverTaught() {
        for (code in listOf(KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_TAB, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER)) {
            assertTrue(code in LearnedButtons.NEVER)
        }
    }

    /** O11-1: the pad and the call keys are never taught; an arm lasts 30 s. */
    @Test fun thePadAndTheCallKeysAreNeverTaughtAndTheArmExpires() {
        for (code in listOf(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_CALL, KeyEvent.KEYCODE_ENDCALL, KeyEvent.KEYCODE_NUMPAD_ENTER)) {
            assertTrue(code in LearnedButtons.NEVER)
        }
        assertTrue(LearnedButtons.stillArmed(1_000, 1_000 + 29_000))
        assertTrue(!LearnedButtons.stillArmed(1_000, 1_000 + 31_000))
        // A clock that went back does not keep an arm forever.
        assertTrue(!LearnedButtons.stillArmed(50_000, 1_000))
    }
    /** HN-2: the hints get the taught actions in the codes' order, each once, never nothing. */
    @org.junit.Test fun theHintsGetTheTaughtActions() {
        val m = mapOf(300 to ButtonAction.NAVIGATE_HOME, 200 to ButtonAction.NEXT, 250 to ButtonAction.NEXT)
        org.junit.Assert.assertEquals(listOf(ButtonAction.NEXT, ButtonAction.NAVIGATE_HOME), learnedActions(m))
        org.junit.Assert.assertEquals(ButtonAction.NAVIGATE_HOME, ButtonAction.byId("navigate_home"))
    }

    /** HN-2: only what opens something is hinted; at most two taught buttons (the two left diagonals). */
    @org.junit.Test fun padHintsLeaveOutNothing() {
        val h = net.prototux.aare.ui.car.myplay.PadHints.of(net.prototux.aare.ui.car.myplay.HomePad.DEFAULT,
            listOf(ButtonAction.NAVIGATE_HOME, ButtonAction.NEXT, ButtonAction.PREVIOUS))
        org.junit.Assert.assertEquals(net.prototux.aare.ui.car.myplay.HomePadAction.MAPS, h.up)
        org.junit.Assert.assertNull(h.down)
        org.junit.Assert.assertEquals(net.prototux.aare.ui.car.myplay.HomePadAction.DIAL, h.side)
        org.junit.Assert.assertEquals(listOf(ButtonAction.NAVIGATE_HOME, ButtonAction.NEXT), h.corners)
        org.junit.Assert.assertTrue(net.prototux.aare.ui.car.myplay.PadHints.of(null, emptyList()).isEmpty)
    }
}
