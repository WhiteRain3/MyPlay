package net.prototux.aare.ui.car

import android.text.format.DateFormat
import androidx.car.app.model.CarIcon
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import net.prototux.aare.R
import net.prototux.aare.host.templates.CarFormat
import net.prototux.aare.host.templates.LocalMapCovers
import net.prototux.aare.host.templates.MapCoverRegistry
import net.prototux.aare.host.templates.MapGeometry.Box as SpecBox
import net.prototux.aare.host.templates.coversMap
import net.prototux.aare.ui.car.CarLayoutSpec.BarSide
import net.prototux.aare.ui.car.CarLayoutSpec.BarStyle
import net.prototux.aare.ui.car.CarLayoutSpec.CardPlace
import net.prototux.aare.ui.car.CarLayoutSpec.SpecCard
import net.prototux.aare.ui.theme.LocalCarDimens

/**
 * MyPlay (M3): loads a navigation app's maneuver icon (CarRuntime provides it; without it, cards
 * show the text only).
 */
internal val LocalTurnIcon = staticCompositionLocalOf<(CarIcon, Int, String?) -> ImageBitmap?> { { _, _, _ -> null } }

/**
 * MyPlay (M4): whether a maps app's picture on the home screen, which opens the app when tapped, is
 * also a stop for the knob. It is on AARE's dashboard card; not where the map fills the screen
 * (the bar's maps button opens the app there).
 */
internal val LocalMapTapIsKnobStop = staticCompositionLocalOf { true }

/**
 * MyPlay (M3): the "map behind" layout. One file draws any combination of the layout options
 * ([CarLayoutSpec], placed by [SpecPlacement]): on the home screen the maps app's picture fills
 * the screen (or the area beside the bar), with the bar and the cards on top; an opened app takes
 * the area beside the bar, as in the other layouts.
 *
 * Everything drawn over the map is marked ([coversMap]): the bar and each showing card cover it,
 * and a card's slot stays reserved while the card is hidden, so the maps app keeps its position
 * arrow in the free part and does not jump when a card comes or goes (M2).
 *
 * The knob (M4): it goes through the bar and the cards in reading order ([SpecPlacement.readingOrder]),
 * and starts on the first of them when the home screen opens (as K2 does for the other layouts).
 * Cards that are only read (next turn, arrival time, clock) are never a stop; tapping them still
 * opens their app. On the music card, its three buttons are the stops.
 */
@Composable
internal fun SpecShell(
    state: CarUiState,
    actions: CarActions,
    appContent: @Composable (CarAppEntry, Modifier) -> Unit,
    navMap: @Composable (CarAppEntry, Modifier) -> Unit,
    overlay: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spec = state.layoutSpec.allowed()
    val covers = remember { MapCoverRegistry() }
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize().then(modifier)) {
            val p = remember(spec, maxWidth, maxHeight) { SpecPlacement.place(spec, maxWidth.value, maxHeight.value) }
            CompositionLocalProvider(LocalMapCovers provides covers) {
                if (state.screen == CarScreen.Dashboard) {
                    val nav = state.app(state.homeNavAppId) // MyPlay (K9)
                    CompositionLocalProvider(LocalMapTapIsKnobStop provides false) {
                        if (spec.mapBackground && nav != null) navMap(nav, Modifier.at(p.map))
                        else Wallpaper(state, Modifier.at(p.map))
                    }
                    // The bar and the cards, composed in reading order: the knob follows composition order.
                    val entry = remember { FocusRequester() }
                    var homeFocused by remember { mutableStateOf(false) }
                    KeepFocusInContent(state.screen, { homeFocused }, entry)
                    Box(Modifier.fillMaxSize().onFocusChanged { homeFocused = it.hasFocus }.focusRequester(entry).focusGroup()) {
                        val slots = if (spec.cardPlace == CardPlace.STAGE) p.slots.take(1) else p.slots
                        // P2-3: the bar leaves out what the "Clock and status" card shows only while
                        // that card is really on the screen (placed, and for a stage, the one shown).
                        val stageShown = if (spec.cardPlace == CardPlace.STAGE) spec.cards.filter { shownNow(it, spec, state) } else null
                        val clockCard = clockCardOnScreen(spec, p.slots.size, stageShown)
                        // M3-3: no maps app shown: the maps buttons in the largest free area, as on AARE's map card.
                        val panel = if (nav == null) SpecPlacement.freeArea(p).takeIf { it.area > 0f } else null
                        for (i in SpecPlacement.readingOrder(listOf(p.bar) + slots + listOfNotNull(panel))) {
                            if (i == 0) SpecBar(state, actions, spec, clockCard, Modifier.at(p.bar).coversMap())
                            else if (i > slots.size) MapsPanel(state, actions, Modifier.at(panel!!))
                            else if (spec.cardPlace == CardPlace.STAGE) StageSlot(spec, state, actions, stageShown.orEmpty(), Modifier.at(slots[0]))
                            else CardSlot(spec.cards[i - 1], spec, state, actions, Modifier.at(slots[i - 1]))
                        }
                    }
                } else {
                    ContentArea(state, actions, appContent, navMap, Modifier.at(p.content))
                    SpecBar(state, actions, spec, cardHasStatus = false, Modifier.at(p.bar).coversMap()) // no cards over an app
                }
            }
            overlay(Modifier.align(Alignment.BottomCenter))
            TopLines(state, actions, Modifier.align(Alignment.TopCenter)) // MyPlay (R6-3): notice and gesture line stacked
            CallOverlay(state, actions, Modifier.align(Alignment.TopCenter))
        }
    }
}

