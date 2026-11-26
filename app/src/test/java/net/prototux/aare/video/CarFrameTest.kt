package net.prototux.aare.video

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.util.Calendar

/** MyPlay (car screenshot): a frame's rows without their padding, and the file's name. */
class CarFrameTest {
    @Test fun rowPaddingIsDropped() {
        // 2 x 2 pixels of 4 bytes, rows 12 bytes apart (4 bytes of padding each).
        val src = ByteBuffer.wrap(byteArrayOf(
            1, 2, 3, 4, 5, 6, 7, 8, -1, -1, -1, -1,
            9, 10, 11, 12, 13, 14, 15, 16, -1, -1, -1, -1,
        ))
        val out = CarFrame.pack(src, rowStride = 12, pixelStride = 4, width = 2, height = 2)
        assertArrayEquals(ByteArray(16) { (it + 1).toByte() }, out.array())
        assertEquals(0, out.position())
        assertEquals("the image's own buffer is left as it was", 0, src.position())
    }

    @Test fun theLastRowNeedNotCarryItsPadding() {
        // Some planes end right after the last pixel.
        val src = ByteBuffer.wrap(byteArrayOf(1, 2, 3, 4, 0, 0, 0, 0, 5, 6, 7, 8))
        val out = CarFrame.pack(src, rowStride = 8, pixelStride = 4, width = 1, height = 2)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8), out.array())
    }

    @Test fun theFileIsNamedByItsLocalTime() {
        val at = Calendar.getInstance().apply { clear(); set(2026, Calendar.NOVEMBER, 26, 20, 30, 15) }.timeInMillis
        assertEquals("MyPlay-car-20261126-203015.png", CarScreenshot.fileName(at))
    }
}
