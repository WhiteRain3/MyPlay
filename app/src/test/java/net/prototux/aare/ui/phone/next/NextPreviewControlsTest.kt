package net.prototux.aare.ui.phone.next

import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Preview's control bar sends what the car would: a down and an up per press, the knob's turns. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w800dp-h400dp-xhdpi")
class NextPreviewControlsTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private class Recorder : PreviewInputs {
        val events = mutableListOf<String>()
        override fun key(code: Int, down: Boolean) { events += "$code ${if (down) "down" else "up"}" }
        override fun rotary(steps: Int) { events += "turn $steps" }
        override fun pretendSpeed(kmh: Float?) { events += "speed $kmh" }
    }

    @Test fun aPushIsADownThenAnUp() {
        val r = Recorder()
        rule.setContent { NextPreviewControls(r, learnedCodes = emptyList()) }
        rule.onNodeWithContentDescription("Push").performTouchInput { down(center) }
        rule.onNodeWithContentDescription("Push").performTouchInput { up() }
        rule.waitForIdle()
        assertEquals(listOf("${KeyEvent.KEYCODE_DPAD_CENTER} down", "${KeyEvent.KEYCODE_DPAD_CENTER} up"), r.events)
    }

    @Test fun theKnobTurnsAndCornersShowOnlyWhenTaught() {
        val r = Recorder()
        rule.setContent { NextPreviewControls(r, learnedCodes = listOf(KeyEvent.KEYCODE_F1)) }
        rule.onNodeWithContentDescription("Turn right").performTouchInput { down(center); up() }
        rule.onNodeWithContentDescription("Corner 1").performTouchInput { down(center); up() }
        rule.onNodeWithContentDescription("Corner 2").assertDoesNotExist()
        rule.waitForIdle()
        assertEquals(listOf("turn 1", "${KeyEvent.KEYCODE_F1} down", "${KeyEvent.KEYCODE_F1} up"), r.events)
        rule.onNodeWithText("Testing aid").assertExists()
    }
}
