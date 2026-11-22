package net.prototux.aare.car

import androidx.core.content.edit
import net.prototux.aare.Settings
import org.json.JSONArray
import org.json.JSONObject

/**
 * MyPlay (ideas 2026-11-22, #5): up to [MAX] places of the owner's own (a name and an address or
 * lat,lng), listed with Home (myplay_home_address) in ORBIT's Navigate world; a push starts the
 * car's maps app there. Typed on the phone next to Home address. Never written to the log, and
 * kept out of backups on purpose (PL-2): a backup is a file that travels, so a list of personal
 * addresses stays on this phone; restoring on a new phone means typing them again. (Home address,
 * HN-1, is backed up; whether it should be is the owner's call.) While none is saved and no home address is set, Navigate
 * stays what it was: the maps app itself.
 */
data class SavedPlace(val name: String, val address: String)

object Places {
    const val MAX = 5
    const val NAME_MAX = 40
    const val ADDRESS_MAX = 200

    /** The stored list: blank entries dropped, at most [MAX], each trimmed and cut to length. */
    fun clean(places: List<SavedPlace>): List<SavedPlace> = places
        .map { SavedPlace(it.name.trim().take(NAME_MAX), it.address.trim().take(ADDRESS_MAX)) }
        .filter { it.address.isNotEmpty() }
        .take(MAX)

    fun encode(places: List<SavedPlace>): String = JSONArray().apply {
        clean(places).forEach { put(JSONObject().put("name", it.name).put("address", it.address)) }
    }.toString()

    fun decode(s: String?): List<SavedPlace> = runCatching {
        val a = JSONArray(s ?: return emptyList())
        clean((0 until a.length()).map { i -> a.getJSONObject(i).let { SavedPlace(it.optString("name"), it.optString("address")) } })
    }.getOrDefault(emptyList())

    /**
     * What Navigate lists after its first row (the maps app itself): Home when set, then the saved
     * places; a place without a name shows its address. Empty: Navigate opens the maps app at once.
     */
    fun destinations(homeAddress: String, places: List<SavedPlace>, homeLabel: String): List<SavedPlace> =
        listOfNotNull(homeAddress.trim().takeIf { it.isNotEmpty() }?.let { SavedPlace(homeLabel, it) }) +
            clean(places).map { if (it.name.isEmpty()) it.copy(name = it.address) else it }
}

/** #5: the saved places. Not backed up (personal). */
const val KEY_PLACES = "myplay_places"
var Settings.myplayPlaces: List<SavedPlace>
    get() = Places.decode(prefs.getString(KEY_PLACES, null))
    set(v) = prefs.edit { putString(KEY_PLACES, Places.encode(v)) }
