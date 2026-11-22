package net.prototux.aare.car

import android.view.KeyEvent

/**
 * MyPlay (plan R3): which action each gesture on each of the car's controls does.
 *
 * A control is a part of the owner's car (the wheel's click, the dash knob's push, Menu, Back);
 * which key codes each one sends is a separate list ([codes]), so when the owner's connection log
 * shows the real codes (R8), only that list changes. Until then the codes are the standard Android
 * ones the bench sends.
 *
 * The table holds the defaults below; the owner's changes are stored as overrides (setting
 * `myplay_buttons`), so "reset to the defaults" is clearing them. Pure, for tests and for the
 * phone's Buttons page (B1).
 */
internal class ButtonTable(
    private val overrides: Map<Pair<Control, Gesture>, ButtonAction> = emptyMap(),
    private val codeOverrides: Map<Control, Set<Int>> = emptyMap(),
) {
    enum class Control(val id: String) {
        KNOB_PUSH("knob_push"), WHEEL_CLICK("wheel_click"), MENU("menu"), BACK("back");

        companion object {
            fun byId(id: String) = entries.firstOrNull { it.id == id }
        }
    }

    enum class Gesture(val id: String) {
        PRESS("press"), HOLD("hold"), DOUBLE("double"), HOLD_TURN("hold_turn"),
        /** G-2: three quick presses. */
        TRIPLE("triple");

        companion object {
            fun byId(id: String) = entries.firstOrNull { it.id == id }
        }
    }

    /** The key codes [control] sends. */
    fun codes(control: Control): Set<Int> = codeOverrides[control] ?: DEFAULT_CODES.getValue(control)

    /** The control a key code belongs to, or null for a key the table does not handle. */
    fun control(code: Int): Control? = Control.entries.firstOrNull { code in codes(it) }

    fun action(control: Control, gesture: Gesture): ButtonAction =
        overrides[control to gesture] ?: DEFAULTS[control to gesture] ?: ButtonAction.NOTHING

    /** The action for [gesture] on the key [code], or null when the key is not in the table. */
    fun resolve(code: Int, gesture: Gesture): ButtonAction? = control(code)?.let { action(it, gesture) }

    /** Whether a double press of [code] does something: if not, its press need not wait. */
    fun doubleCapable(code: Int) = resolve(code, Gesture.DOUBLE).let { it != null && it != ButtonAction.NOTHING }

    /** G-2: whether a triple press of [code] does something: if so, a double press waits for a third. */
    fun tripleCapable(code: Int) = resolve(code, Gesture.TRIPLE).let { it != null && it != ButtonAction.NOTHING }

    /** Whether a knob turn while [code] is held belongs to the hold rather than to the ring. */
    fun turnCapable(code: Int) = resolve(code, Gesture.HOLD_TURN).let { it != null && it != ButtonAction.NOTHING }

    /**
     * G-3: the table as the car side uses it, one line for the connection log: each control's
     * codes and its gestures' actions ("knob_push [23,66,160] press=select double=next ...").
     */
    fun describe(): String = "buttons: " + Control.entries.joinToString("; ") { c ->
        "${c.id} ${codes(c).sorted()} " + Gesture.entries.joinToString(" ") { g -> "${g.id}=${action(c, g).id}" }
    }

    /** Every code the table handles, so these keys are measured by the gesture engine alone. */
    val allCodes: Set<Int> get() = Control.entries.flatMap(::codes).toSet()

    companion object {
        val DEFAULT_CODES: Map<Control, Set<Int>> = mapOf(
            // O5: the number pad's Enter too, which some cars send for the knob's push (the screen
            // already takes it as a push); without it a rebound push still pressed the ring's item.
            Control.KNOB_PUSH to setOf(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER),
            Control.WHEEL_CLICK to setOf(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE),
            Control.MENU to setOf(KeyEvent.KEYCODE_MENU),
            Control.BACK to setOf(KeyEvent.KEYCODE_BACK),
        )

        /**
         * The defaults the plan proposes. G-1 (the owner's car check: the box sends every key as an
         * instant down and up, so a hold never arrives): a double push of the knob is the next
         * song by default, which on that box is also what a held knob becomes (one or two quick
         * presses); a single push then waits the double-press time (250 ms) before it presses.
         * The holds stay for cars that send them; they cost nothing where none come.
         */
        val DEFAULTS: Map<Pair<Control, Gesture>, ButtonAction> = mapOf(
            (Control.KNOB_PUSH to Gesture.PRESS) to ButtonAction.SELECT,
            (Control.KNOB_PUSH to Gesture.HOLD) to ButtonAction.NEXT,
            (Control.KNOB_PUSH to Gesture.DOUBLE) to ButtonAction.NEXT,
            (Control.KNOB_PUSH to Gesture.HOLD_TURN) to ButtonAction.SKIP_SONGS,
            // G-2 (the owner: the wheel has no next/previous buttons; its click is the knob's code):
            // three quick pushes, the previous song.
            (Control.KNOB_PUSH to Gesture.TRIPLE) to ButtonAction.PREVIOUS,
            (Control.WHEEL_CLICK to Gesture.PRESS) to ButtonAction.PLAY_PAUSE,
            (Control.WHEEL_CLICK to Gesture.HOLD) to ButtonAction.NEXT,
            (Control.WHEEL_CLICK to Gesture.DOUBLE) to ButtonAction.PREVIOUS,
            (Control.MENU to Gesture.PRESS) to ButtonAction.HOME_OR_APPS,
            (Control.MENU to Gesture.HOLD) to ButtonAction.ASSISTANT,
            (Control.BACK to Gesture.PRESS) to ButtonAction.BACK,
            (Control.BACK to Gesture.HOLD) to ButtonAction.HOME,
        )

        /** "control.gesture=action" entries, ";"-separated; unknown parts are skipped. */
        fun decodeOverrides(s: String?): Map<Pair<Control, Gesture>, ButtonAction> =
            s.orEmpty().split(';').mapNotNull { e ->
                val (key, value) = e.split('=', limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
                val (c, g) = key.split('.', limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
                val control = Control.byId(c.trim()) ?: return@mapNotNull null
                val gesture = Gesture.byId(g.trim()) ?: return@mapNotNull null
                val action = ButtonAction.byId(value.trim()) ?: return@mapNotNull null
                (control to gesture) to action
            }.toMap()

        fun encodeOverrides(m: Map<Pair<Control, Gesture>, ButtonAction>): String =
            m.entries.sortedBy { (k, _) -> k.first.ordinal * 10 + k.second.ordinal }
                .joinToString(";") { (k, v) -> "${k.first.id}.${k.second.id}=${v.id}" }

        /** "control=code,code" entries, ";"-separated; unknown parts are skipped. */
        fun decodeCodes(s: String?): Map<Control, Set<Int>> =
            s.orEmpty().split(';').mapNotNull { e ->
                val (c, v) = e.split('=', limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
                val control = Control.byId(c.trim()) ?: return@mapNotNull null
                val codes = v.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
                if (codes.isEmpty()) null else control to codes
            }.toMap()

        fun encodeCodes(m: Map<Control, Set<Int>>): String =
            m.entries.sortedBy { it.key.ordinal }.joinToString(";") { (c, codes) -> "${c.id}=${codes.sorted().joinToString(",")}" }
    }
}

/** What a gesture on a car control does. Stored by [id]. Labels come with the phone's page (B1). */
enum class ButtonAction(val id: String) {
    /** Press what has the ring, as the key would on its own. */
    SELECT("select"),
    BACK("back"),
    HOME("home"),
    /** Home; pressed on home, the app grid. */
    HOME_OR_APPS("home_or_apps"),
    ASSISTANT("assistant"),
    PLAY_PAUSE("play_pause"),
    NEXT("next"),
    PREVIOUS("previous"),
    /** With hold and turn: one song on or back per knob step. */
    SKIP_SONGS("skip_songs"),
    OPEN_NAVIGATION("open_navigation"),
    OPEN_MEDIA("open_media"),
    OPEN_PHONE("open_phone"),
    /** HN-1: the car's maps app to the home address set on the phone ([myplayHomeAddress]). */
    NAVIGATE_HOME("navigate_home"),
    NOTHING("nothing"),
    ;

    companion object {
        fun byId(id: String) = entries.firstOrNull { it.id == id }

        /**
         * The key [SELECT] sends to the screen for a gesture on [code]: the centre or Enter key
         * as it came, else the centre key (B1-1: Back, Menu or a media key sent as themselves would
         * not press the ring's item).
         */
        fun selectKeyFor(code: Int): Int =
            if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER) code else KeyEvent.KEYCODE_DPAD_CENTER
    }
}

/** A set whose members are read afresh on every lookup, for a list that settings can change. */
internal class LiveCodes(private val read: () -> Set<Int>) : AbstractSet<Int>() {
    override val size get() = read().size
    override fun iterator() = read().iterator()
    override fun contains(element: Int) = element in read()
}
