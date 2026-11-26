package net.prototux.aare.ui.phone.next

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import net.prototux.aare.cert.CertificateUpdates
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Home's states drawn, its buttons, and where notifications lead (plan §16 chunk 2). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h2400dp-xxhdpi")
class NextHomeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val taps = mutableListOf<String>()
    private val actions = object : HomeActions {
        override fun trust(hero: Hero.Trust) { taps += "trust" }
        override fun notNow(hero: Hero.Trust) { taps += "notNow" }
        override fun cancel() { taps += "cancel" }
        override fun disconnect() { taps += "disconnect" }
        override fun tryAgain() { taps += "tryAgain" }
        override fun fix(why: Why) { taps += "fix $why" }
        override fun preview() { taps += "preview" }
        override fun setUp(item: SetupItem) { taps += "setUp $item" }
        override fun dismissUpdate() { taps += "dismissUpdate" }
        override fun dismissCertificate() { taps += "dismissCertificate" }
        override fun screenshot() { taps += "screenshot" }
    }

    private fun show(hero: Hero, attention: Set<Attention> = emptySet(), setupLeft: List<SetupItem> = emptyList(),
                     certificate: CertificateUpdates.Expiring? = null) {
        rule.setContent {
            NextHomeContent(HomeInputs(hero, attention, DesignInUse.ORBIT, setupLeft = setupLeft, certificate = certificate), actions) {
                Box(Modifier.height(120.dp))
            }
        }
    }

    @Test fun connected() {
        show(Hero.Connected("Sedan", Link.USB, "1080p", "MyPlay's"))
        rule.onNodeWithText("Connected").assertExists()
        rule.onNodeWithText("Sedan · USB").assertExists()
        rule.onNodeWithText("Disconnect").performClick()
        rule.onNodeWithText("Preview").performClick()
        assertEquals(listOf("disconnect", "preview"), taps)
    }

    @Test fun carScreenshotOnlyWhileTheCarShowsMyPlay() {
        rule.setContent {
            NextHomeContent(HomeInputs(Hero.Connected("Sedan", Link.USB, "1080p", "MyPlay's"), emptySet(), DesignInUse.ORBIT, canScreenshot = true), actions) {
                Box(Modifier.height(120.dp))
            }
        }
        rule.onNodeWithText("Car screenshot").performClick()
        assertEquals(listOf("screenshot"), taps)
    }

    @Test fun noCarScreenshotWhenNotLive() {
        show(Hero.Connected("Sedan", Link.USB, "1080p", "MyPlay's"))
        rule.onNodeWithText("Car screenshot").assertDoesNotExist()
    }

    @Test fun connecting() {
        show(Hero.Connecting("Sedan", Link.USB))
        rule.onNodeWithText("Connecting to Sedan…").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(listOf("cancel"), taps)
    }

    @Test fun bluetoothOff() {
        show(Hero.NotConnected(Why.BLUETOOTH_OFF))
        rule.onNodeWithText("Bluetooth is off").assertExists()
        rule.onNodeWithText("Turn on Bluetooth").performClick()
        assertEquals(listOf("fix BLUETOOTH_OFF"), taps)
    }

    @Test fun waitingHasNoAction() {
        show(Hero.NotConnected(Why.WAITING))
        rule.onNodeWithText("Plug in USB or start the car").assertExists()
        rule.onNodeWithText("Or plug in a USB cable").assertDoesNotExist()
    }

    @Test fun trustSaysNewCarAndBothAnswersAreTheSameSize() {
        show(Hero.Trust("k", "4f2a9c1e07b3d5a8ffff", false, Link.USB))
        rule.onNodeWithText("New car · USB").assertExists()
        rule.onNodeWithText("SHA-256 4F2A 9C1E 07B3 D5A8").assertExists()
        val trust = rule.onNodeWithText("Trust").fetchSemanticsNode().boundsInRoot
        val notNow = rule.onNodeWithText("Not now").fetchSemanticsNode().boundsInRoot
        assertEquals(trust.height, notNow.height)
        rule.onNodeWithText("Trust").performClick()
        rule.onNodeWithText("Not now").performClick()
        assertEquals(listOf("trust", "notNow"), taps)
    }

    @Test fun couldNotConnect() {
        show(Hero.Failed("The car stopped the video. Code 4"))
        rule.onNodeWithText("Couldn't connect").assertExists()
        rule.onNodeWithText("The car stopped the video").assertExists()
        rule.onNodeWithText("What happened").performClick()
        rule.onNodeWithText("The car stopped the video. Code 4").assertExists()
        rule.onNodeWithText("Try again").performClick()
        assertEquals(listOf("tryAgain"), taps)
    }

    @Test fun finishSetupCountsAndOpens() {
        show(Hero.NotConnected(Why.WAITING), setOf(Attention.FINISH_SETUP), listOf(SetupItem.NOTIFICATION_ACCESS, SetupItem.CAR_AUDIO))
        rule.onNodeWithText("2 left").performClick()
        rule.onNodeWithText("Notification access").performClick()
        assertEquals(listOf("setUp NOTIFICATION_ACCESS"), taps)
    }

    @Test fun anExpiredCertificateCannotBeDismissed() {
        val e = CertificateUpdates.Expiring("Google", java.util.Date(0), expired = true)
        show(Hero.NotConnected(Why.WAITING), setOf(Attention.CERT_EXPIRED), certificate = e)
        rule.onNodeWithText("Certificate expired").assertExists()
        rule.onAllNodes(androidx.compose.ui.test.hasContentDescription("Dismiss")).assertCountEquals(0)
    }

    @Test fun notificationsOpenTheirPages() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val codes = mapOf(
            NextDeepLink.CODE_STATUS to "home", NextDeepLink.CODE_TRUST to "home",
            NextDeepLink.CODE_CERT_NOTICE to "more/advanced/certificates", NextDeepLink.CODE_CERT_EXPIRY to "more/advanced/certificates",
        )
        assertEquals("request codes are distinct", codes.size, codes.keys.toSet().size)
        for ((code, route) in codes) {
            val pi = NextDeepLink.pendingIntent(ctx, route, code)
            val sent = shadowOf(pi).savedIntent
            assertEquals(route, NextDeepLink.routeOf(sent))
            assertEquals(code, shadowOf(pi).requestCode)
        }
        @Suppress("unused") val keep = listOf(Notification::class, NotificationManager::class)
    }
}
