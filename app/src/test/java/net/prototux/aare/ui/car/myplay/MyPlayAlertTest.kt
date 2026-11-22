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
        // The ring on its button (Close), so the push answers the alert, not the home pad's music.
        rule.onNodeWithTag(mapAlertStop(0)).assertIsSelected()
        rule.onNodeWithTag(ENGINE_TAG).performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(listOf("close"), ran)
        rule.waitForIdle()
        rule.onNodeWithTag(SLOT_MAP_ALERT).assertDoesNotExist()
        rule.onNodeWithTag(SLOT_MUSIC_PANEL).assertDoesNotExist()
        // Gone, the pad's shortcuts are back.
        rule.onNodeWithTag(ENGINE_TAG).performKeyInput { pressKey(Key.DirectionCenter) }
        rule.onNodeWithTag(SLOT_MUSIC_PANEL).assertExists()
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
}
