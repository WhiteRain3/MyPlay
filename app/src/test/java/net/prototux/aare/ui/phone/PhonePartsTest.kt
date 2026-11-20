package net.prototux.aare.ui.phone

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhonePartsTest {
    @Test
    fun aShortValueIsShownAsItIs() {
        assertEquals("0.1.2", tileValueText("0.1.2"))
        assertEquals("Connected", tileValueText("Connected"))
    }

    @Test
    fun aMissingOrBlankValueShowsADash() {
        assertEquals("–", tileValueText(null))
        assertEquals("–", tileValueText("   "))
    }

    @Test
    fun spacesAndLineBreaksAreTidied() {
        assertEquals("2 on", tileValueText("  2\n  on "))
    }

    @Test
    fun aLongValueIsShortenedToTheLimitWithAnEllipsis() {
        val shown = tileValueText("A value much too long for a tile")
        assertEquals(TILE_VALUE_MAX, shown.length)
        assertTrue(shown.endsWith("…"))
        assertEquals("A value…", tileValueText("A value much", max = 9))
    }

    @Test
    fun aValueExactlyAtTheLimitIsNotShortened() {
        assertEquals("1234567890", tileValueText("1234567890"))
    }

    @Test
    fun theChosenColourDotIsFoundByItsValue() {
        val dots = listOf(Color.White, Color(0xFF6EE7A8), Color(0xFFFF5A5F))
        assertEquals(1, selectedColourIndex(dots, Color(0xFF6EE7A8)))
        assertEquals(0, selectedColourIndex(dots, Color(1f, 1f, 1f)))
    }

    @Test
    fun noDotIsChosenForAColourNotInTheRowOrForDefault() {
        val dots = listOf(Color.White, Color(0xFF6EE7A8))
        assertNull(selectedColourIndex(dots, Color(0xFF123456)))
        assertNull(selectedColourIndex(dots, null))
    }

    @Test
    fun everyTileToneHasLightTextOnADarkBackground() {
        TileTone.entries.forEach { assertTrue(it.name, it.content.luminanceApprox() > it.background.luminanceApprox() + 0.3f) }
    }

    private fun Color.luminanceApprox() = 0.2126f * red + 0.7152f * green + 0.0722f * blue
}
