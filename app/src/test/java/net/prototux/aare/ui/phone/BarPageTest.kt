package net.prototux.aare.ui.phone

import net.prototux.aare.ui.car.RailWidget
import net.prototux.aare.ui.car.CarLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun theTaskbarUsesEverythingExceptTheMapBehindBar() {
        val c = barControlsFor(CarLayout.TASKBAR)
        assertTrue(c.taskbarSide && c.items && c.darkBar && c.weather)
        assertFalse(c.mapBehindBar)
    }

    @Test
    fun mapBehindEditsItsOwnBarItsItemsAndTheWeather() {
        val c = barControlsFor(CarLayout.MAP_BEHIND)
        assertTrue(c.mapBehindBar && c.weather && c.items)
        assertFalse(c.taskbarSide || c.darkBar)
    }

    @Test
    fun theDockShowsItemsAndWeatherAndTheAppGridNothing() {
        assertEquals(BarControls(false, true, false, true, false), barControlsFor(CarLayout.DOCK))
        assertEquals(BarControls(false, false, false, false, false), barControlsFor(CarLayout.CLASSIC))
    }
}
