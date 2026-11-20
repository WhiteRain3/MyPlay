package net.prototux.aare.ui.car

import org.junit.Assert.assertEquals
import org.junit.Test

/** MyPlay (P2-1). */
class SpecBarItemsTest {
    @Test fun withTheClockCardTheBarLeavesOutWhatTheCardShows() {
        val all = listOf(RailWidget.CLOCK, RailWidget.TEMPERATURE, RailWidget.BATTERY, RailWidget.WEATHER)
        assertEquals(listOf(RailWidget.TEMPERATURE), barItems(all, cardHasStatus = true))
        assertEquals(all, barItems(all, cardHasStatus = false))
        assertEquals(emptyList<RailWidget>(), barItems(RailWidget.DEFAULT, cardHasStatus = true))
    }
}
