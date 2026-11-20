package net.prototux.aare.ui.phone

import net.prototux.aare.ui.theme.CarAccents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ColoursPageTest {
    @Test
    fun everyAccentHasItsOwnDot() {
        assertEquals(CarAccents.all.size, accentDotColours().toSet().size)
    }

    @Test
    fun aTappedDotStoresThatAccentsId() {
        CarAccents.all.forEach { assertEquals(it.id, accentIdOf(it.dark.primary)) }
    }

    @Test
    fun theStoredAccentShowsAsItsDot() {
        assertEquals(CarAccents.AMBER.dark.primary, accentDotColour("amber"))
        assertEquals(0, selectedColourIndex(accentDotColours(), accentDotColour(CarAccents.all.first().id)))
    }

    @Test
    fun theThemesOwnAndThePhonesAccentHaveNoDot() {
        assertNull(accentDotColour(CarAccents.THEME))
        assertNull(accentDotColour(CarAccents.PHONE))
        assertNull(selectedColourIndex(accentDotColours(), accentDotColour(CarAccents.THEME)))
    }
}
