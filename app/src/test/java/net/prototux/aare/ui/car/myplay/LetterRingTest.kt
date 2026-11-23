package net.prototux.aare.ui.car.myplay

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.withKeyDown
import net.prototux.aare.ui.car.myplay.orbit.OrbitSkin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Look C (design 10): parked search's letter ring: its keys, and ORBIT's ring on the knob. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalTestApi::class)
class LetterRingTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private var text = ""
    private val done = mutableListOf<String>()
    private fun input(found: Int? = null) = LetterRingInput(
        text, "Search Waze", found,
        type = { text += it }, backspace = { text = text.dropLast(1) },
        submit = { done += "submit" }, hide = { done += "hide" },
    )

    @Test fun twentyNineKeysAToZThenSpaceDeleteSearch() {
        assertEquals(29, LetterRing.KEYS.size)
        assertEquals(LetterRing.Key.Letter('A'), LetterRing.KEYS.first())
        assertEquals(LetterRing.Key.Search, LetterRing.KEYS.last())
        // A just right of the top, Search just left of it.
        assertTrue(LetterRing.angleDeg(0) in -90f..-80f)
        assertTrue(LetterRing.angleDeg(28) in 260f..270f)
    }

    @Test fun theKnobGoesRoundTheRing() {
        assertEquals(1, LetterRing.move(0, 1))
        assertEquals(28, LetterRing.move(0, -1))
        assertEquals(0, LetterRing.move(28, 1))
        assertEquals(3, LetterRing.move(1, 31))
    }

    @Test fun aKeyDoesItsJob() {
        val i = input()
        LetterRing.press(LetterRing.Key.Letter('C'), i)
        LetterRing.press(LetterRing.Key.Space, i)
        LetterRing.press(LetterRing.Key.Letter('F'), i)
        assertEquals("c f", text)
        LetterRing.press(LetterRing.Key.Delete, i)
        assertEquals("c ", text)
        LetterRing.press(LetterRing.Key.Search, i)
        assertEquals(listOf("submit"), done)
    }

    private fun showOrbit(found: Int? = null) {
        rule.setContent {
            CompositionLocalProvider(LocalSkinTokens provides OrbitSkin.tokens(true)) {
                OrbitSkin.letterRing(input(found), Modifier)
            }
        }
        rule.waitForIdle()
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun turnAndPushTypesTheLetterTheKnobIsOn() {
        showOrbit()
        val ring = rule.onNodeWithTag(OrbitSkin.LETTER_RING_TAG)
        // From A, two turns on: C; push types it. One turn back from C is B.
        ring.performKeyInput { pressKey(Key.Tab); pressKey(Key.Tab); pressKey(Key.DirectionCenter) }
        assertEquals("c", text)
        ring.performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) }; pressKey(Key.Enter) }
        assertEquals("cb", text)
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun downSearchesAndATapTypes() {
        showOrbit(found = 6)
        rule.onNodeWithTag(OrbitSkin.letterKeyTag(5)).performClick()
        assertEquals("f", text)
        rule.onNodeWithText("6 places so far").assertExists()
        rule.onNodeWithTag(OrbitSkin.LETTER_RING_TAG).performKeyInput { pressKey(Key.DirectionDown) }
        assertEquals(listOf("submit"), done)
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun backClosesTheRing() {
        showOrbit()
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        assertEquals(listOf("hide"), done)
    }
}
