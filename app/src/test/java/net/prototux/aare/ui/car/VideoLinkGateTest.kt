package net.prototux.aare.ui.car

import net.prototux.aare.host.CarVideo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** VA-9: a shared or Open-on-car link waits while driving, even for an app kept running, and opens once parked. */
class VideoLinkGateTest {
    private val link = CarVideo.Link("app.morphe.android.youtube", "https://youtu.be/dQw4w9WgXcQ")

    @Test fun aKeptAppWaitsWhileDrivingThenOpensParked() {
        assertFalse("driving, the app kept and running", VideoLinkGate.opensNow(link, link.packageName, started = true, driving = true))
        assertTrue("parked", VideoLinkGate.opensNow(link, link.packageName, started = true, driving = false))
    }

    @Test fun onlyItsOwnAppOnceRunning() {
        assertFalse("not started yet", VideoLinkGate.opensNow(link, link.packageName, started = false, driving = false))
        assertFalse("another app's link", VideoLinkGate.opensNow(link, "com.movieboxpro.android", started = true, driving = false))
        assertFalse("just the app, no link", VideoLinkGate.opensNow(link.copy(link = null), link.packageName, started = true, driving = false))
        assertFalse(VideoLinkGate.opensNow(null, link.packageName, started = true, driving = false))
    }
}
