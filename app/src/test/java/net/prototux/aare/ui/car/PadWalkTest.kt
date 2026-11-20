package net.prototux.aare.ui.car

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.withKeyDown
import net.prototux.aare.ui.theme.CarTheme
import net.prototux.aare.ui.theme.CarThemes
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * MyPlay (plan R4): the pad on the knob works like a TV remote on every car screen. Starting from
 * each item that can hold the ring, each of the four directions is pressed and where the ring lands
 * is recorded. Then, using only the pad:
 *  - every item can be reached from where the screen starts (nothing is out of reach), and
 *  - from every item, the start can be reached again (nothing traps the ring).
 * Drawn by Compose on Robolectric, at the size of a common head unit and of the owner's Audi.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalTestApi::class)
class PadWalkTest {
    @get:Rule val rule = createComposeRule()

    private val directions = listOf(Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight)

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun taskbarHome() = walk(CarLayout.TASKBAR)
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun dockHome() = walk(CarLayout.DOCK)
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun classicHome() = walk(CarLayout.CLASSIC)
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun mapBehindHome() = walk(CarLayout.MAP_BEHIND)
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun mapBehindHomeAudi() = walk(CarLayout.MAP_BEHIND)
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun taskbarHomeAudi() = walk(CarLayout.TASKBAR)
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun launcher() = walk(CarLayout.TASKBAR, CarScreen.Launcher)
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun launcherAudi() = walk(CarLayout.MAP_BEHIND, CarScreen.Launcher)

    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun settingsScreen() = walk(CarLayout.TASKBAR, CarScreen.App("settings"))
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun settingsScreenAudi() = walk(CarLayout.MAP_BEHIND, CarScreen.App("settings"))
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun notificationsScreenAudi() = walk(CarLayout.MAP_BEHIND, CarScreen.App("notifications"))
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun dockHomeAudi() = walk(CarLayout.DOCK)
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun classicHomeAudi() = walk(CarLayout.CLASSIC)

    // R4-2: the knob (Tab and Shift+Tab, one per click) visits every item, both ways.
    @Test @Config(qualifiers = "w1280dp-h720dp-land-mdpi") fun knobTaskbarHome() = knob(CarLayout.TASKBAR)
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun knobMapBehindHomeAudi() = knob(CarLayout.MAP_BEHIND)
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun knobDockHomeAudi() = knob(CarLayout.DOCK)
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun knobClassicHomeAudi() = knob(CarLayout.CLASSIC)
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun knobLauncherAudi() = knob(CarLayout.MAP_BEHIND, CarScreen.Launcher)
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun knobSettingsAudi() = knob(CarLayout.MAP_BEHIND, CarScreen.App("settings"))

