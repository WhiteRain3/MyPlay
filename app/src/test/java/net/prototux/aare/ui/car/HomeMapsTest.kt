package net.prototux.aare.ui.car

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** MyPlay (K9). */
class HomeMapsTest {
    private fun nav(id: String, pkg: String) = CarAppEntry(id, pkg, id, null, CarAppKind.NAVIGATION)
    private val apps = listOf(nav("organic", "app.organicmaps"), nav("fermata", "me.aap.fermata"), CarAppEntry("music", "m", "Music", null, CarAppKind.MEDIA))

    @Test fun lastUsedIsAsBefore() = assertEquals("fermata", HomeMaps.appId(HomeMaps.LAST, "fermata", apps, emptySet()))

    @Test fun aChosenAppWinsOverTheLastUsed() =
        assertEquals("organic", HomeMaps.appId(HomeMaps.chosen("app.organicmaps"), "fermata", apps, emptySet()))

    @Test fun aChosenAppThatIsNotInstalledGivesTheButtons() =
        assertNull(HomeMaps.appId(HomeMaps.chosen("com.waze"), "fermata", apps, emptySet()))

    @Test fun onlyMapsAppsCanBeChosen() = assertNull(HomeMaps.appId(HomeMaps.chosen("m"), null, apps, emptySet()))

    @Test fun askAlwaysGivesTheButtons() = assertNull(HomeMaps.appId(HomeMaps.ASK, "fermata", apps, emptySet()))

    @Test fun anAppThatShowedNothingGivesTheButtons() {
        assertNull(HomeMaps.appId(HomeMaps.LAST, "fermata", apps, setOf("fermata")))
        assertNull(HomeMaps.appId(HomeMaps.chosen("me.aap.fermata"), null, apps, setOf("fermata")))
    }

    @Test fun anUnknownSettingIsTreatedAsLastUsed() = assertEquals("fermata", HomeMaps.appId("whatever", "fermata", apps, emptySet()))
}
