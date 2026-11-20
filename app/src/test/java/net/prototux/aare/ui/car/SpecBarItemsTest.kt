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

    /** P2-3: on the Audi's screen the fourth card (Clock and status) gets no slot, so the bar keeps the clock. */
    @Test fun aClockCardWithoutASlotDoesNotCount() {
        val d = CarLayoutSpec.PRESETS.getValue("D")
        val slots = SpecPlacement.place(d, 901f, 543f).slots.size
        assertEquals(false, clockCardOnScreen(d, slots, null))
        assertEquals(true, clockCardOnScreen(d, 4, null))
    }

    @Test fun onAStageOnlyTheCardShownNowCounts() {
        val spec = CarLayoutSpec.PRESETS.getValue("D").copy(cardPlace = CarLayoutSpec.CardPlace.STAGE, oneExtraAtATime = false)
        assertEquals(false, clockCardOnScreen(spec, 1, listOf(CarLayoutSpec.SpecCard.MUSIC, CarLayoutSpec.SpecCard.CLOCK_STATUS)))
        assertEquals(true, clockCardOnScreen(spec, 1, listOf(CarLayoutSpec.SpecCard.CLOCK_STATUS)))
    }
}
