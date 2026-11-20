package net.prototux.aare.ui.car

import net.prototux.aare.host.templates.MapGeometry.Box
import net.prototux.aare.ui.car.CarLayoutSpec.BarSide
import net.prototux.aare.ui.car.CarLayoutSpec.BarStyle
import net.prototux.aare.ui.car.CarLayoutSpec.CardPlace

/**
 * MyPlay (M3): where the bar, the map and the card slots of a [CarLayoutSpec] go on a screen of a
 * given size, in dp. Pure, so the rules can be tested; the screen file only draws what this says.
 *
 * Rules: the bar sits on its side (floating pills a gap away from the edge); cards sit only in
 * the area beside the bar ([content]), a gap from its edges and from each other, and never
 * overlap; the map fills the screen when it goes under the bar, else [content].
 */
internal object SpecPlacement {
    const val GAP = 16f
    const val BAR = 96f
    /** A ribbon holds one row of touch targets (76 dp) and a little room. */
    const val RIBBON = 88f
    const val CARD_W = 360f
    const val CARD_H = 200f
    /** Narrower than this, a card is not worth drawing: corners on the far side are left out. */
    const val MIN_CARD_W = 240f
    const val BANNER_H = 112f

    /**
     * M6: the screen's shape decides how big the cards are and how many there are. [SMALL] is the
     * Audi's (about 900 x 543 dp) and narrow or portrait screens: few, narrow cards. [VERY_WIDE]
     * (2.4 : 1 and wider) puts cards side by side so the map keeps the middle.
     */
    enum class Shape(val cardW: Float, val cardH: Float, val maxCards: Int) {
        SMALL(260f, 140f, 3), NORMAL(CARD_W, CARD_H, 4), WIDE(400f, CARD_H, 4), VERY_WIDE(400f, CARD_H, 4),
    }

    fun shapeOf(width: Float, height: Float): Shape = when {
        height < 600f || width < 1000f -> Shape.SMALL
        width / height >= 2.4f -> Shape.VERY_WIDE
        width / height >= 1.9f -> Shape.WIDE
        else -> Shape.NORMAL
    }

    data class Placement(
        val bar: Box,
        /** The screen minus the bar's strip: where an opened app and the cards go. */
        val content: Box,
        val map: Box,
        /** One rectangle per card, in the spec's card order (cards beyond the slots are left out). */
        val slots: List<Box>,
    )

    fun place(spec: CarLayoutSpec, width: Float, height: Float): Placement {
        val s = spec.allowed()
        val screen = Box(0f, 0f, width, height)
        val thick = if (s.barStyle == BarStyle.TOP_RIBBON) RIBBON else BAR
        val inset = if (s.barStyle == BarStyle.FLOATING_PILLS) GAP else 0f
        val strip = thick + inset
        val bar = when (s.barSide) {
            BarSide.LEFT -> Box(inset, inset, inset + thick, height - inset)
            BarSide.RIGHT -> Box(width - inset - thick, inset, width - inset, height - inset)
            BarSide.BOTTOM -> Box(inset, height - inset - thick, width - inset, height - inset)
            BarSide.TOP -> Box(inset, inset, width - inset, inset + thick)
        }
        val content = when (s.barSide) {
            BarSide.LEFT -> Box(strip, 0f, width, height)
            BarSide.RIGHT -> Box(0f, 0f, width - strip, height)
            BarSide.BOTTOM -> Box(0f, 0f, width, height - strip)
            BarSide.TOP -> Box(0f, strip, width, height)
        }
        val map = if (s.mapUnderBar) screen else content
        val shape = shapeOf(width, height)
        // M6-1: divide the space for the cards that will be shown, not for every card in the spec.
        val count = minOf(s.cards.size, shape.maxCards)
        return Placement(bar, content, map, slots(s, content, shape, count).take(count))
    }

    /**
     * M3-3: the largest area beside the bar that no card slot covers (a gap from everything), for
     * the maps buttons when no maps app is shown.
     */
    fun freeArea(p: Placement): Box {
        val inner = Box(p.content.left + GAP, p.content.top + GAP, p.content.right - GAP, p.content.bottom - GAP)
        val padded = p.slots.map { Box(it.left - GAP, it.top - GAP, it.right + GAP, it.bottom + GAP) }
        return net.prototux.aare.host.templates.MapGeometry.unobstructed(inner, padded)
    }

