package net.prototux.aare.ui.phone

import net.prototux.aare.ui.car.myplay.HomePad
import net.prototux.aare.ui.car.myplay.HomePadAction
import net.prototux.aare.ui.car.myplay.HomePadKey
import org.junit.Assert.assertEquals
import org.junit.Test

class HomePadSectionTest {
    @Test
    fun everyKeyAndActionIsOfferedOnce() {
        assertEquals(HomePadKey.entries.toSet(), HOME_PAD_KEY_NAMES.map { it.first }.toSet())
        assertEquals(HOME_PAD_KEY_NAMES.size, HomePadKey.entries.size)
        assertEquals(HomePadAction.entries.toSet(), HOME_PAD_ACTION_NAMES.map { it.first }.toSet())
        assertEquals(HOME_PAD_ACTION_NAMES.size, HomePadAction.entries.size)
    }

    @Test
    fun changingOneKeyKeepsTheOthers() {
        val m = withHomePad(HomePad.DEFAULT, HomePadKey.DOWN, HomePadAction.CALLS)
        assertEquals(HomePadAction.CALLS, m[HomePadKey.DOWN])
        assertEquals(HomePad.DEFAULT - HomePadKey.DOWN, m - HomePadKey.DOWN)
        assertEquals(m, HomePad.decode(HomePad.encode(m)))
    }
}
