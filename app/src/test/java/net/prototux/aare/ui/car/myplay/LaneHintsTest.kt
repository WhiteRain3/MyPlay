package net.prototux.aare.ui.car.myplay

import androidx.car.app.navigation.model.Lane
import androidx.car.app.navigation.model.LaneDirection
import net.prototux.aare.host.NavTextCheck
import net.prototux.aare.ui.car.myplay.orbit.OrbitSkin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** MyPlay (lanes on ORBIT): what is drawn from a maps app's lanes, and what is logged. */
@RunWith(RobolectricTestRunner::class)
class LaneHintsTest {
    private fun lane(vararg dirs: Pair<Int, Boolean>) = Lane.Builder().apply { dirs.forEach { (s, r) -> addDirection(LaneDirection.create(s, r)) } }.build()

    @Test fun theRecommendedDirectionIsDrawnElseTheFirst() {
        val hints = LaneHints.from(listOf(
            lane(LaneDirection.SHAPE_NORMAL_LEFT to false),
            lane(LaneDirection.SHAPE_STRAIGHT to false, LaneDirection.SHAPE_SLIGHT_RIGHT to true),
            lane(LaneDirection.SHAPE_NORMAL_RIGHT to false),
        ))
        assertEquals(listOf(
            LaneHint(LaneDirection.SHAPE_NORMAL_LEFT, false),
            LaneHint(LaneDirection.SHAPE_SLIGHT_RIGHT, true),
            LaneHint(LaneDirection.SHAPE_NORMAL_RIGHT, false),
        ), hints)
    }

    @Test fun nothingWithoutLanesOrWithoutOneToTake() {
        assertTrue(LaneHints.from(null).isEmpty())
        assertTrue(LaneHints.from(emptyList()).isEmpty())
        assertTrue(LaneHints.from(listOf(lane(LaneDirection.SHAPE_STRAIGHT to false))).isEmpty())
        assertTrue(LaneHints.from(List(LaneHints.MAX + 1) { lane(LaneDirection.SHAPE_STRAIGHT to true) }).isEmpty())
    }

    @Test fun theTripsLanesFirst() {
        val a = listOf(LaneHint(LaneDirection.SHAPE_STRAIGHT, true))
        val b = listOf(LaneHint(LaneDirection.SHAPE_NORMAL_LEFT, true))
        assertEquals(a, LaneHints.pick(a, b))
        assertEquals(b, LaneHints.pick(emptyList(), b))
    }

    @Test fun theArrowsFitTheDiscsWidth() {
        for (n in 1..LaneHints.MAX) {
            val total = OrbitSkin.laneArrowSize(n).value * n + 3f * (n - 1) + 16f
            assertTrue("$n lanes: $total", total <= OrbitSkin.LANES_WIDTH.value + 0.5f)
        }
        assertEquals(26f, OrbitSkin.laneArrowSize(2).value)
    }

    @Test fun theLogSaysCountsOnly() {
        assertEquals("nav lanes com.waze trip: 4 lanes, 2 to take, image yes", NavTextCheck.laneLine("com.waze", "trip", 4, 2, true))
    }
}
