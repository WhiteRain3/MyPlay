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

    @Test fun linesAreDistinctAndMarked() {
        assertEquals(
            listOf("nav text waze alert: Police reported", "nav text waze alert: Limit 50 km/h  [looks like a speed]"),
            NavTextCheck.lines("waze", "alert", listOf("Police reported", " ", "Limit 50 km/h", "Police reported")),
        )
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
}
