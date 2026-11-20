package net.prototux.aare.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** MyPlay (map recolouring, option 1). */
class MapColourFilterTest {
    private fun channels(rgb: Int) = listOf((rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF)
    private fun near(want: Int, got: Int, by: Int = 2) =
        assertTrue("want %06x got %06x".format(want, got), channels(want).zip(channels(got)).all { (a, b) -> abs(a - b) <= by })

    @Test fun noFilterIsNoMatrix() {
        assertNull(MapColourFilter().matrix(night = false))
        assertNull(MapColourFilter().matrix(night = true))
        // Darker nights only: nothing by day.
        assertNull(MapColourFilter(nightDarker = 30).matrix(night = false))
        assertNotNull(MapColourFilter(nightDarker = 30).matrix(night = true))
    }

    @Test fun greyTakesTheColourOut() {
        val m = MapColourFilter(saturation = 0).matrix(false)!!
        val (r, g, b) = channels(MapColourFilter.apply(m, 0xFF0000))
        assertEquals(r, g); assertEquals(g, b)
        // Grey stays grey.
        near(0x808080, MapColourFilter.apply(m, 0x808080))
    }

    @Test fun aHueShiftOfZeroOr360DegreesLeavesColours() {
        near(0x3366CC, MapColourFilter.apply(MapColourFilter(hueShift = 180).let { MapColourFilter.concat(it.matrix(false)!!, it.matrix(false)!!) }, 0x3366CC), by = 6)
    }

    @Test fun aHueShiftKeepsGrey() {
        near(0x777777, MapColourFilter.apply(MapColourFilter(hueShift = 90).matrix(false)!!, 0x777777))
    }

    @Test fun contrastPivotsOnMiddleGrey() {
        val m = MapColourFilter(contrast = 200).matrix(false)!!
        near(0x808080, MapColourFilter.apply(m, 0x808080), by = 1)
        near(0x000000, MapColourFilter.apply(m, 0x404040))
        near(0xFFFFFF, MapColourFilter.apply(m, 0xC0C0C0))
    }

    @Test fun brightnessAddsLight() {
        near(0x4C4C4C, MapColourFilter.apply(MapColourFilter(brightness = 30).matrix(false)!!, 0x000000))
    }

    @Test fun darkerNightsScaleTowardsBlack() {
        val m = MapColourFilter(nightDarker = 50).matrix(true)!!
        near(0x808080, MapColourFilter.apply(m, 0xFFFFFF))
        near(0x000000, MapColourFilter.apply(m, 0x000000))
    }

    @Test fun aFullTintOfMiddleGreyKeepsAGreyPictureGrey() {
        val m = MapColourFilter(tint = 0x808080, tintStrength = 100).matrix(false)!!
        near(0xFFFFFF, MapColourFilter.apply(m, 0xFFFFFF), by = 3)
        near(0x404040, MapColourFilter.apply(m, 0x404040), by = 3)
    }

    @Test fun aTintPullsTowardsItsColour() {
        val (r, g, b) = channels(MapColourFilter.apply(MapColourFilter(tint = 0x0000FF, tintStrength = 50).matrix(false)!!, 0xC0C0C0))
        assertTrue("blue grows against red and green: $r $g $b", b > r && b > g)
    }

    @Test fun alphaIsNeverChanged() {
        for (f in MapColourFilter.PRESETS.values) {
            val m = f.matrix(true) ?: continue
            assertEquals(listOf(0f, 0f, 0f, 1f, 0f), m.drop(15).toList())
        }
    }

    @Test fun roundTripAndJunk() {
        val f = MapColourFilter(hueShift = -20, saturation = 70, brightness = -10, contrast = 120, tint = 0x2a4a7a, tintStrength = 15, nightDarker = 40)
        assertEquals(f, MapColourFilter.decode(MapColourFilter.encode(f)))
        assertEquals(f, MapColourFilter.decode(MapColourFilter.encode(f) + ",nope:3,hue,sat:x"))
        assertEquals(MapColourFilter(saturation = 200, nightDarker = 90), MapColourFilter.decode("sat:999,night:100"))
        assertEquals(MapColourFilter.PRESETS["muted"], MapColourFilter.decode("muted"))
        assertEquals(MapColourFilter(), MapColourFilter.decode(null))
    }

    @Test fun perLookWithAFallbackForEveryOther() {
        var all = MapColourFilter.withLook("", "*", MapColourFilter(saturation = 50))
        all = MapColourFilter.withLook(all, "aare", MapColourFilter(hueShift = 30))
        assertEquals(MapColourFilter(hueShift = 30), MapColourFilter.forLook(all, "aare"))
        assertEquals(MapColourFilter(saturation = 50), MapColourFilter.forLook(all, "android_auto_2019"))
        // Setting a look back to nothing removes its entry, so the fallback applies again.
        all = MapColourFilter.withLook(all, "aare", MapColourFilter())
        assertEquals(MapColourFilter(saturation = 50), MapColourFilter.forLook(all, "aare"))
        assertEquals(MapColourFilter(), MapColourFilter.forLook("", "aare"))
    }
}
