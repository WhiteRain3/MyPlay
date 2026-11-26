package net.prototux.aare.ui.car.myplay

import androidx.core.content.edit
import net.prototux.aare.Settings

/**
 * MyPlay (O6, the owner: "pretty much everything reachable from the pad"): on ORBIT's home the
 * pad's centre (the knob's push) and its directions open things directly, while the knob's ring is
 * hidden. A knob turn shows the ring; then the pad and the push work on the ring's stops as
 * before, until the ring hides again after the panel timeout. A direction set to [HomePadAction.NOTHING]
 * moves the ring as before.
 */
/** PL-R (the owner): left and right each have their own action (they were one "side" before). */
enum class HomePadKey(val id: String) { CENTER("center"), UP("up"), DOWN("down"), LEFT("left"), RIGHT("right") }

/** What a pad key does on the home while the ring is hidden. */
enum class HomePadAction(val id: String) {
    /** The music panel (O1), or the Music world when nothing plays. */
    MUSIC("music"),
    /** The maps app, full screen. */
    MAPS("maps"),
    /** The dial of worlds. */
    DIAL("dial"),
    APPS("apps"),
    CALLS("calls"),
    /** MyPlay (#10): the last missed call, called back only with a push on its Call button. */
    CALL_BACK("call_back"),
    /** MyPlay (#5): the Navigate world's list (the maps app, Home, the saved places); with none saved, the maps app. */
    PLACES("places"),
    /** HN-3: the next song of the playing music app. */
    NEXT_SONG("next_song"),
    /** HN-3: the maps app to the home address set on the phone (a notice when none is set). */
    NAVIGATE_HOME("navigate_home"),
    /** MyPlay: a picture of exactly what the car shows, saved on the phone (on no key by default). */
    CAR_SCREENSHOT("car_screenshot"),
    NOTHING("nothing"),
    ;

    companion object {
        fun byId(id: String?) = entries.firstOrNull { it.id == id }
    }
}

object HomePad {
    /**
     * The owner's mapping (the dispatcher, 2026-11-23): centre the dial, up the next song, right the
     * maps app, down home, left music. A mapping already stored keeps its own keys.
     */
    val DEFAULT: Map<HomePadKey, HomePadAction> = mapOf(
        HomePadKey.CENTER to HomePadAction.DIAL,
        HomePadKey.UP to HomePadAction.NEXT_SONG,
        HomePadKey.RIGHT to HomePadAction.MAPS,
        HomePadKey.DOWN to HomePadAction.NAVIGATE_HOME,
        HomePadKey.LEFT to HomePadAction.MUSIC,
    )

    /**
     * "center=music;up=maps;..."; unknown parts are skipped, missing keys take the default. PL-R:
     * a mapping saved before left and right were apart has "side=…": it seeds both, unless one of
     * them is stored itself.
     */
    fun decode(s: String?): Map<HomePadKey, HomePadAction> {
        val pairs = s.orEmpty().split(';').mapNotNull { e ->
            val (k, v) = e.split('=', limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
            val action = HomePadAction.byId(v.trim()) ?: return@mapNotNull null
            k.trim() to action
        }
        val side = pairs.lastOrNull { it.first == "side" }?.second
        val seeded = if (side != null) mapOf(HomePadKey.LEFT to side, HomePadKey.RIGHT to side) else emptyMap()
        val stored = pairs.mapNotNull { (k, a) -> HomePadKey.entries.firstOrNull { it.id == k }?.let { it to a } }.toMap()
        return DEFAULT + seeded + stored
    }

    fun encode(m: Map<HomePadKey, HomePadAction>): String = HomePadKey.entries.joinToString(";") { k -> "${k.id}=${(m[k] ?: HomePadAction.NOTHING).id}" }

    /** Whether any key opens something (else the home works as before: the ring always there). */
    fun active(m: Map<HomePadKey, HomePadAction>) = m.values.any { it != HomePadAction.NOTHING }
}

/**
 * O11: the info-only home: ORBIT's home shows information only, with no knob ring at all; the pad
 * ([myplayHomePad]) and the buttons taught in the car open everything. Off by default. Backed up.
 */
const val KEY_INFO_HOME = "myplay_info_home"
var Settings.myplayInfoHome: Boolean
    get() = prefs.getBoolean(KEY_INFO_HOME, false)
    set(v) = prefs.edit { putBoolean(KEY_INFO_HOME, v) }

/**
 * SH-1: the shade behind ORBIT's parts: soft plates that hug them ("soft", the default) or none.
 * Backed up.
 */
const val KEY_ORBIT_SHADE = "myplay_orbit_shade"
const val ORBIT_SHADE_SOFT = "soft"
const val ORBIT_SHADE_NONE = "none"
var Settings.myplayOrbitShade: String
    get() = prefs.getString(KEY_ORBIT_SHADE, ORBIT_SHADE_SOFT).takeIf { it == ORBIT_SHADE_NONE } ?: ORBIT_SHADE_SOFT
    set(v) = prefs.edit { putString(KEY_ORBIT_SHADE, if (v == ORBIT_SHADE_NONE) ORBIT_SHADE_NONE else ORBIT_SHADE_SOFT) }

/** O6: the home pad's mapping. Backed up. */
const val KEY_HOME_PAD = "myplay_home_pad"
var Settings.myplayHomePad: Map<HomePadKey, HomePadAction>
    get() = HomePad.decode(prefs.getString(KEY_HOME_PAD, null))
    set(v) = prefs.edit { putString(KEY_HOME_PAD, HomePad.encode(v)) }
