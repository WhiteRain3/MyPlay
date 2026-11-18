package net.prototux.aare.headunit

import aap_protobuf.service.media.shared.message.InsetsOuterClass.Insets
import aap_protobuf.service.media.shared.message.UiConfigOuterClass.UiConfig
import aap_protobuf.service.media.sink.message.VideoConfigurationOuterClass.VideoConfiguration
import java.awt.BasicStroke
import java.awt.Color
import java.awt.image.BufferedImage

/**
 * MyPlay: margins for the desktop head unit, to imitate a car that shows only part of the video
 * frame (--width-margin, --height-margin, --margins).
 *
 * A head unit can tell the phone about unused edges in two ways, and this file offers both:
 *  - the plain numbers width_margin / height_margin, which the phone splits evenly between the two
 *    opposite sides;
 *  - UiConfig.margins, one number per side, which replace the plain numbers when present.
 * That is the rule the app follows (app: car/ProjectionLayout.kt, docs/decisions.md "Projection
 * margins"); [visibleArea] repeats it so the pictures can mark the part a car would not show.
 */

/** Margins per side, in video pixels (what --margins L,T,R,B gives). */
data class SideMargins(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    companion object {
        /** "L,T,R,B": four whole numbers, none negative. Null for anything else. */
        fun parse(text: String): SideMargins? {
            val n = text.split(',').map { it.trim().toIntOrNull() ?: return null }
            if (n.size != 4 || n.any { it < 0 }) return null
            return SideMargins(n[0], n[1], n[2], n[3])
        }
    }
}

/** The part of a frame (or of the screen) the car shows. */
data class VisibleArea(val left: Int, val top: Int, val width: Int, val height: Int) {
    val right get() = left + width
    val bottom get() = top + height
}

/** The main display's video configuration with this head unit's margins. The cluster display has none. */
internal fun VideoConfiguration.Builder.withMargins(options: HeadUnit.Options): VideoConfiguration.Builder {
    setWidthMargin(options.widthMargin).setHeightMargin(options.heightMargin)
    options.margins?.let { m ->
        setUiConfig(UiConfig.newBuilder().setMargins(
            Insets.newBuilder().setLeft(m.left).setTop(m.top).setRight(m.right).setBottom(m.bottom)))
    }
    return this
}

/** True when any margin is set, so pictures need marking and touches need moving. */
internal val HeadUnit.Options.hasMargins: Boolean
    get() = widthMargin > 0 || heightMargin > 0 || margins?.let { it.left + it.top + it.right + it.bottom > 0 } == true

/**
 * What stays visible of a [width] x [height] frame, by the phone's rule: per-side margins replace
 * the plain ones; plain ones are split evenly (the odd pixel goes to the right / bottom). At least
 * one pixel always stays visible.
 */
internal fun visibleArea(options: HeadUnit.Options, width: Int, height: Int): VisibleArea {
    fun pair(total: Int, a: Int, b: Int): Pair<Int, Int> {
        val first = a.coerceIn(0, (total - 1).coerceAtLeast(0))
        return first to b.coerceIn(0, (total - 1 - first).coerceAtLeast(0))
    }
    val m = options.margins
    val (l, r) = if (m != null) pair(width, m.left, m.right)
        else options.widthMargin.coerceIn(0, (width - 1).coerceAtLeast(0)).let { it / 2 to it - it / 2 }
    val (t, b) = if (m != null) pair(height, m.top, m.bottom)
        else options.heightMargin.coerceIn(0, (height - 1).coerceAtLeast(0)).let { it / 2 to it - it / 2 }
    return VisibleArea(l, t, width - l - r, height - t - b)
}

/** Why these margins cannot be used with a [width] x [height] frame, or null when they can. */
internal fun marginProblem(options: HeadUnit.Options, width: Int, height: Int): String? {
    val m = options.margins
    return when {
        options.widthMargin < 0 || options.heightMargin < 0 -> "margins cannot be negative"
        options.widthMargin >= width || options.heightMargin >= height -> "the margins leave nothing of the ${width}x$height picture"
        m != null && (m.left + m.right >= width || m.top + m.bottom >= height) -> "the margins leave nothing of the ${width}x$height picture"
        else -> null
    }
}

/**
 * A copy of [frame] with the part the car would not show darkened and a line around the visible
 * part. Returns [frame] itself when there are no margins.
 */
internal fun markHidden(frame: BufferedImage, options: HeadUnit.Options): BufferedImage {
    if (!options.hasMargins) return frame
    val v = visibleArea(options, frame.width, frame.height)
    val out = BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_RGB)
    val g = out.createGraphics()
    try {
        g.drawImage(frame, 0, 0, null)
        g.color = Color(0, 0, 0, 170)
        g.fillRect(0, 0, frame.width, v.top)                                  // above
        g.fillRect(0, v.bottom, frame.width, frame.height - v.bottom)         // below
        g.fillRect(0, v.top, v.left, v.height)                                // left
        g.fillRect(v.right, v.top, frame.width - v.right, v.height)           // right
        g.color = Color(255, 64, 160)
        g.stroke = BasicStroke(1f)
        g.drawRect(v.left, v.top, v.width - 1, v.height - 1)
    } finally {
        g.dispose()
    }
    return out
}

/**
 * A touch at screen position [x], [y] as the touch panel reports it. The phone takes the panel to
 * cover the visible part only (app: car/TouchMapper.kt), so with margins the panel is declared with
 * the visible size and positions are counted from its corner. Positions outside are moved to its edge.
 */
internal fun toPanel(options: HeadUnit.Options, x: Int, y: Int): Pair<Int, Int> {
    val v = visibleArea(options, options.width, options.height)
    return (x - v.left).coerceIn(0, v.width - 1) to (y - v.top).coerceIn(0, v.height - 1)
}
