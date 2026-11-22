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
enum class HomePadKey(val id: String) { CENTER("center"), UP("up"), DOWN("down"), SIDE("side") }

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
    NOTHING("nothing"),
    ;

    companion object {
        fun byId(id: String?) = entries.firstOrNull { it.id == id }
    }
}

object HomePad {
    /** The owner's mapping (the dispatcher, 2026-11-21): centre music, up the maps app, the sides the dial. */
    val DEFAULT: Map<HomePadKey, HomePadAction> = mapOf(
        HomePadKey.CENTER to HomePadAction.MUSIC,
        HomePadKey.UP to HomePadAction.MAPS,
        HomePadKey.SIDE to HomePadAction.DIAL,
        HomePadKey.DOWN to HomePadAction.NOTHING,
    )

    /** "center=music;up=maps;..."; unknown parts are skipped, missing keys take the default. */
    fun decode(s: String?): Map<HomePadKey, HomePadAction> {
        val stored = s.orEmpty().split(';').mapNotNull { e ->
            val (k, v) = e.split('=', limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
            val key = HomePadKey.entries.firstOrNull { it.id == k.trim() } ?: return@mapNotNull null
            val action = HomePadAction.byId(v.trim()) ?: return@mapNotNull null
            key to action
        }.toMap()
        return DEFAULT + stored
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
