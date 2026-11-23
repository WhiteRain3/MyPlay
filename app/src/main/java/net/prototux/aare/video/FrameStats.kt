package net.prototux.aare.video

import android.os.Handler
import android.os.HandlerThread
import android.view.FrameMetrics
import android.view.Window

/**
 * MyPlay (smoothness, the dispatcher 2026-11-23): how many of the car screen's frames took too
 * long, for one line in the log at the end of a drive. Numbers only: frames drawn, how many took
 * over [SLOW_MS] (a missed 60 Hz frame) and over [JANK_MS] (a missed 30 Hz frame, a visible
 * stutter), and the slowest. Counted only while [on] (the hidden "maps text" debug setting, as the
 * other bench lines); off, nothing is counted and no line is written.
 *
 * The frames are the car window's own (Window frame metrics), so only frames really drawn count:
 * a still screen adds nothing. The thresholds are 60 Hz and 30 Hz frames whatever the car's
 * screen runs at (the projection is encoded at 30 or 60 frames a second).
 */
internal object FrameStats {
    /** Set from the debug setting by the car runtime, with the maps text check. */
    @Volatile var on = false

    const val SLOW_MS = 16
    const val JANK_MS = 33

    private var frames = 0L
    private var slow = 0L
    private var jank = 0L
    private var worstMs = 0L

    /** One drawn frame of [totalNanos] (FrameMetrics.TOTAL_DURATION). */
    @Synchronized fun add(totalNanos: Long) {
        if (!on || totalNanos <= 0) return
        // FT-2: compared in nanoseconds, so a 16.9 ms frame counts as over 16 ms.
        frames++
        if (totalNanos > SLOW_MS * 1_000_000L) slow++
        if (totalNanos > JANK_MS * 1_000_000L) jank++
        val ms = (totalNanos + 999_999) / 1_000_000
        if (ms > worstMs) worstMs = ms
    }

    /** The drive's line, or null when nothing was counted (the setting off, or no frame drawn). */
    @Synchronized fun line(): String? = if (frames == 0L) null else
        "frames: $frames drawn, $slow over $SLOW_MS ms, $jank over $JANK_MS ms, slowest $worstMs ms"

    @Synchronized fun reset() { frames = 0; slow = 0; jank = 0; worstMs = 0 }

    /**
     * Listens to [window]'s frames on a thread of its own (nothing added to the main thread's
     * frame); returns what stops it.
     */
    fun watch(window: Window): () -> Unit {
        val thread = HandlerThread("car-frame-stats").apply { start() }
        val listener = Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
            add(metrics.getMetric(FrameMetrics.TOTAL_DURATION))
        }
        return runCatching {
            window.addOnFrameMetricsAvailableListener(listener, Handler(thread.looper))
            val stop: () -> Unit = {
                runCatching { window.removeOnFrameMetricsAvailableListener(listener) }
                thread.quitSafely()
            }
            stop
        }.getOrElse { thread.quitSafely(); {} }
    }
}
