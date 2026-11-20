package net.prototux.aare.ui.phone

import org.junit.Assert.assertEquals
import org.junit.Test

class SizePageTest {
    @Test
    fun autoComesFirstAndTheAudisDensityIsOffered() {
        assertEquals(0, DENSITY_CHOICES.first())
        assert(213 in DENSITY_CHOICES)
    }

    @Test
    fun aStoredDensityOutsideTheChoicesGetsItsOwnPill() {
        assertEquals(DENSITY_CHOICES, densityOptions(160))
        assertEquals(listOf(0, 120, 160, 180, 200, 213, 240), densityOptions(180))
    }

    @Test
    fun thePictureIsLaidOutAtTheChosenDensity() {
        assertEquals(CarPictureSize.layoutSizeDp(213), CarPictureSize.layoutSizeDp(0))
        val (w, h) = CarPictureSize.layoutSizeDp(160)
        assertEquals(1200f, w, 0.01f)
        assertEquals(723f, h, 0.01f)
        val (w2, _) = CarPictureSize.layoutSizeDp(240)
        assertEquals(800f, w2, 0.01f)
    }

    @Test
    fun theAutoSizeMatchesThePicturesShape() {
        val (w, h) = CarPictureSize.layoutSizeDp(0)
        assertEquals(CarPictureSize.WIDTH_DP, w, 1f)
        assertEquals(CarPictureSize.HEIGHT_DP, h, 1f)
    }
}
