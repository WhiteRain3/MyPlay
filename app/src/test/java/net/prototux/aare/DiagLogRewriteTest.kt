package net.prototux.aare

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** When the connection log's file is cut back to the kept lines (DiagLog.shouldRewrite). */
class DiagLogRewriteTest {
    private val limit = DiagLog.REWRITE_ABOVE_BYTES

    @Test fun notWhileTheLogIsNotFull() = assertFalse(DiagLog.shouldRewrite(false, limit * 10, 0))

    @Test fun notWhileTheFileIsSmall() = assertFalse(DiagLog.shouldRewrite(true, limit, 0))

    @Test fun firstCutOnceOverTheFixedSize() = assertTrue(DiagLog.shouldRewrite(true, limit + 1, 0))

    /** Long lines: the kept lines alone are over the fixed size. One more line must not cut again. */
    @Test fun noCutRightAfterACutEvenWhenTheKeptLinesAreLarge() {
        val kept = limit * 3
        assertFalse(DiagLog.shouldRewrite(true, kept + 600, kept))
        assertFalse(DiagLog.shouldRewrite(true, kept * 2, kept))
        assertTrue(DiagLog.shouldRewrite(true, kept * 2 + 1, kept))
    }

    /**
     * A full log of 1000 lines of [lineBytes] each, then [added] more lines: counts the cuts. With
     * the old rule (cut whenever full and over the fixed size) long lines gave one cut per line.
     */
    private fun cuts(lineBytes: Long, added: Int): Int {
        val kept = 1000 * lineBytes
        var file = kept
        var afterLast = 0L
        var cuts = 0
        repeat(added) {
            if (DiagLog.shouldRewrite(true, file, afterLast)) { file = kept; afterLast = file; cuts++ } else file += lineBytes
        }
        return cuts
    }

    @Test fun longLinesCutAboutOncePerThousandLinesNotOncePerLine() {
        // 2000-byte lines: the kept lines are about 2 MB, four times the fixed size.
        assertEquals(5, cuts(lineBytes = 2000, added = 5000))
        // 600-byte lines: just over the fixed size.
        assertTrue(cuts(lineBytes = 600, added = 5000) <= 6)
    }

    @Test fun shortLinesAreCutRarely() {
        // 80-byte lines: 80 KB kept; a cut only after the file passes 512 KB, about every 5400 lines.
        assertEquals(3, cuts(lineBytes = 80, added = 20000))
    }
}
