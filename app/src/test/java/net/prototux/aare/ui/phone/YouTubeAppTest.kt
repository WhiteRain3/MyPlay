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
        // VA-4: a patched build is the one added and checked.
        assertEquals(setOf("app.morphe.android.youtube"), withYouTubeApp(emptySet(), "app.morphe.android.youtube"))
        assertTrue(youTubeAppOnCar(true, setOf("app.morphe.android.youtube"), "app.morphe.android.youtube"))
        assertFalse(youTubeAppOnCar(true, setOf(CarRuntime.YOUTUBE_PACKAGE), "app.morphe.android.youtube"))
    }
}
