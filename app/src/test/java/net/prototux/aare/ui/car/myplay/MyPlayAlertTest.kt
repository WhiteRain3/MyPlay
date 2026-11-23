package net.prototux.aare.ui.car.myplay

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import net.prototux.aare.ui.car.CarActions
import net.prototux.aare.ui.car.CarLayout
import net.prototux.aare.ui.car.CarScreen
import net.prototux.aare.ui.car.PadWalkStates
import net.prototux.aare.ui.theme.CarTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** MyPlay (AL-1): the maps app's alert on the home, drawn by the skin, its buttons on the knob. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class MyPlayAlertTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val ran = mutableListOf<String>()
    private var appAlert by mutableStateOf<MapAlert?>(null)
    private fun alert() = MapAlert(7, "In 380 m", "Reduced speed", null, listOf("Close" to { ran += "close"; appAlert = null }))

    private fun show(skin: MyPlaySkin = TestSkin, night: Boolean = true): net.prototux.aare.ui.car.CarUiState {
        val state = PadWalkStates.state(CarLayout.MAP_BEHIND, CarScreen.Dashboard).apply { enginePreview = true; this.night = night }
        val actions = object : CarActions by PadWalkStates.NoActions {}
        rule.setContent {
            CarTheme(night = state.night, theme = state.theme) {
                MyPlayEngineShell(state, actions, appContent = { _, m -> Box(m.background(Color.DarkGray)) },
                    navMap = { _, m ->
                        // As the hosted maps app does (CarAppScreen): its alert to the skin, when it draws them.
                        val sink = LocalMapAlertSink.current
                        val a = appAlert
                        DisposableEffect(a, sink) { sink?.invoke(a); onDispose { sink?.invoke(null) } }
                        Box(m.background(Color(0xFF2E4A3A)))
                    },
                    overlay = {}, skin = skin, sources = EngineSources.of(state, actions, LocalContext.current))
            }
        }
        rule.waitForIdle()
        return state
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theAlertIsTheSkinsAndItsButtonTakesThePush() {
        val state = show()
        rule.runOnIdle { state.homePad = HomePad.DEFAULT; appAlert = alert() }
        rule.waitForIdle()
        rule.onNodeWithTag(SLOT_MAP_ALERT).assertExists()
        // The ring on its button (Close), so the push answers the alert, not the home pad's dial.
        rule.onNodeWithTag(mapAlertStop(0)).assertIsSelected()
        rule.onNodeWithTag(ENGINE_TAG).performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(listOf("close"), ran)
        rule.waitForIdle()
        rule.onNodeWithTag(SLOT_MAP_ALERT).assertDoesNotExist()
        rule.onNodeWithTag(SLOT_CHOOSER).assertDoesNotExist()
        // Gone, the pad's shortcuts are back: the centre opens the dial (HN-3).
        rule.onNodeWithTag(ENGINE_TAG).performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(SLOT_CHOOSER).assertExists()
    }

    private fun aNotice() = net.prototux.aare.ui.car.CarNotice("n1", "Anna K.", "Running late", null, listOf("Reply" to { ran += "reply" }))

    /** NR-1: a notice, then an alert: the notice going leaves the ring on the alert; both gone, it is back where it was. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun aNoticeAndAnAlertGiveTheRingBackInTurn() {
        val state = show()
        rule.onNodeWithTag(STOP_HUB).assertIsSelected()
        rule.runOnIdle { state.notices.add(0, aNotice()) }
        rule.waitForIdle()
        rule.onNodeWithTag(net.prototux.aare.ui.car.myplay.NOTICE_DISMISS).assertIsSelected()
        rule.runOnIdle { appAlert = alert() }
        rule.waitForIdle()
        rule.onNodeWithTag(mapAlertStop(0)).assertIsSelected()
        rule.runOnIdle { state.notices.clear() }
        rule.waitForIdle()
        // The alert is still up: the ring stays on its button.
        rule.onNodeWithTag(mapAlertStop(0)).assertIsSelected()
        rule.runOnIdle { appAlert = null }
        rule.waitForIdle()
        rule.onNodeWithTag(STOP_HUB).assertIsSelected()
    }

    /** NR-1: the other order: an alert, then a notice; the alert going leaves the ring on the notice. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun anAlertAndANoticeGiveTheRingBackInTurn() {
        val state = show()
        rule.runOnIdle { appAlert = alert() }
        rule.waitForIdle()
        rule.runOnIdle { state.notices.add(0, aNotice()) }
        rule.waitForIdle()
        rule.onNodeWithTag(net.prototux.aare.ui.car.myplay.NOTICE_DISMISS).assertIsSelected()
        rule.runOnIdle { appAlert = null }
        rule.waitForIdle()
        rule.onNodeWithTag(net.prototux.aare.ui.car.myplay.NOTICE_DISMISS).assertIsSelected()
        rule.runOnIdle { state.notices.clear() }
        rule.waitForIdle()
        rule.onNodeWithTag(STOP_HUB).assertIsSelected()
    }

    /** G-4: while the alert or the music panel holds the knob, the car side takes a push as a plain press. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theAlertAndThePanelMakeThePushPlain() {
        val state = show()
        org.junit.Assert.assertFalse(state.knobBusy)
        rule.runOnIdle { appAlert = alert() }
        rule.waitForIdle()
        org.junit.Assert.assertTrue(state.knobBusy)
        rule.runOnIdle { appAlert = null }
        rule.waitForIdle()
        org.junit.Assert.assertFalse(state.knobBusy)
        rule.onNodeWithTag(STOP_SONG).performClick()
        rule.onNodeWithTag(SLOT_MUSIC_PANEL).assertExists()
        org.junit.Assert.assertTrue(state.knobBusy)
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        org.junit.Assert.assertFalse(state.knobBusy)
    }

    /** G-5: an app opened while the panel held the knob: the knob is free again in the app. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun anAppOpenedFromThePanelFreesTheKnob() {
        val state = show()
        rule.onNodeWithTag(STOP_SONG).performClick()
        rule.onNodeWithTag(SLOT_MUSIC_PANEL).assertExists()
        org.junit.Assert.assertTrue(state.knobBusy)
        rule.runOnIdle { state.screen = CarScreen.App(state.launcherApps[0].id) }
        rule.waitForIdle()
        org.junit.Assert.assertFalse(state.knobBusy)
    }

    /** MC-2: ORBIT's pill sits at the bottom between the hub and the song, never in the keep-clear zone round the middle (Waze's arrow). */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theAlertKeepsClearOfTheMiddle() {
        show(skin = net.prototux.aare.ui.car.myplay.orbit.OrbitSkin)
        rule.runOnIdle { appAlert = alert() }
        rule.waitForIdle()
        val pill = rule.onNodeWithTag(SLOT_MAP_ALERT).fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue("clear of the middle: $pill", pill.top >= 360f + 720f * ALERT_KEEP_CLEAR)
        org.junit.Assert.assertEquals("just above the bottom margin: $pill", 720f - 43.2f, pill.bottom, 1f)
        org.junit.Assert.assertEquals("centred: $pill", 640f, pill.center.x, 1f)
        for (tag in listOf(STOP_HUB, STOP_SONG)) {
            val b = rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            org.junit.Assert.assertFalse("$tag $b overlaps $pill", b.overlaps(pill))
        }
        val bmp = rule.onNodeWithTag(ENGINE_TAG).captureToImage().asAndroidBitmap()
        val dir = java.io.File("build/reports/myplay-screens").apply { mkdirs() }
        java.io.File(dir, "orbit_alert_placed.png").outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** ORBIT polish: a long button label is cut, never wider than ALERT_BUTTON_MAX, so the alert's title keeps its room. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun aLongAlertButtonIsCut() {
        show(skin = net.prototux.aare.ui.car.myplay.orbit.OrbitSkin)
        rule.runOnIdle { appAlert = MapAlert(8, "In 380 m", "Police reported", null, listOf("Thanks, I can see it clearly ahead of me" to { ran += "long" })) }
        rule.waitForIdle()
        val button = rule.onNodeWithTag(mapAlertStop(0)).fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue("button $button", button.width <= 200f + 1f)
        rule.onNodeWithTag(SLOT_MAP_ALERT).assertExists()
    }

    /** Look C (design 20): a timed alert's icon sits in a ring that counts its time down; an untimed one has none. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun aTimedAlertCountsDownRoundItsIcon() {
        show(skin = net.prototux.aare.ui.car.myplay.orbit.OrbitSkin)
        rule.runOnIdle { appAlert = alert().copy(durationMs = 8_000, shownAtMs = android.os.SystemClock.uptimeMillis()) }
        rule.waitForIdle()
        rule.onNodeWithTag(net.prototux.aare.ui.car.myplay.orbit.OrbitSkin.ALERT_COUNTDOWN_TAG).assertExists()
        rule.runOnIdle { appAlert = alert().copy(key = 9) }
        rule.waitForIdle()
        rule.onNodeWithTag(SLOT_MAP_ALERT).assertExists()
        rule.onNodeWithTag(net.prototux.aare.ui.car.myplay.orbit.OrbitSkin.ALERT_COUNTDOWN_TAG).assertDoesNotExist()
    }

    @Test fun theTimeLeftRunsFromOneToNothing() {
        val a = alert().copy(durationMs = 10_000, shownAtMs = 1_000)
        assertEquals(1f, alertTimeLeft(a, 1_000)!!, 0f)
        assertEquals(0.75f, alertTimeLeft(a, 3_500)!!, 0.0001f)
        assertEquals(0f, alertTimeLeft(a, 20_000)!!, 0f)
        assertEquals(1f, alertTimeLeft(a, 500)!!, 0f)
        assertEquals(null, alertTimeLeft(alert(), 5_000))
    }

    @Test fun thePillsPlaceIsWorkedOut() {
        val hub = FocusGraph.Box(44f, 464f, 256f, 676f)
        val song = FocusGraph.Box(886f, 524f, 1236f, 676f)
        assertEquals(AlertPlace(43.2f, 428f), alertPlace(1280f, 720f, hub, song, 16f).let { AlertPlace(Math.round(it.bottom * 10) / 10f, it.maxWidth) })
        // No song: only the hub narrows it, on both sides alike.
        assertEquals(2 * (640f - 256f - 32f), alertPlace(1280f, 720f, hub, null, 16f).maxWidth, 0.01f)
    }

    /** AL-2: after the alert, the ring is back on the stop it was on. */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun afterTheAlertTheRingIsBackWhereItWas() {
        show()
        val engine = rule.onNodeWithTag(ENGINE_TAG)
        engine.performKeyInput { pressKey(Key.Tab) }
        rule.onNodeWithTag(STOP_SONG).assertIsSelected()
        rule.runOnIdle { appAlert = alert() }
        rule.waitForIdle()
        rule.onNodeWithTag(mapAlertStop(0)).assertIsSelected()
        rule.runOnIdle { appAlert = null }
        rule.waitForIdle()
        rule.onNodeWithTag(STOP_SONG).assertIsSelected()
    }

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theAlertNeverCoversTheTurnTheHubOrTheSong() = neverCovers(TestSkin)

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun orbitsAlertNeverCoversTheTurnTheHubOrTheSong() = neverCovers(net.prototux.aare.ui.car.myplay.orbit.OrbitSkin, "orbit-alert-1280x720-night")

    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi")
    fun orbitsAlertAt901x543Day() = neverCovers(net.prototux.aare.ui.car.myplay.orbit.OrbitSkin, "orbit-alert-901x543-day", night = false)

    private fun neverCovers(skin: MyPlaySkin, picture: String? = null, night: Boolean = true) {
        val state = show(skin, night)
        rule.runOnIdle {
            state.phoneGuidance = net.prototux.aare.notify.PhoneGuidance("k", "mock.maps", "Maps", "300 m", "Main Street", "", null, null, "")
            appAlert = alert()
        }
        rule.waitForIdle()
        fun box(tag: String) = rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        val a = box(SLOT_MAP_ALERT)
        for (t in listOf(STOP_TURN, STOP_HUB, STOP_SONG)) {
            org.junit.Assert.assertFalse("$t ${box(t)} under the alert $a", box(t).overlaps(a))
        }
        picture?.let(::savePicture)
    }

    private fun savePicture(name: String) {
        val bmp = rule.onNodeWithTag(ENGINE_TAG).captureToImage().asAndroidBitmap()
        val dir = java.io.File("build/reports/myplay-screens").apply { mkdirs() }
        java.io.File(dir, "$name.png").outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    /**
     * Look C (design 31): on ORBIT a call's pill sits at the bottom of the free area, so the turn and
     * lanes stay clear; while a map alert takes that place, the pill goes back to the top.
     */
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi")
    fun theCallsPillIsAtTheBottomUnlessAnAlertIsThere() {
        org.robolectric.Shadows.shadowOf(androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()).grantPermissions(android.Manifest.permission.ANSWER_PHONE_CALLS)
        val state = show(net.prototux.aare.ui.car.myplay.orbit.OrbitSkin)
        rule.runOnIdle { state.call = net.prototux.aare.ui.car.CarCall(ringing = false, name = "Anna K.", sinceMs = System.currentTimeMillis()) }
        rule.waitForIdle()
        val pill = rule.onNodeWithTag(SLOT_IN_CALL_BELOW).fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue("at the bottom: $pill", pill.top > 360f)
        rule.onNodeWithTag(STOP_HANG_UP).assertExists()
        // A map alert takes the bottom: the pill moves to the top, and both stay.
        rule.runOnIdle { appAlert = alert() }
        rule.waitForIdle()
        rule.onNodeWithTag(SLOT_IN_CALL_BELOW).assertDoesNotExist()
        rule.onNodeWithTag(SLOT_MAP_ALERT).assertExists()
        val top = rule.onNodeWithTag(STOP_HANG_UP).fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue("at the top: $top", top.bottom < 360f)
    }
}
