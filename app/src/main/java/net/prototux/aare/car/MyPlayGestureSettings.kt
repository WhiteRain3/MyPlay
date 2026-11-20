package net.prototux.aare.car

import androidx.core.content.edit
import net.prototux.aare.Settings

/**
 * MyPlay (plan R2): how long a hold and how quick a double press are, for the car's keys (keys
 * start with "myplay_gesture_"). Read together as [gestureTimings]; out-of-range values are kept
 * inside [HOLD_RANGE] and [DOUBLE_RANGE].
 */

val HOLD_RANGE = 300L..2_000L
val DOUBLE_RANGE = 150L..600L

var Settings.myplayGestureHoldMs: Long
    get() = prefs.getLong("myplay_gesture_hold_ms", GestureEngine.HOLD_MS).coerceIn(HOLD_RANGE)
    set(v) = prefs.edit { putLong("myplay_gesture_hold_ms", v.coerceIn(HOLD_RANGE)) }

var Settings.myplayGestureDoubleMs: Long
    get() = prefs.getLong("myplay_gesture_double_ms", GestureEngine.DOUBLE_MS).coerceIn(DOUBLE_RANGE)
    set(v) = prefs.edit { putLong("myplay_gesture_double_ms", v.coerceIn(DOUBLE_RANGE)) }

internal val Settings.gestureTimings: GestureEngine.Timings
    get() = GestureEngine.Timings(holdMs = myplayGestureHoldMs, doubleMs = myplayGestureDoubleMs)
