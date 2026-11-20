package net.prototux.aare.ui.car

import androidx.core.content.edit
import net.prototux.aare.Settings

/**
 * MyPlay (F4): the status icons' and clock's settings, as extension properties on Settings (keys
 * start with "myplay_status_" or "myplay_clock_"). Read together as [statusStyle].
 */

var Settings.myplayStatusBatteryIcon: Boolean
    get() = prefs.getBoolean("myplay_status_battery_icon", true)
    set(v) = prefs.edit { putBoolean("myplay_status_battery_icon", v) }

var Settings.myplayStatusBatteryNumber: Boolean
    get() = prefs.getBoolean("myplay_status_battery_number", true)
    set(v) = prefs.edit { putBoolean("myplay_status_battery_number", v) }

var Settings.myplayStatusPercentSign: Boolean
    get() = prefs.getBoolean("myplay_status_percent_sign", false)
    set(v) = prefs.edit { putBoolean("myplay_status_percent_sign", v) }

var Settings.myplayStatusSize: StatusSize
    get() = StatusSize.byKey(prefs.getString("myplay_status_size", null))
    set(v) = prefs.edit { putString("myplay_status_size", v.key) }

/** ARGB, or null for the theme's colour. */
var Settings.myplayStatusColor: Int?
    get() = if (prefs.contains("myplay_status_color")) prefs.getInt("myplay_status_color", 0) else null
    set(v) = prefs.edit { if (v == null) remove("myplay_status_color") else putInt("myplay_status_color", v) }

var Settings.myplayStatusColorByLevel: Boolean
    get() = prefs.getBoolean("myplay_status_color_by_level", false)
    set(v) = prefs.edit { putBoolean("myplay_status_color_by_level", v) }

var Settings.myplayStatusSignal: Boolean
    get() = prefs.getBoolean("myplay_status_signal", true)
    set(v) = prefs.edit { putBoolean("myplay_status_signal", v) }

var Settings.myplayClockSize: StatusSize
    get() = StatusSize.byKey(prefs.getString("myplay_clock_size", null))
    set(v) = prefs.edit { putString("myplay_clock_size", v.key) }

var Settings.myplayClockBold: Boolean
    get() = prefs.getBoolean("myplay_clock_bold", false)
    set(v) = prefs.edit { putBoolean("myplay_clock_bold", v) }

/** ARGB, or null for the theme's colour. */
var Settings.myplayClockColor: Int?
    get() = if (prefs.contains("myplay_clock_color")) prefs.getInt("myplay_clock_color", 0) else null
    set(v) = prefs.edit { if (v == null) remove("myplay_clock_color") else putInt("myplay_clock_color", v) }

/** All of the above as one value. */
val Settings.statusStyle: StatusStyle
    get() = StatusStyle(
        batteryIcon = myplayStatusBatteryIcon,
        batteryNumber = myplayStatusBatteryNumber,
        percentSign = myplayStatusPercentSign,
        size = myplayStatusSize,
        color = myplayStatusColor,
        colorByLevel = myplayStatusColorByLevel,
        signal = myplayStatusSignal,
        clockSize = myplayClockSize,
        clockBold = myplayClockBold,
        clockColor = myplayClockColor,
    )

/** Whether a changed settings key belongs to the status style (the car re-applies it at once). */
internal fun isStatusStyleKey(key: String?): Boolean =
    key != null && (key.startsWith("myplay_status_") || key.startsWith("myplay_clock_"))
