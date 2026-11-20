package net.prototux.aare.media

import org.junit.Assert.assertEquals
import org.junit.Test

/** MyPlay (R5): left and right on the music progress bar. */
class SeekTargetTest {
    @Test fun tenSecondsEachWay() {
        assertEquals(70_000L, seekTarget(60_000, 240_000, SEEK_STEP_MS))
        assertEquals(50_000L, seekTarget(60_000, 240_000, -SEEK_STEP_MS))
    }

    @Test fun keptInsideTheSong() {
        assertEquals(0L, seekTarget(4_000, 240_000, -SEEK_STEP_MS))
        assertEquals(240_000L, seekTarget(235_000, 240_000, SEEK_STEP_MS))
    }

    @Test fun noLengthKnownOnlyStopsAtTheStart() {
        assertEquals(15_000L, seekTarget(5_000, 0, SEEK_STEP_MS))
        assertEquals(0L, seekTarget(5_000, 0, -SEEK_STEP_MS))
    }
}
