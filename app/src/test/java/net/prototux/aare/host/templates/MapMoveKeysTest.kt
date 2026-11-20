package net.prototux.aare.host.templates

import androidx.compose.ui.input.key.Key
import net.prototux.aare.host.templates.MapMoveKeys.Action
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** MyPlay (R5): the knob on a hosted map. */
class MapMoveKeysTest {
    @Test fun outsideOnlyThePushDoesSomething() {
        assertEquals(Action.Enter, MapMoveKeys.action(Key.DirectionCenter, false, moving = false))
        assertEquals(Action.Enter, MapMoveKeys.action(Key.Enter, false, moving = false))
        for (k in listOf(Key.Tab, Key.DirectionLeft, Key.DirectionUp, Key.Back)) assertNull(MapMoveKeys.action(k, false, moving = false))
    }

    @Test fun movingThePadPansAndTurningZooms() {
        val f = MapMoveKeys.PAN_FRACTION
        assertEquals(Action.Pan(-f, 0f), MapMoveKeys.action(Key.DirectionLeft, false, true))
        assertEquals(Action.Pan(f, 0f), MapMoveKeys.action(Key.DirectionRight, false, true))
        assertEquals(Action.Pan(0f, -f), MapMoveKeys.action(Key.DirectionUp, false, true))
        assertEquals(Action.Pan(0f, f), MapMoveKeys.action(Key.DirectionDown, false, true))
        assertEquals(Action.Zoom(MapMoveKeys.ZOOM_IN), MapMoveKeys.action(Key.Tab, false, true))
        assertEquals(Action.Zoom(MapMoveKeys.ZOOM_OUT), MapMoveKeys.action(Key.Tab, true, true))
    }

    @Test fun aPushLeavesAndBackIsLeftToTheBackHandler() {
        assertEquals(Action.Leave, MapMoveKeys.action(Key.DirectionCenter, false, true))
        assertNull(MapMoveKeys.action(Key.Back, false, true))
    }
}
