package net.prototux.aare.car

import android.view.KeyEvent
import net.prototux.aare.car.MirroredKeys.TurnMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** MyPlay (G1). */
class MirroredKeysTest {
    @Test fun thePadAndThePressGoInAsThemselves() {
        for (k in listOf(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER)) {
            assertEquals(k, MirroredKeys.forward(k, false, TurnMode.UP_DOWN))
        }
        assertEquals(KeyEvent.KEYCODE_ENTER, MirroredKeys.forward(KeyEvent.KEYCODE_NUMPAD_ENTER, false, TurnMode.UP_DOWN))
    }

    @Test fun aTurnIsADirectionPerApp() {
        assertEquals(KeyEvent.KEYCODE_DPAD_DOWN, MirroredKeys.forward(KeyEvent.KEYCODE_TAB, false, TurnMode.UP_DOWN))
        assertEquals(KeyEvent.KEYCODE_DPAD_UP, MirroredKeys.forward(KeyEvent.KEYCODE_TAB, true, TurnMode.UP_DOWN))
        assertEquals(KeyEvent.KEYCODE_DPAD_RIGHT, MirroredKeys.forward(KeyEvent.KEYCODE_TAB, false, TurnMode.LEFT_RIGHT))
        assertEquals(KeyEvent.KEYCODE_DPAD_LEFT, MirroredKeys.forward(KeyEvent.KEYCODE_TAB, true, TurnMode.LEFT_RIGHT))
    }

    @Test fun backAndOtherKeysKeepTheirOwnWay() {
        for (k in listOf(KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MENU)) {
            assertNull(MirroredKeys.forward(k, false, TurnMode.UP_DOWN))
        }
    }

    @Test fun turnsPerAppRoundTripAndSkipJunk() {
        val m = mapOf("com.google.android.youtube" to TurnMode.LEFT_RIGHT, "org.example" to TurnMode.UP_DOWN)
        val text = MirroredKeys.encodeTurns(m)
        assertEquals("com.google.android.youtube=left_right", text) // the default is not stored
        assertEquals(TurnMode.LEFT_RIGHT, MirroredKeys.turnFor(text + ";x=sideways;=up_down;junk", "com.google.android.youtube"))
        assertEquals(TurnMode.UP_DOWN, MirroredKeys.turnFor(text, "org.other"))
        assertEquals(TurnMode.UP_DOWN, MirroredKeys.turnFor(null, "org.other"))
    }

    /** G1-3: Back three times within 1.5 s leaves; slower presses do not. */
    @Test fun threeQuickBacksLeave() {
        val q = QuickBacks()
        assertEquals(false, q.press(0)); assertEquals(false, q.press(400)); assertEquals(true, q.press(900))
        assertEquals(false, q.press(1000)) // starts over
        val slow = QuickBacks()
        assertEquals(false, slow.press(0)); assertEquals(false, slow.press(1000)); assertEquals(false, slow.press(2000))
    }
}
