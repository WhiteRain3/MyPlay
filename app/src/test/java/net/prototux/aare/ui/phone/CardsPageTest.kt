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

    @Test
    fun homeMapsOffersLastUsedEachMapsAppThenAlwaysAsk() {
        val apps = listOf("app.organicmaps" to "Organic Maps", "com.waze" to "Waze")
        assertEquals(
            listOf("last" to null, "app:app.organicmaps" to "Organic Maps", "app:com.waze" to "Waze", "ask" to null),
            homeMapsOptions("last", apps),
        )
    }

    @Test
    fun aChosenMapsAppNoLongerInstalledKeepsItsOwnChoice() {
        val options = homeMapsOptions("app:gone.maps", listOf("com.waze" to "Waze"))
        assertEquals(listOf("last", "app:com.waze", "app:gone.maps", "ask"), options.map { it.first })
        assertEquals(null, options[2].second)
    }
}
