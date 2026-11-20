package net.prototux.aare.ui.theme

import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** MyPlay (F3). */
class MyPlayCustomColoursTest {
    private val c = CustomColours(
        night = mapOf(ColourRole.ACCENT to 0xFF6EE7A8.toInt(), ColourRole.CARD to 0xFF202430.toInt()),
        day = mapOf(ColourRole.TEXT to 0xFF101010.toInt()),
    )

    @Test fun theTextFormIsStableAndRoundTrips() {
        assertEquals("night.card=FF202430;night.accent=FF6EE7A8;day.text=FF101010", c.encode())
        assertEquals(c, CustomColours.decode(c.encode()))
    }

    @Test fun brokenPartsAreSkipped() {
        assertEquals(c, CustomColours.decode(c.encode() + ";noon.card=FF000000;night.sky=FF000000;day.card=12;day.bar;x=y;"))
        assertEquals(CustomColours.NONE, CustomColours.decode(null))
        assertEquals(CustomColours(night = mapOf(ColourRole.BAR to 0xFFABCDEF.toInt())), CustomColours.decode("night.bar=ffabcdef"))
    }

    @Test fun nothingSetLeavesTheLookAsItIs() {
        val look = CarThemes.byId("aare")
        assertSame(look, look.withCustomColours(CustomColours.NONE))
    }

    @Test fun theOwnersColoursPaintTheirParts() {
        val look = CarThemes.byId("aare").withCustomColours(c)
        assertEquals(0xFF6EE7A8.toInt(), look.night.primary.toArgb())
        assertEquals(0xFF202430.toInt(), look.night.surfaceContainer.toArgb())
        assertEquals(0xFF202430.toInt(), look.night.surfaceContainerHigh.toArgb())
        assertEquals(0xFF101010.toInt(), look.day.onSurface.toArgb())
        assertEquals(0xFF101010.toInt(), look.day.onBackground.toArgb())
        // Not set: the look's own.
        assertEquals(CarThemes.byId("aare").day.primary, look.day.primary)
        assertEquals(CarThemes.byId("aare").night.background, look.night.background)
    }

    @Test fun contrastFollowsWcag() {
        assertEquals(21.0, contrastRatio(0xFF000000.toInt(), 0xFFFFFFFF.toInt()), 0.01)
        assertEquals(1.0, contrastRatio(0xFF777777.toInt(), 0xFF777777.toInt()), 0.001)
        assertEquals(contrastRatio(0xFF000000.toInt(), 0xFF767676.toInt()), contrastRatio(0xFF767676.toInt(), 0xFF000000.toInt()), 1e-9)
        assertTrue(contrastRatio(0xFFFFFFFF.toInt(), 0xFF767676.toInt()) > 4.5)
    }

    /** F3-2: a see-through colour from a file is made opaque. */
    @Test fun coloursAreAlwaysOpaque() {
        assertEquals(CustomColours(night = mapOf(ColourRole.TEXT to 0xFF123456.toInt())), CustomColours.decode("night.text=00123456"))
    }
}
