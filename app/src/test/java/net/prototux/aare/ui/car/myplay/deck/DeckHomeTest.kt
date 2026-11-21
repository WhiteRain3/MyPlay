package net.prototux.aare.ui.car.myplay.deck

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.car.app.model.DateTimeWithZone
import androidx.car.app.model.Distance
import androidx.car.app.navigation.model.Destination
import androidx.car.app.navigation.model.Maneuver
import androidx.car.app.navigation.model.Step
import androidx.car.app.navigation.model.TravelEstimate
import androidx.car.app.navigation.model.Trip
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.withKeyDown
import kotlinx.coroutines.flow.MutableStateFlow
import net.prototux.aare.car.CarData
import net.prototux.aare.car.LimitShown
import net.prototux.aare.car.PhoneFix
import net.prototux.aare.car.PhoneFixes
import net.prototux.aare.car.SpeedReading
import net.prototux.aare.ui.car.CarActions
import net.prototux.aare.ui.car.CarAppEntry
import net.prototux.aare.ui.car.CarLayout
import net.prototux.aare.ui.car.CarScreen
import net.prototux.aare.ui.car.CarShell
import net.prototux.aare.ui.car.CarUiState
import net.prototux.aare.ui.car.NowPlaying
import net.prototux.aare.ui.car.PadWalkStates
import net.prototux.aare.ui.car.myplay.ENGINE_TAG
import net.prototux.aare.ui.car.myplay.EngineSources
import net.prototux.aare.ui.car.myplay.LocalEngineSources
import net.prototux.aare.ui.car.myplay.MyPlaySkins
import net.prototux.aare.ui.car.myplay.SLOT_APPS
import net.prototux.aare.ui.car.myplay.SLOT_CHOOSER
import net.prototux.aare.ui.car.myplay.SLOT_TRIP
import net.prototux.aare.ui.car.myplay.STOP_HUB
import net.prototux.aare.ui.car.myplay.STOP_SONG
import net.prototux.aare.ui.car.myplay.STOP_TURN
import net.prototux.aare.ui.car.myplay.SpeedSource
import net.prototux.aare.ui.car.myplay.World
import net.prototux.aare.ui.car.myplay.orbit.OrbitSkin
import net.prototux.aare.ui.theme.CarTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.TimeZone

