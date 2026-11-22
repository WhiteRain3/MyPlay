package net.prototux.aare.host

import androidx.car.app.model.Alert
import androidx.car.app.model.CarText
import androidx.car.app.model.Distance
import androidx.car.app.navigation.model.Maneuver
import androidx.car.app.navigation.model.MessageInfo
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.RoutingInfo
import androidx.car.app.navigation.model.Step
import androidx.car.app.navigation.model.Trip
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Action
import androidx.car.app.model.DateTimeWithZone
import androidx.car.app.navigation.model.Destination
import androidx.car.app.navigation.model.TravelEstimate
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** MyPlay (N3 check): what a maps app's text the log gets, and what looks like a speed. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NavTextCheckTest {
    @Test fun speedsAreSpotted() {
        assertTrue(NavTextCheck.looksLikeSpeed("Speed camera, 50 km/h"))
        assertTrue(NavTextCheck.looksLikeSpeed("limit 30mph"))
        assertTrue(NavTextCheck.looksLikeSpeed("90 KMH"))
        assertFalse(NavTextCheck.looksLikeSpeed("In 300 m turn right"))
        assertFalse(NavTextCheck.looksLikeSpeed("Main Street 50"))
    }

    @Test fun linesGiveOnlyTheLengthAndTheSpeed() {
        assertEquals(
            listOf("nav text waze alert: a text of 15 characters", "nav text waze alert: a text of 13 characters; looks like a speed: 50 km/h"),
            NavTextCheck.lines("waze", "alert", listOf("Police reported", " ", "Limit 50 km/h", "Police reported")),
        )
    }

    /** NT-1: no road, place or address ever reaches the log. */
    @Test fun noRoadOrAddressIsLogged() {
        val trip = Trip.Builder()
            .addDestination(
                Destination.Builder().setName("Home").setAddress("Gedimino pr. 9, Vilnius").build(),
                TravelEstimate.Builder(Distance.create(9_400.0, Distance.UNIT_METERS), DateTimeWithZone.create(0L, TimeZone.getTimeZone("UTC"))).build(),
            )
            .setCurrentRoad("Konstitucijos pr.")
            .build()
        val texts = NavTextCheck.tripTexts(trip)
        assertFalse("destinations are not read", texts.any { "Gedimino" in it || it == "Home" })
        val lines = NavTextCheck.lines("waze", "trip", texts + "Savanorių pr. 50 km/h")
        for (word in listOf("Konstitucijos", "Savanorių", "Gedimino", "Home", "Vilnius")) {
            assertFalse("$word in $lines", lines.any { word in it })
        }
        assertTrue(lines.any { it.endsWith("looks like a speed: 50 km/h") })
    }

    @Test fun textsAreReadFromAlertsTripsAndTheTurnCard() {
        val alert = Alert.Builder(1, CarText.create("Police reported"), 5_000).setSubtitle(CarText.create("ahead")).build()
        assertEquals(listOf("Police reported", "ahead"), NavTextCheck.alertTexts(alert))

        val step = Step.Builder("Turn right").setRoad("Main Street")
            .setManeuver(Maneuver.Builder(Maneuver.TYPE_TURN_NORMAL_RIGHT).build()).build()
        val trip = Trip.Builder().setCurrentRoad("Gedimino pr.").build()
        assertEquals(listOf("Gedimino pr."), NavTextCheck.tripTexts(trip))

        val routing = RoutingInfo.Builder().setCurrentStep(step, Distance.create(300.0, Distance.UNIT_METERS)).build()
        val strip = ActionStrip.Builder().addAction(Action.APP_ICON).build()
        val nav = NavigationTemplate.Builder().setNavigationInfo(routing).setActionStrip(strip).build()
        assertEquals(listOf("Turn right", "Main Street"), NavTextCheck.templateTexts(nav))

        val message = NavigationTemplate.Builder().setNavigationInfo(MessageInfo.Builder("Arrived").setText("Have a nice day").build())
            .setActionStrip(strip).build()
        assertEquals(listOf("Arrived", "Have a nice day"), NavTextCheck.templateTexts(message))
    }

    /** AL-4: an alert's structure in one line: its id, title, subtitle, buttons and time; no other text. */
    @Test fun anAlertsStructureIsOneLine() {
        val a = Alert.Builder(7, CarText.create("In 380 m"), 8_000)
            .setSubtitle(CarText.create("Reduced speed"))
            .addAction(Action.Builder().setTitle("Close").setOnClickListener {}.build())
            .build()
        assertEquals("nav alert com.waze: id 7, title \"In 380 m\", subtitle \"Reduced speed\", actions [\"Close\"], 8000 ms",
            NavTextCheck.alertStructure("com.waze", a))
    }
}
