package net.prototux.aare.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.prototux.aare.car.myplayLearnButton
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Preview opens with its testing controls, which hide and come back. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w800dp-h400dp-land-xhdpi")
class CarPreviewActivityTest {
    @get:Rule val rule = createAndroidComposeRule<CarPreviewActivity>()

    @Test fun thePreviewNeverTeachesAButton() {
        // Review PR-1: a teach armed on Car › Buttons is cancelled when the Preview comes to the front.
        val settings = net.prototux.aare.Settings(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        rule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
        settings.myplayLearnButton = net.prototux.aare.car.ButtonAction.entries.first { it != net.prototux.aare.car.ButtonAction.NOTHING }
        rule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        org.junit.Assert.assertEquals(null, settings.myplayLearnButton)
    }

    @Test fun theControlsShowAndHide() {
        rule.onNodeWithTag("preview-controls").assertExists()
        rule.onNodeWithTag("preview-toggle").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("preview-controls").assertDoesNotExist()
        rule.onNodeWithTag("preview-toggle").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("preview-controls").assertExists()
    }
}
