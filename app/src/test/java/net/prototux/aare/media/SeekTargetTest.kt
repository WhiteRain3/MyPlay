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

    /** R5-4: two presses before the app reports the new position add up. */
    @Test fun quickPressesAddUp() {
        val first = nextSeek(null, 60_000, 240_000, SEEK_STEP_MS)
        val second = nextSeek(first, 60_000, 240_000, SEEK_STEP_MS)
        assertEquals(80_000L, second)
        assertEquals(70_000L, nextSeek(first, 60_000, 240_000, -SEEK_STEP_MS).let { nextSeek(it, 60_000, 240_000, SEEK_STEP_MS) })
    }
}
