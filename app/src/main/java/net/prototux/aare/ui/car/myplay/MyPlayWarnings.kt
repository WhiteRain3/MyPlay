package net.prototux.aare.ui.car.myplay

import androidx.compose.runtime.compositionLocalOf

/**
 * MyPlay (drive warnings): whether the skin's status part shows the ice sign (the car reports 4 °C
 * or less outside and the warnings are on; car/MyPlayDriveWarnings.kt). Provided by the engine round
 * [MyPlaySkin.Status]; a skin that draws no sign ignores it.
 */
val LocalRoadIce = compositionLocalOf { false }

/**
 * MyPlay (#7): whether the skin's speed limit sign pulses once when the limit changes (LP-1: its
 * own switch, myplay_limit_pulse). Provided by the engine round the speed; off in the phone's picture.
 */
val LocalLimitPulse = compositionLocalOf { false }

/**
 * #7: when the limit sign pulses. Only a change from one known limit to another known one (50 to
 * 70), never its first showing in a drive or after "not known", and not again within [COOLDOWN_MS],
 * so a limit flickering at a junction does not keep the sign bouncing.
 */
class LimitPulse {
    private var lastKmh: Int? = null
    private var lastPulseAt: Long? = null

    /** A new limit ([kmh] null: none or not known) at [nowMs]; true when the sign should pulse now. */
    fun update(kmh: Int?, nowMs: Long): Boolean {
        val before = lastKmh
        lastKmh = kmh
        if (kmh == null || before == null || kmh == before) return false
        if (lastPulseAt?.let { nowMs - it < COOLDOWN_MS } == true) return false
        lastPulseAt = nowMs
        return true
    }

    companion object {
        const val COOLDOWN_MS = 5_000L
        /** The whole pulse: up and back, once. */
        const val PULSE_MS = 600
        /** How much larger the sign gets at the top of the pulse. */
        const val SCALE = 1.12f
    }
}

/**
 * Polish #5: the outside temperature under the clock, with the ice sign. [reserve]: its place is
 * kept for the whole drive (the car says it has the sensor, or has sent a reading), so the
 * status column's width does not change when a reading comes or goes; [degrees]: what to show
 * (null: nothing, the place stays empty).
 */
data class OutsideTemp(val reserve: Boolean, val degrees: Int?) {
    companion object {
        val NONE = OutsideTemp(false, null)

        fun of(warnings: Boolean, carHasSensor: Boolean, tempC: Float?): OutsideTemp {
            if (!warnings) return NONE
            val reading = tempC?.takeIf { !it.isNaN() && !it.isInfinite() }
            return OutsideTemp(carHasSensor || reading != null, reading?.let { Math.round(it) })
        }
    }
}

val LocalOutsideTemp = compositionLocalOf { OutsideTemp.NONE }
