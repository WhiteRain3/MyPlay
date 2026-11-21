package net.prototux.aare.car

import net.prototux.aare.car.CarData.Timed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** MyPlay (N3): the speed's rules and its feed, with a fake phone. */
class MyPlaySpeedTest {
    private var now = 100_000L
    private val connected = CarData.State(connected = true)
    private fun carAt(mps: Float, at: Long) = connected.copy(speed = Timed(CarData.Speed(mps, null, null), at))

    /** A phone whose fixes the test sends; counts starts and stops. */
    private class FakeFixes(var permitted: Boolean = true) : PhoneFixes {
        var onFix: ((Float, Long) -> Unit)? = null
        var starts = 0
        var stops = 0
        val on get() = onFix != null
        override fun start(onFix: (Float, Long) -> Unit): Boolean {
            if (!permitted) return false
            starts++
            this.onFix = onFix
            return true
        }
        override fun stop() { stops++; onFix = null }
    }

    @Test fun kmhRoundsAndNeverGoesBelowZero() {
        assertEquals(50, SpeedRules.kmh(13.89f))
        assertEquals(0, SpeedRules.kmh(-1f))
    }

    @Test fun theCarsSpeedWinsWhileFresh() {
        assertEquals(SpeedReading(36, SpeedReading.From.CAR), SpeedRules.pick(SpeedSourceMode.AUTO, 10f, 20f, now, now))
        assertEquals(10f, SpeedRules.carSpeed(carAt(10f, now - 1_000), now))
        assertNull("stale", SpeedRules.carSpeed(carAt(10f, now - SpeedRules.CAR_FRESH_MS - 1), now))
        assertNull("not connected", SpeedRules.carSpeed(carAt(10f, now).copy(connected = false), now))
    }

    @Test fun thePhoneOnlyInAutoAndOnlyFresh() {
        assertEquals(SpeedReading(72, SpeedReading.From.PHONE), SpeedRules.pick(SpeedSourceMode.AUTO, null, 20f, now - 1_000, now))
        assertNull(SpeedRules.pick(SpeedSourceMode.CAR, null, 20f, now, now))
        assertNull(SpeedRules.pick(SpeedSourceMode.AUTO, null, 20f, now - SpeedRules.PHONE_FRESH_MS - 1, now))
        assertNull(SpeedRules.pick(SpeedSourceMode.OFF, 10f, 20f, now, now))
    }

    @Test fun thePhoneIsWantedOnlyWhenEveryRuleHolds() {
        assertTrue(SpeedRules.phoneWanted(SpeedSourceMode.AUTO, shown = true, connected = true, mapsRunning = true, carFresh = false))
        assertFalse(SpeedRules.phoneWanted(SpeedSourceMode.AUTO, shown = false, connected = true, mapsRunning = true, carFresh = false))
        assertFalse(SpeedRules.phoneWanted(SpeedSourceMode.AUTO, shown = true, connected = false, mapsRunning = true, carFresh = false))
        assertFalse(SpeedRules.phoneWanted(SpeedSourceMode.AUTO, shown = true, connected = true, mapsRunning = false, carFresh = false))
        assertFalse(SpeedRules.phoneWanted(SpeedSourceMode.AUTO, shown = true, connected = true, mapsRunning = true, carFresh = true))
        assertFalse(SpeedRules.phoneWanted(SpeedSourceMode.CAR, shown = true, connected = true, mapsRunning = true, carFresh = false))
        assertFalse(SpeedRules.phoneWanted(SpeedSourceMode.OFF, shown = true, connected = true, mapsRunning = true, carFresh = false))
    }

    @Test fun theFeedListensOnlyWhileWantedAndShowsTheFakeFixes() {
        val phone = FakeFixes()
        val feed = SpeedFeed(phone) { now }
        val inputs = SpeedFeed.Inputs(SpeedSourceMode.AUTO, shown = true, mapsRunning = true, car = connected)
        feed.update(inputs)
        assertTrue(feed.listening)
        assertNull("no fix yet: no guess", feed.reading)
        phone.onFix!!(25f, now)
        assertEquals(SpeedReading(90, SpeedReading.From.PHONE), feed.reading)

        // The maps app stops: no location at all.
        feed.update(inputs.copy(mapsRunning = false))
        assertFalse(phone.on)
        assertNull(feed.reading)
        // Back: listening again, but the old fix is gone.
        feed.update(inputs)
        assertTrue(phone.on)
        assertNull(feed.reading)
        assertEquals(2, phone.starts)

        // The car sends its speed: the phone stops.
        feed.update(inputs.copy(car = carAt(10f, now)))
        assertFalse(phone.on)
        assertEquals(SpeedReading(36, SpeedReading.From.CAR), feed.reading)
        // The car stops sending: the phone again.
        now += SpeedRules.CAR_FRESH_MS + 1
        feed.update(inputs.copy(car = carAt(10f, now - SpeedRules.CAR_FRESH_MS - 1)))
        assertTrue(phone.on)

        // The car disconnects, and the speed leaves the screen.
        feed.update(inputs.copy(car = CarData.State()))
        assertFalse(phone.on)
        feed.update(inputs)
        feed.release()
        assertFalse(phone.on)
        assertFalse(feed.listening)
        assertNull(feed.reading)
        assertEquals(phone.starts, phone.stops)
    }

    @Test fun withoutThePermissionNothingListens() {
        val phone = FakeFixes(permitted = false)
        val feed = SpeedFeed(phone) { now }
        feed.update(SpeedFeed.Inputs(SpeedSourceMode.AUTO, shown = true, mapsRunning = true, car = connected))
        assertFalse(feed.listening)
        assertNull(feed.reading)
    }

    @Test fun carModeNeverStartsThePhone() {
        val phone = FakeFixes()
        val feed = SpeedFeed(phone) { now }
        feed.update(SpeedFeed.Inputs(SpeedSourceMode.CAR, shown = true, mapsRunning = true, car = connected))
        assertEquals(0, phone.starts)
    }
}
