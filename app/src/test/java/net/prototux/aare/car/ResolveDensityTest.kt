package net.prototux.aare.car

import org.junit.Assert.assertEquals
import org.junit.Test

/** The car screen's density, in precedence order (vehicle override > phone setting > head unit > default). */
class ResolveDensityTest {
    @Test
    fun vehicleOverrideWinsOverEverything() {
        assertEquals(300, CarSession.resolveDensity(vehicleDpi = 300, settingDpi = 160, headUnitDensity = 240, shortSide = 720))
    }

    @Test
    fun phoneSettingWinsWithNoVehicleOverride() {
        assertEquals(180, CarSession.resolveDensity(vehicleDpi = null, settingDpi = 180, headUnitDensity = 240, shortSide = 720))
    }

    @Test
    fun autoSettingFallsBackToTheHeadUnitsOwnValue() {
        // 0 is "Auto": neither a vehicle override nor a fixed phone choice.
        assertEquals(240, CarSession.resolveDensity(vehicleDpi = null, settingDpi = 0, headUnitDensity = 240, shortSide = 720))
    }

    @Test
    fun autoWithNoHeadUnitValueFallsBackToTheSizeDefault() {
        assertEquals(CarSession.defaultDensity(720), CarSession.resolveDensity(vehicleDpi = null, settingDpi = 0, headUnitDensity = 0, shortSide = 720))
        assertEquals(CarSession.defaultDensity(1440), CarSession.resolveDensity(vehicleDpi = null, settingDpi = 0, headUnitDensity = 0, shortSide = 1440))
    }

    /** MyPlay (D3-1): an imported density out of range falls through to the head unit's. */
    @Test
    fun aSettingOutOfRangeIsIgnored() {
        for (bad in listOf(5, 79, 641, 20_000)) {
            assertEquals(240, CarSession.resolveDensity(vehicleDpi = null, settingDpi = bad, headUnitDensity = 240, shortSide = 720))
        }
        assertEquals(80, CarSession.resolveDensity(vehicleDpi = null, settingDpi = 80, headUnitDensity = 240, shortSide = 720))
        assertEquals(640, CarSession.resolveDensity(vehicleDpi = null, settingDpi = 640, headUnitDensity = 240, shortSide = 720))
    }
}
