package net.prototux.aare.host.templates

import androidx.activity.ComponentActivity
import androidx.car.app.OnDoneCallback
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.assertIsFocused
import net.prototux.aare.ui.theme.CarTheme
import net.prototux.aare.ui.theme.CarThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Look C, step 3: an app's list in the wheel's look keeps the focused row in the middle. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w1280dp-h720dp-land-mdpi")
@OptIn(ExperimentalTestApi::class)
class WheelListsTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val host = object : TemplateHost {
        override val driving = false
        override val night = true
        override val appLabel = "Maps"
        override fun contentLimit(type: Int) = 100
        override val done = object : OnDoneCallback {}
        override fun back() = Unit
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

    private fun show(wheel: Boolean) {
        val items = ItemList.Builder().apply {
            for (i in 1..12) addItem(Row.Builder().setTitle("Place $i").setOnClickListener { }.build())
        }.build()
        rule.setContent {
            CarTheme(night = true, theme = CarThemes.byId("aare")) {
                CompositionLocalProvider(LocalTemplateHost provides host, LocalWheelLists provides wheel) {
                    Box(Modifier.fillMaxSize().testTag("list")) {
                        ItemListView(listOf(ListSection(null, items)), androidx.car.app.constraints.ConstraintManager.CONTENT_LIMIT_TYPE_LIST)
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun middleOf(tag: String) = rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.center.y

    @Test fun theFocusedRowComesToTheMiddle() {
        show(wheel = true)
        val middle = middleOf("list")
        // The first row starts in the middle (space above it lets it).
        val first = rule.onNodeWithText("Place 1").fetchSemanticsNode().boundsInRoot.center.y
        assertEquals(middle, first, 40f)
        // The knob: six clicks on from the first row.
        rule.onNodeWithText("Place 1").requestFocus()
        repeat(6) {
            rule.onNode(androidx.compose.ui.test.isFocused()).performKeyInput { pressKey(androidx.compose.ui.input.key.Key.Tab) }
            rule.mainClock.advanceTimeBy(500)
        }
        rule.waitForIdle()
        rule.onNodeWithText("Place 7").assertIsFocused()
        val seventh = rule.onNodeWithText("Place 7").fetchSemanticsNode().boundsInRoot.center.y
        assertEquals("the focused row is in the middle", middle, seventh, 40f)
    }

    @Test fun withoutTheWheelTheListStartsAtTheTop() {
        show(wheel = false)
        val first = rule.onNodeWithText("Place 1").fetchSemanticsNode().boundsInRoot.center.y
        assertTrue("$first", first < middleOf("list") - 100f)
    }
}
