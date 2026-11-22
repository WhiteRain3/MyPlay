package net.prototux.aare.car

import java.util.Locale
import kotlin.math.roundToInt

/**
 * MyPlay (ideas 2026-11-22): one line in the connection log when a drive ends, so a shared log
 * says at a glance whether the speed and the speed limit worked on the owner's roads: how long,
 * how far (from the speed shown), where the speed came from, and how much of the time the limit
 * was known. On purpose never the highest speed or time over the limit: the log is shared.
 *
 * Sampled from the car screen's speed (the engine's speed feed, about once a second) while it is
 * on the screen. A gap longer than [MAX_GAP_MS] (the speed was off the screen) is not counted.
 */
internal class DriveTally {
    private var speedMs = 0L
    private var noSpeedMs = 0L
    private var carMs = 0L
    private var phoneMs = 0L
    private var signedMs = 0L
    private var byRoadMs = 0L
    private var unknownMs = 0L
    private var notLookedUpMs = 0L
    private var metres = 0.0

    /** [reading] held for [dtMs] (already capped). */
    fun add(reading: SpeedReading?, dtMs: Long) {
        if (dtMs <= 0) return
        if (reading == null) { noSpeedMs += dtMs; return }
        speedMs += dtMs
        when (reading.from) {
            SpeedReading.From.CAR -> carMs += dtMs
            SpeedReading.From.PHONE -> phoneMs += dtMs
        }
        when (val l = reading.limit) {
            is LimitShown.Known -> if (l.defaulted) byRoadMs += dtMs else signedMs += dtMs
            LimitShown.Unknown -> unknownMs += dtMs
            null -> notLookedUpMs += dtMs
        }
        metres += reading.kmh / 3.6 * dtMs / 1_000.0
    }

    /** The log line for a drive of [durationMs]. */
    fun line(durationMs: Long): String {
        val minutes = (durationMs / 60_000.0).roundToInt()
        val head = "drive summary: $minutes min connected"
        val seen = speedMs + noSpeedMs
        if (seen == 0L) return "$head; the speed was not on the car screen"
        val km = String.format(Locale.ROOT, "%.1f", metres / 1_000.0)
        return "$head; $km km by the speed shown; speed known ${pc(speedMs, seen)} of the time it was on screen" +
            (if (speedMs == 0L) "" else " (car ${pc(carMs, speedMs)}, phone ${pc(phoneMs, speedMs)}); " +
                "limit: signed ${pc(signedMs, speedMs)}, by road type ${pc(byRoadMs, speedMs)}, " +
                "not known ${pc(unknownMs, speedMs)}, not looked up ${pc(notLookedUpMs, speedMs)}")
    }

    private fun pc(part: Long, whole: Long) = if (whole <= 0) "0 %" else "${(part * 100.0 / whole).roundToInt()} %"

    companion object {
        /** A longer gap between two samples means the speed was off the screen: not counted. */
        const val MAX_GAP_MS = 3_000L
    }
}

/** This drive's tally, fed by the car screen and read when the session ends. Thread-safe. */
internal object DriveSummary {
    private var tally: DriveTally? = null
    private var startedAt = 0L
    private var lastAt: Long? = null

    /** A car session started at [nowMs] (System.currentTimeMillis). */
    @Synchronized fun begin(nowMs: Long) {
        tally = DriveTally()
        startedAt = nowMs
        lastAt = null
    }

    /** The speed now on the car screen (null: none known). Ignored outside a session. */
    @Synchronized fun sample(reading: SpeedReading?, nowMs: Long) {
        val t = tally ?: return
        val dt = lastAt?.let { nowMs - it }
        lastAt = nowMs
        if (dt != null && dt in 1..DriveTally.MAX_GAP_MS) t.add(reading, dt)
    }

    /** The speed left the screen: the next sample starts afresh. */
    @Synchronized fun pause() { lastAt = null }

    /** The session ended at [nowMs]: the line for the log, or null if none started. */
    @Synchronized fun end(nowMs: Long): String? {
        val t = tally ?: return null
        tally = null
        lastAt = null
        return t.line(nowMs - startedAt)
    }
}
