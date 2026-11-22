package net.prototux.aare.ui.car.myplay

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import net.prototux.aare.R
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import android.os.SystemClock
import kotlinx.coroutines.delay
import net.prototux.aare.media.MediaLibrary
import net.prototux.aare.media.SEEK_PENDING_MS
import net.prototux.aare.media.SEEK_STEP_MS
import net.prototux.aare.media.nextSeek
import net.prototux.aare.media.seekConfirmed
import net.prototux.aare.car.SpeedFeed
import net.prototux.aare.car.SpeedReading
import net.prototux.aare.car.SpeedSourceMode
import net.prototux.aare.host.LocalSkinMapFilter
import net.prototux.aare.host.templates.LocalMapCovers
import net.prototux.aare.host.templates.MapCoverRegistry
import net.prototux.aare.host.templates.coversMap
import net.prototux.aare.ui.car.rememberCanEnd
import net.prototux.aare.ui.car.CarActions
import net.prototux.aare.ui.car.CarAppEntry
import net.prototux.aare.ui.car.CarAppKind
import net.prototux.aare.ui.car.CarFocusVisibility
import net.prototux.aare.ui.car.CarScreen
import net.prototux.aare.ui.car.CarUiState
import net.prototux.aare.ui.car.LocalMapTapIsKnobStop
import net.prototux.aare.ui.car.GestureLineOverlay
import net.prototux.aare.ui.car.Wallpaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.prototux.aare.car.LimitWhy
import net.prototux.aare.car.LimitFile

/**
 * MyPlay (plan phase N): the engine of MyPlay's own car screen, drawn by a [MyPlaySkin].
 *
 * The home screen: the maps app's picture (or the wallpaper) behind, the skin's slots on top, the
 * world chooser over it. N2: the knob and the pad move a ring between the home's stops through
 * [FocusGraph] (the engine keeps which stop has the ring, not Compose's focus): the turn (a push
 * opens the maps app), the hub (opens the chooser) and the song (opens the music app). In the
 * chooser a turn or left and right move the choice, a push enters; Back goes up. The Apps world
 * opens the app grid until it draws its own (N6), so the knob always has a way out (N1-1).
 *
 * N3: the slots tell the maps app what they cover ([MyPlaySkin.coversMap]), so it keeps its
 * arrow in the free part, and the skin may suggest a map colour filter ([SkinTokens.mapFilter]).
 * A slot that is not drawn (no route yet) covers nothing until it appears.
 *
 * Every other screen is still the "map behind" layout's until the chunks after N2 replace them.
 * Shown only with the hidden setting myplay_engine_preview.
 */
