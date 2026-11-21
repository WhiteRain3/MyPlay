package net.prototux.aare.ui.car

import android.view.KeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.foundation.focusable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.withKeyDown
import net.prototux.aare.car.MirroredKeys
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** MyPlay (G1): the knob and pad reach a mirrored app, down and up, as the mapping says. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalTestApi::class)
class MirroredKeyStopTest {
    @get:Rule val rule = createComposeRule()

    @Test fun keysGoIn() {
        val sent = ArrayList<Pair<Int, Int>>()
        rule.setContent { MirroredKeyStop(MirroredKeys.TurnMode.UP_DOWN, send = { a, c -> sent += a to c }) }
        val stop = rule.onNode(isFocusable())
        stop.requestFocus()
        stop.performKeyInput { pressKey(Key.DirectionRight) }
        stop.performKeyInput { pressKey(Key.Tab) }
        stop.performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } }
        stop.performKeyInput { pressKey(Key.DirectionCenter) }
        val down = KeyEvent.ACTION_DOWN
        val up = KeyEvent.ACTION_UP
        assertEquals(listOf(
            down to KeyEvent.KEYCODE_DPAD_RIGHT, up to KeyEvent.KEYCODE_DPAD_RIGHT,
            down to KeyEvent.KEYCODE_DPAD_DOWN, up to KeyEvent.KEYCODE_DPAD_DOWN,
            down to KeyEvent.KEYCODE_DPAD_UP, up to KeyEvent.KEYCODE_DPAD_UP,
            down to KeyEvent.KEYCODE_DPAD_CENTER, up to KeyEvent.KEYCODE_DPAD_CENTER,
        ), sent)
    }

    /** G1-2: an up whose down did not go in (the turn that brought the ring here) is not sent. */
    @Test fun anUpWithoutItsDownIsNotSent() {
        val sent = ArrayList<Pair<Int, Int>>()
        rule.setContent {
            androidx.compose.foundation.layout.Column {
                androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.testTag("before").focusable())
                androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.weight(1f)) {
                    MirroredKeyStop(MirroredKeys.TurnMode.UP_DOWN, send = { a, c -> sent += a to c })
                }
            }
        }
        rule.onNodeWithTag("before").requestFocus()
        // The turn's down moves the ring onto the stop; its up then arrives there.
        rule.onNodeWithTag("before").performKeyInput { keyDown(Key.Tab) }
        rule.onNodeWithTag("before").performKeyInput { keyUp(Key.Tab) }
        assertEquals(emptyList<Pair<Int, Int>>(), sent)
    }

    /** G1-1: the first time the ring lands on the app, a line says how the knob works and how to leave. */
    @Test fun aHintTheFirstTime() {
        rule.setContent { MirroredKeyStop(MirroredKeys.TurnMode.UP_DOWN, send = { _, _ -> }) }
        rule.onAllNodes(hasTestTag(MIRROR_HINT_TAG)).assertCountEquals(0)
        rule.onNode(isFocusable()).requestFocus()
        rule.onNodeWithTag(MIRROR_HINT_TAG).assertExists()
    }
}
