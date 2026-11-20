package net.prototux.aare.host.templates

import androidx.activity.ComponentActivity
import androidx.car.app.OnDoneCallback
import androidx.car.app.model.CarIcon
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import net.prototux.aare.R
import net.prototux.aare.ui.theme.CarTheme
import net.prototux.aare.ui.theme.CarThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** MyPlay (R5): the knob moves a hosted map: push in, pad pans, turn zooms, push or Back out. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w901dp-h543dp-land-mdpi")
@OptIn(ExperimentalTestApi::class)
class MapMoveStopTest {
    // R5-3: the key/touch mark is one flag for the whole test process; every test starts and ends on touch.
    @org.junit.Before fun touchInputFirst() { net.prototux.aare.ui.car.CarFocusVisibility.keyInput = false }
    @org.junit.After fun touchInputAfter() { net.prototux.aare.ui.car.CarFocusVisibility.keyInput = false }

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val scrolls = ArrayList<Pair<Float, Float>>()
    private val scales = ArrayList<Float>()

    private val host = object : TemplateHost {
        override val driving = false
        override val night = true
        override val appLabel = "Maps"
        override fun contentLimit(type: Int) = 6
        override val done = object : OnDoneCallback {}
        override fun back() = Unit
        override fun loadIcon(icon: CarIcon, sizePx: Int): ImageBitmap? = null
        override fun showParkedOnlyMessage() = Unit
        @androidx.compose.runtime.Composable override fun MapSurface(modifier: Modifier) = Unit
        override fun setVisibleArea(left: Int, top: Int, right: Int, bottom: Int) = Unit
        override fun setStableArea(left: Int, top: Int, right: Int, bottom: Int) = Unit
        override fun mapScroll(dx: Float, dy: Float) { scrolls += dx to dy }
        override fun mapFling(vx: Float, vy: Float) = Unit
        override fun mapScale(focusX: Float, focusY: Float, factor: Float) { scales += factor }
        override fun mapClick(x: Float, y: Float) = Unit
    }

    @Test fun pushPanZoomAndLeave() {
        rule.setContent {
            CarTheme(night = true, theme = CarThemes.byId("aare")) {
                CompositionLocalProvider(LocalTemplateHost provides host) {
                    MapScaffold(actionStrip = null, mapActionStrip = null, panModeDelegate = null) { }
                }
            }
        }
        val hint = rule.activity.getString(R.string.myplay_map_move_hint)
        val stop = rule.onNode(isFocusable())
        stop.requestFocus()
        stop.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithText(hint).assertExists()

        stop.performKeyInput { pressKey(Key.DirectionRight) }
        stop.performKeyInput { pressKey(Key.Tab) }
        assertEquals(1, scrolls.size)
        assertTrue("pad right moves the view right: ${scrolls[0]}", scrolls[0].first > 0f && scrolls[0].second == 0f)
        assertEquals(listOf(MapMoveKeys.ZOOM_IN), scales)

        stop.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithText(hint).assertDoesNotExist()

        // In again, and out with Back.
        stop.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithText(hint).assertExists()
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithText(hint).assertDoesNotExist()
    }
}
