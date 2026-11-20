package net.prototux.aare.ui.phone

import net.prototux.aare.Settings
import org.junit.Assert.assertEquals
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import net.prototux.aare.ui.car.CarLayoutSpec
import net.prototux.aare.ui.car.CarPart
import net.prototux.aare.ui.car.DashCard
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    private val bar = CarPart.Bar to Rect(0f, 0f, 100f, 543f)
    private val clock = CarPart.Clock to Rect(10f, 10f, 90f, 50f)
    private val wallpaper = CarPart.Wallpaper to Rect(100f, 0f, 901f, 543f)
    private val card = CarPart.DashboardCard(DashCard.MAP) to Rect(120f, 20f, 500f, 520f)

    @Test
    fun theSmallestPartUnderTheFingerWins() {
        val regions = listOf(bar, clock, wallpaper, card)
        assertEquals(CarPart.Clock, partAt(regions, Offset(50f, 30f))?.first)
        assertEquals(CarPart.Bar, partAt(regions, Offset(50f, 300f))?.first)
        assertEquals(CarPart.DashboardCard(DashCard.MAP), partAt(regions, Offset(300f, 300f))?.first)
        assertEquals(CarPart.Wallpaper, partAt(regions, Offset(700f, 300f))?.first)
    }

    @Test
    fun nothingIsHitOutsideEveryPartOrByAnEmptyOne() {
        assertNull(partAt(listOf(clock), Offset(500f, 500f)))
        assertNull(partAt(listOf(CarPart.Status to Rect(10f, 10f, 10f, 10f)), Offset(10f, 10f)))
        assertNull(partAt(emptyList(), Offset(1f, 1f)))
    }

    @Test
    fun eachPartOpensItsPage() {
        assertEquals(PicturePart.BAR, picturePartOf(CarPart.Bar))
        assertEquals(PicturePart.BAR, picturePartOf(CarPart.Clock))
        assertEquals(PicturePart.BAR, picturePartOf(CarPart.Status))
        assertEquals(PicturePart.APPS, picturePartOf(CarPart.AppsButton))
        assertEquals(PicturePart.WALLPAPER, picturePartOf(CarPart.Wallpaper))
        assertEquals(PicturePart.CARDS, picturePartOf(CarPart.DashboardCard(DashCard.MEDIA)))
        assertEquals(PicturePart.CARDS, picturePartOf(CarPart.MapBehindCard(CarLayoutSpec.SpecCard.MUSIC)))
        assertEquals(PicturePart.WHOLE, picturePartOf(null))
    }
}
