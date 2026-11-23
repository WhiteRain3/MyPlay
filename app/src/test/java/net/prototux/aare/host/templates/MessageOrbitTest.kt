package net.prototux.aare.host.templates

import androidx.activity.ComponentActivity
import androidx.car.app.OnDoneCallback
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.MessageTemplate
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import net.prototux.aare.ui.theme.CarTheme
import net.prototux.aare.ui.theme.CarThemes
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Look C (21, 22): a maps app's MessageTemplate under ORBIT (the wheel's look) shows its icon in a
 * ring, or ORBIT's spinner while loading, and never its debug text; elsewhere it is as before.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w1280dp-h720dp-land-mdpi")
class MessageOrbitTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

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
        override fun mapScroll(dx: Float, dy: Float) = Unit
        override fun mapFling(vx: Float, vy: Float) = Unit
        override fun mapScale(focusX: Float, focusY: Float, factor: Float) = Unit
        override fun mapClick(x: Float, y: Float) = Unit
    }

    private fun show(t: MessageTemplate, orbit: Boolean) {
        rule.setContent {
            CarTheme(night = true, theme = CarThemes.byId("aare")) {
                CompositionLocalProvider(LocalTemplateHost provides host, LocalWheelLists provides orbit) {
                    MessageTemplateView(t)
                }
            }
        }
        rule.waitForIdle()
    }

    private fun message(loading: Boolean) = MessageTemplate.Builder("Location is off on your phone.").apply {
        setTitle("Waze")
        setHeaderAction(Action.BACK)
        setDebugMessage("debug: no fix")
        if (loading) setLoading(true) else setIcon(CarIcon.ALERT)
    }.build()

    @Test fun underOrbitTheIconIsInARingAndNoDebugText() {
        show(message(loading = false), orbit = true)
        rule.onNodeWithTag(MESSAGE_ICON_RING_TAG, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Location is off on your phone.").assertExists()
        rule.onNodeWithText("debug: no fix").assertDoesNotExist()
    }

    @Test fun underOrbitLoadingIsOrbitsSpinner() {
        show(MessageTemplate.Builder("Loading your places…").setTitle("Waze").setHeaderAction(Action.BACK).setLoading(true).build(), orbit = true)
        rule.onNodeWithTag(MESSAGE_SPINNER_TAG, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Loading your places…").assertExists()
    }

    @Test fun elsewhereItIsAsBefore() {
        show(message(loading = false), orbit = false)
        rule.onNodeWithTag(MESSAGE_ICON_RING_TAG, useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithText("debug: no fix").assertExists()
    }
}
