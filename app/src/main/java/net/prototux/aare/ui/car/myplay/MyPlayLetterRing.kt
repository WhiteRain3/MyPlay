package net.prototux.aare.ui.car.myplay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Look C (design 10): parked search typed with the knob on a ring of letters, in place of the
 * on-screen keyboard. A hosted app's SearchTemplate hands its text and what to do with each key
 * ([type], [backspace], [submit], [hide]: the same as the keyboard's KeyboardSink); the skin
 * draws the ring ([MyPlaySkin.letterRing]). Only shown where the keyboard would be, so never while
 * driving.
 */
class LetterRingInput(
    /** What has been typed so far. */
    val query: String,
    /** The field's hint while nothing is typed (the app's searchHint). */
    val hint: String,
    /** How many places the app has found for [query] so far; null while it has said nothing. */
    val found: Int?,
    val type: (String) -> Unit,
    val backspace: () -> Unit,
    /** Search: the app is told, and the ring closes onto the results. */
    val submit: () -> Unit,
    /** Back: the ring closes, the text stays. */
    val hide: () -> Unit,
    /**
     * LR-2: true while something over the app holds the knob (a ringing call, a notice with
     * buttons): Back is then its, not the ring's. Read as state, so the ring follows it. Set by the
     * app frame.
     */
    val held: () -> Boolean = { false },
) {
    /** The same input, held while [held] says so. */
    fun heldBy(held: () -> Boolean) = LetterRingInput(query, hint, found, type, backspace, submit, hide, held)
}

/** Where a SearchTemplate finds the skin's letter ring; null: the plain keyboard. Provided by the app frame. */
val LocalLetterRing = staticCompositionLocalOf<(@Composable (LetterRingInput, Modifier) -> Unit)?> { null }

/** The ring's keys and how the knob moves on them (pure, for the skin and the tests). */
object LetterRing {
    sealed interface Key {
        data class Letter(val c: Char) : Key
        data object Space : Key
        data object Delete : Key
        data object Search : Key
    }

    /** Clockwise from the top: A to Z, then space, delete and Search, which sits just left of A. */
    val KEYS: List<Key> = ('A'..'Z').map { Key.Letter(it) } + listOf(Key.Space, Key.Delete, Key.Search)

    /** The dispatcher (the design's open question): pad left and right jump this many keys, the knob one. */
    const val PAD_JUMP = 5

    /** The key [by] steps (a knob detent each; negative: back) from [index], round the ring. */
    fun move(index: Int, by: Int): Int = Math.floorMod(index + by, KEYS.size)

    /** Where key [index] sits, in degrees (0 = right, clockwise; -90 = top), in the middle of its share. */
    fun angleDeg(index: Int): Float = -90f + 360f / KEYS.size * (index + 0.5f)

    /** Pushes [key]: a letter is typed (lower case, as the design's field shows), the others do their job. */
    fun press(key: Key, input: LetterRingInput) = when (key) {
        is Key.Letter -> input.type(key.c.lowercaseChar().toString())
        Key.Space -> input.type(" ")
        Key.Delete -> input.backspace()
        Key.Search -> input.submit()
    }
}
