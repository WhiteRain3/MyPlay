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
}
