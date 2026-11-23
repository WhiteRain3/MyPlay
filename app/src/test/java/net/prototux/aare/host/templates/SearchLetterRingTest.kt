package net.prototux.aare.host.templates

import androidx.activity.ComponentActivity
import androidx.car.app.OnDoneCallback
import androidx.car.app.model.CarIcon
import androidx.car.app.model.SearchTemplate
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import net.prototux.aare.ui.car.myplay.LetterRingInput
import net.prototux.aare.ui.car.myplay.LocalLetterRing
import net.prototux.aare.ui.theme.CarTheme
import net.prototux.aare.ui.theme.CarThemes
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * LR-3 (look C, design 10): a SearchTemplate with its keyboard up shows the skin's letter ring
 * while parked, and neither the ring nor the keyboard while driving; with no ring, the keyboard.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w1280dp-h720dp-land-mdpi")
class SearchLetterRingTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val typed = ArrayList<String>()

    private fun host(driving: Boolean) = object : TemplateHost {
        override val driving = driving
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

    private val template = SearchTemplate.Builder(object : SearchTemplate.SearchCallback {
        override fun onSearchTextChanged(searchText: String) { typed += searchText }
        override fun onSearchSubmitted(searchText: String) { typed += "submit:$searchText" }
    }).setShowKeyboardByDefault(true).setSearchHint("Search Waze").build()

    private var ringInput: LetterRingInput? = null
    private val fakeRing: @androidx.compose.runtime.Composable (LetterRingInput, Modifier) -> Unit = { input, m ->
        ringInput = input
        // LR-5: m already carries SearchUi's tag (the first tag on a node wins): a child of our own.
        Box(m) { Box(Modifier.testTag("fake_ring")) }
    }

    private fun show(driving: Boolean, ring: Boolean) {
        rule.setContent {
            CarTheme(night = true, theme = CarThemes.byId("aare")) {
                CompositionLocalProvider(LocalTemplateHost provides host(driving), LocalLetterRing provides (if (ring) fakeRing else null)) {
                    SearchTemplateView(template)
                }
            }
        }
        rule.waitForIdle()
    }

    @Test fun parkedTheRingTakesTheKeyboardsPlace() {
        show(driving = false, ring = true)
        rule.onNodeWithTag("fake_ring").assertExists()
        rule.onNodeWithTag(LETTER_RING_HOST_TAG).assertExists()
        // The ring types through the template's own sink: the app is told each change.
        rule.runOnIdle { ringInput!!.type("c") }
        assertEquals(listOf("c"), typed)
        assertEquals("Search Waze", ringInput!!.hint)
    }

    @Test fun drivingNeitherTheRingNorTheKeyboard() {
        show(driving = true, ring = true)
        rule.onNodeWithTag("fake_ring").assertDoesNotExist()
        rule.onNodeWithTag(LETTER_RING_HOST_TAG).assertDoesNotExist()
    }

    @Test fun withoutARingTheKeyboardAsBefore() {
        show(driving = false, ring = false)
        rule.onNodeWithTag(LETTER_RING_HOST_TAG).assertDoesNotExist()
    }
}
