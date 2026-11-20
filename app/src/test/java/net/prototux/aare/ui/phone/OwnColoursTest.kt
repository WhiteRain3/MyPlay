package net.prototux.aare.ui.phone

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import net.prototux.aare.ui.theme.ColourRole
import net.prototux.aare.ui.theme.CustomColours
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnColoursTest {
    @Test
    fun settingAColourTouchesOnlyThatSide() {
        val c = CustomColours.NONE.with(night = true, role = ColourRole.ACCENT, argb = 0xFF112233.toInt())
        assertEquals(mapOf(ColourRole.ACCENT to 0xFF112233.toInt()), c.night)
        assertTrue(c.day.isEmpty())
        assertEquals("night.accent=FF112233", c.encode())
    }

    @Test
    fun aPickedColourIsAlwaysSolid() {
        val c = CustomColours.NONE.with(night = false, role = ColourRole.CARD, argb = 0x00123456)
        assertEquals(0xFF123456.toInt(), c.day[ColourRole.CARD])
    }

    @Test
    fun backToTheLooksOwnRemovesTheRoleAndNothingElse() {
        val c = CustomColours.decode("night.accent=FF112233;night.card=FF202430;day.card=FF202430")
            .with(night = true, role = ColourRole.CARD, argb = null)
        assertEquals("night.accent=FF112233;day.card=FF202430", c.encode())
        assertTrue(CustomColours.NONE.with(true, ColourRole.TEXT, 1).with(true, ColourRole.TEXT, null).isEmpty)
    }

    @Test
    fun eachRoleReadsTheColourTheCarPaintsWithIt() {
        val s = darkColorScheme(
            background = Color(0xFF000001), surfaceContainer = Color(0xFF000002), surfaceContainerLowest = Color(0xFF000003),
            primary = Color(0xFF000004), onPrimary = Color(0xFF000005), onSurface = Color(0xFF000006), onSurfaceVariant = Color(0xFF000007),
        )
        assertEquals((1..7).map { 0xFF000000.toInt() or it }, ColourRole.entries.map { roleColour(s, it) })
    }

    @Test
    fun everyPartOfThePictureOpensAColour() {
        assertEquals(ColourRole.BAR, colourRoleOf(PicturePart.BAR))
        assertEquals(ColourRole.CARD, colourRoleOf(PicturePart.CARDS))
        assertEquals(ColourRole.BACKGROUND, colourRoleOf(PicturePart.WALLPAPER))
        PicturePart.entries.forEach { colourRoleOf(it) }
    }

    @Test
    fun onlyPairsTheOwnerTouchedAreWarnedAbout() {
        val grey = 0xFF777777.toInt()
        // Everything the same grey: every pair reads badly.
        assertTrue(hardToRead({ grey }, emptySet()).isEmpty())
        assertEquals(
            listOf(ColourRole.ON_ACCENT to ColourRole.ACCENT),
            hardToRead({ grey }, setOf(ColourRole.ON_ACCENT)),
        )
        assertEquals(3, hardToRead({ grey }, setOf(ColourRole.TEXT)).size)
    }

    @Test
    fun goodContrastIsNotWarnedAbout() {
        val colourOf = { r: ColourRole -> if (r == ColourRole.TEXT || r == ColourRole.TEXT_DIM || r == ColourRole.ON_ACCENT) 0xFFFFFFFF.toInt() else 0xFF000000.toInt() }
        assertTrue(hardToRead(colourOf, ColourRole.entries.toSet()).isEmpty())
    }
}
