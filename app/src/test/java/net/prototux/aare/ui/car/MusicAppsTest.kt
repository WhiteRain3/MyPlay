package net.prototux.aare.ui.car

import org.junit.Assert.assertEquals
import org.junit.Test

/** MyPlay (K10): Music with no music app remembered lists music apps only. */
class MusicAppsTest {
    private fun entry(id: String, kind: CarAppKind) = CarAppEntry(id, "pkg.$id", id, null, kind)
    private val apps = listOf(
        entry("maps", CarAppKind.NAVIGATION),
        entry("spotify", CarAppKind.MEDIA),
        entry("phone", CarAppKind.PHONE),
        entry("radio", CarAppKind.MEDIA),
    )

    @Test fun noFilterShowsEverything() = assertEquals(apps, apps.onlyKinds(null))

    @Test fun musicOnlyShowsMusicApps() =
        assertEquals(listOf("spotify", "radio"), apps.onlyKinds(setOf(CarAppKind.MEDIA)).map { it.id })

    @Test fun noMusicAppsAtAllShowsEverythingRatherThanNothing() =
        assertEquals(apps.filter { it.kind != CarAppKind.MEDIA }, apps.filter { it.kind != CarAppKind.MEDIA }.onlyKinds(setOf(CarAppKind.MEDIA)))

    @Test fun theFilterHoldsOnlyWhileTheGridIsOpen() {
        val ui = CarUiState()
        ui.apps.addAll(apps)
        ui.launcherKinds = setOf(CarAppKind.MEDIA)
        ui.screen = CarScreen.Launcher
        assertEquals(listOf("spotify", "radio"), ui.launcherApps.map { it.id })
        // The grid drawn as the home screen (screen = Dashboard) shows everything.
        ui.screen = CarScreen.Dashboard
        assertEquals(apps.map { it.id }, ui.launcherApps.map { it.id })
    }
}
