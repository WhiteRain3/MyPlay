package net.prototux.aare.car

import android.content.Context
import android.provider.CallLog

/**
 * MyPlay (ideas 2026-11-22, #10): "Call back the last missed call", an action a button or the home
 * pad can be given (none has it by default). It never dials on its own: it shows the caller on the
 * car screen with a Call button, and only a push on that button places the call. Nothing about the
 * call goes in the log (no name, no number).
 */
internal object CallBack {
    /** A missed call older than this is not offered. */
    const val MAX_AGE_MS = 24L * 60 * 60 * 1000

    /** One call log row, newest first. */
    data class Row(val number: String, val name: String, val type: Int, val dateMs: Long)

    /**
     * The call to offer: the newest number whose own newest call is a missed one, within
     * [MAX_AGE_MS] of [nowMs]. A number called (or that called) after it was missed counts as dealt
     * with. Withheld numbers (blank) are skipped: nothing to call back.
     */
    fun pick(rowsNewestFirst: List<Row>, nowMs: Long): Row? {
        val seen = HashSet<String>()
        for (r in rowsNewestFirst) {
            if (r.number.isBlank()) continue
            if (!seen.add(r.number)) continue
            if (nowMs - r.dateMs > MAX_AGE_MS) return null
            if (r.type == CallLog.Calls.MISSED_TYPE) return r
        }
        return null
    }

    /** The newest call log rows (empty without the permission or on any error). Not on the main thread. */
    fun rows(context: Context, limit: Int = 50): List<Row> = runCatching {
        val out = ArrayList<Row>()
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE, CallLog.Calls.DATE),
            null, null, "${CallLog.Calls.DATE} DESC",
        )?.use { c ->
            while (c.moveToNext() && out.size < limit) {
                out += Row(c.getString(0).orEmpty(), c.getString(1).orEmpty(), c.getInt(2), c.getLong(3))
            }
        }
        out
    }.getOrDefault(emptyList())
}
