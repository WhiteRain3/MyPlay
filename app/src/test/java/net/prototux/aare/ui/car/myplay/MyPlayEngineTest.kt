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
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import net.prototux.aare.ui.car.CarLayout
import net.prototux.aare.ui.car.CarScreen
import net.prototux.aare.ui.car.CarShell
import net.prototux.aare.ui.car.CarUiState
import net.prototux.aare.ui.car.PadWalkStates
import net.prototux.aare.ui.theme.CarTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** MyPlay (N1): the engine with the test skin, through the car shell's hidden preview. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(ExperimentalTestApi::class)
class MyPlayEngineTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun show(night: Boolean = true): CarUiState {
        val state = PadWalkStates.state(CarLayout.MAP_BEHIND, CarScreen.Dashboard).apply {
            enginePreview = true
            this.night = night
        }
        rule.setContent {
            CarTheme(night = state.night, theme = state.theme) {
                CarShell(state, PadWalkStates.NoActions, appContent = { _, m -> Box(m.background(Color.DarkGray)) },
                    navMap = { _, m -> Box(m.background(Color(0xFF2E4A3A))) })
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
        rule.onNodeWithTag(SLOT_SONG).assertExists()
        rule.onNodeWithText(PadWalkStates.SONG_TITLE).assertExists()
        // No route in the fixture: no turn or trip slot.
        assertTrue(rule.onAllNodes(hasTestTag(SLOT_TURN)).fetchSemanticsNodes().isEmpty())

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