    /**
     * R4-2: the "map behind" music card's inside mode (M3-6). The card is one stop; the knob's
     * press goes inside, the ring on its first button; turning goes through its three buttons and
     * then leaves the card, whose buttons are no stops again; turning back onto it lands on the
     * card, not on a button.
     */
    @Test @Config(qualifiers = "w901dp-h543dp-land-mdpi") fun knobMusicCardInsideAudi() {
        show(CarLayout.MAP_BEHIND, CarScreen.Dashboard)
        val res = org.robolectric.RuntimeEnvironment.getApplication().resources
        val previous = res.getString(net.prototux.aare.R.string.media_action_previous)
        val playPause = res.getString(net.prototux.aare.R.string.media_action_play_pause)
        val next = res.getString(net.prototux.aare.R.string.media_action_next)
        val buttons = setOf(previous, playPause, next)
        fun focusedLabel() = rule.onAllNodes(isFocused()).fetchSemanticsNodes().firstOrNull()?.let { label(it) }
        fun turn(back: Boolean = false) {
            rule.onNode(isFocused()).performKeyInput { if (back) withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } else pressKey(Key.Tab) }
            settle()
        }
        // A button that can hold the ring now (trying puts the ring there: put it back after).
        fun buttonStops(): List<String> {
            val back = focusedId()
            val held = focusables().filter { label(it) in buttons }.filter { focus(it.id) }.map { label(it) }
            back?.let { focus(it) }
            return held
        }
        // Outside, the buttons are no stops: only the card is.
        assertTrue("buttons are stops before the card is pressed: ${buttonStops()}", buttonStops().isEmpty())
        val card = focusables().firstOrNull { label(it).contains("Night Drive") } ?: focusables().first { n ->
            n.config.getOrNull(SemanticsProperties.Text).orEmpty().any { it.text.contains("Night Drive") } ||
                n.children.any { c -> c.config.getOrNull(SemanticsProperties.Text).orEmpty().any { it.text.contains("Night Drive") } }
        }
        assertTrue("the music card cannot hold the ring", focus(card.id))
        rule.onNode(isFocused()).performKeyInput { pressKey(Key.Enter) }
        settle()
        assertTrue("pressed: the ring is not on the first button but on ${focusedLabel()}", focusedLabel() == previous)
        turn(); assertTrue("second stop is ${focusedLabel()}", focusedLabel() == playPause)
        turn(); assertTrue("third stop is ${focusedLabel()}", focusedLabel() == next)
        turn()
        val after = focusedLabel()
        assertTrue("turning past the last button stays in the card: $after", after != null && after !in buttons && focusedId() != card.id)
        settle()
        assertTrue("left the card, its buttons are still stops: ${buttonStops()}", buttonStops().isEmpty())
        // Back onto the card: the card itself, not a button.
        var clicks = 0
        while (focusedId() != card.id && clicks++ < MAX_KNOB_CLICKS) {
            turn(back = true)
            assertTrue("turning back reached a button without a press: ${focusedLabel()}", focusedLabel() !in buttons)
        }
        assertTrue("turning back never reaches the card", focusedId() == card.id)
    }

    private fun knob(layout: CarLayout, screen: CarScreen = CarScreen.Dashboard) {
        show(layout, screen)
        val holdable = focusables().filter { focus(it.id) }
        assertTrue("$layout $screen: nothing can hold the ring", holdable.isNotEmpty())
        // Rows of a long list are made anew as they scroll back into view, with new ids: the knob's
        // walk compares items by what they say. R4-4: items that say the same (two "Settings", two
        // rows of one song) are counted, so one of them out of reach is not hidden by its twin:
        // each label must be reached on as many different items as there are. (A row that
        // scrolled away and back counts again under its new id, so a long list of twins is
        // checked less strictly than a screen that does not scroll.)
        val want = holdable.groupingBy { label(it) }.eachCount()
        for (back in listOf(false, true)) {
            focus(startId(holdable))
            val seen = HashMap<String, MutableSet<Int>>()
            fun look() {
                rule.onAllNodes(isFocused()).fetchSemanticsNodes().firstOrNull()?.let { seen.getOrPut(label(it)) { HashSet() } += it.id }
            }
            fun missing() = want.filter { (l, n) -> (seen[l]?.size ?: 0) < n }
            look()
            // Turned back from the first item, a long list wraps to its end first: keep turning
            // until every item was seen, or a full lap of even the longest screen.
            var clicks = 0
            while (missing().isNotEmpty() && clicks++ < MAX_KNOB_CLICKS) {
                rule.onNode(isFocused()).performKeyInput {
                    if (back) withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } else pressKey(Key.Tab)
                }
                settle()
                look()
            }
            val missed = missing().map { (l, n) -> if (n > 1) "$l (${seen[l]?.size ?: 0} of $n)" else l }
            assertTrue("$layout $screen: the knob turned ${if (back) "back" else "on"} never reaches: $missed", missed.isEmpty())
        }
    }

    /** What an item says (its description or text, its own or its first child's), with its place in a row of the same. */
    private fun label(n: SemanticsNode): String =
        n.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
            ?: n.config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text
            ?: n.children.firstNotNullOfOrNull { c -> c.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull() ?: c.config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text }
            ?: "? at ${n.boundsInRoot.left.toInt()},${n.boundsInRoot.top.toInt()}"

    private fun show(layout: CarLayout, screen: CarScreen) {
        val state = PadWalkStates.state(layout, screen)
        val settings = net.prototux.aare.Settings(org.robolectric.RuntimeEnvironment.getApplication())
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CarTheme(night = state.night, theme = state.theme) {
                CarShell(state, PadWalkStates.NoActions, appContent = { app, m ->
                    when (app.kind) {
                        CarAppKind.SETTINGS -> CarSettingsScreen(settings, m, state.apps.filter { it.kind == CarAppKind.NAVIGATION })
                        CarAppKind.NOTIFICATIONS -> NotificationsScreen(state, m)
                        else -> Box(m.background(Color.DarkGray))
                    }
                }, navMap = { _, m -> Box(m.background(Color.DarkGray)) })
            }
        }
        CarFocusVisibility.keyInput = true
        // Past the screen's own first-focus requests (FocusSettle gives up after 4 s).
        rule.mainClock.advanceTimeBy(FocusSettle.GIVE_UP_AFTER_MS + 1_000)
        settle()
        focusedIdAtStart = focusedId()
    }

    private fun walk(layout: CarLayout, screen: CarScreen = CarScreen.Dashboard) {
        show(layout, screen)
        val nodes = focusables()
        assertTrue("$layout $screen: nothing can hold the ring", nodes.isNotEmpty())
        // Where each direction leads from each item.
        val edges = HashMap<Int, MutableSet<Int>>()
        for (n in nodes) {
            for (d in directions) {
                if (!focus(n.id)) continue
                rule.onNode(isFocused()).performKeyInput { pressKey(d) }
                settle()
                val now = focusedId() ?: continue
                if (now != n.id) edges.getOrPut(n.id) { HashSet() } += now
            }
        }
        // Items that cannot take the ring now (a card's buttons before the card is pressed) are not counted.
        val holdable = nodes.filter { focus(it.id) }
        val start = startId(holdable)
        val reach = reachable(start, edges)
        val back = reachable(start, reverse(edges))
        val ids = holdable.map { it.id }.toSet()
        val unreachable = ids - reach
        val traps = ids - back
        val moves = { holdable.joinToString("\n") { n -> "  ${describe(setOf(n.id), nodes)} -> " + edges[n.id].orEmpty().joinToString { describe(setOf(it), nodes) } } }
        assertTrue("$layout $screen: out of reach with the pad: ${describe(unreachable, nodes)}\nmoves:\n${moves()}", unreachable.isEmpty())
        assertTrue("$layout $screen: the ring cannot get back from: ${describe(traps, nodes)}\nmoves:\n${moves()}", traps.isEmpty())
    }

    private companion object {
        const val MAX_KNOB_CLICKS = 400
    }

    private fun settle() {
        repeat(6) { rule.mainClock.advanceTimeBy(100) }
    }

    private fun focusables(): List<SemanticsNode> =
        rule.onAllNodes(isFocusable() and SemanticsMatcher("visible") { it.boundsInRoot.width > 0f && it.boundsInRoot.height > 0f })
            .fetchSemanticsNodes()

    private fun focus(id: Int): Boolean = runCatching {
        rule.onNode(SemanticsMatcher("id $id") { it.id == id }).requestFocus()
        settle()
        focusedId() == id
    }.getOrDefault(false)

    private fun focusedId(): Int? = rule.onAllNodes(isFocused()).fetchSemanticsNodes().firstOrNull()?.id

    /** Where the screen puts the ring first, or the first item if none. */
    private fun startId(nodes: List<SemanticsNode>): Int = focusedIdAtStart ?: nodes.first().id
    private var focusedIdAtStart: Int? = null

    private fun reachable(from: Int, edges: Map<Int, Set<Int>>): Set<Int> {
        val seen = hashSetOf(from)
        val todo = ArrayDeque(listOf(from))
        while (todo.isNotEmpty()) for (n in edges[todo.removeFirst()].orEmpty()) if (seen.add(n)) todo += n
        return seen
    }

    private fun reverse(edges: Map<Int, Set<Int>>): Map<Int, Set<Int>> {
        val r = HashMap<Int, MutableSet<Int>>()
        for ((a, bs) in edges) for (b in bs) r.getOrPut(b) { HashSet() } += a
        return r
    }

    private fun describe(ids: Set<Int>, nodes: List<SemanticsNode>): String = nodes.filter { it.id in ids }.joinToString {
        val label = it.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
            ?: it.config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text
            ?: it.children.firstNotNullOfOrNull { c -> c.config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text }
        "\"${label ?: "?"}\" at ${it.boundsInRoot}"
    }
}