/** M3-3: "Where to?" and the maps buttons (or how to get a maps app), over the wallpaper. */
@Composable
private fun MapsPanel(state: CarUiState, actions: CarActions, modifier: Modifier) {
    val navApps = state.apps.filter { it.kind == CarAppKind.NAVIGATION }
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            Modifier.clip(MaterialTheme.shapes.large).background(Color.Black.copy(alpha = DARK_BASE))
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f)).padding(24.dp),
        ) {
            Text(stringResource(if (navApps.isEmpty()) R.string.nav_dashboard_no_app_title else R.string.nav_dashboard_where_to),
                style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(if (navApps.isEmpty()) R.string.nav_dashboard_install_hint else R.string.nav_dashboard_choose_app),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (navApps.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                MapsButtons(navApps, actions::openApp, actions::openLauncher)
            }
        }
    }
}

/** Place at [b] (dp) in the parent box. */
private fun Modifier.at(b: SpecBox): Modifier = offset(b.left.dp, b.top.dp).size(b.width.dp, b.height.dp)

// ---------------------------------------------------------------- the bar

@Composable
private fun SpecBar(state: CarUiState, actions: CarActions, spec: CarLayoutSpec, cardHasStatus: Boolean, modifier: Modifier) {
    val vertical = spec.barSide == BarSide.LEFT || spec.barSide == BarSide.RIGHT
    val scheme = MaterialTheme.colorScheme
    val (shape, background) = when (spec.barStyle) {
        BarStyle.SOLID_RAIL, BarStyle.TOP_RIBBON -> RoundedCornerShape(0.dp) to scheme.surfaceContainerLowest
        BarStyle.SEE_THROUGH_RAIL -> RoundedCornerShape(0.dp) to scheme.surfaceContainerLowest.copy(alpha = 0.55f)
        BarStyle.FLOATING_PILLS -> RoundedCornerShape(percent = 50) to scheme.surfaceContainerHigh.copy(alpha = 0.9f)
        BarStyle.BOTTOM_DOCK -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp) to scheme.surfaceContainerLow
    }
    val nav = state.app(state.activeNavAppId)
    val media = state.app(state.activeMediaAppId)
    val phone = state.apps.firstOrNull { it.kind == CarAppKind.PHONE }
    fun on(app: CarAppEntry?) = app != null && state.screen == CarScreen.App(app.id)
    val phoneStatus = rememberPhoneStatus()
    // The bar items chosen in the settings (upstream's rail widgets), as on the taskbar; a ribbon
    // or dock along an edge has room for them in its row, a side bar stacks them.
    // P2-1: while a "Clock and status" card is on the screen, the card keeps the clock, weather
    // and phone status and the bar leaves them out, so they are not shown twice.
    val items: @Composable () -> Unit = {
        RailWidgets(barItems(state.railWidgets, cardHasStatus), state.weather.takeIf { state.weatherInBar }, phoneStatus, horizontal = !vertical, spacing = if (vertical) 6.dp else 10.dp, weatherOn = state.weatherInBar)
        if (!cardHasStatus) NetworkStatusIcons(phoneStatus)
    }
    val buttons: @Composable () -> Unit = {
        MicButton(state.listening) { actions.assistant() }
        if (state.screen == CarScreen.Dashboard) Box(Modifier.carPart(CarPart.AppsButton)) { RailButton(Icons.Rounded.Apps, stringResource(R.string.dash_menu_apps), selected = false) { actions.openLauncher() } }
        else RailButton(Icons.Rounded.Home, stringResource(R.string.dash_menu_home), selected = false) { actions.goHome() }
        RailApp(nav, Icons.Rounded.Map, on(nav)) { nav?.let(actions::openApp) ?: actions.openLauncher() }
        RailApp(media, Icons.Rounded.MusicNote, on(media)) { media?.let(actions::openApp) ?: actions.openMusicApps() }
        phone?.let { RailApp(it, Icons.Rounded.Call, on(it)) { actions.openApp(it) } }
        NotificationBell(state) { state.apps.firstOrNull { it.kind == CarAppKind.NOTIFICATIONS }?.let(actions::openApp) }
    }
    Box(modifier.carPart(CarPart.Bar).clip(shape).background(background), contentAlignment = Alignment.Center) {
        if (vertical) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { items(); buttons() }
        } else {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) { items(); buttons() }
        }
    }
}

