package net.prototux.aare.speedlimit

import net.prototux.aare.speedlimit.Matcher.Result.Known
import net.prototux.aare.speedlimit.Matcher.Result.Unknown
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.cos

/** MyPlay (N20): which road, and so which limit; unknown whenever it cannot tell. */
class MatcherTest {
    // Roads drawn in metres east (x) and north (y) of a point in Vilnius.
    private val lat0 = 54.68; private val lon0 = 25.27
    private val mPerDegLat = 111_195.0
    private val mPerDegLon = mPerDegLat * cos(Math.toRadians(lat0))
    private fun lat(y: Double) = lat0 + y / mPerDegLat
    private fun lon(x: Double) = lon0 + x / mPerDegLon
    private fun road(limit: Int, vararg xy: Pair<Double, Double>, flags: Int = 0, winter: Int = 0, cls: Int = 3) = Piece(
        limit, winter, flags, cls,
        xy.map { Math.round(lat(it.second) * 1e5).toInt() }.toIntArray(),
        xy.map { Math.round(lon(it.first) * 1e5).toInt() }.toIntArray(),
    )
    private fun matcher(vararg pieces: Piece) = Matcher(SpeedLimitFile(SpeedLimitWriter.write(pieces.toList())))
    private fun Matcher.at(x: Double, y: Double, heading: Float? = null, accuracy: Float? = 5f, month: Int = 6) =
        match(lat(y), lon(x), heading, accuracy, month)

    @Test fun onARoad() {
        val m = matcher(road(50, -500.0 to 0.0, 500.0 to 0.0))
        assertEquals(Known(50, false), m.at(10.0, 3.0, heading = 90f))
        assertEquals(Known(50, false), m.at(10.0, 3.0)) // stopped: no heading
    }

    @Test fun nothingNearOrAPoorFixIsUnknown() {
        val m = matcher(road(50, -500.0 to 0.0, 500.0 to 0.0))
        assertEquals(Unknown, m.at(0.0, 40.0, heading = 90f))
        assertEquals(Unknown, m.at(0.0, 2.0, heading = 90f, accuracy = 60f))
        assertEquals(Unknown, m.at(2000.0, 0.0, heading = 90f))
    }

    @Test fun parallelRoadsWithDifferentLimits() {
        // A 90 road and a 50 service road beside it, 14 m apart.
        val main = road(90, -500.0 to 0.0, 500.0 to 0.0)
        val service = road(50, -500.0 to 14.0, 500.0 to 14.0, cls = 5)
        val m = matcher(main, service)
        // Halfway between, never matched before: cannot tell.
        assertEquals(Unknown, m.at(0.0, 7.0, heading = 90f))
        // Clearly on the main road, then drifting towards the service road: stays on the main road.
        assertEquals(Known(90, false), m.at(-100.0, 1.0, heading = 90f))
        assertEquals(Known(90, false), m.at(-50.0, 8.0, heading = 90f))
        // Clearly on the service road.
        val n = matcher(main, service)
        assertEquals(Known(50, false), n.at(0.0, 13.0, heading = 90f))
    }

    @Test fun aJunction() {
        // An east-west 90 road; a 50 road going north from it at x = 0.
        val m = matcher(road(90, -500.0 to 0.0, 0.0 to 0.0, 500.0 to 0.0), road(50, 0.0 to 0.0, 0.0 to 500.0))
        assertEquals(Known(90, false), m.at(2.0, 1.0, heading = 90f))
        val n = matcher(road(90, -500.0 to 0.0, 0.0 to 0.0, 500.0 to 0.0), road(50, 0.0 to 0.0, 0.0 to 500.0))
        assertEquals(Known(50, false), n.at(1.0, 2.0, heading = 0f))
        // At the junction, stopped: either road.
        val o = matcher(road(90, -500.0 to 0.0, 0.0 to 0.0, 500.0 to 0.0), road(50, 0.0 to 0.0, 0.0 to 500.0))
        assertEquals(Unknown, o.at(1.0, 1.0))
        // Turning off the main road onto the side road: the side road once the heading says so.
        val p = matcher(road(90, -500.0 to 0.0, 0.0 to 0.0, 500.0 to 0.0), road(50, 0.0 to 0.0, 0.0 to 500.0))
        assertEquals(Known(90, false), p.at(-30.0, 0.0, heading = 90f))
        assertEquals(Known(50, false), p.at(1.0, 20.0, heading = 5f))
    }

    @Test fun oneWayLimitsCountOnlyInTheirDirection() {
        // The same road, 50 going east, 70 going west.
        val east = road(50, -500.0 to 0.0, 500.0 to 0.0, flags = SpeedLimitFormat.FORWARD_ONLY)
        val west = road(70, -500.0 to 0.0, 500.0 to 0.0, flags = SpeedLimitFormat.BACKWARD_ONLY)
        assertEquals(Known(50, false), matcher(east, west).at(0.0, 2.0, heading = 85f))
        assertEquals(Known(70, false), matcher(east, west).at(0.0, 2.0, heading = 268f))
        assertEquals(Unknown, matcher(east, west).at(0.0, 2.0))
        // Driving the wrong way up a one-way road: not its limit.
        assertEquals(Unknown, matcher(east).at(0.0, 2.0, heading = 270f))
    }

    @Test fun aLongRoadCutIntoPiecesStaysTheSameRoad() {
        val way = Opl.Way(mapOf("highway" to "primary", "maxspeed" to "70"), intArrayOf(5468000, 5478000), intArrayOf(2527000, 2527000))
        val pieces = Pieces.of(way)
        val m = Matcher(SpeedLimitFile(SpeedLimitWriter.write(pieces)))
        for (k in 0..20) assertEquals(Known(70, false), m.match(54.68 + k * 0.005, 25.27, 0f, 5f, 6))
    }

    @Test fun winterAndDefaultedLimits() {
        val m = matcher(road(130, -500.0 to 0.0, 500.0 to 0.0, flags = SpeedLimitFormat.DEFAULTED or SpeedLimitFormat.FORWARD_ONLY, winter = 110, cls = 0))
        assertEquals(Known(110, true), m.at(0.0, 1.0, heading = 90f, month = 1))
        assertEquals(Known(130, true), m.at(0.0, 1.0, heading = 90f, month = 7))
    }

    @Test fun aBrokenFileIsUnknown() {
        val m = Matcher { _, _ -> throw IllegalArgumentException("bad tile") }
        assertEquals(Unknown, m.match(54.68, 25.27, 0f, 5f, 6))
    }

    /** Option A: the matcher says a guess is a guess, and leaves guesses out when asked (the owner's switch off). */
    @Test fun guessesAreMarkedAndCanBeLeftOut() {
        val guess = Piece(90, 0, SpeedLimitFormat.DEFAULTED or SpeedLimitFormat.GUESSED, 4, intArrayOf(5_470_000, 5_470_100), intArrayOf(2_527_000, 2_527_000))
        val m = Matcher { _, _ -> listOf(guess) }
        val r = m.match(54.70050, 25.27000, 0f, 5f, 6) as Matcher.Result.Known
        org.junit.Assert.assertEquals(90, r.limit)
        org.junit.Assert.assertTrue(r.defaulted && r.guessed)
        m.withGuessed = false
        org.junit.Assert.assertEquals(Matcher.Result.Unknown, m.match(54.70050, 25.27000, 0f, 5f, 6))
    }
}