/** MyPlay (N13): DECK's home through the car shell, chosen by the language setting; its pictures and its knob. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class DeckHomeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @org.junit.After fun touchInputAfter() { net.prototux.aare.ui.car.CarFocusVisibility.keyInput = false }

    private val asked = mutableListOf<String>()
    private val actions = object : CarActions by PadWalkStates.NoActions {
        override fun openLauncher() { asked += "launcher" }
        override fun openApp(app: CarAppEntry) { asked += "app:${app.id}" }
    }

    private fun estimate(meters: Double, seconds: Long) = TravelEstimate.Builder(
        Distance.create(meters, Distance.UNIT_METERS),
        DateTimeWithZone.create(1_700_000_000_000L + seconds * 1000, TimeZone.getTimeZone("UTC")),
    ).setRemainingTimeSeconds(seconds).build()

    private fun route(): Trip {
        val step = Step.Builder("Turn right").setRoad("Hauptstraße")
            .setManeuver(Maneuver.Builder(Maneuver.TYPE_TURN_NORMAL_RIGHT).build()).build()
        return Trip.Builder()
            .addStep(step, estimate(350.0, 40))
            .addDestination(Destination.Builder().setName("Home").build(), estimate(9_400.0, 1_080))
            .build()
    }

    private fun art() = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply {
        for (x in 0 until 64) for (y in 0 until 64) setPixel(x, y, if (x < y) 0xFFFF8A3D.toInt() else 0xFF3D7BFF.toInt())
    }

    private object NoPhoneFixes : PhoneFixes {
        override fun start(onFix: (PhoneFix) -> Unit) = false
        override fun stop() = Unit
    }

    private fun show(night: Boolean = true, trip: Boolean = true, withArt: Boolean = false, song: Boolean = true, speed: SpeedReading? = null): CarUiState {
        val state = PadWalkStates.state(CarLayout.MAP_BEHIND, CarScreen.Dashboard).apply {
            enginePreview = true
            engineLanguage = "deck"
            this.night = night
            if (trip) this.trip = route()
            nowPlaying = if (song) NowPlaying("music", PadWalkStates.SONG_TITLE, "The Synthwaves", if (withArt) art() else null, playing = true,
                positionMs = 95_000, durationMs = 240_000, updatedAtMs = android.os.SystemClock.elapsedRealtime()) else null
        }
        rule.setContent {
            CarTheme(night = state.night, theme = state.theme) {
                val context = androidx.compose.ui.platform.LocalContext.current
                CompositionLocalProvider(LocalEngineSources provides remember(state) {
                    EngineSources.of(state, actions, context).let { s ->
                        if (speed == null) s else s.copy(speed = SpeedSource(MutableStateFlow(CarData.State()), { NoPhoneFixes }, speed))
                    }
                }) {
                    CarShell(state, actions, appContent = { _, m -> Box(m.background(Color.DarkGray)) },
                        navMap = { _, m -> Box(m.background(if (night) Color(0xFF10151B) else Color(0xFFDCE3E8))) })
                }
            }
        }
        rule.waitForIdle()
        return state
    }

    private val engine get() = rule.onNodeWithTag(ENGINE_TAG)
    private fun turn(steps: Int) = repeat(kotlin.math.abs(steps)) {
        if (steps > 0) engine.performKeyInput { pressKey(Key.Tab) } else engine.performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } }
    }

    private fun picture(name: String) {
        rule.mainClock.advanceTimeBy(600)
        rule.waitForIdle()
        val bmp = engine.captureToImage().asAndroidBitmap()
        val dir = File("build/reports/myplay-screens").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun languagesByTheirStoredId() {
        assertSame(DeckSkin, MyPlaySkins.byId("deck"))
        assertSame(OrbitSkin, MyPlaySkins.byId("orbit"))
        assertSame("anything else is ORBIT", OrbitSkin, MyPlaySkins.byId("race"))
        assertSame(OrbitSkin, MyPlaySkins.byId(null))
        assertEquals(listOf("orbit", "deck"), MyPlaySkins.ALL.map { it.id })
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun decksHomeIsTheHandWithMusicInFront() {
        show()
        rule.onNodeWithTag(deckCardTag(World.MUSIC)).assertExists()
        rule.onNodeWithText(PadWalkStates.SONG_TITLE).assertExists()
        rule.onNodeWithTag(STOP_SONG).assertDoesNotExist()
        rule.onNodeWithTag(STOP_HUB).assertIsSelected()
        rule.onNodeWithTag(SLOT_TRIP).assertExists()
        rule.onNodeWithText("18 min · 9400 m").assertExists() // the test route is in metres
        rule.onNodeWithTag(DECK_PROGRESS_TAG).assertExists()
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theKnobSwingsToAppsAndOpensThem() {
        show()
        turn(-1)
        rule.onNodeWithText("APPS").assertExists()
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(SLOT_APPS).assertExists()
        rule.onNodeWithTag(SLOT_CHOOSER).assertDoesNotExist()
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        rule.onNodeWithText("APPS").assertExists()
        rule.onNodeWithTag(STOP_HUB).assertIsSelected()
        // Past the last card, the turn read-out.
        turn(1); turn(1)
        rule.onNodeWithTag(STOP_TURN).assertIsSelected()
    }

    /** Nothing over the middle of the map: the hand (and so the map area it covers) stays right of the middle. */
    private fun handRightOfTheMiddle() {
        val screen = engine.fetchSemanticsNode().boundsInRoot
        val hand = rule.onNodeWithTag(STOP_HUB).fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue("hand from ${hand.left} on a screen ${screen.width} wide", hand.left > screen.center.x)
        org.junit.Assert.assertTrue(hand.right <= screen.right && hand.bottom <= screen.bottom)
    }

    /** The Reviewer's note on N13: a tap on the front card opens it, never a card behind it. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun aTapOnTheFrontCardOpensIt() {
        show()
        rule.onNodeWithTag(deckCardTag(World.MUSIC)).performClick()
        rule.onNodeWithTag(net.prototux.aare.ui.car.myplay.SLOT_MUSIC).assertExists()
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun theHandStaysRightOfTheMiddle1280() { show(); handRightOfTheMiddle() }
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun theHandStaysRightOfTheMiddle901() { show(); handRightOfTheMiddle() }
    @Test @Config(qualifiers = "w1200dp-h723dp-land-mdpi") fun theHandStaysRightOfTheMiddle1200() { show(); handRightOfTheMiddle() }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun home1280x720Night() { show(withArt = false); picture("deck-home-1280x720-night") }
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun home1280x720Day() { show(night = false, withArt = true); picture("deck-home-1280x720-day") }
    @Test @Config(qualifiers = "w1200dp-h723dp-land-mdpi") fun home1200x723Night() { show(withArt = true); picture("deck-home-1200x723-night") }
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun home901x543Night() { show(); picture("deck-home-901x543-night") }
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun home901x543Day() { show(night = false); picture("deck-home-901x543-day") }
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun appsInFrontNight() {
        net.prototux.aare.ui.car.CarFocusVisibility.keyInput = true // as after a knob turn in the car: the ring shows
        show(); turn(-1); picture("deck-apps-card-1280x720-night") }
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun navigateInFrontNoRouteNight() { show(trip = false, song = false); turn(-5); picture("deck-navigate-card-no-route-1280x720-night") }
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun speedOverTheLimitNight() {
        show(speed = SpeedReading(64, SpeedReading.From.CAR, LimitShown.Known(50), over = true)); picture("deck-speed-limit-over-1280x720-night")
    }
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun speedNoLimitKnownDay() {
        show(night = false, speed = SpeedReading(48, SpeedReading.From.CAR, LimitShown.Unknown)); picture("deck-speed-limit-unknown-901x543-day")
    }
}
