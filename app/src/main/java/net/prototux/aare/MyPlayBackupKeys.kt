package net.prototux.aare

import net.prototux.aare.SettingsBackup.Type

/**
 * MyPlay: MyPlay's own settings that move in a settings backup, with the type each is read with.
 * Joined to upstream's list (SettingsBackup.ALLOWED). The same rule applies: nothing that opens
 * MyPlay to the network or another app, skips a safety check, or is a secret or phone-specific.
 * A setting added in MyPlay's own files is added here too, or it stays out of backups.
 */
internal object MyPlayBackupKeys {
    val ALLOWED: Map<String, Type> = mapOf(
        // Car screen (car-side code)
        // Upstream's, from branches taken into MyPlay but not yet on upstream's own list:
        "rail_widgets" to Type.STRING,
        "car_density" to Type.INT,
        "car_text_size" to Type.STRING,
        "car_control_size" to Type.STRING,
        "log_car_input" to Type.BOOLEAN,
        "myplay_maps_follow_car_lights" to Type.BOOLEAN,
        "myplay_debug_map_areas" to Type.BOOLEAN,
        "myplay_layout_spec" to Type.STRING,
        "myplay_layout_preset" to Type.STRING,
        "myplay_status_battery_icon" to Type.BOOLEAN,
        "myplay_status_battery_number" to Type.BOOLEAN,
        "myplay_status_percent_sign" to Type.BOOLEAN,
        "myplay_status_size" to Type.STRING,
        "myplay_status_color" to Type.INT,
        "myplay_status_color_by_level" to Type.BOOLEAN,
        "myplay_status_signal" to Type.BOOLEAN,
        "myplay_clock_size" to Type.STRING,
        "myplay_clock_bold" to Type.BOOLEAN,
        "myplay_clock_color" to Type.INT,
        "myplay_home_maps" to Type.STRING,
        "myplay_gesture_hold_ms" to Type.LONG,
        "myplay_gesture_double_ms" to Type.LONG,
        "myplay_buttons" to Type.STRING,
        "myplay_look_colours" to Type.STRING,
        "myplay_button_codes" to Type.STRING,
        "myplay_looks" to Type.STRING, // the owner's saved looks (phone, F7)
    )
}
