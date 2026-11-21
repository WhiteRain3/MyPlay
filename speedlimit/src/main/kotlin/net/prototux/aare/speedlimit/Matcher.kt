package net.prototux.aare.speedlimit

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * MyPlay (plan N20): which road the car is on, and so its limit. Conservative: when it cannot
 * tell (no road near, a poor fix, two close roads with different limits and nothing to choose
 * between them) the answer is [Result.Unknown], shown as "no limit known", never a guess.
 *
 * A road is chosen by distance, then by heading (a road at an angle to the way the car moves
 * scores worse, and a one-way limit only counts in its own direction). Between roads that score
 * about the same, the one the car was on a moment ago wins ([previous]), so a road beside another
 * (a service road, the other half of a dual carriageway) does not make the limit flicker.
 *
 * Not thread-safe: one matcher per stream of fixes.
 */
class Matcher(private val near: (lat: Int, lon: Int) -> List<Piece>) {
    constructor(file: SpeedLimitFile) : this(file::piecesNear)

    sealed interface Result {
        data class Known(val limit: Int, val defaulted: Boolean) : Result
        data object Unknown : Result
    }

    /** The piece matched last time (for ties), or null. */
    var previous: Piece? = null
        private set

    /**
     * The limit at ([lat], [lon]) in degrees, moving along [headingDeg] (from north, clockwise;
     * null when not known, as when stopped), with the fix's [accuracyM] (null: not known), in
     * [month] (1..12, for winter limits).
     */
    fun match(lat: Double, lon: Double, headingDeg: Float?, accuracyM: Float?, month: Int): Result {
        if (accuracyM != null && accuracyM > MAX_ACCURACY_M) return forget()
        val pieces = runCatching { near(Math.round(lat * 1e5).toInt(), Math.round(lon * 1e5).toInt()) }.getOrElse { return forget() }
        val mPerLat = 1.11195e5 / 1e5 // metres per 1e-5 degree of latitude
        val mPerLon = mPerLat * cos(Math.toRadians(lat))
        val y0 = lat * 1e5; val x0 = lon * 1e5
        val scored = ArrayList<Pair<Piece, Double>>()
        for (p in pieces) {
            var best = Double.MAX_VALUE
            for (k in 1 until p.lats.size) {
                // In metres, east (x) and north (y), from the fix.
                val ax = (p.lons[k - 1] - x0) * mPerLon; val ay = (p.lats[k - 1] - y0) * mPerLat
                val bx = (p.lons[k] - x0) * mPerLon; val by = (p.lats[k] - y0) * mPerLat
                val dist = distanceToSegment(ax, ay, bx, by)
                if (dist > MAX_DISTANCE_M) continue
                val penalty = if (headingDeg == null) 0.0 else {
                    if (ax == bx && ay == by) continue
                    val bearing = Math.toDegrees(atan2(bx - ax, by - ay))
                    val d = angle(headingDeg.toDouble(), bearing) // 0 = along the piece's direction
                    val off = when {
                        p.flags and SpeedLimitFormat.FORWARD_ONLY != 0 -> d
                        p.flags and SpeedLimitFormat.BACKWARD_ONLY != 0 -> 180 - d
                        else -> minOf(d, 180 - d)
                    }
                    if (off > MAX_ANGLE_DEG) continue
                    off * METRES_PER_DEGREE
                }
                best = minOf(best, dist + penalty)
            }
            // Without a heading, a one-way limit cannot be told from the other direction's: both
            // stay in, and if their limits differ the answer is unknown.
            if (best < Double.MAX_VALUE) scored += p to best
        }
        if (scored.isEmpty()) return forget()
        val bestScore = scored.minOf { it.second }
        val close = scored.filter { it.second <= bestScore + TIE_M }
        val prev = previous
        val chosen = close.firstOrNull { prev != null && continues(prev, it.first) }?.first
            ?: close.minBy { it.second }.first.takeIf { b -> close.all { it.first.limit == b.limit && it.first.winterLimit == b.winterLimit } }
            ?: return forget()
        previous = chosen
        return Result.Known(chosen.limitIn(month), chosen.flags and SpeedLimitFormat.DEFAULTED != 0)
    }

    private fun forget(): Result {
        previous = null
        return Result.Unknown
    }

    companion object {
        /** A fix worse than this is not used. */
        const val MAX_ACCURACY_M = 35f
        /** A road further than this is not the car's. */
        const val MAX_DISTANCE_M = 25.0
        /** A road at more than this to the car's heading is not the car's. */
        const val MAX_ANGLE_DEG = 50.0
        /** How much an angle to the heading counts against a road, in metres per degree. */
        const val METRES_PER_DEGREE = 0.2
        /** Scores within this are about the same. */
        const val TIE_M = 6.0

        /** The same piece, or the next or last piece of the same road (they share an end, as cut). */
        fun continues(prev: Piece, p: Piece): Boolean {
            if (prev == p) return true
            if (prev.limit != p.limit || prev.flags != p.flags || prev.roadClass != p.roadClass) return false
            fun same(i: Int, j: Int) = prev.lats[i] == p.lats[j] && prev.lons[i] == p.lons[j]
            val a = prev.lats.size - 1; val b = p.lats.size - 1
            return same(a, 0) || same(0, b)
        }

        /** Distance from the origin to the segment a-b, in the same units. */
        fun distanceToSegment(ax: Double, ay: Double, bx: Double, by: Double): Double {
            val dx = bx - ax; val dy = by - ay
            val len2 = dx * dx + dy * dy
            val t = if (len2 == 0.0) 0.0 else ((-ax * dx - ay * dy) / len2).coerceIn(0.0, 1.0)
            val px = ax + t * dx; val py = ay + t * dy
            return sqrt(px * px + py * py)
        }

        /** The angle between two bearings, 0..180. */
        fun angle(a: Double, b: Double): Double {
            val d = abs((a - b) % 360.0)
            return if (d > 180) 360 - d else d
        }
    }
}
