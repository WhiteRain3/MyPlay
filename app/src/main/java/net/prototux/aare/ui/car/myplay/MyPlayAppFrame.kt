package net.prototux.aare.ui.car.myplay

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import net.prototux.aare.R
import net.prototux.aare.ui.car.CarActions
import net.prototux.aare.ui.car.CarAppEntry
import net.prototux.aare.ui.car.CarUiState
import net.prototux.aare.ui.car.ContentArea
import net.prototux.aare.ui.car.GestureLineOverlay
import net.prototux.aare.ui.car.carFocusHighlight
import net.prototux.aare.ui.car.rememberCanEnd

/**
 * MyPlay (F1, the owner's first drive: "some buttons just point to the original layout"): a
 * full-screen app (the maps app, a music app, any app, MyPlay's own built-in screens) inside MyPlay's
 * own screen instead of the old layout's bar. The app takes the screen beside a narrow rail with
 * the way home, on the driver's side; a notice and a ringing call are the skin's, as on the home.
 * The knob is the app's, except while a call rings (then it is the call's, as on the home); Back
 * comes home as before.
 */
@Composable
internal fun MyPlayAppFrame(
    state: CarUiState,
    actions: CarActions,
    appContent: @Composable (CarAppEntry, Modifier) -> Unit,
    navMap: @Composable (CarAppEntry, Modifier) -> Unit,
    overlay: @Composable (Modifier) -> Unit,
    modifier: Modifier,
    skin: MyPlaySkin,
    sources: EngineSources,
    memory: EngineMemory = remember { EngineMemory() },
) {
    // O3: without the rail, the app grows out of the stop it was opened from, and the first few
    // times in a drive the way home shows for a moment.
    val grows = !skin.appRail
    val appKey = (state.screen as? net.prototux.aare.ui.car.CarScreen.App)?.appId ?: "launcher"
    val grow = remember(appKey) { Animatable(if (grows && !sources.picture) 0f else 1f) }
    LaunchedEffect(appKey) { grow.animateTo(1f, tween(APP_OPEN_MS, easing = FastOutSlowInEasing)) }
    // SM-2: whether the reveal is still running, read through derivedStateOf: reading grow.value here
    // composed the whole frame (the app, the rail, the call layer) again on every frame of the opening.
    val revealing by remember(grow) { derivedStateOf { grow.value < 1f } }
    if (grows) SideEffect { memory.returning = true }
    val hint = remember(appKey) { grows && !sources.picture && state.homeHints < HOME_HINT_TIMES }
    // O2b: a maps app full screen goes home after the driver's chosen minutes without input (off by default).
    var inputs by remember { mutableStateOf(0) }
    val idleHomeMs = MapsIdleHome.ms(state.mapsIdleHomeMin)
    val isMaps = (state.screen as? net.prototux.aare.ui.car.CarScreen.App)?.appId?.let { state.app(it)?.kind } == net.prototux.aare.ui.car.CarAppKind.NAVIGATION
    LaunchedEffect(appKey, inputs, idleHomeMs, isMaps, state.call != null) {
        if (idleHomeMs == null || !isMaps || state.call != null || sources.picture) return@LaunchedEffect
        delay(idleHomeMs)
        actions.goHome()
    }
    LaunchedEffect(appKey) { if (hint) state.homeHints++ }
    val tokens = skin.tokens(state.night)
    val call = sources.calls.call
    val ringing = call?.ringing == true
    val canEnd = rememberCanEnd()
    val callBoxes = remember { mutableStateMapOf<String, FocusGraph.Box>() }
    var callChosen by remember(call?.sinceMs) { mutableStateOf(CALL_ANSWER) }
    val callActions: Map<String, () -> Unit> = buildMap {
        if (ringing) {
            put(CALL_ANSWER, sources.calls::answer)
            if (canEnd) put(CALL_DECLINE, sources.calls::end)
        }
    }
    val callRing = callChosen.takeIf { it in callActions } ?: CALL_ANSWER
    val notice = sources.notifications.notices.firstOrNull()
    LaunchedEffect(notice?.key) {
        if (notice != null) {
            delay(NOTICE_SHOW_MS)
            sources.notifications.dismiss(notice)
        }
    }
    // F1-1 (R5-2): a ringing call takes the knob's keys without taking the focus from the app: the
    // frame sees every key before the focused part of the app does (onPreviewKeyEvent on an
    // ancestor), so while the call rings it keeps them; when it ends the focus is still where it was
    // in the app (a mirrored app's key stop too), and nothing has to be given back.
    // When nothing in the app holds the focus (an app with nothing to focus, like a bare map), the
    // keys would reach no one: only then does the call's own layer take the focus.
    var frameHasFocus by remember { mutableStateOf(false) }
    val callFocus = remember { FocusRequester() }
    LaunchedEffect(ringing) {
        if (ringing && !sources.picture) {
            delay(50) // let the layer be laid out
            if (!frameHasFocus) runCatching { callFocus.requestFocus() }
        }
    }
    // F1-5 (the owner's car has no touch screen): a notice with buttons takes the knob's keys too,
    // the same way, while it shows; the ring starts on Dismiss, so a push meant for the app at the
    // moment it appears only dismisses it. Back dismisses it. A notice without buttons never does.
    val noticeKnob = !ringing && notice != null && notice.actions.isNotEmpty()
    val noticeBoxes = remember(notice?.key) { mutableStateMapOf<String, FocusGraph.Box>() }
    var noticeChosen by remember(notice?.key) { mutableStateOf(NOTICE_DISMISS) }
    val noticeActions: Map<String, () -> Unit> = buildMap {
        if (noticeKnob) {
            val n = notice!!
            n.actions.take(2).forEachIndexed { i, (_, run) -> put(noticeActionStop(i)) { run(); sources.notifications.dismiss(n) } }
            put(NOTICE_DISMISS) { sources.notifications.dismiss(n) }
        }
    }
    val noticeRing = noticeChosen.takeIf { it in noticeActions } ?: NOTICE_DISMISS
    // The knob is the call's or the notice's for now (the app keeps its focus all along).
    val modal = ringing || noticeKnob
    // F1-4: keys down when the call starts ringing (or the notice appears) went down to the app;
    // their ups go to it too, so the app (a mirrored app's key stop) never keeps a key pressed.
    val down = remember { HashSet<Key>() }
    val passUps = remember { HashSet<Key>() }
    LaunchedEffect(modal) { passUps.clear(); if (modal) passUps.addAll(down) }
    val currentActions by rememberUpdatedState(if (ringing) callActions else noticeActions)
    val currentRing by rememberUpdatedState(if (ringing) callRing else noticeRing)
    val currentNoticeDismiss by rememberUpdatedState(noticeActions[NOTICE_DISMISS])
    val currentCallActions by rememberUpdatedState(callActions)
    // Back as the system's Back too (not only as a key): it dismisses a notice holding the knob.
    BackHandler(enabled = noticeKnob) { currentNoticeDismiss?.invoke() }
    fun choose(id: String) { if (ringing) callChosen = id else noticeChosen = id }
    fun modalKey(key: Key, shift: Boolean): Boolean {
        val live = (if (ringing) callBoxes else noticeBoxes).filterKeys { it in currentActions }
        when (key) {
            Key.Tab -> FocusGraph.knob(live, currentRing, if (shift) -1 else 1)?.let(::choose)
            Key.DirectionLeft -> FocusGraph.pad(live, currentRing, FocusGraph.Direction.LEFT)?.let(::choose)
            Key.DirectionRight -> FocusGraph.pad(live, currentRing, FocusGraph.Direction.RIGHT)?.let(::choose)
            Key.Enter, Key.DirectionCenter, Key.NumPadEnter -> currentActions[currentRing]?.invoke()
            // Up and down do nothing meanwhile.
            Key.DirectionUp, Key.DirectionDown -> Unit
            // Back dismisses a notice (a ringing call leaves Back to the car, as before).
            Key.Back, Key.Escape -> if (!ringing) currentNoticeDismiss?.invoke() else return false
            // Anything else (media keys) goes on as usual.
            else -> return false
        }
        return true
    }

    CompositionLocalProvider(LocalSkinTokens provides tokens) {
        BoxWithConstraints(
            Modifier.fillMaxSize().background(tokens.background).then(modifier).testTag(FRAME_TAG)
                .onFocusChanged { frameHasFocus = it.hasFocus }
                // O2b: any touch is input (only looked at).
                .pointerInput(Unit) { awaitPointerEventScope { while (true) { awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial); inputs++ } } }
                // Key ups of the call's keys are kept too, so the app never sees half a press.
                .onPreviewKeyEvent { e ->
                    if (e.type == KeyEventType.KeyDown) inputs++
                    when (e.type) {
                        KeyEventType.KeyDown -> { down += e.key; modal && modalKey(e.key, e.isShiftPressed) }
                        KeyEventType.KeyUp -> {
                            down -= e.key
                            modal && (e.key in CALL_KEYS || (!ringing && e.key in BACK_KEYS)) && !passUps.remove(e.key)
                        }
                        else -> false
                    }
                },
        ) {
            Row(Modifier.fillMaxSize()) {
                // An app's fullscreen video takes the whole screen: no rail meanwhile (Back leaves fullscreen).
                val rail = @Composable { if (!state.appFullscreen && skin.appRail) skin.AppRail(actions::goHome, Modifier.fillMaxHeight().testTag(FRAME_RAIL_TAG)) }
                if (!state.railOnRight) rail()
                val origin = memory.origin
                ContentArea(state, actions, appContent, navMap,
                    Modifier.weight(1f).fillMaxHeight().testTag(FRAME_APP_TAG))
                if (state.railOnRight) rail()
            }
            // O3-3: the app shows through a window growing out of its stop (over any surface view).
            if (grows && revealing) RevealCover(memory.origin, { grow.value }, tokens.background, Modifier.matchParentSize())
            overlay(Modifier.align(Alignment.BottomCenter))
            // O3: the way home, the first few times in a drive (there is no rail to show it).
            if (hint) key(appKey) { HomeHint(true, Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp)) }
            // A notice for a few seconds at the top: its buttons by touch, and (F1-5) by the knob.
            // F1-3 (R6, R6-3): the notice and, under it, the gesture line (what a button just did), never on top of each other.
            Column(Modifier.align(Alignment.TopCenter).padding(top = tokens.edge), horizontalAlignment = Alignment.CenterHorizontally) {
                if (notice != null && !ringing) {
                    val currentNotice by rememberUpdatedState(notice)
                    skin.Notice(
                        notice, hideText = state.drivingRestricted && notice.message, ringed = noticeRing.takeIf { noticeKnob },
                        stop = { id ->
                            Modifier.pointerInput(id) {
                                detectTapGestures {
                                    val n = currentNotice
                                    // Dismiss only dismisses; an action runs, then the notice goes (as on the home).
                                    (0 until 2).firstOrNull { noticeActionStop(it) == id }?.let { i -> n.actions.getOrNull(i)?.second?.invoke() }
                                    sources.notifications.dismiss(n)
                                }
                            }
                                .onGloballyPositioned { c -> c.boundsInRoot().let { noticeBoxes[id] = FocusGraph.Box(it.left, it.top, it.right, it.bottom) } }
                                .semantics { selected = noticeKnob && id == noticeRing }
                                .testTag(id)
                        },
                        Modifier,
                    )
                }
                CompositionLocalProvider(net.prototux.aare.ui.car.LocalGestureLineDraw provides skin.gestureLine) { GestureLineOverlay(state, Modifier) }
            }
            // A ringing call: the skin's, over everything, with the knob, as on the home (C-1: touches stop here).
            if (ringing) {
                Box(Modifier.fillMaxSize().focusRequester(callFocus).focusable()) {
                    skin.Ringing(call!!, canEnd, callRing, { id ->
                        Modifier
                            .pointerInput(id) { detectTapGestures { callChosen = id; currentCallActions[id]?.invoke() } }
                            .onGloballyPositioned { c -> c.boundsInRoot().let { callBoxes[id] = FocusGraph.Box(it.left, it.top, it.right, it.bottom) } }
                            .semantics { selected = id == callRing }
                            .testTag(id)
                    }, Modifier.fillMaxSize().testTag(SLOT_CALL)
                        .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } })
                }
            }
        }
    }
}

