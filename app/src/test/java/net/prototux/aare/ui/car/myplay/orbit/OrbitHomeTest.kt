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
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
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
import net.prototux.aare.ui.car.CarCall
import org.robolectric.Shadows
import net.prototux.aare.ui.car.PadWalkStates
import net.prototux.aare.ui.car.myplay.ENGINE_TAG
import androidx.compose.ui.test.performClick
import net.prototux.aare.media.LibraryRow
import net.prototux.aare.media.MediaLibrary
import net.prototux.aare.media.MediaTransport
import net.prototux.aare.media.UpNextItem
import androidx.compose.runtime.remember
import net.prototux.aare.ui.car.myplay.EngineSources
import net.prototux.aare.ui.car.myplay.LocalEngineSources
import net.prototux.aare.ui.car.myplay.PhoneEntry
import net.prototux.aare.ui.car.myplay.PhoneSource
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.testTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.TimeZone
import net.prototux.aare.car.CarData
import net.prototux.aare.car.LimitShown
import net.prototux.aare.car.PhoneFix
import net.prototux.aare.car.PhoneFixes
import net.prototux.aare.car.SpeedReading
import net.prototux.aare.ui.car.myplay.SpeedSource

/** MyPlay (N5): ORBIT's home through the car shell's hidden preview, its pictures and its knob walk. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class OrbitHomeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val asked = mutableListOf<String>()
    private var libraryShown: MediaLibrary = PictureLibrary

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
        override fun mediaLibrary(): MediaLibrary = libraryShown
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

    /** A phone for the pictures. */
    private object PicturePhone : PhoneSource {
        override val permitted = true
        override suspend fun recents() = listOf(
            PhoneEntry("Anna K.", "111", "2 min ago", missed = true), PhoneEntry("Ben Ford", "222", "Today, 08:14"),
            PhoneEntry("", "+370 612 34567", "Yesterday"),
        )
        override suspend fun favourites() = listOf(PhoneEntry("Carla", "333", "Mobile · 333"))
        override fun call(entry: PhoneEntry) = Unit
        override fun open() = Unit
    }

    private fun show(night: Boolean = true, trip: Boolean = true, withArt: Boolean = true, speed: SpeedReading? = null): CarUiState {
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
                val context = androidx.compose.ui.platform.LocalContext.current
                CompositionLocalProvider(LocalEngineSources provides remember(state) {
                    EngineSources.of(state, actions, context).copy(phone = PicturePhone, carLines = {
                        listOf("Car" to "Audi A4 2016", "Driving" to "Moving", "Speed" to "48 km/h", "Fuel" to "62% · 480 km",
                            "Outside" to "7.5 °C · 101 kPa", "Tyres" to "230 · 230 · 240 · 240 kPa", "Doors" to "Closed")
                    }).let { s ->
                        if (speed == null) s else s.copy(speed = SpeedSource(kotlinx.coroutines.flow.MutableStateFlow(CarData.State()), { NoPhoneFixes }, speed))
                    }
                }) {
                    CarShell(state, actions, appContent = { _, m -> Box(m.background(Color.DarkGray)) },
                        navMap = { _, m -> covers = net.prototux.aare.host.templates.LocalMapCovers.current; Box(m.background(if (night) Color(0xFF0B1318) else Color(0xFFDCE3E8))) })
                }
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
        rule.onNodeWithText("18 min", substring = true).assertExists()
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
        // HN-3: the dial holds four worlds round Apps in the middle, and opens on Apps.
        assertEquals(1, rule.onAllNodesWithText("Apps").fetchSemanticsNodes().size)
        for (w in listOf("Navigate", "Calls", "Settings", "Music")) assertEquals(w, 1, rule.onAllNodesWithText(w).fetchSemanticsNodes().size)
        // HN-3: pushing again opens Apps.
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

    /** DW-1: the ice sign's place is there when it is warm too, empty, so the free area never moves because of it. */
    @Test
    fun theIceSlotIsKeptEmptyWhenWarm() {
        show()
        rule.waitForIdle()
        assertFalse(rule.onAllNodes(hasTestTag(OrbitSkin.ICE_SLOT_TAG), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        assertTrue(rule.onAllNodes(hasTestTag(OrbitSkin.ICE_TAG), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
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

    /**
     * O7, HN-3: the dial's four worlds, one press of a pad direction each, and Apps a second push;
     * Back from a world opened from the dial is the dial on it. (Up is the maps app itself.)
     */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theDialsDirectionsOpenTheirWorlds() {
        show()
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        fun back() { rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle() }
        for ((k, slot) in listOf(Key.DirectionRight to net.prototux.aare.ui.car.myplay.SLOT_PHONE, Key.DirectionDown to net.prototux.aare.ui.car.myplay.SLOT_SETTINGS,
            Key.DirectionLeft to net.prototux.aare.ui.car.myplay.SLOT_MUSIC, Key.DirectionCenter to net.prototux.aare.ui.car.myplay.SLOT_APPS)) {
            engine.performKeyInput { pressKey(Key.DirectionCenter) }
            rule.onNodeWithTag(SLOT_CHOOSER).assertExists()
            engine.performKeyInput { pressKey(k) }
            rule.onNodeWithTag(slot).assertExists()
            back()
            rule.onNodeWithTag(SLOT_CHOOSER).assertExists()
            back()
            rule.onNodeWithTag(SLOT_CHOOSER).assertDoesNotExist()
        }
        // No line under the dial any more.
        engine.performKeyInput { pressKey(Key.DirectionCenter) }
        assertTrue(rule.onAllNodesWithText("push: open", substring = true).fetchSemanticsNodes().isEmpty())
    }

    @Test fun theDialHoldsFourWorldsClockwiseFromTheTop() {
        assertEquals(listOf(net.prototux.aare.ui.car.myplay.World.NAVIGATE, net.prototux.aare.ui.car.myplay.World.CALLS, net.prototux.aare.ui.car.myplay.World.SETTINGS, net.prototux.aare.ui.car.myplay.World.MUSIC), OrbitSkin.dial)
        assertEquals(net.prototux.aare.ui.car.myplay.World.APPS, OrbitSkin.dialCentre)
        // A world opened from the home pad (even one on the dial) goes straight home on Back.
        val m = net.prototux.aare.ui.car.myplay.MyPlayScreenModel(dial = OrbitSkin.dial, centre = OrbitSkin.dialCentre)
        m.enter(net.prototux.aare.ui.car.myplay.World.MUSIC)
        m.back()
        assertEquals(net.prototux.aare.ui.car.myplay.Place.Home, m.place)
        // HN-3: the dial always opens on its middle (Apps), even after another world.
        m.choose(net.prototux.aare.ui.car.myplay.World.CALLS)
        m.back()
        assertEquals(net.prototux.aare.ui.car.myplay.Place.Chooser(net.prototux.aare.ui.car.myplay.World.CALLS), m.place)
        m.back()
        m.push()
        assertEquals(net.prototux.aare.ui.car.myplay.Place.Chooser(net.prototux.aare.ui.car.myplay.World.APPS), m.place)
        // A knob click from the middle: forward the first world (up), back the last (left).
        m.turn(1)
        assertEquals(net.prototux.aare.ui.car.myplay.Place.Chooser(net.prototux.aare.ui.car.myplay.World.NAVIGATE), m.place)
        m.turn(-1)
        assertEquals(net.prototux.aare.ui.car.myplay.Place.Chooser(net.prototux.aare.ui.car.myplay.World.MUSIC), m.place)
        m.back(); m.push(); m.turn(-1)
        assertEquals(net.prototux.aare.ui.car.myplay.Place.Chooser(net.prototux.aare.ui.car.myplay.World.MUSIC), m.place)
        m.push()
        assertEquals(net.prototux.aare.ui.car.myplay.Place.InWorld(net.prototux.aare.ui.car.myplay.World.MUSIC), m.place)
    }

    /** SH-1: plates hug the parts (the clock's too); "none" draws no shade at all. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theShadesHugThePartsOrAreNone() {
        val state = show()
        rule.onNodeWithTag(OrbitSkin.SHADE_STATUS_TAG).assertExists()
        rule.onNodeWithTag(OrbitSkin.SHADE_TURN_TAG).assertExists()
        rule.runOnIdle { state.orbitShadeSoft = false }
        rule.waitForIdle()
        for (t in listOf(OrbitSkin.SHADE_STATUS_TAG, OrbitSkin.SHADE_TURN_TAG, OrbitSkin.SHADE_SONG_TAG)) rule.onNodeWithTag(t).assertDoesNotExist()
    }

    /** O8: the speed is in the hub with the trip, and the limit is a sign with its number; no separate speed part. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theSpeedAndTheLimitNumberAreInTheHub() {
        show(speed = SpeedReading(64, SpeedReading.From.CAR, LimitShown.Known(50), over = true))
        rule.onNodeWithTag(OrbitSkin.HUB_SPEED_TAG, useUnmergedTree = true).assertExists()
        rule.onNodeWithTag(OrbitSkin.LIMIT_SIGN_TAG, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("50", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("64", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag(net.prototux.aare.ui.car.myplay.SLOT_SPEED).assertDoesNotExist()
        // The sign sits on the hub's edge.
        val hub = rule.onNodeWithTag(net.prototux.aare.ui.car.myplay.STOP_HUB).fetchSemanticsNode().boundsInRoot
        val sign = rule.onNodeWithTag(OrbitSkin.LIMIT_SIGN_TAG, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("sign $sign on the hub $hub", sign.overlaps(hub))
    }

    /** O4: the corners' shades only behind the parts shown: none without a route, none without a song. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theShadesFollowTheParts() {
        val state = show()
        rule.onNodeWithTag(OrbitSkin.SHADE_TURN_TAG).assertExists()
        rule.onNodeWithTag(OrbitSkin.SHADE_SONG_TAG).assertExists()
        rule.runOnIdle { state.nowPlaying = null }
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithTag(OrbitSkin.SHADE_SONG_TAG).assertDoesNotExist()
        rule.onNodeWithTag(OrbitSkin.SHADE_TURN_TAG).assertExists()
        rule.runOnIdle { state.trip = null; state.phoneGuidance = null }
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithTag(OrbitSkin.SHADE_TURN_TAG).assertDoesNotExist()
        rule.onNodeWithTag(net.prototux.aare.ui.car.myplay.STOP_TURN).assertDoesNotExist()
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
    // N20: the map's limit under the speed, always labelled; over it, the speed turns amber.
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun speedOverTheLimitNight() =
        picture("orbit-speed-limit-over-1280x720-night", night = true, speed = SpeedReading(64, SpeedReading.From.CAR, LimitShown.Known(50), over = true))
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun speedWithinTheLimit901x543Day() =
        picture("orbit-speed-limit-901x543-day", night = false, speed = SpeedReading(48, SpeedReading.From.CAR, LimitShown.Known(50)))
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun speedOverTheLimitDay() =
        picture("orbit-speed-limit-over-1280x720-day", night = false, speed = SpeedReading(64, SpeedReading.From.CAR, LimitShown.Known(50), over = true))
    @Test @Config(qualifiers = "w1200dp-h723dp-land-mdpi") fun speedNoLimitKnown() =
        picture("orbit-speed-limit-unknown-1200x723-night", night = true, speed = SpeedReading(48, SpeedReading.From.CAR, LimitShown.Unknown))

    private object NoPhoneFixes : PhoneFixes {
        override fun start(onFix: (PhoneFix) -> Unit) = false
        override fun stop() = Unit
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun homeNoRoute() = picture("orbit-home-no-route-night", night = true, trip = false)
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun homeNotNavigatingNothingPlaying() {
        picture("orbit-home-idle-1280x720-day", night = false, trip = false)
        rule.runOnIdle { state!!.nowPlaying = null }
        rule.mainClock.advanceTimeBy(1_000)
        picture2("orbit-home-idle-nothing-playing-1280x720-day")
    }
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun homeNoArtDay() = picture("orbit-home-no-art-day", night = false, withArt = false)
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun dialNight() = picture("orbit-dial-1280x720-night", night = true, dial = true)
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun dial901x543Day() = picture("orbit-dial-901x543-day", night = false, dial = true)
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun musicNight() = picture("orbit-music-1280x720-night", night = true, music = true)
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun music901x543Day() = picture("orbit-music-901x543-day", night = false, music = true)
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun musicPanelNight() = picture("orbit-music-panel-1280x720-night", night = true, panel = true)
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun musicPanelDay() = picture("orbit-music-panel-1280x720-day", night = false, panel = true)
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun musicPanel901x543Night() = picture("orbit-music-panel-901x543-night", night = true, panel = true)
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun libraryNight() = picture("orbit-library-1280x720-night", night = true, music = true, library = true)
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun library901x543Day() = picture("orbit-library-901x543-day", night = false, music = true, library = true)
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun ringingNight() = picture("orbit-call-ringing-1280x720-night", night = true, call = CarCall(true, "Anna K.", 1L))
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun ringing901x543Day() = picture("orbit-call-ringing-901x543-day", night = false, call = CarCall(true, "+370 612 34567", 1L))
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun inCallNight() = picture("orbit-call-in-call-1280x720-night", night = true, call = CarCall(false, "Anna K.", System.currentTimeMillis() - 133_000))

    /** L-1: a level that lists the same id twice (a song twice in a playlist) draws, rather than crash. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun aLibraryWithARepeatedIdDraws() {
        libraryShown = object : MediaLibrary by PictureLibrary {
            // The repeat is second, so both copies are on screen together.
            override val rows = listOf(PictureLibrary.rows.first()) + PictureLibrary.rows
        }
        picture("orbit-library-repeated-id", night = true, music = true, library = true)
        rule.onNodeWithTag(net.prototux.aare.ui.car.myplay.libraryRowTag(1), useUnmergedTree = true).assertExists()
    }

    @Test fun initialsAreLettersOrNone() {
        assertEquals("AK", OrbitSkin.initials("Anna K."))
        assertEquals("AB", OrbitSkin.initials("anna   bell cole"))
        assertNull(OrbitSkin.initials("+370 612 34567"))
        assertNull(OrbitSkin.initials(""))
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun phoneNight() = picture("orbit-phone-1280x720-night", night = true, calls = true)
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun phone901x543Day() = picture("orbit-phone-901x543-day", night = false, calls = true)
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun noticeNight() = picture("orbit-notice-1280x720-night", night = true, notice = true)
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun notice901x543Day() = picture("orbit-notice-901x543-day", night = false, notice = true)
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun bellNight() = picture("orbit-bell-1280x720-night", night = true, bell = 1)
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun noticeListNight() = picture("orbit-notice-list-1280x720-night", night = true, bell = 2)
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun noticeList901x543Day() = picture("orbit-notice-list-901x543-day", night = false, bell = 2)
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun settingsNight() = picture("orbit-settings-1280x720-night", night = true, world = 3)
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun settings901x543Day() = picture("orbit-settings-901x543-day", night = false, world = 3)
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun appsNight() = picture("orbit-apps-1280x720-night", night = true, apps = true)
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun apps901x543Day() = picture("orbit-apps-901x543-day", night = false, apps = true)

    private fun picture(name: String, night: Boolean, trip: Boolean = true, dial: Boolean = false, withArt: Boolean = true, apps: Boolean = false, music: Boolean = false, library: Boolean = false, call: CarCall? = null, calls: Boolean = false, notice: Boolean = false, bell: Int = 0, world: Int = -1, speed: SpeedReading? = null, panel: Boolean = false) {
        val state = show(night, trip, withArt, speed)
        if (world >= 0) {
            val engine = rule.onNodeWithTag(ENGINE_TAG)
            engine.performKeyInput { pressKey(Key.DirectionCenter) }
            // HN-3: [world] turns round the dial from Apps in the middle (2 Calls, 3 Settings), then a push.
            repeat(world) { engine.performKeyInput { pressKey(Key.Tab) } }
            engine.performKeyInput { pressKey(Key.DirectionCenter) }
            engine.performKeyInput { pressKey(Key.DirectionDown) }
            rule.mainClock.advanceTimeBy(1_000)
        }
        if (bell > 0) {
            rule.runOnIdle {
                state.history.add(net.prototux.aare.ui.car.CarNotice("h1", "Anna K.", "Running late, see you at 6", null, listOf("Reply" to {}), System.currentTimeMillis() - 120_000))
                state.history.add(net.prototux.aare.ui.car.CarNotice("h2", "Calendar", "Dentist at 15:00", null, timeMs = System.currentTimeMillis() - 3_600_000))
                state.history.add(net.prototux.aare.ui.car.CarNotice("h3", "Ben Ford", "Photo", null, timeMs = System.currentTimeMillis() - 7_200_000))
                state.unseen = 3
            }
            rule.waitForIdle()
            if (bell > 1) rule.onNodeWithTag(net.prototux.aare.ui.car.myplay.STOP_BELL).performClick()
            rule.mainClock.advanceTimeBy(800)
        }
        if (notice) {
            rule.runOnIdle {
                state.notices.add(0, net.prototux.aare.ui.car.CarNotice("n1", "Anna K.", "Running late, see you at 6", null, listOf("Reply" to {}, "Mark read" to {}), message = true))
            }
            val engine = rule.onNodeWithTag(ENGINE_TAG)
            engine.performKeyInput { pressKey(Key.Tab) }
            engine.performKeyInput { pressKey(Key.Tab) }
            engine.performKeyInput { pressKey(Key.Tab) }
            rule.mainClock.advanceTimeBy(500)
        }
        if (calls) {
            val engine = rule.onNodeWithTag(ENGINE_TAG)
            engine.performKeyInput { pressKey(Key.DirectionCenter) }
            engine.performKeyInput { pressKey(Key.DirectionUp) }
            engine.performKeyInput { pressKey(Key.DirectionDown) }
            rule.mainClock.advanceTimeBy(1_000)
        }
        if (call != null) {
            Shadows.shadowOf(rule.activity.application).grantPermissions(android.Manifest.permission.ANSWER_PHONE_CALLS)
            rule.runOnIdle { state.call = call }
            rule.mainClock.advanceTimeBy(1_500)
        }
        if (music) {
            rule.runOnIdle {
                state.transport = MediaTransport(canSeek = true, canPrevious = true, canNext = true,
                    upNext = listOf(UpNextItem(7, "Midnight Run", "Blue Harbor"), UpNextItem(8, "Glass Roads", "Kite & Co."), UpNextItem(9, "Slow Harbour", "Lumen")))
            }
            val engine = rule.onNodeWithTag(ENGINE_TAG)
            // O7: Music is not on the dial; the car runtime's way in (as the home pad's would be).
            rule.runOnIdle { state.engineOpenWorld = net.prototux.aare.ui.car.myplay.World.MUSIC.id }
            rule.waitForIdle()
            if (library) {
                rule.onNodeWithTag(net.prototux.aare.ui.car.myplay.MUSIC_LIBRARY).performClick()
                engine.performKeyInput { pressKey(Key.DirectionDown) }
            }
            rule.mainClock.advanceTimeBy(1_000)
        }
        if (panel) {
            // O1: the song's push opens the music panel; the ring on play/pause.
            rule.runOnIdle { state.transport = MediaTransport(canSeek = true, canPrevious = true, canNext = true) }
            val engine = rule.onNodeWithTag(ENGINE_TAG)
            engine.performKeyInput { pressKey(Key.Tab) }
            engine.performKeyInput { pressKey(Key.DirectionCenter) }
            rule.onNodeWithTag(net.prototux.aare.ui.car.myplay.SLOT_MUSIC_PANEL).assertExists()
            rule.mainClock.advanceTimeBy(500)
        }
        if (apps) {
            rule.runOnIdle { addApps(state) }
            val engine = rule.onNodeWithTag(ENGINE_TAG)
            engine.performKeyInput { pressKey(Key.DirectionCenter) }
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

    /** What the engine tells the map covers it (MC-1). */
    private var covers: net.prototux.aare.host.templates.MapCoverRegistry? = null

    /** MC-1: the free area the maps app would be told, from what ORBIT marks, on the whole screen. */
    private fun freeArea(): net.prototux.aare.host.templates.MapGeometry.Box {
        val c = rule.runOnIdle { covers }!!
        org.junit.Assert.assertTrue("ORBIT centres the free area", c.centred)
        val s = rule.onNodeWithTag(ENGINE_TAG).fetchSemanticsNode().boundsInRoot
        fun boxes(m: Map<Any, androidx.compose.ui.geometry.Rect>) = m.values.map { net.prototux.aare.host.templates.MapGeometry.Box(it.left, it.top, it.right, it.bottom) }
        return net.prototux.aare.host.templates.MapAreas.compute(net.prototux.aare.host.templates.MapGeometry.Box(s.left, s.top, s.right, s.bottom),
            boxes(c.shown), boxes(c.reserved), centred = true, minGutter = 16f).visible
    }

    /** MC-1: with the turn, the hub's speed and limit, the clock and the song, Waze's area is centred and nearly full height. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theMapsFreeAreaIsCentred() {
        val state = show(speed = SpeedReading(64, SpeedReading.From.CAR, LimitShown.Known(50), over = true))
        rule.runOnIdle { state.phoneGuidance = net.prototux.aare.notify.PhoneGuidance("k", "mock.maps", "Maps", "300 m", "Main Street", "", null, null, "") }
        rule.waitForIdle()
        val free = freeArea()
        org.junit.Assert.assertEquals(640f, (free.left + free.right) / 2, 0.5f)
        org.junit.Assert.assertEquals(360f, (free.top + free.bottom) / 2, 0.5f)
        org.junit.Assert.assertEquals("left $free", 288f, free.left, 1f)
        org.junit.Assert.assertEquals("top $free", 43.2f, free.top, 1f)
        // Clear of the discs, the hub and the clock; the song's title may lie over it.
        for (tag in listOf(STOP_HUB, OrbitSkin.SONG_RING_TAG, net.prototux.aare.ui.car.myplay.SLOT_STATUS)) {
            val b = rule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            org.junit.Assert.assertNull("$tag $b", net.prototux.aare.host.templates.MapGeometry.Box(b.left, b.top, b.right, b.bottom).intersect(free))
        }
        // The picture with the area outlined, for a look.
        val bmp = rule.onNodeWithTag(ENGINE_TAG).captureToImage().asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, true)
        android.graphics.Canvas(bmp).drawRect(free.left, free.top, free.right, free.bottom,
            android.graphics.Paint().apply { style = android.graphics.Paint.Style.STROKE; strokeWidth = 4f; color = android.graphics.Color.YELLOW })
        val dir = File("build/reports/myplay-screens").apply { mkdirs() }
        File(dir, "orbit_free_area_centred.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /**
     * AR-1 (the owner: the album art sometimes an oval): wide (16:9) and tall (9:16) art, with a
     * title long enough to fill the corner, is still a true circle: the ring and the art square,
     * the art cropped, never stretched. On the narrower 1200 wide screen too.
     */
    @Test @Config(qualifiers = "w1200dp-h720dp-land-mdpi")
    fun theArtIsAlwaysACircle() {
        val state = show()
        val long = "A Very Long Song Title That Goes On And On Past The Corner (Extended Remix)"
        for ((w, h) in listOf(1600 to 900, 900 to 1600, 640 to 640)) {
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFF3D7BFF.toInt()) }
            rule.runOnIdle { state.nowPlaying = NowPlaying("music", long, "The Synthwaves", bmp, playing = true, positionMs = 95_000, durationMs = 240_000, updatedAtMs = android.os.SystemClock.elapsedRealtime()) }
            rule.waitForIdle()
            for (tag in listOf(OrbitSkin.SONG_ART_TAG, OrbitSkin.SONG_RING_TAG)) {
                val b = rule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                org.junit.Assert.assertEquals("$tag ${w}x$h $b", b.width, b.height, 0.5f)
            }
            val ring = rule.onNodeWithTag(OrbitSkin.SONG_RING_TAG, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            // Its full size, not a squeezed one (152 dp at ORBIT's scale for this screen).
            org.junit.Assert.assertEquals(152f * OrbitSkin.scale(1200, 720), ring.width, 1f)
        }
        // The turn's disc likewise beside a long road name.
        rule.runOnIdle { state.phoneGuidance = net.prototux.aare.notify.PhoneGuidance("k", "mock.maps", "Maps", "300 m", long, "", null, null, "") }
        rule.waitForIdle()
        val turn = rule.onNodeWithTag(STOP_TURN).fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue("turn $turn", turn.height >= 132f * OrbitSkin.scale(1200, 720) - 1f)
    }

    /**
     * HN-2: the hints round the hub (the owner's A, C, E, F): a touch wakes them; the pad's
     * directions that open something show as icons, left of the map's free area; a remap redraws
     * them; they fade after the panel time. HN-3: no hints for taught buttons.
     */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theHintsShowWhatTheControlsDo() {
        val state = show(speed = SpeedReading(48, SpeedReading.From.CAR, LimitShown.Known(50), over = false))
        rule.runOnIdle {
            state.homePad = net.prototux.aare.ui.car.myplay.HomePad.DEFAULT + (net.prototux.aare.ui.car.myplay.HomePadKey.DOWN to net.prototux.aare.ui.car.myplay.HomePadAction.NOTHING)
        }
        rule.waitForIdle()
        rule.onNodeWithTag(OrbitHints.TAG_UP).assertDoesNotExist()
        rule.onNodeWithTag(ENGINE_TAG).performTouchInput { click(center) }
        rule.waitForIdle()
        for (tag in listOf(OrbitHints.TAG_UP, OrbitHints.TAG_LEFT, OrbitHints.TAG_RIGHT)) {
            val b = rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            org.junit.Assert.assertTrue("$tag $b left of the free area", b.right <= 288f)
            org.junit.Assert.assertTrue("$tag $b on screen", b.left >= 0f && b.bottom <= 720f)
        }
        // Down opens nothing: no hint.
        rule.onNodeWithTag(OrbitHints.TAG_DOWN).assertDoesNotExist()
        val bmp = rule.onNodeWithTag(ENGINE_TAG).captureToImage().asAndroidBitmap()
        File(File("build/reports/myplay-screens").apply { mkdirs() }, "orbit_hints.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        // F: the owner's remap shows at once.
        rule.runOnIdle { state.homePad = net.prototux.aare.ui.car.myplay.HomePad.DEFAULT + (net.prototux.aare.ui.car.myplay.HomePadKey.DOWN to net.prototux.aare.ui.car.myplay.HomePadAction.CALLS) }
        rule.onNodeWithTag(OrbitHints.TAG_DOWN).assertExists()
        // PL-R: left and right are apart: one set to nothing loses its icon, the other keeps it.
        rule.runOnIdle { state.homePad = state.homePad + (net.prototux.aare.ui.car.myplay.HomePadKey.LEFT to net.prototux.aare.ui.car.myplay.HomePadAction.NOTHING) }
        rule.onNodeWithTag(OrbitHints.TAG_LEFT).assertDoesNotExist()
        rule.onNodeWithTag(OrbitHints.TAG_RIGHT).assertExists()
        // C: they fade after the panel time without input.
        rule.mainClock.advanceTimeBy(30_000)
        rule.waitForIdle()
        rule.onNodeWithTag(OrbitHints.TAG_UP).assertDoesNotExist()
        rule.onNodeWithTag(OrbitHints.TAG_RIGHT).assertDoesNotExist()
    }

    /**
     * TH-1 (the owner: "our theming doesn't work"): a chosen theme colours ORBIT at once (its glass
     * and Navigate's accent); the default theme keeps ORBIT's own colours.
     */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun orbitTakesTheChosenTheme() {
        val state = show()
        assertEquals(Orbit.NIGHT, Orbit.glass(true))
        val retro = net.prototux.aare.ui.theme.CarThemes.byId("retro")
        rule.runOnIdle { state.theme = retro; state.orbitThemed = true }
        rule.waitForIdle()
        val themed = Orbit.Themed.from(retro)
        assertEquals(themed.night, Orbit.glass(true))
        assertEquals(retro.night.primary, Orbit.accent(net.prototux.aare.ui.car.myplay.World.NAVIGATE, true))
        val bmp = rule.onNodeWithTag(ENGINE_TAG).captureToImage().asAndroidBitmap()
        File(File("build/reports/myplay-screens").apply { mkdirs() }, "orbit_themed_retro.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        // Back to the default: ORBIT's own colours.
        rule.runOnIdle { state.orbitThemed = false }
        rule.waitForIdle()
        assertEquals(Orbit.NIGHT, Orbit.glass(true))
        assertEquals(Color(0xFF6EE7A8), Orbit.accent(net.prototux.aare.ui.car.myplay.World.NAVIGATE, true))
    }

    /** TH-3: the owner's Control size makes ORBIT's parts bigger (the hub's disc here), live. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun controlSizeScalesOrbitsParts() {
        var scale by androidx.compose.runtime.mutableStateOf(1f)
        rule.setContent {
            CarTheme(night = true, theme = net.prototux.aare.ui.theme.CarThemes.byId("aare")) {
                CompositionLocalProvider(net.prototux.aare.ui.theme.LocalCarControlScale provides scale,
                    net.prototux.aare.ui.car.myplay.LocalSkinTokens provides Orbit.tokens(true)) {
                    OrbitSkin.Hub(null, androidx.compose.ui.Modifier.testTag("hub"))
                }
            }
        }
        val normal = rule.onNodeWithTag("hub").fetchSemanticsNode().boundsInRoot.width
        rule.runOnIdle { scale = 1.3f }
        rule.waitForIdle()
        val bigger = rule.onNodeWithTag("hub").fetchSemanticsNode().boundsInRoot.width
        assertEquals(normal * 1.3f, bigger, 2f)
    }

    @org.junit.After fun noThemeLeftOver() { Orbit.override = null }

    /** A picture of the screen as it is now. */
    private fun picture2(name: String) {
        val bmp = rule.onNodeWithTag(ENGINE_TAG).captureToImage().asAndroidBitmap()
        val dir = File("build/reports/myplay-screens").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
