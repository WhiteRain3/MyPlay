package net.prototux.aare.ui.car

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

class FocusViewAreaTest {
    @Test fun theAreaIsTheElementWithTheMarginAllAround() =
        assertEquals(Rect(-12f, -12f, 212f, 92f), focusViewArea(200, 80, 12f))

    @Test fun noMarginIsTheElementItself() = assertEquals(Rect(0f, 0f, 200f, 80f), focusViewArea(200, 80, 0f))
}
