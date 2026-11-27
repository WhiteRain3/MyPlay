package net.prototux.aare.host

import net.prototux.aare.host.MyPlayAppList.Named
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MyPlayAppListTest {
    private val own = "io.github.whiterain3.myplay"
    private fun names(vararg e: Named) = MyPlayAppList.names(e.toList(), own)

    @Test fun googlesAndroidAutoIsLeftOut() {
        assertFalse(MyPlayAppList.canShow("com.google.android.projection.gearhead"))
        assertTrue(MyPlayAppList.canShow("app.organicmaps"))
    }

    @Test fun namesNobodyElseHasStay() =
        assertEquals(listOf("Organic Maps", "Waze"), names(Named("app.organicmaps", "Organic Maps", "Organic Maps"), Named("com.waze", "Waze", "Waze")))

    @Test fun clashingEntriesOfOtherAppsGetTheirAppsName() =
        assertEquals(
            listOf("Phone", "Google · Phone"),
            names(Named(own, "Phone", "MyPlay"), Named("com.google.android.dialer.car", "Phone", "Google")),
        )

    @Test fun clashesAreFoundWhateverTheCase() =
        assertEquals(
            listOf("Settings", "Pixel · SETTINGS"),
            names(Named(own, "Settings", "MyPlay"), Named("com.example.pixel", "SETTINGS", "Pixel")),
        )

    /** Two apps both called "Google": the app's name would add nothing, so both stay as they are. */
    @Test fun anAppNameThatSaysNothingNewIsNotAdded() =
        assertEquals(listOf("Google", "Google"), names(Named("com.google.a", "Google", "Google"), Named("com.google.b", "Google", "")))

    @Test fun theSameAppTwiceWithDifferentPagesIsToldApartByItsApp() =
        assertEquals(
            listOf("Spotify", "Fermata · Music", "Other · Music"),
            names(Named("com.spotify", "Spotify", "Spotify"), Named("me.aap.fermata", "Music", "Fermata"), Named("com.other", "Music", "Other")),
        )

    // VA-1: an app on the car screen as itself is not also listed as a media player.
    @Test fun aRealAppHidesItsMediaPlayer() {
        val real = setOf(MyPlayAppList.YOUTUBE)
        assertFalse(MyPlayAppList.mediaEntryShown(MyPlayAppList.YOUTUBE, real))
        assertTrue("YouTube Music stays a media app", MyPlayAppList.mediaEntryShown("com.google.android.apps.youtube.music", real))
        assertTrue("phone apps off: the media player is the only YouTube", MyPlayAppList.mediaEntryShown(MyPlayAppList.YOUTUBE, emptySet()))
    }

    // VA-2: the phone's YouTube app and MovieBox Pro (by name: its package varies) are the video apps put on the car.
    @Test fun theVideoAppsAreYouTubeAndMovieBox() {
        val installed = mapOf(
            "app.morphe.android.youtube" to "YouTube Morphe",
            MyPlayAppList.YOUTUBE to "YouTube",
            "com.google.android.apps.youtube.music" to "YouTube Music",
            "com.movieboxpro.android" to "MovieBox Pro",
            "org.example.box" to "Movie Box",
            "com.spotify.music" to "Spotify",
        )
        assertEquals(setOf("app.morphe.android.youtube", "com.movieboxpro.android", "org.example.box"),
            MyPlayAppList.videoDefaults("app.morphe.android.youtube", installed))
        assertEquals(emptySet<String>(), MyPlayAppList.videoDefaults(null, mapOf("com.spotify.music" to "Spotify")))
        assertTrue(MyPlayAppList.isMovieBox("MOVIEBOX PRO"))
        assertFalse(MyPlayAppList.isMovieBox("Movies & TV"))
    }

    // VA-4: the YouTube app is whatever opens YouTube links, a patched build first, or the owner's choice.
    @Test fun theYouTubeAppIsFoundNotFixed() {
        val morphe = "app.morphe.android.youtube"
        val chrome = "com.android.chrome"
        val installed = mapOf(morphe to "YouTube Morphe", MyPlayAppList.YOUTUBE to "YouTube", chrome to "Chrome")
        val handlers = listOf(MyPlayAppList.YOUTUBE, chrome, morphe)
        assertEquals("a patched build before Google's", morphe, MyPlayAppList.youTubeApp(handlers, setOf(chrome), null, installed))
        assertEquals("the owner's choice", MyPlayAppList.YOUTUBE, MyPlayAppList.youTubeApp(handlers, setOf(chrome), MyPlayAppList.YOUTUBE, installed))
        assertEquals("a choice no longer installed is passed over", morphe, MyPlayAppList.youTubeApp(handlers, setOf(chrome), "gone.app", installed))
        assertEquals("only Google's", MyPlayAppList.YOUTUBE, MyPlayAppList.youTubeApp(listOf(MyPlayAppList.YOUTUBE, chrome), setOf(chrome), null, installed))
        assertEquals("a browser is never it", null, MyPlayAppList.youTubeApp(listOf(chrome), setOf(chrome), null, mapOf(chrome to "Chrome")))
        assertEquals("a patched build that claims no links, by its name", morphe,
            MyPlayAppList.youTubeApp(emptyList(), emptySet(), null, mapOf(morphe to "YouTube Morphe", "com.google.android.apps.youtube.music" to "YouTube Music")))
    }

    // VA-18: the log says why YouTube is or isn't on the car.
    @Test fun thePhoneAppsLine() {
        val yt = "app.morphe.android.youtube"
        assertEquals("phone apps on the car: off, root/Shizuku no, 0 allowed (0 hidden); no YouTube app found",
            MyPlayAppList.phoneAppsLine(false, false, emptySet(), emptySet(), null))
        assertEquals("phone apps on the car: on, root/Shizuku yes, 2 allowed (0 hidden); YouTube app ($yt) allowed; " +
            "allowed [$yt, x]; on the car [x, $yt]; YouTube candidates [$yt]",
            MyPlayAppList.phoneAppsLine(true, true, setOf(yt, "x"), emptySet(), yt, listOf("x", yt), candidates = listOf(yt, yt)))
        assertTrue(MyPlayAppList.phoneAppsLine(true, true, setOf("x"), emptySet(), yt).contains("YouTube app ($yt) not allowed"))
        // VA-22: hidden (the owner hid the old media entry, the same package) and not installed say so.
        val hiddenLine = MyPlayAppList.phoneAppsLine(true, true, setOf(yt), setOf(yt), yt)
        assertTrue(hiddenLine.contains("YouTube app ($yt) hidden") && hiddenLine.contains("hidden [$yt]"))
        assertTrue(MyPlayAppList.phoneAppsLine(true, true, setOf(yt), emptySet(), yt, missing = setOf(yt)).contains("not installed [$yt]"))
    }

    // VA-17: the car names what went wrong with a start; all is well says nothing.
    @Test fun launchProblems() {
        val s = net.prototux.aare.launch.LaunchStatus
        assertEquals(net.prototux.aare.launch.LaunchStatus.Problem.ON_PHONE, s.problem(s.ON_PHONE))
        assertEquals(net.prototux.aare.launch.LaunchStatus.Problem.NO_DISPLAY, s.problem(s.NO_DISPLAY))
        assertEquals(net.prototux.aare.launch.LaunchStatus.Problem.NOT_CONNECTED, s.problem(s.NOT_CONNECTED))
        assertEquals(null, s.problem(s.ON_CAR))
        assertEquals(null, s.problem(s.LAUNCHED))
        assertEquals(null, s.problem(null))
    }

    // VA-20: a failure carries how it was tried and the concrete error, shown on the car.
    @Test fun launchDetails() {
        val s = net.prototux.aare.launch.LaunchStatus
        val st = s.with(s.LAUNCH_FAILED, "root", "SecurityException: not allowed\non display 7")
        assertEquals(net.prototux.aare.launch.LaunchStatus.Problem.LAUNCH_FAILED, s.problem(st))
        assertEquals("root: SecurityException: not allowed on display 7", s.detail(st))
        assertEquals(null, s.detail(s.ON_CAR))
        assertEquals(181 - 1, s.detail(s.with(s.NOT_CONNECTED, "root", "x".repeat(400)))!!.length)
    }
}
