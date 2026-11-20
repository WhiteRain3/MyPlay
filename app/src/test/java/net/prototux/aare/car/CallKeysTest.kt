package net.prototux.aare.car

import net.prototux.aare.ui.car.CarCall
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** MyPlay (R5). */
class CallKeysTest {
    @Test fun backDeclinesOnlyARingingCallThatCanBeEnded() {
        assertTrue(CallKeys.backDeclines(CarCall(ringing = true, name = "A", sinceMs = 0), canEnd = true))
        assertFalse(CallKeys.backDeclines(CarCall(ringing = true, name = "A", sinceMs = 0), canEnd = false))
        assertFalse(CallKeys.backDeclines(CarCall(ringing = false, name = "A", sinceMs = 0), canEnd = true))
        assertFalse(CallKeys.backDeclines(null, canEnd = true))
    }
}
