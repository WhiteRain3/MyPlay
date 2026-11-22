package net.prototux.aare.car

import android.os.PowerManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** MyPlay (drive warnings): the phone's heat, logged on each change, its line once per drive. */
class PhoneHeatTest {
    @Test fun hotFromSevereOn() {
        assertFalse(PhoneHeatRule.hot(PowerManager.THERMAL_STATUS_MODERATE))
        assertTrue(PhoneHeatRule.hot(PowerManager.THERMAL_STATUS_SEVERE))
        assertTrue(PhoneHeatRule.hot(PowerManager.THERMAL_STATUS_CRITICAL))
    }

    @Test fun logsChangesAndShowsTheLineOnce() {
        val t = PhoneHeatTracker()
        assertEquals(true to false, t.update(PowerManager.THERMAL_STATUS_NONE))
        assertEquals(false to false, t.update(PowerManager.THERMAL_STATUS_NONE))
        assertEquals(true to true, t.update(PowerManager.THERMAL_STATUS_SEVERE))
        assertEquals(true to false, t.update(PowerManager.THERMAL_STATUS_LIGHT))
        assertEquals(true to false, t.update(PowerManager.THERMAL_STATUS_CRITICAL))
    }

    @Test fun namesInPlainWords() {
        assertEquals("severe (slowing down)", PhoneHeatRule.name(PowerManager.THERMAL_STATUS_SEVERE))
        assertEquals("unknown (42)", PhoneHeatRule.name(42))
    }
}
