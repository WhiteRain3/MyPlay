package net.prototux.aare.ui.phone

import net.prototux.aare.ui.car.CarLayoutSpec.SpecCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LayoutScreenTest {
    @Test
    fun neverStaysNever() {
        assertNull(hideChoiceOf(null))
    }

    @Test
    fun aStoredTimeShowsAsTheNearestChoice() {
        assertEquals(15, hideChoiceOf(15))
        assertEquals(5, hideChoiceOf(7))
        assertEquals(30, hideChoiceOf(25))
        assertEquals(60, hideChoiceOf(60))
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
