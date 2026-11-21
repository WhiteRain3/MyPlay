package net.prototux.aare.ui.car.myplay

import kotlin.math.abs

/**
 * MyPlay (plan phase N, N2): how the knob and the pad move the ring between the engine's stops.
 * The engine keeps which stop holds the ring itself (not Compose's focus), so a move is one pure
 * step, the same in the car and in tests.
 *
 *  - The knob goes through the stops in reading order (top to bottom, then left to right), one
 *    stop per click, wrapping round.
 *  - The pad goes to the nearest stop in its direction, like a TV remote; nothing that way, it stays.
 */
object FocusGraph {
    /** A stop's place on the screen, in any unit (pixels in the engine). */
    data class Box(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        val cx get() = (left + right) / 2
        val cy get() = (top + bottom) / 2
    }

    enum class Direction { UP, DOWN, LEFT, RIGHT }

    /**
     * The stops in reading order: rows from the top (stops whose heights overlap share a row, so
     * two slots resting on the bottom edge are one row even when one is taller), left to right
     * within a row.
     */
    fun readingOrder(stops: Map<String, Box>): List<String> {
        val rows = mutableListOf<MutableList<Map.Entry<String, Box>>>()
        var rowBottom = Float.NEGATIVE_INFINITY
        for (e in stops.entries.sortedBy { it.value.top }) {
            if (rows.isEmpty() || e.value.top >= rowBottom) {
                rows += mutableListOf(e)
                rowBottom = e.value.bottom
            } else {
                rows.last() += e
                rowBottom = maxOf(rowBottom, e.value.bottom)
            }
        }
        return rows.flatMap { row -> row.sortedBy { it.value.left }.map { it.key } }
    }

    /** The stop [steps] knob clicks from [current] (wrapping); the first stop when [current] is not one. */
    fun knob(stops: Map<String, Box>, current: String?, steps: Int): String? {
        val order = readingOrder(stops)
        if (order.isEmpty()) return null
        val i = order.indexOf(current)
        if (i < 0) return order.first()
        val n = order.size
        return order[((i + steps) % n + n) % n]
    }

    /**
     * The nearest stop from [current] in [dir]: only stops wholly that way (past [current]'s edge,
     * so a neighbour on the same row is never "up"), scored by the distance between middles, along
     * the direction counted once and across it twice (so a stop straight ahead wins over a nearer
     * one off to the side). Null when there is none; the first stop when [current] is not one.
     */
    fun pad(stops: Map<String, Box>, current: String?, dir: Direction): String? {
        val from = stops[current] ?: return readingOrder(stops).firstOrNull()
        return stops.filterKeys { it != current }.entries.mapNotNull { (id, b) ->
            val past = when (dir) {
                Direction.UP -> b.bottom <= from.top + EDGE_SLACK
                Direction.DOWN -> b.top >= from.bottom - EDGE_SLACK
                Direction.LEFT -> b.right <= from.left + EDGE_SLACK
                Direction.RIGHT -> b.left >= from.right - EDGE_SLACK
            }
            if (!past) return@mapNotNull null
            val (along, across) = when (dir) {
                Direction.UP, Direction.DOWN -> abs(b.cy - from.cy) to abs(b.cx - from.cx)
                Direction.LEFT, Direction.RIGHT -> abs(b.cx - from.cx) to abs(b.cy - from.cy)
            }
            id to (along + 2 * across)
        }.minByOrNull { it.second }?.first
    }

    /** Stops that touch or overlap by this much still count as past the edge. */
    private const val EDGE_SLACK = 1f
}
