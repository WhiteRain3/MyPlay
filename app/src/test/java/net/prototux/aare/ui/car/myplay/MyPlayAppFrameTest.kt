package net.prototux.aare.ui.car.myplay

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.withKeyDown
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
        // The knob's push, as the car sends it: to whatever holds the focus; the frame keeps it for the call.
        // (This app has nothing to focus: the call's layer holds the focus then.)
        rule.mainClock.advanceTimeBy(100)
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

    /**
     * F1-1 (R5-2): a call rings over an app and ends; the knob is back where it was in the app (here
     * a mirrored app's key stop, the second stop), so the next push goes to the app, not Home.
     */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun afterACallTheKnobIsBackWhereItWasInTheApp() {
        Shadows.shadowOf(rule.activity.application).grantPermissions(android.Manifest.permission.ANSWER_PHONE_CALLS)
        val sent = mutableListOf<Int>()
        val mirror = androidx.compose.ui.focus.FocusRequester()
        val s = PadWalkStates.state(CarLayout.MAP_BEHIND, CarScreen.Dashboard).apply {
            enginePreview = true
            screen = CarScreen.App("music")
        }
        state = s
        rule.setContent {
            CarTheme(night = s.night, theme = s.theme) {
                CarShell(s, actions, appContent = { _, m ->
                    androidx.compose.foundation.layout.Column(m) {
                        Box(Modifier.size(80.dp).focusable().testTag("app_first"))
                        net.prototux.aare.ui.car.MirroredKeyStop(net.prototux.aare.car.MirroredKeys.TurnMode.UP_DOWN, { action, _ -> sent += action },
                            Modifier.focusRequester(mirror).testTag("app_mirror"))
                    }
                }, navMap = { _, m -> Box(m) })
            }
        }
        rule.waitForIdle()
        rule.runOnIdle { mirror.requestFocus() }
        rule.onNodeWithTag("app_mirror").assertIsFocused()
        // A call rings: it takes the knob's keys (the app's stop keeps the focus but gets nothing).
        rule.runOnIdle { s.call = CarCall(true, "Anna K.", 1L) }
        rule.waitForIdle()
        rule.onNodeWithTag("app_mirror").performKeyInput { pressKey(Key.Tab); pressKey(Key.DirectionDown) }
        assertTrue("the app got no keys while it rang: $sent", sent.isEmpty())
        // It ends (declined on the phone): the knob is the mirrored app's again.
        rule.runOnIdle { s.call = null }
        rule.waitForIdle()
        rule.onNodeWithTag("app_mirror").assertIsFocused()
        rule.onNodeWithTag("app_mirror").performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(listOf(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.ACTION_UP), sent)
        assertTrue("never home: $asked", "home" !in asked)
    }

    /** F1-4: a key held as the call starts ringing: its up still goes to the app (no key left pressed). */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun aKeyHeldAsTheCallRingsIsReleasedInTheApp() {
        Shadows.shadowOf(rule.activity.application).grantPermissions(android.Manifest.permission.ANSWER_PHONE_CALLS)
        val sent = mutableListOf<Int>()
        val mirror = androidx.compose.ui.focus.FocusRequester()
        val s = PadWalkStates.state(CarLayout.MAP_BEHIND, CarScreen.Dashboard).apply {
            enginePreview = true
            screen = CarScreen.App("music")
        }
        state = s
        rule.setContent {
            CarTheme(night = s.night, theme = s.theme) {
                CarShell(s, actions, appContent = { _, m ->
                    net.prototux.aare.ui.car.MirroredKeyStop(net.prototux.aare.car.MirroredKeys.TurnMode.UP_DOWN, { action, _ -> sent += action },
                        m.focusRequester(mirror).testTag("app_mirror"))
                }, navMap = { _, m -> Box(m) })
            }
        }
        rule.waitForIdle()
        rule.runOnIdle { mirror.requestFocus() }
        rule.onNodeWithTag("app_mirror").performKeyInput { keyDown(Key.DirectionCenter) }
        rule.runOnIdle { s.call = CarCall(true, "Anna K.", 1L) }
        rule.waitForIdle()
        rule.onNodeWithTag("app_mirror").performKeyInput { keyUp(Key.DirectionCenter) }
        assertEquals("down, then its up: nothing left pressed", listOf(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.ACTION_UP), sent)
        assertTrue("the held key did not answer the call: $asked", "answer" !in asked)
    }

    // F1-5: the owner's car has no touch screen: a notice's buttons by the knob over an app too.

    private val sentToApp = mutableListOf<Int>()
    private val appStop = androidx.compose.ui.focus.FocusRequester()

    /** An app whose knob stop (a mirrored app's) holds the focus, as in the car. */
    private fun showMirroredApp(): CarUiState {
        val s = PadWalkStates.state(CarLayout.MAP_BEHIND, CarScreen.Dashboard).apply {
            enginePreview = true
            screen = CarScreen.App("music")
        }
        state = s
        rule.setContent {
            CarTheme(night = s.night, theme = s.theme) {
                CarShell(s, actions, appContent = { _, m ->
                    net.prototux.aare.ui.car.MirroredKeyStop(net.prototux.aare.car.MirroredKeys.TurnMode.UP_DOWN, { action, _ -> sentToApp += action },
                        m.focusRequester(appStop).testTag("app_mirror"))
                }, navMap = { _, m -> Box(m) })
            }
        }
        rule.waitForIdle()
        rule.runOnIdle { appStop.requestFocus() }
        return s
    }

    private val replied = mutableListOf<String>()
    private fun withButtons(key: String = "n1") = CarNotice(key, "Anna K.", "Running late", null, listOf("Reply" to { replied += key }, "Mark read" to { replied += "read:$key" }), message = true)
    private fun keys(block: androidx.compose.ui.test.KeyInjectionScope.() -> Unit) = rule.onNodeWithTag("app_mirror").performKeyInput(block)

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun aNoticesButtonsByTheKnobOverAnApp() {
        val s = showMirroredApp()
        rule.runOnIdle { s.notices.add(0, withButtons()) }
        rule.waitForIdle()
        // The ring starts on Dismiss; a turn back reaches its buttons (Reply, Mark read, Dismiss).
        rule.onNodeWithTag(NOTICE_DISMISS).assertIsSelected()
        keys { withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } }
        keys { withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } }
        rule.onNodeWithTag(noticeActionStop(0)).assertIsSelected()
        keys { pressKey(Key.DirectionCenter) }
        assertEquals(listOf("n1"), replied)
        assertEquals(listOf("dismiss:n1"), asked)
        assertTrue("the app got none of these keys: $sentToApp", sentToApp.isEmpty())
        // The notice gone, the knob is the app's again, and it never lost the focus.
        rule.onNodeWithTag("app_mirror").assertIsFocused()
        keys { pressKey(Key.DirectionCenter) }
        assertEquals(listOf(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.ACTION_UP), sentToApp)
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun aPushAsTheNoticeAppearsOnlyDismissesIt() {
        val s = showMirroredApp()
        rule.runOnIdle { s.notices.add(0, withButtons()) }
        rule.waitForIdle()
        keys { pressKey(Key.DirectionCenter) }
        assertTrue("no button ran: $replied", replied.isEmpty())
        assertEquals(listOf("dismiss:n1"), asked)
        assertTrue(sentToApp.isEmpty())
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun backDismissesTheNoticeNotTheApp() {
        val s = showMirroredApp()
        rule.runOnIdle { s.notices.add(0, withButtons()) }
        rule.waitForIdle()
        keys { pressKey(Key.Back) }
        assertEquals(listOf("dismiss:n1"), asked)
        assertTrue("still in the app: ${s.screen}", s.screen == CarScreen.App("music"))
        rule.onNodeWithTag("app_mirror").assertIsFocused()
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun aNoticeWithoutButtonsLeavesTheKnobToTheApp() {
        val s = showMirroredApp()
        rule.runOnIdle { s.notices.add(0, CarNotice("n2", "Calendar", "Dentist at 15:00", null)) }
        rule.waitForIdle()
        keys { pressKey(Key.DirectionCenter) }
        assertEquals(listOf(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.ACTION_UP), sentToApp)
        assertTrue(asked.isEmpty())
    }

    // F1-3: the gesture line (R6) over an app, under the notice (R6-3).

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theGestureLineShowsOverAnAppUnderTheNotice() {
        val s = show()
        rule.runOnIdle { s.gestureLine = net.prototux.aare.ui.car.GestureLine(net.prototux.aare.car.ButtonAction.NEXT, forward = true, atMs = 1) }
        rule.waitForIdle()
        rule.onNodeWithTag(net.prototux.aare.ui.car.GESTURE_LINE_TAG).assertExists()
        rule.runOnIdle { s.notices.add(0, CarNotice("n1", "Anna K.", "Running late", null)) }
        rule.waitForIdle()
        val line = rule.onNodeWithTag(net.prototux.aare.ui.car.GESTURE_LINE_TAG).fetchSemanticsNode().boundsInRoot
        val dismiss = rule.onNodeWithTag(NOTICE_DISMISS).fetchSemanticsNode().boundsInRoot
        assertTrue("the line ($line) is under the notice (its Dismiss at $dismiss)", line.top >= dismiss.bottom)
    }

    /** The owner: a browser video in fullscreen takes the whole screen (no rail); after it, the rail again. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun anAppsFullscreenHidesTheRail() {
        val s = show()
        rule.onNodeWithTag(FRAME_RAIL_TAG).assertExists()
        rule.runOnIdle { s.appFullscreen = true }
        rule.waitForIdle()
        rule.onNodeWithTag(FRAME_RAIL_TAG).assertDoesNotExist()
        val app = rule.onNodeWithTag("app_music").fetchSemanticsNode().boundsInRoot
        assertTrue("the app takes the whole width: $app", app.left == 0f && app.right == 1280f)
        rule.runOnIdle { s.appFullscreen = false }
        rule.waitForIdle()
        rule.onNodeWithTag(FRAME_RAIL_TAG).assertExists()
    }
}
