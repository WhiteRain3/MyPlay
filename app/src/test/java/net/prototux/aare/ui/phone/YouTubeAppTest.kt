package net.prototux.aare.ui.phone

import net.prototux.aare.host.CarRuntime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeAppTest {
    @Test
    fun theYouTubeAppIsAddedBesideTheOthers() {
        assertEquals(setOf("org.example", CarRuntime.YOUTUBE_PACKAGE), withYouTubeApp(setOf("org.example")))
        assertEquals(setOf(CarRuntime.YOUTUBE_PACKAGE), withYouTubeApp(setOf(CarRuntime.YOUTUBE_PACKAGE)))
    }

    @Test
    fun itIsOnTheCarOnlyWithPhoneAppsOn() {
        assertTrue(youTubeAppOnCar(true, setOf(CarRuntime.YOUTUBE_PACKAGE)))
        assertFalse(youTubeAppOnCar(false, setOf(CarRuntime.YOUTUBE_PACKAGE)))
        assertFalse(youTubeAppOnCar(true, emptySet()))
    }
}
