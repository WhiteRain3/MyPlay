package net.prototux.aare.ui.phone

import org.junit.Assert.assertEquals
import org.junit.Test

class CardSlotsTest {
    private val cards = listOf("turn", "arrival", "music")

    @Test
    fun aCardDroppedOnASlotTakesThatPlace() {
        assertEquals(listOf("music", "turn", "arrival"), dropCard(cards, "music", CardDrop.Slot(0)))
        assertEquals(listOf("arrival", "turn", "music"), dropCard(cards, "turn", CardDrop.Slot(1)))
    }

    @Test
    fun aHiddenCardDroppedOnASlotIsShownThere() {
        assertEquals(listOf("turn", "clock", "arrival", "music"), dropCard(cards, "clock", CardDrop.Slot(1)))
    }

    @Test
    fun aSlotPastTheEndMeansLast() {
        assertEquals(listOf("turn", "arrival", "music", "clock"), dropCard(cards, "clock", CardDrop.Slot(3)))
        assertEquals(listOf("arrival", "music", "turn"), dropCard(cards, "turn", CardDrop.Slot(3)))
    }

    @Test
    fun theTrayHidesACardButNotTheLastOne() {
        assertEquals(listOf("turn", "music"), dropCard(cards, "arrival", CardDrop.Tray))
        assertEquals(listOf("turn"), dropCard(listOf("turn"), "turn", CardDrop.Tray))
        assertEquals(cards, dropCard(cards, "clock", CardDrop.Tray))
    }

    @Test
    fun droppedNowhereNothingChanges() {
        assertEquals(cards, dropCard(cards, "turn", CardDrop.Nowhere))
    }
}
