package net.prototux.aare.host

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MyPlayNightTest {
    @Test fun followingTheCarByDayIsDay() = assertFalse(MyPlayNight.forMaps(screenNight = true, carNight = false, followCar = true))

    @Test fun followingTheCarByNightIsNight() = assertTrue(MyPlayNight.forMaps(screenNight = false, carNight = true, followCar = true))

    /** Before the car has said anything, maps start like the screen: dark by default, no flash of light. */
    @Test fun noSignalYetIsLikeTheScreen() = assertTrue(MyPlayNight.forMaps(screenNight = true, carNight = null, followCar = true))

    @Test fun notFollowingIsLikeTheScreen() {
        assertTrue(MyPlayNight.forMaps(screenNight = true, carNight = false, followCar = false))
        assertFalse(MyPlayNight.forMaps(screenNight = false, carNight = true, followCar = false))
    }
}
