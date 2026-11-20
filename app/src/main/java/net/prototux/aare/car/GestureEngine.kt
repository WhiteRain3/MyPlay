package net.prototux.aare.car

/**
 * MyPlay (plan R2): turns the car's key and knob stream into gestures, per key code: a press, a
 * hold, a double press and "hold and turn". Pure: the clock and the timers come from outside, so
 * every gesture is tested without a car.
 *
 * - **Press**: down and up before [Timings.holdMs]. For a key in [doubleCapable], only once no
 *   second press followed within [Timings.doubleMs] (the cost of a double press: a single one waits
 *   that long); for other keys at once, on the up.
 * - **Hold**: the key stays down for [Timings.holdMs], or the car flags the down as a long press,
 *   whichever comes first. Fires once, while still down; its up does nothing more.
 * - **Double**: two presses of a [doubleCapable] key within [Timings.doubleMs].
 * - **Hold and turn**: a knob turn while a key is down. The turn is the hold's (it does not move
 *   the ring), each turn reported with its steps; the key's up then does nothing more.
 *
 * Repeated downs while a key is held (the car's, or the ones fix/aa18-input synthesises) keep the
 * first down's time. A down whose up never came is forgotten once no down for that key has been
 * seen for [Timings.staleMs], so the next press is a press.
 */
internal class GestureEngine(
    private val timings: () -> Timings,
    private val doubleCapable: (code: Int) -> Boolean,
    private val clock: Clock,
    private val emit: (Gesture) -> Unit,
) {
    data class Timings(val holdMs: Long = HOLD_MS, val doubleMs: Long = DOUBLE_MS, val staleMs: Long = STALE_MS)

    sealed interface Gesture {
        val code: Int
        data class Press(override val code: Int) : Gesture
        data class Hold(override val code: Int) : Gesture
        data class Double(override val code: Int) : Gesture
        data class HoldTurn(override val code: Int, val steps: Int) : Gesture
    }

    /** Time and timers, from the main thread's handler in the app, a fake in tests. */
    interface Clock {
        fun now(): Long
        /** Runs [task] after [delayMs]; the returned function cancels it. */
        fun after(delayMs: Long, task: () -> Unit): () -> Unit
    }

    private class Down(val at: Long, var lastSeen: Long, var cancelHold: () -> Unit = {}) {
        /** The hold (or a hold and turn) already fired: the up does nothing. */
        var used = false
    }

    private val downs = HashMap<Int, Down>()
    /** A first press of a double-capable key, waiting to see whether a second follows. */
    private val waiting = HashMap<Int, () -> Unit>()

    fun onKey(code: Int, down: Boolean, carLongPress: Boolean = false) {
        val now = clock.now()
        if (down) {
            var d = downs[code]
            if (d != null && now - d.lastSeen > timings().staleMs) {
                d.cancelHold()
                downs.remove(code)
                d = null
            }
            if (d == null) {
                val fresh = Down(now, now)
                fresh.cancelHold = clock.after(timings().holdMs) { fireHold(code, fresh) }
                downs[code] = fresh
                d = fresh
            } else d.lastSeen = now
            if (carLongPress) fireHold(code, d)
            return
        }
        val d = downs.remove(code)
        d?.cancelHold?.invoke()
        if (d != null && d.used) return
        // The car flags the up only (or sends no down): it was a long press all the same.
        if (carLongPress) { emit(Gesture.Hold(code)); return }
        if (!doubleCapable(code)) { emit(Gesture.Press(code)); return }
        val first = waiting.remove(code)
        if (first != null) {
            first()
            emit(Gesture.Double(code))
        } else {
            waiting[code] = clock.after(timings().doubleMs) {
                waiting.remove(code)
                emit(Gesture.Press(code))
            }
        }
    }

    /**
     * A knob turn of [steps]. True when it belongs to a held key (a hold and turn, reported);
     * false when no key is down, so the turn moves the ring as usual.
     */
    fun onTurn(steps: Int): Boolean {
        if (steps == 0) return false
        val now = clock.now()
        val (code, d) = downs.entries.filter { now - it.value.lastSeen <= timings().staleMs }
            .maxByOrNull { it.value.at }?.toPair() ?: return false
        d.cancelHold()
        d.used = true
        emit(Gesture.HoldTurn(code, steps))
        return true
    }

    private fun fireHold(code: Int, d: Down) {
        if (d.used || downs[code] !== d) return
        d.used = true
        d.cancelHold()
        emit(Gesture.Hold(code))
    }

    companion object {
        const val HOLD_MS = 600L
        const val DOUBLE_MS = 250L
        const val STALE_MS = 5_000L
    }
}