/** The rail's width at the sheet's size, before a skin's own drawing. */
internal val APP_RAIL_WIDTH = 84.dp

/**
 * The default rail beside an app: the skin's surface with a round home button in its accent. A
 * tap goes home; the knob reaches it too (it is focusable, with a visible ring when focused).
 */
@Composable
internal fun DefaultAppRail(home: () -> Unit, modifier: Modifier) {
    val t = LocalSkinTokens.current
    Column(modifier.width(APP_RAIL_WIDTH).background(t.surface), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.padding(top = 22.dp).size(56.dp).clip(CircleShape).background(t.accent)
                // The car screen's own focus ring (K4), shown once the knob is used.
                .carFocusHighlight(CircleShape)
                .clickable(onClickLabel = stringResource(R.string.myplay_frame_home)) { home() }
                .testTag(FRAME_HOME_TAG),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Home, stringResource(R.string.myplay_frame_home), Modifier.size(30.dp), tint = t.onAccent)
        }
    }
}

/** Back, which a notice that holds the knob keeps (it dismisses the notice). */
private val BACK_KEYS = setOf(Key.Back, Key.Escape)

/** The keys a ringing call keeps (down and up) while it rings over an app. */
private val CALL_KEYS = setOf(Key.Tab, Key.DirectionLeft, Key.DirectionRight, Key.DirectionUp, Key.DirectionDown, Key.Enter, Key.DirectionCenter, Key.NumPadEnter)

internal const val FRAME_TAG = "app_frame"
internal const val FRAME_RAIL_TAG = "app_frame_rail"
internal const val FRAME_APP_TAG = "app_frame_app"
internal const val FRAME_HOME_TAG = "app_frame_home"