@Composable
internal fun MyPlayEngineShell(
    state: CarUiState,
    actions: CarActions,
    appContent: @Composable (CarAppEntry, Modifier) -> Unit,
    navMap: @Composable (CarAppEntry, Modifier) -> Unit,
    overlay: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    skin: MyPlaySkin = net.prototux.aare.ui.car.myplay.orbit.OrbitSkin,
    sources: EngineSources = LocalEngineSources.current ?: LocalContext.current.let { c -> remember(state, actions) { EngineSources.of(state, actions, c) } },
) {
    // O3: kept while an app has the screen (the rest of the engine is not), so Back comes back to it.
    val memory = remember { EngineMemory() }
    if (state.screen != CarScreen.Dashboard) {
        // F1: a full-screen app (or the app grid) in MyPlay's own frame, not the old layout's.
        MyPlayAppFrame(state, actions, appContent, navMap, overlay, modifier, skin, sources, memory)
        return
    }
    // O3: a skin without a rail comes back from an app to where it was; Home asked for (the Menu
    // button, Back held) is the plain home; a new language, or a skin with a rail, starts afresh.
    val keeps = !skin.appRail
    remember(skin.id) { if (memory.skinId != skin.id || state.engineHomeAsked || !keeps) memory.reset(skin.id); true }
    // N13: a new language starts afresh (a hand has no dial to be in).
    val hand = skin.hand
    val model = remember(skin.id) { memory.model(hand, skin.dial) }
    val tokens = skin.tokens(state.night)
    val context = LocalContext.current
    val timeFormat = remember(context) { DateFormat.getTimeFormat(context) }
    val plain = HomeSlots.from(state) { timeFormat.format(it) }
    // N5: the trip's progress, from the longest distance left seen on this route. N5-4: a new
    // destination is a new route even when navigation never ended (compared only, never logged).
    val destination = state.trip?.destinations?.firstOrNull()?.let { it.name?.toString() to it.address?.toString() }
    var longest by remember(destination) { mutableStateOf<Double?>(null) }
    val left = state.trip?.destinationTravelEstimates?.firstOrNull()?.remainingDistance?.let { TripProgress.meters(it.displayDistance, it.displayUnit) }
    val routeOn = state.trip != null
    val (seen, progress) = TripProgress.update(longest.takeIf { routeOn }, left)
    LaunchedEffect(seen, routeOn) { longest = if (routeOn) seen else null }
    val slots = plain.copy(trip = plain.trip?.copy(progress = progress))
    // The home's stops and where they are, as the slots report them.
    val boxes = remember { mutableStateMapOf<String, FocusGraph.Box>() }
    var chosen by memory.chosen
    // N8: a call. While it rings it takes the knob and the pad; in a call its strip is on the home.
    val call = sources.calls.call
    val canEnd = rememberCanEnd()
    val ringing = call?.ringing == true
    val callBoxes = remember { mutableStateMapOf<String, FocusGraph.Box>() }
    var callChosen by remember(call?.sinceMs) { mutableStateOf(CALL_ANSWER) }
    val callActions: Map<String, () -> Unit> = buildMap {
        if (ringing) {
            put(CALL_ANSWER, sources.calls::answer)
            if (canEnd) put(CALL_DECLINE, sources.calls::end)
        }
    }
    // R5: the ring starts on Answer, so a push answers.
    val callRing = callChosen.takeIf { it in callActions } ?: CALL_ANSWER
    // N8: the newest notice, for a few seconds; on the home its actions and Dismiss are stops.
    val notice = sources.notifications.notices.firstOrNull()
    LaunchedEffect(notice?.key) {
        if (notice != null) {
            delay(NOTICE_SHOW_MS)
            sources.notifications.dismiss(notice)
        }
    }
    // N8: the bell (while there are unseen notifications) opens the list over the home.
    val notes = sources.notifications
    var listOpen by remember { mutableStateOf(false) }
    var listChosen by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(model.place) { if (model.place != Place.Home) listOpen = false }
    // N13: the hand's push; set further down, once the worlds' state it resets is declared.
    var openFront: () -> Unit = {}
    // F3: the maps app, or the music app; with none known, the Apps world with those apps (set further down).
    var openMaps: () -> Unit = {}
    var openMusic: () -> Unit = {}
    // F4: the in-call strip opens the Calls world (set further down).
    var openCalls: () -> Unit = {}
    // O1: the music panel over the home, opened by a push on the song (never the music app itself).
    // O1-1: keyed on the skin, so a new language (DECK has no song slot to draw it) starts closed.
    var panelOpen by remember(skin.id) { mutableStateOf(false) }
    var panelChosen by remember(skin.id) { mutableStateOf(PANEL_PLAY) }
    // O6: the home pad opens things directly while the knob's ring is hidden; a turn shows the ring.
    val padMap = state.homePad
    val padHome = hand == null && HomePad.active(padMap)
    var ringAwake by remember(skin.id) { mutableStateOf(false) }
    val actionsOf: Map<String, () -> Unit> = buildMap {
        if (notes.unseen > 0 && model.place == Place.Home && !listOpen) put(STOP_BELL) { listOpen = true; listChosen = null; notes.markSeen() }
        if (notice != null && model.place == Place.Home) {
            // At most two actions: two fit in the room at the top (a phone's notice can carry more; the
            // rest stay on the phone, and in the notifications list).
            notice.actions.take(2).forEachIndexed { i, (_, run) -> put(noticeActionStop(i)) { run(); sources.notifications.dismiss(notice) } }
            put(NOTICE_DISMISS) { sources.notifications.dismiss(notice) }
        }
        if (call != null && !ringing) {
            put(STOP_CALL) { openCalls() }
            if (canEnd) put(STOP_HANG_UP, sources.calls::end)
        }
        if (slots.turn != null) put(STOP_TURN) { memory.origin = boxes[STOP_TURN]; openMaps() }
        if (hand != null) put(STOP_HUB) { openFront() } else put(STOP_HUB) { model.push() }
        if (slots.song != null && hand == null) put(STOP_SONG) { panelOpen = true; panelChosen = PANEL_PLAY }
    }
    // A stop that went away (the song ended) leaves the graph; the ring goes back to the hub.
    val live = boxes.filterKeys { it in actionsOf }
    val ring = chosen.takeIf { it in actionsOf } ?: STOP_HUB
    fun move(to: String?) { if (to != null) chosen = to }
    /**
     * N13: a knob click or a pad left/right on the hand: the next card; past the end, on to the
     * other home stops (as a knob or pad move from the hand), or round to the other end of the
     * hand when there is none (a knob only: the pad just stops).
     */
    fun swing(step: Int, knob: Boolean) {
        val h = hand ?: return
        HandMoves.turn(h, model.front, step)?.let { model.front = it; return }
        val to = if (knob) FocusGraph.knob(live, ring, step)
        else FocusGraph.pad(live, ring, if (step > 0) FocusGraph.Direction.RIGHT else FocusGraph.Direction.LEFT)
        if (to != null && to != STOP_HUB) move(to) else if (knob) model.front = HandMoves.wrap(h, step)
    }
    // N7: the Music world: its stops, which holds the ring, and a pad seek not yet confirmed.
    val music = sources.music
    val musicPage = MusicPage(slots.song, music.transport)
    // N7: the music app's library, while it is open inside the Music world.
    var library by remember { mutableStateOf<MediaLibrary?>(null) }
    var libraryRing by remember { mutableStateOf(0) }
    val lib = library
    val libraryKey = lib?.key
    LaunchedEffect(libraryKey) { libraryRing = 0 }
    // Leaving the Music world closes the library.
    LaunchedEffect(model.place) { if (model.place != Place.InWorld(World.MUSIC)) library = null }
    if (lib != null) {
        DisposableEffect(lib) {
            lib.start()
            onDispose { lib.stop() }
        }
        val rowCount = lib.rows.size
        // Near the end of what is loaded: the next page.
        LaunchedEffect(libraryKey, rowCount, libraryRing, lib.loading, lib.end) {
            if (!lib.end && !lib.loading && lib.error == null && libraryRing >= rowCount - 3) lib.loadMore()
        }
    }
    fun openRow(i: Int) {
        val l = library ?: return
        val row = l.rows.getOrNull(i) ?: return
        libraryRing = i
        // A song starts playing: back to now playing, as the app's own screen does.
        if (l.open(row)) library = null
    }
    // N7-1: keyed on the next songs, so boxes of songs that left the queue do not pile up.
    val musicBoxes = remember(music.transport.upNext) { mutableStateMapOf<String, FocusGraph.Box>() }
    var musicChosen by remember { mutableStateOf(MUSIC_PLAY) }
    val musicActions: Map<String, () -> Unit> = buildMap {
        if (musicPage.song != null) {
            put(MUSIC_ART, music::playPause)
            put(MUSIC_PREV, music::previous)
            put(MUSIC_PLAY, music::playPause)
            put(MUSIC_NEXT, music::next)
        }
        // N7: the library drawn by the skin when the app offers one, else the app's own screen.
        put(MUSIC_LIBRARY) { music.library()?.let { library = it } ?: run { memory.origin = musicBoxes[MUSIC_LIBRARY]; openMusic() } }
        for (u in musicPage.transport.upNext) put(upNextStop(u.queueId)) { music.skipTo(u.queueId) }
    }
    val musicRing = musicChosen.takeIf { it in musicActions } ?: if (musicPage.song != null) MUSIC_PLAY else MUSIC_LIBRARY
    var pendingSeek by remember { mutableStateOf<Long?>(null) }
    val np = music.nowPlaying
    // R5-9: the player's report confirms the seek; R5-4: or it stops counting after a while.
    LaunchedEffect(np?.positionMs, np?.updatedAtMs) { if (np != null && seekConfirmed(pendingSeek, np.positionMs)) pendingSeek = null }
    LaunchedEffect(pendingSeek) { if (pendingSeek != null) { delay(SEEK_PENDING_MS); pendingSeek = null } }
    fun seek(forward: Boolean) {
        val p = music.nowPlaying ?: return
        val now = if (p.playing) p.positionMs + (SystemClock.elapsedRealtime() - p.updatedAtMs).coerceAtLeast(0) else p.positionMs
        val target = nextSeek(pendingSeek, now, p.durationMs, if (forward) SEEK_STEP_MS else -SEEK_STEP_MS)
        pendingSeek = target
        music.seekTo(target)
    }
    // O1: the music panel's stops: the progress bar (left and right seek there, R5-5), previous,
    // play/pause, next, and the music app as one stop among them.
    val panelBoxes = remember { mutableStateMapOf<String, FocusGraph.Box>() }
    val panelActions: Map<String, () -> Unit> = buildMap {
        if (musicPage.transport.canSeek) put(PANEL_SEEK, music::playPause)
        put(PANEL_APP) { panelOpen = false; memory.origin = panelBoxes[PANEL_APP]; openMusic() }
        put(PANEL_PREV, music::previous)
        put(PANEL_PLAY, music::playPause)
        put(PANEL_NEXT, music::next)
    }
    val panelRing = panelChosen.takeIf { it in panelActions } ?: PANEL_PLAY
    // The song ended, or the home was left: the panel closes.
    LaunchedEffect(model.place, slots.song == null, hand) { if (model.place != Place.Home || slots.song == null || hand != null) panelOpen = false }
    // O2: a panel over the map (the music panel, the dial) closes by itself after the driver's
    // chosen time without a knob, pad or touch input; any input starts the time again. The ring
    // goes back to where it was on the home (the song, the hub). Real screens never close so.
    var inputs by remember { mutableIntStateOf(0) }
    val transient = panelOpen || model.place is Place.Chooser
    val idleMs = PanelTimeout.ms(state.panelTimeoutS)
    LaunchedEffect(transient, inputs, idleMs) {
        if (!transient || idleMs == null) return@LaunchedEffect
        delay(idleMs)
        if (panelOpen) { panelOpen = false; chosen = STOP_SONG }
        if (model.place is Place.Chooser) model.back()
    }
    // O6: the ring hides again after the panel time without input (8 s when panels never close).
    LaunchedEffect(ringAwake, inputs, idleMs) {
        if (!ringAwake) return@LaunchedEffect
        delay(idleMs ?: 8_000L)
        ringAwake = false
    }
    fun padPanel(dir: FocusGraph.Direction) {
        val sideways = dir == FocusGraph.Direction.LEFT || dir == FocusGraph.Direction.RIGHT
        if (panelRing == PANEL_SEEK && sideways) seek(dir == FocusGraph.Direction.RIGHT)
        else FocusGraph.pad(panelBoxes.filterKeys { it in panelActions }, panelRing, dir)?.let { panelChosen = it }
    }
    fun padMusic(dir: FocusGraph.Direction) {
        val sideways = dir == FocusGraph.Direction.LEFT || dir == FocusGraph.Direction.RIGHT
        // R5-5: on the artwork (it carries the progress bar) left and right seek; up, down and the knob leave it.
        if (musicRing == MUSIC_ART && sideways && musicPage.transport.canSeek) seek(dir == FocusGraph.Direction.RIGHT)
        else FocusGraph.pad(musicBoxes.filterKeys { it in musicActions }, musicRing, dir)?.let { musicChosen = it }
    }
    // N8: the Calls world: recent calls, then favourites, then the phone app; loaded when entered,
    // and again after a call.
    val inCalls = model.place == Place.InWorld(World.CALLS)
    val phone = sources.phone
    val phoneData by produceState<Pair<List<PhoneEntry>, List<PhoneEntry>>?>(null, inCalls, call == null) {
        value = if (inCalls && phone.permitted) runCatching { phone.recents() to phone.favourites() }.getOrNull() ?: (emptyList<PhoneEntry>() to emptyList()) else null
    }
    val phoneEntries: List<PhoneEntry> = phoneData?.let { (r, f) -> r + f.filter { fav -> r.none { it.number == fav.number } } }.orEmpty()
    val phoneRecents = phoneData?.first?.size ?: 0
    val phoneRows: List<PhoneRow> = phoneEntries.mapIndexed { i, e ->
        PhoneRow(e.name.ifBlank { e.number }, e.detail, when { i >= phoneRecents -> PhoneRow.Kind.FAVOURITE; e.missed -> PhoneRow.Kind.MISSED; else -> PhoneRow.Kind.RECENT })
    } + PhoneRow(stringResource(R.string.myplay_phone_app), "", PhoneRow.Kind.PHONE_APP)
    // P-1: the ring stays on a person (their number), not a row number: the list reloads after a
    // call with the newest one on top, and a push must never call whoever moved into the row.
    val phoneKeys: List<String> = phoneEntries.map { "tel:${it.number}" } + PHONE_APP_KEY
    var phoneChosen by remember { mutableStateOf<String?>(null) }
    val phoneIndex = phoneKeys.indexOf(phoneChosen).takeIf { it >= 0 } ?: 0
    fun movePhone(to: Int) { phoneChosen = phoneKeys[to.coerceIn(0, phoneKeys.size - 1)] }
    val currentPhoneKeys by rememberUpdatedState(phoneKeys)
    val currentPhoneEntries by rememberUpdatedState(phoneEntries)
    /** Calls the entry with [key] as the screen shows it now (or opens the phone app). */
    fun openPhone(key: String) {
        phoneChosen = key
        if (key == PHONE_APP_KEY) { phone.open(); return }
        currentPhoneEntries.firstOrNull { "tel:${it.number}" == key }?.let(phone::call)
    }
    // N9: the Car world: the car's data to read; the ring stays on a line (its label) as values change.
    var carChosen by remember { mutableStateOf<String?>(null) }
    val carLines = if (model.place == Place.InWorld(World.CAR)) sources.carLines() else emptyList()
    val carLabels = carLines.map { it.first }
    // N9: the Settings world: read again after each change; the ring stays on a setting (its id).
    var settingsVersion by remember { mutableStateOf(0) }
    val inSettings = model.place == Place.InWorld(World.SETTINGS)
    // S-3: also read again when the car starts or stops (passenger mode's note follows at once).
    val settingItems = remember(inSettings, settingsVersion, sources.settings, state.drivingRestricted) { if (inSettings) sources.settings.items() else emptyList() }
    var settingChosen by remember { mutableStateOf<String?>(null) }
    val settingIndex = settingItems.indexOfFirst { it.id == settingChosen }.takeIf { it >= 0 } ?: 0
    val currentSettings by rememberUpdatedState(settingItems)
    fun changeSetting(id: String) {
        settingChosen = id
        when (val item = currentSettings.firstOrNull { it.id == id }) {
            // S-1: a switch that cannot be turned on now (passenger mode while moving) stays off.
            is SettingItem.Toggle -> if (item.on || item.canTurnOn) item.set(!item.on) else return
            is SettingItem.Choice -> if (item.options.isNotEmpty()) item.select(item.next)
            null -> return
        }
        settingsVersion++
    }
    // N6: the Apps world: which app holds the ring (over all pages), and where this page's discs are.
    // F2-1: the page on screen was opened from an Apps tile (Back returns to the Apps world). Any
    // other way out of it forgets that: home, the dial, another world.
    var fromApps by memory.fromApps
    // F3: the Apps world opened as a fallback shows only the kinds asked for (maps apps, music apps).
    var appsKinds by memory.appsKinds
    LaunchedEffect(model.place) { if (model.place != Place.InWorld(World.APPS)) appsKinds = null }
    LaunchedEffect(model.place, listOpen) {
        val p = model.place
        val onTilePage = (p is Place.InWorld && p.world in setOf(World.SETTINGS, World.CAR, World.CALLS)) || (p == Place.Home && listOpen)
        if (!onTilePage && p != Place.InWorld(World.APPS)) fromApps = false
    }
    val apps = sources.apps.apps.let { all -> appsKinds?.let { k -> all.filter { it.kind in k }.ifEmpty { null } } ?: all }
    val perPage = skin.appsPerPage
    var appRing by memory.appRing
    val appIndex = appRing.coerceIn(0, (apps.size - 1).coerceAtLeast(0))
    val page = AppsPages.pageOf(appIndex, perPage)
    val pageApps = apps.drop(page * perPage).take(perPage)
    val appBoxes = remember(page) { mutableStateMapOf<String, FocusGraph.Box>() }
    fun padApps(dir: FocusGraph.Direction) {
        // N6-3: before this page is laid out there is nothing to move to: wait rather than turn the page.
        if (appBoxes.isEmpty()) return
        val next = FocusGraph.pad(appBoxes, appIndex.toString(), dir)?.toIntOrNull()
        val pages = AppsPages.pages(apps.size, perPage)
        appRing = when {
            next != null -> next
            // Off the right or left edge: the next or the previous page.
            dir == FocusGraph.Direction.RIGHT && page + 1 < pages -> (page + 1) * perPage
            dir == FocusGraph.Direction.LEFT && page > 0 -> page * perPage - 1
            else -> appIndex
        }
    }
    /** A world just entered starts with its ring on its first stop. */
    fun freshWorld(w: World) {
        when (w) {
            World.APPS -> appRing = 0
            World.MUSIC -> musicChosen = MUSIC_PLAY
            World.CALLS -> phoneChosen = null
            World.SETTINGS -> settingChosen = null
            World.CAR -> carChosen = null
            World.NAVIGATE -> Unit
        }
    }
    // N13: the hand's push. Navigate is the maps app itself (as on the dial, N9); the others open their world.
    openFront = {
        val w = model.front
        if (w == World.NAVIGATE) openMaps() else { model.enter(w); freshWorld(w) }
    }
    // F4, the owner's first drive: in a call, the strip opens the Calls world (recent calls,
    // favourites, the phone app last), not the phone app itself.
    openCalls = { model.enter(World.CALLS); freshWorld(World.CALLS) }
    // F4: a world the car runtime asks for (a call placed from an app opens the Calls world).
    val openWorld = state.engineOpenWorld
    LaunchedEffect(openWorld) {
        val w = World.entries.firstOrNull { it.id == openWorld } ?: return@LaunchedEffect
        fromApps = false // F3-1: not opened from an Apps tile, whatever was on screen before
        model.enter(w)
        freshWorld(w)
        state.engineOpenWorld = null
    }
    /** F3: the Apps world with only apps of [kinds] (all apps when there are none of them). */
    fun showApps(kinds: Set<CarAppKind>) {
        model.enter(World.APPS)
        freshWorld(World.APPS)
        appsKinds = kinds
    }
    // F3, the owner's first drive: with no maps app (or no music app) known, the old app grid opened;
    // now the Apps world does, with the maps apps (or the music apps) to choose from.
    openMaps = {
        if (!sources.picture && state.app(state.homeNavAppId ?: state.activeNavAppId) == null) showApps(setOf(CarAppKind.NAVIGATION))
        else sources.openMaps()
    }
    openMusic = {
        if (!sources.picture && state.app(state.nowPlaying?.appId ?: state.activeMediaAppId) == null) showApps(setOf(CarAppKind.MEDIA))
        else sources.music.open()
    }
    /**
     * F2, the owner's first drive: an Apps world tile. MyPlay's built-in screens that the engine has
     * its own page for open that page, not the old screen: Settings, the car's data, the phone (the
     * Calls world) and the notifications (the bell's list). Any other app opens full screen (in the
     * engine's frame, F1), the phone's screen and the browser among them.
     */
    fun openTile(app: CarAppEntry) {
        fromApps = app.kind in setOf(CarAppKind.SETTINGS, CarAppKind.CAR_DATA, CarAppKind.PHONE, CarAppKind.NOTIFICATIONS)
        val world = when (app.kind) {
            CarAppKind.SETTINGS -> World.SETTINGS
            CarAppKind.CAR_DATA -> World.CAR
            CarAppKind.PHONE -> World.CALLS
            else -> null
        }
        when {
            world != null -> { model.enter(world); freshWorld(world) }
            app.kind == CarAppKind.NOTIFICATIONS -> {
                model.place = Place.Home
                listOpen = true
                listChosen = null
                notes.markSeen()
            }
            else -> {
                memory.origin = appBoxes[apps.indexOfFirst { it.id == app.id }.toString()]
                sources.apps.open(app)
            }
        }
    }

    // O3-2: Home asked for, here or already on MyPlay's screen (the Menu button at home): the plain
    // home now, and the flag cleared at once, so a later Back from an app is not taken for Home.
    LaunchedEffect(state.engineHomeAsked) {
        if (!state.engineHomeAsked) return@LaunchedEffect
        state.engineHomeAsked = false
        if (!keeps) return@LaunchedEffect
        model.place = Place.Home
        chosen = STOP_HUB
        panelOpen = false
        listOpen = false
        fromApps = false
    }
    // N6-2: taps run what the screen shows now, not what it showed when the tap handler started.
    val currentActions by rememberUpdatedState(actionsOf)
    val currentApps by rememberUpdatedState(apps)
    val currentMusicActions by rememberUpdatedState(musicActions)
    val covers = remember { MapCoverRegistry() }
    val focus = remember { FocusRequester() }
    // Not in the phone's picture of the car screen: it must not pull focus into the phone's own UI.
    LaunchedEffect(Unit) { if (!sources.picture) runCatching { focus.requestFocus() } }
    BackHandler(enabled = model.place != Place.Home) {
        // F2-1: a page opened from an Apps tile goes back to the Apps world, the ring on that tile.
        if (fromApps && model.place != Place.InWorld(World.APPS)) { fromApps = false; model.enter(World.APPS) } else model.back()
    }
    BackHandler(enabled = listOpen) {
        listOpen = false
        if (fromApps) { fromApps = false; model.enter(World.APPS) }
    }
    // The list's rows: one per notification (keyed by it, so new ones arriving do not move the ring), then Clear all.
    val driving = state.drivingRestricted
    val listKeys: List<String> = notes.history.map { "n:${it.key}" } + CLEAR_ALL_KEY
    val listIndex = listKeys.indexOf(listChosen).takeIf { it >= 0 } ?: 0
    fun runListRow(key: String) {
        listChosen = key
        if (key == CLEAR_ALL_KEY) { notes.clearAll(); listOpen = false; return }
        notes.history.firstOrNull { "n:${it.key}" == key }?.actions?.firstOrNull()?.second?.invoke()
    }
    // O1: Back closes the music panel, the ring back on the song.
    BackHandler(enabled = panelOpen) { panelOpen = false; chosen = STOP_SONG }
    // Declared later, so it comes first: Back goes up the library, then out of it.
    BackHandler(enabled = lib != null) { if (lib?.canGoBack == true) lib.back() else library = null }

    /** O6: what a home pad key does while the ring is hidden. */
    fun runPad(action: HomePadAction) {
        when (action) {
            HomePadAction.MUSIC -> if (slots.song != null) { panelOpen = true; panelChosen = PANEL_PLAY } else { model.enter(World.MUSIC); freshWorld(World.MUSIC) }
            HomePadAction.MAPS -> { memory.origin = null; openMaps() }
            HomePadAction.DIAL -> model.push()
            HomePadAction.APPS -> { model.enter(World.APPS); freshWorld(World.APPS) }
            HomePadAction.CALLS -> openCalls()
            HomePadAction.NOTHING -> Unit
        }
    }

    fun onKey(key: Key, shift: Boolean): Boolean = if (ringing) {
        // R5-2: the engine's own ring is left where it was, so it is there again once the call is answered or declined.
        val live = callBoxes.filterKeys { it in callActions }
        when (key) {
            Key.Tab -> FocusGraph.knob(live, callRing, if (shift) -1 else 1)?.let { callChosen = it }
            Key.DirectionLeft -> FocusGraph.pad(live, callRing, FocusGraph.Direction.LEFT)?.let { callChosen = it }
            Key.DirectionRight -> FocusGraph.pad(live, callRing, FocusGraph.Direction.RIGHT)?.let { callChosen = it }
            Key.Enter, Key.DirectionCenter, Key.NumPadEnter -> callActions[callRing]?.invoke()
            else -> Unit
        }
        key in WORLD_KEYS
    } else if (listOpen) {
        val n = listKeys.size
        when (key) {
            Key.Tab -> listChosen = listKeys[(listIndex + if (shift) -1 else 1).coerceIn(0, n - 1)]
            Key.DirectionUp -> listChosen = listKeys[(listIndex - 1).coerceAtLeast(0)]
            Key.DirectionDown -> listChosen = listKeys[(listIndex + 1).coerceAtMost(n - 1)]
            Key.Enter, Key.DirectionCenter, Key.NumPadEnter -> runListRow(listKeys[listIndex])
            else -> Unit
        }
        key in WORLD_KEYS
    } else if (panelOpen) {
        when (key) {
            Key.Tab -> FocusGraph.knob(panelBoxes.filterKeys { it in panelActions }, panelRing, if (shift) -1 else 1)?.let { panelChosen = it }
            Key.DirectionUp -> padPanel(FocusGraph.Direction.UP)
            Key.DirectionDown -> padPanel(FocusGraph.Direction.DOWN)
            Key.DirectionLeft -> padPanel(FocusGraph.Direction.LEFT)
            Key.DirectionRight -> padPanel(FocusGraph.Direction.RIGHT)
            Key.Enter, Key.DirectionCenter, Key.NumPadEnter -> panelActions[panelRing]?.invoke()
            else -> Unit
        }
        key in WORLD_KEYS
    } else when (val p = model.place) {
        Place.Home -> if (padHome && !ringAwake && notice == null && call == null && padKeyOf(key)?.let { padMap[it] != HomePadAction.NOTHING } == true) {
            runPad(padMap.getValue(padKeyOf(key)!!))
            true
        } else if (padHome && !ringAwake && notice == null && (key == Key.Tab || padKeyOf(key) != null)) {
            // The first turn only shows the ring, where it was. O6-1: so does a pad key set to
            // nothing: it never moves the ring while it is hidden.
            ringAwake = true
            true
        } else when (key) {
            Key.Tab -> { if (hand != null && ring == STOP_HUB) swing(if (shift) -1 else 1, knob = true) else move(FocusGraph.knob(live, ring, if (shift) -1 else 1)); true }
            // N13: on the hand, left and right swing it; past its ends they move on as elsewhere.
            Key.DirectionLeft, Key.DirectionRight -> if (hand != null && ring == STOP_HUB) {
                swing(if (key == Key.DirectionRight) 1 else -1, knob = false)
                true
            } else if (key == Key.DirectionLeft) {
                move(FocusGraph.pad(live, ring, FocusGraph.Direction.LEFT))
                true
            } else {
                // N6, the ORBIT sheet: pad right is the next song. N6-1: it first moves the ring, as the
                // other directions do (else a car with a pad and no knob could never reach the song);
                // with nothing further right (the ring on the song) it plays the next song.
                val to = FocusGraph.pad(live, ring, FocusGraph.Direction.RIGHT)
                if (to != null) move(to) else if (slots.song != null && hand == null) sources.music.next()
                true
            }
            Key.DirectionUp -> { move(FocusGraph.pad(live, ring, FocusGraph.Direction.UP)); true }
            Key.DirectionDown -> { move(FocusGraph.pad(live, ring, FocusGraph.Direction.DOWN)); true }
            Key.Enter, Key.DirectionCenter, Key.NumPadEnter -> { actionsOf[ring]?.invoke(); true }
            else -> false
        }
        // O7: a dial with a world on each pad direction: one press opens that world.
        is Place.Chooser -> if (skin.dial != null && dialIndex(key) != null) {
            skin.dial!!.getOrNull(dialIndex(key)!!)?.let { w -> model.choose(w); freshWorld(w) }
            true
        } else when (key) {
            Key.Tab -> { model.turn(if (shift) -1 else 1); true }
            Key.DirectionLeft, Key.DirectionUp -> { model.turn(-1); true }
            Key.DirectionRight, Key.DirectionDown -> { model.turn(1); true }
            Key.Enter, Key.DirectionCenter, Key.NumPadEnter -> {
                if (p.selected == World.NAVIGATE) {
                    // N9, the dispatcher: Navigate is the maps app itself, full screen with its own search
                    // and destinations (the last used one); Back from it comes home. The dial closes.
                    model.back()
                    memory.origin = null
                    openMaps()
                } else {
                    model.push()
                    freshWorld(p.selected)
                }
                true
            }
            else -> false
        }
        // N2-1: until the worlds draw their own stops, a world keeps the knob and pad (only Back,
        // through the BackHandler, leaves), so a turn cannot carry focus off the engine.
        is Place.InWorld -> if (p.world == World.MUSIC && lib != null) {
            val n = lib.rows.size
            when (key) {
                Key.Tab -> if (n > 0) libraryRing = (libraryRing + if (shift) -1 else 1).coerceIn(0, n - 1)
                Key.DirectionUp -> libraryRing = (libraryRing - 1).coerceAtLeast(0)
                Key.DirectionDown -> if (n > 0) libraryRing = (libraryRing + 1).coerceAtMost(n - 1)
                Key.Enter, Key.DirectionCenter, Key.NumPadEnter -> openRow(libraryRing)
                else -> Unit
            }
            key in WORLD_KEYS
        } else if (p.world == World.MUSIC) {
            when (key) {
                Key.Tab -> FocusGraph.knob(musicBoxes.filterKeys { it in musicActions }, musicRing, if (shift) -1 else 1)?.let { musicChosen = it }
                Key.DirectionUp -> padMusic(FocusGraph.Direction.UP)
                Key.DirectionDown -> padMusic(FocusGraph.Direction.DOWN)
                Key.DirectionLeft -> padMusic(FocusGraph.Direction.LEFT)
                Key.DirectionRight -> padMusic(FocusGraph.Direction.RIGHT)
                Key.Enter, Key.DirectionCenter, Key.NumPadEnter -> musicActions[musicRing]?.invoke()
                else -> Unit
            }
            key in WORLD_KEYS
        } else if (p.world == World.CAR) {
            val labels = carLabels
            val n = labels.size
            val i = labels.indexOf(carChosen).takeIf { it >= 0 } ?: 0
            if (n > 0) when (key) {
                Key.Tab -> carChosen = labels[(i + if (shift) -1 else 1).coerceIn(0, n - 1)]
                Key.DirectionUp -> carChosen = labels[(i - 1).coerceAtLeast(0)]
                Key.DirectionDown -> carChosen = labels[(i + 1).coerceAtMost(n - 1)]
                else -> Unit
            }
            key in WORLD_KEYS
        } else if (p.world == World.SETTINGS) {
            val n = settingItems.size
            if (n > 0) when (key) {
                Key.Tab -> settingChosen = settingItems[(settingIndex + if (shift) -1 else 1).coerceIn(0, n - 1)].id
                Key.DirectionUp -> settingChosen = settingItems[(settingIndex - 1).coerceAtLeast(0)].id
                Key.DirectionDown -> settingChosen = settingItems[(settingIndex + 1).coerceAtMost(n - 1)].id
                Key.Enter, Key.DirectionCenter, Key.NumPadEnter -> changeSetting(settingItems[settingIndex].id)
                else -> Unit
            }
            key in WORLD_KEYS
        } else if (p.world == World.CALLS) {
            val n = phoneRows.size
            when (key) {
                Key.Tab -> movePhone(phoneIndex + if (shift) -1 else 1)
                Key.DirectionUp -> movePhone(phoneIndex - 1)
                Key.DirectionDown -> movePhone(phoneIndex + 1)
                Key.Enter, Key.DirectionCenter, Key.NumPadEnter -> openPhone(phoneKeys[phoneIndex.coerceAtMost(n - 1)])
                else -> Unit
            }
            key in WORLD_KEYS
        } else if (p.world == World.APPS) {
            when (key) {
                Key.Tab -> appRing = AppsPages.knob(appIndex, if (shift) -1 else 1, apps.size)
                Key.DirectionUp -> padApps(FocusGraph.Direction.UP)
                Key.DirectionDown -> padApps(FocusGraph.Direction.DOWN)
                Key.DirectionLeft -> padApps(FocusGraph.Direction.LEFT)
                Key.DirectionRight -> padApps(FocusGraph.Direction.RIGHT)
                Key.Enter, Key.DirectionCenter, Key.NumPadEnter -> apps.getOrNull(appIndex)?.let(::openTile)
                else -> Unit
            }
            key in WORLD_KEYS
        } else {
            key in WORLD_KEYS
        }
    }

    CompositionLocalProvider(LocalSkinTokens provides tokens) {
        BoxWithConstraints(
            Modifier.fillMaxSize().background(tokens.background).then(modifier).testTag(ENGINE_TAG)
                .focusRequester(focus)
                .onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown) inputs++; e.type == KeyEventType.KeyDown && onKey(e.key, e.isShiftPressed) }
                // O2: a touch anywhere is input too (only looked at, never taken).
                .pointerInput(Unit) { awaitPointerEventScope { while (true) { awaitPointerEvent(PointerEventPass.Initial); inputs++ } } }
                .focusable(),
        ) {
            // N3: the maps app is told what the slots cover (as the spec layouts do, M2), and gets the
            // skin's colour filter while the look sets none.
            CompositionLocalProvider(LocalMapCovers provides covers) {
                val nav = state.app(state.homeNavAppId)
                // O9: while the skin shows the next turn itself, the maps app's turn card is left out on the
                // home. O9-1: only then: a maps app whose turn reaches MyPlay only through its card keeps it.
                CompositionLocalProvider(LocalMapTapIsKnobStop provides false, LocalSkinMapFilter provides tokens.mapFilter,
                    net.prototux.aare.host.templates.LocalOwnTurnShown provides (slots.turn != null)) {
                    if (nav != null) navMap(nav, Modifier.fillMaxSize()) else Wallpaper(state, Modifier.fillMaxSize())
                }
                val edge = Modifier.padding(tokens.edge).widthIn(max = maxWidth * 0.4f)
                val showRing = model.place == Place.Home && CarFocusVisibility.keyInput && !panelOpen &&
                    (!padHome || ringAwake || notice != null || call != null)
                // O4: shades only behind the parts shown (DECK, frozen, keeps both).
                skin.Backdrop(if (hand != null) BackdropParts(turn = true, song = true)
                    else BackdropParts(turn = slots.turn != null && model.place !is Place.InWorld, song = slots.song != null && model.place !is Place.InWorld && !listOpen),
                    Modifier.fillMaxSize())
                @Composable
                fun Modifier.slot(slot: Slot): Modifier = if (skin.coversMap(slot)) coversMap() else this
                fun Modifier.stop(id: String): Modifier = this
                    // A tap does what a push does. Not clickable(): that would make the slot a
                    // Compose focus stop, and a knob turn could then leave the engine (N2-1).
                    .pointerInput(id) { detectTapGestures { chosen = id; currentActions[id]?.invoke() } }
                    .onGloballyPositioned { c -> c.boundsInRoot().let { boxes[id] = FocusGraph.Box(it.left, it.top, it.right, it.bottom) } }
                    .semantics { selected = ring == id }
                    .let { if (showRing && ring == id && !skin.drawsFocus) it.border(3.dp, tokens.accent, RoundedCornerShape(tokens.corner)) else it }
                    .testTag(id)
                @Composable
                fun ringed(id: String, content: @Composable () -> Unit) =
                    CompositionLocalProvider(LocalStopRinged provides (showRing && ring == id), content = content)
                // Inside a world only the status corner stays: the world's page has the screen.
                // (and over the notifications list, likewise)
                val home = model.place !is Place.InWorld && !listOpen
                if (home && hand != null) {
                    // N13: with a hand, the trip goes under the turn read-out (there is no hub to carry it).
                    Column(edge.align(Alignment.TopStart)) {
                        slots.turn?.let { ringed(STOP_TURN) { skin.Turn(it, Modifier.slot(Slot.TURN).stop(STOP_TURN)) } }
                        slots.trip?.let { skin.Trip(it, Modifier.slot(Slot.TURN).testTag(SLOT_TRIP)) }
                    }
                } else if (home) slots.turn?.let { ringed(STOP_TURN) { skin.Turn(it, edge.align(Alignment.TopStart).slot(Slot.TURN).stop(STOP_TURN)) } }
                Column(edge.align(Alignment.TopEnd), horizontalAlignment = Alignment.End) {
                    skin.Status(Modifier.slot(Slot.STATUS).testTag(SLOT_STATUS))
                    if (STOP_BELL in actionsOf) {
                        ringed(STOP_BELL) { skin.Bell(notes.unseen, LocalStopRinged.current, Modifier.slot(Slot.STATUS).stop(STOP_BELL)) }
                    }
                    if (skin.showsSpeed && !(skin.speedInHub && hand == null) && state.speedSource != SpeedSourceMode.OFF) {
                        EngineSpeed(state, sources.speed) { skin.Speed(it, Modifier.slot(Slot.SPEED).testTag(SLOT_SPEED)) }
                    }
                }
                if (home && hand != null) {
                    // N13: the hand, bottom right; it is the hub's stop (a push opens the front card).
                    ringed(STOP_HUB) {
                        skin.Hand(model.front, slots.song, LocalStopRinged.current, { w -> chosen = STOP_HUB; if (w == model.front) openFront() else model.front = w },
                            edge.align(Alignment.BottomEnd).slot(Slot.HUB).stop(STOP_HUB))
                    }
                } else if (home) ringed(STOP_HUB) {
                    val hubMod = edge.align(Alignment.BottomStart).slot(Slot.HUB).stop(STOP_HUB)
                    // O8: the speed and its limit in the hub (ORBIT), read only while shown.
                    if (skin.speedInHub && skin.showsSpeed && state.speedSource != SpeedSourceMode.OFF) {
                        EngineSpeed(state, sources.speed) { skin.HubSpeed(slots.trip, it, hubMod) }
                    } else skin.Hub(slots.trip, hubMod)
                }
                // N8: at the top of the home: in a call its strip, and a notice under it; their parts are home stops.
                Column(Modifier.align(Alignment.TopCenter).padding(top = tokens.edge), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (model.place == Place.Home && call != null && !ringing) {
                        skin.InCall(call, canEnd, ring.takeIf { showRing }, { id -> Modifier.stop(id) }, Modifier)
                    }
                    if (notice != null && !ringing) {
                        val atHome = model.place == Place.Home
                        // Away from the home the notice only shows (its stops are the home's).
                        skin.Notice(notice, hideText = state.drivingRestricted && notice.message, ring.takeIf { showRing && atHome },
                            { id -> if (atHome) Modifier.stop(id) else Modifier.testTag(id) }, Modifier.padding(top = 12.dp))
                    }
                    // The gesture line (R6), under the call's strip and the notice (R6-3: never on top of them).
                    if (model.place == Place.Home) GestureLineOverlay(state, Modifier)
                }
                if (home && hand == null) slots.song?.let { song ->
                    // O1: the music panel opens above the song, in the same corner.
                    Column(edge.align(Alignment.BottomEnd), horizontalAlignment = Alignment.End) {
                        if (panelOpen) {
                            val currentPanel by rememberUpdatedState(panelActions)
                            skin.MusicPanel(musicPage, panelRing, { id ->
                                Modifier
                                    .pointerInput(id) { detectTapGestures { panelChosen = id; currentPanel[id]?.invoke() } }
                                    .onGloballyPositioned { c -> c.boundsInRoot().let { panelBoxes[id] = FocusGraph.Box(it.left, it.top, it.right, it.bottom) } }
                                    .semantics { selected = id == panelRing }
                                    .testTag(id)
                            }, Modifier.slot(Slot.SONG).testTag(SLOT_MUSIC_PANEL))
                        }
                        ringed(STOP_SONG) { skin.Song(song, Modifier.slot(Slot.SONG).stop(STOP_SONG)) }
                    }
                }
                when (val p = model.place) {
                    is Place.Chooser -> skin.Chooser(p.selected, Modifier.align(Alignment.Center).slot(Slot.CHOOSER).testTag(SLOT_CHOOSER))
                    is Place.InWorld -> if (p.world == World.MUSIC && lib != null) {
                        val currentLib by rememberUpdatedState(lib)
                        skin.Library(
                            LibraryPage(lib.title, lib.rows, lib.loading, lib.error),
                            libraryRing.coerceIn(0, (lib.rows.size - 1).coerceAtLeast(0)),
                            { row, px -> currentLib.art(row, px) },
                            { i ->
                                Modifier
                                    .pointerInput(lib.key, i) { detectTapGestures { openRow(i) } }
                                    .semantics { selected = i == libraryRing }
                                    .testTag(libraryRowTag(i))
                            },
                            Modifier.fillMaxSize().testTag(SLOT_LIBRARY),
                        )
                    } else if (p.world == World.MUSIC) {
                        skin.MusicWorld(
                            musicPage, musicRing,
                            { id ->
                                Modifier
                                    .pointerInput(id) { detectTapGestures { musicChosen = id; currentMusicActions[id]?.invoke() } }
                                    .onGloballyPositioned { c -> c.boundsInRoot().let { musicBoxes[id] = FocusGraph.Box(it.left, it.top, it.right, it.bottom) } }
                                    .semantics { selected = id == musicRing }
                                    .testTag(id)
                            },
                            Modifier.fillMaxSize().testTag(SLOT_MUSIC),
                        )
                    } else if (p.world == World.CAR) {
                        val i = carLines.indexOfFirst { it.first == carChosen }.takeIf { it >= 0 } ?: 0
                        skin.CarWorld(carLines, i, { r ->
                            Modifier.semantics { selected = r == i }.testTag(carLineTag(r))
                        }, Modifier.fillMaxSize().testTag(SLOT_CAR))
                    } else if (p.world == World.SETTINGS) {
                        val on = stringResource(R.string.myplay_on)
                        val off = stringResource(R.string.myplay_off)
                        val rows = settingItems.map {
                            when (it) {
                                is SettingItem.Toggle -> SettingRow(it.title, if (it.on) on else off, switch = true, on = it.on, note = it.note)
                                is SettingItem.Choice -> SettingRow(it.title, it.label, switch = false, on = false)
                            }
                        }
                        skin.SettingsWorld(SettingsPage(rows), settingIndex, { i ->
                            val id = settingItems[i].id
                            Modifier
                                .pointerInput(id) { detectTapGestures { changeSetting(id) } }
                                .semantics { selected = i == settingIndex }
                                .testTag(settingRowTag(id))
                        }, Modifier.fillMaxSize().testTag(SLOT_SETTINGS))
                    } else if (p.world == World.CALLS) {
                        skin.PhoneWorld(
                            PhonePage(phoneRows, loading = phoneData == null && phone.permitted, permitted = phone.permitted),
                            phoneIndex,
                            { i ->
                                Modifier
                                    // P-2: keyed on the entry, and resolved by it when tapped.
                                    .let { m ->
                                        val key = phoneKeys[i]
                                        m.pointerInput(key) { detectTapGestures { if (key in currentPhoneKeys) openPhone(key) } }
                                    }
                                    .semantics { selected = i == phoneIndex }
                                    .testTag(phoneRowTag(i))
                            },
                            Modifier.fillMaxSize().testTag(SLOT_PHONE),
                        )
                    } else if (p.world == World.APPS) {
                        val first = page * perPage
                        skin.AppsWorld(
                            pageApps, appIndex - first, page, AppsPages.pages(apps.size, perPage),
                            { i ->
                                val global = first + i
                                Modifier
                                    // N6-2: keyed on the app, and opening the app shown now, should the list change.
                                    .let { m ->
                                        val id = apps[global].id
                                        m.pointerInput(id) {
                                            detectTapGestures { currentApps.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { appRing = it; openTile(currentApps[it]) } }
                                        }
                                    }
                                    .onGloballyPositioned { c -> c.boundsInRoot().let { appBoxes[global.toString()] = FocusGraph.Box(it.left, it.top, it.right, it.bottom) } }
                                    .semantics { selected = global == appIndex }
                                    .testTag(appTag(apps[global]))
                            },
                            Modifier.fillMaxSize().testTag(SLOT_APPS),
                        )
                    } else {
                        skin.WorldPlaceholder(p.world, Modifier.align(Alignment.Center).slot(Slot.WORLD).testTag(SLOT_WORLD))
                    }
                    Place.Home -> Unit
                }
            }
            overlay(Modifier.align(Alignment.BottomCenter))
            // O3: the app shrinking back into the stop it came from.
            if (keeps) ShrinkBack(memory, Modifier)
            // The gesture line (R6) over a world's page or the dial (at home it is under the notice, above).
            if (model.place != Place.Home) GestureLineOverlay(state, Modifier.align(Alignment.TopCenter))
            // N8: the notifications list, over the home.
            if (listOpen && !ringing) {
                val currentKeys by rememberUpdatedState(listKeys)
                val time = { t: Long -> android.text.format.DateUtils.getRelativeTimeSpanString(t, System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS, android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE).toString() }
                val hidden = stringResource(R.string.notif_new_message_placeholder)
                val rows = notes.history.map { NoticeRow(it, if (driving) hidden else it.text, time(it.timeMs), it.actions.firstOrNull()?.first) } +
                    NoticeRow(null, "", "", null)
                skin.NoticeList(NoticeListPage(rows), listIndex, { i ->
                    val key = listKeys[i]
                    Modifier
                        .pointerInput(key) { detectTapGestures { if (key in currentKeys) runListRow(key) } }
                        .semantics { selected = i == listIndex }
                        .testTag(noticeRowTag(i))
                }, Modifier.fillMaxSize().testTag(SLOT_NOTICES)
                    .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } })
            }
            // N8: a ringing call, drawn by the skin over everything.
            if (ringing) {
                val currentCall by rememberUpdatedState(callActions)
                skin.Ringing(call!!, canEnd, callRing, { id ->
                    Modifier
                        .pointerInput(id) { detectTapGestures { callChosen = id; currentCall[id]?.invoke() } }
                        .onGloballyPositioned { c -> c.boundsInRoot().let { callBoxes[id] = FocusGraph.Box(it.left, it.top, it.right, it.bottom) } }
                        .semantics { selected = id == callRing }
                        .testTag(id)
                }, Modifier.fillMaxSize().testTag(SLOT_CALL)
                    // C-1: the call covers the screen: a touch anywhere else on it stops here and never
                    // reaches the home's stops underneath (its own buttons see their touches first).
                    .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } })
            }
        }
    }
}