/** Made-up car apps and music for the walks, as in the README mockups. */
internal object PadWalkStates {
    fun state(layout: CarLayout, screen: CarScreen) = CarUiState().apply {
        night = true
        this.screen = screen
        this.layout = layout
        theme = CarThemes.byId(if (layout == CarLayout.CLASSIC) "android_auto_2019" else "aare")
        layoutSpec = CarLayoutSpec.DEFAULT
        val icon = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        fun app(id: String, label: String, kind: CarAppKind) = CarAppEntry(id, "mock.$id", label, icon, kind)
        val maps = app("maps", "Maps", CarAppKind.NAVIGATION)
        val music = app("music", "Music", CarAppKind.MEDIA)
        apps += listOf(
            maps, music,
            app("podcasts", "Podcasts", CarAppKind.MEDIA),
            app("radio", "Radio", CarAppKind.MEDIA),
            app("messages", "Messages", CarAppKind.MESSAGING),
            app("phone", "Phone", CarAppKind.PHONE),
            app("notifications", "Notifications", CarAppKind.NOTIFICATIONS),
            app("settings", "Settings", CarAppKind.SETTINGS),
        )
        activeNavAppId = maps.id
        activeMediaAppId = music.id
        recentAppIds += listOf("podcasts", "messages")
        nowPlaying = NowPlaying(music.id, "Night Drive", "The Synthwaves", icon, playing = true,
            positionMs = 95_000, durationMs = 240_000, updatedAtMs = 0)
    }

    object NoActions : CarActions {
        override fun openApp(app: CarAppEntry) = Unit
        override fun goHome() = Unit
        override fun openLauncher() = Unit
        override fun assistant() = Unit
        override fun mediaPlayPause() = Unit
        override fun mediaNext() = Unit
        override fun mediaPrevious() = Unit
        override fun dismissNotice(notice: CarNotice) = Unit
        override fun answerCall() = Unit
        override fun endCall() = Unit
        override fun navigateTo(place: String) = Unit
    }
}
