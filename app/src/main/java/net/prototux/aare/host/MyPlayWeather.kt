package net.prototux.aare.host

import net.prototux.aare.ui.car.CarLayout
import net.prototux.aare.ui.car.CarLayoutSpec
import net.prototux.aare.ui.car.DashCard
import net.prototux.aare.ui.car.RailWidget

/**
 * MyPlay (R1): whether anything on the car screen as it is laid out now shows the weather, so the
 * weather (an approximate position sent to Open-Meteo) is only fetched when it is really used,
 * and not, for example, for a taskbar weather card chosen earlier while another layout is in use.
 */
internal object MyPlayWeather {
    /**
     * [showWeather] is the weather switch (the bar's weather items only show with it on);
     * [carHasTemperature]: the car sends its own outside temperature, so the temperature item
     * does not need the weather.
     */
    fun wanted(
        layout: CarLayout,
        showWeather: Boolean,
        railWidgets: List<RailWidget>,
        dashboardCards: List<DashCard>,
        spec: CarLayoutSpec,
        carHasTemperature: Boolean,
        /** MyPlay's own car screen (ORBIT, DECK) is shown instead of [layout]: it shows no weather. */
        engine: Boolean = false,
    ): Boolean {
        // The owner's privacy (the dispatcher): MyPlay's own screen draws no weather, so with it the
        // phone's position is never sent for one, whatever the older layout's choices were.
        if (engine) return false
        val barUsesIt = showWeather &&
            (RailWidget.WEATHER in railWidgets || (RailWidget.TEMPERATURE in railWidgets && !carHasTemperature))
        return when (layout) {
            CarLayout.TASKBAR -> barUsesIt || DashCard.WEATHER in dashboardCards
            CarLayout.DOCK -> barUsesIt
            CarLayout.MAP_BEHIND -> barUsesIt || (showWeather && CarLayoutSpec.SpecCard.CLOCK_STATUS in spec.allowed().cards)
            // The classic layout's top line has the clock and the phone's status, no weather.
            CarLayout.CLASSIC -> false
        }
    }
}