/**
 * N3: the speed, read only while this is composed (the skin draws a speed): the car's, else the
 * phone's passive fixes under the battery rules ([SpeedFeed]); everything stops when it leaves.
 */
@Composable
private fun EngineSpeed(state: CarUiState, source: SpeedSource, content: @Composable (SpeedReading?) -> Unit) {
    // The phone's picture shows a fixed speed and never listens to anything.
    source.fixed?.let { content(it); return }
    val limits = remember(source) { source.limits() }
    val feed = remember(source) { SpeedFeed(source.fixes(), limits) }
    val car by source.car.collectAsState()
    // N20: whether the phone keeps a speed limit file (looked at off the main thread, now and then:
    // the phone may download one while the car is connected).
    val limitFile by produceState(LimitFile.NO_FILE, limits, state.speedSource) {
        value = LimitFile.NO_FILE
        if (limits == null || state.speedSource != SpeedSourceMode.AUTO) return@produceState
        while (true) {
            value = withContext(Dispatchers.IO) { limits.file() }
            delay(LIMITS_RECHECK_MS)
        }
    }
    val inputs = SpeedFeed.Inputs(state.speedSource, shown = true, mapsRunning = state.activeNavAppId != null, car = car,
        limitsOn = limitFile == LimitFile.READY, limitFile = limitFile)
    // The owner's drive: why the limit shows or not goes in the connection log (the Car check) when it
    // changes, and into the Car world. Never a position, only the reason.
    val why = feed.why
    LaunchedEffect(why) {
        SpeedLimitDiag.why = why
        // WD-1: only a reason that held a while goes in the log, so a limit flickering at a junction
        // does not push the rest of the drive's lines out (the log keeps 1000).
        delay(WHY_LOG_AFTER_MS)
        if (why != null) net.prototux.aare.DiagLog.add("Speed limit: ${why.line}")
    }
    // Again each second, so a speed the car stopped sending goes away.
    LaunchedEffect(feed, inputs) {
        while (true) {
            feed.update(inputs.copy(margin = source.margin()))
            delay(SPEED_TICK_MS)
        }
    }
    DisposableEffect(feed) { onDispose { feed.release(); limits?.close(); SpeedLimitDiag.why = null } }
    content(feed.reading)
}

