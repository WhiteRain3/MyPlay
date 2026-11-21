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
        var onFix: ((PhoneFix) -> Unit)? = null
        var starts = 0
        var stops = 0
        val on get() = onFix != null
        override fun start(onFix: (PhoneFix) -> Unit): Boolean {
            if (!permitted) return false
            starts++
            this.onFix = onFix
            return true
        }
        override fun stop() { stops++; onFix = null }
    }

    private fun fix(mps: Float?, at: Long) = PhoneFix(mps, 54.68, 25.27, 90f, 5f, at)

    /** Limits the test answers by hand (as the real lookup answers later, on the main thread). */
    private class FakeLimits(var there: Boolean = true) : SpeedLimits {
        val asked = ArrayList<Pair<PhoneFix, (LimitShown) -> Unit>>()
        var closed = false
        override fun available() = there
        override fun lookup(fix: PhoneFix, onResult: (LimitShown) -> Unit) { asked += fix to onResult }
        override fun close() { closed = true }
        fun answer(r: LimitShown) = asked.last().second(r)
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
        phone.onFix!!(fix(25f, now))
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

    // N20: the speed limit.

    @Test fun overTheLimitOnlyByMoreThanTheMargin() {
        val fifty = LimitShown.Known(50)
        assertFalse(SpeedRules.over(59, fifty, 9))
        assertTrue(SpeedRules.over(60, fifty, 9))
        assertFalse(SpeedRules.over(64, fifty, 15))
        assertTrue(SpeedRules.over(56, fifty, 5))
        assertFalse("the warning turned off", SpeedRules.over(150, fifty, SpeedRules.MARGIN_OFF))
        assertFalse("no limit known: never", SpeedRules.over(150, LimitShown.Unknown, 9))
        assertFalse(SpeedRules.over(150, null, 9))
    }

    @Test fun withALimitFileThePhonesFixesAreWantedEvenWithTheCarsSpeed() {
        assertTrue(SpeedRules.phoneWanted(SpeedSourceMode.AUTO, shown = true, connected = true, mapsRunning = true, carFresh = true, limitsOn = true))
        // The other battery rules still hold.
        assertFalse(SpeedRules.phoneWanted(SpeedSourceMode.AUTO, shown = true, connected = true, mapsRunning = false, carFresh = true, limitsOn = true))
        assertFalse(SpeedRules.phoneWanted(SpeedSourceMode.AUTO, shown = false, connected = true, mapsRunning = true, carFresh = true, limitsOn = true))
        assertFalse(SpeedRules.phoneWanted(SpeedSourceMode.CAR, shown = true, connected = true, mapsRunning = true, carFresh = true, limitsOn = true))
    }

    @Test fun theFeedShowsTheLimitFromAFreshFixAndColoursTheSpeedOverIt() {
        val phone = FakeFixes()
        val limits = FakeLimits()
        val feed = SpeedFeed(phone, limits) { now }
        val inputs = SpeedFeed.Inputs(SpeedSourceMode.AUTO, shown = true, mapsRunning = true, car = carAt(60 / 3.6f, now), limitsOn = true)
        feed.update(inputs)
        assertTrue("the car's speed, and the phone's fixes for the position", phone.on)
        assertEquals(SpeedReading(60, SpeedReading.From.CAR, LimitShown.Unknown), feed.reading)
        phone.onFix!!(fix(16.7f, now))
        assertEquals(1, limits.asked.size)
        limits.answer(LimitShown.Known(50))
        assertEquals(SpeedReading(60, SpeedReading.From.CAR, LimitShown.Known(50), over = true), feed.reading)
        feed.update(inputs.copy(margin = 15))
        assertEquals(SpeedReading(60, SpeedReading.From.CAR, LimitShown.Known(50), over = false), feed.reading)
        feed.update(inputs.copy(margin = SpeedRules.MARGIN_OFF))
        assertFalse(feed.reading!!.over)

        // No fix for a while: no limit known (never an old one).
        now += SpeedRules.PHONE_FRESH_MS + 1
        feed.update(inputs.copy(car = carAt(60 / 3.6f, now)))
        assertEquals(LimitShown.Unknown, feed.reading!!.limit)
        assertFalse(feed.reading!!.over)

        // An answer to an older fix that arrives late is dropped.
        phone.onFix!!(fix(16.7f, now))
        val late = limits.asked.last().second
        phone.onFix!!(fix(16.7f, now))
        limits.answer(LimitShown.Known(90))
        late(LimitShown.Known(30))
        assertEquals(LimitShown.Known(90), feed.reading!!.limit)
    }

    @Test fun noFileNoLimitAndThePhoneRestsWhileTheCarSendsItsSpeed() {
        val phone = FakeFixes()
        val limits = FakeLimits()
        val feed = SpeedFeed(phone, limits) { now }
        feed.update(SpeedFeed.Inputs(SpeedSourceMode.AUTO, shown = true, mapsRunning = true, car = carAt(10f, now), limitsOn = false))
        assertFalse(phone.on)
        assertEquals(SpeedReading(36, SpeedReading.From.CAR), feed.reading)
        assertTrue(limits.asked.isEmpty())
    }

    @Test fun aLimitAnswerAfterTheSpeedLeftTheScreenIsDropped() {
        val phone = FakeFixes()
        val limits = FakeLimits()
        val feed = SpeedFeed(phone, limits) { now }
        feed.update(SpeedFeed.Inputs(SpeedSourceMode.AUTO, shown = true, mapsRunning = true, car = carAt(10f, now), limitsOn = true))
        phone.onFix!!(fix(10f, now))
        feed.release()
        limits.answer(LimitShown.Known(50))
        assertNull(feed.reading)
        assertFalse(phone.on)
    }

    @Test fun carModeShowsNoLimit() {
        val phone = FakeFixes()
        val feed = SpeedFeed(phone, FakeLimits()) { now }
        feed.update(SpeedFeed.Inputs(SpeedSourceMode.CAR, shown = true, mapsRunning = true, car = carAt(10f, now), limitsOn = true))
        assertEquals(0, phone.starts)
        assertEquals(SpeedReading(36, SpeedReading.From.CAR), feed.reading)
    }

    @Test fun aFixWithoutASpeedLeavesTheSpeedAlone() {
        val phone = FakeFixes()
        val feed = SpeedFeed(phone) { now }
        feed.update(SpeedFeed.Inputs(SpeedSourceMode.AUTO, shown = true, mapsRunning = true, car = connected))
        phone.onFix!!(fix(25f, now))
        now += 300
        phone.onFix!!(PhoneFix(null, 54.68, 25.27, null, 40f, now)) // another app's network fix
        assertEquals(SpeedReading(90, SpeedReading.From.PHONE), feed.reading)
    }

    @Test fun aPoorFixAsksForNoLimitAndKeepsTheLastOne() {
        val phone = FakeFixes()
        val limits = FakeLimits()
        val feed = SpeedFeed(phone, limits) { now }
        val inputs = SpeedFeed.Inputs(SpeedSourceMode.AUTO, shown = true, mapsRunning = true, car = carAt(10f, now), limitsOn = true)
        feed.update(inputs)
        phone.onFix!!(fix(10f, now))
        limits.answer(LimitShown.Known(50))
        now += 500
        phone.onFix!!(PhoneFix(10f, 54.68, 25.27, 90f, 100f, now)) // 100 m
        phone.onFix!!(PhoneFix(null, 54.68, 25.27, null, 20f, now)) // network: no speed, no heading
        phone.onFix!!(PhoneFix(10f, 54.68, 25.27, 90f, null, now)) // accuracy not known
        assertEquals(1, limits.asked.size)
        feed.update(inputs.copy(car = carAt(10f, now)))
        assertEquals(LimitShown.Known(50), feed.reading!!.limit)
        // Only good fixes for longer than the freshness window: no limit known.
        now += SpeedRules.PHONE_FRESH_MS
        feed.update(inputs.copy(car = carAt(10f, now)))
        assertEquals(LimitShown.Unknown, feed.reading!!.limit)
    }

    // The owner's drive: why the limit shows or not.

    @Test fun theFirstReasonThatHoldsIsTheOneGiven() {
        fun why(mode: SpeedSourceMode = SpeedSourceMode.AUTO, file: LimitFile = LimitFile.READY, connected: Boolean = true, maps: Boolean = true,
                listening: Boolean = true, fresh: Boolean = true, speed: Boolean = true, limit: LimitShown? = LimitShown.Known(50)) =
            SpeedRules.limitWhy(mode, file, connected, maps, listening, fresh, speed, limit)
        assertEquals(LimitWhy.SHOWN, why())
        assertEquals(LimitWhy.SOURCE_OFF, why(mode = SpeedSourceMode.OFF, file = LimitFile.NO_REGION))
        assertEquals(LimitWhy.CAR_ONLY, why(mode = SpeedSourceMode.CAR))
        assertEquals(LimitWhy.NO_REGION, why(file = LimitFile.NO_REGION, maps = false))
        assertEquals(LimitWhy.NO_FILE, why(file = LimitFile.NO_FILE))
        assertEquals(LimitWhy.NOT_CONNECTED, why(connected = false, maps = false))
        assertEquals(LimitWhy.NO_MAPS_APP, why(maps = false, listening = false))
        assertEquals(LimitWhy.NO_PERMISSION, why(listening = false, fresh = false))
        assertEquals(LimitWhy.NO_FIX, why(fresh = false, limit = null))
        assertEquals(LimitWhy.NO_SPEED, why(speed = false))
        assertEquals(LimitWhy.NO_ROAD, why(limit = LimitShown.Unknown))
    }

    @Test fun theFeedSaysWhy() {
        val inputs = SpeedFeed.Inputs(SpeedSourceMode.AUTO, shown = true, mapsRunning = true, car = carAt(10f, now), limitsOn = true)
        // Without the location permission.
        val denied = SpeedFeed(FakeFixes(permitted = false), FakeLimits()) { now }
        denied.update(inputs)
        assertEquals(LimitWhy.NO_PERMISSION, denied.why)
        // No region chosen, or no file.
        val phone = FakeFixes()
        val limits = FakeLimits()
        val feed = SpeedFeed(phone, limits) { now }
        feed.update(inputs.copy(limitsOn = false, limitFile = LimitFile.NO_REGION))
        assertEquals(LimitWhy.NO_REGION, feed.why)
        feed.update(inputs.copy(limitsOn = false, limitFile = LimitFile.NO_FILE))
        assertEquals(LimitWhy.NO_FILE, feed.why)
        // With a file: waiting for a fix, then the answer.
        feed.update(inputs)
        assertEquals(LimitWhy.NO_FIX, feed.why)
        phone.onFix!!(fix(10f, now))
        assertEquals(LimitWhy.NO_ROAD, feed.why)
        limits.answer(LimitShown.Known(50))
        assertEquals(LimitWhy.SHOWN, feed.why)
        feed.update(inputs.copy(mode = SpeedSourceMode.CAR))
        assertEquals(LimitWhy.CAR_ONLY, feed.why)
        feed.release()
        assertNull(feed.why)
        // No limit lookup at all (the phone's picture): no reason either.
        val none = SpeedFeed(FakeFixes()) { now }
        none.update(inputs)
        assertNull(none.why)
    }
}
