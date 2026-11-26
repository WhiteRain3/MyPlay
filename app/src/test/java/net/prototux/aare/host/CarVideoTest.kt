package net.prototux.aare.host

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** VA-5, VA-6: which link a share carries, which app opens it on the car, and the playing video's link. */
class CarVideoTest {
    @Test fun theLinkInSharedText() {
        assertEquals("https://youtu.be/dQw4w9WgXcQ?si=abc", CarVideo.linkIn("Watch this! https://youtu.be/dQw4w9WgXcQ?si=abc"))
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", CarVideo.linkIn("(https://www.youtube.com/watch?v=dQw4w9WgXcQ)."))
        assertEquals("vnd.youtube:dQw4w9WgXcQ", CarVideo.linkIn("vnd.youtube:dQw4w9WgXcQ"))
        assertNull(CarVideo.linkIn("no link here"))
        assertNull(CarVideo.linkIn(null))
    }

    @Test fun youTubeLinks() {
        assertTrue(CarVideo.isYouTube("https://youtu.be/x"))
        assertTrue(CarVideo.isYouTube("https://m.youtube.com/watch?v=x"))
        assertTrue(CarVideo.isYouTube("vnd.youtube:x"))
        assertFalse(CarVideo.isYouTube("https://notyoutube.com/watch?v=x"))
        assertFalse(CarVideo.isYouTube("https://example.com/youtube.com"))
    }

    @Test fun theAppThatOpensIt() {
        val morphe = "app.morphe.android.youtube"
        val box = "com.movieboxpro.android"
        val allowed = setOf(morphe, box)
        assertEquals(morphe, CarVideo.appFor("https://youtu.be/x", morphe, allowed, listOf("com.google.android.youtube", morphe)))
        assertNull("the YouTube app is not allowed on the car", CarVideo.appFor("https://youtu.be/x", "com.google.android.youtube", allowed, emptyList()))
        assertEquals(box, CarVideo.appFor("https://moviebox.example/v/1", morphe, allowed, listOf("com.android.chrome", box)))
        assertNull("only a browser opens it", CarVideo.appFor("https://example.com", morphe, allowed, listOf("com.android.chrome")))
    }

    @Test fun thePlayingVideosLink() {
        assertEquals("https://youtu.be/dQw4w9WgXcQ", CarVideo.linkForMediaId("dQw4w9WgXcQ"))
        assertEquals("https://youtu.be/abc", CarVideo.linkForMediaId("https://youtu.be/abc"))
        assertNull("not a video id", CarVideo.linkForMediaId("root/recent/42"))
        assertNull(CarVideo.linkForMediaId(null))
        assertNull(CarVideo.linkForMediaId(" "))
    }
}
