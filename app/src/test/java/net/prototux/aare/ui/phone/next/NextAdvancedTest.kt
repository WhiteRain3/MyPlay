package net.prototux.aare.ui.phone.next

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import net.prototux.aare.Settings
import net.prototux.aare.car.VehicleOverrides
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** More III, first part (plan §16 chunk 11): Advanced, Certificates, Developer. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h2400dp-xxhdpi")
class NextAdvancedTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val settings get() = Settings(ctx)

    private val pages: @Composable (String) -> Unit = { r ->
        when (r) {
            "more/advanced" -> NextAdvancedPage()
            "more/advanced/certificates" -> NextCertificatesPage()
            "more/advanced/developer" -> NextDeveloperPage()
            else -> NextPlaceholder(r) {}
        }
    }

    @Test fun theResolutionPickerWritesTheSameKey() {
        rule.setContent { NextApp(start = "more/advanced", page = pages) }
        rule.onNodeWithText("Maximum resolution").performClick()
        rule.onNodeWithText("720p").performClick()
        rule.waitForIdle()
        assertEquals(720, settings.maxVideoHeight)
        assertEquals(720, settings.prefs.getInt("max_video_height", 0))
    }

    @Test fun theServerOptionsShowOnlyWithTheServerAndEachRiskyOneWarns() {
        rule.setContent { NextApp(start = "more/advanced/developer", page = pages) }
        rule.onNodeWithText("Allow localhost").assertDoesNotExist()
        rule.onNodeWithText("Head unit server").performClick()
        rule.waitForIdle()
        assertTrue(settings.headUnitServer)
        rule.onNodeWithText("Any app on this phone can connect").assertExists()
        rule.onNodeWithText("Anyone on the Wi-Fi can connect").assertExists()
        rule.onNodeWithText("They can see and tap the phone").assertExists()
        rule.onNodeWithText("Allow localhost").performClick()
        rule.waitForIdle()
        assertTrue(settings.prefs.getBoolean("hu_server_localhost", false))
        // Off after one session keeps today's default (on).
        assertTrue(settings.headUnitServerLanOnce)
    }

    @Test fun theDetailedLogIsOnlyALinkHere() {
        rule.setContent { NextApp(start = "more/advanced/developer", page = pages) }
        rule.onNodeWithText("Lives in Connection log").assertExists()
        rule.onNodeWithText("Detailed protocol log").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("next-page-more/diagnostics/log").assertExists()
    }

    @Test fun aTemplateAsksBeforeReplacingTheFile() {
        val file = VehicleOverrides.file(ctx)
        file.parentFile?.mkdirs()
        file.writeText("{\"rules\":[]}")
        rule.setContent { NextApp(start = "more/advanced", page = pages) }
        rule.onNodeWithTag("next-page-more/advanced").performScrollToNode(hasText("Template"))
        rule.onNodeWithText("Template").performClick()
        rule.onNodeWithText("Replace vehicles.json?").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        assertEquals("{\"rules\":[]}", file.readText())
        rule.onNodeWithText("Template").performClick()
        rule.onNodeWithText("Replace").performClick()
        rule.waitForIdle()
        assertEquals(VehicleOverrides.template(), file.readText())
        file.delete()
    }

    @Test fun onlyAddedCertificatesCanBeRemoved() {
        rule.setContent { NextApp(start = "more/advanced/certificates", page = pages) }
        rule.waitUntil(10_000) { rule.onAllNodes(hasText("This phone's own")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Remove").assertDoesNotExist()
        rule.onNodeWithText("Update it automatically").performClick()
        rule.waitForIdle()
        assertTrue(settings.certAutoUpdate)
        assertFalse(settings.prefs.getBoolean("hu_server", false))
    }
}
