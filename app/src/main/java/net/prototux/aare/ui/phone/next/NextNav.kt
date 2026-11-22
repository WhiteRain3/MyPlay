package net.prototux.aare.ui.phone.next

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Every page of the rebuilt phone app, by route (plan §3.3). A route belongs to exactly one tab: the
 * part before the first '/'. The design renamed `car/buttons/home-shortcuts` to `car/buttons/pad-shortcuts`.
 */
object NextRoutes {
    val ALL: List<String> = listOf(
        "home",
        "screen", "screen/design", "screen/looks", "screen/colours", "screen/colours/own",
        "screen/wallpaper", "screen/wallpaper/edit", "screen/map-colours", "screen/size",
        "car", "car/buttons", "car/buttons/pad-shortcuts", "car/buttons/timing", "car/buttons/teach",
        "car/maps", "car/speed", "car/sound", "car/messages", "car/voice",
        "apps", "apps/phone-apps", "apps/phone-screen", "apps/browser",
        "more", "more/updates", "more/whats-new", "more/cars", "more/connection", "more/backup",
        "more/diagnostics", "more/diagnostics/car-check", "more/diagnostics/car-data", "more/diagnostics/log",
        "more/advanced", "more/advanced/certificates", "more/advanced/developer",
        "more/archive", "more/archive/deck", "more/archive/layouts", "more/about",
    )

    /** One car's page: `more/cars/{car}`. */
    const val CAR_PREFIX = "more/cars/"

    fun car(id: String): String = CAR_PREFIX + id

    fun isKnown(route: String): Boolean =
        route in ALL || (route.startsWith(CAR_PREFIX) && route.length > CAR_PREFIX.length && '/' !in route.removePrefix(CAR_PREFIX))

    /** The tab that owns [route], or null for an unknown route. */
    fun tabOf(route: String): NextTab? {
        if (!isKnown(route)) return null
        val head = route.substringBefore('/')
        return NextTab.entries.firstOrNull { it.root == head }
    }

    /** The known routes from the tab root down to [route], e.g. more, more/advanced, more/advanced/certificates. */
    fun pathTo(route: String): List<String> {
        val parts = route.split('/')
        return parts.indices.map { parts.subList(0, it + 1).joinToString("/") }.filter { isKnown(it) }
    }
}

val NextTab.root: String get() = name.lowercase()

/** A page on a tab's stack. [returnTo] is set when a link from another tab opened it: Back goes there once. */
data class NavEntry(val route: String, val returnTo: NextTab? = null)

/**
 * Where the app is: the tab on screen and one back stack per tab (plan §3.4). Immutable; every move
 * returns a new state, so it can be kept in Compose state and saved as strings.
 */