    /**
     * M4: the order the knob goes through [boxes] (the bar and the card slots): reading order, top
     * to bottom, then left to right. Indices into [boxes].
     */
    fun readingOrder(boxes: List<Box>): List<Int> =
        boxes.indices.sortedWith(compareBy<Int>({ boxes[it].top }, { boxes[it].left }))

    private fun slots(s: CarLayoutSpec, c: Box, shape: Shape, count: Int): List<Box> {
        val inner = Box(c.left + GAP, c.top + GAP, c.right - GAP, c.bottom - GAP)
        if (inner.width <= 0f || inner.height <= 0f) return emptyList()
        val n = count.coerceAtLeast(1)
        val side = s.barSide == BarSide.LEFT || s.barSide == BarSide.RIGHT
        // The "start" side is the bar's side for a side bar, the left otherwise.
        val startIsRight = s.barSide == BarSide.RIGHT
        val w = minOf(shape.cardW, inner.width * 0.4f)
        return when (s.cardPlace) {
            // Very wide: two columns side by side next to the bar, two cards each.
            CardPlace.COLUMN_BY_BAR -> if (side && shape == Shape.VERY_WIDE && n > 2) {
                val rows = (n + 1) / 2
                val h = (inner.height - (rows - 1) * GAP) / rows
                val x0 = if (startIsRight) inner.right - 2 * w - GAP else inner.left
                List(n) { i ->
                    val x = x0 + (i % 2) * (w + GAP)
                    val y = inner.top + (i / 2) * (h + GAP)
                    Box(x, y, x + w, y + h)
                }
            } else if (side) {
                val h = (inner.height - (n - 1) * GAP) / n
                val x = if (startIsRight) inner.right - w else inner.left
                List(n) { i -> Box(x, inner.top + i * (h + GAP), x + w, inner.top + i * (h + GAP) + h) }
            } else {
                val h = minOf(160f, inner.height * 0.3f)
                val cw = (inner.width - (n - 1) * GAP) / n
                val y = if (s.barSide == BarSide.BOTTOM) inner.bottom - h else inner.top
                List(n) { i -> Box(inner.left + i * (cw + GAP), y, inner.left + i * (cw + GAP) + cw, y + h) }
            }
            // Very wide: the four cards as a block of two by two at the far end, the map keeps the middle.
            CardPlace.CORNERS -> if (shape == Shape.VERY_WIDE) {
                val h = minOf(shape.cardH, (inner.height - GAP) / 2)
                val x0 = if (startIsRight) inner.left else inner.right - 2 * w - GAP
                listOf(
                    Box(x0, inner.top, x0 + w, inner.top + h),
                    Box(x0, inner.bottom - h, x0 + w, inner.bottom),
                    Box(x0 + w + GAP, inner.top, x0 + 2 * w + GAP, inner.top + h),
                    Box(x0 + w + GAP, inner.bottom - h, x0 + 2 * w + GAP, inner.bottom),
                )
            } else {
                val h = minOf(shape.cardH, (inner.height - GAP) / 2)
                // Too narrow for a card in each corner: only the two on the bar's side, wider.
                val both = w >= MIN_CARD_W
                val cw = if (both) w else minOf(shape.cardW, inner.width * 0.6f)
                val startL = if (startIsRight) inner.right - cw else inner.left
                val endL = if (startIsRight) inner.left else inner.right - cw
                listOf(
                    Box(startL, inner.top, startL + cw, inner.top + h),
                    Box(startL, inner.bottom - h, startL + cw, inner.bottom),
                    Box(endL, inner.top, endL + cw, inner.top + h),
                    Box(endL, inner.bottom - h, endL + cw, inner.bottom),
                ).let { if (both) it else it.take(2) }
            }
            CardPlace.STAGE -> {
                val sw = minOf(420f, inner.width * 0.45f)
                val x = if (startIsRight) inner.left else inner.right - sw
                listOf(Box(x, inner.top, x + sw, inner.bottom))
            }
            CardPlace.TOP_BANNER_AND_CORNER -> {
                val banner = Box(inner.left, inner.top, inner.right, inner.top + minOf(BANNER_H, inner.height * 0.3f))
                val below = inner.bottom - (banner.bottom + GAP)
                val h = minOf(120f, (below - 2 * GAP) / 3)
                val x = if (startIsRight) inner.right - w else inner.left
                listOf(banner) + List(3) { i ->
                    val bottom = inner.bottom - i * (h + GAP)
                    Box(x, bottom - h, x + w, bottom)
                }.filter { it.top >= banner.bottom + GAP }
            }
        }
    }
}
