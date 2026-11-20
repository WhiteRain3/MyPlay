package net.prototux.aare.ui.phone

import net.prototux.aare.car.CarInputEvent
import net.prototux.aare.car.CarInputLog

/** One thing the car sent, as the Car check page records it (from the car's input events). */
sealed interface CheckInput {
    val channel: Int
    val atMs: Long
    data class Key(override val channel: Int, val code: Int, val down: Boolean, val longPress: Boolean, override val atMs: Long) : CheckInput
    data class Turn(override val channel: Int, val code: Int, val delta: Int, override val atMs: Long) : CheckInput
    data class Absolute(override val channel: Int, val code: Int, val value: Int, override val atMs: Long) : CheckInput
    data class Touchpad(override val channel: Int, override val atMs: Long) : CheckInput
}

/** The car's event as the check records it. */
fun CarInputEvent.toCheckInput(): CheckInput = when (this) {
    is CarInputEvent.Key -> CheckInput.Key(channel, code, down, longPress, atMs)
    is CarInputEvent.Turn -> CheckInput.Turn(channel, code, delta, atMs)
    is CarInputEvent.Absolute -> CheckInput.Absolute(channel, code, value, atMs)
    is CarInputEvent.Touchpad -> CheckInput.Touchpad(channel, atMs)
}

/** The steps of the Car check, in order: what the owner does with each control (R8 needs each one's codes). */
enum class CheckStep(val id: String, val hold: Boolean = false) {
    KNOB_RIGHT("knob_right"), KNOB_LEFT("knob_left"), KNOB_PUSH("knob_push"), KNOB_HOLD("knob_hold", hold = true),
    PAD_UP("pad_up"), PAD_DOWN("pad_down"), PAD_LEFT("pad_left"), PAD_RIGHT("pad_right"),
    MENU("menu"), BACK("back"), BACK_HOLD("back_hold", hold = true),
    WHEEL_SCROLL("wheel_scroll"), WHEEL_CLICK("wheel_click"), WHEEL_HOLD("wheel_hold", hold = true),
}

/** One key during a step: how often it went down and up, the car's long-press flag, and the longest hold. */
data class KeySeen(
    val channel: Int, val code: Int, val downs: Int = 0, val ups: Int = 0, val longPress: Boolean = false,
    /** Downs that came while the key was already down: the car repeating a held key. */
    val repeats: Int = 0, val heldMs: Long = 0, val downAt: Long? = null,
)

/** A run of turns of one code during a step. */
data class TurnSeen(val channel: Int, val code: Int, val forward: Int = 0, val back: Int = 0, val events: Int = 0, val still: Int = 0)

/** Everything the car sent during one step. */
data class StepCapture(
    val keys: List<KeySeen> = emptyList(),
    val turns: List<TurnSeen> = emptyList(),
    val absolutes: Map<Pair<Int, Int>, Int> = emptyMap(), // (channel, code) to the last value
    val touchpad: Boolean = false,
) {
    val isEmpty: Boolean get() = keys.isEmpty() && turns.isEmpty() && absolutes.isEmpty() && !touchpad

    /** This capture with [input] added. */
    fun with(input: CheckInput): StepCapture = when (input) {
        is CheckInput.Key -> {
            val old = keys.firstOrNull { it.channel == input.channel && it.code == input.code } ?: KeySeen(input.channel, input.code)
            val next = if (input.down) {
                old.copy(downs = old.downs + 1, repeats = old.repeats + if (old.downAt != null) 1 else 0,
                    downAt = old.downAt ?: input.atMs, longPress = old.longPress || input.longPress)
            } else {
                val held = old.downAt?.let { input.atMs - it } ?: 0
                old.copy(ups = old.ups + 1, downAt = null, heldMs = maxOf(old.heldMs, held), longPress = old.longPress || input.longPress)
            }
            copy(keys = if (old in keys) keys.map { if (it === old) next else it } else keys + next)
        }
        is CheckInput.Turn -> {
            val old = turns.firstOrNull { it.channel == input.channel && it.code == input.code } ?: TurnSeen(input.channel, input.code)
            val next = old.copy(
                forward = old.forward + maxOf(input.delta, 0), back = old.back + maxOf(-input.delta, 0),
                events = old.events + 1, still = old.still + if (input.delta == 0) 1 else 0,
            )
            copy(turns = if (old in turns) turns.map { if (it === old) next else it } else turns + next)
        }
        is CheckInput.Absolute -> copy(absolutes = absolutes + ((input.channel to input.code) to input.value))
        is CheckInput.Touchpad -> copy(touchpad = true)
    }

    /** What arrived, one line each, for the page and the shared log. [nowMs] closes a key still held. */
    fun lines(nowMs: Long): List<String> =
        keys.map { k ->
            val held = maxOf(k.heldMs, k.downAt?.let { nowMs - it } ?: 0)
            buildString {
                append(CarInputLog.keyName(k.code))
                append(": ${k.downs} down, ${k.ups} up")
                if (held > 0) append(", held ${held} ms")
                if (k.downAt != null) append(" (still down)")
                if (k.repeats > 0) append(", the car repeated the down ${k.repeats} ${if (k.repeats == 1) "time" else "times"} while held")
                if (k.longPress) append(", long press flag")
                append(", channel ${k.channel}")
            }
        } + turns.map { t ->
            "${CarInputLog.keyName(t.code)} turned: ${t.forward} forward, ${t.back} back in ${t.events} ${if (t.events == 1) "event" else "events"}" +
                (if (t.still > 0) ", ${t.still} with no movement" else "") + ", channel ${t.channel}"
        } + absolutes.map { (k, v) -> "${CarInputLog.keyName(k.second)} absolute value $v, channel ${k.first}" } +
            (if (touchpad) listOf("touchpad events") else emptyList())
}

/** The check's findings for the shared log: each step's lines, or that nothing arrived; [titles] names the steps. */
fun checkSummary(captures: Map<CheckStep, StepCapture>, titles: (CheckStep) -> String, nowMs: Long): List<String> =
    CheckStep.entries.flatMap { step ->
        val capture = captures[step]
        when {
            capture == null -> listOf("${titles(step)}: not tried")
            capture.isEmpty -> listOf("${titles(step)}: nothing arrived")
            else -> capture.lines(nowMs).mapIndexed { i, line -> if (i == 0) "${titles(step)}: $line" else "  $line" }
        }
    }
