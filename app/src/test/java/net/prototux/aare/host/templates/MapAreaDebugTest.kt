package net.prototux.aare.host.templates

import org.junit.Assert.assertEquals
import org.junit.Test

/** MyPlay: the connection log's line for what a maps app is told. */
class MapAreaDebugTest {
    @Test fun aLineSaysTheMapAndBothAreas() {
        assertEquals(
            "map area: map 1280x720 px at 0,0 on the screen; visible 136,0 to 1280,720 (1144x720); stable 136,0 to 900,720 (764x720)",
            MapAreaDebug.line(MapGeometry.Box(0f, 0f, 1280f, 720f), listOf(136, 0, 1280, 720), listOf(136, 0, 900, 720)),
        )
    }

    @Test fun whileTheMapIsMovedTheStableAreaIsNotSent() {
        assertEquals(
            "map area: map 805x543 px at 96,0 on the screen; visible 0,0 to 805,543 (805x543); stable unchanged (moving the map)",
            MapAreaDebug.line(MapGeometry.Box(96f, 0f, 901f, 543f), listOf(0, 0, 805, 543), null),
        )
    }
}
