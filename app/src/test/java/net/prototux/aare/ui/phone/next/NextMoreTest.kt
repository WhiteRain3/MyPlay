package net.prototux.aare.ui.phone.next

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import net.prototux.aare.PinnedHeadUnits
import net.prototux.aare.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** More I (plan §16 chunk 9): the root, Cars, Connection, Backup. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h2400dp-xxhdpi")
class NextMoreTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val settings get() = Settings(ctx)

    @Test fun oneListOfCars() {
        val known = mapOf("Sedan" to 200L, "Hatchback" to 100L)
        val pins = mapOf("AA:BB" to setOf("f1"), "tcp" to setOf("f2"))
        val labels = mapOf("AA:BB" to "Sedan", "tcp" to "Head unit server")
        val cars = carEntries(known, pins) { labels.getValue(it) }
        assertEquals(listOf("Sedan", "Hatchback", "Head unit server"), cars.map { it.name })
        assertEquals("AA:BB", cars[0].pinKey)
        assertEquals(setOf("f1"), cars[0].fingerprints)
        assertEquals(null, cars[1].pinKey)
        assertEquals(null, cars[2].lastMs)
    }

    @Test fun forgetTakesEffectOnlyAfterTheUndoTime() {
        settings.rememberCar("Sedan")
        rule.setContent { NextApp(start = "more/cars") { r -> if (r == "more/cars") NextCarsPage() else if (r.startsWith(NextRoutes.CAR_PREFIX)) NextCarPage(r.removePrefix(NextRoutes.CAR_PREFIX)) else NextPlaceholder(r) } }
        rule.onNodeWithText("Sedan").performClick()
        rule.onNodeWithText("Forget this car").performClick()
        rule.waitForIdle()
        // Hidden at once, kept until the time is up.
        rule.onNodeWithText("Cars appear here after the first connection.").assertExists()
        assertTrue("Sedan" in settings.knownCars)
        rule.mainClock.advanceTimeBy(10_000)
        rule.waitForIdle()
        assertFalse("Sedan" in settings.knownCars)
    }

    @Test fun forgetCanBeUndone() {
        settings.rememberCar("Sedan")
        rule.setContent { NextApp(start = "more/cars") { r -> if (r == "more/cars") NextCarsPage() else if (r.startsWith(NextRoutes.CAR_PREFIX)) NextCarPage(r.removePrefix(NextRoutes.CAR_PREFIX)) else NextPlaceholder(r) } }
        rule.onNodeWithText("Sedan").performClick()
        rule.onNodeWithText("Forget this car").performClick()
        rule.onNodeWithText("Undo").performClick()
        rule.mainClock.advanceTimeBy(10_000)
        rule.waitForIdle()
        assertTrue("Sedan" in settings.knownCars)
        rule.onNodeWithText("Sedan").assertExists()
    }

    @Test fun forgettingATrustedCarClearsThePinToo() {
        settings.rememberCar("AA:BB:CC:DD:EE:FF")
        PinnedHeadUnits(ctx).add("AA:BB:CC:DD:EE:FF", "abcd")
        rule.setContent { NextApp(start = "more/cars") { r -> if (r == "more/cars") NextCarsPage() else if (r.startsWith(NextRoutes.CAR_PREFIX)) NextCarPage(r.removePrefix(NextRoutes.CAR_PREFIX)) else NextPlaceholder(r) } }
        rule.onNodeWithText("Trusted").assertExists()
        rule.onNodeWithText("AA:BB:CC:DD:EE:FF").performClick()
        rule.onNodeWithText("Forget this car").performClick()
        rule.mainClock.advanceTimeBy(10_000)
        rule.waitForIdle()
        assertTrue(PinnedHeadUnits(ctx).all.isEmpty())
        assertFalse("AA:BB:CC:DD:EE:FF" in settings.knownCars)
    }

    @Test fun connectionWritesWireless() {
        settings.wireless = false
        rule.setContent { NextConnectionPage() }
        rule.onNode(hasText("Wireless") and isToggleable()).performClick()
        assertTrue(settings.wireless)
        rule.onNodeWithText("Auto").assertExists()
    }

    @Test fun theRootShowsItsValues() {
        rule.setContent { NextMoreRoot() }
        rule.onNodeWithText("Never").assertExists()
        rule.onNodeWithText("Not used").assertExists()
        rule.onNodeWithText("Normal").assertExists()
        rule.onNodeWithTag("next-page-more").assertExists()
    }

    @Test fun aBackupRoundTripsThroughTheSameEngine() {
        // The page uses SettingsBackup unchanged: an export made today loads back to the same values.
        settings.wireless = true
        settings.carTextSize = "larger"
        settings.prefs.edit().putString("myplay_map_filter", "muted").putBoolean(net.prototux.aare.car.KEY_DRIVE_WARNINGS, false).commit()
        val file = net.prototux.aare.SettingsBackup.export(settings.all).toString(2)
        settings.prefs.edit().clear().commit()
        val imported = net.prototux.aare.SettingsBackup.import(org.json.JSONObject(file))
        settings.restore(imported.values)
        assertTrue(settings.wireless)
        assertEquals("larger", settings.carTextSize)
        assertEquals("muted", settings.prefs.getString("myplay_map_filter", null))
        assertEquals(false, settings.prefs.getBoolean(net.prototux.aare.car.KEY_DRIVE_WARNINGS, true))
    }
}
