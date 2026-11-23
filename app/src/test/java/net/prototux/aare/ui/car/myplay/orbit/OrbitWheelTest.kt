package net.prototux.aare.ui.car.myplay.orbit

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Look C, step 3: the orbit wheel composes the ringed row and one either side; rows away from the middle sit left on the arc. */
class OrbitWheelTest {
    @Test fun oneRowEitherSide() {
        assertEquals(0..1, OrbitWheelSpec.window(0f, 10))
        assertEquals(3..5, OrbitWheelSpec.window(4f, 10))
        assertEquals(8..9, OrbitWheelSpec.window(9f, 10))
        // While it turns between two rows, both neighbours stay composed.
        assertEquals(3..6, OrbitWheelSpec.window(4.5f, 10))
        assertTrue(OrbitWheelSpec.window(0f, 0).isEmpty())
        assertEquals(0..0, OrbitWheelSpec.window(0f, 1))
    }

    /** "N more" counts only the rows out of sight (the neighbours show). */
    @Test fun moreCountsOnlyHiddenRows() {
        assertEquals(0 to 5, OrbitWheelSpec.hidden(0, 7))
        assertEquals(0 to 3, OrbitWheelSpec.hidden(1, 6))
        assertEquals(3 to 0, OrbitWheelSpec.hidden(4, 5))
        assertEquals(0 to 0, OrbitWheelSpec.hidden(0, 1))
    }

    @Test fun theArcBendsAwayFromTheMiddle() {
        assertEquals(0f, OrbitWheelSpec.arcX(0.dp).value, 0.01f)
        val one = OrbitWheelSpec.arcX(OrbitWheelSpec.STEP).value
        assertTrue("$one", one > 10f && one < 80f)
        assertEquals(one, OrbitWheelSpec.arcX(-OrbitWheelSpec.STEP).value, 0.01f)
    }
}
