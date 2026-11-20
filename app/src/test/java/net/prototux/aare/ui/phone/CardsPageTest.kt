package net.prototux.aare.ui.phone

import net.prototux.aare.ui.car.DashCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardsPageTest {
    @Test
    fun shownCardsComeFirstInTheirOrderThenTheHiddenOnes() {
        val order = listOf(DashCard.MEDIA, DashCard.MAP)
        assertEquals(listOf(DashCard.MEDIA, DashCard.MAP, DashCard.CALENDAR, DashCard.NOTIFICATION, DashCard.WEATHER), cardRows(order))
    }

    @Test
    fun everyCardHasExactlyOneRow() {
        assertEquals(DashCard.entries.toSet(), cardRows(DashCard.DEFAULT).toSet())
        assertEquals(DashCard.entries.size, cardRows(DashCard.DEFAULT).size)
    }

    @Test
    fun theLastShownCardCannotBeHidden() {
        assertFalse(canSwitchCard(listOf(DashCard.MAP), DashCard.MAP))
        assertTrue(canSwitchCard(listOf(DashCard.MAP), DashCard.WEATHER))
        assertTrue(canSwitchCard(listOf(DashCard.MAP, DashCard.MEDIA), DashCard.MAP))
    }
}
