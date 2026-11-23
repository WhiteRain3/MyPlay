package net.prototux.aare.ui.phone.next

import androidx.compose.material3.Text
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import net.prototux.aare.Settings
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Settings are observed: a change made anywhere shows without the page copying the value. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NextSettingsTest {
    @get:Rule val rule = createComposeRule()
    private val settings get() = Settings(ApplicationProvider.getApplicationContext())

    @Test fun aChangeFromElsewhereShows() {
        settings.wireless = false
        rule.setContent {
            val wireless = rememberSetting("wireless") { wireless }
            Text(wireless.value.toString(), Modifier.testTag("v"))
        }
        rule.onNodeWithTag("v").assertTextEquals("false")
        settings.wireless = true
        rule.waitForIdle()
        rule.onNodeWithTag("v").assertTextEquals("true")
    }

    @Test fun otherKeysDoNotMatterButAClearDoes() {
        rule.setContent {
            val on = rememberSetting("check_updates") { checkUpdates }
            Text(on.value.toString(), Modifier.testTag("v"))
        }
        settings.checkUpdates = true
        rule.waitForIdle()
        rule.onNodeWithTag("v").assertTextEquals("true")
        settings.prefs.edit().clear().commit()
        rule.waitForIdle()
        rule.onNodeWithTag("v").assertTextEquals("false")
    }

}
