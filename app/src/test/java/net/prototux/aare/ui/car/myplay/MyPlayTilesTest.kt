package net.prototux.aare.ui.car.myplay

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import net.prototux.aare.ui.car.CarActions
import net.prototux.aare.ui.car.CarAppEntry
import net.prototux.aare.ui.car.CarAppKind
import net.prototux.aare.ui.car.CarLayout
import net.prototux.aare.ui.car.CarScreen
import net.prototux.aare.ui.car.CarUiState
import net.prototux.aare.ui.car.PadWalkStates
import net.prototux.aare.ui.theme.CarTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * MyPlay (F2, the owner's first drive): MyPlay's built-in screens in the Apps world open the
 * engine's own pages, never the old screens; other apps (the phone's screen and the browser among
 * them) open full screen, in the engine's frame (F1).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class MyPlayTilesTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val asked = mutableListOf<String>()
    private val actions = object : CarActions by PadWalkStates.NoActions {
        override fun openApp(app: CarAppEntry) { asked += "app:${app.id}" }
        override fun openLauncher() { asked += "launcher" }
    }

    private fun entry(id: String, kind: CarAppKind) = CarAppEntry(id, "mock.$id", id, null, kind)
    private val tiles = listOf(
        entry("car-settings", CarAppKind.SETTINGS), entry("car-data", CarAppKind.CAR_DATA), entry("phone", CarAppKind.PHONE),
        entry("notifications", CarAppKind.NOTIFICATIONS), entry("phone-screen", CarAppKind.PHONE_SCREEN), entry("browser", CarAppKind.BROWSER),
        entry("radio", CarAppKind.MEDIA),
    )

    private fun show(): CarUiState {
        val state = PadWalkStates.state(CarLayout.MAP_BEHIND, CarScreen.Dashboard).apply {
            enginePreview = true
            apps.clear()
            apps.addAll(tiles)
        }
        rule.setContent {
            CarTheme(night = state.night, theme = state.theme) {
                MyPlayEngineShell(state, actions, appContent = { _, m -> Box(m.background(Color.DarkGray)) },
                    navMap = { _, m -> Box(m.background(Color(0xFF2E4A3A))) }, overlay = {}, skin = TestSkin,
                    sources = EngineSources.of(state, actions, LocalContext.current))
            }
        }
        rule.waitForIdle()
        return state
    }

    private val engine get() = rule.onNodeWithTag(ENGINE_TAG)

    /** Home, then the Apps world; the dial opens on the world last entered ([from]). */
    private fun toApps(from: World = World.NAVIGATE) {
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        val steps = World.APPS.ordinal - from.ordinal
        repeat(kotlin.math.abs(steps)) { engine.performKeyInput { pressKey(if (steps > 0) Key.DirectionRight else Key.DirectionLeft) } }
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(SLOT_APPS).assertExists()
    }

    private fun tap(id: String) = rule.onNodeWithTag(appTag(tiles.single { it.id == id })).performClick()

    private fun back() {
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun builtInTilesOpenTheEnginesOwnPages() {
        show()
        toApps()
        // F2-1: Back from each page returns to the Apps world, the ring on the tile that opened it.
        for ((id, slot) in listOf("car-settings" to SLOT_SETTINGS, "car-data" to SLOT_CAR, "phone" to SLOT_PHONE, "notifications" to SLOT_NOTICES)) {
            tap(id)
            rule.onNodeWithTag(slot).assertExists()
            back()
            rule.onNodeWithTag(SLOT_APPS).assertExists()
            rule.onNodeWithTag(appTag(tiles.single { it.id == id })).assertIsSelected()
        }
        assertTrue("none of them opened an old screen: $asked", asked.isEmpty())
        // From the Apps world itself, Back goes to the dial as always.
        back()
        rule.onNodeWithTag(SLOT_CHOOSER).assertExists()
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun otherAppsOpenFullScreen() {
        show()
        // (The fake opens nothing, so the Apps world stays.)
        toApps(); tap("phone-screen"); tap("browser"); tap("radio")
        assertEquals(listOf("app:phone-screen", "app:browser", "app:radio"), asked)
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theKnobsPushDoesTheSame() {
        show()
        toApps()
        // The ring starts on the first tile, Settings.
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(SLOT_SETTINGS).assertExists()
        assertTrue(asked.isEmpty())
    }

    // F3: with no maps app or no music app known, the Apps world with those apps, never the old grid.

    private fun showNoChoice(apps: List<CarAppEntry>): CarUiState {
        val state = PadWalkStates.state(CarLayout.MAP_BEHIND, CarScreen.Dashboard).apply {
            enginePreview = true
            this.apps.clear()
            this.apps.addAll(apps)
            activeNavAppId = null
            nowPlaying = null
            activeMediaAppId = null
        }
        rule.setContent {
            CarTheme(night = state.night, theme = state.theme) {
                MyPlayEngineShell(state, actions, appContent = { _, m -> Box(m.background(Color.DarkGray)) },
                    navMap = { _, m -> Box(m.background(Color(0xFF2E4A3A))) }, overlay = {}, skin = TestSkin,
                    sources = EngineSources.of(state, actions, LocalContext.current))
            }
        }
        rule.waitForIdle()
        return state
    }

    private val maps = entry("maps", CarAppKind.NAVIGATION)
    private val radio = entry("radio", CarAppKind.MEDIA)
    private val browser = entry("browser", CarAppKind.BROWSER)

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun navigateWithNoMapsAppShowsTheMapsApps() {
        showNoChoice(listOf(browser, maps, radio))
        // The dial opens on Navigate.
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(SLOT_APPS).assertExists()
        rule.onNodeWithTag(appTag(maps)).assertExists()
        rule.onNodeWithTag(appTag(radio)).assertDoesNotExist()
        rule.onNodeWithTag(appTag(browser)).assertDoesNotExist()
        assertTrue("never the old grid: $asked", asked.isEmpty())
        rule.onNodeWithTag(appTag(maps)).performClick()
        assertEquals(listOf("app:maps"), asked)
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun withNoMapsAppAtAllEveryAppIsShown() {
        showNoChoice(listOf(browser, radio))
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(appTag(radio)).assertExists()
        rule.onNodeWithTag(appTag(browser)).assertExists()
        assertTrue(asked.isEmpty())
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theLibraryWithNoMusicAppShowsTheMusicApps() {
        showNoChoice(listOf(browser, maps, radio))
        // Navigate, Music: one turn, then the library button (the only stop with nothing playing).
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        engine.performKeyInput { pressKey(Key.DirectionRight) }
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(SLOT_MUSIC).assertExists()
        rule.onNodeWithTag(MUSIC_LIBRARY).performClick()
        rule.onNodeWithTag(SLOT_APPS).assertExists()
        rule.onNodeWithTag(appTag(radio)).assertExists()
        rule.onNodeWithTag(appTag(maps)).assertDoesNotExist()
        assertTrue("never the old grid: $asked", asked.isEmpty())
    }
}
