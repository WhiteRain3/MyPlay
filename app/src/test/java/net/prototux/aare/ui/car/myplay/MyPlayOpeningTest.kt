package net.prototux.aare.ui.car.myplay

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import net.prototux.aare.ui.car.CarActions
import net.prototux.aare.ui.car.CarAppEntry
import net.prototux.aare.ui.car.CarLayout
import net.prototux.aare.ui.car.CarScreen
import net.prototux.aare.ui.car.CarShell
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
 * MyPlay (O3): with ORBIT an app takes the whole screen with no rail, grows out of the stop it was
 * opened from, and Back comes back to that stop (the Apps world with the ring on its disc, or the
 * home); Home asked for is the plain home; the way home shows the first few times in a drive.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class MyPlayOpeningTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var state: CarUiState
    private val asked = mutableListOf<String>()
    private val actions = object : CarActions by PadWalkStates.NoActions {
        override fun openApp(app: CarAppEntry) { asked += "app:${app.id}"; state.screen = CarScreen.App(app.id) }
        // As the car runtime's: Home asked for.
        override fun goHome() { state.engineHomeAsked = true; state.screen = CarScreen.Dashboard }
    }

    private fun show() {
        state = PadWalkStates.state(CarLayout.MAP_BEHIND, CarScreen.Dashboard).apply {
            enginePreview = true
            engineLanguage = "orbit"
            phoneGuidance = net.prototux.aare.notify.PhoneGuidance("k", "mock.maps", "Maps", "300 m", "Main Street", "", null, null, "")
        }
        rule.setContent {
            CarTheme(night = state.night, theme = state.theme) {
                CarShell(state, actions, appContent = { app, m ->
                    Box(m.background(Color.DarkGray).testTag("app_${app.id}").clickable { asked += "tap:${app.id}" })
                }, navMap = { _, m -> Box(m.background(Color(0xFF10151B))) })
            }
        }
        rule.waitForIdle()
    }

    private val engine get() = rule.onNodeWithTag(ENGINE_TAG)
    private fun key(k: Key) = engine.performKeyInput { pressKey(k) }

    /** The dial, then a second push: Apps (HN-3). */
    private fun toApps() {
        key(Key.DirectionCenter)
        key(Key.DirectionCenter)
        rule.onNodeWithTag(SLOT_APPS).assertExists()
    }

    /** The car's Back leaving the app (the runtime's back stack): MyPlay's screen again, not Home asked for. */
    private fun backFromApp() {
        rule.runOnIdle { state.screen = CarScreen.Dashboard }
        rule.waitForIdle()
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun backComesBackToTheDiscTheAppGrewFrom() {
        show()
        toApps()
        val apps = state.launcherApps
        key(Key.Tab)
        key(Key.DirectionCenter)
        assertEquals(listOf("app:${apps[1].id}"), asked)
        // The whole screen, no rail.
        rule.onNodeWithTag(FRAME_TAG).assertExists()
        rule.onNodeWithTag(FRAME_RAIL_TAG).assertDoesNotExist()
        backFromApp()
        // The Apps world, the ring on that disc; the app shrinks back into it, then is gone.
        rule.onNodeWithTag(SLOT_APPS).assertExists()
        rule.onNodeWithTag(appTag(apps[1])).assertIsSelected()
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithTag(SHRINK_TAG).assertDoesNotExist()
        // Back goes on up as always: the dial.
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag(SLOT_CHOOSER).assertExists()
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun homeIsThePlainHome() {
        show()
        toApps()
        key(Key.Tab)
        key(Key.DirectionCenter)
        rule.onNodeWithTag(FRAME_TAG).assertExists()
        rule.runOnIdle { actions.goHome() }
        rule.waitForIdle()
        rule.onNodeWithTag(SLOT_APPS).assertDoesNotExist()
        rule.onNodeWithTag(STOP_HUB).assertIsSelected()
        rule.onNodeWithTag(SHRINK_TAG).assertDoesNotExist()
        assertTrue("cleared once seen", !state.engineHomeAsked)
        // And the next app's Back comes back where it was again.
        rule.onNodeWithTag(STOP_TURN).performClick()
        rule.onNodeWithTag(FRAME_TAG).assertExists()
        backFromApp()
        rule.onNodeWithTag(STOP_TURN).assertIsSelected()
    }

    /** O3-2: Home pressed while already on MyPlay's screen: the plain home, and the next app's Back still comes back. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun homeOnTheHomeLeavesNoStaleFlag() {
        show()
        toApps()
        rule.runOnIdle { actions.goHome() }
        rule.waitForIdle()
        assertTrue(!state.engineHomeAsked)
        rule.onNodeWithTag(SLOT_APPS).assertDoesNotExist()
        rule.onNodeWithTag(STOP_HUB).assertIsSelected()
        // HN-3: the dial opens on its middle, Apps.
        key(Key.DirectionCenter)
        key(Key.DirectionCenter)
        rule.onNodeWithTag(SLOT_APPS).assertExists()
        key(Key.Tab)
        key(Key.DirectionCenter)
        rule.onNodeWithTag(FRAME_TAG).assertExists()
        backFromApp()
        rule.onNodeWithTag(SLOT_APPS).assertExists()
        rule.onNodeWithTag(appTag(state.launcherApps[1])).assertIsSelected()
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theMusicPanelsAppComesBackToTheSong() {
        show()
        rule.onNodeWithTag(STOP_SONG).performClick()
        rule.onNodeWithTag(PANEL_APP).performClick()
        rule.onNodeWithTag(FRAME_TAG).assertExists()
        backFromApp()
        rule.onNodeWithTag(SLOT_MUSIC_PANEL).assertDoesNotExist()
        rule.onNodeWithTag(STOP_SONG).assertIsSelected()
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theAppGrowsOutOfItsDisc() {
        show()
        toApps()
        val disc = rule.onNodeWithTag(appTag(state.launcherApps[0])).fetchSemanticsNode().boundsInRoot
        rule.mainClock.autoAdvance = false
        key(Key.DirectionCenter)
        // The first frame the app is on screen (of its 260 ms).
        var frames = 0
        while (rule.onAllNodes(androidx.compose.ui.test.hasTestTag(FRAME_APP_TAG)).fetchSemanticsNodes().isEmpty() && frames++ < 30) rule.mainClock.advanceTimeByFrame()
        // O3-3: the app itself is full size at once (a surface view never has to follow a scale);
        // the cover over it leaves a window round the disc.
        val app = rule.onNodeWithTag(FRAME_APP_TAG).fetchSemanticsNode().boundsInRoot
        assertEquals(1280f, app.width, 1f)
        rule.onNodeWithTag(REVEAL_TAG).assertExists()
        val box = FocusGraph.Box(disc.left, disc.top, disc.right, disc.bottom)
        val window = revealRect(box, 1280f, 720f, 0f)
        assertEquals(box, window)
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithTag(REVEAL_TAG).assertDoesNotExist()
        assertEquals(FocusGraph.Box(0f, 0f, 1280f, 720f), revealRect(box, 1280f, 720f, 1f))
    }

    @Test fun theScaleRunsFromTheStopToTheScreen() {
        val box = FocusGraph.Box(100f, 100f, 228f, 172f)
        assertEquals(0.1f, openScale(box, 1280f, 720f, 0f), 0.001f)
        assertEquals(1f, openScale(box, 1280f, 720f, 1f), 0.001f)
        assertEquals(0.6f, openScale(null, 1280f, 720f, 0f), 0.001f)
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theWayHomeShowsTheFirstThreeTimes() {
        show()
        repeat(4) { i ->
            rule.onNodeWithTag(STOP_TURN).performClick()
            if (i < HOME_HINT_TIMES) {
                rule.onNodeWithTag(HOME_HINT_TAG).assertExists()
                rule.mainClock.advanceTimeBy(HOME_HINT_MS + 500)
            }
            rule.onNodeWithTag(HOME_HINT_TAG).assertDoesNotExist()
            backFromApp()
        }
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun touchesAndKeysStillReachTheApp() {
        show()
        rule.onNodeWithTag(STOP_TURN).performClick()
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithTag("app_maps").performClick()
        assertEquals("tap:maps", asked.last())
        // Nothing of MyPlay's is over the app to catch the knob: the frame takes no keys (no call, no notice).
        rule.onNodeWithTag("app_maps").performKeyInput { pressKey(Key.Tab) }
        rule.onNodeWithTag(FRAME_TAG).assertExists()
    }
}
