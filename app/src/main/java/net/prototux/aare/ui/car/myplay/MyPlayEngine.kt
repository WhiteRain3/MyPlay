package net.prototux.aare.ui.car.myplay

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.testTag
import net.prototux.aare.ui.car.CallOverlay
import net.prototux.aare.ui.car.CarActions
import net.prototux.aare.ui.car.CarAppEntry
import net.prototux.aare.ui.car.CarScreen
import net.prototux.aare.ui.car.CarUiState
import net.prototux.aare.ui.car.LocalMapTapIsKnobStop
import net.prototux.aare.ui.car.SpecShell
import net.prototux.aare.ui.car.TopLines
import net.prototux.aare.ui.car.Wallpaper

/**
 * MyPlay (plan phase N, N1): the engine of MyPlay's own car screen, drawn by a [MyPlaySkin].
 *
 * N1 draws the home screen only: the maps app's picture (or the wallpaper) behind, the skin's
 * corner slots on top (turn, status, trip, song), and the world chooser over it; a push opens the
 * chooser, a turn moves its choice, a push enters, Back goes up ([MyPlayScreenModel]). Every other
 * screen is still the "map behind" layout's until the chunks after N1 replace them. The slots do
 * not yet tell the maps app what they cover (N3), and the knob has one stop (N2 brings the focus
 * graph).
 *
 * Shown only with the hidden setting myplay_engine_preview; nothing else changes.
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
) {
    if (state.screen != CarScreen.Dashboard) {
        SpecShell(state, actions, appContent, navMap, overlay, modifier)
        return
    }
    val model = remember { MyPlayScreenModel() }
    val tokens = skin.tokens(state.night)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    BackHandler(enabled = model.place != Place.Home) { model.back() }
    CompositionLocalProvider(LocalSkinTokens provides tokens) {
        BoxWithConstraints(
            Modifier.fillMaxSize().background(tokens.background).then(modifier).testTag(ENGINE_TAG)
                .focusRequester(focus)
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (e.key) {
                        Key.Enter, Key.DirectionCenter, Key.NumPadEnter -> { model.push(); true }
                        Key.Tab -> { model.turn(if (e.isShiftPressed) -1 else 1); true }
                        else -> false
                    }
                }
                .focusable(),
        ) {
            val nav = state.app(state.homeNavAppId)
            CompositionLocalProvider(LocalMapTapIsKnobStop provides false) {
                if (nav != null) navMap(nav, Modifier.fillMaxSize()) else Wallpaper(state, Modifier.fillMaxSize())
            }
            val slots = HomeSlots.from(state)
            val edge = Modifier.padding(tokens.edge).widthIn(max = maxWidth * 0.4f)
            slots.turn?.let { skin.Turn(it, edge.align(Alignment.TopStart).testTag(SLOT_TURN)) }
            skin.Status(edge.align(Alignment.TopEnd).testTag(SLOT_STATUS))
            slots.trip?.let { skin.Trip(it, edge.align(Alignment.BottomStart).testTag(SLOT_TRIP)) }
            slots.song?.let { skin.Song(it, edge.align(Alignment.BottomEnd).testTag(SLOT_SONG)) }
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
internal const val SLOT_TURN = "slot_turn"
internal const val SLOT_STATUS = "slot_status"
internal const val SLOT_TRIP = "slot_trip"
internal const val SLOT_SONG = "slot_song"
internal const val SLOT_CHOOSER = "slot_chooser"
internal const val SLOT_WORLD = "slot_world"
