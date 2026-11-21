package net.prototux.aare.ui.car.myplay

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.withKeyDown
import net.prototux.aare.ui.car.CarActions
import net.prototux.aare.ui.car.CarAppEntry
import net.prototux.aare.ui.car.CarCall
import net.prototux.aare.ui.car.CarLayout
import net.prototux.aare.ui.car.CarScreen
import net.prototux.aare.ui.car.CarShell
import net.prototux.aare.ui.car.CarUiState
import net.prototux.aare.ui.car.PadWalkStates
import net.prototux.aare.ui.theme.CarTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * MyPlay (F5, the owner's first drive): the routes out of the new screen, through the car shell as
 * the car draws it, for ORBIT and DECK. The actions change the screen as the car runtime does, so
 * an app opened is drawn as the car would draw it. Never the old layout, never the old app grid.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class MyPlayRoutesTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val asked = mutableListOf<String>()
    private lateinit var state: CarUiState
    /** As the car runtime: opening an app shows it; the grid is the old grid (never asked for, below). */
    private val actions = object : CarActions by PadWalkStates.NoActions {
        override fun openApp(app: CarAppEntry) { asked += "app:${app.id}"; state.screen = CarScreen.App(app.id) }
        override fun openLauncher() { asked += "launcher"; state.screen = CarScreen.Launcher }
        override fun openMusicApps() { asked += "music-apps"; state.screen = CarScreen.Launcher }
        override fun goHome() { state.screen = CarScreen.Dashboard }
    }

    private fun show(language: String, maps: Boolean = true, guidance: Boolean = true) {
        state = PadWalkStates.state(CarLayout.MAP_BEHIND, CarScreen.Dashboard).apply {
            enginePreview = true
            engineLanguage = language
            if (guidance) phoneGuidance = net.prototux.aare.notify.PhoneGuidance("k", "mock.maps", "Maps", "300 m", "Main Street", "", null, null, "")
            if (!maps) activeNavAppId = null
        }
        rule.setContent {
            CarTheme(night = state.night, theme = state.theme) {
                CarShell(state, actions, appContent = { app, m -> Box(m.background(Color.DarkGray).testTag("app_${app.id}")) },
                    navMap = { _, m -> Box(m.background(Color(0xFF10151B))) })
            }
        }
        rule.waitForIdle()
    }

    /** Wherever a route led: MyPlay's home, or an app in MyPlay's frame; never the old grid. */
    private fun inMyPlay(what: String) {
        val engine = rule.onAllNodesWithTagCount(ENGINE_TAG)
        val frame = rule.onAllNodesWithTagCount(FRAME_TAG)
        assertTrue("$what: MyPlay's screen or frame (engine $engine, frame $frame)", engine + frame == 1)
        assertTrue("$what: never the old app grid: $asked", "launcher" !in asked && "music-apps" !in asked)
        assertTrue("$what: never the old grid on screen", state.screen != CarScreen.Launcher)
    }

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTagCount(tag: String) =
        onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().size

    private fun home() {
        rule.runOnIdle { state.screen = CarScreen.Dashboard }
        rule.waitForIdle()
    }

    private fun routes(language: String) {
        show(language)
        // The turn read-out: the maps app, in the frame.
        rule.onNodeWithTag(STOP_TURN).performClick()
        rule.onNodeWithTag("app_maps").assertExists()
        rule.onNodeWithTag(FRAME_TAG).assertExists()
        inMyPlay("turn read-out")
        // The frame's way home.
        rule.onNodeWithTag(FRAME_HOME_TAG).performClick()
        rule.onNodeWithTag(ENGINE_TAG).assertExists()
        // A call placed from an app: the car runtime asks for the Calls world.
        rule.runOnIdle { state.engineOpenWorld = World.CALLS.id }
        rule.waitForIdle()
        rule.onNodeWithTag(SLOT_PHONE).assertExists()
        inMyPlay("a call placed")
        // Back home (ORBIT: the dial, then home; DECK: home).
        while (rule.onAllNodesWithTagCount(STOP_HUB) == 0 || rule.onAllNodesWithTagCount(SLOT_PHONE) + rule.onAllNodesWithTagCount(SLOT_CHOOSER) > 0) {
            rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
            rule.waitForIdle()
        }
        // In a call: the strip opens the Calls world.
        rule.runOnIdle { state.call = CarCall(ringing = false, name = "Anna K.", sinceMs = System.currentTimeMillis()) }
        rule.waitForIdle()
        rule.onNodeWithTag(STOP_CALL).performClick()
        rule.onNodeWithTag(SLOT_PHONE).assertExists()
        inMyPlay("the call's strip")
        assertTrue("the strip opened no app: $asked", asked.none { it == "app:phone" })
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun orbitsRoutesStayInMyPlay() = routes("orbit")
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun decksRoutesStayInMyPlay() = routes("deck")

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun orbitWithNoMapsAppNavigateIsTheAppsWorld() {
        show("orbit", maps = false, guidance = false)
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        engine.performKeyInput { pressKey(Key.DirectionCenter) } // the dial, on Navigate
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(SLOT_APPS).assertExists()
        inMyPlay("Navigate with no maps app")
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun deckWithNoMapsAppTheNavigateCardIsTheAppsWorld() {
        show("deck", maps = false, guidance = false)
        // The knob: five turns back bring Navigate (the last card) to the front, a push opens it.
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        repeat(5) { engine.performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } } }
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(SLOT_APPS).assertExists()
        inMyPlay("DECK's Navigate card with no maps app")
    }
}
