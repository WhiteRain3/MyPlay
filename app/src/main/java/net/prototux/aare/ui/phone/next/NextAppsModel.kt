package net.prototux.aare.ui.phone.next

/** One entry of the car screen's app list: its order key (a package, or a built-in app's id) and name. */
data class AppKey(val key: String, val label: String)

/** [entries] in the car's order: by their place in [order] (app_order), the rest by name after (as CarRuntime). */
fun inCarOrder(entries: List<AppKey>, order: List<String>): List<AppKey> {
    val index = order.withIndex().associate { (i, k) -> k to i }
    return entries.sortedWith(compareBy({ index[it.key] ?: Int.MAX_VALUE }, { it.label.lowercase() }))
}

/**
 * app_order after moving the [i]th of [listed] (the keys as shown, in order) by [by] places. Keys not
 * listed keep their saved slots, so an app shown again comes back where it was: the listed keys fill
 * their old slots in the new order, new ones go at the end. The same rule as today's Apps page.
 */
fun movedOrder(order: List<String>, listed: List<String>, i: Int, by: Int): List<String> {
    val keys = listed.toMutableList()
    val j = (i + by).coerceIn(0, keys.lastIndex)
    keys.add(j, keys.removeAt(i))
    val queue = ArrayDeque(keys)
    val shown = keys.toSet()
    return order.mapNotNull { if (it in shown) queue.removeFirstOrNull() else it } + queue
}

/** hidden_apps after showing or hiding [key]. */
fun withHidden(hidden: Set<String>, key: String, visible: Boolean): Set<String> = if (visible) hidden - key else hidden + key
