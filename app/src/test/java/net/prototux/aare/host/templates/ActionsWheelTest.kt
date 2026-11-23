package net.prototux.aare.host.templates

import androidx.activity.ComponentActivity
import androidx.car.app.OnDoneCallback
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import net.prototux.aare.R
import net.prototux.aare.ui.theme.CarTheme
import net.prototux.aare.ui.theme.CarThemes
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Look C, steps 1 and 2: under ORBIT the maps app's strips are not drawn; the pad's centre or Back
 * opens them as a wheel; Back closes it, and a second Back soon after goes home (K3: Back never traps).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w1280dp-h720dp-land-mdpi")
@OptIn(ExperimentalTestApi::class)
class ActionsWheelTest {
    @org.junit.Before fun touchInputFirst() { net.prototux.aare.ui.car.CarFocusVisibility.keyInput = false }
    @org.junit.After fun touchInputAfter() { net.prototux.aare.ui.car.CarFocusVisibility.keyInput = false }

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val ran = ArrayList<String>()
    private var clock = 10_000L
    private var homes = 0
    private var held = false

    private val host = object : TemplateHost {
        override val driving = false
        override val night = true
        override val appLabel = "Maps"
        override fun contentLimit(type: Int) = 6
        override val done = object : OnDoneCallback {}
        override fun back() { ran += "app-back" }
        override fun loadIcon(icon: CarIcon, sizePx: Int): ImageBitmap? = null
        override fun showParkedOnlyMessage() = Unit
        @androidx.compose.runtime.Composable override fun MapSurface(modifier: Modifier) = Unit
        override fun setVisibleArea(left: Int, top: Int, right: Int, bottom: Int) = Unit
        override fun setStableArea(left: Int, top: Int, right: Int, bottom: Int) = Unit
        override fun mapScroll(dx: Float, dy: Float) = Unit
        override fun mapFling(vx: Float, vy: Float) = Unit
        override fun mapScale(focusX: Float, focusY: Float, factor: Float) = Unit
        override fun mapClick(x: Float, y: Float) = Unit
    }

    private val strip = ActionStrip.Builder()
        .addAction(Action.Builder().setTitle("Report").setOnClickListener { ran += "report" }.build())
        .addAction(Action.Builder().setTitle("Mute").setOnClickListener { ran += "mute" }.build())
        .build()
    private val mapStrip = ActionStrip.Builder().addAction(Action.PAN).build()

    private fun show(wheel: Boolean) = rule.setContent {
        CarTheme(night = true, theme = CarThemes.byId("aare")) {
            val w = if (wheel) ActionsWheelHost({ homes++ }, "MyPlay home", "Move map", now = { clock }, held = { held }) else null
            CompositionLocalProvider(LocalTemplateHost provides host, LocalActionsWheel provides w) {
                MapScaffold(strip, mapStrip, panModeDelegate = null, wheel = true) { }
            }
        }
    }

    private fun back() { rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle() }

    @Test fun withoutTheWheelTheStripsShowAsBefore() {
        show(wheel = false)
        rule.onNodeWithText("Report").assertExists()
        rule.onNodeWithTag(ACTIONS_WHEEL_TAG).assertDoesNotExist()
    }

    @Test fun thePushOpensTheWheelAndRunsAnAction() {
        show(wheel = true)
        rule.onNodeWithText("Report").assertDoesNotExist()
        val stop = rule.onNode(isFocusable())
        stop.requestFocus()
        stop.performKeyInput { pressKey(Key.DirectionCenter) }
        val wheel = rule.onNodeWithTag(ACTIONS_WHEEL_TAG)
        wheel.assertExists()
        // Report, Mute, Move map, MyPlay home: the first ringed; down to Mute and push.
        rule.onNodeWithTag(wheelRowTag("a0")).assertIsSelected()
        wheel.performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithTag(wheelRowTag("a1")).assertIsSelected()
        wheel.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.waitForIdle()
        shadowOf(android.os.Looper.getMainLooper()).idle()
        rule.onNodeWithTag(ACTIONS_WHEEL_TAG).assertDoesNotExist()
        assertEquals(listOf("mute"), ran)
    }

