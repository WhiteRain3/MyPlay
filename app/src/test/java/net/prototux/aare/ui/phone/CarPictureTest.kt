package net.prototux.aare.ui.phone

import net.prototux.aare.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarPictureTest {
    @Test
    fun thePictureIsDarkUnlessTheCarScreenIsSetToLight() {
        assertTrue(pictureNight(Settings.THEME_DARK))
        assertTrue(pictureNight(Settings.THEME_AUTO))
        assertFalse(pictureNight(Settings.THEME_LIGHT))
    }

    @Test
    fun theBarIsOnTheRightOnlyWhenSetSo() {
        assertTrue(pictureRailOnRight(Settings.RAIL_RIGHT))
        assertFalse(pictureRailOnRight(Settings.RAIL_LEFT))
        assertFalse(pictureRailOnRight(Settings.RAIL_AUTO))
        assertFalse(pictureRailOnRight(Settings.RAIL_BOTTOM))
    }

    @Test
    fun thePictureHasTheAudisShape() {
        // 1200 x 723 pixels as Google's Android Auto reported the Audi's screen.
        assertEquals(1200f / 723f, CarPictureSize.ASPECT, 0.01f)
    }
}
