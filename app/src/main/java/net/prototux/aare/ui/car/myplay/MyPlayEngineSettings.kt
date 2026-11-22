package net.prototux.aare.ui.car.myplay

import androidx.core.content.edit
import net.prototux.aare.Settings

/**
 * MyPlay (N1, N10): draw the car screen with MyPlay's own engine (ORBIT, or DECK) instead of the
 * chosen older layout. N10 (the owner: "focus fully on ORBIT"): on by default, and every install
 * is moved to it once ([MyPlayOrbitDefault]); turning it off still picks an older layout, until
 * N12 removes them. Backed up.
 */
var Settings.myplayEnginePreview: Boolean
    get() {
        MyPlayOrbitDefault.migrate(this)
        return prefs.getBoolean("myplay_engine_preview", true)
    }
    set(v) {
        MyPlayOrbitDefault.migrate(this)
        prefs.edit { putBoolean("myplay_engine_preview", v) }
    }

/** N10: moving an install to ORBIT once, whatever it chose while ORBIT was a preview. */
object MyPlayOrbitDefault {
    private const val DONE = "myplay_orbit_default_done"

    fun migrate(settings: Settings) {
        val p = settings.prefs
        if (p.getBoolean(DONE, false)) return
        p.edit { remove("myplay_engine_preview"); putBoolean(DONE, true) }
    }
}

/** N13: the design language of MyPlay's own car screen ([MyPlaySkins]: "orbit" or "deck"). Backed up. */
const val KEY_ENGINE_LANGUAGE = "myplay_engine_language"
var Settings.myplayEngineLanguage: String
    get() = MyPlaySkins.byId(prefs.getString(KEY_ENGINE_LANGUAGE, null)).id
    set(v) = prefs.edit { putString(KEY_ENGINE_LANGUAGE, MyPlaySkins.byId(v).id) }

/**
 * O2: how long a panel over the map (the music panel, the dial) stays open without a knob, pad or
 * touch input, in seconds ([PanelTimeout.CHOICES]; 0 is never). Backed up.
 */
const val KEY_PANEL_TIMEOUT = "myplay_panel_timeout"
var Settings.myplayPanelTimeout: Int
    get() = PanelTimeout.valid(prefs.getInt(KEY_PANEL_TIMEOUT, PanelTimeout.DEFAULT))
    set(v) = prefs.edit { putInt(KEY_PANEL_TIMEOUT, PanelTimeout.valid(v)) }

/** O2: the times a driver can choose for [myplayPanelTimeout]. */
object PanelTimeout {
    const val DEFAULT = 5
    const val NEVER = 0
    val CHOICES = listOf(3, 5, 8, 15, NEVER)

    /** [s] when it is one of the choices, else the default. */
    fun valid(s: Int): Int = if (s in CHOICES) s else DEFAULT

    /** The time in milliseconds, or null for never. */
    fun ms(s: Int): Long? = valid(s).takeIf { it != NEVER }?.let { it * 1_000L }
}

/**
 * O2b: a maps app full screen goes back to MyPlay's home after this many minutes without a knob,
 * pad or touch input ([MapsIdleHome.CHOICES]; 0, the default, is never). Backed up.
 */
const val KEY_MAPS_IDLE_HOME = "myplay_maps_idle_home"
var Settings.myplayMapsIdleHome: Int
    get() = MapsIdleHome.valid(prefs.getInt(KEY_MAPS_IDLE_HOME, MapsIdleHome.OFF))
    set(v) = prefs.edit { putInt(KEY_MAPS_IDLE_HOME, MapsIdleHome.valid(v)) }

/** O2b: the choices for [myplayMapsIdleHome], in minutes. */
object MapsIdleHome {
    const val OFF = 0
    val CHOICES = listOf(OFF, 1, 2, 5, 10)
    fun valid(m: Int): Int = if (m in CHOICES) m else OFF
    fun ms(m: Int): Long? = valid(m).takeIf { it != OFF }?.let { it * 60_000L }
}

/** N13: the languages a driver can choose, by their stored id (never changed once released). */
object MyPlaySkins {
    val ALL: List<MyPlaySkin> get() = listOf(net.prototux.aare.ui.car.myplay.orbit.OrbitSkin, net.prototux.aare.ui.car.myplay.deck.DeckSkin)

    /** The language with [id]; ORBIT for anything else. */
    fun byId(id: String?): MyPlaySkin = ALL.firstOrNull { it.id == id } ?: net.prototux.aare.ui.car.myplay.orbit.OrbitSkin
}
