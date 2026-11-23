package net.prototux.aare.host.templates

import androidx.activity.ComponentActivity
import androidx.car.app.OnDoneCallback
import androidx.car.app.model.CarIcon
import androidx.car.app.navigation.model.MessageInfo
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.RoutingInfo
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import net.prototux.aare.ui.car.myplay.LocalNavMessageSink
import net.prototux.aare.ui.car.myplay.NavMessage
import net.prototux.aare.ui.theme.CarTheme
import net.prototux.aare.ui.theme.CarThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Look C (18, 19): the navigation template hands its message to MyPlay's skin; NM-1: the same
 * message sent again in a new template (as maps apps do often) is not handed over again.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w1280dp-h720dp-land-mdpi")
class NavMessageSinkTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val host = object : TemplateHost {
        override val driving = true
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
        override fun mapScroll(dx: Float, dy: Float) = Unit
        override fun mapFling(vx: Float, vy: Float) = Unit
        override fun mapScale(focusX: Float, focusY: Float, factor: Float) = Unit
        override fun mapClick(x: Float, y: Float) = Unit
    }

    private fun arrived() = NavigationTemplate.Builder()
        .setNavigationInfo(MessageInfo.Builder("You have arrived").setText("Work").build())
        .setActionStrip(androidx.car.app.model.ActionStrip.Builder().addAction(androidx.car.app.model.Action.APP_ICON).build())
        .build()

    private fun loading() = NavigationTemplate.Builder()
        .setNavigationInfo(RoutingInfo.Builder().setLoading(true).build())
        .setActionStrip(androidx.car.app.model.ActionStrip.Builder().addAction(androidx.car.app.model.Action.APP_ICON).build())
        .build()

    @Test fun theMessageIsHandedOverOnceAndTakenBack() {
        val handed = ArrayList<NavMessage?>()
        var template by mutableStateOf(arrived())
        var shown by mutableStateOf(true)
        rule.setContent {
            CarTheme(night = true, theme = CarThemes.byId("aare")) {
                CompositionLocalProvider(LocalTemplateHost provides host, LocalNavMessageSink provides { m: NavMessage? -> handed += m }) {
                    if (shown) NavigationTemplateView(template)
                }
            }
        }
        rule.waitForIdle()
        assertEquals(1, handed.size)
        assertEquals("You have arrived", handed.last()?.title)
        assertEquals("Work", handed.last()?.text)
        assertTrue("no image: the spinner", handed.last()!!.loading)
        // NM-1: the same message in a new template: not handed over again.
        rule.runOnIdle { template = arrived() }
        rule.waitForIdle()
        assertEquals(1, handed.size)
        // The way being worked out: the spinner with no words.
        rule.runOnIdle { template = loading() }
        rule.waitForIdle()
        val last = handed.last()!!
        assertTrue(last.loading)
        assertNull(last.title)
        // The template goes: the message is taken back.
        rule.runOnIdle { shown = false }
        rule.waitForIdle()
        assertNull(handed.last())
    }
}
