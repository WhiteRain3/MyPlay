package net.prototux.aare.ui.car

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import net.prototux.aare.car.ButtonAction
import net.prototux.aare.ui.car.GestureLines.Words
import net.prototux.aare.ui.theme.CarTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** MyPlay (R6): the line after a button gesture. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w901dp-h543dp-land-mdpi")
class GestureLinesTest {
    @get:Rule val rule = createComposeRule()

    @Test fun onlyWhatHappensAwayFromTheRingGetsALine() {
        for (a in listOf(ButtonAction.PLAY_PAUSE, ButtonAction.NEXT, ButtonAction.PREVIOUS, ButtonAction.SKIP_SONGS, ButtonAction.ASSISTANT)) {
            assertTrue(a.id, GestureLines.shows(a))
        }
        for (a in listOf(ButtonAction.SELECT, ButtonAction.BACK, ButtonAction.HOME, ButtonAction.HOME_OR_APPS,
            ButtonAction.OPEN_NAVIGATION, ButtonAction.OPEN_MEDIA, ButtonAction.OPEN_PHONE, ButtonAction.NOTHING)) {
            assertFalse(a.id, GestureLines.shows(a))
        }
    }

    @Test fun theWords() {
        fun w(a: ButtonAction, forward: Boolean = true, playing: Boolean? = true) = GestureLines.words(GestureLine(a, forward, 0), playing)
        assertEquals(Words.NEXT, w(ButtonAction.NEXT))
        assertEquals(Words.PREVIOUS, w(ButtonAction.PREVIOUS))
        assertEquals(Words.NEXT, w(ButtonAction.SKIP_SONGS, forward = true))
        assertEquals(Words.PREVIOUS, w(ButtonAction.SKIP_SONGS, forward = false))
        assertEquals(Words.PLAYING, w(ButtonAction.PLAY_PAUSE, playing = true))
        assertEquals(Words.PAUSED, w(ButtonAction.PLAY_PAUSE, playing = false))
        assertEquals(Words.ASSISTANT, w(ButtonAction.ASSISTANT))
        assertFalse(GestureLines.withTitle(Words.ASSISTANT))
        assertTrue(GestureLines.withTitle(Words.NEXT))
    }

    @Test fun theLineShowsWithTheSongThenGoes() {
        val state = PadWalkStates.state(CarLayout.MAP_BEHIND, CarScreen.Dashboard)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CarTheme(night = state.night, theme = state.theme) {
                CarShell(state, PadWalkStates.NoActions, appContent = { _, m -> Box(m.background(Color.DarkGray)) },
                    navMap = { _, m -> Box(m.background(Color.DarkGray)) })
            }
        }
        rule.mainClock.advanceTimeBy(100)
        rule.runOnIdle { state.gestureLine = GestureLine(ButtonAction.NEXT, forward = true, atMs = 1) }
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithTag(GESTURE_LINE_TAG).assertExists()
        val text = rule.onNodeWithTag(GESTURE_LINE_TAG).fetchSemanticsNode().config
            .getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Text)?.firstOrNull()?.text
        assertEquals("Next song: ${PadWalkStates.SONG_TITLE}", text)
        rule.mainClock.advanceTimeBy(GestureLines.SHOW_MS + 200)
        assertTrue(rule.onAllNodes(hasTestTag(GESTURE_LINE_TAG)).fetchSemanticsNodes().isEmpty())
    }
}
