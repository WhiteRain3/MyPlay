package net.prototux.aare.headunit

/**
 * MyPlay (plan R7): button gestures for `--script`, so the bench can play what a driver does with
 * the car's buttons:
 *  - `hold:CODE,MS`: the key down, held for MS milliseconds, then up (a hold; 600 ms and more is
 *    MyPlay's default hold);
 *  - `double:CODE`: two quick presses, 120 ms apart (MyPlay's double press is within 250 ms);
 *  - `holdturn:CODE,N`: the key held, N knob clicks (one at a time, negative = back), then up.
 *
 * Each becomes a list of steps; [steps] is pure, for tests.
 */
object ScriptGestures {
    sealed interface Step {
        data class Down(val code: Int) : Step
        data class Up(val code: Int) : Step
        data class Turn(val clicks: Int) : Step
        data class Wait(val ms: Long) : Step
    }

    const val DOUBLE_GAP_MS = 120L
    const val TAP_MS = 60L
    const val TURN_GAP_MS = 200L

    /** The steps of a gesture event, or null when [what] is not one (or is malformed). */
    fun steps(what: String): List<Step>? {
        val (name, args) = what.split(':', limit = 2).takeIf { it.size == 2 } ?: return null
        val parts = args.split(',').map { it.trim() }
        fun int(i: Int) = parts.getOrNull(i)?.toIntOrNull()
        return when (name) {
            "hold" -> {
                val code = int(0) ?: return null
                val ms = int(1)?.takeIf { it > 0 } ?: return null
                listOf(Step.Down(code), Step.Wait(ms.toLong()), Step.Up(code))
            }
            "double" -> {
                val code = int(0) ?: return null
                listOf(Step.Down(code), Step.Wait(TAP_MS), Step.Up(code), Step.Wait(DOUBLE_GAP_MS), Step.Down(code), Step.Wait(TAP_MS), Step.Up(code))
            }
            "holdturn" -> {
                val code = int(0) ?: return null
                val n = int(1)?.takeIf { it != 0 } ?: return null
                val one = if (n > 0) 1 else -1
                listOf(Step.Down(code), Step.Wait(TURN_GAP_MS)) +
                    List(kotlin.math.abs(n)) { listOf(Step.Turn(one), Step.Wait(TURN_GAP_MS)) }.flatten() +
                    Step.Up(code)
            }
            else -> null
        }
    }
}
