package net.prototux.aare.ui.car.myplay

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
) {
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
    val currentCall by rememberUpdatedState(callActions)
    val currentRing by rememberUpdatedState(callRing)
    fun callKey(key: Key, shift: Boolean): Boolean {
        val live = callBoxes.filterKeys { it in currentCall }
        when (key) {
            Key.Tab -> FocusGraph.knob(live, currentRing, if (shift) -1 else 1)?.let { callChosen = it }
            Key.DirectionLeft -> FocusGraph.pad(live, currentRing, FocusGraph.Direction.LEFT)?.let { callChosen = it }
            Key.DirectionRight -> FocusGraph.pad(live, currentRing, FocusGraph.Direction.RIGHT)?.let { callChosen = it }
            Key.Enter, Key.DirectionCenter, Key.NumPadEnter -> currentCall[currentRing]?.invoke()
            // Up and down do nothing while it rings; anything else (Back, media keys) goes on as usual.
            Key.DirectionUp, Key.DirectionDown -> Unit
            else -> return false
        }
        return true
    }

    CompositionLocalProvider(LocalSkinTokens provides tokens) {
        BoxWithConstraints(
            Modifier.fillMaxSize().background(tokens.background).then(modifier).testTag(FRAME_TAG)
                .onFocusChanged { frameHasFocus = it.hasFocus }
                // Key ups of the call's keys are kept too, so the app never sees half a press.
                .onPreviewKeyEvent { e -> ringing && (if (e.type == KeyEventType.KeyDown) callKey(e.key, e.isShiftPressed) else e.key in CALL_KEYS) },
        ) {
            Row(Modifier.fillMaxSize()) {
                val rail = @Composable { skin.AppRail(actions::goHome, Modifier.fillMaxHeight().testTag(FRAME_RAIL_TAG)) }
                if (!state.railOnRight) rail()
                ContentArea(state, actions, appContent, navMap, Modifier.weight(1f).fillMaxHeight().testTag(FRAME_APP_TAG))
                if (state.railOnRight) rail()
            }
            overlay(Modifier.align(Alignment.BottomCenter))
            // A notice for a few seconds at the top; its buttons by touch (the knob is the app's here).
            if (notice != null && !ringing) {
                val currentNotice by rememberUpdatedState(notice)
                skin.Notice(
                    notice, hideText = state.drivingRestricted && notice.message, ringed = null,
                    stop = { id ->
                        Modifier.pointerInput(id) {
                            detectTapGestures {
                                val n = currentNotice
                                // Dismiss only dismisses; an action runs, then the notice goes (as on the home).
                                (0 until 2).firstOrNull { noticeActionStop(it) == id }?.let { i -> n.actions.getOrNull(i)?.second?.invoke() }
                                sources.notifications.dismiss(n)
                            }
                        }.testTag(id)
                    },
                    Modifier.align(Alignment.TopCenter).padding(top = tokens.edge),
                )
            }
            // A ringing call: the skin's, over everything, with the knob, as on the home (C-1: touches stop here).
            if (ringing) {
                Box(Modifier.fillMaxSize().focusRequester(callFocus).focusable()) {
                    skin.Ringing(call!!, canEnd, callRing, { id ->
                        Modifier
                            .pointerInput(id) { detectTapGestures { callChosen = id; currentCall[id]?.invoke() } }
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

/** The keys a ringing call keeps (down and up) while it rings over an app. */
private val CALL_KEYS = setOf(Key.Tab, Key.DirectionLeft, Key.DirectionRight, Key.DirectionUp, Key.DirectionDown, Key.Enter, Key.DirectionCenter, Key.NumPadEnter)

internal const val FRAME_TAG = "app_frame"
internal const val FRAME_RAIL_TAG = "app_frame_rail"
internal const val FRAME_APP_TAG = "app_frame_app"
internal const val FRAME_HOME_TAG = "app_frame_home"
