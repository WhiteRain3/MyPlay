package net.prototux.aare.ui.car.myplay

import net.prototux.aare.ui.car.myplay.FocusGraph.Box
import net.prototux.aare.ui.car.myplay.FocusGraph.Direction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** MyPlay (N2): the knob and pad moves over the engine's stops. */
class FocusGraphTest {
    // The home's layout on a 1280x720 screen: turn top left, hub bottom left, song bottom right (taller).
    private val home = mapOf(
        "turn" to Box(16f, 16f, 400f, 140f),
        "hub" to Box(16f, 600f, 300f, 704f),
        "song" to Box(900f, 560f, 1264f, 704f),
    )

    @Test fun readingOrderKeepsBottomSlotsOnOneRow() {
        assertEquals(listOf("turn", "hub", "song"), FocusGraph.readingOrder(home))
    }

    @Test fun knobWrapsBothWays() {
        assertEquals("song", FocusGraph.knob(home, "hub", 1))
        assertEquals("turn", FocusGraph.knob(home, "song", 1))
        assertEquals("song", FocusGraph.knob(home, "turn", -1))
        assertEquals("hub", FocusGraph.knob(home, "turn", 4))
        assertEquals("turn", FocusGraph.knob(home, "gone", 1))
        assertNull(FocusGraph.knob(emptyMap(), null, 1))
    }

    @Test fun padGoesToTheNearestStopThatWay() {
        assertEquals("turn", FocusGraph.pad(home, "hub", Direction.UP))
        assertEquals("song", FocusGraph.pad(home, "hub", Direction.RIGHT))
        assertEquals("hub", FocusGraph.pad(home, "song", Direction.LEFT))
        // The hub is lower-middled than the taller song but on the same row: not "up" from the song.
        assertEquals("turn", FocusGraph.pad(home, "song", Direction.UP))
        assertEquals("hub", FocusGraph.pad(home, "turn", Direction.DOWN))
        assertNull(FocusGraph.pad(home, "hub", Direction.LEFT))
        assertNull(FocusGraph.pad(home, "turn", Direction.UP))
    }

    @Test fun padPrefersStraightAheadOverNearerToTheSide() {
        val stops = mapOf(
            "from" to Box(0f, 500f, 100f, 600f),
            "ahead" to Box(0f, 100f, 100f, 200f),
            "side" to Box(300f, 350f, 400f, 450f),
        )
        assertEquals("ahead", FocusGraph.pad(stops, "from", Direction.UP))
    }
}