    /** For the design comparison: the wheel open over a plain map, its first action ringed. */
    @Test @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun theOpenWheelPicture() {
        rule.setContent {
            CarTheme(night = true, theme = CarThemes.byId("aare")) {
                val w = ActionsWheelHost({ homes++ }, "MyPlay home", "Move map", now = { clock }, held = { held })
                CompositionLocalProvider(LocalTemplateHost provides host, LocalActionsWheel provides w) {
                    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF0B1318))) {
                        MapScaffold(strip, mapStrip, panModeDelegate = null, wheel = true) { }
                    }
                }
            }
        }
        val stop = rule.onNode(isFocusable())
        stop.requestFocus()
        stop.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(ACTIONS_WHEEL_TAG).assertExists()
        rule.onNodeWithTag(wheelRowTag("a0")).assertIsSelected()
        rule.mainClock.advanceTimeBy(1_000)
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        val dir = java.io.File("build/reports/myplay-screens").apply { mkdirs() }
        java.io.File(dir, "actions-wheel-1280x720-night.png").outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun moveMapStartsMovingTheMap() {
        show(wheel = true)
        back()
        val wheel = rule.onNodeWithTag(ACTIONS_WHEEL_TAG)
        repeat(2) { wheel.performKeyInput { pressKey(Key.Tab) } }
        rule.onNodeWithText("Move map").assertExists()
        wheel.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.waitForIdle()
        // Look C (design 17): ORBIT's look: the hint pill and the crosshair, not the old line.
        rule.onNodeWithTag(MAP_MOVE_PILL_TAG).assertExists()
        rule.onNodeWithTag(MAP_MOVE_CROSS_TAG).assertExists()
        rule.onNodeWithText(rule.activity.getString(R.string.myplay_map_move_back)).assertExists()
        rule.onNodeWithText(rule.activity.getString(R.string.myplay_map_move_hint)).assertDoesNotExist()
    }

    /** While a call or a notice holds the knob, Back and the push are not the wheel's. */
    @Test fun aNoticeHoldingTheKnobKeepsBack() {
        held = true
        show(wheel = true)
        back()
        rule.onNodeWithTag(ACTIONS_WHEEL_TAG).assertDoesNotExist()
        val stop = rule.onNode(isFocusable())
        stop.requestFocus()
        stop.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(ACTIONS_WHEEL_TAG).assertDoesNotExist()
        assertEquals(0, homes)
    }

    /** K3: Back twice quickly goes home; a later Back with the wheel open closes it, and one more soon after goes home. */
    @Test fun backNeverTraps() {
        show(wheel = true)
        back()
        rule.onNodeWithTag(ACTIONS_WHEEL_TAG).assertExists()
        clock += 800
        back()
        assertEquals(1, homes)
        rule.onNodeWithTag(ACTIONS_WHEEL_TAG).assertDoesNotExist()
        // Later: Back opens it, a slow Back closes it, a quick third goes home.
        clock += 10_000
        back()
        clock += 3_000
        back()
        rule.onNodeWithTag(ACTIONS_WHEEL_TAG).assertDoesNotExist()
        assertEquals(1, homes)
        clock += 1_500
        back()
        assertEquals(2, homes)
        // Turning in the wheel means it is in use: a quick Back then only closes it.
        clock += 10_000
        back()
        val wheel = rule.onNodeWithTag(ACTIONS_WHEEL_TAG)
        wheel.performKeyInput { pressKey(Key.DirectionDown) }
        back()
        rule.onNodeWithTag(ACTIONS_WHEEL_TAG).assertDoesNotExist()
        assertEquals(2, homes)
        // Its last row is MyPlay home.
        clock += 10_000
        back()
        repeat(5) { rule.onNodeWithTag(ACTIONS_WHEEL_TAG).performKeyInput { pressKey(Key.DirectionDown) } }
        rule.onNodeWithTag(wheelRowTag(WHEEL_HOME)).assertIsSelected()
        rule.onNodeWithTag(ACTIONS_WHEEL_TAG).performKeyInput { pressKey(Key.DirectionCenter) }
        rule.waitForIdle()
        assertEquals(3, homes)
        // The app's own Back is never asked for on its map.
        assertEquals(emptyList<String>(), ran)
    }
}
