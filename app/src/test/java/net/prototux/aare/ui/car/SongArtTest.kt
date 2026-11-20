package net.prototux.aare.ui.car

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import net.prototux.aare.ui.theme.CarTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** MyPlay: the song's artwork is on the home screen's music card in every layout (it was missing from the spec layouts' card). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w901dp-h543dp-land-mdpi")
class SongArtTest {
    // R5-3: the key/touch mark is one flag for the whole test process; every test starts and ends on touch.
    @org.junit.Before fun touchInputFirst() { CarFocusVisibility.keyInput = false }
    @org.junit.After fun touchInputAfter() { CarFocusVisibility.keyInput = false }

    @get:Rule val rule = createComposeRule()

    @Test fun mapBehind() = shows(CarLayout.MAP_BEHIND)
    @Test fun taskbar() = shows(CarLayout.TASKBAR)
    @Test fun dock() = shows(CarLayout.DOCK)

    private fun shows(layout: CarLayout) {
        val state = PadWalkStates.state(layout, CarScreen.Dashboard)
        rule.setContent {
            CarTheme(night = state.night, theme = state.theme) {
                CarShell(state, PadWalkStates.NoActions, appContent = { _, m -> Box(m.background(Color.DarkGray)) },
                    navMap = { _, m -> Box(m.background(Color.DarkGray)) })
            }
        }
        rule.waitForIdle()
        val art = rule.onAllNodes(hasTestTag(SONG_ART_TAG), useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue("$layout: no song artwork on the home screen", art.isNotEmpty())
    }
}
