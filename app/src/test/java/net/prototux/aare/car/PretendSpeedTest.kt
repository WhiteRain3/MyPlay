package net.prototux.aare.car

import android.os.Handler
import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/** MyPlay (Preview): the pretend speed is a car of its own, fresh while set, and never CarData. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PretendSpeedTest {
    private var now = 1_000_000L
    private val pretend = PretendSpeed(Handler(Looper.getMainLooper())) { now }

    @Test fun noSpeedIsNoCar() {
        assertFalse(pretend.car.value.connected)
        assertNull(SpeedRules.carSpeed(pretend.car.value, now))
    }

    @Test fun aSpeedIsTheCarsSpeedForTheSpeedRules() {
        pretend.set(50)
        assertTrue(pretend.car.value.connected)
        val mps = SpeedRules.carSpeed(pretend.car.value, now)
        assertNotNull(mps)
        assertEquals(50, SpeedRules.kmh(mps!!))
    }

    @Test fun itIsSentAgainSoItNeverGoesStale() {
        pretend.set(90)
        now += 10_000
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(PretendSpeed.REFRESH_MS))
        assertEquals(90, SpeedRules.kmh(SpeedRules.carSpeed(pretend.car.value, now)!!))
    }

    @Test fun stoppingEndsItAndItsRefresh() {
        pretend.set(30)
        pretend.stop()
        assertNull(pretend.kmh)
        assertFalse(pretend.car.value.connected)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(PretendSpeed.REFRESH_MS * 3))
        assertFalse(pretend.car.value.connected)
    }

    @Test fun anOddSpeedIsClamped() {
        pretend.set(400)
        assertEquals(PretendSpeed.MAX_KMH, pretend.kmh)
        pretend.set(-5)
        assertEquals(0, pretend.kmh)
    }

    @Test fun itNeverWritesTheRealCarData() {
        val before = CarData.state.value
        pretend.set(120)
        assertEquals(before, CarData.state.value)
    }
}
