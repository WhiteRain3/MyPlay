package net.prototux.aare.car

import org.junit.Assert.assertEquals
import org.junit.Test

class HeldKeyTest {
    private val events = ArrayList<String>()
    private var clock = 0L
    private val key = HeldKey({ clock }, onPress = { events += "press" }, onHold = { events += "hold" })

    @Test fun shortPressFiresOnTheUp() {
        key.onKey(down = true, longPress = false)
        assertEquals(emptyList<String>(), events)
        clock += 100
        key.onKey(down = false, longPress = false)
        assertEquals(listOf("press"), events)
    }

    /** A car that does not flag long presses: the time the key stayed down decides. */
    @Test fun heldWithoutAFlagIsAHold() {
        key.onKey(down = true, longPress = false)
        clock += HeldKey.HOLD_MS
        key.onKey(down = false, longPress = false)
        assertEquals(listOf("hold"), events)
    }

    /** Repeated downs while held keep the time of the first one. */
    @Test fun repeatsDoNotRestartTheClock() {
        key.onKey(down = true, longPress = false)
        clock += 400
        key.onKey(down = true, longPress = false)
        clock += 400
        key.onKey(down = false, longPress = false)
        assertEquals(listOf("hold"), events)
    }

    @Test fun flaggedDownFiresOnceAndItsUpDoesNothing() {
        key.onKey(down = true, longPress = false)
        key.onKey(down = true, longPress = true)
        key.onKey(down = true, longPress = true)
        clock += 2000
        key.onKey(down = false, longPress = true)
        assertEquals(listOf("hold"), events)
    }

    @Test fun flagOnlyOnTheUpIsAHold() {
        key.onKey(down = true, longPress = false)
        key.onKey(down = false, longPress = true)
        assertEquals(listOf("hold"), events)
    }

    @Test fun anUpWithoutADownIsAPress() {
        key.onKey(down = false, longPress = false)
        assertEquals(listOf("press"), events)
    }

    /** A down whose up was lost: the next press, seconds later, is a press, not the end of a hold. */
    @Test fun aDownWithALostUpIsForgotten() {
        key.onKey(down = true, longPress = false)
        clock += HeldKey.STALE_MS + 1
        key.onKey(down = true, longPress = false)
        clock += 100
        key.onKey(down = false, longPress = false)
        assertEquals(listOf("press"), events)
    }

    /** A real long hold whose repeats keep coming is still a hold, however long. */
    @Test fun aLongHoldWithRepeatsIsStillAHold() {
        key.onKey(down = true, longPress = false)
        repeat(20) { clock += 500; key.onKey(down = true, longPress = false) }
        key.onKey(down = false, longPress = false)
        assertEquals(listOf("hold"), events)
    }

    @Test fun aPressAfterAHoldIsAPressAgain() {
        key.onKey(down = true, longPress = false); clock += 1000; key.onKey(down = false, longPress = false)
        key.onKey(down = true, longPress = false); clock += 50; key.onKey(down = false, longPress = false)
        assertEquals(listOf("hold", "press"), events)
    }
}
