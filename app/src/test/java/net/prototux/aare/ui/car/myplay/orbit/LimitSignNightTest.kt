package net.prototux.aare.ui.car.myplay.orbit

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** MyPlay (polish #2): the limit sign's fill softens at night but stays a clearly readable sign. */
class LimitSignNightTest {
    @Test fun whiteByDaySofterAtNight() {
        assertEquals(Color.White, OrbitSkin.limitSignFill(night = false))
        assertTrue(OrbitSkin.limitSignFill(night = true).luminance() < Color.White.luminance())
    }

    @Test fun theBlackNumberStillReadsOnTheNightFill() {
        // WCAG contrast of the number (0xFF111111) on the night fill: far above 7:1.
        val fill = OrbitSkin.limitSignFill(night = true).luminance() + 0.05f
        val text = Color(0xFF111111).luminance() + 0.05f
        assertTrue("contrast ${fill / text}", fill / text >= 7f)
    }
}
