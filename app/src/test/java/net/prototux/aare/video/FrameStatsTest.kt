package net.prototux.aare.video

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FrameStatsTest {
    @Before fun setUp() { FrameStats.setRefreshRate(60f); FrameStats.reset() }
    @After fun tearDown() { FrameStats.on = false; FrameStats.setRefreshRate(60f); FrameStats.reset() }

    private fun ms(n: Double) = (n * 1_000_000).toLong()

    @Test fun offCountsNothing() {
        FrameStats.on = false
        FrameStats.add(ms(50.0))
        assertNull(FrameStats.line())
    }

    @Test fun countsSlowAndJankFramesAt60Hz() {
        FrameStats.on = true
        listOf(8.0, 16.6, 16.9, 33.3, 33.4, 120.0).forEach { FrameStats.add(ms(it)) }
        assertEquals("frames at 60 Hz: 6 drawn, 4 over 16.7 ms, 2 over 33.3 ms, slowest 120.0 ms", FrameStats.line())
    }

    @Test fun thresholdsFollowTheRefreshRate() {
        FrameStats.on = true
        FrameStats.setRefreshRate(30f)
        listOf(20.0, 34.0, 70.0).forEach { FrameStats.add(ms(it)) }
        assertEquals("frames at 30 Hz: 3 drawn, 2 over 33.3 ms, 1 over 66.7 ms, slowest 70.0 ms", FrameStats.line())
    }

    @Test fun anOddRateKeeps60Hz() {
        FrameStats.on = true
        FrameStats.setRefreshRate(0f)
        FrameStats.add(ms(17.0))
        assertEquals("frames at 60 Hz: 1 drawn, 1 over 16.7 ms, 0 over 33.3 ms, slowest 17.0 ms", FrameStats.line())
    }

    @Test fun resetStartsAfresh() {
        FrameStats.on = true
        FrameStats.add(ms(40.0))
        FrameStats.reset()
        assertNull(FrameStats.line())
    }
}
