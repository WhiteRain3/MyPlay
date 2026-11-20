package net.prototux.aare.ui.phone

import androidx.core.content.edit
import net.prototux.aare.Settings
import net.prototux.aare.SettingsBackup
import net.prototux.aare.ui.car.CarLayoutSpec
import org.json.JSONArray
import org.json.JSONObject

/**
 * A whole look (F7): the car screen's colours, layout, wallpaper, status icons and clock, and the
 * bar's items, saved together under a name. [values] holds only what differs from MyPlay's
 * defaults: a setting of the look that is not in it goes back to its default when the look is
 * chosen, so choosing a look always gives the same screen.
 */
data class Look(val name: String, val values: Map<String, Any>)

object MyPlayLooks {
    /**
     * The settings that make up a look. Not day/night (car_theme), sizes, density or apps: those
     * follow the car and the driver, not the look. Older keys that the newer ones fall back to
     * (rail_right, car_style) are here so that choosing a look clears them as well.
     */
    val KEYS: Set<String> = setOf(
        "style_theme", "style_accent", "myplay_look_colours", "car_style",
        "style_layout", "myplay_layout_preset", "myplay_layout_spec", "rail_position", "rail_right", "dark_bar",
        "wallpaper",
        "rail_widgets", "show_weather",
        "myplay_status_battery_icon", "myplay_status_battery_number", "myplay_status_percent_sign", "myplay_status_size",
        "myplay_status_color", "myplay_status_color_by_level", "myplay_status_signal",
        "myplay_clock_size", "myplay_clock_bold", "myplay_clock_color",
    )

    /** In a look file, next to the settings backup's own fields. */
    const val NAME_KEY = "look_name"

    /** The longest name kept. */
    const val MAX_NAME = 40

    private fun spec(preset: String) = CarLayoutSpec.encode(CarLayoutSpec.PRESETS.getValue(preset))

    private fun preset(name: String) = mapOf("myplay_layout_preset" to name, "myplay_layout_spec" to spec(name))

    /** The looks that come with MyPlay; the first is MyPlay's own defaults. */
    val BUILT_IN: List<Look> = listOf(
        Look("MyPlay", emptyMap()),
        Look("Midnight", mapOf("style_theme" to "night", "style_accent" to "red", "dark_bar" to true) + preset("A")),
        Look("Forest", mapOf("style_accent" to "green", "wallpaper" to "forest") + preset("E")),
        Look("Ocean", mapOf("style_accent" to "cyan", "wallpaper" to "ocean") + preset("F")),
        Look("High contrast", mapOf("style_theme" to "high_contrast") + preset("A")),
    )

    /**
     * What a look setting reads as when it is not stored (Settings' own defaults). A value stored
     * at its default is no difference, so [current] leaves it out. style_theme and style_layout
     * default to these only without the older car_style, which [current] then keeps them beside.
     */
    val DEFAULTS: Map<String, Any> = mapOf(
        "style_theme" to "aare", "style_accent" to "theme", "style_layout" to "taskbar", "myplay_look_colours" to "",
        "myplay_layout_preset" to CarLayoutSpec.DEFAULT_PRESET, "myplay_layout_spec" to CarLayoutSpec.encode(CarLayoutSpec.DEFAULT),
        "rail_position" to Settings.RAIL_AUTO, "dark_bar" to false, "wallpaper" to "none",
        "rail_widgets" to "clock,battery,weather", "show_weather" to false,
        "myplay_status_battery_icon" to true, "myplay_status_battery_number" to true, "myplay_status_percent_sign" to false,
        "myplay_status_size" to "normal", "myplay_status_color_by_level" to false, "myplay_status_signal" to true,
        "myplay_clock_size" to "normal", "myplay_clock_bold" to false,
    )

    /** The look now set, from `SharedPreferences.getAll()`, named [name]: only what differs from the defaults. */
    fun current(all: Map<String, *>, name: String): Look {
        val legacy = all["car_style"] != null
        val values = all.filterKeys { it in KEYS }.mapNotNull { (k, v) -> v?.let { k to it } }.filter { (k, v) ->
            val default = DEFAULTS[k]
            default == null || default != v || (legacy && (k == "style_theme" || k == "style_layout"))
        }
        return Look(cleanName(name), values.toMap())
    }

    /** Whether the settings now set are exactly [look]. */
    fun isCurrent(all: Map<String, *>, look: Look): Boolean = current(all, look.name).values == current(look.values, look.name).values

    fun cleanName(name: String): String = name.filter { !it.isISOControl() }.trim().take(MAX_NAME)

    /** Writes [look] into [settings]: its values, and every other setting of a look back to its default. */
    fun choose(settings: Settings, look: Look) {
        settings.prefs.edit {
            KEYS.forEach { remove(it) }
            look.values.forEach { (k, v) ->
                when (v) {
                    is String -> putString(k, v)
                    is Boolean -> putBoolean(k, v)
                    is Int -> putInt(k, v)
                    is Long -> putLong(k, v)
                    is Float -> putFloat(k, v)
                }
            }
        }
    }

    /** [look] as a file in the settings backup's format, holding the look only. */
    fun toFile(look: Look): JSONObject = SettingsBackup.export(look.values).put(NAME_KEY, look.name)

    /**
     * A look file (or a whole settings backup) back: only the look's settings are taken. A photo
     * wallpaper is dropped ([keepPhoto] false), as the photo is not in the file; looks saved on
     * this phone keep it. Throws [IllegalArgumentException] on a file this version cannot read.
     */
    fun fromFile(json: JSONObject, fallbackName: String, keepPhoto: Boolean = false): Look {
        val imported = SettingsBackup.import(json)
        val values = imported.values.filterKeys { it in KEYS }.toMutableMap()
        if (keepPhoto && "wallpaper" in imported.skipped &&
            json.optJSONObject("settings")?.optJSONObject("wallpaper")?.optString("v") == Settings.WALLPAPER_CUSTOM
        ) values["wallpaper"] = Settings.WALLPAPER_CUSTOM
        val name = cleanName(json.optString(NAME_KEY)).ifEmpty { cleanName(fallbackName) }
        return Look(name, values)
    }

    /** The owner's saved looks as text (setting myplay_looks). */
    fun encode(looks: List<Look>): String = JSONArray(looks.map { toFile(it) }).toString()

    /** The text form back; a broken entry is skipped. */
    fun decode(text: String?): List<Look> {
        val array = runCatching { JSONArray(text.orEmpty()) }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            array.optJSONObject(i)?.let { runCatching { fromFile(it, "", keepPhoto = true) }.getOrNull() }?.takeIf { it.name.isNotEmpty() }
        }
    }

    /** [looks] with [look] added, replacing one of the same name. */
    fun saved(looks: List<Look>, look: Look): List<Look> = looks.filter { it.name != look.name } + look
}

/** The owner's own looks (F7), as [MyPlayLooks.encode] writes them. */
var Settings.myplayLooks: List<Look>
    get() = MyPlayLooks.decode(prefs.getString("myplay_looks", null))
    set(v) = prefs.edit { putString("myplay_looks", MyPlayLooks.encode(v)) }
