package net.prototux.aare.ui.phone

import androidx.compose.ui.graphics.Color
import net.prototux.aare.ui.car.StatusStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusIconsPageTest {
    @Test
    fun aTappedDotIsStoredAndShownAsTheSameDot() {
        STATUS_COLOURS.forEachIndexed { i, c ->
            assertEquals(i, selectedColourIndex(STATUS_COLOURS, statusDotColour(statusArgbOf(c))))
        }
    }

    @Test
    fun noStoredColourMeansTheThemesOwn() {
        assertNull(statusDotColour(null))
        assertNull(selectedColourIndex(STATUS_COLOURS, statusDotColour(null)))
    }

    @Test
    fun aColourStoredElsewhereShowsNoDot() {
        assertNull(selectedColourIndex(STATUS_COLOURS, statusDotColour(Color(0xFF123456).let(::statusArgbOf))))
    }

    @Test
    fun everyDotHasItsOwnColour() {
        assertEquals(STATUS_COLOURS.size, STATUS_COLOURS.map(::statusArgbOf).toSet().size)
    }

    @Test
    fun theBandsFollowTheCarsLimits() {
        val (low, mid, high) = levelBands()
        assertEquals(StatusStyle.LOW / 100f, low)
        assertEquals(StatusStyle.MEDIUM / 100f, mid)
        assertEquals(1f, high)
        assertTrue(low < mid && mid < high)
    }
}
