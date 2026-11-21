package net.prototux.aare.ui.car.myplay

import androidx.core.content.edit
import net.prototux.aare.Settings

/**
 * MyPlay (N1): draw the car screen with MyPlay's own engine (phase N) instead of the chosen
 * layout. Hidden and off: a preview while phase N is built, until N10 makes it the layout.
 */
var Settings.myplayEnginePreview: Boolean
    get() = prefs.getBoolean("myplay_engine_preview", false)
    set(v) = prefs.edit { putBoolean("myplay_engine_preview", v) }
