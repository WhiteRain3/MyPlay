package net.prototux.aare.ui.car.myplay

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.withKeyDown
import net.prototux.aare.ui.car.CarActions
import net.prototux.aare.ui.car.CarAppEntry
import net.prototux.aare.ui.car.CarLayout
import net.prototux.aare.ui.car.CarScreen
import net.prototux.aare.ui.car.CarUiState
import net.prototux.aare.ui.car.PadWalkStates
import net.prototux.aare.ui.theme.CarTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The test skin with a hand of cards, as DECK has: what the engine does with a hand (N13). */
internal object TestHandSkin : MyPlaySkin by TestSkin {
    override val id = "test-hand"
    override val hand = listOf(World.NAVIGATE, World.CALLS, World.CAR, World.SETTINGS, World.APPS, World.MUSIC)

    @Composable
    override fun Hand(front: World, song: SongSlot?, ringed: Boolean, pick: (World) -> Unit, modifier: Modifier) = Column(modifier) {
        Text("FRONT ${front.id}${song?.let { " · ${it.title}" }.orEmpty()}")
        for (w in hand) Text(w.id, Modifier.testTag("card_${w.id}").pointerInput(w) { detectTapGestures { pick(w) } })
    }

    @Composable
    override fun Trip(trip: TripSlot, modifier: Modifier) = Text("TRIP ${trip.arrival}", modifier)
}

/** MyPlay (N13): a skin with a hand of cards (DECK) on the shared engine. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class MyPlayHandTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val asked = mutableListOf<String>()
    private val actions = object : CarActions by PadWalkStates.NoActions {
        override fun openLauncher() { asked += "launcher" }
        override fun openApp(app: CarAppEntry) { asked += "app:${app.id}" }
        override fun mediaNext() { asked += "next" }
    }

    private fun show(turn: Boolean = true): CarUiState {
        val state = PadWalkStates.state(CarLayout.MAP_BEHIND, CarScreen.Dashboard).apply {
            enginePreview = true
            if (turn) phoneGuidance = net.prototux.aare.notify.PhoneGuidance("k", "mock.maps", "Maps", "300 m", "Main Street", "", null, null, "")
        }
        rule.setContent {
            CarTheme(night = state.night, theme = state.theme) {
                MyPlayEngineShell(state, actions, appContent = { _, m -> Box(m.background(Color.DarkGray)) },
                    navMap = { _, m -> Box(m.background(Color(0xFF2E4A3A))) }, overlay = {}, skin = TestHandSkin,
                    sources = EngineSources.of(state, actions, LocalContext.current))
            }
        }
        rule.waitForIdle()
        return state
    }

    private val engine get() = rule.onNodeWithTag(ENGINE_TAG)
    private fun turn(steps: Int) = repeat(kotlin.math.abs(steps)) {
        if (steps > 0) engine.performKeyInput { pressKey(Key.Tab) } else engine.performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } }
    }
    private fun push() = engine.performKeyInput { pressKey(Key.DirectionCenter) }
    private fun front(w: World) = rule.onNodeWithText("FRONT ${w.id}", substring = true).assertExists()

    @Test fun handMovesArePure() {
        val hand = TestHandSkin.hand
        assertEquals(World.APPS, HandMoves.turn(hand, World.MUSIC, -1))
        assertNull("past the end", HandMoves.turn(hand, World.MUSIC, 1))
        assertNull("past the start", HandMoves.turn(hand, World.NAVIGATE, -1))
        assertEquals(World.NAVIGATE, HandMoves.wrap(hand, 1))
        assertEquals(World.MUSIC, HandMoves.wrap(hand, -1))
        // Back from a world goes home with its card in front: no dial.
        val m = MyPlayScreenModel(hand)
        assertEquals(World.MUSIC, m.front)
        m.enter(World.CAR)
        assertTrue(m.back())
        assertEquals(Place.Home, m.place)
        assertEquals(World.CAR, m.front)
        assertTrue("Back at home is not ours", !m.back())
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theHandStartsOnMusicAndTheSongIsOnItsCardNotASlot() {
        show()
        front(World.MUSIC)
        rule.onNodeWithText(PadWalkStates.SONG_TITLE, substring = true).assertExists()
        rule.onNodeWithTag(STOP_SONG).assertDoesNotExist()
        rule.onNodeWithTag(STOP_HUB).assertIsSelected()
        rule.onNodeWithTag(SLOT_TRIP).assertDoesNotExist() // no route in this state
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun aTurnSwingsTheHandAndAPushOpensTheFrontCard() {
        show()
        turn(-1)
        front(World.APPS)
        rule.onNodeWithTag(STOP_HUB).assertIsSelected()
        push()
        rule.onNodeWithTag(SLOT_APPS).assertExists()
        // Back: home, with Apps still in front and the ring on the hand.
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        rule.onNodeWithTag(SLOT_APPS).assertDoesNotExist()
        rule.onNodeWithTag(SLOT_CHOOSER).assertDoesNotExist()
        front(World.APPS)
        rule.onNodeWithTag(STOP_HUB).assertIsSelected()
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun navigateOpensTheMapsAppAndStaysHome() {
        show()
        turn(-5)
        front(World.NAVIGATE)
        push()
        assertEquals(listOf("app:maps"), asked)
        rule.onNodeWithTag(SLOT_CHOOSER).assertDoesNotExist()
        front(World.NAVIGATE)
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun pastTheEndsTheKnobReachesTheOtherStops() {
        show(turn = true)
        // Music is the last card: one more click leaves the hand for the turn read-out, and back.
        turn(1)
        rule.onNodeWithTag(STOP_TURN).assertIsSelected()
        front(World.MUSIC)
        turn(1)
        rule.onNodeWithTag(STOP_HUB).assertIsSelected()
        front(World.MUSIC)
        turn(-1)
        front(World.APPS)
        // A push on the turn read-out opens the maps app, as in ORBIT.
        turn(-4)
        front(World.NAVIGATE)
        turn(-1)
        rule.onNodeWithTag(STOP_TURN).assertIsSelected()
        push()
        assertEquals(listOf("app:maps"), asked)
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun withNoOtherStopTheHandGoesRound() {
        show(turn = false)
        turn(1)
        front(World.NAVIGATE)
        rule.onNodeWithTag(STOP_HUB).assertIsSelected()
        turn(-1)
        front(World.MUSIC)
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun thePadSwingsTheHandAndRightIsNotNextSongHere() {
        show(turn = false)
        engine.performKeyInput { pressKey(Key.DirectionLeft) }
        front(World.APPS)
        engine.performKeyInput { pressKey(Key.DirectionRight) }
        front(World.MUSIC)
        // At the end the pad stops (no wrap, no next song).
        engine.performKeyInput { pressKey(Key.DirectionRight) }
        front(World.MUSIC)
        assertTrue(asked.isEmpty())
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun aTapOnACardBringsItForwardAndATapOnTheHandOpensIt() {
        show()
        rule.onNodeWithTag("card_${World.CAR.id}").performClick()
        front(World.CAR)
        rule.onNodeWithText("FRONT ${World.CAR.id}", substring = true).performClick()
        rule.onNodeWithTag(SLOT_CAR).assertExists()
    }
}
