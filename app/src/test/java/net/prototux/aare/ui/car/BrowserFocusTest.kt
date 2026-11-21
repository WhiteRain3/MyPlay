package net.prototux.aare.ui.car

import net.prototux.aare.host.CarRuntime
import org.junit.Assert.assertTrue
import org.junit.Test

/** MyPlay (the owner: the knob's place on a page, YouTube by the knob). */
class BrowserFocusTest {
    @Test fun everyPageGetsTheKnobsOutlineOnce() {
        val js = BrowserFocus.OUTLINE_JS
        assertTrue(js.contains(":focus") && js.contains(BrowserFocus.COLOUR) && js.contains("!important"))
        // Added once per page: a second run finds it and stops.
        assertTrue(js.contains("getElementById('myplay-focus')") && js.contains("s.id = 'myplay-focus'"))
    }

    @Test fun youtubeAsksForItsTvInterface() {
        assertTrue(CarRuntime.YOUTUBE_URL.endsWith("/tv"))
        // YouTube serves its TV interface to TV browsers (Cobalt is YouTube's own TV engine).
        assertTrue(BrowserFocus.TV_USER_AGENT.contains("Cobalt") && BrowserFocus.TV_USER_AGENT.contains("TV"))
    }
}
