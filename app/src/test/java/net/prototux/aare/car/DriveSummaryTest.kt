package net.prototux.aare.car

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** MyPlay (ideas 2026-11-22): the drive summary's line in the connection log. */
class DriveSummaryTest {
    private val car50 = SpeedReading(50, SpeedReading.From.CAR, LimitShown.Known(50))
    private val phone90 = SpeedReading(90, SpeedReading.From.PHONE, LimitShown.Known(90, defaulted = true))

    @Test fun nothingOnScreen() {
        assertEquals("drive summary: 12 min connected; the speed was not on the car screen", DriveTally().line(12 * 60_000L))
    }

    @Test fun sharesAndDistance() {
        val t = DriveTally()
        repeat(60) { t.add(car50, 1_000) } // 1 min at 50 km/h, signed limit
        repeat(20) { t.add(phone90, 1_000) } // 20 s at 90 km/h, limit by road type
        repeat(10) { t.add(SpeedReading(30, SpeedReading.From.CAR, LimitShown.Unknown), 1_000) }
        repeat(10) { t.add(SpeedReading(30, SpeedReading.From.CAR), 1_000) }
        repeat(25) { t.add(null, 1_000) }
        // 833 + 500 + 83 + 83 m
        assertEquals(
            "drive summary: 3 min connected; 1.5 km by the speed shown; speed known 80 % of the time it was on screen " +
                "(car 80 %, phone 20 %); limit: signed 60 %, by road type 20 %, not known 10 %, not looked up 10 %",
            t.line(3 * 60_000L),
        )
    }

    @Test fun neverTheTopSpeedOrTimeOverTheLimit() {
        val t = DriveTally()
        t.add(SpeedReading(150, SpeedReading.From.CAR, LimitShown.Known(90), over = true), 1_000)
        val line = t.line(60_000)
        assertFalse(line, "150" in line)
        assertFalse(line, "over" in line)
    }

    @Test fun gapsAreNotCountedAndOnlyInsideASession() {
        DriveSummary.end(0) // whatever an earlier test left
        DriveSummary.sample(car50, 1_000) // no session: ignored
        assertNull(DriveSummary.end(2_000))
        DriveSummary.begin(0)
        DriveSummary.sample(car50, 1_000)
        DriveSummary.sample(car50, 2_000) // 1 s counted
        DriveSummary.sample(car50, 60_000) // a 58 s gap: not counted
        DriveSummary.pause()
        DriveSummary.sample(car50, 61_000) // first after a pause: nothing counted
        DriveSummary.sample(car50, 62_000) // 1 s counted
        val line = DriveSummary.end(120_000)!!
        assertTrue(line, line.startsWith("drive summary: 2 min connected; 0.0 km"))
        assertTrue(line, "speed known 100 %" in line)
        assertNull(DriveSummary.end(130_000))
    }
}