// ---------------------------------------------------------------- cards

/** True for [seconds] after [key] last changed, then false; always true when [seconds] is null. */
@Composable
private fun shownFor(key: Any?, seconds: Int?): Boolean {
    var shown by remember { mutableStateOf(true) }
    LaunchedEffect(key, seconds) {
        shown = true
        if (seconds != null) {
            delay(seconds * 1000L)
            shown = false
        }
    }
    return shown
}

/** Whether [card] has something to show now (before any "hide after"). */
private fun hasContent(card: SpecCard, state: CarUiState): Boolean = when (card) {
    SpecCard.NEXT_TURN -> state.trip?.steps?.firstOrNull() != null || state.phoneGuidance != null
    SpecCard.ARRIVAL -> state.trip?.destinationTravelEstimates?.firstOrNull() != null
    SpecCard.MUSIC -> state.nowPlaying != null
    SpecCard.CLOCK_STATUS -> true
}

/** The card's "hide after" timer: which change restarts it, and how long. */
@Composable
private fun shownNow(card: SpecCard, spec: CarLayoutSpec, state: CarUiState): Boolean {
    val np = state.nowPlaying
    val timer = when (card) {
        SpecCard.MUSIC -> shownFor(np?.let { it.title to it.playing }, spec.hideMusicAfterS)
        SpecCard.ARRIVAL -> shownFor(state.trip?.destinationTravelEstimates?.firstOrNull()?.arrivalTimeAtDestination?.timeSinceEpochMillis, spec.hideArrivalAfterS)
        else -> true
    }
    return timer && hasContent(card, state)
}

/** One card in its slot; the slot stays reserved over the map while the card is hidden. */
@Composable
private fun CardSlot(card: SpecCard, spec: CarLayoutSpec, state: CarUiState, actions: CarActions, modifier: Modifier) {
    val shown = shownNow(card, spec, state)
    Box(modifier.coversMap(now = shown).carPart(CarPart.MapBehindCard(card))) {
        if (shown) SpecCard(card, spec, state, actions, Modifier.fillMaxSize())
    }
}

/** The stage: the first card that has something to show, plus one more with "one extra at a time". */
@Composable
private fun StageSlot(spec: CarLayoutSpec, state: CarUiState, actions: CarActions, withContent: List<SpecCard>, modifier: Modifier) {
    // P2-5: [withContent] is the same list the bar's clock rule reads, worked out once by the caller.
    val shownCards = withContent.take(if (spec.oneExtraAtATime) 2 else 1)
    Box(modifier.coversMap(now = shownCards.isNotEmpty())) {
        if (shownCards.isNotEmpty()) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(SpecPlacement.GAP.dp)) {
                shownCards.forEach { card -> SpecCard(card, spec, state, actions, Modifier.weight(1f).fillMaxWidth()) }
            }
        }
    }
}

private fun onClick(card: SpecCard, state: CarUiState, actions: CarActions): (() -> Unit)? = when (card) {
    SpecCard.NEXT_TURN, SpecCard.ARRIVAL -> state.app(state.activeNavAppId)?.let { nav -> { actions.openApp(nav) } }
    SpecCard.MUSIC -> state.app(state.nowPlaying?.appId ?: state.activeMediaAppId)?.let { m -> { actions.openApp(m) } }
    SpecCard.CLOCK_STATUS -> null
}

