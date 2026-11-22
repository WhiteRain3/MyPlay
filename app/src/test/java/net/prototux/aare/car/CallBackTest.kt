package net.prototux.aare.car

import android.provider.CallLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** MyPlay (#10): which missed call "Call back the last missed call" offers. */
class CallBackTest {
    private val now = 100L * 60 * 60 * 1000
    private fun row(number: String, type: Int, minutesAgo: Long, name: String = "") = CallBack.Row(number, name, type, now - minutesAgo * 60_000)

    @Test fun theNewestMissedCall() {
        val rows = listOf(
            row("+3701", CallLog.Calls.MISSED_TYPE, 5, "Anna"),
            row("+3702", CallLog.Calls.MISSED_TYPE, 30),
        )
        assertEquals("+3701", CallBack.pick(rows, now)?.number)
    }

    @Test fun aNumberCalledAfterItWasMissedIsDealtWith() {
        val rows = listOf(
            row("+3701", CallLog.Calls.OUTGOING_TYPE, 2),
            row("+3701", CallLog.Calls.MISSED_TYPE, 5),
            row("+3702", CallLog.Calls.MISSED_TYPE, 30),
        )
        assertEquals("+3702", CallBack.pick(rows, now)?.number)
    }

    @Test fun withheldNumbersAreSkipped() {
        val rows = listOf(row("", CallLog.Calls.MISSED_TYPE, 1), row("+3702", CallLog.Calls.MISSED_TYPE, 3))
        assertEquals("+3702", CallBack.pick(rows, now)?.number)
    }

    @Test fun nothingOlderThanADay() {
        assertNull(CallBack.pick(listOf(row("+3701", CallLog.Calls.MISSED_TYPE, 25 * 60)), now))
        assertNull(CallBack.pick(listOf(row("+3701", CallLog.Calls.INCOMING_TYPE, 5)), now))
        assertNull(CallBack.pick(emptyList(), now))
    }
}