private const val SPEED_TICK_MS = 1_000L

/** N20: the speed limit's last reason (while a speed is on the screen), for the Car world's line. */
internal object SpeedLimitDiag {
    var why by mutableStateOf<LimitWhy?>(null)
}
private const val LIMITS_RECHECK_MS = 60_000L
private const val WHY_LOG_AFTER_MS = 10_000L

/** How long a notice shows (as the car's notice overlay). */
internal const val NOTICE_SHOW_MS = 8_000L

/** The keys a world not drawn yet takes for itself (N2-1). */
private val WORLD_KEYS = setOf(
    Key.Tab, Key.Enter, Key.DirectionCenter, Key.NumPadEnter,
    Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight,
)

internal const val ENGINE_TAG = "engine_root"
/** The home's stops; also their test tags. */
internal const val STOP_TURN = "stop_turn"
internal const val STOP_HUB = "stop_hub"
internal const val STOP_SONG = "stop_song"
/** N8: in a call, the strip (opens the phone) and Hang up. */
const val STOP_CALL = "stop_call"
const val STOP_HANG_UP = "stop_hang_up"
/** N8: the bell under the clock (while there are unseen notifications). */
const val STOP_BELL = "stop_bell"
internal const val SLOT_STATUS = "slot_status"
internal const val SLOT_SPEED = "slot_speed"
/** N13: the trip under the turn read-out (a skin with a hand; the hand itself is [STOP_HUB]). */
internal const val SLOT_TRIP = "slot_trip"
internal const val SLOT_CHOOSER = "slot_chooser"
internal const val SLOT_WORLD = "slot_world"
internal const val SLOT_APPS = "slot_apps"
internal const val SLOT_MUSIC = "slot_music"
/** O1: the music panel over the home. */
internal const val SLOT_MUSIC_PANEL = "slot_music_panel"
internal const val SLOT_CALL = "slot_call"
internal const val SLOT_PHONE = "slot_phone"
internal const val SLOT_NOTICES = "slot_notices"
internal const val SLOT_SETTINGS = "slot_settings"
internal const val SLOT_CAR = "slot_car"

