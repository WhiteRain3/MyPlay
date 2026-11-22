package net.prototux.aare.car

import android.view.KeyEvent
import androidx.core.content.edit
import net.prototux.aare.Settings

/**
 * MyPlay (O11, the owner: "the 4 buttons around the knob"): buttons the owner taught MyPlay in the
 * car. The phone arms an action ([myplayLearnButton]); the next car key that is not Back comes in
 * and is bound to it ([myplayLearnedButtons]), whatever its code, so no code has to be known
 * beforehand (R8). A learned key does its action on release and goes nowhere else. Pure codec here.
 */
object LearnedButtons {
    /** "code=action;..."; unknown parts are skipped. */
    fun decode(s: String?): Map<Int, ButtonAction> = s.orEmpty().split(';').mapNotNull { e ->
        val (c, a) = e.split('=', limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
        val code = c.trim().toIntOrNull() ?: return@mapNotNull null
        val action = ButtonAction.byId(a.trim()) ?: return@mapNotNull null
        code to action
    }.toMap()

    fun encode(m: Map<Int, ButtonAction>): String = m.entries.sortedBy { it.key }.joinToString(";") { "${it.key}=${it.value.id}" }

    /**
     * Keys never learned: Back must stay a way out, and the knob's turns and pushes are the ring's
     * (they are in the button table already).
     */
    val NEVER: Set<Int> = setOf(KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_TAB, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER)

    /** The binding after learning [code] for [action] (an action of NOTHING forgets the code). */
    fun learn(current: Map<Int, ButtonAction>, code: Int, action: ButtonAction): Map<Int, ButtonAction> =
        if (action == ButtonAction.NOTHING) current - code else current + (code to action)
}

/** O11: the learned buttons, code to action. Backed up. */
var Settings.myplayLearnedButtons: Map<Int, ButtonAction>
    get() = LearnedButtons.decode(prefs.getString("myplay_learned_buttons", ""))
    set(v) = prefs.edit { putString("myplay_learned_buttons", LearnedButtons.encode(v)) }

/**
 * O11: the action the next car key will be taught (set by the phone's Buttons page; cleared once a
 * key is learned). Not backed up: it is a moment's state.
 */
var Settings.myplayLearnButton: ButtonAction?
    get() = prefs.getString("myplay_learn_button", null)?.let(ButtonAction::byId)
    set(v) = prefs.edit { if (v == null) remove("myplay_learn_button") else putString("myplay_learn_button", v.id) }

internal fun isLearnKey(key: String?) = key == "myplay_learned_buttons" || key == "myplay_learn_button"
