package net.prototux.aare.ui.car.myplay.orbit

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import net.prototux.aare.ui.car.CarActions
import net.prototux.aare.ui.car.CarAppEntry
import net.prototux.aare.ui.car.CarLayout
import net.prototux.aare.ui.car.CarScreen
import net.prototux.aare.ui.car.CarShell
import net.prototux.aare.ui.car.CarUiState
import net.prototux.aare.ui.car.PadWalkStates
import net.prototux.aare.ui.car.myplay.ENGINE_TAG
import net.prototux.aare.ui.car.myplay.SLOT_CHOOSER
import net.prototux.aare.ui.car.myplay.STOP_HUB
import net.prototux.aare.ui.car.myplay.STOP_SONG
import net.prototux.aare.ui.car.myplay.STOP_TURN
import net.prototux.aare.ui.car.myplay.TripProgress
import net.prototux.aare.ui.car.myplay.TurnDirection
import net.prototux.aare.ui.theme.CarTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.TimeZone

/** MyPlay (N5): ORBIT's home through the car shell's hidden preview, its pictures and its knob walk. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class OrbitHomeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

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

    private fun show(night: Boolean = true, trip: Boolean = true): CarUiState {
        val state = PadWalkStates.state(CarLayout.MAP_BEHIND, CarScreen.Dashboard).apply {
            enginePreview = true
            this.night = night
            if (trip) this.trip = route()
        }
        rule.setContent {
            CarTheme(night = state.night, theme = state.theme) {
                CarShell(state, actions, appContent = { _, m -> Box(m.background(Color.DarkGray)) },
                    navMap = { _, m -> Box(m.background(if (night) Color(0xFF0B1318) else Color(0xFFDCE3E8))) })
            }
        }
        rule.waitForIdle()
        return state
    }

    @Test fun turnArrowsAreOnlyTheOnesKnown() {
        assertEquals(TurnDirection.RIGHT, TurnDirection.of(Maneuver.TYPE_TURN_NORMAL_RIGHT))
        assertEquals(TurnDirection.SLIGHT_LEFT, TurnDirection.of(Maneuver.TYPE_KEEP_LEFT))
        assertNull("a roundabout gets no guessed arrow", TurnDirection.of(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW))
        assertNull(TurnDirection.of(null))
    }

    @Test fun tripProgressFromTheLongestDistanceSeen() {
        val (l1, p1) = TripProgress.update(null, 10_000.0)
        assertEquals(0f, p1!!, 0.001f)
        val (l2, p2) = TripProgress.update(l1, 2_500.0)
        assertEquals(10_000.0, l2!!, 0.0)
        assertEquals(0.75f, p2!!, 0.001f)
        assertNull(TripProgress.update(l2, null).second)
        assertEquals(1_000.0, TripProgress.meters(1.0, Distance.UNIT_KILOMETERS)!!, 0.0)
    }

    @Test fun scaleFollowsTheScreen() {
        assertEquals(1f, OrbitSkin.scale(1280, 720), 0.001f)
        assertEquals(901f / 1280f, OrbitSkin.scale(901, 543), 0.001f)
        assertEquals(0.6f, OrbitSkin.scale(400, 240), 0.001f)
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theKnobWalksOrbitsHomeAndTheDialOpensApps() {
        show()
        rule.onNodeWithText("350 m").assertExists()
        rule.onNodeWithText("Hauptstraße").assertExists()
        rule.onNodeWithText("18 min").assertExists()
        rule.onNodeWithText(PadWalkStates.SONG_TITLE).assertExists()
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        rule.onNodeWithTag(STOP_HUB).assertIsSelected()
        engine.performKeyInput { pressKey(Key.Tab) }
        rule.onNodeWithTag(STOP_SONG).assertIsSelected()
        engine.performKeyInput { pressKey(Key.Tab) }
        rule.onNodeWithTag(STOP_TURN).assertIsSelected()
        engine.performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithTag(STOP_HUB).assertIsSelected()
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(SLOT_CHOOSER).assertExists()
        // The chosen world: on the dial and in the middle disc.
        assertEquals(2, rule.onAllNodesWithText("Navigate").fetchSemanticsNodes().size)
        repeat(3) { engine.performKeyInput { pressKey(Key.Tab) } }
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(listOf("launcher"), asked)
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun home1280x720Night() = picture("orbit-home-1280x720-night", night = true)
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun home1280x720Day() = picture("orbit-home-1280x720-day", night = false)
    @Test @Config(qualifiers = "w1200dp-h723dp-land-mdpi") fun home1200x723Night() = picture("orbit-home-1200x723-night", night = true)
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun home901x543Night() = picture("orbit-home-901x543-night", night = true)
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun homeNoRoute() = picture("orbit-home-no-route-night", night = true, trip = false)
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun dialNight() = picture("orbit-dial-1280x720-night", night = true, dial = true)
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun dial901x543Day() = picture("orbit-dial-901x543-day", night = false, dial = true)

    private fun picture(name: String, night: Boolean, trip: Boolean = true, dial: Boolean = false) {
        show(night, trip)
        if (dial) {
            val engine = rule.onNodeWithTag(ENGINE_TAG)
            engine.performKeyInput { pressKey(Key.DirectionCenter) }
            engine.performKeyInput { pressKey(Key.Tab) }
            rule.mainClock.advanceTimeBy(1_000)
        }
        val bmp = rule.onNodeWithTag(ENGINE_TAG).captureToImage().asAndroidBitmap()
        val dir = File("build/reports/myplay-screens").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
