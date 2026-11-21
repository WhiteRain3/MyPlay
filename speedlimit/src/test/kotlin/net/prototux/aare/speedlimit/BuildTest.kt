package net.prototux.aare.speedlimit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** MyPlay (N19): Lithuania's rules, the OPL reader, cutting ways into pieces, and the file round trip. */
class BuildTest {
    private fun rules(vararg tags: Pair<String, String>) = LithuaniaRules.rules(tags.toMap())

    @Test fun limitsFromTagsZonesAndKinds() {
        assertEquals(listOf(LithuaniaRules.Rule(50, 0, 0)), rules("highway" to "residential", "maxspeed" to "50"))
        assertEquals(listOf(LithuaniaRules.Rule(90, 0, SpeedLimitFormat.DEFAULTED)), rules("highway" to "secondary", "maxspeed:type" to "LT:rural"))
        assertEquals(listOf(LithuaniaRules.Rule(50, 0, 0)), rules("highway" to "tertiary", "maxspeed" to "LT:urban"))
        // A motorway with no tag: 130, 110 in winter, one way.
        assertEquals(listOf(LithuaniaRules.Rule(130, 110, SpeedLimitFormat.DEFAULTED or SpeedLimitFormat.FORWARD_ONLY)), rules("highway" to "motorway"))
        assertEquals(listOf(LithuaniaRules.Rule(20, 0, SpeedLimitFormat.DEFAULTED)), rules("highway" to "living_street"))
        // Unknown: no guess.
        assertTrue(rules("highway" to "residential").isEmpty())
        assertTrue(rules("highway" to "primary", "maxspeed" to "signals").isEmpty())
        // Not a road for cars.
        assertTrue(rules("highway" to "footway", "maxspeed" to "50").isEmpty())
    }

    @Test fun winterAndDirections() {
        assertEquals(110, rules("highway" to "trunk", "maxspeed" to "120").single().winter)
        assertEquals(90, rules("highway" to "primary", "maxspeed" to "130", "maxspeed:conditional" to "90 @ (Nov 1-Mar 31)").single().winter)
        assertEquals(0, rules("highway" to "primary", "maxspeed" to "90").single().winter)
        val two = rules("highway" to "primary", "maxspeed" to "70", "maxspeed:forward" to "50")
        assertEquals(listOf(LithuaniaRules.Rule(50, 0, SpeedLimitFormat.FORWARD_ONLY), LithuaniaRules.Rule(70, 0, SpeedLimitFormat.BACKWARD_ONLY)), two)
        assertEquals(SpeedLimitFormat.BACKWARD_ONLY, rules("highway" to "primary", "maxspeed" to "50", "oneway" to "-1").single().flags)
    }

    @Test fun parsesValues() {
        assertEquals(50, LithuaniaRules.parse("50"))
        assertEquals(50, LithuaniaRules.parse("50 km/h"))
        assertEquals(48, LithuaniaRules.parse("30 mph"))
        assertNull(LithuaniaRules.parse("none"))
        assertNull(LithuaniaRules.parse("walk"))
        assertNull(LithuaniaRules.parse("0"))
    }

    @Test fun readsOplWithLocationsAndEscapes() {
        val w = Opl.way("w7 v3 dV c1 t2020-01-01T00:00:00Z i1 uX Thighway=primary,maxspeed=50,name=Gedimino%20%pr. Nn1x25.27y54.68,n2x25.28y54.69,n3")!!
        assertEquals("Gedimino pr.", w.tags["name"])
        assertEquals(2, w.lats.size) // n3 has no location: left out
        assertEquals(5468000, w.lats[0]); assertEquals(2527000, w.lons[0])
        assertNull(Opl.way("n1 v1 x25.0 y54.0"))
    }

    @Test fun longWaysAreCutIntoPiecesThatFitTheirTiles() {
        // 0.6 degrees of straight motorway, two points only.
        val way = Opl.Way(mapOf("highway" to "motorway"), intArrayOf(5400000, 5460000), intArrayOf(2400000, 2400000))
        val pieces = Pieces.of(way)
        assertTrue(pieces.size > 1)
        for (p in pieces) assertTrue(p.lats.max() - p.lats.min() <= SpeedLimitFormat.MAX_SPAN_E5)
        // Consecutive pieces join up.
        for (k in 1 until pieces.size) assertEquals(pieces[k - 1].lats.last(), pieces[k].lats.first())
        assertEquals(5400000, pieces.first().lats.first()); assertEquals(5460000, pieces.last().lats.last())
    }

    @Test fun theFileRoundTripsAndFindsPiecesNearAPoint() {
        val a = Piece(50, 0, 0, 5, intArrayOf(5468000, 5468100), intArrayOf(2527000, 2527100))
        // Across a tile edge (lat 54.70): stored in both tiles.
        val b = Piece(90, 0, SpeedLimitFormat.DEFAULTED, 3, intArrayOf(5469900, 5470100), intArrayOf(2540000, 2540000))
        val far = Piece(130, 110, SpeedLimitFormat.FORWARD_ONLY, 0, intArrayOf(5590000, 5591000), intArrayOf(2390000, 2391000))
        val bytes = SpeedLimitWriter.write(listOf(a, b, far))
        assertEquals("MPSL", String(bytes, 0, 4)); assertEquals(1, bytes[4].toInt())
        val file = SpeedLimitFile(bytes)
        assertEquals(listOf(a), file.piecesNear(5468050, 2527050).filter { it.limit == 50 })
        assertTrue(file.piecesNear(5469950, 2540000).contains(b))
        assertTrue(file.piecesNear(5470050, 2540000).contains(b))
        assertTrue(file.piecesNear(5468050, 2527050).none { it.limit == 130 })
        assertEquals(110, far.limitIn(12)); assertEquals(130, far.limitIn(7))
    }

    @Test fun aTinyExtractBuildsAFileAndItsManifest() {
        val dir = Files.createTempDirectory("sl").toFile()
        val opl = File(dir, "roads.opl").apply {
            writeText(
                """
                n1 v1 x25.27 y54.68
                w1 v1 Thighway=residential,maxspeed=50 Nn1x25.27y54.68,n2x25.271y54.681
                w2 v1 Thighway=residential Nn3x25.28y54.68,n4x25.281y54.681
                w3 v1 Thighway=motorway Nn5x24.0y54.9,n6x24.1y54.95
                w4 v1 Thighway=footway,maxspeed=50 Nn7x25.3y54.7,n8x25.31y54.71
                """.trimIndent(),
            )
        }
        main(arrayOf("lithuania", opl.path, File(dir, "out").path, "2026-11-21"))
        val bin = File(dir, "out/speed-limits-lithuania.bin").readBytes()
        val file = SpeedLimitFile(bin)
        val all = (0 until file.rows).flatMap { r -> (0 until file.cols).flatMap { c -> file.tilePieces(r, c) } }.toSet()
        assertEquals(setOf(50, 130), all.map { it.limit }.toSet())
        val json = File(dir, "out/speed-limits-lithuania.json").readText()
        assertTrue(json.contains("\"region\":\"lithuania\"") && json.contains("\"format\":1") && json.contains("\"built\":\"2026-11-21\""))
        assertTrue(json.contains("\"bytes\":${bin.size}") && Regex("\"sha256\":\"[0-9a-f]{64}\"").containsMatchIn(json))
        assertTrue(json.contains("openstreetmap.org/copyright"))
    }
}
