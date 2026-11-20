package net.prototux.aare.ui.car

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import net.prototux.aare.ui.car.CarLayoutSpec.BarSide
import net.prototux.aare.ui.car.CarLayoutSpec.BarStyle
import net.prototux.aare.ui.theme.CarTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * MyPlay: in "map behind", for every allowed bar style, side and "map under the bar", the maps
 * app's picture is drawn once, over the whole screen when it goes under the bar, else over the
 * area beside the bar (SpecPlacement). Drawn by Compose at the Audi's size.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w901dp-h543dp-land-mdpi")
class MapBehindBarsTest {
    @get:Rule val rule = createComposeRule()

    @Test fun theMapIsWhereThePlacementSays() {
        val state = PadWalkStates.state(CarLayout.MAP_BEHIND, CarScreen.Dashboard)
        rule.setContent {
            CarTheme(night = state.night, theme = state.theme) {
                CarShell(state, PadWalkStates.NoActions, appContent = { _, m -> Box(m.background(Color.DarkGray)) },
                    navMap = { _, m -> Box(m.background(Color.Magenta).testTag("map")) })
            }
        }
        var checked = 0
        for (style in BarStyle.entries) for (side in BarSide.entries) for (under in listOf(false, true)) {
            val spec = CarLayoutSpec.DEFAULT.copy(barStyle = style, barSide = side, mapUnderBar = under).allowed()
            if (spec.barSide != side || spec.mapUnderBar != under) continue
            rule.runOnIdle { state.layoutSpec = spec }
            rule.waitForIdle()
            val maps = rule.onAllNodes(hasTestTag("map"), useUnmergedTree = true).fetchSemanticsNodes()
            val want = SpecPlacement.place(spec, 901f, 543f).map
            val name = "$style $side under=$under"
            assertEquals("$name: map drawn", 1, maps.size)
            val got = maps[0].boundsInRoot
            assertEquals("$name: map", listOf(want.left, want.top, want.right, want.bottom), listOf(got.left, got.top, got.right, got.bottom))
            checked++
        }
        // 4 sides x 2 for each of the three free styles, one side x 2 for the dock and the ribbon.
        assertEquals(28, checked)
    }
}
