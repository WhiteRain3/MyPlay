package net.prototux.aare.speedlimit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Option A (docs/ideas/speed-limits.md): Lithuania's standard limits where nothing is tagged. */
class StandardLimitsTest {
    private fun rule(tags: Map<String, String>, urban: Boolean = false) =
        LithuaniaRules.rules(tags, fill = true) { urban }.single()

    @Test fun theKetDefaults() {
        assertEquals(50, rule(mapOf("highway" to "tertiary"), urban = true).limit)
        assertEquals(90, rule(mapOf("highway" to "tertiary")).limit)
        assertEquals(70, rule(mapOf("highway" to "unclassified", "surface" to "gravel")).limit)
        assertEquals(120, rule(mapOf("highway" to "trunk", "motorroad" to "yes")).limit)
        // A residential street outside any mapped area: still 50.
        assertEquals(50, rule(mapOf("highway" to "residential")).limit)
        // Service roads get none.
        assertTrue(LithuaniaRules.rules(mapOf("highway" to "service"), fill = true) { true }.isEmpty())
    }

    @Test fun aGuessIsMarkedAndNeverBeatsATag() {
        val g = rule(mapOf("highway" to "secondary"))
        assertTrue(g.flags and SpeedLimitFormat.GUESSED != 0 && g.flags and SpeedLimitFormat.DEFAULTED != 0)
        val tagged = rule(mapOf("highway" to "secondary", "maxspeed" to "70"))
        assertEquals(70, tagged.limit)
        assertEquals(0, tagged.flags and (SpeedLimitFormat.GUESSED or SpeedLimitFormat.DEFAULTED))
        // A zone tag is defaulted but not a guess.
        val zone = rule(mapOf("highway" to "secondary", "zone:maxspeed" to "LT:urban"))
        assertTrue(zone.flags and SpeedLimitFormat.DEFAULTED != 0)
        assertFalse(zone.flags and SpeedLimitFormat.GUESSED != 0)
        // Without fill, as before: an untagged secondary road has no limit.
        assertTrue(LithuaniaRules.rules(mapOf("highway" to "secondary")).isEmpty())
    }

    @Test fun settlementsFromGeoJson() {
        // A square town around (54.70, 25.27) with a hole, and a multipolygon village elsewhere.
        val town = """{"type":"Feature","geometry":{"type":"Polygon","coordinates":[[[25.26,54.69],[25.28,54.69],[25.28,54.71],[25.26,54.71],[25.26,54.69]],[[25.265,54.695],[25.266,54.695],[25.266,54.696],[25.265,54.695]]]},"properties":{"place":"town"}}"""
        val village = """{"type":"Feature","geometry":{"type":"MultiPolygon","coordinates":[[[[24.00,55.00],[24.01,55.00],[24.01,55.01],[24.00,55.01],[24.00,55.00]]],[[[24.10,55.10],[24.11,55.10],[24.11,55.11],[24.10,55.10]]]]},"properties":{"place":"village"}}"""
        val line = """{"type":"Feature","geometry":{"type":"LineString","coordinates":[[25.0,54.0],[25.1,54.1]]},"properties":{}}"""
        assertEquals(1, Settlements.ringsOf(town).size)
        assertEquals(2, Settlements.ringsOf(village).size)
        assertEquals(0, Settlements.ringsOf(line).size)
        val s = Settlements(listOf(town, village, line).flatMap { Settlements.ringsOf(it) })
        assertTrue(s.contains(5_470_000, 2_527_000))
        assertFalse(s.contains(5_480_000, 2_527_000))
        assertTrue(s.contains(5_500_500, 2_400_500))
        assertFalse(s.contains(5_500_500, 2_402_000))
    }

    @Test fun piecesFillOnlyWithSettlements() {
        val way = Opl.Way(mapOf("highway" to "tertiary"), intArrayOf(5_470_000, 5_470_100), intArrayOf(2_527_000, 2_527_100))
        assertTrue(Pieces.of(way).isEmpty())
        val town = """{"geometry":{"type":"Polygon","coordinates":[[[25.26,54.69],[25.28,54.69],[25.28,54.71],[25.26,54.71],[25.26,54.69]]]}}"""
        val inTown = Pieces.of(way, Settlements(Settlements.ringsOf(town)))
        assertEquals(50, inTown.first().limit)
        val outside = Pieces.of(way, Settlements(emptyList()))
        assertEquals(90, outside.first().limit)
    }

    /** The matcher says a guess is a guess, and leaves guesses out when asked. */
    @Test fun theMatcherReportsAndCanLeaveOutGuesses() {
        val guess = Piece(90, 0, SpeedLimitFormat.DEFAULTED or SpeedLimitFormat.GUESSED, 4, intArrayOf(5_470_000, 5_470_100), intArrayOf(2_527_000, 2_527_000))
        val m = Matcher { _, _ -> listOf(guess) }
        val r = m.match(54.70050, 25.27000, 0f, 5f, 6) as Matcher.Result.Known
        assertEquals(90, r.limit)
        assertTrue(r.defaulted && r.guessed)
        m.withGuessed = false
        assertEquals(Matcher.Result.Unknown, m.match(54.70050, 25.27000, 0f, 5f, 6))
    }
}
