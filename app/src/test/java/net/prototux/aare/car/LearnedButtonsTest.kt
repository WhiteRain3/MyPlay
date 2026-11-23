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
    @org.junit.Test fun navigateHomeIsAButtonAction() {
        org.junit.Assert.assertEquals(ButtonAction.NAVIGATE_HOME, ButtonAction.byId("navigate_home"))
    }

    /** HN-2, HN-3: only what opens something is hinted; the default pad's four directions all are. */
    @org.junit.Test fun padHintsLeaveOutNothing() {
        val h = net.prototux.aare.ui.car.myplay.PadHints.of(net.prototux.aare.ui.car.myplay.HomePad.DEFAULT)
        org.junit.Assert.assertEquals(net.prototux.aare.ui.car.myplay.HomePadAction.NEXT_SONG, h.up)
        org.junit.Assert.assertEquals(net.prototux.aare.ui.car.myplay.HomePadAction.NAVIGATE_HOME, h.down)
        org.junit.Assert.assertEquals(net.prototux.aare.ui.car.myplay.HomePadAction.MUSIC, h.left)
        org.junit.Assert.assertEquals(net.prototux.aare.ui.car.myplay.HomePadAction.MAPS, h.right)
        val none = net.prototux.aare.ui.car.myplay.HomePad.DEFAULT + (net.prototux.aare.ui.car.myplay.HomePadKey.DOWN to net.prototux.aare.ui.car.myplay.HomePadAction.NOTHING)
        org.junit.Assert.assertNull(net.prototux.aare.ui.car.myplay.PadHints.of(none).down)
        org.junit.Assert.assertTrue(net.prototux.aare.ui.car.myplay.PadHints.of(null).isEmpty)
    }
}
