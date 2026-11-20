package net.prototux.aare.ui.phone

import net.prototux.aare.car.ButtonAction
import net.prototux.aare.car.ButtonTable
import net.prototux.aare.car.ButtonTable.Control
import net.prototux.aare.car.ButtonTable.Gesture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ButtonsScreenTest {
    @Test
    fun aChangeIsStoredAndReadBackByTheCarsTable() {
        val stored = withAction("", Control.KNOB_PUSH, Gesture.HOLD, ButtonAction.ASSISTANT)
        assertEquals(ButtonAction.ASSISTANT, ButtonTable(ButtonTable.decodeOverrides(stored)).action(Control.KNOB_PUSH, Gesture.HOLD))
        assertEquals(1, changedCount(stored))
    }

    @Test
    fun choosingTheDefaultAgainLeavesNoChangeStored() {
        val changed = withAction("", Control.MENU, Gesture.PRESS, ButtonAction.HOME)
        val back = withAction(changed, Control.MENU, Gesture.PRESS, defaultAction(Control.MENU, Gesture.PRESS))
        assertEquals("", back)
        assertEquals(0, changedCount(back))
    }

    @Test
    fun otherChangesAreKept() {
        var s = withAction("", Control.WHEEL_CLICK, Gesture.DOUBLE, ButtonAction.NOTHING)
        s = withAction(s, Control.BACK, Gesture.HOLD, ButtonAction.ASSISTANT)
        val t = ButtonTable(ButtonTable.decodeOverrides(s))
        assertEquals(ButtonAction.NOTHING, t.action(Control.WHEEL_CLICK, Gesture.DOUBLE))
        assertEquals(ButtonAction.ASSISTANT, t.action(Control.BACK, Gesture.HOLD))
        assertEquals(2, changedCount(s))
    }

    @Test
    fun aGestureWithNoDefaultMeansNothing() {
        assertEquals(ButtonAction.NOTHING, defaultAction(Control.MENU, Gesture.DOUBLE))
        assertEquals("", withAction("", Control.MENU, Gesture.DOUBLE, ButtonAction.NOTHING))
    }

    @Test
    fun skippingSongsOnlyGoesWithHoldAndTurn() {
        assertEquals(listOf(ButtonAction.SKIP_SONGS, ButtonAction.NOTHING), actionsFor(Gesture.HOLD_TURN))
        assertFalse(ButtonAction.SKIP_SONGS in actionsFor(Gesture.PRESS))
        assertTrue(ButtonAction.NEXT in actionsFor(Gesture.HOLD))
    }

    @Test
    fun backHasNoDoublePressAndOnlyTheKnobHasHoldAndTurn() {
        assertFalse(Gesture.DOUBLE in gesturesFor(Control.BACK))
        assertTrue(Gesture.HOLD_TURN in gesturesFor(Control.KNOB_PUSH))
        Control.entries.filter { it != Control.KNOB_PUSH }.forEach { assertFalse(it.name, Gesture.HOLD_TURN in gesturesFor(it)) }
    }

    @Test
    fun everyDefaultIsOfferedOnThePage() {
        ButtonTable.DEFAULTS.forEach { (k, v) ->
            assertTrue("$k", k.second in gesturesFor(k.first))
            assertTrue("$k $v", v in actionsFor(k.second))
        }
    }
}
