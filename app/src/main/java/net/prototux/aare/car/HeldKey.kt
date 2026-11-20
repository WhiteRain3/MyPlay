package net.prototux.aare.car

/**
 * MyPlay: a car key with a press and a hold action, for Back (press: back, hold: home).
 *
 * Like [LongPressKey], but it also measures the hold itself. Head units may flag a long press (a
 * repeated down, or the up, with longpress set); whether the owner's car does is not known, so a
 * key that stays down for [holdMs] counts as held too. With the flag the hold fires while the key
 * is still down; measured, it fires when the key comes up. Main thread.
 */
internal class HeldKey(
    private val now: () -> Long,
    private val onPress: () -> Unit,
    private val onHold: () -> Unit,
    private val holdMs: Long = HOLD_MS,
) {
    private var downAt = -1L
    /** The hold already fired on a flagged down: its up and further repeats do nothing. */
    private var holdFired = false

    fun onKey(down: Boolean, longPress: Boolean) {
        if (down) {
            // Repeats while held keep the time of the first down.
            if (downAt < 0) { downAt = now(); holdFired = false }
            if (longPress && !holdFired) { holdFired = true; onHold() }
            return
        }
        val held = downAt >= 0 && now() - downAt >= holdMs
        downAt = -1
        when {
            holdFired -> holdFired = false
            longPress || held -> onHold()
            else -> onPress()
        }
    }

    companion object {
        const val HOLD_MS = 700L
    }
}
