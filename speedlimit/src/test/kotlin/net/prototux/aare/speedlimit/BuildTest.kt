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
        // A motorway with no tag: 110 (the lower guess: its winter limit), one way.
        assertEquals(listOf(LithuaniaRules.Rule(110, 0, SpeedLimitFormat.DEFAULTED or SpeedLimitFormat.FORWARD_ONLY)), rules("highway" to "motorway"))
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

    /** Option A: an untagged road gets Lithuania's default where it is: 50 in a settlement, 90 outside, 70 unpaved. */
    @Test fun defaultsByPlace() {
        fun d(inside: Boolean, vararg tags: Pair<String, String>) = LithuaniaRules.defaultLimit(tags.toMap(), inside)
        assertEquals(50, d(true, "highway" to "tertiary"))
        assertEquals(90, d(false, "highway" to "tertiary"))
        assertEquals(50, d(true, "highway" to "unclassified", "surface" to "gravel"))
        assertEquals(70, d(false, "highway" to "unclassified", "surface" to "gravel"))
        assertNull(d(false, "highway" to "track"))
        // A residential street outside a mapped area: still inside the town signs, most likely.
        assertEquals(50, d(false, "highway" to "residential"))
        assertEquals(110, d(false, "highway" to "trunk", "motorroad" to "yes"))
        assertEquals(50, d(true, "highway" to "trunk", "motorroad" to "yes"))
        // No default for service roads, or anything not for cars.
        assertNull(d(true, "highway" to "service"))
        assertNull(d(false, "highway" to "footway"))
        assertEquals(90, d(false, "highway" to "primary_link"))
    }

    /** Option A: a square settlement; a road crossing it gets 50 inside, 90 outside, each piece DEFAULTED. */
    @Test fun piecesTakeTheDefaultWhereTheyAre() {
        val town = Settlements.of(listOf(Settlements.Area(listOf(
            intArrayOf(5400000, 5400000, 5401000, 5401000) to intArrayOf(2400000, 2401000, 2401000, 2400000),
        ))))
        assertTrue(town.contains(5400500, 2400500))
        assertTrue(!town.contains(5402000, 2400500))
        val inside = Opl.Way(mapOf("highway" to "tertiary"), intArrayOf(5400200, 5400800), intArrayOf(2400500, 2400500))
        val outside = Opl.Way(mapOf("highway" to "tertiary"), intArrayOf(5402000, 5403000), intArrayOf(2400500, 2400500))
        assertEquals(listOf(50), Pieces.of(inside, town).map { it.limit })
        assertEquals(listOf(90), Pieces.of(outside, town).map { it.limit })
        assertTrue(Pieces.of(inside, town).all { it.flags and SpeedLimitFormat.DEFAULTED != 0 })
        // Without the areas (the old build), no guess.
        assertTrue(Pieces.of(outside, null).isEmpty())
        // A signed limit always wins.
        val signed = Opl.Way(mapOf("highway" to "tertiary", "maxspeed" to "70"), intArrayOf(5400200, 5400800), intArrayOf(2400500, 2400500))
        assertEquals(listOf(70), Pieces.of(signed, town).map { it.limit })
        assertEquals(0, Pieces.of(signed, town).single().flags and SpeedLimitFormat.DEFAULTED)
        // Service roads stay out.
        assertTrue(Pieces.of(Opl.Way(mapOf("highway" to "service"), intArrayOf(5400200, 5400800), intArrayOf(2400500, 2400500)), town).isEmpty())
    }

    /** osmium's GeoJSON lines: a polygon with a hole, and a multipolygon. */
    @Test fun readsSettlementAreas() {
        val poly = Settlements.area("\u001e" + """{"type":"Feature","geometry":{"type":"Polygon","coordinates":[[[24.0,54.0],[24.01,54.0],[24.01,54.01],[24.0,54.01],[24.0,54.0]],[[24.004,54.004],[24.006,54.004],[24.006,54.006],[24.004,54.006],[24.004,54.004]]]},"properties":{"place":"village"}}""")!!
        assertTrue(poly.contains(5400200, 2400200))
        assertTrue("the hole is outside", !poly.contains(5400500, 2400500))
        val multi = Settlements.area("""{"type":"Feature","geometry":{"type":"MultiPolygon","coordinates":[[[[25.0,55.0],[25.01,55.0],[25.01,55.01],[25.0,55.0]]],[[[26.0,55.0],[26.01,55.0],[26.01,55.01],[26.0,55.0]]]]},"properties":{}}""")!!
        assertTrue(multi.contains(5500200, 2600800))
        assertNull(Settlements.area("""{"type":"Feature","geometry":{"type":"LineString","coordinates":[[24.0,54.0],[24.1,54.1]]}}"""))
    }

    /** SL-1: a village mapped only as a point: a road through it gets 50, one 2 km away 90. */
    @Test fun aVillagePointCountsAsASettlement() {
        val village = Settlements.place("""{"type":"Feature","geometry":{"type":"Point","coordinates":[24.0,54.0]},"properties":{"place":"village","name":"Kaimas"}}""")!!
        assertNull(Settlements.place("""{"type":"Feature","geometry":{"type":"Point","coordinates":[24.0,54.0]},"properties":{"place":"locality"}}"""))
        val s = Settlements.of(emptyList(), listOf(village))
        val through = Opl.Way(mapOf("highway" to "secondary"), intArrayOf(5399800, 5400200), intArrayOf(2400000, 2400000))
        val away = Opl.Way(mapOf("highway" to "secondary"), intArrayOf(5401800, 5402400), intArrayOf(2400000, 2400000))
        assertEquals(listOf(50), Pieces.of(through, s).map { it.limit })
        assertEquals(listOf(90), Pieces.of(away, s).map { it.limit })
        // SL-6: 700 m. East-west reaches as far on the ground: about 0.011 degrees of longitude here.
        assertTrue(s.contains(5400000, 2401000))
        assertTrue(!s.contains(5400000, 2401200))
    }

    /** SL-2, SL-3: an untagged expressway or motorway (or link) is 110, the lower guess, all year; a tagged limit still wins. */
    @Test fun fastRoadDefaults() {
        val none = Settlements.of(emptyList())
        val expressway = Opl.Way(mapOf("highway" to "primary", "motorroad" to "yes"), intArrayOf(5402000, 5403000), intArrayOf(2400500, 2400500))
        val p = Pieces.of(expressway, none).single()
        assertEquals(110, p.limit); assertEquals(0, p.winterLimit)
        val link = Opl.Way(mapOf("highway" to "motorway_link"), intArrayOf(5402000, 5403000), intArrayOf(2400500, 2400500))
        val l = Pieces.of(link, none).single()
        assertEquals(110, l.limit); assertEquals(0, l.winterLimit)
        // A tagged 130 still wins, with its winter 110.
        val signed = Pieces.of(Opl.Way(mapOf("highway" to "motorway", "maxspeed" to "130"), intArrayOf(5402000, 5403000), intArrayOf(2400500, 2400500)), none).single()
        assertEquals(130, signed.limit); assertEquals(110, signed.winterLimit)
        assertEquals(0, Pieces.of(Opl.Way(mapOf("highway" to "primary"), intArrayOf(5402000, 5403000), intArrayOf(2400500, 2400500)), none).single().winterLimit)
    }

    /** Pieces that continue one another with the same limit become one; a different limit, or too many points, does not join. */
    @Test fun continuingPiecesJoin() {
        val a = Piece(50, 0, 0, 5, intArrayOf(5400000, 5400100), intArrayOf(2400000, 2400000))
        val b = Piece(50, 0, 0, 5, intArrayOf(5400100, 5400200), intArrayOf(2400000, 2400000))
        val c = Piece(50, 0, 0, 5, intArrayOf(5400200, 5400300), intArrayOf(2400000, 2400000))
        val other = Piece(90, 0, 0, 5, intArrayOf(5400300, 5400400), intArrayOf(2400000, 2400000))
        val joined = Joiner.join(listOf(c, a, other, b))
        assertEquals(2, joined.size)
        val fifty = joined.single { it.limit == 50 }
        assertEquals(listOf(5400000, 5400100, 5400200, 5400300), fifty.lats.toList())
        // A one-way piece is never reversed: one that ends where the other ends does not join.
        val f1 = Piece(50, 0, SpeedLimitFormat.FORWARD_ONLY, 5, intArrayOf(5400000, 5400100), intArrayOf(2400000, 2400000))
        val f2 = Piece(50, 0, SpeedLimitFormat.FORWARD_ONLY, 5, intArrayOf(5400200, 5400100), intArrayOf(2400000, 2400000))
        assertEquals(2, Joiner.join(listOf(f1, f2)).size)
        // No piece grows past MAX_POINTS.
        val chain = (0 until 40).map { k -> Piece(50, 0, 0, 5, intArrayOf(5400000 + k * 10, 5400000 + (k + 1) * 10), intArrayOf(2400000, 2400000)) }
        val chained = Joiner.join(chain)
        assertTrue(chained.all { it.lats.size <= SpeedLimitFormat.MAX_POINTS })
        assertEquals(41, chained.sumOf { it.lats.size } - (chained.size - 1))
        assertEquals(2, chained.size)
    }

    @Test fun theCoverageTableCountsByClass() {
        val c = Coverage()
        c.add(4, LithuaniaRules.Source.SIGNED); c.add(4, LithuaniaRules.Source.AREA); c.add(4, LithuaniaRules.Source.NONE); c.add(4, LithuaniaRules.Source.AREA)
        assertEquals(2, c.count(4, LithuaniaRules.Source.AREA))
        assertTrue(c.table().lines().any { it.startsWith("tertiary") && it.trim().endsWith("75 %") })
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
        // The untagged motorway: 110, the lower guess.
        assertEquals(setOf(50, 110), all.map { it.limit }.toSet())
        val json = File(dir, "out/speed-limits-lithuania.json").readText()
        assertTrue(json.contains("\"region\":\"lithuania\"") && json.contains("\"format\":1") && json.contains("\"built\":\"2026-11-21\""))
        assertTrue(json.contains("\"bytes\":${bin.size}") && Regex("\"sha256\":\"[0-9a-f]{64}\"").containsMatchIn(json))
        assertTrue(json.contains("openstreetmap.org/copyright"))
        // Option A: with settlement areas, the untagged residential street gets 50 (DEFAULTED).
        val areas = File(dir, "settlements.geojsonseq").apply {
            writeText("""{"type":"Feature","geometry":{"type":"Polygon","coordinates":[[[25.27,54.67],[25.29,54.67],[25.29,54.69],[25.27,54.69],[25.27,54.67]]]},"properties":{}}""" + "\n")
        }
        main(arrayOf("lithuania", opl.path, File(dir, "out2").path, "2026-11-21", areas.path))
        val file2 = SpeedLimitFile(File(dir, "out2/speed-limits-lithuania.bin").readBytes())
        val all2 = (0 until file2.rows).flatMap { r -> (0 until file2.cols).flatMap { c -> file2.tilePieces(r, c) } }.toSet()
        assertTrue(all2.any { it.limit == 50 && it.flags and SpeedLimitFormat.DEFAULTED != 0 && it.lats.first() == 5468000 && it.lons.first() == 2528000 })
    }
}