data class NavState(
    val tab: NextTab = NextTab.HOME,
    val stacks: Map<NextTab, List<NavEntry>> = NextTab.entries.associateWith { listOf(NavEntry(it.root)) },
) {
    fun stack(t: NextTab = tab): List<NavEntry> = stacks[t] ?: listOf(NavEntry(t.root))
    val top: NavEntry get() = stack().last()
    val route: String get() = top.route

    private fun withStack(t: NextTab, s: List<NavEntry>) = copy(stacks = stacks + (t to s))

    /**
     * Open [route]. Inside the tab on screen it is pushed. A route of another tab switches to that tab and
     * pushes it there, remembering where the link was tapped. Unknown routes do nothing.
     */
    fun open(route: String): NavState {
        val owner = NextRoutes.tabOf(route) ?: return this
        if (route == owner.root) return selectTab(owner, resetIfCurrent = false)
        return if (owner == tab) {
            if (top.route == route) this else withStack(tab, stack() + NavEntry(route))
        } else {
            val s = stack(owner)
            if (s.last().route == route) copy(tab = owner)
            else withStack(owner, s + NavEntry(route, returnTo = tab)).copy(tab = owner)
        }
    }

    /**
     * Open [route] from outside the app (a notification, the `route` extra): switch to its tab and show it
     * with its parent pages under it. An unknown route opens Home.
     */
    fun deepLink(route: String): NavState {
        val owner = NextRoutes.tabOf(route) ?: return copy(tab = NextTab.HOME)
        val path = NextRoutes.pathTo(route)
        return withStack(owner, path.map { NavEntry(it) }).copy(tab = owner)
    }

    /**
     * A tab tapped. Another tab: switch, keeping both stacks. The tab on screen: back to its root
     * (and [atRoot] tells the caller to scroll to the top when it already was there).
     */
    fun selectTab(t: NextTab, resetIfCurrent: Boolean = true): NavState =
        if (t != tab) copy(tab = t)
        else if (resetIfCurrent || stack().size > 1) withStack(t, listOf(NavEntry(t.root)))
        else this

    val atRoot: Boolean get() = stack().size == 1

    /** Back. Returns null when the app should close (Back at Home's root). */
    fun back(): NavState? {
        val s = stack()
        val ret = s.last().returnTo
        return when {
            ret != null -> withStack(tab, s.dropLast(1)).copy(tab = ret)
            s.size > 1 -> withStack(tab, s.dropLast(1))
            tab != NextTab.HOME -> copy(tab = NextTab.HOME)
            else -> null
        }
    }

    /** Every route on any stack: scroll positions of other routes can be forgotten. */
    val liveRoutes: Set<String> get() = stacks.values.flatten().mapTo(HashSet()) { it.route }

    /** As strings, for saved instance state: the tab, then each tab's entries as "route|returnTo". */
    fun save(): ArrayList<String> {
        val out = arrayListOf(tab.name)
        for (t in NextTab.entries) {
            out += "#" + t.name
            for (e in stack(t)) out += e.route + "|" + (e.returnTo?.name ?: "")
        }
        return out
    }

    companion object {
        /** Reads [save]'s strings back. Anything it does not recognise falls back to a fresh state. */
        fun restore(saved: List<String>?): NavState {
            if (saved.isNullOrEmpty()) return NavState()
            val tab = NextTab.entries.firstOrNull { it.name == saved[0] } ?: return NavState()
            val stacks = NextTab.entries.associateWith { mutableListOf<NavEntry>() }.toMutableMap()
            var cur: NextTab? = null
            for (line in saved.drop(1)) {
                if (line.startsWith("#")) { cur = NextTab.entries.firstOrNull { it.name == line.drop(1) }; continue }
                val t = cur ?: continue
                val route = line.substringBefore('|')
                val ret = line.substringAfter('|', "").let { r -> NextTab.entries.firstOrNull { it.name == r } }
                if (NextRoutes.tabOf(route) == t) stacks[t]!!.add(NavEntry(route, ret))
            }
            val fixed = stacks.mapValues { (t, s) ->
                if (s.isEmpty() || s.first().route != t.root) listOf(NavEntry(t.root)) + s.filter { it.route != t.root } else s
            }
            return NavState(tab, fixed)
        }
    }
}

/**
 * Opening a page from outside the app (plan §3.5). MainActivity is exported (it is the launcher), so any
 * app could send it a `route`; the extra counts only when the intent came through the non-exported
 * alias [ALIAS], which only MyPlay's own PendingIntents can target. Opening a page never changes
 * anything by itself: every change needs a tap.
 */
object NextDeepLink {
    const val EXTRA_ROUTE = "route"
    const val ALIAS = "net.prototux.aare.ui.OpenPage"

    /** An intent that opens [route], for a notification's PendingIntent. */
    fun intent(context: Context, route: String): Intent =
        Intent().setComponent(ComponentName(context.packageName, ALIAS))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_ROUTE, route)

    /**
     * A notification's tap: opens [route] (plan §3.5). Each notification has its own [requestCode] so
     * their taps don't collapse into one PendingIntent.
     */
    fun pendingIntent(context: Context, route: String, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(context, requestCode, intent(context, route), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    /** Request codes of the notifications that open a page. */
    const val CODE_STATUS = 100
    const val CODE_TRUST = 101
    const val CODE_CERT_NOTICE = 102
    const val CODE_CERT_EXPIRY = 103

    /** The route [intent] asks for, or null when it has none or did not come through [ALIAS]. */
    fun routeOf(intent: Intent?): String? {
        if (intent?.component?.className != ALIAS) return null
        return intent.getStringExtra(EXTRA_ROUTE)
    }
}
