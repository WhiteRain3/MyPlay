package net.prototux.aare.ui.car

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * MyPlay (F4): how the phone's status icons and the clock are drawn on the car screen. Every field
 * is a setting (MyPlayStatusSettings.kt); the defaults are the look AARE had.
 *
 * Colours are ARGB values, or null for the theme's own colour.
 */
data class StatusStyle(
    val batteryIcon: Boolean = true,
    val batteryNumber: Boolean = true,
    val percentSign: Boolean = false,
    val size: StatusSize = StatusSize.NORMAL,
    val color: Int? = null,
    /** Green, amber or red by how full the battery is, instead of only red when nearly empty. */
    val colorByLevel: Boolean = false,
    val signal: Boolean = true,
    val clockSize: StatusSize = StatusSize.NORMAL,
    val clockBold: Boolean = false,
    val clockColor: Int? = null,
) {
    /** The battery's number as shown, or null when the number is switched off. */
    fun batteryText(percent: Int): String? =
        if (!batteryNumber) null else percent.coerceIn(0, 100).toString() + if (percentSign) "%" else ""

    /** Which colour the battery takes at [percent]. */
    fun batteryLevel(percent: Int, charging: Boolean): BatteryLevel = when {
        charging -> BatteryLevel.NORMAL
        percent <= LOW -> BatteryLevel.LOW
        !colorByLevel -> BatteryLevel.NORMAL
        percent <= MEDIUM -> BatteryLevel.MEDIUM
        else -> BatteryLevel.HIGH
    }

    enum class BatteryLevel { NORMAL, LOW, MEDIUM, HIGH }

    companion object {
        /** At or below: red (as AARE always did). */
        const val LOW = 15
        /** At or below, with colour by level: amber. */
        const val MEDIUM = 30
    }
}

/** A size step for the status icons or the clock. Stored by [key]. */
enum class StatusSize(val key: String, val scale: Float) {
    SMALL("small", 0.85f), NORMAL("normal", 1f), LARGE("large", 1.25f), LARGER("larger", 1.5f);

    companion object {
        fun byKey(key: String?): StatusSize = entries.firstOrNull { it.key == key } ?: NORMAL
    }
}

/** The status style in use; CarShell provides the car's, the phone's picture its own. */
val LocalStatusStyle = staticCompositionLocalOf { StatusStyle() }
