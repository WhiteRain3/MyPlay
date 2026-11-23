package net.prototux.aare.video

import android.os.Handler
import android.os.HandlerThread
import android.view.FrameMetrics
import android.view.Window

/**
 * MyPlay (smoothness, the dispatcher 2026-11-23): how many of the car screen's frames took too
 * long, for one line in the log at the end of a drive. Numbers only: frames drawn, how many took
 * longer than one refresh of the display and longer than two (a visible stutter), and the slowest. Counted only while [on] (the hidden "maps text" debug setting, as the
 * other bench lines); off, nothing is counted and no line is written.
 *
 * The frames are the car window's own (Window frame metrics), so only frames really drawn count:
 * a still screen adds nothing. The thresholds come from the refresh rate of the display the
 * window is on (FT-2), 60 Hz when it gives none.
 */
internal object FrameStats {
    /** Set from the debug setting by the car runtime, with the maps text check. */
    @Volatile var on = false

    /** FT-2: the refresh rate assumed until the window's display tells its own. */
    const val DEFAULT_HZ = 60f

    /** One frame's time at the display's refresh rate ([setRefreshRate]), in nanoseconds. */
    @Volatile private var budgetNanos = budget(DEFAULT_HZ)
    @Volatile private var hz = DEFAULT_HZ

    private var frames = 0L
    private var slow = 0L
    private var jank = 0L
    private var worstNanos = 0L

    private fun budget(rate: Float): Long = (1_000_000_000.0 / rate).toLong()

    /**
     * FT-2: the thresholds follow the display the car screen is drawn on: a frame is slow over one
     * refresh ([budgetNanos], 16.7 ms at 60 Hz) and a stutter over two (33.3 ms). A rate the display
     * does not give sensibly (0, or out of 20..240 Hz) keeps [DEFAULT_HZ].
     */
    fun setRefreshRate(rate: Float) {
        val r = if (rate.isFinite() && rate in 20f..240f) rate else DEFAULT_HZ
        hz = r
        budgetNanos = budget(r)
    }

    /** One drawn frame of [totalNanos] (FrameMetrics.TOTAL_DURATION); compared in nanoseconds (FT-2). */
    @Synchronized fun add(totalNanos: Long) {
        if (!on || totalNanos <= 0) return
        frames++
        if (totalNanos > budgetNanos) slow++
        if (totalNanos > 2 * budgetNanos) jank++
        if (totalNanos > worstNanos) worstNanos = totalNanos
    }

    private fun ms(nanos: Long) = String.format(java.util.Locale.ROOT, "%.1f", nanos / 1_000_000.0)

    /** The drive's line, or null when nothing was counted (the setting off, or no frame drawn). */
    @Synchronized fun line(): String? = if (frames == 0L) null else
        "frames at ${Math.round(hz)} Hz: $frames drawn, $slow over ${ms(budgetNanos)} ms, $jank over ${ms(2 * budgetNanos)} ms, " +
            "slowest ${ms(worstNanos)} ms"

    @Synchronized fun reset() { frames = 0; slow = 0; jank = 0; worstNanos = 0 }

    /**
     * Listens to [window]'s frames on a thread of its own (nothing added to the main thread's
     * frame); returns what stops it.
     */
    fun watch(window: Window): () -> Unit {
        @Suppress("DEPRECATION") // Window.getContext().display needs API 30; minSdk is 26.
        setRefreshRate(runCatching { window.windowManager.defaultDisplay.refreshRate }.getOrDefault(DEFAULT_HZ))
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
