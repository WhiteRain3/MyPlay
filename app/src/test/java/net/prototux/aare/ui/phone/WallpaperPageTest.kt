package net.prototux.aare.ui.phone

import net.prototux.aare.Settings
import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperPageTest {
    @Test
    fun noneComesFirstAndThePhotoLast() {
        assertEquals(listOf("none", "dusk", "ocean", Settings.WALLPAPER_CUSTOM), wallpaperChoices(listOf("dusk", "ocean")))
    }

    @Test
    fun everyBuiltInWallpaperIsOffered() {
        val choices = wallpaperChoices()
        net.prototux.aare.ui.car.WALLPAPER_NAMES.forEach { assert(it in choices) { it } }
        assertEquals(choices.size, choices.toSet().size)
    }
}
