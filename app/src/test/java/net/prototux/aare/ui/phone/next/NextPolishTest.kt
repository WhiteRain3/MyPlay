package net.prototux.aare.ui.phone.next

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Polish (plan §16 chunk 13): large screens and large text. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w1280dp-h800dp-xhdpi")
class NextPolishTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun aLargeScreenKeepsTheListBesideThePage() {
        rule.setContent { NextApp(start = "more") }
        rule.onNodeWithTag("next-list-pane").assertDoesNotExist()
        rule.onNodeWithTag("next-tab-more").assertExists()
        rule.onNodeWithText("Updates").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("next-list-pane").assertExists()
        // A second row from the list replaces the page instead of stacking on it: Back goes to the root.
        rule.onNodeWithText("Backup").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("next-back").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("next-list-pane").assertDoesNotExist()
    }

    @Test fun atTwiceTheTextSizeTheValueGoesUnderTheLabel() {
        rule.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, 2f)) {
                NextCard { NextValueRow("Maps", "Last used") {} }
            }
        }
        val label = rule.onNodeWithText("Maps", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val value = rule.onNodeWithText("Last used", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue(value.top >= label.bottom)
    }
}
