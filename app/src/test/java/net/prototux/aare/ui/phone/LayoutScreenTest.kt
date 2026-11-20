package net.prototux.aare.ui.phone

import net.prototux.aare.ui.car.CarLayoutSpec.SpecCard
import org.junit.Assert.assertEquals
import org.junit.Test

class LayoutScreenTest {
    @Test
    fun aStoredChoiceShowsTheUsualPills() {
        assertEquals(HIDE_CHOICES, hideOptions(null))
        assertEquals(HIDE_CHOICES, hideOptions(15))
    }

    @Test
    fun aStoredTimeThatIsNotAChoiceGetsItsOwnPillInOrder() {
        assertEquals(listOf(null, 5, 15, 20, 30, 60), hideOptions(20))
        assertEquals(listOf(null, 5, 7, 15, 30, 60), hideOptions(7))
    }

    @Test
    fun everyChoiceIsInTheRangeTheLayoutAllows() {
        HIDE_CHOICES.filterNotNull().forEach { assert(it in 5..60) { it } }
    }

    @Test
    fun shownCardsComeFirstThenTheOthers() {
        assertEquals(
            listOf(SpecCard.MUSIC, SpecCard.NEXT_TURN, SpecCard.ARRIVAL, SpecCard.CLOCK_STATUS),
            specCardRows(listOf(SpecCard.MUSIC, SpecCard.NEXT_TURN)),
        )
    }

    @Test
    fun aCardIsAddedAtTheEndAndTheLastOneStays() {
        assertEquals(listOf(SpecCard.MUSIC, SpecCard.ARRIVAL), listOf(SpecCard.MUSIC).withSpecCard(SpecCard.ARRIVAL, true))
        assertEquals(listOf(SpecCard.MUSIC), listOf(SpecCard.MUSIC, SpecCard.ARRIVAL).withSpecCard(SpecCard.ARRIVAL, false))
        assertEquals(listOf(SpecCard.MUSIC), listOf(SpecCard.MUSIC).withSpecCard(SpecCard.MUSIC, false))
        assertEquals(listOf(SpecCard.MUSIC), listOf(SpecCard.MUSIC).withSpecCard(SpecCard.MUSIC, true))
    }
}
