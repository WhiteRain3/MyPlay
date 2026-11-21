package net.prototux.aare.ui.car.myplay

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import net.prototux.aare.ui.car.CarActions
import net.prototux.aare.ui.car.CarAppEntry
import net.prototux.aare.ui.car.CarCall
import net.prototux.aare.ui.car.CarLayout
import net.prototux.aare.ui.car.CarNotice
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
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * MyPlay (F1, the owner's first drive): a full-screen app opened from MyPlay's own screen is shown
 * in MyPlay's frame (the rail with the way home, the skin's notice and call), never the old
 * layout's bar. For ORBIT and DECK.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class MyPlayAppFrameTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val asked = mutableListOf<String>()
    private var state: CarUiState? = null
    private val actions = object : CarActions by PadWalkStates.NoActions {
        override fun goHome() { asked += "home"; state?.screen = CarScreen.Dashboard }
        override fun openApp(app: CarAppEntry) { asked += "app:${app.id}"; state?.screen = CarScreen.App(app.id) }
        override fun answerCall() { asked += "answer" }
        override fun dismissNotice(notice: CarNotice) { asked += "dismiss:${notice.key}"; state?.notices?.remove(notice) }
    }

    private fun show(language: String = "orbit", screen: CarScreen = CarScreen.App("music"), railRight: Boolean = false): CarUiState {
        val s = PadWalkStates.state(CarLayout.MAP_BEHIND, CarScreen.Dashboard).apply {
            enginePreview = true
            engineLanguage = language
            this.screen = screen
            railOnRight = railRight
        }
        state = s
        rule.setContent {
            CarTheme(night = s.night, theme = s.theme) {
                CarShell(s, actions, appContent = { app, m -> Box(m.background(Color.DarkGray).testTag("app_${app.id}")) },
                    navMap = { _, m -> Box(m.background(Color(0xFF10151B))) })
            }
        }
        rule.waitForIdle()
        return s
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun anAppIsInMyPlaysFrameWithTheWayHome() {
        for (language in listOf("orbit", "deck")) {
            asked.clear()
            show(language)
            rule.onNodeWithTag(FRAME_TAG).assertExists()
            rule.onNodeWithTag("app_music").assertExists()
            rule.onNodeWithTag(FRAME_RAIL_TAG).assertExists()
            // The rail is on the driver's side (left here), the app takes the rest.
            val rail = rule.onNodeWithTag(FRAME_RAIL_TAG).fetchSemanticsNode().boundsInRoot
            val app = rule.onNodeWithTag("app_music").fetchSemanticsNode().boundsInRoot
            assertTrue("rail ${rail.left}..${rail.right}, app ${app.left}", rail.left == 0f && app.left >= rail.right)
            rule.onNodeWithTag(FRAME_HOME_TAG).performClick()
            assertEquals(listOf("home"), asked)
            // Home is MyPlay's own home again.
            rule.onNodeWithTag(ENGINE_TAG).assertExists()
            rule.onNodeWithTag(FRAME_TAG).assertDoesNotExist()
            if (language == "orbit") break // one setContent per test: DECK below
        }
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun deckToo() {
        show("deck")
        rule.onNodeWithTag(FRAME_TAG).assertExists()
        rule.onNodeWithTag(FRAME_HOME_TAG).performClick()
        assertEquals(listOf("home"), asked)
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theRailFollowsTheDriversSide() {
        show(railRight = true)
        val rail = rule.onNodeWithTag(FRAME_RAIL_TAG).fetchSemanticsNode().boundsInRoot
        val app = rule.onNodeWithTag("app_music").fetchSemanticsNode().boundsInRoot
        assertTrue(rail.left >= app.right)
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theAppGridIsFramedToo() {
        show(screen = CarScreen.Launcher)
        rule.onNodeWithTag(FRAME_TAG).assertExists()
        rule.onNodeWithTag(FRAME_HOME_TAG).assertExists()
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun aCallRingingOverAnAppIsTheSkinsAndTakesTheKnob() {
        Shadows.shadowOf(rule.activity.application).grantPermissions(android.Manifest.permission.ANSWER_PHONE_CALLS)
        val s = show()
        rule.runOnIdle { s.call = CarCall(true, "Anna K.", 1L) }
        rule.waitForIdle()
        rule.onNodeWithTag(SLOT_CALL).assertExists()
        rule.onNodeWithText("Anna K.", substring = true).assertExists()
        rule.onNodeWithTag(SLOT_CALL).performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(listOf("answer"), asked)
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun aNoticeOverAnAppIsTheSkinsAndATapDismissesIt() {
        val s = show()
        rule.runOnIdle { s.notices.add(0, CarNotice("n1", "Anna K.", "Running late", null)) }
        rule.waitForIdle()
        rule.onNodeWithText("Anna K.", substring = true).assertExists()
        rule.onNodeWithTag(NOTICE_DISMISS).performClick()
        assertEquals(listOf("dismiss:n1"), asked)
    }

    private fun picture(name: String) {
        val bmp = rule.onNodeWithTag(FRAME_TAG).captureToImage().asAndroidBitmap()
        val dir = java.io.File("build/reports/myplay-screens").apply { mkdirs() }
        java.io.File(dir, "$name.png").outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun orbitFrameNight() { show("orbit"); picture("frame-orbit-1280x720-night") }
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun deckFrameNight() { show("deck"); picture("frame-deck-1280x720-night") }
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun orbitFrameDay901() {
        show("orbit"); rule.runOnIdle { state!!.night = false }; rule.waitForIdle(); picture("frame-orbit-901x543-day")
    }
}
