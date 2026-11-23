package net.prototux.aare.video

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FrameStatsTest {
    @After fun tearDown() { FrameStats.on = false; FrameStats.reset() }

    private fun ms(n: Long) = n * 1_000_000

    @Test fun offCountsNothing() {
        FrameStats.on = false
        FrameStats.reset()
        FrameStats.add(ms(50))
        assertNull(FrameStats.line())
    }

    @Test fun countsSlowAndJankFrames() {
        FrameStats.on = true
        FrameStats.reset()
        listOf(8L, 16L, 17L, 33L, 34L, 120L).forEach { FrameStats.add(ms(it)) }
        assertEquals("frames: 6 drawn, 4 over 16 ms, 2 over 33 ms, slowest 120 ms", FrameStats.line())
    }

    @Test fun aFractionOverCounts() {
        FrameStats.on = true
        FrameStats.reset()
        FrameStats.add(16_900_000)
        assertEquals("frames: 1 drawn, 1 over 16 ms, 0 over 33 ms, slowest 17 ms", FrameStats.line())
    }

    @Test fun resetStartsAfresh() {
        FrameStats.on = true
        FrameStats.add(ms(40))
        FrameStats.reset()
        assertNull(FrameStats.line())
    }
}
