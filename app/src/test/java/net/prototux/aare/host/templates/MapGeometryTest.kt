package net.prototux.aare.host.templates

import net.prototux.aare.host.templates.MapGeometry.Box
import org.junit.Assert.assertEquals
import org.junit.Test

class MapGeometryTest {
    private val map = Box(0f, 0f, 1000f, 500f)

    @Test
    fun noObstaclesIsTheWholeMap() {
        assertEquals(map, MapGeometry.unobstructed(map, emptyList()))
    }

    @Test
    fun startColumnAndEndStrips() {
        val card = Box(12f, 12f, 412f, 300f) // routing card, top start
        val eta = Box(12f, 380f, 412f, 488f) // travel estimate, bottom start
        val mapStrip = Box(920f, 250f, 988f, 488f) // map controls, bottom end
        val free = MapGeometry.unobstructed(map, listOf(card, eta, mapStrip))
        assertEquals(Box(412f, 0f, 920f, 500f), free)
    }

    @Test
    fun portraitTopAndBottomCards() {
        val tall = Box(0f, 0f, 600f, 1000f)
        val top = Box(12f, 12f, 588f, 300f)
        val bottom = Box(12f, 880f, 588f, 988f)
        assertEquals(Box(0f, 300f, 600f, 880f), MapGeometry.unobstructed(tall, listOf(top, bottom)))
    }

    @Test
    fun obstaclesOutsideTheMapAreIgnored() {
        assertEquals(map, MapGeometry.unobstructed(map, listOf(Box(-100f, -100f, -10f, -10f))))
    }

    /** MA-1: ORBIT's four corner parts leave a large free middle, not a thin full-width band. */
    @org.junit.Test
    fun cornerPartsLeaveTheMiddleFree() {
        val map = MapGeometry.Box(0f, 0f, 1280f, 720f)
        val turn = MapGeometry.Box(44f, 44f, 374f, 176f)
        val clock = MapGeometry.Box(1100f, 44f, 1236f, 110f)
        val hub = MapGeometry.Box(44f, 485f, 236f, 676f)
        val song = MapGeometry.Box(682f, 520f, 1236f, 676f)
        val free = MapGeometry.unobstructed(map, listOf(turn, clock, hub, song))
        // The old greedy cut's band was 1037x278 (288 286 px).
        org.junit.Assert.assertTrue("free $free", free.area > 1037f * 278f)
        org.junit.Assert.assertTrue("taller than a band: $free", free.height > 400f)
        for (o in listOf(turn, clock, hub, song)) org.junit.Assert.assertNull("$o overlaps $free", o.intersect(free))
    }

    @org.junit.Test
    fun nothingInTheWayIsTheWholeMap() {
        val map = MapGeometry.Box(0f, 0f, 100f, 50f)
        org.junit.Assert.assertEquals(map, MapGeometry.unobstructed(map, emptyList()))
    }
    /**
     * MC-1: ORBIT's hard parts as drawn at 1280x720 (the turn's and the song's discs, the hub, the
     * clock and battery): the free area is centred on the screen, nearly full height, clear of them.
     */
    @org.junit.Test
    fun orbitsFreeAreaIsCentredAt1280() {
        val map = MapGeometry.Box(0f, 0f, 1280f, 720f)
        val hard = listOf(
            MapGeometry.Box(44f, 44f, 176f, 176f), // the turn's disc
            MapGeometry.Box(1141f, 44f, 1236f, 98f), // the clock and battery
            MapGeometry.Box(44f, 464f, 256f, 676f), // the hub and its limit sign
            MapGeometry.Box(1084f, 524f, 1236f, 676f), // the song's disc
        )
        val free = MapGeometry.centred(map, hard, gutter = 32f)
        assertBox(MapGeometry.Box(288f, 43.2f, 992f, 676.8f), free)
        for (o in hard) org.junit.Assert.assertNull("$o overlaps $free", o.intersect(free))
    }

    @org.junit.Test
    fun orbitsFreeAreaIsCentredAt1200() {
        val map = MapGeometry.Box(0f, 0f, 1200f, 720f)
        val hard = listOf(
            MapGeometry.Box(44f, 44f, 168f, 168f),
            MapGeometry.Box(1068f, 44f, 1156f, 95f),
            MapGeometry.Box(44f, 477f, 243f, 676f),
            MapGeometry.Box(1013f, 533f, 1156f, 676f),
        )
        val free = MapGeometry.centred(map, hard, gutter = 30f)
        assertBox(MapGeometry.Box(273f, 43.2f, 927f, 676.8f), free)
    }

    /** MC-1: a part near the middle line narrows the area; then it is shortened to keep its shape, and stays centred. */
    @org.junit.Test
    fun aNearPartKeepsTheShapeAndTheMiddle() {
        val map = MapGeometry.Box(0f, 0f, 1000f, 600f)
        // A tall part close to the middle, low down: the area stops above it rather than become a thin column.
        val part = MapGeometry.Box(420f, 400f, 470f, 600f)
        val free = MapGeometry.centred(map, listOf(part), gutter = 20f)
        org.junit.Assert.assertEquals(500f, (free.left + free.right) / 2, 0.01f)
        org.junit.Assert.assertEquals(300f, (free.top + free.bottom) / 2, 0.01f)
        org.junit.Assert.assertNull(part.intersect(free))
        org.junit.Assert.assertTrue("not flatter than allowed: $free", free.width >= 0.9f * free.height - 0.01f)
        // The tallest that fits: up to 20 px above the part (half height 80).
        assertBox(MapGeometry.Box(0f, 220f, 1000f, 380f), free)
    }

    /** MC-1: a part across the middle line (a strip at the top) only shortens the area, symmetrically. */
    @org.junit.Test
    fun aStripAcrossTheTopShortensBothEnds() {
        val map = MapGeometry.Box(0f, 0f, 1000f, 600f)
        val strip = MapGeometry.Box(0f, 0f, 1000f, 100f)
        val free = MapGeometry.centred(map, listOf(strip), gutter = 20f)
        assertBox(MapGeometry.Box(0f, 120f, 1000f, 480f), free)
    }

    @org.junit.Test
    fun nothingHardIsTheMapLessItsMargins() {
        val map = MapGeometry.Box(0f, 0f, 1000f, 500f)
        assertBox(MapGeometry.Box(0f, 30f, 1000f, 470f), MapGeometry.centred(map, emptyList(), gutter = 20f))
    }

    private fun assertBox(want: MapGeometry.Box, got: MapGeometry.Box) {
        for ((a, b) in listOf(want.left to got.left, want.top to got.top, want.right to got.right, want.bottom to got.bottom))
            org.junit.Assert.assertEquals("want $want got $got", a, b, 0.05f)
    }
}
