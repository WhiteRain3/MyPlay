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
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.withKeyDown
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import net.prototux.aare.car.CarData
import net.prototux.aare.media.MediaTransport
import net.prototux.aare.media.UpNextItem
import net.prototux.aare.ui.car.NowPlaying
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
        override fun mediaNext() { asked += "next" }
        override fun mediaPrevious() { asked += "previous" }
        override fun mediaPlayPause() { asked += "playPause" }
        override fun mediaSeekTo(positionMs: Long) { asked += "seek:$positionMs" }
        override fun mediaSkipTo(queueId: Long) { asked += "skip:$queueId" }
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

    /** [otherStop]: a focusable in the overlay, as a notice's buttons would be (N2-1). */
    private fun show(night: Boolean = true, guidance: Boolean = false, otherStop: Boolean = false): CarUiState {
        val state = PadWalkStates.state(CarLayout.MAP_BEHIND, CarScreen.Dashboard).apply {
            enginePreview = true
            this.night = night
            if (guidance) phoneGuidance = guidance()
        }
        this.state = state
        rule.setContent {
            CarTheme(night = state.night, theme = state.theme) {
                MyPlayEngineShell(state, actions, appContent = { _, m -> Box(m.background(Color.DarkGray)) },
                    navMap = { _, m -> covers = LocalMapCovers.current; Box(m.background(Color(0xFF2E4A3A))) },
                    overlay = { m -> if (otherStop) Box(m.size(40.dp).focusable().testTag("other_stop")) }, skin = TestSkin)
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
        // Calls: a world not drawn yet (Music and Apps draw their own).
        repeat(2) { engine.performKeyInput { pressKey(Key.Tab) } }
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(SLOT_WORLD).assertExists()
        rule.onNodeWithText(World.CALLS.name, useUnmergedTree = true).assertExists()

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
        ringed(STOP_HUB)
        // N6: pad right on the home is the next song while one is there; the ring stays.
        engine.performKeyInput { pressKey(Key.DirectionRight) }
        ringed(STOP_HUB)
        engine.performKeyInput { pressKey(Key.Tab) }
        ringed(STOP_SONG)
        engine.performKeyInput { pressKey(Key.Enter) }
        assertEquals(listOf("app:maps", "next", "app:music"), asked)
        // A tap does what a push does.
        rule.onNodeWithTag(STOP_TURN).performClick()
        assertEquals("app:maps", asked.last())
    }

    /** N2-1: turns inside a world not drawn yet stay with the engine; Back still leaves. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun aWorldKeepsTheKnob() {
        show(otherStop = true)
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(SLOT_WORLD).assertExists()
        repeat(3) { engine.performKeyInput { pressKey(Key.Tab) } }
        engine.performKeyInput { pressKey(Key.DirectionRight) }
        engine.assertIsFocused()
        rule.onNodeWithTag("other_stop").assertIsNotFocused()
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag(SLOT_CHOOSER).assertExists()
        repeat(2) { engine.performKeyInput { pressKey(Key.Tab) } }
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithText(World.CALLS.name, useUnmergedTree = true).assertExists()
    }

    /** N6: the Apps world lists the apps; knob, pad, push and tap; Back returns to the dial. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theAppsWorldWalksTheAppsAndOpensOne() {
        val state = show(otherStop = true)
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        // NAVIGATE, MUSIC, CALLS, APPS: three turns forward.
        repeat(3) { engine.performKeyInput { pressKey(Key.DirectionRight) } }
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(SLOT_APPS).assertExists()
        val apps = state.launcherApps
        fun ringed(i: Int) = rule.onNodeWithTag(appTag(apps[i])).assertIsSelected()
        ringed(0)
        engine.performKeyInput { pressKey(Key.Tab) }
        ringed(1)
        engine.performKeyInput { pressKey(Key.DirectionRight) }
        ringed(2)
        engine.performKeyInput { pressKey(Key.DirectionDown) }
        // The test skin's rows hold 5: down lands in the second row (which disc depends on the labels' widths).
        val below = apps.indices.single { i ->
            rule.onNodeWithTag(appTag(apps[i])).fetchSemanticsNode().config.getOrNull(SemanticsProperties.Selected) == true
        }
        assertTrue("row 2: $below", below in 5 until apps.size)
        engine.performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } }
        ringed(below - 1)
        // The knob wraps round.
        repeat(apps.size - (below - 1)) { engine.performKeyInput { pressKey(Key.Tab) } }
        ringed(0)
        engine.assertIsFocused()
        rule.onNodeWithTag("other_stop").assertIsNotFocused()
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(listOf("app:${apps[0].id}"), asked)
        rule.onNodeWithTag(appTag(apps[3])).performClick()
        assertEquals("app:${apps[3].id}", asked.last())
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag(SLOT_CHOOSER).assertExists()
        assertTrue("no app grid any more", "launcher" !in asked)
    }

    /** N7: the Music world with the test skin. */
    private fun openMusic(canSeek: Boolean = true): CarUiState {
        val state = show(otherStop = true)
        rule.runOnIdle {
            // Paused, so the position does not move while the test runs.
            state.nowPlaying = NowPlaying("music", PadWalkStates.SONG_TITLE, "The Synthwaves", null, playing = false,
                positionMs = 95_000, durationMs = 240_000, updatedAtMs = android.os.SystemClock.elapsedRealtime())
            state.transport = MediaTransport(canSeek = canSeek, canPrevious = true, canNext = true,
                upNext = listOf(UpNextItem(7, "Midnight Run", "Blue Harbor"), UpNextItem(8, "Glass Roads", "Kite & Co.")))
        }
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        engine.performKeyInput { pressKey(Key.Tab) } // NAVIGATE -> MUSIC
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(SLOT_MUSIC).assertExists()
        return state
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theMusicWorldPlaysSeeksAndSkips() {
        openMusic()
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        rule.onNodeWithTag(MUSIC_PLAY).assertIsSelected()
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(listOf("playPause"), asked)
        // The pad to the artwork above; there left and right seek 10 s (R5-5), from the last seek
        // while the player has not confirmed it (R5-4).
        engine.performKeyInput { pressKey(Key.DirectionUp) }
        rule.onNodeWithTag(MUSIC_ART).assertIsSelected()
        engine.performKeyInput { pressKey(Key.DirectionRight) }
        engine.performKeyInput { pressKey(Key.DirectionRight) }
        engine.performKeyInput { pressKey(Key.DirectionLeft) }
        assertEquals(listOf("playPause", "seek:105000", "seek:115000", "seek:105000"), asked)
        rule.onNodeWithTag(MUSIC_ART).assertIsSelected()
        // The knob still leaves the artwork; a next song plays when pushed.
        engine.performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } }
        engine.assertIsFocused()
        rule.onNodeWithTag(upNextStop(8)).performClick()
        assertEquals("skip:8", asked.last())
        rule.onNodeWithTag(MUSIC_LIBRARY).performClick()
        assertEquals("app:music", asked.last())
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag(SLOT_CHOOSER).assertExists()
    }

    /** Where the player does not allow seeking, left and right on the artwork move the ring instead. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun noSeekWithoutThePlayersLeave() {
        openMusic(canSeek = false)
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        engine.performKeyInput { pressKey(Key.DirectionUp) }
        rule.onNodeWithTag(MUSIC_ART).assertIsSelected()
        engine.performKeyInput { pressKey(Key.DirectionRight) }
        assertTrue(asked.none { it.startsWith("seek") })
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun nothingPlayingOffersTheLibrary() {
        val state = show()
        rule.runOnIdle { state.nowPlaying = null }
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        engine.performKeyInput { pressKey(Key.Tab) }
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(MUSIC_LIBRARY).assertIsSelected()
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(1, asked.size)
    }

    @Test fun appsPagesWrapAndPage() {
        assertEquals(1, AppsPages.pages(0, 10))
        assertEquals(2, AppsPages.pages(14, 10))
        assertEquals(1, AppsPages.pageOf(10, 10))
        assertEquals(13, AppsPages.knob(0, -1, 14))
        assertEquals(0, AppsPages.knob(13, 1, 14))
        assertEquals(0, AppsPages.knob(5, 1, 0))
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
                    val sources = EngineSources.of(state, actions, LocalContext.current).copy(speed = SpeedSource(car, { phone }))
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

    /** N11: the phone's picture gets a fixed speed, takes no focus and never listens for location. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun thePicturesSourcesListenToNothingAndTakeNoFocus() {
        val state = PadWalkStates.state(CarLayout.MAP_BEHIND, CarScreen.Dashboard).apply { enginePreview = true }
        rule.setContent {
            CarTheme(night = state.night, theme = state.theme) {
                CompositionLocalProvider(LocalEngineSources provides EngineSources.picture(state, actions)) {
                    CarShell(state, actions, appContent = { _, m -> Box(m) }, navMap = { _, m -> Box(m) })
                }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("48", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag(ENGINE_TAG).assertIsNotFocused()
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