/** A line of the Car world (its test tag). */
internal fun carLineTag(i: Int) = "car_line_$i"

/** A setting's row (its test tag). */
internal fun settingRowTag(id: String) = "setting_$id"
private const val CLEAR_ALL_KEY = "clear_all"

/** A row of the notifications list (its test tag). */
internal fun noticeRowTag(i: Int) = "notice_row_$i"
private const val PHONE_APP_KEY = "phone_app"

/** A row of the Calls world (its test tag). */
internal fun phoneRowTag(i: Int) = "phone_row_$i"
internal const val SLOT_LIBRARY = "slot_library"

/** A row of the library (its test tag). */
internal fun libraryRowTag(i: Int) = "library_row_$i"

/** An app's disc in the Apps world (its test tag). */
internal fun appTag(app: CarAppEntry) = "app_${app.id}"

/** O6: the home pad key a key is, or null. */
internal fun padKeyOf(key: Key): HomePadKey? = when (key) {
    Key.Enter, Key.DirectionCenter, Key.NumPadEnter -> HomePadKey.CENTER
    Key.DirectionUp -> HomePadKey.UP
    Key.DirectionDown -> HomePadKey.DOWN
    Key.DirectionLeft, Key.DirectionRight -> HomePadKey.SIDE
    else -> null
}

/** O7: the dial's world index for a pad direction: up, right, down, left (clockwise from the top). */
internal fun dialIndex(key: Key): Int? = when (key) {
    Key.DirectionUp -> 0
    Key.DirectionRight -> 1
    Key.DirectionDown -> 2
    Key.DirectionLeft -> 3
    else -> null
}
