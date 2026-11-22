package net.prototux.aare.ui.phone.next

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import net.prototux.aare.R

/** The tab bar's icons: house, screen, car, apps, more (design frame 01, review finding 11). */
val NextTab.icon: ImageVector
    get() = when (this) {
        NextTab.HOME -> Icons.Outlined.Home
        NextTab.SCREEN -> Icons.Outlined.DesktopWindows
        NextTab.CAR -> Icons.Outlined.DirectionsCar
        NextTab.APPS -> Icons.Outlined.GridView
        NextTab.MORE -> Icons.Rounded.MoreHoriz
    }

/** What a page needs from the shell: open a route, and the tab's "scroll to top" signal. */
class NextNavigator internal constructor(
    val open: (String) -> Unit,
    internal val scrollTop: Int,
    /** Back, as the system's Back would do it. */
    val back: () -> Unit = {},
)

val LocalNextNavigator = compositionLocalOf { NextNavigator(open = {}, scrollTop = 0) }

/**
 * A page's list position, saved with the page (Back, tab switches, rotation, process death), and
 * scrolled to the top when its tab is tapped again at the root (plan §3.4 rules 4–5).
 */
@Composable
fun rememberNextListState(): LazyListState {
    val state = rememberLazyListState()
    val signal = LocalNextNavigator.current.scrollTop
    var handled by rememberSaveable { mutableIntStateOf(signal) }
    LaunchedEffect(signal) {
        if (signal != handled) {
            handled = signal
            state.animateScrollToItem(0)
        }
    }
    return state
}

private val NavStateSaver = Saver<NavState, ArrayList<String>>(save = { it.save() }, restore = { NavState.restore(it) })

/**
 * The rebuilt phone app (plan §3, §16 chunk 0): five tabs, each with its own back stack; every page's
 * state kept by route. [deepLink] is a route from the `route` extra; [onDeepLinkUsed] clears it.
 * [page] draws a route; until the pages are built it is a placeholder. [start] is the first route
 * shown, before any saved place (for pictures of a page).
 */
