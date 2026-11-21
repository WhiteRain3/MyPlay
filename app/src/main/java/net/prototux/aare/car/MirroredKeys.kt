package net.prototux.aare.car

import android.view.KeyEvent
import androidx.core.content.edit
import net.prototux.aare.Settings

/**
 * MyPlay (plan G1): the car's knob and pad inside a mirrored phone app (AARE's "Phone apps", run on
 * a car-sized screen through Shizuku or root). Until now only touches and Back reached it, so on a
 * car without touch it could not be used.
 *
 * While the app is on screen: the pad's four directions and the knob's press go to it as the same
 * keys; a turn of the knob becomes a direction, per app ([TurnMode]): up and down (lists, most
 * apps) or left and right (rows, carousels). A plain key is all the launcher can send (no Shift),
 * so a turn cannot be "previous item" as Shift+Tab.
 */
object MirroredKeys {
    enum class TurnMode(val id: String) {
        UP_DOWN("up_down"), LEFT_RIGHT("left_right");

        companion object {
            fun byId(id: String?) = entries.firstOrNull { it.id == id }
        }
    }

    val DEFAULT_TURN = TurnMode.UP_DOWN

    /**
     * The key to send into the app for a key the car screen got: the same key for the pad and the
     * press, a direction for a knob turn (Tab, Shift+Tab when [shift]), null for anything else
     * (Back and the rest keep their own way).
     */
    fun forward(keyCode: Int, shift: Boolean, turn: TurnMode): Int? = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> keyCode
        KeyEvent.KEYCODE_NUMPAD_ENTER -> KeyEvent.KEYCODE_ENTER
        KeyEvent.KEYCODE_TAB -> when (turn) {
            TurnMode.UP_DOWN -> if (shift) KeyEvent.KEYCODE_DPAD_UP else KeyEvent.KEYCODE_DPAD_DOWN
            TurnMode.LEFT_RIGHT -> if (shift) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        }
        else -> null
    }

    /** "package=mode;..." of the apps whose turn is not the default; unknown parts are skipped. */
    fun decodeTurns(s: String?): Map<String, TurnMode> = s.orEmpty().split(';').mapNotNull { e ->
        val i = e.indexOf('=')
        if (i <= 0) return@mapNotNull null
        val mode = TurnMode.byId(e.substring(i + 1).trim()) ?: return@mapNotNull null
        e.substring(0, i).trim() to mode
    }.toMap()

    fun encodeTurns(m: Map<String, TurnMode>): String =
        m.entries.filter { it.value != DEFAULT_TURN }.sortedBy { it.key }.joinToString(";") { "${it.key}=${it.value.id}" }

    fun turnFor(all: String?, packageName: String): TurnMode = decodeTurns(all)[packageName] ?: DEFAULT_TURN
}

/** MyPlay (G1): what a knob turn means in each mirrored app ([MirroredKeys.decodeTurns]); empty: up and down everywhere. */
var Settings.myplayMirrorTurns: String
    get() = prefs.getString("myplay_mirror_turns", "") ?: ""
    set(v) = prefs.edit { putString("myplay_mirror_turns", v) }
