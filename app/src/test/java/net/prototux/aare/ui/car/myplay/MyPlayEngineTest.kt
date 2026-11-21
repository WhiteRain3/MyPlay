package net.prototux.aare.ui.car.myplay

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.withKeyDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import net.prototux.aare.car.CarData
import net.prototux.aare.car.PhoneFixes
import net.prototux.aare.car.SpeedSourceMode
import net.prototux.aare.host.templates.LocalMapCovers
import net.prototux.aare.host.templates.MapAreas
import net.prototux.aare.host.templates.MapCoverRegistry
import net.prototux.aare.host.templates.MapGeometry
import net.prototux.aare.notify.PhoneGuidance
import net.prototux.aare.ui.car.CarActions
import net.prototux.aare.ui.car.CarAppEntry
import net.prototux.aare.ui.car.CarLayout
import net.prototux.aare.ui.car.CarScreen
import net.prototux.aare.ui.car.CarShell
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
import java.io.File

/** MyPlay (N1, N2, N3): the engine with the test skin. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class MyPlayEngineTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private var state: CarUiState? = null
    private fun guidance() = PhoneGuidance("k", "mock.maps", "Maps", "300 m", "Main Street", "", null, null, "")

    /** What the engine asked of the app: "launcher", or "app:<id>". */
    private val asked = mutableListOf<String>()
    private val actions = object : CarActions by PadWalkStates.NoActions {
        override fun openLauncher() { asked += "launcher" }
        override fun openApp(app: CarAppEntry) { asked += "app:${app.id}" }
    }

    /** What the engine tells the maps app covers its map, as the map sees it. */
    private var covers: MapCoverRegistry? = null

    private fun bounds(tag: String) = rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        .let { MapGeometry.Box(it.left, it.top, it.right, it.bottom) }

    /** The map's free area, as the maps app would be told it, on the whole screen. */
    private fun freeArea(): MapAreas.Areas {
        val c = rule.runOnIdle { covers }!!
        val screen = bounds(ENGINE_TAG)
        fun boxes(m: Map<Any, androidx.compose.ui.geometry.Rect>) = m.values.map { MapGeometry.Box(it.left, it.top, it.right, it.bottom) }
        return MapAreas.compute(screen, boxes(c.shown), boxes(c.reserved))
    }

    private fun show(night: Boolean = true, guidance: Boolean = false): CarUiState {
        val state = PadWalkStates.state(CarLayout.MAP_BEHIND, CarScreen.Dashboard).apply {
            enginePreview = true
            this.night = night
            if (guidance) phoneGuidance = guidance()
        }
        this.state = state
        rule.setContent {
            CarTheme(night = state.night, theme = state.theme) {
                MyPlayEngineShell(state, actions, appContent = { _, m -> Box(m.background(Color.DarkGray)) },
                    navMap = { _, m -> covers = LocalMapCovers.current; Box(m.background(Color(0xFF2E4A3A))) }, overlay = {}, skin = TestSkin)
            }
        }
        rule.waitForIdle()
        return state
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun homeShowsTheSlotsAndTheKnobWalksTheWorlds() {
        show()
        rule.onNodeWithTag(ENGINE_TAG).assertExists()
        rule.onNodeWithTag(SLOT_STATUS).assertExists()
        rule.onNodeWithTag(STOP_SONG).assertExists()
        rule.onNodeWithTag(STOP_HUB).assertExists()
        rule.onNodeWithText(PadWalkStates.SONG_TITLE).assertExists()
        // No route in the fixture: no turn slot.
        assertTrue(rule.onAllNodes(hasTestTag(STOP_TURN)).fetchSemanticsNodes().isEmpty())

        val engine = rule.onNodeWithTag(ENGINE_TAG)
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(SLOT_CHOOSER).assertExists()
        engine.performKeyInput { pressKey(Key.Tab) }
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(SLOT_WORLD).assertExists()
        rule.onNodeWithText(World.MUSIC.name, useUnmergedTree = true).assertExists()

        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag(SLOT_CHOOSER).assertExists()
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        assertTrue(rule.onAllNodes(hasTestTag(SLOT_CHOOSER)).fetchSemanticsNodes().isEmpty())
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun knobAndPadWalkTheHomeStops() {
        show(guidance = true)
        fun ringed(id: String) = rule.onNodeWithTag(id).assertIsSelected()
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        ringed(STOP_HUB)
        // The knob: reading order, turn first, wrapping.
        engine.performKeyInput { pressKey(Key.Tab) }
        ringed(STOP_SONG)
        engine.performKeyInput { pressKey(Key.Tab) }
        ringed(STOP_TURN)
        engine.performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } }
        ringed(STOP_SONG)
        // The pad: nearest stop that way; nothing that way, it stays.
        engine.performKeyInput { pressKey(Key.DirectionLeft) }
        ringed(STOP_HUB)
        engine.performKeyInput { pressKey(Key.DirectionLeft) }
        ringed(STOP_HUB)
        engine.performKeyInput { pressKey(Key.DirectionUp) }
        ringed(STOP_TURN)
        // A push runs the stop: the turn opens the maps app, the song the music app.
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        engine.performKeyInput { pressKey(Key.DirectionDown) }
        engine.performKeyInput { pressKey(Key.DirectionRight) }
        ringed(STOP_SONG)
        engine.performKeyInput { pressKey(Key.Enter) }
        assertEquals(listOf("app:maps", "app:music"), asked)
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theAppsWorldOpensTheAppGrid() {
        show()
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        // NAVIGATE, MUSIC, CALLS, APPS: three turns forward.
        repeat(3) { engine.performKeyInput { pressKey(Key.DirectionRight) } }
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(listOf("launcher"), asked)
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theFreeAreaMissesEverySlotAndTheChooserDoesNotMoveIt() {
        show(guidance = true)
        val withAll = freeArea()
        for (tag in listOf(STOP_TURN, SLOT_STATUS, STOP_HUB, STOP_SONG)) {
            assertNull("$tag overlaps the free area", bounds(tag).intersect(withAll.visible))
        }
        assertTrue("the map keeps a usable middle", withAll.visible.width > 400f && withAll.visible.height > 200f)
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(SLOT_CHOOSER).assertExists()
        assertEquals(withAll, freeArea())
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun aSlotCountsFromWhenItAppears() {
        show(guidance = false)
        val area = freeArea()
        for (tag in listOf(SLOT_STATUS, STOP_HUB, STOP_SONG)) assertNull(tag, bounds(tag).intersect(area.visible))
        // When a turn appears, its slot is counted from then on.
        rule.runOnIdle { state!!.phoneGuidance = guidance() }
        rule.waitForIdle()
        assertNull(bounds(STOP_TURN).intersect(freeArea().visible))
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theSpeedListensOnlyWhileShownAndWithAMapsApp() {
        val state = PadWalkStates.state(CarLayout.MAP_BEHIND, CarScreen.Dashboard).apply { enginePreview = true }
        val car = MutableStateFlow(CarData.State(connected = true))
        val phone = object : PhoneFixes {
            var onFix: ((Float, Long) -> Unit)? = null
            override fun start(onFix: (Float, Long) -> Unit): Boolean { this.onFix = onFix; return true }
            override fun stop() { onFix = null }
        }
        var shown by mutableStateOf(true)
        rule.setContent {
            CarTheme(night = state.night, theme = state.theme) {
                if (shown) {
                    val sources = EngineSources.of(state, actions, LocalContext.current).copy(speed = SpeedSource(car) { phone })
                    MyPlayEngineShell(state, actions, appContent = { _, m -> Box(m) }, navMap = { _, m -> Box(m) }, overlay = {}, skin = TestSkin, sources = sources)
                }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("– km/h").assertExists()
        assertTrue("listening with a maps app", rule.runOnIdle { phone.onFix != null })
        rule.runOnIdle { phone.onFix!!(15f, System.currentTimeMillis()) }
        rule.onNodeWithText("54 km/h").assertExists()

        rule.runOnIdle { state.activeNavAppId = null }
        rule.waitForIdle()
        assertTrue("no maps app: no location", rule.runOnIdle { phone.onFix == null })
        rule.onNodeWithText("– km/h").assertExists()

        rule.runOnIdle { state.activeNavAppId = "maps" }
        rule.waitForIdle()
        assertTrue(rule.runOnIdle { phone.onFix != null })
        rule.runOnIdle { shown = false }
        rule.waitForIdle()
        assertTrue("off the screen: no location", rule.runOnIdle { phone.onFix == null })
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun speedOffDrawsNoSpeed() {
        val state = PadWalkStates.state(CarLayout.MAP_BEHIND, CarScreen.Dashboard).apply {
            enginePreview = true
            speedSource = SpeedSourceMode.OFF
        }
        rule.setContent {
            CarTheme(night = state.night, theme = state.theme) {
                CarShell(state, actions, appContent = { _, m -> Box(m) }, navMap = { _, m -> Box(m) })
            }
        }
        rule.waitForIdle()
        assertTrue(rule.onAllNodes(hasTestTag(SLOT_SPEED)).fetchSemanticsNodes().isEmpty())
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun screenshot1280x720Night() = screenshot("test-skin-1280x720-night", true, 1280, 720)
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun screenshot1280x720Day() = screenshot("test-skin-1280x720-day", false, 1280, 720)
    @Test @Config(qualifiers = "w1200dp-h723dp-land-mdpi") fun screenshot1200x723Night() = screenshot("test-skin-1200x723-night", true, 1200, 723)
    @Test @Config(qualifiers = "w1200dp-h723dp-land-mdpi") fun screenshot1200x723Day() = screenshot("test-skin-1200x723-day", false, 1200, 723)

    /** A picture of the home screen, saved under build/reports/myplay-screens for people to look at. */
    private fun screenshot(name: String, night: Boolean, w: Int, h: Int) {
        show(night)
        val bmp = rule.onNodeWithTag(ENGINE_TAG).captureToImage().asAndroidBitmap()
        assertEquals(w, bmp.width)
        assertEquals(h, bmp.height)
        val dir = File("build/reports/myplay-screens").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
