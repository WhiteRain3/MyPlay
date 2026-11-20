package net.prototux.aare.ui.car

import net.prototux.aare.ui.car.StatusStyle.BatteryLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** MyPlay (F4). */
class StatusStyleTest {
    @Test fun theDefaultsAreAaresLook() {
        val s = StatusStyle()
        assertEquals("57", s.batteryText(57))
        assertEquals(BatteryLevel.LOW, s.batteryLevel(15, charging = false))
        assertEquals(BatteryLevel.NORMAL, s.batteryLevel(16, charging = false))
        assertEquals(BatteryLevel.NORMAL, s.batteryLevel(25, charging = false))
        assertTrue(s.batteryIcon && s.signal)
        assertEquals(StatusSize.NORMAL, s.size)
    }

    @Test fun theNumberCanHaveASignOrBeOff() {
        assertEquals("57%", StatusStyle(percentSign = true).batteryText(57))
        assertNull(StatusStyle(batteryNumber = false).batteryText(57))
        assertEquals("100", StatusStyle().batteryText(130))
    }

    @Test fun colourByLevel() {
        val s = StatusStyle(colorByLevel = true)
        assertEquals(BatteryLevel.LOW, s.batteryLevel(10, charging = false))
        assertEquals(BatteryLevel.MEDIUM, s.batteryLevel(30, charging = false))
        assertEquals(BatteryLevel.HIGH, s.batteryLevel(31, charging = false))
    }

    @Test fun chargingIsNeverRed() = assertEquals(BatteryLevel.NORMAL, StatusStyle(colorByLevel = true).batteryLevel(5, charging = true))

    @Test fun sizesAreStoredByKey() {
        StatusSize.entries.forEach { assertEquals(it, StatusSize.byKey(it.key)) }
        assertEquals(StatusSize.NORMAL, StatusSize.byKey("huge"))
        assertEquals(StatusSize.NORMAL, StatusSize.byKey(null))
    }

    @Test fun statusKeysAreRecognised() {
        assertTrue(isStatusStyleKey("myplay_status_size"))
        assertTrue(isStatusStyleKey("myplay_clock_bold"))
        assertFalse(isStatusStyleKey("car_theme"))
        assertFalse(isStatusStyleKey(null))
    }
}
