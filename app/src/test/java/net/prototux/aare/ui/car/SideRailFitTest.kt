package net.prototux.aare.ui.car

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class SideRailFitTest {
    @Test
    fun tallRailShowsEverythingAndARecentApp() {
        assertEquals(SideRailFit(recentSlots = 1, status = true, settings = true), sideRailFit(768.dp))
    }

    @Test
    fun mediumRailKeepsEverythingButTheRecentApp() {
        assertEquals(SideRailFit(recentSlots = 0, status = true, settings = true), sideRailFit(700.dp))
    }

    /** 1280x720 at 213 dpi: about 540 dp tall, 508 inside the padding (issue #68's size class). */
    @Test
    fun shortRailDropsStatusThenSettings() {
        assertEquals(SideRailFit(recentSlots = 0, status = false, settings = true), sideRailFit(600.dp))
        assertEquals(SideRailFit(recentSlots = 0, status = false, settings = false), sideRailFit(508.dp))
        assertEquals(SideRailFit(recentSlots = 0, status = false, settings = false), sideRailFit(300.dp))
    }

    /** MyPlay (F4-2): a larger clock and status icons need more height, so items go sooner. */
    @Test
    fun largerStatusAndClockLeaveLessRoom() {
        val larger = StatusStyle(size = StatusSize.LARGER, clockSize = StatusSize.LARGER)
        // 768 dp fits the recent app at the normal sizes; 1.5x takes about 42 dp more.
        assertEquals(SideRailFit(recentSlots = 0, status = true, settings = true), sideRailFit(768.dp, larger))
        assertEquals(SideRailFit(recentSlots = 1, status = true, settings = true), sideRailFit(810.dp, larger))
        assertEquals(SideRailFit(recentSlots = 0, status = false, settings = true), sideRailFit(640.dp, larger))
    }

    /** F4-6: smaller sizes keep the normal thresholds. */
    @Test
    fun smallerSizesKeepTheNormalThresholds() {
        val small = StatusStyle(size = StatusSize.SMALL, clockSize = StatusSize.SMALL)
        for (h in listOf(300, 508, 600, 700, 767, 768)) assertEquals(sideRailFit(h.dp), sideRailFit(h.dp, small))
    }
}
