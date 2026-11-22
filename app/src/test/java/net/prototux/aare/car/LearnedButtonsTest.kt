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
}
