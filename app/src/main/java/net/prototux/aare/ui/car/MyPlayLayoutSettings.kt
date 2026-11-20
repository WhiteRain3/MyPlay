package net.prototux.aare.ui.car

import androidx.core.content.edit
import net.prototux.aare.Settings

/**
 * MyPlay (M1): the car screen layout's settings, as extension properties so Settings.kt is not
 * edited (the phone app work adds its settings in a file of its own the same way).
 */

/** The preset last chosen ("A" to "F"), or "" once an option was changed on its own. */
var Settings.myplayLayoutPreset: String
    get() = prefs.getString(KEY_PRESET, CarLayoutSpec.DEFAULT_PRESET) ?: CarLayoutSpec.DEFAULT_PRESET
    set(v) = prefs.edit { putString(KEY_PRESET, v) }

/** The eight options. Without a stored value: the default preset. */
var Settings.myplayLayoutSpec: CarLayoutSpec
    get() = CarLayoutSpec.decode(prefs.getString(KEY_SPEC, null))
    set(v) = prefs.edit { putString(KEY_SPEC, CarLayoutSpec.encode(v.allowed())) }

/** Chooses preset [name]: all eight options at once. Unknown names are ignored. */
fun Settings.chooseLayoutPreset(name: String) {
    val spec = CarLayoutSpec.PRESETS[name] ?: return
    prefs.edit {
        putString(KEY_SPEC, CarLayoutSpec.encode(spec))
        putString(KEY_PRESET, name)
    }
}

/** Changes one or more options from the current layout; the preset name is then cleared. */
fun Settings.changeLayout(change: (CarLayoutSpec) -> CarLayoutSpec) {
    val next = change(myplayLayoutSpec).allowed()
    prefs.edit {
        putString(KEY_SPEC, CarLayoutSpec.encode(next))
        putString(KEY_PRESET, CarLayoutSpec.PRESETS.entries.firstOrNull { it.value == next }?.key ?: "")
    }
}

internal const val KEY_PRESET = "myplay_layout_preset"
internal const val KEY_SPEC = "myplay_layout_spec"
