package net.prototux.aare.car

import net.prototux.aare.MyPlayBackupKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** MyPlay (#5): the saved places and the Navigate list. */
@RunWith(RobolectricTestRunner::class)
class PlacesTest {
    @Test fun roundTripAndCleaning() {
        val list = listOf(SavedPlace(" Work ", " Gedimino pr. 1, Vilnius "), SavedPlace("Empty", "  "), SavedPlace("", "54.68,25.28"))
        assertEquals(listOf(SavedPlace("Work", "Gedimino pr. 1, Vilnius"), SavedPlace("", "54.68,25.28")), Places.decode(Places.encode(list)))
    }

    @Test fun atMostFive() {
        val many = (1..8).map { SavedPlace("P$it", "A$it") }
        assertEquals(Places.MAX, Places.decode(Places.encode(many)).size)
    }

    @Test fun badStoredTextIsNoPlaces() {
        assertTrue(Places.decode("not json").isEmpty())
        assertTrue(Places.decode(null).isEmpty())
    }

    @Test fun homeFirstThenThePlacesAndANamelessOneShowsItsAddress() {
        val d = Places.destinations(" Home street 1 ", listOf(SavedPlace("Work", "W 2"), SavedPlace("", "54.68,25.28")), "Home")
        assertEquals(listOf(SavedPlace("Home", "Home street 1"), SavedPlace("Work", "W 2"), SavedPlace("54.68,25.28", "54.68,25.28")), d)
        assertTrue(Places.destinations("", emptyList(), "Home").isEmpty())
    }

    @Test fun neverBackedUp() {
        assertFalse(KEY_PLACES in MyPlayBackupKeys.ALLOWED)
        assertFalse(KEY_PLACES in net.prototux.aare.SettingsBackup.ALLOWED)
    }
}
