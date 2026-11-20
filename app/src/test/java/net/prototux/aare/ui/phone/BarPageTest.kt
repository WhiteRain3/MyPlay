package net.prototux.aare.ui.phone

import net.prototux.aare.ui.car.RailWidget
import org.junit.Assert.assertEquals
import org.junit.Test

class BarPageTest {
    @Test
    fun shownItemsComeFirstInTheirOrderThenTheOthers() {
        assertEquals(
            listOf(RailWidget.WEATHER, RailWidget.CLOCK, RailWidget.TEMPERATURE, RailWidget.BATTERY),
            barItemRows(listOf(RailWidget.WEATHER, RailWidget.CLOCK)),
        )
    }

    @Test
    fun everyItemHasOneRowEvenWithNoneShown() {
        assertEquals(RailWidget.entries, barItemRows(emptyList()))
    }
}