@Composable
fun NextApp(
    deepLink: String? = null,
    start: String? = null,
    onDeepLinkUsed: () -> Unit = {},
    onOldApp: () -> Unit = {},
    badges: Set<NextTab> = emptySet(),
    page: @Composable (String) -> Unit = { NextPlaceholder(it, onOldApp) },
) {
    var nav by rememberSaveable(stateSaver = NavStateSaver) { mutableStateOf(start?.let { NavState().deepLink(it) } ?: NavState()) }
    // Per tab: bumped when the tab is tapped at its root, so its list scrolls to the top.
    var scrollTop by rememberSaveable { mutableStateOf(IntArray(NextTab.entries.size)) }
    val pages = rememberSaveableStateHolder()
    // Search (plan §7) takes the page's place; the tab bar stays.
    var searching by rememberSaveable { mutableStateOf(false) }

    fun go(next: NavState) {
        // A page that left every stack starts fresh next time.
        (nav.liveRoutes - next.liveRoutes).forEach { pages.removeState(it) }
        nav = next
    }

    LaunchedEffect(deepLink) {
        if (deepLink != null) {
            go(nav.deepLink(deepLink))
            onDeepLinkUsed()
        }
    }

    val snackbar = remember { SnackbarHostState() }
    val undo = rememberNextUndo(snackbar, stringResource(R.string.next_undo_label))

    var backProgress by remember { mutableFloatStateOf(0f) }
    PredictiveBackHandler(enabled = nav.back() != null) { events ->
        try {
            events.collect { backProgress = it.progress }
            nav.back()?.let { go(it) }
        } catch (_: CancellationException) {
            // The gesture was let go of: stay.
        } finally {
            backProgress = 0f
        }
    }

    val world = nav.tab.world
    val onTab: (NextTab) -> Unit = { tab ->
        if (searching) {
            searching = false
            go(nav.selectTab(tab))
        } else if (tab == nav.tab && nav.atRoot) {
            scrollTop = scrollTop.copyOf().also { it[tab.ordinal]++ }
        } else {
            go(nav.selectTab(tab))
        }
    }
    BoxWithConstraints(
        Modifier.fillMaxSize().background(NextColors.Bg).drawBehind {
            drawRect(
                Brush.radialGradient(
                    listOf(world.copy(alpha = 0.11f), Color.Transparent),
                    center = Offset(size.width / 2, -size.height * 0.08f),
                    radius = size.width * 0.9f,
                ),
            )
        },
    ) {
        // Large screens (design frame 76): a rail from 600 dp; from 840 dp the tab's root list stays
        // on the left while a page of it is open on the right.
        val full = nav.route in NextRoutes.FULL_SCREEN
        val rail = maxWidth >= 600.dp && !full
        val split = maxWidth >= 840.dp && !full && !searching && !nav.atRoot
        val navigator = NextNavigator(open = { go(nav.open(it)) }, scrollTop = scrollTop[nav.tab.ordinal], back = { nav.back()?.let { go(it) } })
        // From the list on the left, a page replaces the one on the right instead of stacking on it.
        val listNavigator = NextNavigator(open = { go(nav.selectTab(nav.tab).open(it)) }, scrollTop = scrollTop[nav.tab.ordinal], back = navigator.back)
        val shift = with(LocalDensity.current) { 48.dp.toPx() }
        Row(Modifier.fillMaxSize()) {
            if (rail) NextRail(nav.tab, badges, onTab)
            Column(Modifier.weight(1f).fillMaxSize()) {
                Box(
                    Modifier.weight(1f).fillMaxWidth().graphicsLayer {
                        translationX = backProgress * shift
                        alpha = 1f - backProgress * 0.25f
                    },
                ) {
                    CompositionLocalProvider(
                        LocalNextUndo provides undo,
                        LocalTextStyle provides NextType.Label.copy(color = NextColors.Text),
                    ) {
                        Row(Modifier.fillMaxSize()) {
                            if (split) {
                                val root = nav.tab.root
                                Box(Modifier.fillMaxHeight().fillMaxWidth(0.42f).testTag("next-list-pane")) {
                                    CompositionLocalProvider(LocalNextNavigator provides listNavigator) {
                                        // Its own key: during the fade the full-width root may still be drawn.
                                        pages.SaveableStateProvider("$root#list") {
                                            Column(Modifier.fillMaxSize()) {
                                                NextTopBar(root, onBack = {}, onSearch = { searching = true })
                                                Box(Modifier.weight(1f)) { page(root) }
                                            }
                                        }
                                    }
                                }
                                Box(Modifier.fillMaxHeight().width(1.dp).background(NextColors.Edge))
                            }
                            Box(Modifier.weight(1f).fillMaxHeight()) {
                                CompositionLocalProvider(LocalNextNavigator provides navigator) {
                                    if (searching) NextSearch(onOpen = { searching = false; go(nav.open(it)) }, onClose = { searching = false })
                                    else AnimatedContent(
                                        targetState = nav.top,
                                        transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) },
                                        label = "route",
                                    ) { entry ->
                                        pages.SaveableStateProvider(entry.route) {
                                            Column(Modifier.fillMaxSize()) {
                                                if (entry.route !in NextRoutes.FULL_SCREEN) NextTopBar(entry.route, onBack = { nav.back()?.let { go(it) } }, onSearch = { searching = true })
                                                Box(Modifier.weight(1f)) { page(entry.route) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    NextSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp))
                }
                if (!rail && !full) NextTabBar(nav.tab, badges, onTab)
            }
        }
    }
}

/** The tabs as a rail on large screens (design frame 76): the mark on top, the same five tabs. */
@Composable
fun NextRail(selected: NextTab, badge: Set<NextTab> = emptySet(), onSelect: (NextTab) -> Unit) {
    Row(Modifier.fillMaxHeight()) {
        Column(
            Modifier.fillMaxHeight().width(96.dp).background(NextColors.TabBar).statusBarsPadding().navigationBarsPadding().padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NextMark(36.dp)
            Spacer(Modifier.height(16.dp))
            for (tab in NextTab.entries) TabItem(tab, tab == selected, tab in badge, Modifier.fillMaxWidth().height(80.dp), onSelect)
        }
        Box(Modifier.fillMaxHeight().width(1.dp).background(NextColors.Edge))
    }
}

@Composable
private fun TabItem(tab: NextTab, on: Boolean, badged: Boolean, modifier: Modifier, onSelect: (NextTab) -> Unit) {
    val label = stringResource(NextPages.tabLabel(tab))
    Column(
        modifier.clickable(role = Role.Tab) { onSelect(tab) }
            .semantics { this.selected = on }
            .testTag("next-tab-${tab.root}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape)
                .then(if (on) Modifier.background(tab.world.soft()).border(2.dp, tab.world, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(tab.icon, null, tint = if (on) tab.world else NextColors.Text2, modifier = Modifier.size(22.dp))
            if (badged) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(2.dp).size(10.dp).clip(CircleShape)
                        .background(NextColors.Caution),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label, style = NextType.TabLabel.copy(fontWeight = if (on) FontWeight(650) else FontWeight(520)),
            color = if (on) NextColors.Text else NextColors.Text3,
        )
    }
}

/** A root's title with the tab's world glyph, or a page's title with Back (design frames 01, 20, 10). */
@Composable
private fun NextTopBar(route: String, onBack: () -> Unit, onSearch: () -> Unit = {}) {
    val tab = NextRoutes.tabOf(route) ?: NextTab.HOME
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val title = NextPages.title(route)?.let { stringResource(it) } ?: carTitle(ctx, route) ?: route.substringAfterLast('/')
    val isRoot = route == tab.root
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().height(if (isRoot) 72.dp else 64.dp)
            .padding(start = if (isRoot) 20.dp else 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (isRoot && tab == NextTab.HOME) {
            NextMark()
            Text(title, style = NextType.Wordmark, color = NextColors.Text, modifier = Modifier.semantics { heading() })
        } else if (isRoot) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(tab.world.soft()).border(2.dp, tab.world, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(tab.icon, null, tint = tab.world, modifier = Modifier.size(20.dp)) }
            Text(title, style = NextType.RootTitle, color = NextColors.Text, modifier = Modifier.semantics { heading() })
        } else {
            val back = stringResource(R.string.next_back_label)
            Box(
                Modifier.size(NextSpace.Target).clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onBack).semantics { contentDescription = back }
                    .testTag("next-back"),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = NextColors.Text2) }
            Text(title, style = NextType.PageTitle, color = NextColors.Text, modifier = Modifier.semantics { heading() })
        }
        Spacer(Modifier.weight(1f))
        // In every tab's top bar (plan §7).
        val search = stringResource(R.string.next_search_label)
        Box(
            Modifier.size(NextSpace.Target).clip(CircleShape).clickable(role = Role.Button, onClick = onSearch)
                .semantics { contentDescription = search }.testTag("next-search-button"),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Search, null, tint = NextColors.Text2) }
    }
}

/** The five tabs (design frame 01): the selected one in a ring of its world colour. */
@Composable
fun NextTabBar(selected: NextTab, badge: Set<NextTab> = emptySet(), onSelect: (NextTab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(NextColors.TabBar)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(NextColors.Edge))
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(NextSpace.TabBar)) {
            for (tab in NextTab.entries) TabItem(tab, tab == selected, tab in badge, Modifier.weight(1f).fillMaxSize(), onSelect)
        }
    }
}

