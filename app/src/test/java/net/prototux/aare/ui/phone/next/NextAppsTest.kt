package net.prototux.aare.ui.phone.next

import androidx.compose.ui.test.onNodeWithTag
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import net.prototux.aare.Settings
import net.prototux.aare.host.CarRuntime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Apps (plan §16 chunk 8): app_order and hidden_apps written exactly as today. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h2400dp-xxhdpi")
class NextAppsTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val settings get() = Settings(ApplicationProvider.getApplicationContext())

    @Test fun movingKeepsHiddenAppsInTheirSlots() {
        // Today's rule (MainActivity.AppOrder.move): b and d listed, x hidden between them.
        val order = listOf("b", "x", "d")
        assertEquals(listOf("d", "x", "b"), movedOrder(order, listOf("b", "d"), 0, 1))
        // A new key goes at the end.
        assertEquals(listOf("b", "x", "d", "n"), movedOrder(order, listOf("b", "n", "d"), 2, -1))
        assertEquals(listOf("a", "b"), movedOrder(emptyList(), listOf("b", "a"), 0, 1))
    }

    @Test fun theCarsOrder() {
        val apps = listOf(AppKey("z", "Zed"), AppKey("a", "Alpha"), AppKey("m", "Mid"))
        assertEquals(listOf("m", "a", "z"), inCarOrder(apps, listOf("m")).map { it.key })
    }

    @Test fun hiding() {
        assertEquals(setOf("a", "b"), withHidden(setOf("a"), "b", visible = false))
        assertEquals(emptySet<String>(), withHidden(setOf("a"), "a", visible = true))
    }

    @Test fun builtInsWriteTheirIdsToHiddenApps() {
        rule.setContent { NextAppsRoot() }
        rule.waitForIdle()
        rule.onNode(hasText("Notifications") and isToggleable()).performClick()
        assertTrue(CarRuntime.NOTIFICATIONS_ID in settings.hiddenApps)
        rule.onNode(hasText("Notifications") and isToggleable()).performClick()
        assertTrue(CarRuntime.NOTIFICATIONS_ID !in settings.hiddenApps)
    }

    @Test fun reorderWithTheArrows() {
        rule.setContent { NextAppsRoot() }
        // Reorder waits for the lists, read off the main thread.
        rule.waitUntil(10_000) { rule.onAllNodes(hasText("Reorder") and androidx.compose.ui.test.isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Reorder").performClick()
        rule.onNodeWithText("Order in the car").assertExists()
        rule.onNodeWithContentDescription("Move Phone down").performClick()
        rule.waitForIdle()
        val order = settings.appOrder
        assertTrue("Phone moved after Settings: $order", order.indexOf(CarRuntime.SETTINGS_ID) < order.indexOf(CarRuntime.PHONE_ID))
        rule.onNodeWithText("Done").performClick()
        rule.onNodeWithText("Phone apps").assertExists()
    }

    @Test fun phoneAppsWithoutRootOrShizuku() {
        suPresentCheck = { false }
        rule.setContent { NextPhoneAppsPage() }
        rule.onNodeWithText("Needs root or Shizuku").assertExists()
        rule.onNode(hasText("Launch phone apps on the car") and isToggleable()).assertIsNotEnabled()
        // Shizuku isn't installed here: the first step is the one to do, with its button.
        rule.onNodeWithText("Get Shizuku").assertExists()
        rule.onNodeWithText("Come back and tap Allow").assertExists()
    }

    @Test fun howToSetUpOpensTheHelpPage() {
        suPresentCheck = { false }
        rule.setContent { NextApp(start = "apps/phone-apps") { r -> if (r == "apps/phone-apps") NextPhoneAppsPage() else NextShizukuHelpPage() } }
        rule.onNodeWithText("How to set up").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("next-page-apps/phone-apps/shizuku").assertExists()
        rule.onNodeWithText("Pair Shizuku once").assertExists()
    }

    @Test fun aRootedPhoneAsksForRootNotShizuku() {
        suPresentCheck = { true }
        try {
            rule.setContent { NextPhoneAppsPage() }
            rule.onNodeWithText("Allow root").assertExists()
            rule.onNodeWithText("Install Shizuku").assertDoesNotExist()
        } finally { suPresentCheck = { false } }
    }

    @Test fun shizukuSteps() {
        assertEquals(ShizukuStep.INSTALL, shizukuStep(installed = false, running = false, allowed = false))
        assertEquals(ShizukuStep.START, shizukuStep(installed = true, running = false, allowed = false))
        assertEquals(ShizukuStep.ALLOW, shizukuStep(installed = true, running = true, allowed = false))
        assertEquals(ShizukuStep.READY, shizukuStep(installed = true, running = true, allowed = true))
        // Root first (dispatcher), then the gates that would leave the owner stuck (review PH-1).
        assertEquals(ShizukuStep.ROOT_READY, shizukuStep(installed = true, running = true, allowed = false, rootGranted = true, suPresent = true))
        assertEquals(ShizukuStep.ROOT_ALLOW, shizukuStep(installed = false, running = false, allowed = false, suPresent = true))
        assertEquals(ShizukuStep.OLD_ANDROID, shizukuStep(installed = true, running = true, allowed = true, android12 = false))
        assertEquals(ShizukuStep.OLD_SHIZUKU, shizukuStep(installed = true, running = true, allowed = true, preV11 = true))
    }
}
