package net.prototux.aare.ui.phone

import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Taken unchanged from the owner's earlier project AA-Experiments (core/ImageBlur.kt, GPL-3.0),
 * for the wallpaper editor (plan F5).
 *
 * A Gaussian-like blur of ARGB pixels: three box blur passes in each direction, edges clamped. */
object ImageBlur {
    /** Returns a blurred copy of [pixels] ([width] x [height]). A [radius] of 0 or less returns the input values. */
    fun blur(pixels: IntArray, width: Int, height: Int, radius: Float): IntArray {
        require(width > 0 && height > 0 && pixels.size == width * height) { "pixel count does not match the size" }
        if (radius <= 0f) return pixels.copyOf()
        // Box width that three passes need to approximate a Gaussian whose spread is [radius].
        val boxRadius = ((sqrt(12f * radius * radius / 3f + 1f) - 1f) / 2f).roundToInt().coerceAtLeast(1)
        var current = pixels
        var scratch = IntArray(pixels.size)
        repeat(3) {
            boxPass(current, scratch, width, height, boxRadius, horizontal = true)
            val horizontalDone = scratch
            scratch = if (current === pixels) IntArray(pixels.size) else current
            boxPass(horizontalDone, scratch, width, height, boxRadius, horizontal = false)
            current = scratch
            scratch = horizontalDone
        }
        return current
    }

    private fun boxPass(src: IntArray, dst: IntArray, width: Int, height: Int, r: Int, horizontal: Boolean) {
        val length = if (horizontal) width else height
        val lines = if (horizontal) height else width
        val step = if (horizontal) 1 else width
        val size = 2 * r + 1
        for (line in 0 until lines) {
            val base = if (horizontal) line * width else line
            for (shift in intArrayOf(24, 16, 8, 0)) {
                fun value(i: Int) = (src[base + i.coerceIn(0, length - 1) * step] ushr shift) and 0xFF
                var sum = 0
                for (j in -r..r) sum += value(j)
                for (i in 0 until length) {
                    val out = (sum + size / 2) / size
                    val index = base + i * step
                    dst[index] = if (shift == 24) (out shl 24) else dst[index] or (out shl shift)
                    sum += value(i + r + 1) - value(i - r)
                }
            }
        }
    }
}
