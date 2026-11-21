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

/** N13: the design language of MyPlay's own car screen ([MyPlaySkins]: "orbit" or "deck"). Backed up. */
const val KEY_ENGINE_LANGUAGE = "myplay_engine_language"
var Settings.myplayEngineLanguage: String
    get() = MyPlaySkins.byId(prefs.getString(KEY_ENGINE_LANGUAGE, null)).id
    set(v) = prefs.edit { putString(KEY_ENGINE_LANGUAGE, MyPlaySkins.byId(v).id) }

/** N13: the languages a driver can choose, by their stored id (never changed once released). */
object MyPlaySkins {
    val ALL: List<MyPlaySkin> get() = listOf(net.prototux.aare.ui.car.myplay.orbit.OrbitSkin, net.prototux.aare.ui.car.myplay.deck.DeckSkin)

    /** The language with [id]; ORBIT for anything else. */
    fun byId(id: String?): MyPlaySkin = ALL.firstOrNull { it.id == id } ?: net.prototux.aare.ui.car.myplay.orbit.OrbitSkin
}
