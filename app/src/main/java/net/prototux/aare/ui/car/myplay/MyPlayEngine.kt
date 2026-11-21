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
import net.prototux.aare.ui.car.CarFocusVisibility
import net.prototux.aare.ui.car.CarScreen
import net.prototux.aare.ui.car.CarUiState
import net.prototux.aare.ui.car.LocalMapTapIsKnobStop
import net.prototux.aare.ui.car.SpecShell
import net.prototux.aare.ui.car.GestureLineOverlay
import net.prototux.aare.ui.car.Wallpaper

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
    if (state.screen != CarScreen.Dashboard) {
        SpecShell(state, actions, appContent, navMap, overlay, modifier)
        return
    }
    val model = remember { MyPlayScreenModel() }
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
    var chosen by remember { mutableStateOf(STOP_HUB) }
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
    val actionsOf: Map<String, () -> Unit> = buildMap {
        if (notice != null && model.place == Place.Home) {
            // At most two actions: two fit in the room at the top (a phone's notice can carry more; the
            // rest stay on the phone, and in the notifications list).
            notice.actions.take(2).forEachIndexed { i, (_, run) -> put(noticeActionStop(i)) { run(); sources.notifications.dismiss(notice) } }
            put(NOTICE_DISMISS) { sources.notifications.dismiss(notice) }
        }
        if (call != null && !ringing) {
            put(STOP_CALL, sources.phone::open)
            if (canEnd) put(STOP_HANG_UP, sources.calls::end)
        }
        if (slots.turn != null) put(STOP_TURN, sources.openMaps)
        put(STOP_HUB) { model.push() }
        if (slots.song != null) put(STOP_SONG, sources.music::open)
    }
    // A stop that went away (the song ended) leaves the graph; the ring goes back to the hub.
    val live = boxes.filterKeys { it in actionsOf }
    val ring = chosen.takeIf { it in actionsOf } ?: STOP_HUB
    fun move(to: String?) { if (to != null) chosen = to }
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
        put(MUSIC_LIBRARY) { music.library()?.let { library = it } ?: music.open() }
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
    // N6: the Apps world: which app holds the ring (over all pages), and where this page's discs are.
    val apps = sources.apps.apps
    val perPage = skin.appsPerPage
    var appRing by remember { mutableStateOf(0) }
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
    // N6-2: taps run what the screen shows now, not what it showed when the tap handler started.
    val currentActions by rememberUpdatedState(actionsOf)
    val currentApps by rememberUpdatedState(apps)
    val currentMusicActions by rememberUpdatedState(musicActions)
    val covers = remember { MapCoverRegistry() }
    val focus = remember { FocusRequester() }
    // Not in the phone's picture of the car screen: it must not pull focus into the phone's own UI.
    LaunchedEffect(Unit) { if (!sources.picture) runCatching { focus.requestFocus() } }
    BackHandler(enabled = model.place != Place.Home) { model.back() }
    // Declared later, so it comes first: Back goes up the library, then out of it.
    BackHandler(enabled = lib != null) { if (lib?.canGoBack == true) lib.back() else library = null }

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
    } else when (val p = model.place) {
        Place.Home -> when (key) {
            Key.Tab -> { move(FocusGraph.knob(live, ring, if (shift) -1 else 1)); true }
            Key.DirectionUp -> { move(FocusGraph.pad(live, ring, FocusGraph.Direction.UP)); true }
            Key.DirectionDown -> { move(FocusGraph.pad(live, ring, FocusGraph.Direction.DOWN)); true }
            Key.DirectionLeft -> { move(FocusGraph.pad(live, ring, FocusGraph.Direction.LEFT)); true }
            // N6, the ORBIT sheet: pad right is the next song. N6-1: it first moves the ring, as the
            // other directions do (else a car with a pad and no knob could never reach the song);
            // with nothing further right (the ring on the song) it plays the next song.
            Key.DirectionRight -> {
                val to = FocusGraph.pad(live, ring, FocusGraph.Direction.RIGHT)
                if (to != null) move(to) else if (slots.song != null) sources.music.next()
                true
            }
            Key.Enter, Key.DirectionCenter, Key.NumPadEnter -> { actionsOf[ring]?.invoke(); true }
            else -> false
        }
        is Place.Chooser -> when (key) {
            Key.Tab -> { model.turn(if (shift) -1 else 1); true }
            Key.DirectionLeft, Key.DirectionUp -> { model.turn(-1); true }
            Key.DirectionRight, Key.DirectionDown -> { model.turn(1); true }
            Key.Enter, Key.DirectionCenter, Key.NumPadEnter -> {
                model.push()
                if (p.selected == World.APPS) appRing = 0
                if (p.selected == World.MUSIC) musicChosen = MUSIC_PLAY
                if (p.selected == World.CALLS) phoneChosen = null
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
                Key.Enter, Key.DirectionCenter, Key.NumPadEnter -> apps.getOrNull(appIndex)?.let(sources.apps::open)
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
                .onPreviewKeyEvent { e -> e.type == KeyEventType.KeyDown && onKey(e.key, e.isShiftPressed) }
                .focusable(),
        ) {
            // N3: the maps app is told what the slots cover (as the spec layouts do, M2), and gets the
            // skin's colour filter while the look sets none.
            CompositionLocalProvider(LocalMapCovers provides covers) {
                val nav = state.app(state.homeNavAppId)
                CompositionLocalProvider(LocalMapTapIsKnobStop provides false, LocalSkinMapFilter provides tokens.mapFilter) {
                    if (nav != null) navMap(nav, Modifier.fillMaxSize()) else Wallpaper(state, Modifier.fillMaxSize())
                }
                val edge = Modifier.padding(tokens.edge).widthIn(max = maxWidth * 0.4f)
                val showRing = model.place == Place.Home && CarFocusVisibility.keyInput
                skin.Backdrop(Modifier.fillMaxSize())
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
                val home = model.place !is Place.InWorld
                if (home) slots.turn?.let { ringed(STOP_TURN) { skin.Turn(it, edge.align(Alignment.TopStart).slot(Slot.TURN).stop(STOP_TURN)) } }
                Column(edge.align(Alignment.TopEnd), horizontalAlignment = Alignment.End) {
                    skin.Status(Modifier.slot(Slot.STATUS).testTag(SLOT_STATUS))
                    if (skin.showsSpeed && state.speedSource != SpeedSourceMode.OFF) {
                        EngineSpeed(state, sources.speed) { skin.Speed(it, Modifier.slot(Slot.SPEED).testTag(SLOT_SPEED)) }
                    }
                }
                if (home) ringed(STOP_HUB) { skin.Hub(slots.trip, edge.align(Alignment.BottomStart).slot(Slot.HUB).stop(STOP_HUB)) }
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
                }
                if (home) slots.song?.let { ringed(STOP_SONG) { skin.Song(it, edge.align(Alignment.BottomEnd).slot(Slot.SONG).stop(STOP_SONG)) } }
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
                                            detectTapGestures { currentApps.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { appRing = it; sources.apps.open(currentApps[it]) } }
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
            // The gesture line (R6); the notice is the skin's now (N8).
            GestureLineOverlay(state, Modifier.align(Alignment.TopCenter))
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
    val feed = remember(source) { SpeedFeed(source.fixes()) }
    val car by source.car.collectAsState()
    val inputs = SpeedFeed.Inputs(state.speedSource, shown = true, mapsRunning = state.activeNavAppId != null, car = car)
    // Again each second, so a speed the car stopped sending goes away.
    LaunchedEffect(feed, inputs) {
        while (true) {
            feed.update(inputs)
            delay(SPEED_TICK_MS)
        }
    }
    DisposableEffect(feed) { onDispose { feed.release() } }
    content(feed.reading)
}

private const val SPEED_TICK_MS = 1_000L

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
internal const val SLOT_STATUS = "slot_status"
internal const val SLOT_SPEED = "slot_speed"
internal const val SLOT_CHOOSER = "slot_chooser"
internal const val SLOT_WORLD = "slot_world"
internal const val SLOT_APPS = "slot_apps"
internal const val SLOT_MUSIC = "slot_music"
internal const val SLOT_CALL = "slot_call"
internal const val SLOT_PHONE = "slot_phone"
private const val PHONE_APP_KEY = "phone_app"

/** A row of the Calls world (its test tag). */
internal fun phoneRowTag(i: Int) = "phone_row_$i"
internal const val SLOT_LIBRARY = "slot_library"

/** A row of the library (its test tag). */
internal fun libraryRowTag(i: Int) = "library_row_$i"

/** An app's disc in the Apps world (its test tag). */
internal fun appTag(app: CarAppEntry) = "app_${app.id}"
