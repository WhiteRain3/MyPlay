package net.prototux.aare.host.templates

/**
 * Pure geometry for SurfaceCallback.onVisibleAreaChanged: the largest axis-aligned rectangle of
 * the map left free by the floating cards and strips.
 */
internal object MapGeometry {
    /** A rectangle in pixels; [right]/[bottom] exclusive like android.graphics.Rect. */
    data class Box(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        val width get() = right - left
        val height get() = bottom - top
        val area get() = if (width <= 0f || height <= 0f) 0f else width * height

        fun intersect(o: Box): Box? {
            val l = maxOf(left, o.left)
            val t = maxOf(top, o.top)
            val r = minOf(right, o.right)
            val b = minOf(bottom, o.bottom)
            return if (r > l && b > t) Box(l, t, r, b) else null
        }
    }

    /**
     * The largest rectangle of [map] that no obstacle overlaps. MyPlay (MA-1, the owner's log:
     * ORBIT's four corner parts made the greedy cut keep a thin full-width band, 1037x278 of
     * 1280x720, so Waze's arrow sat in a strip): an exact search over the rectangles whose edges
     * are the map's and the obstacles' edges (a largest free rectangle always has such edges).
     * There are only a few obstacles, so this is cheap. A free area much flatter (or taller) than the
     * map itself counts for less (by the square root of how far its shape is from the map's): the
     * app centres its arrow in it, and a strip leaves it no room. Ties keep the one nearer the middle.
     */
    fun unobstructed(map: Box, obstacles: List<Box>): Box {
        val obs = obstacles.mapNotNull { it.intersect(map) }
        if (obs.isEmpty()) return map
        if (obs.size > MAX_EXACT) return greedy(map, obs)
        val xs = (listOf(map.left, map.right) + obs.flatMap { listOf(it.left, it.right) }).distinct().sorted()
        val ys = (listOf(map.top, map.bottom) + obs.flatMap { listOf(it.top, it.bottom) }).distinct().sorted()
        val cx = (map.left + map.right) / 2
        val cy = (map.top + map.bottom) / 2
        val mapShape = map.height / map.width
        var best: Box? = null
        var bestArea = -1f
        var bestOff = Float.MAX_VALUE
        for (i in xs.indices) for (j in i + 1 until xs.size) for (k in ys.indices) for (l in k + 1 until ys.size) {
            val r = Box(xs[i], ys[k], xs[j], ys[l])
            if (r.area <= 0f) continue
            val f = (r.height / r.width) / mapShape
            val area = r.area * kotlin.math.sqrt(minOf(f, 1f / f))
            if (area < bestArea) continue
            // MA-3: an overlap test with no allocation.
            if (obs.any { o -> o.left < r.right && r.left < o.right && o.top < r.bottom && r.top < o.bottom }) continue
            val off = kotlin.math.abs((r.left + r.right) / 2 - cx) + kotlin.math.abs((r.top + r.bottom) / 2 - cy)
            if (area > bestArea || off < bestOff) { best = r; bestArea = area; bestOff = off }
        }
        return best ?: greedy(map, obs)
    }

    /**
     * MC-1 (the owner: "the map's free area centred", for ORBIT): the free area as a rectangle
     * centred on [map]'s middle, so the app's arrow sits in the middle of the screen. Its half
     * height is at most the map's less a [margin] (a share of the height) at the top and the bottom;
     * its half width is the distance from the middle to the nearest [hard] part beside the span it
     * covers, less a [gutter]. It is never flatter than [minAspect] (width over height): the tallest
     * such rectangle is kept. A part that crosses the middle line limits the height instead (it is
     * not beside the span but in it). Only the parts given count: text the map may lie under (the
     * song's title) is left out by the caller. Pure.
     */
    fun centred(map: Box, hard: List<Box>, gutter: Float, margin: Float = CENTRED_MARGIN, minAspect: Float = CENTRED_MIN_ASPECT): Box {
        val cx = (map.left + map.right) / 2
        val cy = (map.top + map.bottom) / 2
        val hhMax = map.height / 2 - map.height * margin
        if (map.width <= 0f || hhMax <= 0f) return map
        // Each part, grown by the gutter: from which half height it is level with the span, and the
        // half width it then leaves (0 when it crosses the middle line: the span must stop short of it).
        class Part(val from: Float, val room: Float)
        val parts = hard.mapNotNull { it.intersect(map) }.map { o ->
            val from = when {
                o.top - gutter >= cy -> o.top - gutter - cy
                o.bottom + gutter <= cy -> cy - (o.bottom + gutter)
                else -> 0f
            }
            val room = when {
                o.left >= cx -> o.left - gutter - cx
                o.right <= cx -> cx - (o.right + gutter)
                else -> 0f
            }.coerceAtLeast(0f)
            Part(from, room)
        }.sortedBy { it.from }
        val wide = map.width / 2
        // The half width is a step function of the half height (it narrows each time a part comes
        // level): in each step the tallest half height that keeps the shape is min(the step's end,
        // the half width / minAspect); the tallest over the steps is the answer.
        var bestHh = 0f
        var bestHw = 0f
        var room = wide
        var start = 0f
        var i = 0
        while (start < hhMax) {
            while (i < parts.size && parts[i].from <= start) { room = minOf(room, parts[i].room); i++ }
            val end = if (i < parts.size) minOf(parts[i].from, hhMax) else hhMax
            val hh = minOf(end, room / minAspect)
            if (hh > start && hh > bestHh && room > 0f) { bestHh = hh; bestHw = room }
            if (room <= 0f) break
            start = end
        }
        if (bestHh <= 0f || bestHw <= 0f) return unobstructed(map, hard)
        return Box(cx - bestHw, cy - bestHh, cx + bestHw, cy + bestHh)
    }

    /** MC-1: the share of the height kept free at the top and at the bottom. */
    const val CENTRED_MARGIN = 0.06f

    /** MC-1: the centred area is at least this wide for its height (the app needs width). */
    const val CENTRED_MIN_ASPECT = 0.9f

    /** MC-1: the gap kept between the centred area and a part: this share of the width, or more. */
    const val CENTRED_GUTTER = 0.025f

    /** MC-1: the smallest gap, in dp. */
    const val CENTRED_MIN_GUTTER_DP = 16

    /** More obstacles than this: the greedy cut below (never on MyPlay's screens). */
    private const val MAX_EXACT = 10

    /**
     * Greedy cut: for each obstacle (largest first) keep the biggest of the four slices of the
     * remaining area beside it. Cards hug the screen edges, so this finds the true free area for
     * the layouts we draw (start column + end strips, or top/bottom cards in portrait).
     */
    private fun greedy(map: Box, obstacles: List<Box>): Box {
        var r = map
        val sorted = obstacles.mapNotNull { it.intersect(map) }.sortedByDescending { it.area }
        for (o in sorted) {
            val i = o.intersect(r) ?: continue
            r = listOf(
                Box(i.right, r.top, r.right, r.bottom), // keep what is right of it
                Box(r.left, r.top, i.left, r.bottom), // left of it
                Box(r.left, i.bottom, r.right, r.bottom), // below it
                Box(r.left, r.top, r.right, i.top), // above it
            ).maxBy { it.area }
        }
        return r
    }
}
