package net.prototux.aare.ui.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ColourMathTest {
    @Test
    fun hsvRoundTripsForEveryHue() {
        listOf(0xFFE10600, 0xFF6EE7A8, 0xFF4285F4, 0xFFFFFFFF, 0xFF000000, 0xFF808080).map { it.toInt() }.forEach { c ->
            val hsv = ColourMath.colorToHsv(c)
            val back = ColourMath.hsvToColor(hsv[0], hsv[1], hsv[2])
            listOf(ColourMath::red, ColourMath::green, ColourMath::blue).forEach { ch -> assert(kotlin.math.abs(ch(c) - ch(back)) <= 1) { "%08X".format(c) } }
        }
    }

    @Test
    fun hexIsSixDigitsAndAlwaysOpaque() {
        assertEquals(0xFF6EE7A8.toInt(), ColourMath.parseHex("#6ee7a8"))
        assertEquals("6EE7A8", ColourMath.toHex(0xFF6EE7A8.toInt()))
        assertNull(ColourMath.parseHex("80FF0000"))
        assertNull(ColourMath.parseHex("GG0000"))
        assertNull(ColourMath.parseHex(""))
    }
}
