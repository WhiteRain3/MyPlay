package net.prototux.aare.ui.phone.next

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import net.prototux.aare.Settings
import net.prototux.aare.car.ButtonAction
import net.prototux.aare.car.ButtonTable
import net.prototux.aare.car.ButtonTable.Control
import net.prototux.aare.car.ButtonTable.Gesture
import net.prototux.aare.car.GestureEngine
import net.prototux.aare.car.myplayButtons
import net.prototux.aare.car.myplayGestureDoubleMs
import net.prototux.aare.car.myplayLearnButton
import net.prototux.aare.car.myplayLearnedButtons
import net.prototux.aare.ui.car.myplay.HomePad
import net.prototux.aare.ui.car.myplay.HomePadAction
import net.prototux.aare.ui.car.myplay.HomePadKey
import net.prototux.aare.ui.car.myplay.myplayHomePad
import net.prototux.aare.ui.phone.myplayCarSendsHolds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Car I (plan §16 chunk 6): Buttons, pad shortcuts, Timing, Teach, the root. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h2400dp-xxhdpi")
class NextCarTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val settings get() = Settings(ApplicationProvider.getApplicationContext())

    @Test fun choosingAnActionStoresTodaysOverride() {
        rule.setContent { NextButtonsPage() }
        rule.onNodeWithText("Double press").performClick()
        rule.onNodeWithText("Open the phone").performClick()
        rule.waitForIdle()
        val stored = settings.myplayButtons
        assertEquals(ButtonAction.OPEN_PHONE, ButtonTable(ButtonTable.decodeOverrides(stored)).action(Control.KNOB_PUSH, Gesture.DOUBLE))
        // Round trip: the same text the old page writes.
        assertEquals(stored, ButtonTable.encodeOverrides(ButtonTable.decodeOverrides(stored)))
    }

    @Test fun resetUsesUndo() {
        settings.myplayButtons = "knob_push.double=open_phone"
        settings.myplayGestureDoubleMs = 400
        rule.setContent { NextApp(start = "car/buttons") { NextButtonsPage() } }
        rule.onNodeWithText("Reset this page").performClick()
        rule.waitForIdle()
        assertEquals("", settings.myplayButtons)
        assertEquals(GestureEngine.DOUBLE_MS, settings.myplayGestureDoubleMs)
        rule.onNodeWithText("Undo").performClick()
        rule.waitForIdle()
        assertEquals("knob_push.double=open_phone", settings.myplayButtons)
        assertEquals(400L, settings.myplayGestureDoubleMs)
    }

    @Test fun holdsAreShownDisabledWhenTheCarSendsNone() {
        settings.myplayCarSendsHolds = false
        rule.setContent { NextButtonsPage() }
        rule.onNodeWithText("This car doesn't send holds").assertExists()
        rule.onNode(hasText("Press and hold")).assertExists()
        rule.onAllNodes(hasText("Needs holds")).fetchSemanticsNodes().let { assertTrue(it.size >= 2) }
    }

    @Test fun triplePressIsOffered() {
        rule.setContent { NextButtonsPage() }
        rule.onNodeWithText("Triple press").assertExists()
    }

    @Test fun padShortcutsWriteTodaysMapping() {
        rule.setContent { NextPadShortcutsPage() }
        rule.onNodeWithText("Pad down").performClick()
        rule.onNodeWithText("Apps").performClick()
        rule.waitForIdle()
        assertEquals(HomePadAction.APPS, settings.myplayHomePad[HomePadKey.DOWN])
        assertEquals(HomePad.DEFAULT[HomePadKey.UP], settings.myplayHomePad[HomePadKey.UP])
    }

    @Test fun teachNeedsTheCar() {
        rule.setContent { NextTeachPage() }
        rule.onNodeWithText("Connect to the car first").assertExists()
        rule.onNodeWithText("Teach it").assertIsNotEnabled()
        assertEquals(null, settings.myplayLearnButton)
    }

    @Test fun forgettingATaughtButtonCanBeUndone() {
        settings.myplayLearnedButtons = mapOf(84 to ButtonAction.OPEN_MEDIA)
        rule.setContent { NextApp(start = "car/buttons/teach") { NextTeachPage() } }
        rule.onNodeWithContentDescription("Forget", substring = true).performClick()
        rule.waitForIdle()
        assertEquals(emptyMap<Int, ButtonAction>(), settings.myplayLearnedButtons)
        rule.onNodeWithText("Undo").performClick()
        rule.waitForIdle()
        assertEquals(mapOf(84 to ButtonAction.OPEN_MEDIA), settings.myplayLearnedButtons)
    }

    @Test fun theRootCountsChangesAndPassengerModeWritesItsKey() {
        assertEquals(0, buttonsChanged("", HomePad.DEFAULT, false, GestureEngine.HOLD_MS, GestureEngine.DOUBLE_MS))
        assertEquals(2, buttonsChanged("knob_push.double=open_phone", HomePad.DEFAULT, true, GestureEngine.HOLD_MS, GestureEngine.DOUBLE_MS))
        rule.setContent { NextCarRoot() }
        rule.onNodeWithText("Default").assertExists()
        rule.onNode(hasText("Passenger mode") and isToggleable()).performClick()
        assertTrue(settings.ignoreDriving)
    }
}
