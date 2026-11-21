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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import net.prototux.aare.car.CarData
import net.prototux.aare.media.LibraryRow
import net.prototux.aare.media.MediaLibrary
import net.prototux.aare.media.MediaTransport
import net.prototux.aare.media.UpNextItem
import net.prototux.aare.ui.car.NowPlaying
import net.prototux.aare.ui.car.CarCall
import net.prototux.aare.ui.car.CarNotice
import androidx.compose.ui.test.onAllNodesWithText
import org.robolectric.Shadows
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
import net.prototux.aare.car.PhoneFix
import net.prototux.aare.car.SpeedLimits
import net.prototux.aare.car.LimitShown

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
        override fun mediaLibrary(): MediaLibrary? = library
        override fun answerCall() { asked += "answer" }
        override fun dismissNotice(notice: CarNotice) { asked += "dismiss:${notice.key}"; state?.notices?.remove(notice) }
        override fun endCall() { asked += "end" }
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
    private fun show(night: Boolean = true, guidance: Boolean = false, otherStop: Boolean = false, phone: PhoneSource? = null, settings: SettingsSource? = null,
                     carLines: (@Composable () -> List<Pair<String, String>>)? = null): CarUiState {
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
                    overlay = { m -> if (otherStop) Box(m.size(40.dp).focusable().testTag("other_stop")) }, skin = TestSkin,
                    sources = EngineSources.of(state, actions, LocalContext.current).let { if (phone != null) it.copy(phone = phone) else it }
                        .let { if (settings != null) it.copy(settings = settings) else it }
                        .let { if (carLines != null) it.copy(carLines = carLines) else it })
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
        // A world: Car (reading its data); Back to the dial, Back home.
        repeat(4) { engine.performKeyInput { pressKey(Key.Tab) } }
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(SLOT_CAR).assertExists()

        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag(SLOT_CHOOSER).assertExists()
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        assertTrue(rule.onAllNodes(hasTestTag(SLOT_CHOOSER)).fetchSemanticsNodes().isEmpty())
        // Navigate is the maps app itself: it opens, and the dial closes. (The dial opens on the
        // last world, Car: two turns on, round to Navigate.)
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        repeat(2) { engine.performKeyInput { pressKey(Key.Tab) } }
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(listOf("app:maps"), asked)
        assertTrue(rule.onAllNodes(hasTestTag(SLOT_CHOOSER)).fetchSemanticsNodes().isEmpty())
        assertTrue(rule.onAllNodes(hasTestTag(SLOT_WORLD)).fetchSemanticsNodes().isEmpty())
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
        // N6-1: the pad alone reaches the song (right from the hub); right again, with nothing
        // further right, is the next song, and the ring stays on the song.
        engine.performKeyInput { pressKey(Key.DirectionRight) }
        ringed(STOP_SONG)
        engine.performKeyInput { pressKey(Key.DirectionRight) }
        ringed(STOP_SONG)
        engine.performKeyInput { pressKey(Key.Enter) }
        // O1: a push on the song opens the music panel, never the music app; Back closes it.
        rule.onNodeWithTag(SLOT_MUSIC_PANEL).assertExists()
        assertEquals(listOf("app:maps", "next"), asked)
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag(SLOT_MUSIC_PANEL).assertDoesNotExist()
        ringed(STOP_SONG)
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
        repeat(4) { engine.performKeyInput { pressKey(Key.Tab) } } // the Car world
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(SLOT_CAR).assertExists()
        repeat(3) { engine.performKeyInput { pressKey(Key.Tab) } }
        engine.performKeyInput { pressKey(Key.DirectionRight) }
        engine.assertIsFocused()
        rule.onNodeWithTag("other_stop").assertIsNotFocused()
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag(SLOT_CHOOSER).assertExists()
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(SLOT_CAR).assertExists()
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

    /** O1: the song's push opens the music panel; its knob and pad stay in it; the music app is one stop in it. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theSongOpensTheMusicPanel() {
        val state = show()
        rule.runOnIdle {
            state.nowPlaying = NowPlaying("music", PadWalkStates.SONG_TITLE, "The Synthwaves", null, playing = false,
                positionMs = 95_000, durationMs = 240_000, updatedAtMs = android.os.SystemClock.elapsedRealtime())
            state.transport = MediaTransport(canSeek = true, canPrevious = true, canNext = true)
        }
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        engine.performKeyInput { pressKey(Key.Tab) }
        rule.onNodeWithTag(STOP_SONG).assertIsSelected()
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(SLOT_MUSIC_PANEL).assertExists()
        assertTrue("the push opened no app: $asked", asked.isEmpty())
        // The ring starts on play/pause.
        rule.onNodeWithTag(PANEL_PLAY).assertIsSelected()
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        engine.performKeyInput { pressKey(Key.DirectionRight) }
        rule.onNodeWithTag(PANEL_NEXT).assertIsSelected()
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        // Right with nothing further: the ring stays (no song skipped by the pad here).
        engine.performKeyInput { pressKey(Key.DirectionRight) }
        rule.onNodeWithTag(PANEL_NEXT).assertIsSelected()
        engine.performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } }
        rule.onNodeWithTag(PANEL_PLAY).assertIsSelected()
        engine.performKeyInput { pressKey(Key.DirectionLeft) }
        rule.onNodeWithTag(PANEL_PREV).assertIsSelected()
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(listOf("playPause", "next", "previous"), asked)
        // Down to the progress bar: left and right seek there (R5-5); up leaves it.
        engine.performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithTag(PANEL_SEEK).assertIsSelected()
        engine.performKeyInput { pressKey(Key.DirectionRight) }
        assertEquals("seek:105000", asked.last())
        rule.onNodeWithTag(PANEL_SEEK).assertIsSelected()
        engine.performKeyInput { pressKey(Key.DirectionUp) }
        // The home's stops are out of reach while it is open: the knob stays in the panel.
        repeat(6) { engine.performKeyInput { pressKey(Key.Tab) } }
        rule.onNodeWithTag(SLOT_MUSIC_PANEL).assertExists()
        engine.assertIsFocused()
        // The music app is one stop in the panel; it closes the panel.
        rule.onNodeWithTag(PANEL_APP).performClick()
        assertEquals("app:music", asked.last())
        rule.onNodeWithTag(SLOT_MUSIC_PANEL).assertDoesNotExist()
        // A tap on the song opens it too; the song ending closes it.
        rule.onNodeWithTag(STOP_SONG).performClick()
        rule.onNodeWithTag(SLOT_MUSIC_PANEL).assertExists()
        rule.onNodeWithTag(PANEL_NEXT).performClick()
        assertEquals("next", asked.last())
        rule.runOnIdle { state.nowPlaying = null }
        rule.waitForIdle()
        rule.onNodeWithTag(SLOT_MUSIC_PANEL).assertDoesNotExist()
        assertTrue("never the music app from the disc: $asked", asked.count { it == "app:music" } == 1)
    }

    /** O2: the music panel and the dial close after the chosen time without input; any input starts it again. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun panelsCloseByThemselves() {
        val state = show()
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        fun open(tag: String) = rule.onNodeWithTag(tag).assertExists()
        fun closed(tag: String) = rule.onNodeWithTag(tag).assertDoesNotExist()
        // The music panel: still open just before 5 s.
        engine.performKeyInput { pressKey(Key.Tab) }
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.mainClock.advanceTimeBy(4_500)
        open(SLOT_MUSIC_PANEL)
        // A knob turn starts the time again; then 5 s untouched closes it, the ring on the song.
        engine.performKeyInput { pressKey(Key.Tab) }
        rule.mainClock.advanceTimeBy(4_500)
        open(SLOT_MUSIC_PANEL)
        rule.mainClock.advanceTimeBy(1_000)
        closed(SLOT_MUSIC_PANEL)
        rule.onNodeWithTag(STOP_SONG).assertIsSelected()
        // A touch counts as input too.
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.mainClock.advanceTimeBy(4_500)
        rule.onNodeWithTag(PANEL_PREV).performClick()
        rule.mainClock.advanceTimeBy(4_500)
        open(SLOT_MUSIC_PANEL)
        rule.mainClock.advanceTimeBy(1_000)
        closed(SLOT_MUSIC_PANEL)
        // The dial, likewise; the ring back on the hub.
        engine.performKeyInput { pressKey(Key.DirectionLeft) }
        rule.onNodeWithTag(STOP_HUB).assertIsSelected()
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        open(SLOT_CHOOSER)
        rule.mainClock.advanceTimeBy(5_500)
        closed(SLOT_CHOOSER)
        rule.onNodeWithTag(STOP_HUB).assertIsSelected()
        // A real screen (a world) never closes so.
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        repeat(4) { engine.performKeyInput { pressKey(Key.Tab) } }
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        open(SLOT_CAR)
        rule.mainClock.advanceTimeBy(30_000)
        open(SLOT_CAR)
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        // The driver's time: 15 s, and never.
        rule.runOnIdle { state.panelTimeoutS = 15 }
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.mainClock.advanceTimeBy(10_000)
        open(SLOT_CHOOSER)
        rule.mainClock.advanceTimeBy(5_500)
        closed(SLOT_CHOOSER)
        rule.runOnIdle { state.panelTimeoutS = PanelTimeout.NEVER }
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.mainClock.advanceTimeBy(60_000)
        open(SLOT_CHOOSER)
    }

    @Test fun panelTimeoutsAreTheChoicesOnly() {
        assertEquals(5_000L, PanelTimeout.ms(5))
        assertNull(PanelTimeout.ms(PanelTimeout.NEVER))
        assertEquals(PanelTimeout.DEFAULT, PanelTimeout.valid(7))
        assertEquals(15, PanelTimeout.valid(15))
    }

    /** O1: without the player's leave to seek the panel has no progress bar stop. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun thePanelSeeksOnlyWithThePlayersLeave() {
        val state = show()
        rule.runOnIdle { state.transport = MediaTransport(canSeek = false, canPrevious = true, canNext = true) }
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        engine.performKeyInput { pressKey(Key.Tab) }
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(SLOT_MUSIC_PANEL).assertExists()
        rule.onNodeWithTag(PANEL_SEEK).assertDoesNotExist()
        engine.performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithTag(PANEL_PLAY).assertIsSelected()
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

    /** N6-2: a tap opens the app the disc shows now, also after the list changed under it. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun aTapOpensTheAppShownAfterTheListChanged() {
        val state = show()
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        repeat(3) { engine.performKeyInput { pressKey(Key.DirectionRight) } }
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(SLOT_APPS).assertExists()
        // The first app goes away (uninstalled, or moved on the phone): every disc now shows another app.
        rule.runOnIdle { state.apps.removeAt(0) }
        rule.waitForIdle()
        val first = state.launcherApps.first()
        rule.onNodeWithTag(appTag(first)).performClick()
        assertEquals(listOf("app:${first.id}"), asked)
    }

    /** The library the engine is given (R4-2's test seam): null means "the app has none". */
    private var library: FakeLibrary? = null

    /** A music app's library: a root with a folder and a song; the folder holds [inFolder] songs, paged by ten. */
    private class FakeLibrary(val inFolder: Int = 2) : MediaLibrary {
        var started = 0
        var stopped = 0
        val played = mutableListOf<String>()
        var loadMores = 0
        private var inside by mutableStateOf(false)
        private var shown by mutableStateOf(10)
        override val title get() = if (inside) "Albums" else "Music app"
        override val key get() = if (inside) "albums" else "root"
        override val rows get() = if (inside) (1..minOf(inFolder, shown)).map { LibraryRow("song$it", "Song $it", "Artist", browsable = false, playable = true) }
            else listOf(LibraryRow("albums", "Albums", "", browsable = true, playable = false), LibraryRow("top", "Top song", "Artist", browsable = false, playable = true))
        override val loading = false
        override val end get() = !inside || shown >= inFolder
        override val error: String? = null
        override val canGoBack get() = inside
        override fun open(row: LibraryRow): Boolean {
            if (row.browsable) { inside = true; return false }
            played += row.id
            return true
        }
        override fun back() { inside = false }
        override fun loadMore() { loadMores++; shown += 10 }
        override fun start() { started++ }
        override fun stop() { stopped++ }
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theLibraryWalksOpensPlaysAndGoesBack() {
        val lib = FakeLibrary().also { library = it }
        openMusic()
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        rule.onNodeWithTag(MUSIC_LIBRARY).performClick()
        rule.onNodeWithTag(SLOT_LIBRARY).assertExists()
        assertEquals(1, lib.started)
        rule.onNodeWithTag(libraryRowTag(0)).assertIsSelected()
        engine.performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithTag(libraryRowTag(1)).assertIsSelected()
        engine.performKeyInput { pressKey(Key.DirectionUp) }
        // Into the folder: the ring starts at its top.
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithText("Song 1", substring = true).assertExists()
        rule.onNodeWithTag(libraryRowTag(0)).assertIsSelected()
        // Back goes up the library, then out of it to now playing, then to the dial.
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithText("Albums", substring = true).assertExists()
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag(SLOT_MUSIC).assertExists()
        assertEquals(1, lib.stopped)
        // A song plays: back to now playing.
        rule.onNodeWithTag(MUSIC_LIBRARY).performClick()
        engine.performKeyInput { pressKey(Key.Tab) }
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(listOf("top"), lib.played)
        rule.onNodeWithTag(SLOT_MUSIC).assertExists()
        engine.assertIsFocused()
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag(SLOT_CHOOSER).assertExists()
    }

    /** The next page loads as the ring nears the end of what is loaded. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theLibraryLoadsMoreNearTheEnd() {
        val lib = FakeLibrary(inFolder = 25).also { library = it }
        openMusic()
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        rule.onNodeWithTag(MUSIC_LIBRARY).performClick()
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(0, lib.loadMores)
        repeat(7) { engine.performKeyInput { pressKey(Key.DirectionDown) } }
        rule.waitForIdle()
        assertEquals(1, lib.loadMores)
        repeat(5) { engine.performKeyInput { pressKey(Key.DirectionDown) } }
        rule.onNodeWithTag(libraryRowTag(12)).assertIsSelected()
    }

    /** Without a library (no media app known), the button opens the app as before. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun noLibraryOpensTheApp() {
        library = null
        openMusic()
        rule.onNodeWithTag(MUSIC_LIBRARY).performClick()
        assertEquals("app:music", asked.last())
        assertTrue(rule.onAllNodes(hasTestTag(SLOT_LIBRARY)).fetchSemanticsNodes().isEmpty())
    }

    private fun allowEndingCalls() =
        Shadows.shadowOf(rule.activity.application).grantPermissions(android.Manifest.permission.ANSWER_PHONE_CALLS)

    /** N8: a ringing call takes the knob and the pad; R5-2: afterwards the ring is where it was. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun aRingingCallAnswersOrDeclinesAndTheRingComesBack() {
        allowEndingCalls()
        val state = show(otherStop = true)
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        engine.performKeyInput { pressKey(Key.Tab) }
        rule.onNodeWithTag(STOP_SONG).assertIsSelected()
        rule.runOnIdle { state.call = CarCall(ringing = true, name = "Anna K.", sinceMs = 1L) }
        rule.waitForIdle()
        rule.onNodeWithTag(SLOT_CALL).assertExists()
        rule.onNodeWithTag(CALL_ANSWER).assertIsSelected()
        // Turns and the pad stay inside the call (the test skin puts Decline right of Answer).
        engine.performKeyInput { pressKey(Key.Tab) }
        rule.onNodeWithTag(CALL_DECLINE).assertIsSelected()
        engine.performKeyInput { pressKey(Key.DirectionLeft) }
        rule.onNodeWithTag(CALL_ANSWER).assertIsSelected()
        engine.assertIsFocused()
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(listOf("answer"), asked)
        // The call is answered, then over: the ring is on the song again.
        rule.runOnIdle { state.call = null }
        rule.waitForIdle()
        rule.onNodeWithTag(STOP_SONG).assertIsSelected()
        // A second call, declined by a tap.
        rule.runOnIdle { state.call = CarCall(ringing = true, name = "Ben", sinceMs = 2L) }
        rule.waitForIdle()
        rule.onNodeWithTag(CALL_DECLINE).performClick()
        assertEquals("end", asked.last())
    }

    /** C-1: while a call rings, a tap on the screen around it reaches nothing underneath. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun aRingingCallKeepsTapsFromTheHome() {
        val state = show()
        rule.runOnIdle { state.call = CarCall(ringing = true, name = "Anna K.", sinceMs = 1L) }
        rule.waitForIdle()
        rule.onNodeWithTag(STOP_HUB).performClick()
        rule.onNodeWithTag(STOP_SONG).performClick()
        assertTrue(rule.onAllNodes(hasTestTag(SLOT_CHOOSER)).fetchSemanticsNodes().isEmpty())
        assertTrue("nothing opened: $asked", asked.isEmpty())
        // Answer itself still takes its tap.
        rule.onNodeWithTag(CALL_ANSWER).performClick()
        assertEquals(listOf("answer"), asked)
    }

    /** Without the permission to end calls there is no Decline: Answer only. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun noDeclineWithoutThePermission() {
        val state = show()
        rule.runOnIdle { state.call = CarCall(ringing = true, name = "Anna K.", sinceMs = 1L) }
        rule.waitForIdle()
        rule.onNodeWithTag(CALL_ANSWER).assertIsSelected()
        assertTrue(rule.onAllNodes(hasTestTag(CALL_DECLINE)).fetchSemanticsNodes().isEmpty())
    }

    /** In a call, its strip is on the home: the knob reaches it; it opens the Calls world (F4), or hangs up. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun inACallTheStripOpensTheCallsWorldOrHangsUp() {
        allowEndingCalls()
        val state = show()
        rule.runOnIdle { state.call = CarCall(ringing = false, name = "Anna K.", sinceMs = System.currentTimeMillis()) }
        rule.waitForIdle()
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        // Reading order: the strip is at the top, first after a wrap from the song.
        engine.performKeyInput { pressKey(Key.Tab) }
        engine.performKeyInput { pressKey(Key.Tab) }
        rule.onNodeWithTag(STOP_CALL).assertIsSelected()
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        // F4: MyPlay's own Calls world, never AARE's phone app.
        rule.onNodeWithTag(SLOT_PHONE).assertExists()
        assertTrue("no app opened: $asked", asked.none { it.startsWith("app:") })
        // Back to the dial, and home: the strip again, and Hang up next to it.
        repeat(2) { rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle() }
        rule.onNodeWithTag(STOP_CALL).assertIsSelected()
        engine.performKeyInput { pressKey(Key.Tab) }
        rule.onNodeWithTag(STOP_HANG_UP).assertIsSelected()
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals("end", asked.last())
    }

    /** F4: the car runtime asks for a world (a call placed from an app opens the Calls world); once. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun aWorldTheRuntimeAsksForOpensOnce() {
        val state = show()
        rule.runOnIdle { state.engineOpenWorld = World.CALLS.id }
        rule.waitForIdle()
        rule.onNodeWithTag(SLOT_PHONE).assertExists()
        assertNull("the request is used up", rule.runOnIdle { state.engineOpenWorld })
        assertTrue(asked.none { it.startsWith("app:") })
        // Something unknown is ignored (and left for no one).
        rule.runOnIdle { state.engineOpenWorld = "nowhere" }
        rule.waitForIdle()
        rule.onNodeWithTag(SLOT_PHONE).assertExists()
    }

    /** F1-3 (R6-3): on the home the gesture line is under a notice, never on it; in a world it shows over the page. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theGestureLineIsUnderTheNoticeAndOverAWorld() {
        val state = show()
        rule.runOnIdle {
            state.notices.add(0, CarNotice("n1", "Anna K.", "Running late", null))
            state.gestureLine = net.prototux.aare.ui.car.GestureLine(net.prototux.aare.car.ButtonAction.NEXT, forward = true, atMs = 1)
        }
        rule.waitForIdle()
        val line = rule.onNodeWithTag(net.prototux.aare.ui.car.GESTURE_LINE_TAG).fetchSemanticsNode().boundsInRoot
        val dismiss = rule.onNodeWithTag(NOTICE_DISMISS).fetchSemanticsNode().boundsInRoot
        assertTrue("under the notice: $line, $dismiss", line.top >= dismiss.bottom)
        // In a world (the dial, then Navigate's neighbour Music): still shown.
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        engine.performKeyInput { pressKey(Key.DirectionRight) }
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(SLOT_MUSIC).assertExists()
        rule.onNodeWithTag(net.prototux.aare.ui.car.GESTURE_LINE_TAG).assertExists()
    }

    /** A phone with two recent calls (one missed) and two favourites, one of them also a recent call. */
    private class FakePhone(override val permitted: Boolean = true) : PhoneSource {
        val called = mutableListOf<String>()
        var opened = 0
        /** A call that came in since: on top of the recents at the next load. */
        var newest: PhoneEntry? = null
        override suspend fun recents() = listOfNotNull(newest) + listOf(PhoneEntry("Anna K.", "111", "2 min ago", missed = true), PhoneEntry("Ben", "222", "1 hour ago"))
        override suspend fun favourites() = listOf(PhoneEntry("Carla", "333", "Mobile"), PhoneEntry("Ben", "222", "Mobile"))
        override fun call(entry: PhoneEntry) { called += entry.number }
        override fun open() { opened++ }
    }

    private fun openCalls(phone: PhoneSource) {
        show(otherStop = true, phone = phone)
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        repeat(2) { engine.performKeyInput { pressKey(Key.Tab) } } // NAVIGATE, MUSIC, CALLS
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(SLOT_PHONE).assertExists()
    }

    /** N8: the Calls world lists recent calls, then favourites not already there, then the phone app. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theCallsWorldCallsAndOpensThePhone() {
        val phone = FakePhone()
        openCalls(phone)
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        for ((i, name) in listOf("Anna K.", "Ben", "Carla", "Keypad and all contacts").withIndex()) {
            rule.onNodeWithTag(phoneRowTag(i)).assertExists()
            rule.onNodeWithText(name, useUnmergedTree = true).assertExists()
        }
        assertTrue("Ben once", rule.onAllNodes(hasTestTag(phoneRowTag(4))).fetchSemanticsNodes().isEmpty())
        rule.onNodeWithTag(phoneRowTag(0)).assertIsSelected()
        engine.performKeyInput { pressKey(Key.DirectionDown) }
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(listOf("222"), phone.called)
        repeat(3) { engine.performKeyInput { pressKey(Key.Tab) } }
        rule.onNodeWithTag(phoneRowTag(3)).assertIsSelected()
        engine.assertIsFocused()
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(1, phone.opened)
        rule.onNodeWithTag(phoneRowTag(2)).performClick()
        assertEquals(listOf("222", "333"), phone.called)
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag(SLOT_CHOOSER).assertExists()
    }

    /** P-1: the list reloads after a call with a new call on top; the ring stays on the same person. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theRingStaysOnThePersonWhenTheListReloads() {
        val phone = FakePhone()
        openCalls(phone)
        val state = this.state!!
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        engine.performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithText("Ben", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag(phoneRowTag(1)).assertIsSelected()
        // A call comes and goes: the list reloads with Dora on top, and Ben moves to row 2.
        phone.newest = PhoneEntry("Dora", "444", "now")
        rule.runOnIdle { state.call = CarCall(ringing = false, name = "Dora", sinceMs = 1L) }
        rule.waitForIdle()
        rule.runOnIdle { state.call = null }
        rule.waitForIdle()
        rule.onNodeWithText("Dora", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag(phoneRowTag(2)).assertIsSelected()
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(listOf("222"), phone.called)
    }

    /** Without the phone's permissions: nothing is read; only the phone app is offered. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theCallsWorldWithoutPermission() {
        val phone = FakePhone(permitted = false)
        openCalls(phone)
        rule.onNodeWithText("NO PERMISSION").assertExists()
        rule.onNodeWithTag(phoneRowTag(0)).assertIsSelected()
        assertTrue(rule.onAllNodes(hasTestTag(phoneRowTag(1))).fetchSemanticsNodes().isEmpty())
        rule.onNodeWithTag(ENGINE_TAG).performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(1, phone.opened)
        assertTrue(phone.called.isEmpty())
    }

    private fun aNotice(message: Boolean = false) =
        CarNotice("n1", "Anna K.", "Running late, see you at 6", null, listOf("Reply" to { asked += "reply" }), message = message)

    /** N8: a notice's action and Dismiss are knob stops on the home; a push runs the action and dismisses it. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun aNoticesButtonsAreReachedByTheKnob() {
        val state = show(otherStop = true)
        rule.runOnIdle { state.notices.add(0, aNotice()) }
        rule.waitForIdle()
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        // Reading order: the notice sits at the top, before the hub and the song.
        engine.performKeyInput { pressKey(Key.Tab) }
        engine.performKeyInput { pressKey(Key.Tab) }
        rule.onNodeWithTag(noticeActionStop(0)).assertIsSelected()
        engine.assertIsFocused()
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(listOf("reply", "dismiss:n1"), asked)
        // Gone: the ring is back on the hub.
        rule.waitForIdle()
        rule.onNodeWithTag(STOP_HUB).assertIsSelected()
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun aNoticeGoesAwayByItself() {
        val state = show()
        rule.runOnIdle { state.notices.add(0, aNotice()) }
        rule.waitForIdle()
        rule.onNodeWithTag(NOTICE_DISMISS).assertExists()
        rule.mainClock.advanceTimeBy(NOTICE_SHOW_MS + 100)
        rule.waitForIdle()
        assertEquals(listOf("dismiss:n1"), asked)
        assertTrue(rule.onAllNodes(hasTestTag(NOTICE_DISMISS)).fetchSemanticsNodes().isEmpty())
    }

    /** While driving a message shows who wrote, not what. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun aMessagesTextIsHiddenWhileDriving() {
        val state = show()
        rule.runOnIdle { state.drivingRestricted = true; state.notices.add(0, aNotice(message = true)) }
        rule.waitForIdle()
        rule.onNodeWithText("Anna K.", substring = true).assertExists()
        assertTrue(rule.onAllNodesWithText("Running late", substring = true).fetchSemanticsNodes().isEmpty())
    }

    /** In a world the notice shows, but its buttons are not stops there (the world keeps the knob). */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun inAWorldTheNoticeOnlyShows() {
        val state = show()
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        repeat(3) { engine.performKeyInput { pressKey(Key.Tab) } }
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(SLOT_APPS).assertExists()
        rule.runOnIdle { state.notices.add(0, aNotice()) }
        rule.waitForIdle()
        rule.onNodeWithTag(NOTICE_DISMISS).assertExists()
        engine.performKeyInput { pressKey(Key.Tab) }
        rule.onNodeWithTag(appTag(state.launcherApps[1])).assertIsSelected()
    }

    private fun twoInHistory(state: CarUiState) = rule.runOnIdle {
        state.history.add(CarNotice("h1", "Anna K.", "Running late", null, listOf("Reply" to { asked += "reply:h1" })))
        state.history.add(CarNotice("h2", "Calendar", "Dentist at 15:00", null))
        state.unseen = 2
    }

    /** N8: the bell shows only with unseen notifications; it opens the list, which marks them seen. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theBellOpensTheNotificationsList() {
        val state = show(otherStop = true)
        assertTrue("no bell with nothing unseen", rule.onAllNodes(hasTestTag(STOP_BELL)).fetchSemanticsNodes().isEmpty())
        twoInHistory(state)
        rule.waitForIdle()
        rule.onNodeWithText("BELL 2").assertExists()
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        // Reading order: the bell is under the clock, top right: after the hub and the song, round to the top.
        repeat(2) { engine.performKeyInput { pressKey(Key.Tab) } }
        rule.onNodeWithTag(STOP_BELL).assertIsSelected()
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(SLOT_NOTICES).assertExists()
        assertEquals(0, state.unseen)
        rule.onNodeWithTag(noticeRowTag(0)).assertIsSelected()
        // A push runs the row's first action; the list stays.
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(listOf("reply:h1"), asked)
        engine.assertIsFocused()
        // The second has no action: a push does nothing. The last row clears all and closes.
        engine.performKeyInput { pressKey(Key.DirectionDown) }
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(listOf("reply:h1"), asked)
        engine.performKeyInput { pressKey(Key.DirectionDown) }
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.waitForIdle()
        assertTrue(state.history.isEmpty())
        assertTrue(rule.onAllNodes(hasTestTag(SLOT_NOTICES)).fetchSemanticsNodes().isEmpty())
        assertTrue("the bell is gone", rule.onAllNodes(hasTestTag(STOP_BELL)).fetchSemanticsNodes().isEmpty())
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theListClosesWithBackAndHidesTextWhileDriving() {
        val state = show()
        rule.runOnIdle { state.drivingRestricted = true }
        twoInHistory(state)
        rule.onNodeWithTag(STOP_BELL).performClick()
        rule.onNodeWithTag(SLOT_NOTICES).assertExists()
        assertTrue(rule.onAllNodesWithText("Running late", substring = true).fetchSemanticsNodes().isEmpty())
        rule.onNodeWithText("Anna K.", substring = true).assertExists()
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        assertTrue(rule.onAllNodes(hasTestTag(SLOT_NOTICES)).fetchSemanticsNodes().isEmpty())
        assertEquals(2, state.history.size)
    }

    /** Two settings: a switch and a choice of three. */
    private class FakeSettings : SettingsSource {
        var on = false
        var colour = "auto"
        override fun items() = listOf(
            SettingItem.Toggle("banners", "Message notifications", on) { on = it },
            SettingItem.Choice("colors", "Colors", listOf("auto" to "Auto", "dark" to "Dark", "light" to "Light"), colour) { colour = it },
        )
    }

    /** N9: the Settings world: a push flips a switch or steps a choice; the ring stays on the setting. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theSettingsWorldChangesSettings() {
        val fake = FakeSettings()
        show(otherStop = true, settings = fake)
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        repeat(5) { engine.performKeyInput { pressKey(Key.Tab) } } // ... CAR, SETTINGS
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(SLOT_SETTINGS).assertExists()
        rule.onNodeWithTag(settingRowTag("banners")).assertIsSelected()
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        assertTrue(fake.on)
        rule.onNodeWithText("Message notifications: On").assertExists()
        engine.performKeyInput { pressKey(Key.DirectionDown) }
        repeat(3) { engine.performKeyInput { pressKey(Key.DirectionCenter) } }
        assertEquals("auto", fake.colour) // dark, light, and round to auto
        rule.onNodeWithTag(settingRowTag("colors")).assertIsSelected()
        engine.assertIsFocused()
        rule.onNodeWithTag(settingRowTag("banners")).performClick()
        assertTrue(!fake.on)
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag(SLOT_CHOOSER).assertExists()
    }

    /** The real settings: a change is stored where the car runtime reads it. */
    @Test fun theCarsSettingsAreStored() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val source = CarSettingsSource(context, PadWalkStates.state(CarLayout.MAP_BEHIND, CarScreen.Dashboard))
        val passenger = source.items().filterIsInstance<SettingItem.Toggle>().single { it.id == "passenger" }
        passenger.set(!passenger.on)
        assertEquals(!passenger.on, net.prototux.aare.Settings(context).ignoreDriving)
        val speed = source.items().filterIsInstance<SettingItem.Choice>().single { it.id == "speed_source" }
        speed.select(speed.next)
        assertEquals(speed.next, source.items().filterIsInstance<SettingItem.Choice>().single { it.id == "speed_source" }.selected)
        // The home maps choice offers the maps apps of the car.
        val maps = source.items().filterIsInstance<SettingItem.Choice>().single { it.id == "home_maps" }
        assertTrue(maps.options.any { it.second == "Maps" })
    }

    /** N9: the Car world shows the car's data to read; the ring stays on a line as values change. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theCarWorldReadsTheCarsData() {
        var speed by mutableStateOf("48 km/h")
        var withFuel by mutableStateOf(false)
        show(otherStop = true, carLines = {
            listOfNotNull(("Fuel" to "62% · 480 km").takeIf { withFuel }, "Speed" to speed, "Outside" to "7.5 °C", "Tyres" to "230 · 230 · 240 · 240 kPa")
        })
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        repeat(4) { engine.performKeyInput { pressKey(Key.Tab) } } // ... APPS, CAR
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(SLOT_CAR).assertExists()
        rule.onNodeWithTag(carLineTag(0)).assertIsSelected()
        engine.performKeyInput { pressKey(Key.DirectionDown) }
        rule.onNodeWithTag(carLineTag(1)).assertIsSelected()
        // A new line comes in above (the car sent its fuel) and the speed changes: the ring stays on "Outside".
        rule.runOnIdle { withFuel = true; speed = "50 km/h" }
        rule.waitForIdle()
        rule.onNodeWithText("Outside: 7.5 °C").assertExists()
        rule.onNodeWithTag(carLineTag(2)).assertIsSelected()
        // Nothing to push.
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        assertTrue(asked.isEmpty())
        engine.assertIsFocused()
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithTag(SLOT_CHOOSER).assertExists()
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theCarWorldWithoutACar() {
        show(carLines = { emptyList() })
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        repeat(4) { engine.performKeyInput { pressKey(Key.Tab) } }
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithText("NO CAR").assertExists()
        engine.performKeyInput { pressKey(Key.DirectionDown) }
        engine.assertIsFocused()
    }

    /** S-1: passenger mode can be turned off at any time, but on only while the car is stopped. */
    @Test fun passengerModeTurnsOnOnlyWhileStopped() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val settings = net.prototux.aare.Settings(context)
        val state = PadWalkStates.state(CarLayout.MAP_BEHIND, CarScreen.Dashboard)
        val source = CarSettingsSource(context, state)
        fun passenger() = source.items().filterIsInstance<SettingItem.Toggle>().single { it.id == "passenger" }
        settings.ignoreDriving = false
        state.drivingRestricted = true // moving
        assertTrue(!passenger().canTurnOn && passenger().note != null)
        passenger().set(true)
        assertTrue("not on while moving", !settings.ignoreDriving)
        state.drivingRestricted = false // stopped
        assertTrue(passenger().canTurnOn)
        passenger().set(true)
        assertTrue(settings.ignoreDriving)
        // On while moving (restrictions lifted, so drivingRestricted reads false): it can always be turned off.
        passenger().set(false)
        assertTrue(!settings.ignoreDriving)
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
            var onFix: ((PhoneFix) -> Unit)? = null
            override fun start(onFix: (PhoneFix) -> Unit): Boolean { this.onFix = onFix; return true }
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
        rule.runOnIdle { phone.onFix!!(PhoneFix(15f, 54.68, 25.27, null, 5f, System.currentTimeMillis())) }
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

    /** N20: the map's limit, always labelled; over it by more than the margin the speed changes colour (silently). */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theMapsLimitIsLabelledAndTheMarginDecidesTheColour() {
        val state = PadWalkStates.state(CarLayout.MAP_BEHIND, CarScreen.Dashboard).apply { enginePreview = true }
        val car = MutableStateFlow(CarData.State(connected = true, speed = CarData.Timed(CarData.Speed(60 / 3.6f, null, null), System.currentTimeMillis())))
        val phone = object : PhoneFixes {
            var onFix: ((PhoneFix) -> Unit)? = null
            override fun start(onFix: (PhoneFix) -> Unit): Boolean { this.onFix = onFix; return true }
            override fun stop() { onFix = null }
        }
        val limits = object : SpeedLimits {
            var closed = false
            override fun available() = true
            override fun lookup(fix: PhoneFix, onResult: (LimitShown) -> Unit) = onResult(LimitShown.Known(50))
            override fun close() { closed = true }
        }
        var margin = 9
        var shown by mutableStateOf(true)
        rule.setContent {
            CarTheme(night = state.night, theme = state.theme) {
                if (shown) {
                    val sources = EngineSources.of(state, actions, LocalContext.current)
                        .copy(speed = SpeedSource(car, { phone }, limits = { limits }, margin = { margin }))
                    MyPlayEngineShell(state, actions, appContent = { _, m -> Box(m) }, navMap = { _, m -> Box(m) }, overlay = {}, skin = TestSkin, sources = sources)
                }
            }
        }
        // The car sends its speed; with a limit file the phone's fixes are wanted for the position.
        rule.waitUntil(5_000) { rule.runOnIdle { phone.onFix != null } }
        rule.onNodeWithText("60 km/h").assertExists()
        rule.onNodeWithText("no limit known").assertExists()
        rule.onNodeWithTag(SPEED_OVER_TAG).assertDoesNotExist()
        rule.runOnIdle { phone.onFix!!(PhoneFix(16.7f, 54.68, 25.27, 90f, 5f, System.currentTimeMillis())) }
        rule.onNodeWithText("limit 50 · limit from map data, may be wrong").assertExists()
        rule.onNodeWithTag(SPEED_OVER_TAG).assertExists()
        margin = 15
        rule.mainClock.advanceTimeBy(1_100)
        rule.waitForIdle()
        rule.onNodeWithTag(SPEED_OVER_TAG).assertDoesNotExist()
        rule.runOnIdle { shown = false }
        rule.waitForIdle()
        assertTrue("off the screen: no location, file closed", rule.runOnIdle { phone.onFix == null && limits.closed })
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
