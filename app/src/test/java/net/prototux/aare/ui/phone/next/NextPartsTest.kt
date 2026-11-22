package net.prototux.aare.ui.phone.next

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The parts (plan §11, §16 chunk 1): every control has a name TalkBack can read and a 48 dp target. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h2400dp-xxhdpi")
class NextPartsTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun SemanticsNode.spoken(): String {
        val c = config
        val words = (c.getOrNull(SemanticsProperties.ContentDescription).orEmpty() + c.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text })
        // A merged row reads its children too.
        val children = children.flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty().map { t -> t.text } }
        return (words + children).joinToString(" ").trim()
    }

    @Test fun everyControlHasANameAndA48dpTarget() {
        rule.setContent { NextPartsGallery() }
        val nodes = rule.onAllNodesClickable()
        assertTrue("found no controls", nodes.isNotEmpty())
        val density = rule.density.density
        for (n in nodes) {
            val name = n.spoken()
            assertTrue("a control with no name at ${n.boundsInRoot}", name.isNotEmpty())
            val w = n.touchBoundsInRoot.width / density
            val h = n.touchBoundsInRoot.height / density
            assertTrue("'$name' is ${w}x$h dp, under 48", w >= 47.5f && h >= 47.5f)
        }
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.onAllNodesClickable(): List<SemanticsNode> =
        onAllNodes(hasClickAction()).fetchSemanticsNodes()

    @Test fun swatchesAreNamed() {
        rule.setContent { NextPartsGallery() }
        rule.onNodeWithTag("next-gallery").performScrollToNode(hasContentDescription("Amber"))
        rule.onNodeWithContentDescription("Amber").assertIsNotSelected().performClick().assertIsSelected()
    }

    @Test fun theInfoButtonSaysWhatItIsAbout() {
        rule.setContent { NextPartsGallery() }
        rule.onNodeWithContentDescription("About Passenger mode").performClick()
        rule.onNodeWithText("Lets the car screen work while driving. Only for a passenger.").assertExists()
    }

    @Test fun aPickerShowsTheRealValuesAndPicks() {
        rule.setContent { NextPartsGallery() }
        rule.onNode(hasText("Warn when over by")).performClick()
        rule.onNodeWithText("15 km/h").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("15 km/h").assertExists() // now the row's value
    }

    @Test fun aSwitchRowTogglesAsAWhole() {
        rule.setContent { NextPartsGallery() }
        val row = rule.onNode(hasText("Prefer 60 fps") and androidx.compose.ui.test.isToggleable(), useUnmergedTree = false)
        row.assertIsOff().performClick().assertIsOn()
    }

    @Test fun undoPutsBack() {
        var value = 1
        rule.setContent {
            NextApp(start = "car") {
                NextResetPage({ val before = value; value = 0; { value = before } })
            }
        }
        rule.onNodeWithText("Reset this page").performClick()
        rule.waitForIdle()
        assertTrue(value == 0)
        rule.onNodeWithText("Undo").performClick()
        rule.waitForIdle()
        assertTrue(value == 1)
    }

}