/**
 * A page not built yet: one line, then links to the pages under it (and its cross-tab links), so every
 * route can be reached. About also has "Back to the old app" while the hidden switch exists.
 */
@Composable
fun NextPlaceholder(route: String, onOldApp: () -> Unit) {
    val navigator = LocalNextNavigator.current
    val children = NextPages.children(route)
    val links = NextPages.links(route)
    val title: @Composable (String) -> String = { r -> NextPages.title(r)?.let { stringResource(it) } ?: r }
    LazyColumn(
        Modifier.fillMaxSize().testTag("next-page-$route"),
        state = rememberNextListState(),
        contentPadding = PaddingValues(horizontal = NextSpace.Gutter, vertical = 8.dp),
    ) {
        item { NextNote(stringResource(R.string.next_placeholder_help)) }
        if (children.isNotEmpty() || links.isNotEmpty()) item {
            Spacer(Modifier.height(12.dp))
            NextGroup(
                children.map { r -> @Composable { Box(Modifier.testTag("next-row-$r")) { NextValueRow(title(r)) { navigator.open(r) } } } } +
                    links.map { r -> @Composable { Box(Modifier.testTag("next-row-$r")) { NextLinkRow(title(r)) { navigator.open(r) } } } },
                inset = 16.dp,
            )
        }
        if (route == "more/about") item {
            Spacer(Modifier.height(12.dp))
            NextCard {
                NextRow(
                    stringResource(R.string.next_old_app_label), note = RowNote.Help(stringResource(R.string.next_old_app_help)),
                    onClick = onOldApp, role = Role.Button,
                )
            }
        }
    }
}
