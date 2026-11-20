package net.prototux.aare.ui.car

import org.junit.Assert.assertEquals
import org.junit.Test

class MapsButtonsFitTest {
    private fun fit(widths: List<Int>, available: Int) = mapsButtonsThatFit(widths, moreWidth = 80, spacing = 10, available = available)

    @Test fun noAppsNoButtons() = assertEquals(0, fit(emptyList(), 500))

    @Test fun allAreShownWhenTheyFit() {
        assertEquals(3, fit(listOf(100, 100, 100), 320))
        assertEquals(3, fit(listOf(100, 100, 100), 1000))
    }

    @Test fun allFitNeedsNoRoomForMore() = assertEquals(2, fit(listOf(150, 150), 310))

    @Test fun whenSomeAreLeftOutThereIsRoomForMore() {
        // 3 x 100 + 2 x 10 = 320 does not fit in 319; 100 + 10 + 100 + 10 + 80 = 300 does.
        assertEquals(2, fit(listOf(100, 100, 100), 319))
        // Two and More need 300; in 299 only one and More.
        assertEquals(1, fit(listOf(100, 100, 100), 299))
    }

    @Test fun neverFewerThanOne() {
        assertEquals(1, fit(listOf(400, 100), 200))
        assertEquals(1, fit(listOf(100, 100), 50))
    }

    @Test fun aSingleAppIsShownEvenWhenTooWide() = assertEquals(1, fit(listOf(900), 200))

    @Test fun whatIsShownWithMoreFitsTheRow() {
        val widths = listOf(120, 90, 200, 60, 150)
        for (available in 300..800) {
            val n = fit(widths, available)
            val used = widths.take(n).sum() + 10 * (n - 1) + if (n < widths.size) 10 + 80 else 0
            if (n > 1) assertEquals("at $available", true, used <= available)
        }
    }
}
