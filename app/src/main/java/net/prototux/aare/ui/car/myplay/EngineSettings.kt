package net.prototux.aare.ui.car.myplay

import android.content.Context
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.car.SpeedRules
import net.prototux.aare.car.SpeedSourceMode
import net.prototux.aare.car.myplaySpeedLimitMargin
import net.prototux.aare.car.myplayDriveWarnings
import net.prototux.aare.car.myplayLimitPulse
import net.prototux.aare.car.myplayGestureLine
import net.prototux.aare.car.myplaySpeedSource
import net.prototux.aare.ui.car.CarAppKind
import net.prototux.aare.ui.car.CarUiState
import net.prototux.aare.ui.car.HomeMaps
import net.prototux.aare.ui.car.myplayHomeMaps

/**
 * MyPlay (plan phase N, N9): the settings the Settings world offers in the car, as plain data, so
 * the skin draws them and tests walk them with a fake. Only what a driver changes from the car;
 * the rest stays in the phone app. A setting is a switch or a choice ([SettingItem]).
 */
sealed interface SettingItem {
    /** Stable: the ring stays on it across changes. */
    val id: String
    val title: String

    /** [note]: shown under the title; with [canTurnOn] false a push cannot turn it on (it can still be turned off). */
    data class Toggle(
        override val id: String, override val title: String, val on: Boolean,
        val canTurnOn: Boolean = true, val note: String? = null, val set: (Boolean) -> Unit,
    ) : SettingItem

    /** [options]: value to label; a push steps to the next one, round. */
    data class Choice(
        override val id: String, override val title: String, val options: List<Pair<String, String>>, val selected: String,
        val select: (String) -> Unit,
    ) : SettingItem {
        val label: String get() = options.firstOrNull { it.first == selected }?.second ?: options.firstOrNull()?.second.orEmpty()
        val next: String get() {
            val i = options.indexOfFirst { it.first == selected }
            return options[(i + 1).mod(options.size)].first
        }
    }
}

interface SettingsSource {
    /** The settings as they are now (read again after each change). */
    fun items(): List<SettingItem>
}

/** The car's real settings (the phone's stored ones, applied at once by the car runtime's listener). */
internal class CarSettingsSource(private val context: Context, private val state: CarUiState) : SettingsSource {
    private val settings = Settings(context)
    private fun s(id: Int) = context.getString(id)

