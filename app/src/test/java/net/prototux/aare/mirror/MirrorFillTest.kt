package net.prototux.aare.mirror

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** MyPlay: the phone's screen fills the car's when the phone is sideways. */
class MirrorFillTest {
    @Test fun sidewaysIsMirroredAtThePhonesShapeThenStretched() {
        assertTrue(MirrorFill.fills(2400, 1080))
        // A 20:9 phone on a 1280x720 view: the mirror at 20:9, which the view stretches to 1280x720.
        assertEquals(1280 to 576, MirrorFill.size(1280, 720, 2400, 1080))
        // A touch in a corner of the car screen is the phone's corner.
        assertEquals(2400f to 1080f, MirrorFill.toPhone(1280f, 720f, 1280, 720, 2400, 1080))
        assertEquals(1200f to 540f, MirrorFill.toPhone(640f, 360f, 1280, 720, 2400, 1080))
    }

    @Test fun uprightKeepsItsShape() {
        assertFalse(MirrorFill.fills(1080, 2400))
        assertEquals(1280 to 720, MirrorFill.size(1280, 720, 1080, 2400))
        // Centred: the middle of the view is the middle of the phone.
        val (x, y) = MirrorFill.toPhone(640f, 360f, 1280, 720, 1080, 2400)
        assertEquals(540f, x, 0.5f)
        assertEquals(1200f, y, 0.5f)
    }
}
