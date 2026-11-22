package net.prototux.aare.car

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** MyPlay (drive warnings): the ice sign's rule and its once-per-drive line. */
class DriveWarningsTest {
    @Test fun noTemperatureNoIce() {
        assertFalse(IceWarning.risk(null, shownBefore = false))
        assertFalse(IceWarning.risk(null, shownBefore = true))
        assertFalse(IceWarning.risk(Float.NaN, shownBefore = false))
    }

    @Test fun comesOnAtFourDegreesOrLess() {
        assertFalse(IceWarning.risk(4.1f, shownBefore = false))
        assertTrue(IceWarning.risk(4f, shownBefore = false))
        assertTrue(IceWarning.risk(-12f, shownBefore = false))
    }

    @Test fun goesOffOnlyAboveFiveDegrees() {
        assertTrue(IceWarning.risk(4.6f, shownBefore = true))
        assertTrue(IceWarning.risk(5f, shownBefore = true))
        assertFalse(IceWarning.risk(5.1f, shownBefore = true))
    }

    @Test fun theLineComesOncePerDrive() {
        val t = IceTracker()
        assertFalse(t.update(8f))
        assertFalse(t.showing)
        assertTrue(t.update(3f))
        assertTrue(t.showing)
        assertFalse(t.update(2f))
        assertFalse(t.update(7f))
        assertFalse(t.showing)
        // Back below: the sign again, but no second line in the same drive.
        assertFalse(t.update(1f))
        assertTrue(t.showing)
    }

    @Test fun degreesAreWhole() {
        assertEquals(3, IceWarning.degrees(3.4f))
        assertEquals(-2, IceWarning.degrees(-2.4f))
        assertEquals(0, IceWarning.degrees(-0.4f))
    }
}
