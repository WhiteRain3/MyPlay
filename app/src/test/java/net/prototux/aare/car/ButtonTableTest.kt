package net.prototux.aare.car

import android.view.KeyEvent
import net.prototux.aare.car.ButtonTable.Control
import net.prototux.aare.car.ButtonTable.Gesture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** MyPlay (plan R3). */
class ButtonTableTest {
    private val table = ButtonTable()

    @Test fun everyDefaultResolves() {
        for ((key, action) in ButtonTable.DEFAULTS) {
            for (code in table.codes(key.first)) assertEquals(action, table.resolve(code, key.second))
        }
        assertEquals(ButtonAction.SELECT, table.resolve(KeyEvent.KEYCODE_DPAD_CENTER, Gesture.PRESS))
        assertEquals(ButtonAction.NEXT, table.resolve(KeyEvent.KEYCODE_ENTER, Gesture.HOLD))
        assertEquals(ButtonAction.HOME, table.resolve(KeyEvent.KEYCODE_BACK, Gesture.HOLD))
        assertEquals(ButtonAction.ASSISTANT, table.resolve(KeyEvent.KEYCODE_MENU, Gesture.HOLD))
        assertEquals(ButtonAction.PREVIOUS, table.resolve(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, Gesture.DOUBLE))
    }

    @Test fun anUnknownKeyIsNotInTheTable() {
        assertNull(table.resolve(KeyEvent.KEYCODE_DPAD_UP, Gesture.PRESS))
        assertNull(table.control(KeyEvent.KEYCODE_A))
    }

    @Test fun aGestureWithoutADefaultDoesNothing() {
        assertEquals(ButtonAction.NOTHING, table.resolve(KeyEvent.KEYCODE_BACK, Gesture.HOLD_TURN))
    }

    @Test fun aChangeWinsOverTheDefault() {
        val t = ButtonTable(mapOf((Control.KNOB_PUSH to Gesture.HOLD) to ButtonAction.PLAY_PAUSE))
        assertEquals(ButtonAction.PLAY_PAUSE, t.resolve(KeyEvent.KEYCODE_ENTER, Gesture.HOLD))
        assertEquals(ButtonAction.SELECT, t.resolve(KeyEvent.KEYCODE_ENTER, Gesture.PRESS))
    }

    @Test fun theCodesCanBeChanged() {
        val t = ButtonTable(codeOverrides = mapOf(Control.WHEEL_CLICK to setOf(KeyEvent.KEYCODE_HEADSETHOOK)))
        assertEquals(ButtonAction.PLAY_PAUSE, t.resolve(KeyEvent.KEYCODE_HEADSETHOOK, Gesture.PRESS))
        assertNull(t.resolve(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, Gesture.PRESS))
    }

    @Test fun onlyKeysWithADoubleActionWaitForASecondPress() {
        assertFalse(table.doubleCapable(KeyEvent.KEYCODE_DPAD_CENTER))
        assertFalse(table.doubleCapable(KeyEvent.KEYCODE_BACK))
        assertTrue(table.doubleCapable(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        assertFalse(table.doubleCapable(KeyEvent.KEYCODE_DPAD_UP))
    }

    @Test fun onlyTheKnobsHoldTakesTheTurnByDefault() {
        assertTrue(table.turnCapable(KeyEvent.KEYCODE_DPAD_CENTER))
        assertFalse(table.turnCapable(KeyEvent.KEYCODE_BACK))
    }

    @Test fun overridesRoundTripAndSkipJunk() {
        val m = mapOf(
            (Control.MENU to Gesture.PRESS) to ButtonAction.OPEN_MEDIA,
            (Control.BACK to Gesture.DOUBLE) to ButtonAction.HOME,
        )
        assertEquals(m, ButtonTable.decodeOverrides(ButtonTable.encodeOverrides(m)))
        assertEquals(m, ButtonTable.decodeOverrides(ButtonTable.encodeOverrides(m) + ";nope.press=home;menu.fly=home;back.press=fly;x"))
        assertTrue(ButtonTable.decodeOverrides(null).isEmpty())
    }

    @Test fun codesRoundTripAndSkipJunk() {
        val m = mapOf(Control.MENU to setOf(82, 1000), Control.BACK to setOf(4))
        assertEquals(m, ButtonTable.decodeCodes(ButtonTable.encodeCodes(m)))
        assertEquals(m, ButtonTable.decodeCodes(ButtonTable.encodeCodes(m) + ";knob_push=;wheel=3"))
    }

    /** B1-1: "press what has the ring" on Back, Menu or the wheel presses the item, not that key. */
    @Test fun selectAlwaysPressesTheRingsItem() {
        assertEquals(KeyEvent.KEYCODE_DPAD_CENTER, ButtonAction.selectKeyFor(KeyEvent.KEYCODE_BACK))
        assertEquals(KeyEvent.KEYCODE_DPAD_CENTER, ButtonAction.selectKeyFor(KeyEvent.KEYCODE_MENU))
        assertEquals(KeyEvent.KEYCODE_DPAD_CENTER, ButtonAction.selectKeyFor(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        assertEquals(KeyEvent.KEYCODE_DPAD_CENTER, ButtonAction.selectKeyFor(KeyEvent.KEYCODE_DPAD_CENTER))
        assertEquals(KeyEvent.KEYCODE_ENTER, ButtonAction.selectKeyFor(KeyEvent.KEYCODE_ENTER))
    }
}