    override fun items(): List<SettingItem> = listOf(
        SettingItem.Choice(
            "colors", s(R.string.setup_choice_colors),
            listOf(Settings.THEME_AUTO to s(R.string.setup_choice_colors_auto), Settings.THEME_DARK to s(R.string.setup_choice_dark),
                Settings.THEME_LIGHT to s(R.string.setup_choice_light)),
            settings.carTheme,
        ) { settings.carTheme = it },
        SettingItem.Choice(
            "home_maps", s(R.string.myplay_home_maps_title),
            listOf(HomeMaps.LAST to s(R.string.myplay_home_maps_last)) +
                state.apps.filter { it.kind == CarAppKind.NAVIGATION }.map { HomeMaps.chosen(it.packageName) to it.label } +
                listOf(HomeMaps.ASK to s(R.string.myplay_home_maps_ask)),
            settings.myplayHomeMaps,
        ) { settings.myplayHomeMaps = it },
        SettingItem.Choice(
            "speed_source", s(R.string.myplay_setting_speed),
            listOf(SpeedSourceMode.AUTO.id to s(R.string.myplay_setting_speed_auto), SpeedSourceMode.CAR.id to s(R.string.myplay_setting_speed_car),
                SpeedSourceMode.OFF.id to s(R.string.myplay_off)),
            settings.myplaySpeedSource.id,
        ) { settings.myplaySpeedSource = SpeedSourceMode.byId(it) },
        // N20: how far over the map's limit before the speed turns amber (silent; Off: never).
        SettingItem.Choice(
            "speed_limit_margin", s(R.string.myplay_setting_speed_limit_margin),
            SpeedRules.MARGINS.map { m ->
                m.toString() to if (m == SpeedRules.MARGIN_OFF) s(R.string.myplay_off) else context.getString(R.string.myplay_setting_speed_limit_margin_by, m)
            },
            settings.myplaySpeedLimitMargin.toString(),
        ) { settings.myplaySpeedLimitMargin = it.toInt() },
        // O2: how long a panel over the map (the music panel, the dial) stays open untouched.
        SettingItem.Choice(
            "panel_timeout", s(R.string.myplay_setting_panel_timeout),
            PanelTimeout.CHOICES.map { t ->
                t.toString() to if (t == PanelTimeout.NEVER) s(R.string.myplay_setting_panel_timeout_never) else context.getString(R.string.myplay_setting_panel_timeout_after, t)
            },
            settings.myplayPanelTimeout.toString(),
        ) { settings.myplayPanelTimeout = it.toInt() },
        // O2b: a maps app full screen goes back home when untouched (off by default).
        SettingItem.Choice(
            "maps_idle_home", s(R.string.myplay_setting_maps_idle_home),
            MapsIdleHome.CHOICES.map { m ->
                m.toString() to if (m == MapsIdleHome.OFF) s(R.string.myplay_off) else context.getString(R.string.myplay_setting_maps_idle_home_after, m)
            },
            settings.myplayMapsIdleHome.toString(),
        ) { settings.myplayMapsIdleHome = it.toInt() },
        // SH-1: the shade behind ORBIT's parts.
        SettingItem.Choice(
            "orbit_shade", s(R.string.myplay_setting_orbit_shade),
            listOf(ORBIT_SHADE_SOFT to s(R.string.myplay_setting_orbit_shade_soft), ORBIT_SHADE_NONE to s(R.string.myplay_setting_orbit_shade_none)),
            settings.myplayOrbitShade,
        ) { settings.myplayOrbitShade = it },
        // O11: the info-only home (ORBIT, with the home pad on).
        SettingItem.Toggle("info_home", s(R.string.myplay_setting_info_home), settings.myplayInfoHome) { settings.myplayInfoHome = it },
        SettingItem.Toggle("gesture_line", s(R.string.myplay_setting_gesture_line), settings.myplayGestureLine) { settings.myplayGestureLine = it },
        SettingItem.Toggle("drive_warnings", s(R.string.myplay_setting_drive_warnings), settings.myplayDriveWarnings) { settings.myplayDriveWarnings = it },
        SettingItem.Toggle("limit_pulse", s(R.string.myplay_setting_limit_pulse), settings.myplayLimitPulse) { settings.myplayLimitPulse = it },
        SettingItem.Toggle("banners", s(R.string.setup_switch_message_notifications), settings.messageBanners) { settings.messageBanners = it },
        SettingItem.Toggle("read_aloud", s(R.string.msg_toggle_read_aloud_title), settings.readMessagesAloud) { settings.readMessagesAloud = it },
        SettingItem.Toggle("car_audio", s(R.string.setup_switch_audio_next_connection), settings.carAudio) { settings.carAudio = it },
        passenger(),
    )

    /**
     * S-1: passenger mode lifts every driving restriction. From the car it can always be turned off,
     * but turned on only while the car is stopped (the phone app is unchanged). While it is off, the
     * car's driving state is [CarUiState.drivingRestricted].
     */
    private fun passenger(): SettingItem.Toggle {
        val on = settings.ignoreDriving
        val moving = !on && state.drivingRestricted
        return SettingItem.Toggle(
            "passenger", s(R.string.setup_switch_passenger), on, canTurnOn = !moving,
            note = if (moving) s(R.string.myplay_setting_passenger_stopped) else null,
        ) { v -> if (!v || !(state.drivingRestricted && !settings.ignoreDriving)) settings.ignoreDriving = v }
    }
}
