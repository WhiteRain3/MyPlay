package net.prototux.aare.ui.car.myplay

import org.junit.Assert.assertEquals
import org.junit.Test

/** MyPlay (polish #5): the outside temperature under ORBIT's clock. */
class OutsideTempTest {
    @Test fun shownOnlyWhenTheCarReportsIt() {
        assertEquals(OutsideTemp(true, 3), OutsideTemp.of(warnings = true, carHasSensor = true, tempC = 3.4f))
        assertEquals(OutsideTemp(true, -2), OutsideTemp.of(true, carHasSensor = false, tempC = -2.4f))
    }

    @Test fun itsPlaceIsKeptWithoutAReadingWhenTheCarHasTheSensor() {
        assertEquals(OutsideTemp(true, null), OutsideTemp.of(true, carHasSensor = true, tempC = null))
        assertEquals(OutsideTemp(true, null), OutsideTemp.of(true, carHasSensor = true, tempC = Float.NaN))
    }

    @Test fun nothingWithoutTheSensorOrWithTheWarningsOff() {
        assertEquals(OutsideTemp.NONE, OutsideTemp.of(true, carHasSensor = false, tempC = null))
        assertEquals(OutsideTemp.NONE, OutsideTemp.of(warnings = false, carHasSensor = true, tempC = 3f))
    }
}
