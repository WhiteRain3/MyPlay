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
}