/**
 * The music card's knob state (M3-6): the card is one stop; pressing it with the knob goes
 * [inside], where its buttons are stops too and the ring moves to the first of them. Turning past
 * them leaves the card, which is then one stop again.
 */
private class CardKnob {
    var inside by mutableStateOf(false)
    val first = FocusRequester()
    /**
     * Whether the first button has held the ring since the card was pressed (H-1: the press keeps
     * asking until it has; H-1b: once is enough, so a quick turn onward is not pulled back).
     */
    var reached by mutableStateOf(false)
}

/** H-1: how many frames the music card's press keeps asking for its first button. */
private const val FIRST_BUTTON_TRIES = 10

/** One card: its frame and content, and for the music card its knob behaviour. */
@Composable
private fun SpecCard(card: SpecCard, spec: CarLayoutSpec, state: CarUiState, actions: CarActions, modifier: Modifier) {
    val open = onClick(card, state, actions)
    if (card != SpecCard.MUSIC) {
        SpecCardBox(spec, open, knobStop = false, modifier) { CardContent(card, state, actions, null) }
        return
    }
    val knob = remember { CardKnob() }
    // The knob's press goes inside; a touch opens the app, as on the other cards.
    // H1-a: pressed again after turning back onto the card (still inside): go to the first button now.
    val press = {
        if (!CarFocusVisibility.keyInput) open?.invoke() ?: Unit
        else if (knob.inside) { runCatching { knob.first.requestFocus() }; Unit }
        else {
            // H-1c: the buttons read [CardKnob.inside] when focus asks whether they are stops, not
            // when they are drawn, so the ring can go to the first one at once.
            knob.inside = true
            runCatching { knob.first.requestFocus() }
            Unit
        }
    }
    // The buttons become stops only once that change is drawn: ask for the focus after it.
    // H-1: a frame's callbacks run before that frame's recomposition, so after one frame the first
    // button may not be a stop yet and the request fails quietly (the ring stayed on the card and
    // the driver had to press again). Ask again each frame until the button holds the ring.
    // H-1b: stop for good once the first button has held the ring, even if the ring has already
    // moved on by the next frame (a quick turn): asking again would pull it back.
    LaunchedEffect(knob.inside) {
        if (!knob.inside) return@LaunchedEffect
        knob.reached = false
        repeat(FIRST_BUTTON_TRIES) {
            withFrameNanos { }
            if (!knob.inside || knob.reached) return@LaunchedEffect
            runCatching { knob.first.requestFocus() }
        }
    }
    // H1: the card stays a stop even inside. Making it unfocusable while it held the focus made
    // Compose clear the focus, which reset "inside" before the ring could reach the buttons.
    // Turning back from the first button lands on the card again, which is harmless.
    SpecCardBox(spec, press, knobStop = true, modifier.onFocusChanged { if (!it.hasFocus) knob.inside = false }) {
        CardContent(card, state, actions, knob)
    }
}

/**
 * A card's frame: always a dark base, the theme's card colour over it as see-through as the spec
 * says. A tap runs [onClick]; the knob stops on the card only when [knobStop] (M4: cards that are
 * only read are passed by).
 */
@Composable
private fun SpecCardBox(spec: CarLayoutSpec, onClick: (() -> Unit)?, knobStop: Boolean, modifier: Modifier, content: @Composable () -> Unit) {
    val shape: Shape = MaterialTheme.shapes.large
    val card = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 1f - spec.cardSeeThrough / 100f)
    Box(
        modifier.clip(shape).background(Color.Black.copy(alpha = DARK_BASE)).background(card)
            .let { if (onClick != null) it.focusProperties { canFocus = knobStop }.carFocusHighlight(shape).clickable(onClick = onClick) else it }
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart,
    ) { content() }
}

/** How dark the base under every card is, whatever the see-through amount. */
private const val DARK_BASE = 0.55f

@Composable
private fun CardContent(card: SpecCard, state: CarUiState, actions: CarActions, knob: CardKnob?) {
    when (card) {
        SpecCard.NEXT_TURN -> NextTurn(state)
        SpecCard.ARRIVAL -> Arrival(state)
        SpecCard.MUSIC -> Music(state, actions, knob)
        SpecCard.CLOCK_STATUS -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Clock()
            WeatherChip(state.weather.takeIf { state.weatherInBar })
            PhoneStatusIcons()
        }
    }
}

