package net.prototux.aare.ui.car

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CarPartsTest {
    @Test fun partsAreListedWithTheirPlaces() {
        val reg = CarPartRegistry()
        reg.put("bar", CarPart.Bar, Rect(0f, 0f, 96f, 720f))
        reg.put("music", CarPart.MapBehindCard(CarLayoutSpec.SpecCard.MUSIC), Rect(112f, 16f, 472f, 216f))
        assertEquals(2, reg.regions.size)
        assertTrue(CarPart.Bar to Rect(0f, 0f, 96f, 720f) in reg.regions)
    }

    @Test fun aPartDrawnAgainMovesAndOneGoneIsForgotten() {
        val reg = CarPartRegistry()
        reg.put("clock", CarPart.Clock, Rect(0f, 0f, 10f, 10f))
        reg.put("clock", CarPart.Clock, Rect(5f, 5f, 15f, 15f))
        assertEquals(listOf(CarPart.Clock to Rect(5f, 5f, 15f, 15f)), reg.regions)
        reg.remove("clock")
        assertTrue(reg.regions.isEmpty())
    }

    /** The same part in two places (a clock in the bar and in a card) is listed twice. */
    @Test fun thePartInTwoPlacesIsListedTwice() {
        val reg = CarPartRegistry()
        reg.put(Any(), CarPart.Clock, Rect(0f, 0f, 10f, 10f))
        reg.put(Any(), CarPart.Clock, Rect(20f, 0f, 30f, 10f))
        assertEquals(2, reg.regions.count { it.first == CarPart.Clock })
    }
}
