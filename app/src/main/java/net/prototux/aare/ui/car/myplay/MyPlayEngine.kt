package net.prototux.aare.ui.car.myplay

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import net.prototux.aare.ui.car.CallOverlay
import net.prototux.aare.ui.car.CarActions
import net.prototux.aare.ui.car.CarAppEntry
import net.prototux.aare.ui.car.CarFocusVisibility
import net.prototux.aare.ui.car.CarScreen
import net.prototux.aare.ui.car.CarUiState
import net.prototux.aare.ui.car.LocalMapTapIsKnobStop
import net.prototux.aare.ui.car.SpecShell
import net.prototux.aare.ui.car.TopLines
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
 * Every other screen is still the "map behind" layout's until the chunks after N2 replace them;
 * the slots do not yet tell the maps app what they cover (N3). Shown only with the hidden setting
 * myplay_engine_preview.
 */
@Composable
internal fun MyPlayEngineShell(
    state: CarUiState,
    actions: CarActions,
    appContent: @Composable (CarAppEntry, Modifier) -> Unit,
    navMap: @Composable (CarAppEntry, Modifier) -> Unit,
    overlay: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    skin: MyPlaySkin = TestSkin,
    sources: EngineSources = remember(state, actions) { EngineSources.of(state, actions) },
) {
    if (state.screen != CarScreen.Dashboard) {
        SpecShell(state, actions, appContent, navMap, overlay, modifier)
        return
    }
    val model = remember { MyPlayScreenModel() }
    val tokens = skin.tokens(state.night)
    val context = LocalContext.current
    val timeFormat = remember(context) { DateFormat.getTimeFormat(context) }
    val slots = HomeSlots.from(state) { timeFormat.format(it) }
    // The home's stops and where they are, as the slots report them.
    val boxes = remember { mutableStateMapOf<String, FocusGraph.Box>() }
    var chosen by remember { mutableStateOf(STOP_HUB) }
    val actionsOf: Map<String, () -> Unit> = buildMap {
        if (slots.turn != null) put(STOP_TURN, sources.openMaps)
        put(STOP_HUB) { model.push() }
        if (slots.song != null) put(STOP_SONG, sources.music::open)
    }
    // A stop that went away (the song ended) leaves the graph; the ring goes back to the hub.
    val live = boxes.filterKeys { it in actionsOf }
    val ring = chosen.takeIf { it in actionsOf } ?: STOP_HUB
    fun move(to: String?) { if (to != null) chosen = to }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    BackHandler(enabled = model.place != Place.Home) { model.back() }

    fun onKey(key: Key, shift: Boolean): Boolean = when (val p = model.place) {
        Place.Home -> when (key) {
            Key.Tab -> { move(FocusGraph.knob(live, ring, if (shift) -1 else 1)); true }
            Key.DirectionUp -> { move(FocusGraph.pad(live, ring, FocusGraph.Direction.UP)); true }
            Key.DirectionDown -> { move(FocusGraph.pad(live, ring, FocusGraph.Direction.DOWN)); true }
            Key.DirectionLeft -> { move(FocusGraph.pad(live, ring, FocusGraph.Direction.LEFT)); true }
            Key.DirectionRight -> { move(FocusGraph.pad(live, ring, FocusGraph.Direction.RIGHT)); true }
            Key.Enter, Key.DirectionCenter, Key.NumPadEnter -> { actionsOf[ring]?.invoke(); true }
            else -> false
        }
        is Place.Chooser -> when (key) {
            Key.Tab -> { model.turn(if (shift) -1 else 1); true }
            Key.DirectionLeft, Key.DirectionUp -> { model.turn(-1); true }
            Key.DirectionRight, Key.DirectionDown -> { model.turn(1); true }
            Key.Enter, Key.DirectionCenter, Key.NumPadEnter -> {
                model.push()
                if (p.selected == World.APPS) sources.openApps() // N1-1: until the Apps world draws its own (N6)
                true
            }
            else -> false
        }
        is Place.InWorld -> false
    }

    CompositionLocalProvider(LocalSkinTokens provides tokens) {
        BoxWithConstraints(
            Modifier.fillMaxSize().background(tokens.background).then(modifier).testTag(ENGINE_TAG)
                .focusRequester(focus)
                .onPreviewKeyEvent { e -> e.type == KeyEventType.KeyDown && onKey(e.key, e.isShiftPressed) }
                .focusable(),
        ) {
            val nav = state.app(state.homeNavAppId)
            CompositionLocalProvider(LocalMapTapIsKnobStop provides false) {
                if (nav != null) navMap(nav, Modifier.fillMaxSize()) else Wallpaper(state, Modifier.fillMaxSize())
            }
            val edge = Modifier.padding(tokens.edge).widthIn(max = maxWidth * 0.4f)
            val showRing = model.place == Place.Home && CarFocusVisibility.keyInput
            fun Modifier.stop(id: String): Modifier = this
                .onGloballyPositioned { c -> c.boundsInRoot().let { boxes[id] = FocusGraph.Box(it.left, it.top, it.right, it.bottom) } }
                .semantics { selected = ring == id }
                .let { if (showRing && ring == id) it.border(3.dp, tokens.accent, RoundedCornerShape(tokens.corner)) else it }
                .testTag(id)
            slots.turn?.let { skin.Turn(it, edge.align(Alignment.TopStart).stop(STOP_TURN)) }
            skin.Status(edge.align(Alignment.TopEnd).testTag(SLOT_STATUS))
            skin.Hub(slots.trip, edge.align(Alignment.BottomStart).stop(STOP_HUB))
            slots.song?.let { skin.Song(it, edge.align(Alignment.BottomEnd).stop(STOP_SONG)) }
            when (val p = model.place) {
                is Place.Chooser -> skin.Chooser(p.selected, Modifier.align(Alignment.Center).testTag(SLOT_CHOOSER))
                is Place.InWorld -> skin.WorldPlaceholder(p.world, Modifier.align(Alignment.Center).testTag(SLOT_WORLD))
                Place.Home -> Unit
            }
            overlay(Modifier.align(Alignment.BottomCenter))
            TopLines(state, actions, Modifier.align(Alignment.TopCenter))
            CallOverlay(state, actions, Modifier.align(Alignment.TopCenter))
        }
    }
}

internal const val ENGINE_TAG = "engine_root"
/** The home's stops; also their test tags. */
internal const val STOP_TURN = "stop_turn"
internal const val STOP_HUB = "stop_hub"
internal const val STOP_SONG = "stop_song"
internal const val SLOT_STATUS = "slot_status"
internal const val SLOT_CHOOSER = "slot_chooser"
internal const val SLOT_WORLD = "slot_world"
