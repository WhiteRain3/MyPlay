package net.prototux.aare.ui.phone.next

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import net.prototux.aare.Settings
import net.prototux.aare.ui.phone.myplayUpdateAsked
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Setup and search (plan §16 chunk 12). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h2400dp-xxhdpi")
class NextSearchSetupTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val settings get() = Settings(ctx)
    private val text: (Int) -> String = { ctx.getString(it) }

    @Test fun everyPageIsFindableByItsTitle() {
        val entries = NextSearchIndex.entries()
        for (route in NextRoutes.ALL) {
            val title = NextPages.title(route) ?: continue
            assertTrue("$route not found", searchMatches(ctx.getString(title), entries, text).any { it.route == route })
        }
    }

    @Test fun everySettingIsFindableAndOnAKnownPage() {
        val entries = NextSearchIndex.entries()
        for (e in NextSearchIndex.SETTINGS) {
            assertTrue(e.route, NextRoutes.isKnown(e.route))
            assertTrue(ctx.getString(e.label), searchMatches(ctx.getString(e.label), entries, text).any { it.route == e.route && it.label == e.label })
        }
    }

    @Test fun synonymsAndAccentsFind() {
        val entries = NextSearchIndex.entries()
        assertEquals("screen/size", searchMatches("dpi", entries, text).first().route)
        assertTrue(searchMatches("WÄZE", entries, text).any { it.route == "car/maps" })
        assertTrue(searchMatches("   ", entries, text).isEmpty())
    }

    @Test fun aHiddenSettingSaysWhatToTurnOnFirst() {
        settings.autoReply = false
        var opened: String? = null
        rule.setContent { NextSearch(onOpen = { opened = it }, onClose = {}, initial = "reply text") }
        rule.onNodeWithText("first", substring = true).assertExists()
        rule.onNodeWithText("Car › Messages and calls", substring = true).performClick()
        assertEquals("car/messages", opened)
    }

    @Test fun searchOpensFromTheTopBarAndATabClosesIt() {
        rule.setContent { NextApp(start = "more") }
        rule.onNodeWithTag("next-search-button").performClick()
        rule.onNodeWithTag("next-search-field").assertExists()
        rule.onNodeWithTag("next-tab-car").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("next-search").assertDoesNotExist()
    }

    @Test fun setupOnlyOnAFreshInstallUntilDone() {
        assertTrue(showSetup(fresh = true, done = false))
        assertFalse(showSetup(fresh = true, done = true))
        assertFalse(showSetup(fresh = false, done = false))
    }

    @Test fun setupAsksAboutUpdatesOnceAndFinishes() {
        var done = 0
        rule.setContent { NextSetup { done++ } }
        rule.onNodeWithText("1 of 4").assertExists()
        rule.onNodeWithText("Skip for now").performClick()
        rule.onNodeWithText("2 of 4").assertExists()
        rule.onNodeWithText("Skip for now").performClick()
        rule.onNodeWithText("Check for updates?").assertExists()
        rule.onNodeWithText("Yes").performClick()
        rule.waitForIdle()
        assertTrue(settings.checkUpdates)
        assertTrue(settings.myplayUpdateAsked)
        rule.onNodeWithText("Back").performClick()
        rule.onNodeWithText("3 of 4").assertExists()
        rule.onNodeWithText("No").performClick()
        rule.waitForIdle()
        assertFalse(settings.checkUpdates)
        rule.onNodeWithText("Done").performClick()
        assertEquals(1, done)
    }
}
