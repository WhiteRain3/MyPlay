package net.prototux.aare.ui.phone.next

import org.junit.Assert.assertTrue
import org.junit.Test

/** The style sheet's contrast rules: text >= 4.5:1 on every surface, state-showing non-text parts >= 3:1. */
class NextTokensTest {
    private val surfaces = listOf(NextColors.Bg, NextColors.Surface, NextColors.Surface2, NextColors.Sheet)

    private fun atLeast(min: Double, fg: androidx.compose.ui.graphics.Color, bg: androidx.compose.ui.graphics.Color, what: String) {
        val c = contrast(fg, bg)
        assertTrue("$what is ${"%.2f".format(c)}:1, needs $min", c >= min)
    }

    @Test fun textOnEverySurface() {
        val texts = listOf(NextColors.Text, NextColors.Text2, NextColors.Text3, NextColors.Warn, NextColors.Caution, NextColors.Ok, NextColors.Int) +
            NextTab.entries.map { it.world }
        for (s in surfaces) for (t in texts) atLeast(4.5, t, s, "text $t on $s")
    }

    @Test fun textOnFills() {
        val fills = listOf(NextColors.Int, NextColors.Ok, NextColors.Caution, NextColors.Warn) + NextTab.entries.map { it.world }
        for (f in fills) atLeast(4.5, NextColors.On, f, "On on $f")
        atLeast(4.5, NextColors.SnackAction, NextColors.Snack, "snack action")
        atLeast(4.5, NextColors.On, NextColors.Snack, "snack text")
    }

    @Test fun nonTextControls() {
        for (s in surfaces) {
            atLeast(3.0, NextColors.Outline, s, "switch outline")
            atLeast(3.0, NextColors.SliderRail, s, "slider rail")
            atLeast(3.0, NextColors.Int, s, "focus ring")
        }
    }
}
