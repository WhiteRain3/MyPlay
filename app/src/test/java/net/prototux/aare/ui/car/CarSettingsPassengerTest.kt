package net.prototux.aare.ui.car

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import net.prototux.aare.Settings
import net.prototux.aare.ui.theme.CarTheme
import net.prototux.aare.ui.theme.CarThemes
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** MyPlay (S-4): on the old car settings screen too, passenger mode turns on only while the car is stopped. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w1280dp-h720dp-land-mdpi")
class CarSettingsPassengerTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun passengerModeTurnsOnOnlyWhileStopped() {
        val settings = Settings(rule.activity)
        settings.ignoreDriving = false
        var moving by mutableStateOf(true)
        rule.setContent {
            CarTheme(night = true, theme = CarThemes.byId("aare")) {
                CarSettingsScreen(settings, Modifier.testTag("settings"), restricted = { moving })
            }
        }
        val title = rule.activity.getString(net.prototux.aare.R.string.setup_switch_passenger)
        rule.onNodeWithTag("settings").performScrollToNode(hasText(title))
        rule.onNodeWithText(title).performClick()
        assertFalse("not while moving", settings.ignoreDriving)
        moving = false
        rule.onNodeWithText(title).performClick()
        assertTrue(settings.ignoreDriving)
        moving = true
        rule.onNodeWithText(title).performClick()
        assertFalse("always off", settings.ignoreDriving)
    }
}
