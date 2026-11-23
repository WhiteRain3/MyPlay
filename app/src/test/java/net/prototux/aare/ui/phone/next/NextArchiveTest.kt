package net.prototux.aare.ui.phone.next

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import net.prototux.aare.Settings
import net.prototux.aare.ui.car.myplay.myplayEngineLanguage
import net.prototux.aare.ui.car.myplay.myplayEnginePreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** More III, second part (plan §16 chunk 11): Archive and About write the same keys as today. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h2400dp-xxhdpi")
class NextArchiveTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val settings get() = Settings(ctx)

    private val pages: @Composable (String) -> Unit = { r -> NextPage(r) }

    @Test fun deckWritesTheDesignLanguage() {
        settings.myplayEnginePreview = true
        rule.setContent { NextApp(start = "more/archive", page = pages) }
        rule.onNode(hasText("Use DECK instead of ORBIT") and isToggleable()).performClick()
        rule.waitForIdle()
        assertEquals("deck", settings.prefs.getString("myplay_engine_language", null))
        rule.onNode(hasText("Use DECK instead of ORBIT") and isToggleable()).performClick()
        rule.waitForIdle()
        assertEquals("orbit", settings.myplayEngineLanguage)
    }

    @Test fun deckWaitsForMyPlaysOwnScreen() {
        settings.myplayEnginePreview = false
        rule.setContent { NextApp(start = "more/archive", page = pages) }
        rule.onNode(hasText("Use DECK instead of ORBIT") and isToggleable()).assertIsNotEnabled()
        rule.onNodeWithText("Needs MyPlay's own screen, in Older layouts").assertExists()
    }

    @Test fun olderLayoutsWriteTodaysKeys() {
        settings.myplayEnginePreview = true
        rule.setContent { NextApp(start = "more/archive/layouts", page = pages) }
        rule.onNode(hasText("Use MyPlay's own screen") and isToggleable()).performClick()
        rule.waitForIdle()
        assertFalse(settings.prefs.getBoolean("myplay_engine_preview", true))
        rule.onNodeWithText("Kind of layout").performClick()
        rule.onNodeWithText("Dock").performClick()
        rule.waitForIdle()
        assertEquals("dock", settings.prefs.getString("style_layout", null))
        val before = settings.showWeather
        rule.onNodeWithTag("next-page-more/archive/layouts").performScrollToNode(hasText("Show the weather"))
        rule.onNode(hasText("Show the weather") and isToggleable()).performClick()
        rule.waitForIdle()
        assertEquals(!before, settings.showWeather)
    }

    @Test fun theKeptPagesOpen() {
        rule.setContent { NextApp(start = "more/archive/layouts/bar", page = pages) }
        rule.onNodeWithText("Kept as it was. Not updated any more.").assertExists()
    }

}