@Composable
private fun NextTurn(state: CarUiState) {
    val trip = state.trip
    val step = trip?.steps?.firstOrNull()
    val phone = state.phoneGuidance
    val loadIcon = LocalTurnIcon.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (step != null) {
            step.maneuver?.icon?.let { loadIcon(it, 160, state.tripPackage) }?.let { Image(it, null, Modifier.size(64.dp)) }
            Spacer(Modifier.width(12.dp))
            Column {
                trip.stepTravelEstimates.firstOrNull()?.remainingDistance?.let { d ->
                    Text(CarFormat.distance(d.displayDistance, d.displayUnit), style = MaterialTheme.typography.headlineMedium)
                }
                val road = (step.road ?: step.cue ?: trip.currentRoad)?.toString().orEmpty()
                if (road.isNotBlank()) Text(road, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        } else if (phone != null) {
            phone.arrow?.let { a ->
                Image(a.asImageBitmap(), null, Modifier.size(64.dp), colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface, BlendMode.SrcIn))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(phone.primary, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (phone.secondary.isNotBlank()) Text(phone.secondary, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun Arrival(state: CarUiState) {
    val est = state.trip?.destinationTravelEstimates?.firstOrNull() ?: return
    val context = LocalContext.current
    Column {
        est.arrivalTimeAtDestination?.let { eta ->
            Text(stringResource(R.string.myplay_card_arrival, DateFormat.getTimeFormat(context).format(eta.timeSinceEpochMillis)),
                style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        }
        est.remainingDistance?.let { d ->
            Text(CarFormat.distance(d.displayDistance, d.displayUnit), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun Music(state: CarUiState, actions: CarActions, knob: CardKnob?) {
    val np = state.nowPlaying ?: return
    Row(verticalAlignment = Alignment.CenterVertically) {
        // MyPlay: the song's artwork, as tall as the buttons so the card keeps its height.
        Art(np, LocalCarDimens.current.touchTarget)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(np.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(np.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        // Stops for the knob only inside the card (M3-6); a touch reaches them always. Read when
        // focus asks (H-1c), so a press that goes inside can move the ring at once.
        val stop = { knob == null || knob.inside }
        CardButton(Icons.Rounded.SkipPrevious, stringResource(R.string.media_action_previous), filled = false, stop, knob?.first,
            onFocus = knob?.let { k -> { focused: Boolean -> if (focused) k.reached = true } }) { actions.mediaPrevious() }
        CardButton(if (np.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, stringResource(R.string.media_action_play_pause), filled = true, stop) { actions.mediaPlayPause() }
        CardButton(Icons.Rounded.SkipNext, stringResource(R.string.media_action_next), filled = false, stop) { actions.mediaNext() }
    }
}

/** A round button on a card, like [RoundIcon], that is a knob stop only when [knobStop]. */
@Composable
private fun CardButton(icon: ImageVector, label: String, filled: Boolean, knobStop: () -> Boolean, requester: FocusRequester? = null,
                       onFocus: ((Boolean) -> Unit)? = null, onClick: () -> Unit) {
    val d = LocalCarDimens.current
    val bg = if (filled) MaterialTheme.colorScheme.primary else Color.Transparent
    val fg = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(
        Modifier.size(d.touchTarget).clip(CircleShape).background(bg)
            .let { if (requester != null) it.focusRequester(requester) else it }
            .let { if (onFocus != null) it.onFocusChanged { f -> onFocus(f.isFocused) } else it }
            .focusProperties { canFocus = knobStop() }.carFocusHighlight(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = fg, modifier = Modifier.size(d.iconSize)) }
}

/** MyPlay (P2-1): the bar items for "map behind": without those the "Clock and status" card already shows. */
internal fun barItems(chosen: List<RailWidget>, cardHasStatus: Boolean): List<RailWidget> =
    if (cardHasStatus) chosen - setOf(RailWidget.CLOCK, RailWidget.WEATHER, RailWidget.BATTERY) else chosen

/**
 * MyPlay (P2-3): whether the "Clock and status" card is on the screen: among the cards that got a
 * slot ([slotCount]; a small screen cuts the last ones), or for a stage, among the ones it shows
 * now ([stageShown], the cards with something to show, in order).
 */
internal fun clockCardOnScreen(spec: CarLayoutSpec, slotCount: Int, stageShown: List<SpecCard>?): Boolean =
    if (stageShown != null) SpecCard.CLOCK_STATUS in stageShown.take(if (spec.oneExtraAtATime) 2 else 1)
    else SpecCard.CLOCK_STATUS in spec.cards.take(slotCount)
