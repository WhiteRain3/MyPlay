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
            if (obs.any { it.intersect(r) != null }) continue
            val off = kotlin.math.abs((r.left + r.right) / 2 - cx) + kotlin.math.abs((r.top + r.bottom) / 2 - cy)
            if (area > bestArea || off < bestOff) { best = r; bestArea = area; bestOff = off }
        }
        return best ?: greedy(map, obs)
    }

    /** More obstacles than this: the greedy cut below (never on MyPlay's screens). */
    private const val MAX_EXACT = 16

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
