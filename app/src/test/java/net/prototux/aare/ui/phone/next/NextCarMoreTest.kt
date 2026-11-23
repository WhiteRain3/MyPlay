package net.prototux.aare.ui.phone.next

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import net.prototux.aare.Settings
import net.prototux.aare.car.SpeedSourceMode
import net.prototux.aare.car.myplaySpeedLimitMargin
import net.prototux.aare.car.myplaySpeedSource
import net.prototux.aare.ui.car.myplay.myplayMapsIdleHome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import net.prototux.aare.ui.phone.myplaySpeedLimitRegion
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Car II (plan §16 chunk 7): Maps, Speed, Sound, Messages and calls, Voice. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h2400dp-xxhdpi")
class NextCarMoreTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val settings get() = Settings(ApplicationProvider.getApplicationContext())

    @Test fun mapsWritesTodaysKeysAndLinksToMapColours() {
        rule.setContent { NextApp(start = "car/maps") { if (it == "car/maps") NextMapsPage() else NextPlaceholder(it) } }
        rule.onNodeWithText("Home when maps untouched").performClick()
        rule.onNodeWithText("5 min").performClick()
        rule.waitForIdle()
        assertEquals(5, settings.myplayMapsIdleHome)
        rule.onNode(hasText("Follow the car's lights") and isToggleable()).performClick()
        assertFalse(settings.mapsFollowCarLights)
        rule.onNodeWithText("Map colours").performClick()
        rule.onNodeWithTag("next-page-screen/map-colours").assertExists()
    }

    @Test fun speedWritesTodaysKeys() {
        rule.setContent { NextSpeedPage() }
        rule.onNodeWithText("Car only").performClick()
        assertEquals(SpeedSourceMode.CAR, settings.myplaySpeedSource)
        rule.onNodeWithText("Warn when over by").performClick()
        rule.onNodeWithText("15 km/h").performClick()
        rule.waitForIdle()
        assertEquals(15, settings.myplaySpeedLimitMargin)
        rule.onNodeWithText("Limit from Waze").assertExists()
    }

    @Test fun soundLivesInAdvancedButItsSwitchStaysOnCar() {
        // The owner's car plays over its own Bluetooth: the page moved; the switch carries the maps voice too, so it stays.
        rule.setContent { NextApp(start = "car") { r -> when (r) { "car" -> NextCarRoot(); "more/advanced" -> NextAdvancedPage(); else -> NextPlaceholder(r) } } }
        rule.onNode(hasText("Sound to the car") and isToggleable()).assertExists()
        rule.onNodeWithText("Needs setting up: More › Advanced › Sound").assertExists() // no capture method in the test
        assertFalse(NextRoutes.isKnown("car/sound"))
        assertEquals(NextTab.MORE, NextRoutes.tabOf("more/advanced/sound"))
    }

    @Test fun voiceAsksForTheMicrophoneOnlyWhenMissing() {
        // Review CH-1: since chunk 14 this is the one place the microphone is asked for.
        rule.setContent { NextVoicePage() }
        rule.onNodeWithText("Allow the microphone").assertExists()
    }

    @Test fun voiceSaysNothingWhenTheMicrophoneIsAllowed() {
        org.robolectric.Shadows.shadowOf(androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()).grantPermissions(android.Manifest.permission.RECORD_AUDIO)
        rule.setContent { NextVoicePage() }
        rule.onNodeWithText("Allow the microphone").assertDoesNotExist()
    }

    @Test fun fillInStandardLimitsWritesItsKey() {
        settings.myplaySpeedLimitRegion = net.prototux.aare.ui.phone.SpeedLimitFiles.REGIONS.first().id
        rule.setContent { NextSpeedPage() }
        rule.onNode(hasText("Fill in standard limits") and isToggleable()).performScrollTo().performClick()
        assertEquals(false, settings.prefs.getBoolean(net.prototux.aare.car.KEY_FILL_STANDARD_LIMITS, true))
    }

    @Test fun soundSaysNoMethodNamesOutsideItsRows() {
        rule.setContent { NextSoundPage() }
        // On a phone with nothing set up: the status is words only.
        rule.onNodeWithText("No method set up yet").assertExists()
        rule.onNodeWithText("Shizuku or root").assertExists() // the method's own row
        rule.onAllNodesWithText("Shizuku", substring = true).fetchSemanticsNodes().forEach { node ->
            val t = node.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Text)?.joinToString { it.text }.orEmpty()
            assertFalse("status names a method: $t", t.startsWith("No method") || t.startsWith("Sound reaches"))
        }
        val before = settings.carAudio
        rule.onNode(hasText("Sound to the car") and isToggleable()).performClick()
        assertEquals(!before, settings.carAudio)
    }

    @Test fun messagesAskForAccessInContext() {
        rule.setContent { NextMessagesPage() }
        rule.onNodeWithText("Allow notification access").assertExists()
        rule.onNode(hasText("Show message notifications") and isToggleable()).assertIsNotEnabled()
        rule.onNodeWithText("Allow calls and contacts").assertExists()
        rule.onNode(hasText("Calls on phone speaker") and isToggleable()).assertIsNotEnabled()
        // Do Not Disturb needs no permission to switch; its access row appears once it is on.
        rule.onNode(hasText("Do Not Disturb") and isToggleable()).performClick()
        assertEquals(true, settings.dndWhileDriving)
        rule.onNodeWithText("Allow MyPlay to turn it on").assertExists()
    }

    @Test fun voiceSameAsThePhoneClearsTheChoice() {
        settings.assistantPackage = "com.example.chat"
        rule.setContent { NextVoicePage() }
        rule.onNodeWithText("Same as the phone").performClick()
        assertEquals(null, settings.assistantPackage)
    }

    @Test fun driveWarningsWriteTheirKey() {
        rule.setContent { NextSpeedPage() }
        rule.onNode(hasText("Ice and phone warnings") and isToggleable()).performClick()
        assertEquals(false, settings.prefs.getBoolean(net.prototux.aare.car.KEY_DRIVE_WARNINGS, true))
        // LP-1: the pulse is its own switch, untouched by the warnings one.
        assertEquals(true, settings.prefs.getBoolean(net.prototux.aare.car.KEY_LIMIT_PULSE, true))
        rule.onNode(hasText("Limit sign pulse") and isToggleable()).performScrollTo().performClick()
        assertEquals(false, settings.prefs.getBoolean(net.prototux.aare.car.KEY_LIMIT_PULSE, true))
    }

    @Test fun placesForgetWithUndo() {
        settings.prefs.edit().putString(net.prototux.aare.car.KEY_PLACES,
            net.prototux.aare.car.Places.encode(listOf(net.prototux.aare.car.SavedPlace("Work", "54.68,25.28")))).commit()
        rule.setContent { NextApp(start = "car/buttons") { NextButtonsPage() } }
        rule.onNodeWithText("Work").assertExists()
        rule.onNodeWithText("Add a place").assertExists()
        rule.onNode(androidx.compose.ui.test.hasContentDescription("Forget Work")).performClick()
        rule.waitForIdle()
        assertEquals(0, net.prototux.aare.car.Places.decode(settings.prefs.getString(net.prototux.aare.car.KEY_PLACES, null)).size)
        rule.onNodeWithText("Undo").performClick()
        rule.waitForIdle()
        assertEquals(1, net.prototux.aare.car.Places.decode(settings.prefs.getString(net.prototux.aare.car.KEY_PLACES, null)).size)
    }

    @Test fun parkedSpotSwitchWritesItsKey() {
        rule.setContent { NextMapsPage() }
        rule.onNode(hasText("Remember where I parked") and isToggleable()).performClick()
        assertEquals(true, settings.prefs.getBoolean(net.prototux.aare.car.KEY_PARKED_HERE, false))
    }
}
