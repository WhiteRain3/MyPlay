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
        "myplay_mirror_turns" to Type.STRING, // what a knob turn means in each mirrored app (G1)
        "myplay_video_app" to Type.STRING, // the owner's "Video app", the YouTube build to use (VA-4)
        "myplay_gesture_line" to Type.BOOLEAN, // the line after a button gesture (R6)
        "myplay_speed_source" to Type.STRING, // where the car screen's speed comes from (N3)
        "myplay_engine_preview" to Type.BOOLEAN, // MyPlay's own car screen (ORBIT) rather than an older layout (N10: on by default)
        "myplay_engine_language" to Type.STRING, // the new car screen's design language, ORBIT or DECK (N13)
        "myplay_maps_idle_home" to Type.INT, // minutes before a full-screen maps app goes home, 0 never (O2b)
        "myplay_learned_buttons" to Type.STRING, // car buttons taught an action in the car, code to action (O11)
        "myplay_orbit_shade" to Type.STRING, // the shade behind ORBIT's parts: soft or none (SH-1)
        "myplay_info_home" to Type.BOOLEAN, // ORBIT's home without the knob's ring (O11)
        "myplay_limit_pulse" to Type.BOOLEAN, // the limit sign's one pulse when the limit changes (LP-1)
        "myplay_drive_warnings" to Type.BOOLEAN, // the ice sign and the phone's heat on the car screen (ideas 2026-11-22)
        "myplay_home_pad" to Type.STRING, // what ORBIT's home pad opens while the ring is hidden (O6)
        "myplay_panel_timeout" to Type.INT, // seconds before a panel over the map closes by itself, 0 never (O2)
        "myplay_speed_limit_margin" to Type.INT, // how far over the limit before the speed changes colour (N20)
        "myplay_map_filter" to Type.STRING, // the map colour filter, one of a look's settings
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
        "myplay_speed_limit_region" to Type.STRING, // which region's speed limit file to keep (phone, N21)
        "myplay_fill_standard_limits" to Type.BOOLEAN, // option A: show the standard limit, as a guess, where none is tagged
        "myplay_speed_limit_mobile" to Type.BOOLEAN, // the speed limit file may download on mobile data (phone)
    )
}
