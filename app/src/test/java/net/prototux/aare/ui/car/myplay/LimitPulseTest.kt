package net.prototux.aare.ui.car.myplay

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** MyPlay (#7): when the limit sign pulses. */
class LimitPulseTest {
    @Test fun onlyAChangeBetweenTwoKnownLimits() {
        val p = LimitPulse()
        assertFalse(p.update(50, 0)) // first showing in the drive
        assertFalse(p.update(50, 1_000)) // the same limit again
        assertTrue(p.update(70, 10_000)) // 50 to 70
    }

    @Test fun notAfterNotKnown() {
        val p = LimitPulse()
        assertFalse(p.update(null, 0))
        assertFalse(p.update(50, 10_000))
        assertFalse(p.update(null, 20_000))
        assertFalse(p.update(70, 30_000))
        assertTrue(p.update(90, 40_000))
    }

    @Test fun notAgainWithinTheCooldown() {
        val p = LimitPulse()
        p.update(50, 0)
        assertTrue(p.update(70, 10_000))
        assertFalse(p.update(50, 12_000)) // a junction's flicker
        assertFalse(p.update(70, 14_000))
        assertTrue(p.update(90, 10_000 + LimitPulse.COOLDOWN_MS))
    }
}
