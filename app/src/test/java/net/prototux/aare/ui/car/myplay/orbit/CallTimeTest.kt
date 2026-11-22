package net.prototux.aare.ui.car.myplay.orbit

import org.junit.Assert.assertEquals
import org.junit.Test

/** ORBIT polish: the in-call strip's time. */
class CallTimeTest {
    @Test fun minutesThenHours() {
        assertEquals("0:00", OrbitSkin.callTime(0))
        assertEquals("0:07", OrbitSkin.callTime(7))
        assertEquals("12:34", OrbitSkin.callTime(12 * 60 + 34))
        assertEquals("59:59", OrbitSkin.callTime(3599))
        assertEquals("1:00:00", OrbitSkin.callTime(3600))
        assertEquals("1:05:03", OrbitSkin.callTime(3600 + 5 * 60 + 3))
        assertEquals("0:00", OrbitSkin.callTime(-5))
    }
}
