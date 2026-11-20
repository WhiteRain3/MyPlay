package net.prototux.aare.host

import net.prototux.aare.ui.car.CarLayout
import net.prototux.aare.ui.car.CarLayoutSpec
import net.prototux.aare.ui.car.DashCard
import net.prototux.aare.ui.car.RailWidget
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** MyPlay (R1). */
class MyPlayWeatherTest {
    private val spec = CarLayoutSpec.DEFAULT
    private val noWeatherCards = listOf(DashCard.MAP, DashCard.MEDIA)
    private fun wanted(
        layout: CarLayout, showWeather: Boolean = false, rail: List<RailWidget> = listOf(RailWidget.CLOCK),
        cards: List<DashCard> = noWeatherCards, s: CarLayoutSpec = spec, carTemp: Boolean = false,
    ) = MyPlayWeather.wanted(layout, showWeather, rail, cards, s, carTemp)

    @Test fun nothingUsesItNothingIsFetched() = CarLayout.entries.forEach { assertFalse("$it", wanted(it)) }

    @Test fun aTaskbarWeatherCardWantsIt() = assertTrue(wanted(CarLayout.TASKBAR, cards = listOf(DashCard.WEATHER)))

    /** A taskbar card chosen earlier does not count while another layout is in use. */
    @Test fun aTaskbarCardDoesNotCountInOtherLayouts() {
        for (l in listOf(CarLayout.DOCK, CarLayout.MAP_BEHIND, CarLayout.CLASSIC)) assertFalse("$l", wanted(l, cards = listOf(DashCard.WEATHER)))
    }

    @Test fun aWeatherBarItemWantsItOnlyWithTheSwitchOn() {
        val rail = listOf(RailWidget.CLOCK, RailWidget.WEATHER)
        for (l in listOf(CarLayout.TASKBAR, CarLayout.DOCK, CarLayout.MAP_BEHIND)) {
            assertTrue("$l", wanted(l, showWeather = true, rail = rail))
            assertFalse("$l", wanted(l, showWeather = false, rail = rail))
        }
        assertFalse(wanted(CarLayout.CLASSIC, showWeather = true, rail = rail))
    }

    @Test fun theTemperatureItemNeedsItOnlyWithoutACarSensor() {
        val rail = listOf(RailWidget.TEMPERATURE)
        assertTrue(wanted(CarLayout.TASKBAR, showWeather = true, rail = rail, carTemp = false))
        assertFalse(wanted(CarLayout.TASKBAR, showWeather = true, rail = rail, carTemp = true))
    }

    @Test fun mapBehindsClockCardWantsItWithTheSwitchOn() {
        val withClock = spec.copy(cards = listOf(CarLayoutSpec.SpecCard.CLOCK_STATUS))
        val without = spec.copy(cards = listOf(CarLayoutSpec.SpecCard.MUSIC))
        assertTrue(wanted(CarLayout.MAP_BEHIND, showWeather = true, s = withClock))
        assertFalse(wanted(CarLayout.MAP_BEHIND, showWeather = false, s = withClock))
        assertFalse(wanted(CarLayout.MAP_BEHIND, showWeather = true, s = without))
    }
}
