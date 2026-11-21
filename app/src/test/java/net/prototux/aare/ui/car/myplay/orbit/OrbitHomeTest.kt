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
import net.prototux.aare.ui.car.CarPart
import net.prototux.aare.ui.car.CarPartRegistry
import net.prototux.aare.ui.car.LocalCarParts
import androidx.compose.runtime.CompositionLocalProvider
import net.prototux.aare.ui.car.CarScreen
import net.prototux.aare.ui.car.CarShell
import net.prototux.aare.ui.car.CarUiState
import net.prototux.aare.ui.car.NowPlaying
import net.prototux.aare.ui.car.PadWalkStates
import net.prototux.aare.ui.car.myplay.ENGINE_TAG
import androidx.compose.ui.test.performClick
import net.prototux.aare.media.LibraryRow
import net.prototux.aare.media.MediaLibrary
import net.prototux.aare.media.MediaTransport
import net.prototux.aare.media.UpNextItem
import net.prototux.aare.ui.car.myplay.SLOT_APPS
import net.prototux.aare.ui.car.myplay.SLOT_CHOOSER
import net.prototux.aare.ui.car.myplay.appTag
import net.prototux.aare.ui.car.CarAppKind
import net.prototux.aare.ui.car.myplay.STOP_HUB
import net.prototux.aare.ui.car.myplay.STOP_SONG
import net.prototux.aare.ui.car.myplay.STOP_TURN
import net.prototux.aare.ui.car.myplay.TripProgress
import net.prototux.aare.ui.car.myplay.TurnDirection
import net.prototux.aare.ui.theme.CarTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Battery0Bar
import androidx.compose.material.icons.rounded.Battery3Bar
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasTestTag
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

    /** A library for the pictures: a few folders and songs, the first with artwork. */
    private object PictureLibrary : MediaLibrary {
        override val title = "Recently played"
        override val key = "root"
        override val rows = listOf(
            LibraryRow("a", "Night Drive", "Blue Harbor", browsable = false, playable = true),
            LibraryRow("b", "Road Trip Mix", "Playlist · 42 songs", browsable = true, playable = false),
            LibraryRow("c", "Glass Roads", "Kite & Co.", browsable = false, playable = true),
            LibraryRow("d", "Podcasts", "", browsable = true, playable = false),
            LibraryRow("e", "Slow Harbour", "Lumen", browsable = false, playable = true),
        )
        override val loading = false
        override val end = true
        override val error: String? = null
        override val canGoBack = false
        override fun open(row: LibraryRow) = false
        override fun back() = Unit
        override fun loadMore() = Unit
        override suspend fun art(row: LibraryRow, sizePx: Int): Bitmap? =
            if (row.id == "a") Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFFFF8A3D.toInt()) } else null
    }
    private var state: CarUiState? = null
    private val actions = object : CarActions by PadWalkStates.NoActions {
        override fun openLauncher() { asked += "launcher" }
        override fun openApp(app: CarAppEntry) { asked += "app:${app.id}" }
        override fun mediaLibrary(): MediaLibrary = PictureLibrary
    }

    private fun estimate(meters: Double, seconds: Long) = TravelEstimate.Builder(
        Distance.create(meters, Distance.UNIT_METERS),
        DateTimeWithZone.create(1_700_000_000_000L + seconds * 1000, TimeZone.getTimeZone("UTC")),
    ).setRemainingTimeSeconds(seconds).build()

    private fun route(destination: String = "Home", left: Double = 9_400.0): Trip {
        val step = Step.Builder("Turn right").setRoad("Hauptstraße")
            .setManeuver(Maneuver.Builder(Maneuver.TYPE_TURN_NORMAL_RIGHT).build()).build()
        return Trip.Builder()
            .addStep(step, estimate(350.0, 40))
            .addDestination(Destination.Builder().setName(destination).build(), estimate(left, 1_080))
            .build()
    }

    /** The fixture's song, its position told now, so the ring shows the real look. */
    private fun song(art: Bitmap?) = NowPlaying("music", PadWalkStates.SONG_TITLE, "The Synthwaves", art, playing = true,
        positionMs = 95_000, durationMs = 240_000, updatedAtMs = android.os.SystemClock.elapsedRealtime())

    /** Artwork a player could send: a two-colour square. */
    private fun art() = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply {
        for (x in 0 until 64) for (y in 0 until 64) setPixel(x, y, if (x < y) 0xFFFF8A3D.toInt() else 0xFF3D7BFF.toInt())
    }

    private fun show(night: Boolean = true, trip: Boolean = true, withArt: Boolean = true): CarUiState {
        val state = PadWalkStates.state(CarLayout.MAP_BEHIND, CarScreen.Dashboard).apply {
            enginePreview = true
            this.night = night
            if (trip) this.trip = route()
            // N5-2: the position as a player tells it now, so the ring shows the real look.
            nowPlaying = song(if (withArt) art() else null)
        }
        this.state = state
        rule.setContent {
            CarTheme(night = state.night, theme = state.theme) {
                CarShell(state, actions, appContent = { _, m -> Box(m.background(Color.DarkGray)) },
                    navMap = { _, m -> Box(m.background(if (night) Color(0xFF0B1318) else Color(0xFFDCE3E8))) })
            }
        }
        rule.waitForIdle()
        return state
    }

    /** For the phone's picture (N11): ORBIT's parts are tagged so a tap can find them. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun orbitsPartsAreTaggedForThePhonesPicture() {
        val registry = CarPartRegistry()
        val state = PadWalkStates.state(CarLayout.MAP_BEHIND, CarScreen.Dashboard).apply {
            enginePreview = true
            trip = route()
        }
        rule.setContent {
            CarTheme(night = state.night, theme = state.theme) {
                CompositionLocalProvider(LocalCarParts provides registry) {
                    CarShell(state, actions, appContent = { _, m -> Box(m) }, navMap = { _, m -> Box(m) })
                }
            }
        }
        rule.waitForIdle()
        val parts = rule.runOnIdle { registry.regions.map { it.first }.toSet() }
        for (p in listOf(CarPart.OrbitTurn, CarPart.OrbitHub, CarPart.OrbitSong, CarPart.OrbitSpeed, CarPart.Clock)) {
            assertTrue("$p in $parts", p in parts)
        }
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
        // N6: the Apps world, the first app ringed; a push opens it.
        rule.onNodeWithTag(SLOT_APPS).assertExists()
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(listOf("app:maps"), asked)
    }

    /** N6: more apps than a page holds: the knob goes on to the next page, the pad off the right edge too. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theHoneycombPages() {
        val state = show()
        rule.runOnIdle { addApps(state) }
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        repeat(3) { engine.performKeyInput { pressKey(Key.Tab) } }
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        val apps = state.launcherApps
        assertEquals(14, apps.size)
        rule.onNodeWithTag(appTag(apps[0])).assertIsSelected()
        assertTrue(rule.onAllNodes(hasTestTag(appTag(apps[10]))).fetchSemanticsNodes().isEmpty())
        repeat(10) { engine.performKeyInput { pressKey(Key.Tab) } }
        rule.onNodeWithTag(appTag(apps[10])).assertIsSelected()
        assertTrue("page 1 is gone", rule.onAllNodes(hasTestTag(appTag(apps[0]))).fetchSemanticsNodes().isEmpty())
        // Back a page with the pad off the left edge.
        engine.performKeyInput { pressKey(Key.DirectionLeft) }
        rule.onNodeWithTag(appTag(apps[9])).assertIsSelected()
    }

    /** Six more apps (14 in all, two pages); every app gets a plain coloured icon so the pictures show where icons go. */
    private fun addApps(state: CarUiState) {
        val colours = listOf(0xFF4CC9F0, 0xFFFFC24A, 0xFF5FD39A, 0xFFFF8FB1, 0xFFA78BFA, 0xFF9DB4E8).map { it.toInt() }
        fun icon(i: Int) = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888).apply { eraseColor(colours[i % colours.size]) }
        val old = state.apps.toList()
        state.apps.clear()
        old.forEachIndexed { i, a -> state.apps += CarAppEntry(a.id, a.packageName, a.label, icon(i), a.kind) }
        for (i in 1..6) state.apps += CarAppEntry("extra$i", "mock.extra$i", "Extra $i", icon(i + 1), CarAppKind.TEMPLATED)
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theSongShowsItsArtOrTheFallback() {
        show(withArt = true)
        rule.onNodeWithTag(OrbitSkin.SONG_ART_TAG, useUnmergedTree = true).assertExists()
        rule.runOnIdle { state!!.nowPlaying = song(null) }
        rule.waitForIdle()
        rule.onNodeWithTag(OrbitSkin.SONG_ART_FALLBACK_TAG, useUnmergedTree = true).assertExists()
    }

    /** The song ring runs on the player's clock (elapsedRealtime): 95 s of 240 s is about 0.4, not full. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theSongRingShowsThePosition() {
        show()
        val p = rule.onNodeWithTag(OrbitSkin.SONG_RING_TAG, useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsProperties.ProgressBarRangeInfo].current
        assertTrue("progress $p", p in 0.39f..0.5f)
    }

    /** N5-3: Robolectric's phone tells no battery level, so none is shown (never "0%"). */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun anUnknownBatteryIsNotShown() {
        show()
        rule.waitForIdle()
        assertTrue(rule.onAllNodes(hasTestTag(OrbitSkin.BATTERY_TAG), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        assertTrue(rule.onAllNodesWithText("0%").fetchSemanticsNodes().isEmpty())
    }

    @Test fun batteryIconsAndLow() {
        assertTrue(OrbitSkin.batteryLow(20, charging = false))
        assertFalse(OrbitSkin.batteryLow(20, charging = true))
        assertFalse(OrbitSkin.batteryLow(21, charging = false))
        assertEquals(Icons.Rounded.BatteryFull, OrbitSkin.batteryIcon(100, false))
        assertEquals(Icons.Rounded.Battery0Bar, OrbitSkin.batteryIcon(5, false))
        assertEquals(Icons.Rounded.Battery3Bar, OrbitSkin.batteryIcon(50, false))
        assertEquals(Icons.Rounded.BatteryChargingFull, OrbitSkin.batteryIcon(5, true))
    }

    /** N5-4: a new destination is a new route, even when navigation never ended. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theTripRingStartsAgainForANewDestination() {
        show()
        fun progress() = rule.onNodeWithTag(OrbitSkin.TRIP_RING_TAG, useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsProperties.ProgressBarRangeInfo].current
        assertEquals(0f, progress(), 0.01f)
        rule.runOnIdle { state!!.trip = route(left = 4_700.0) }
        rule.waitForIdle()
        assertEquals(0.5f, progress(), 0.01f)
        rule.runOnIdle { state!!.trip = route(destination = "Work", left = 3_000.0) }
        rule.waitForIdle()
        assertEquals(0f, progress(), 0.01f)
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun home1280x720Night() = picture("orbit-home-1280x720-night", night = true)
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun home1280x720Day() = picture("orbit-home-1280x720-day", night = false)
    @Test @Config(qualifiers = "w1200dp-h723dp-land-mdpi") fun home1200x723Night() = picture("orbit-home-1200x723-night", night = true)
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun home901x543Night() = picture("orbit-home-901x543-night", night = true)
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun homeNoRoute() = picture("orbit-home-no-route-night", night = true, trip = false)
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun homeNoArtDay() = picture("orbit-home-no-art-day", night = false, withArt = false)
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun dialNight() = picture("orbit-dial-1280x720-night", night = true, dial = true)
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun dial901x543Day() = picture("orbit-dial-901x543-day", night = false, dial = true)
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun musicNight() = picture("orbit-music-1280x720-night", night = true, music = true)
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun music901x543Day() = picture("orbit-music-901x543-day", night = false, music = true)
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun libraryNight() = picture("orbit-library-1280x720-night", night = true, music = true, library = true)
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun library901x543Day() = picture("orbit-library-901x543-day", night = false, music = true, library = true)
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun appsNight() = picture("orbit-apps-1280x720-night", night = true, apps = true)
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun apps901x543Day() = picture("orbit-apps-901x543-day", night = false, apps = true)

    private fun picture(name: String, night: Boolean, trip: Boolean = true, dial: Boolean = false, withArt: Boolean = true, apps: Boolean = false, music: Boolean = false, library: Boolean = false) {
        val state = show(night, trip, withArt)
        if (music) {
            rule.runOnIdle {
                state.transport = MediaTransport(canSeek = true, canPrevious = true, canNext = true,
                    upNext = listOf(UpNextItem(7, "Midnight Run", "Blue Harbor"), UpNextItem(8, "Glass Roads", "Kite & Co."), UpNextItem(9, "Slow Harbour", "Lumen")))
            }
            val engine = rule.onNodeWithTag(ENGINE_TAG)
            engine.performKeyInput { pressKey(Key.DirectionCenter) }
            engine.performKeyInput { pressKey(Key.Tab) }
            engine.performKeyInput { pressKey(Key.DirectionCenter) }
            if (library) {
                rule.onNodeWithTag(net.prototux.aare.ui.car.myplay.MUSIC_LIBRARY).performClick()
                engine.performKeyInput { pressKey(Key.DirectionDown) }
            }
            rule.mainClock.advanceTimeBy(1_000)
        }
        if (apps) {
            rule.runOnIdle { addApps(state) }
            val engine = rule.onNodeWithTag(ENGINE_TAG)
            engine.performKeyInput { pressKey(Key.DirectionCenter) }
            repeat(3) { engine.performKeyInput { pressKey(Key.Tab) } }
            engine.performKeyInput { pressKey(Key.DirectionCenter) }
            engine.performKeyInput { pressKey(Key.Tab) }
            rule.mainClock.advanceTimeBy(1_000)
        }
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
