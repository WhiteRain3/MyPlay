package net.prototux.aare.car

import androidx.core.content.edit
import net.prototux.aare.Settings

/**
 * MyPlay (ideas 2026-11-22): quiet warnings on the car screen about the road and the phone. No
 * sound, nothing that takes the knob: a small sign and, once per drive, a short line at the top.
 * On by default; the setting myplay_drive_warnings turns them off. Backed up.
 */
const val KEY_DRIVE_WARNINGS = "myplay_drive_warnings"
var Settings.myplayDriveWarnings: Boolean
    get() = prefs.getBoolean(KEY_DRIVE_WARNINGS, true)
    set(v) = prefs.edit { putBoolean(KEY_DRIVE_WARNINGS, v) }

/**
 * LP-1: the speed limit sign's one pulse when the limit changes (#7), on its own switch so the
 * motion can be turned off while keeping the warnings. On by default. Backed up.
 */
const val KEY_LIMIT_PULSE = "myplay_limit_pulse"
var Settings.myplayLimitPulse: Boolean
    get() = prefs.getBoolean(KEY_LIMIT_PULSE, true)
    set(v) = prefs.edit { putBoolean(KEY_LIMIT_PULSE, v) }

/**
 * Ice: the outside temperature the car reports (CarData.Environment) at or below [ON_AT_C], as car
 * makers' own clusters warn. It goes off again only above [OFF_ABOVE_C], so a reading wavering
 * around 4 °C does not make the sign blink. No temperature from the car: no warning.
 */
object IceWarning {
    const val ON_AT_C = 4f
    const val OFF_ABOVE_C = 5f

    fun risk(tempC: Float?, shownBefore: Boolean): Boolean = when {
        tempC == null || tempC.isNaN() -> false
        shownBefore -> tempC <= OFF_ABOVE_C
        else -> tempC <= ON_AT_C
    }

    /** The temperature as the line says it: whole degrees. */
    fun degrees(tempC: Float): Int = Math.round(tempC)
}

/**
 * Per drive: whether the ice sign shows now, and whether the line was already shown (once per
 * drive, the first time the sign comes on). Main thread only.
 */
internal class IceTracker {
    var showing = false
        private set
    private var announced = false

    /** A new reading; returns true when the line should be shown now. */
    fun update(tempC: Float?): Boolean {
        showing = IceWarning.risk(tempC, showing)
        if (showing && !announced) { announced = true; return true }
        return false
    }
}
