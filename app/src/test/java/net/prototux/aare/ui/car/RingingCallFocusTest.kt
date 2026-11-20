package net.prototux.aare.ui.car

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import net.prototux.aare.R
import net.prototux.aare.ui.theme.CarTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** MyPlay (R5): a ringing call puts the ring on Answer, so the knob's push answers. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w901dp-h543dp-land-mdpi")
class RingingCallFocusTest {
    // R5-3: the key/touch mark is one flag for the whole test process; every test starts and ends on touch.
    @org.junit.Before fun touchInputFirst() { CarFocusVisibility.keyInput = false }
    @org.junit.After fun touchInputAfter() { CarFocusVisibility.keyInput = false }

    @get:Rule val rule = createComposeRule()

    @Test fun mapBehind() = answerHasTheRing(CarLayout.MAP_BEHIND)
    @Test fun taskbar() = answerHasTheRing(CarLayout.TASKBAR)

    private fun answerHasTheRing(layout: CarLayout) {
        val state = PadWalkStates.state(layout, CarScreen.Dashboard)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CarTheme(night = state.night, theme = state.theme) {
                CarShell(state, PadWalkStates.NoActions, appContent = { _, m -> Box(m.background(Color.DarkGray)) },
                    navMap = { _, m -> Box(m.background(Color.DarkGray)) })
            }
        }
        CarFocusVisibility.keyInput = true
        // Past the home screen's own first-focus requests, then the call comes in.
        rule.mainClock.advanceTimeBy(FocusSettle.GIVE_UP_AFTER_MS + 1_000)
        rule.runOnIdle { state.call = CarCall(ringing = true, name = "Alex", sinceMs = 1) }
        repeat(10) { rule.mainClock.advanceTimeBy(100) }
        val focused = rule.onAllNodes(isFocused()).fetchSemanticsNodes().firstOrNull()
        val label = focused?.let { n ->
            n.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
                ?: n.children.firstNotNullOfOrNull { it.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull() }
        }
        assertEquals(RuntimeEnvironment.getApplication().getString(R.string.phone_action_answer), label)
    }
}
