package net.prototux.aare.ui.phone.next

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import net.prototux.aare.Settings
import net.prototux.aare.ui.phone.Look
import net.prototux.aare.ui.phone.MyPlayLooks
import net.prototux.aare.ui.phone.myplayLooks
import net.prototux.aare.ui.theme.myplayLookColours
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Screen II (plan §16 chunk 5): Looks, Colours and Your own colours. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h2400dp-xxhdpi")
class NextLooksColoursTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val settings get() = Settings(ApplicationProvider.getApplicationContext())
    private val forest = MyPlayLooks.BUILT_IN.first { it.name == "Forest" }

    @Test fun whatALookChanges() {
        assertEquals(listOf(LookPart.COLOURS, LookPart.WALLPAPER, LookPart.OLDER_LAYOUTS), lookChanges(emptyMap<String, Any>(), forest))
        assertEquals(emptyList<LookPart>(), lookChanges(forest.values, forest))
        assertEquals("deck", lookSwitchesDesign(emptyMap<String, Any>(), Look("D", mapOf("myplay_engine_language" to "deck"))))
        assertEquals(null, lookSwitchesDesign(emptyMap<String, Any>(), forest))
    }

    @Test fun applyingALookWritesExactlyItsKeys() {
        settings.carTextSize = "larger"
        settings.wireless = true
        settings.prefs.edit().putString("style_accent", "red").commit()
        rule.setContent { NextLooksPage() }
        rule.onNodeWithText("Forest").performClick()
        rule.onNodeWithText("Use Forest?").assertExists()
        rule.onNodeWithText("Use it").performClick()
        rule.waitForIdle()
        val all = settings.prefs.all
        assertTrue(MyPlayLooks.isCurrent(all, forest))
        // Settings outside a look are untouched.
        assertEquals("larger", settings.carTextSize)
        assertEquals(true, settings.wireless)
    }

    @Test fun aDeckLookAsksFirst() {
        settings.myplayLooks = listOf(Look("Night drive", mapOf("myplay_engine_language" to "deck", "style_accent" to "amber")))
        rule.setContent { NextLooksPage() }
        rule.onNodeWithText("Night drive").performClick()
        rule.onNodeWithText("This look uses DECK").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        rule.waitForIdle()
        assertEquals(null, settings.prefs.getString("myplay_engine_language", null))
    }

    @Test fun deletingALookCanBeUndone() {
        settings.myplayLooks = listOf(Look("Mine", mapOf("style_accent" to "amber")))
        rule.setContent { NextApp(start = "screen/looks") { NextLooksPage() } }
        rule.onNodeWithContentDescription("Mine: more").performClick()
        rule.onNodeWithText("Delete").performClick()
        rule.waitForIdle()
        assertEquals(emptyList<Look>(), settings.myplayLooks)
        rule.onNodeWithText("Undo").performClick()
        rule.waitForIdle()
        assertEquals(listOf("Mine"), settings.myplayLooks.map { it.name })
    }

    @Test fun coloursWriteTodaysKeys() {
        rule.setContent { NextColoursPage() }
        rule.onNodeWithText("Night").performClick()
        assertEquals("night", settings.carThemeId)
        rule.onNodeWithContentDescription("Amber").performClick()
        assertEquals("amber", settings.carAccent)
        rule.onNodeWithText("Light").performClick()
        assertEquals(Settings.THEME_LIGHT, settings.carTheme)
    }

    @Test fun clearingYourColoursCanBeUndone() {
        settings.myplayLookColours = "night.accent=FFFFB000"
        rule.setContent { NextApp(start = "screen/colours/own") { NextOwnColoursPage() } }
        rule.onNodeWithText("Yours · #FFB000").assertExists()
        rule.onNodeWithText("Clear all my colours").performClick()
        rule.waitForIdle()
        assertEquals("", settings.myplayLookColours)
        rule.onNodeWithText("Undo").performClick()
        rule.waitForIdle()
        assertEquals("night.accent=FFFFB000", settings.myplayLookColours)
    }
}
