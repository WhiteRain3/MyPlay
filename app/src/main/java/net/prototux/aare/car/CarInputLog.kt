package net.prototux.aare.car

import aap_protobuf.service.ServiceOuterClass.Service
import aap_protobuf.service.inputsource.message.InputReportOuterClass.InputReport

/**
 * MyPlay: writes what the car's buttons and knob send into the connection log, the one the owner
 * can read and save on the phone. Nobody knows yet what a given car sends for its knob's press and
 * tilt and for its buttons; this makes a parked test answer that without a computer.
 *
 * Only key codes, directions and counts are written: nothing about the driver, and no touches.
 *
 *  - every key, when it goes down and when it comes up, with its code and whether the car says it
 *    was held;
 *  - knob turns, added up: one line for a run of turns in one direction, not one per click;
 *  - "absolute" inputs (a code and a value), which the app does not use;
 *  - once per session, that a touchpad is sending events, which the app does not use either.
 *
 * At most [maxLines] lines per session, so a long drive does not push the connection's own lines
 * out of the log.
 *
 * The idea of logging the car's keys is from HyperCriSiS's fork of AARE (commit d272542b, "Car
 * keys in the connection log"); this is written anew and also logs the knob and D-pad codes, which
 * that one leaves out.
 */
internal class CarInputLog(
    private val enabled: () -> Boolean,
    private val sink: (String) -> Unit,
    /** Runs the block after about that many milliseconds (to close a run of knob turns). */
    private val later: (Long, () -> Unit) -> Unit,
    private val now: () -> Long,
    private val maxLines: Int = MAX_LINES,
) {
    private class Turns(val channel: Int, val code: Int, val forward: Boolean, var steps: Int, var events: Int, var lastAt: Long)

    private var written = 0
    private var turns: Turns? = null
    private var touchpadSeen = false

    /** One input report from the car, received on input channel [channel]. Any thread. */
    @Synchronized
    fun report(channel: Int, report: InputReport) {
        if (!enabled()) return
        if (report.hasKeyEvent()) {
            flushTurns()
            for (k in report.keyEvent.keysList) write(keyLine(channel, k.keycode, k.down, k.longpress, k.metastate))
        }
        if (report.hasAbsoluteEvent()) {
            flushTurns()
            for (a in report.absoluteEvent.dataList) write(absoluteLine(channel, a.keycode, a.value))
        }
        if (report.hasRelativeEvent()) {
            for (r in report.relativeEvent.dataList) turn(channel, r.keycode, r.delta)
        }
        if (report.hasTouchpadEvent() && !touchpadSeen) {
            touchpadSeen = true
            write("car touchpad is sending events (MyPlay does not use them), input channel $channel")
        }
    }

    /** The session ended: write what is still being added up. */
    @Synchronized
    fun end() = flushTurns()

    private fun turn(channel: Int, code: Int, delta: Int) {
        if (delta == 0) {
            flushTurns()
            write("car knob event with no movement (code $code), input channel $channel")
            return
        }
        val t = now()
        val run = turns
        val forward = delta > 0
        if (run != null && run.channel == channel && run.code == code && run.forward == forward && t - run.lastAt <= RUN_GAP_MS) {
            run.steps += kotlin.math.abs(delta)
            run.events++
            run.lastAt = t
        } else {
            flushTurns()
            turns = Turns(channel, code, forward, kotlin.math.abs(delta), 1, t)
        }
        later(RUN_GAP_MS + 100) { closeIdleRun() }
    }

    @Synchronized
    private fun closeIdleRun() {
        val run = turns ?: return
        if (now() - run.lastAt >= RUN_GAP_MS) flushTurns()
    }

    private fun flushTurns() {
        val run = turns ?: return
        turns = null
        write(turnLine(run.channel, run.code, run.forward, run.steps, run.events))
    }

    private fun write(line: String) {
        if (written > maxLines) return
        written++
        sink(if (written > maxLines) "car input: $maxLines lines written, no more in this session" else line)
    }

    companion object {
        const val MAX_LINES = 200
        /** Turns in one direction closer together than this are one run. */
        const val RUN_GAP_MS = 1500L

        /** Android's key code names for the keys a car is likely to send; the number is always shown too. */
        private val NAMES = mapOf(
            3 to "HOME", 4 to "BACK", 5 to "CALL", 6 to "ENDCALL",
            19 to "DPAD_UP", 20 to "DPAD_DOWN", 21 to "DPAD_LEFT", 22 to "DPAD_RIGHT", 23 to "DPAD_CENTER",
            24 to "VOLUME_UP", 25 to "VOLUME_DOWN", 61 to "TAB", 66 to "ENTER", 79 to "HEADSETHOOK",
            82 to "MENU", 84 to "SEARCH", 85 to "MEDIA_PLAY_PAUSE", 86 to "MEDIA_STOP", 87 to "MEDIA_NEXT",
            88 to "MEDIA_PREVIOUS", 89 to "MEDIA_REWIND", 90 to "MEDIA_FAST_FORWARD", 91 to "MUTE",
            111 to "ESCAPE", 126 to "MEDIA_PLAY", 127 to "MEDIA_PAUSE", 164 to "VOLUME_MUTE",
            219 to "ASSIST", 231 to "VOICE_ASSIST",
            260 to "NAVIGATE_PREVIOUS", 261 to "NAVIGATE_NEXT", 262 to "NAVIGATE_IN", 263 to "NAVIGATE_OUT",
            268 to "DPAD_UP_LEFT", 269 to "DPAD_DOWN_LEFT", 270 to "DPAD_UP_RIGHT", 271 to "DPAD_DOWN_RIGHT",
            // Not an Android key: the code Android Auto head units use for the rotary knob.
            65536 to "ROTARY_CONTROLLER",
        )

        /** "BACK (4)", or "key 999" for a code with no name here. */
        fun keyName(code: Int): String = NAMES[code]?.let { "$it ($code)" } ?: "key $code"

        fun keyLine(channel: Int, code: Int, down: Boolean, longPress: Boolean, metaState: Int): String =
            "car key ${keyName(code)} ${if (down) "down" else "up"}" +
                (if (longPress) ", long press" else "") +
                (if (metaState != 0) ", meta 0x${Integer.toHexString(metaState)}" else "") +
                ", input channel $channel"

        /** Forward is a positive number from the car: the app moves to the next element on it. */
        fun turnLine(channel: Int, code: Int, forward: Boolean, steps: Int, events: Int): String =
            "car knob turned ${if (forward) "forward" else "back"} $steps ${if (steps == 1) "step" else "steps"} " +
                "in $events ${if (events == 1) "event" else "events"} (${keyName(code)}), input channel $channel"

        fun absoluteLine(channel: Int, code: Int, value: Int): String =
            "car absolute input ${keyName(code)} value $value (MyPlay does not use it), input channel $channel"

        /** What one of the car's input services says about itself, for the start of a session. */
        fun serviceLine(service: Service, used: Boolean): String {
            val input = service.inputSourceService
            val touch = input.touchscreenList.joinToString(" and ") { "${it.width}x${it.height}" }.ifEmpty { "none" }
            val pad = input.touchpadList.joinToString(" and ") { "${it.width}x${it.height}" }.ifEmpty { "none" }
            val keys = input.keycodesSupportedList.joinToString(", ") { keyName(it) }.ifEmpty { "none listed" }
            return "car input channel ${service.id}${if (used) "" else " (not used by MyPlay)"}: display ${input.displayId}, " +
                "touch screen $touch, touchpad $pad, keys it says it sends: $keys"
        }
    }
}
