package net.prototux.aare.car

import androidx.core.content.edit
import net.prototux.aare.Settings

/**
 * MyPlay (HN-1, the owner: a corner button by the knob to start Waze to home): the address the
 * "Navigate home" action ([ButtonAction.NAVIGATE_HOME]) goes to, as the owner typed it on the
 * phone: an address, or "lat,lng". Empty: not set (the action then says to set it on the phone).
 * Sent to the car's maps app as a geo: query (CarRuntime.navigateTo), so nothing has to be saved
 * in the maps app itself. Backed up.
 */
const val KEY_HOME_ADDRESS = "myplay_home_address"

var Settings.myplayHomeAddress: String
    get() = prefs.getString(KEY_HOME_ADDRESS, "").orEmpty().trim()
    set(v) = prefs.edit { putString(KEY_HOME_ADDRESS, v.trim()) }
