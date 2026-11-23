package net.prototux.aare.ui.phone.next

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import net.prototux.aare.SessionAapLog
import net.prototux.aare.Settings
import net.prototux.aare.ui.phone.CheckInput
import net.prototux.aare.ui.phone.CheckStep
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** More II (plan §16 chunk 10): Diagnostics, the Car check's run, Car data, the log. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h2400dp-xxhdpi")
class NextDiagnosticsTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val settings get() = Settings(ctx)
    private val total = CheckStep.entries.size

    @Before fun clean() = CarCheckRun.resetForTest()
    @After fun after() { CarCheckRun.resetForTest(); settings.prefs.edit { remove(CarCheckRun.KEY_RUNNING) } }

    private val pages: @Composable (String) -> Unit = { r ->
        when (r) {
            "more/diagnostics" -> NextDiagnosticsPage()
            "more/diagnostics/car-check" -> NextCarCheckPage()
            "more/diagnostics/log" -> NextLogPage()
            else -> NextPlaceholder(r)
        }
    }

    @Test fun statusWords() {
        assertEquals(CarCheckStatus.NotRun, carCheckStatus(0, total, false, false, false, null))
        assertEquals(CarCheckStatus.InProgress(4, total), carCheckStatus(4, total, true, true, false, null))
        assertEquals(CarCheckStatus.Stopped, carCheckStatus(0, total, false, false, true, null))
        assertEquals(CarCheckStatus.Stopped, carCheckStatus(3, total, false, true, false, null))
        assertEquals(CarCheckStatus.Done(5L), carCheckStatus(total, total, false, true, false, 5L))
    }

    @Test fun theRunRecordsEachStepAndFinishes() {
        val inputs = MutableSharedFlow<CheckInput>(extraBufferCapacity = 8)
        CarCheckRun.start(settings, inputs)
        assertTrue(CarCheckRun.running.value)
        assertTrue(settings.prefs.getBoolean(CarCheckRun.KEY_RUNNING, false))
        CarCheckRun.add(CheckInput.Turn(0, 65536, 1, 10))
        CarCheckRun.next(settings)
        assertEquals(1, CarCheckRun.step.value)
        assertFalse(CarCheckRun.captures.value.getValue(CheckStep.KNOB_RIGHT).isEmpty)
        repeat(total - 1) { CarCheckRun.next(settings) }
        assertTrue(CarCheckRun.done)
        assertFalse(CarCheckRun.running.value)
        assertTrue(CarCheckRun.finishedAt.value != null)
        // Done: the run is over, so nothing says it was stopped.
        assertFalse(settings.prefs.contains(CarCheckRun.KEY_RUNNING))
        // "Nothing happened" left an empty capture for each skipped step.
        assertTrue(CarCheckRun.captures.value.getValue(CheckStep.KNOB_LEFT).isEmpty)
    }

    @Test fun theRunSurvivesPageAndTabChanges() {
        CarCheckRun.start(settings, MutableSharedFlow())
        CarCheckRun.next(settings)
        CarCheckRun.next(settings)
        rule.setContent { NextApp(start = "more/diagnostics/car-check", page = pages) }
        rule.onNodeWithText("3 of $total").assertExists()
        rule.onNodeWithTag("next-tab-home").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("next-tab-more").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("3 of $total").assertExists()
        assertEquals(2, CarCheckRun.step.value)
        assertTrue(CarCheckRun.running.value)
    }

    @Test fun afterTheProcessDiedTheCheckSaysStopped() {
        // A run was going on in a process that is gone: the setting stayed, the holder is empty.
        settings.prefs.edit { putBoolean(CarCheckRun.KEY_RUNNING, true) }
        assertTrue(CarCheckRun.stoppedEarlier(settings))
        rule.setContent { NextApp(start = "more/diagnostics", page = pages) }
        rule.onNodeWithText("Stopped").assertExists()
        // Opening the check is seeing it: the card goes.
        rule.onNodeWithText("Car check").performClick()
        rule.waitForIdle()
        assertFalse(CarCheckRun.stoppedEarlier(settings))
    }

    @Test fun clearingWhileRunningKeepsTheMark() {
        CarCheckRun.start(settings, MutableSharedFlow())
        CarCheckRun.clearStopped(settings)
        assertTrue(settings.prefs.getBoolean(CarCheckRun.KEY_RUNNING, false))
    }

    @Test fun alertTextsAreUntickedEachTime() {
        CarCheckRun.goTo(total)
        rule.setContent { NextApp(start = "more/diagnostics/car-check", page = pages) }
        rule.onNodeWithText("All steps done").assertExists()
        rule.onNodeWithText("Include alert texts").assertIsOff().performClick()
        rule.onNodeWithText("Include alert texts").assertIsOn()
        rule.onNodeWithTag("next-tab-home").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("next-tab-more").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Include alert texts").assertIsOff()
    }

    @Test fun theDetailedLogSwitchAlsoTurnsTheTraceOn() {
        val before = SessionAapLog.verbose
        try {
            rule.setContent { NextApp(start = "more/diagnostics/log", page = pages) }
            rule.onNodeWithText("Detailed protocol log").performClick()
            rule.waitForIdle()
            assertEquals(!before, settings.verboseProtocolLog)
            assertEquals(!before, SessionAapLog.verbose)
        } finally {
            SessionAapLog.verbose = before
        }
    }
}
