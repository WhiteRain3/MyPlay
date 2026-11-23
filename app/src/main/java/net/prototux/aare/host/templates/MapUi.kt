@file:Suppress("DEPRECATION") // PlaceListNavigation, RoutePreview and Map templates are deprecated but still used

package net.prototux.aare.host.templates

import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.OnContentRefreshDelegate
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.PlaceListMapTemplate
import androidx.car.app.navigation.model.MapTemplate
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.car.app.navigation.model.PanModeDelegate
import androidx.car.app.navigation.model.PlaceListNavigationTemplate
import androidx.car.app.navigation.model.RoutePreviewNavigationTemplate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.activity.compose.BackHandler
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import net.prototux.aare.R
import net.prototux.aare.ui.car.carFocusHighlight
import net.prototux.aare.ui.theme.LocalCarDimens
import kotlin.math.abs
import kotlin.math.roundToInt

// ---------------------------------------------------------------- obstruction tracking

/** Bounds (in root px) of the map and of everything floating over it. */
internal class ObstructionRegistry {
    var map by mutableStateOf(Rect.Zero)
    val rects = mutableStateMapOf<Any, Rect>()
    /** MyPlay: the areas last told to the app (map surface px), for [MapAreaOutline]. */
    var toldVisible by mutableStateOf<List<Int>?>(null)
    var toldStable by mutableStateOf<List<Int>?>(null)
}

internal val LocalObstructions = staticCompositionLocalOf<ObstructionRegistry?> { null }

/**
 * True in the dashboard's map card: the map scaffold leaves out the action strips and gestures,
 * and templates draw only what [MapScaffold]'s `inCompact` overlay asks for (the next turn).
 * Without this the full-screen chrome squeezed into the card covered most of it, and the app,
 * told only the small uncovered part was visible, fitted its map into that (very zoomed in).
 */
internal val LocalCompactMap = staticCompositionLocalOf { false }

/**
 * Marks a card/strip as covering the map: its bounds shrink the visible area reported to the
 * app, and it swallows touches so they do not reach the map gesture layer underneath.
 */
@Composable
internal fun Modifier.obstructsMap(): Modifier {
    val registry = LocalObstructions.current ?: return this
    val key = remember { Any() }
    DisposableEffect(registry) { onDispose { registry.rects.remove(key) } }
    return this
        .onGloballyPositioned { registry.rects[key] = it.boundsInRoot() }
        .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false) } }
}

// ---------------------------------------------------------------- scaffold

/**
 * A map template frame: the app's map surface fills the area, gestures go to its
 * SurfaceCallback, the action strip sits top end and the map action strip bottom end, and
 * [overlay] lays the template's cards on the start (driver) side.
 *
 * Pan mode (Action.PAN in the map action strip, or dragging the map when it is offered) hides
 * the cards and the action strip and reports PanModeDelegate.sendPanModeChanged. The visible
 * area (map minus whatever floats over it) and the stable area (same, outside pan mode) are
 * sent to the app whenever the layout changes.
 */
@Composable
internal fun MapScaffold(
    actionStrip: ActionStrip?,
    mapActionStrip: ActionStrip?,
    panModeDelegate: PanModeDelegate?,
    modifier: Modifier = Modifier,
    inCompact: Boolean = false,
    /** Look C: the template whose strips go into the actions wheel when MyPlay provides one (navigation). */
    wheel: Boolean = false,
    overlay: @Composable BoxWithConstraintsScope.() -> Unit,
) {
    if (LocalCompactMap.current) {
        CompactMapScaffold(modifier, if (inCompact) overlay else null)
        return
    }
    val host = LocalTemplateHost.current
    val gutter = LocalCarDimens.current.gutter
    val registry = remember { ObstructionRegistry() }
    val panState = remember { mutableStateOf(false) }
    val panMode = panState.value
    val panOffered = mapActionStrip?.getFirstActionOfType(Action.TYPE_PAN) != null
    val delegate by rememberUpdatedState(panModeDelegate)
    val setPan: (Boolean) -> Unit = { on ->
        if (on != panState.value) {
            panState.value = on
            delegate?.let { d -> safeCall("onPanModeChanged") { d.sendPanModeChanged(on, host.done) } }
        }
    }
    if (panMode && !panOffered) LaunchedEffect(Unit) { setPan(false) }
    // the gesture detector outlives recompositions: read the current template through this
    val onMapDrag by rememberUpdatedState { if (panOffered) setPan(true) }
    // Like Android Auto, drags and flings move the map only where the app offers panning.
    val canPan by rememberUpdatedState(panOffered || panMode)
    DisposableEffect(Unit) {
        onDispose { if (panState.value) delegate?.let { d -> safeCall("onPanModeChanged") { d.sendPanModeChanged(false, host.done) } } }
    }

    // Look C (steps 1, 2): under ORBIT, no strips on the map; Waze's actions in a wheel instead.
    val wheelHost = LocalActionsWheel.current?.takeIf { wheel }
    var wheelOpen by remember { mutableStateOf(false) }
    var wheelRing by remember { mutableIntStateOf(0) }
    var wheelClosedAt by remember { mutableLongStateOf(Long.MIN_VALUE / 2) }
    /** When Back opened the wheel (and nothing has been done in it since): a second Back soon after goes home. */
    var wheelBackAt by remember { mutableLongStateOf(Long.MIN_VALUE / 2) }
    var moveAsked by remember { mutableIntStateOf(0) }
    val mapFocus = remember { FocusRequester() }
    val entries = if (wheelHost == null) emptyList() else wheelEntries(actionStrip, mapActionStrip, wheelHost,
        click = { a -> host.click(a.onClickDelegate) }, movePan = { moveAsked++ })
    fun closeWheel() {
        wheelOpen = false
        wheelClosedAt = wheelHost?.now?.invoke() ?: 0L
        runCatching { mapFocus.requestFocus() }
    }
    fun openWheel() { wheelRing = 0; wheelOpen = true; wheelBackAt = Long.MIN_VALUE / 2 }
    // Declared before the map's own (move mode's), so moving the map keeps Back while it lasts.
    BackHandler(enabled = wheelHost != null && !panMode && !wheelHost.held()) {
        val w = wheelHost ?: return@BackHandler
        val now = w.now()
        when {
            // (b), the dispatcher: Back twice quickly goes home, from the map as anywhere.
            wheelOpen && now - wheelBackAt <= ActionsWheelHost.SECOND_BACK_MS -> { wheelOpen = false; w.home() }
            wheelOpen -> closeWheel()
            now - wheelClosedAt <= ActionsWheelHost.SECOND_BACK_MS -> w.home()
            else -> { openWheel(); wheelBackAt = now }
        }
    }

    Box(
        modifier.fillMaxSize().onGloballyPositioned { registry.map = it.boundsInRoot() }
            // The pad's centre opens the wheel (the map stop passes it on while the wheel is in use).
            .onKeyEvent { e ->
                if (wheelHost == null || wheelOpen || panMode || wheelHost.held()) return@onKeyEvent false
                val push = e.key == Key.Enter || e.key == Key.DirectionCenter || e.key == Key.NumPadEnter
                if (push && e.type == KeyEventType.KeyDown) openWheel()
                push
            },
    ) {
        // The app's map surface itself is behind this, drawn by CarAppScreen (it must outlive templates).
        Box(Modifier.fillMaxSize().mapGestures(host, canPan = { canPan }) { onMapDrag() })
        CompositionLocalProvider(
            LocalObstructions provides registry,
            LocalPanControl provides PanControl(panMode) { setPan(!panState.value) },
            LocalInCard provides true,
        ) {
            BoxWithConstraints(Modifier.fillMaxSize().padding(gutter)) {
                // obstructsMap() only for a strip that is drawn: its rect goes away with it (an empty strip draws
                // nothing, so its old position would never be updated or removed).
                if (!panMode) {
                    overlay()
                    if (wheelHost == null && !actionStrip?.actions.isNullOrEmpty()) {
                        ActionStripView(actionStrip, ActionLook.MapStrip, vertical = false, Modifier.align(Alignment.TopEnd).obstructsMap())
                    }
                }
                if (wheelHost == null && !mapActionStrip?.actions.isNullOrEmpty()) {
                    ActionStripView(mapActionStrip, ActionLook.MapStrip, vertical = true, Modifier.align(Alignment.BottomEnd).obstructsMap())
                }
            }
        }
        // MyPlay (R5): the map as the knob's last stop here, and moving it with the knob (MapMoveMode.kt).
        MapMoveStop(host, panOffered, { panState.value }, setPan, pushEnters = wheelHost == null, moveAsked = moveAsked, focus = mapFocus)
        if (wheelOpen && entries.isNotEmpty()) {
            ActionsWheelView(entries, wheelRing.coerceIn(0, entries.lastIndex),
                move = { d -> wheelRing = (wheelRing + d).coerceIn(0, entries.lastIndex); wheelBackAt = Long.MIN_VALUE / 2 },
                run = { val e = entries[wheelRing.coerceIn(0, entries.lastIndex)]; closeWheel(); e.run() })
        }
        MapAreaOutline(registry.toldVisible, registry.toldStable) // MyPlay
    }
    ReportAreas(registry, panState)
}

/** The dashboard card: the map alone, plus [overlay] if the template keeps one there; taps go to the card. */
@Composable
private fun CompactMapScaffold(modifier: Modifier, overlay: (@Composable BoxWithConstraintsScope.() -> Unit)?) {
    val registry = remember { ObstructionRegistry() }
    val gutter = LocalCarDimens.current.gutter
    Box(modifier.fillMaxSize().onGloballyPositioned { registry.map = it.boundsInRoot() }) {
        if (overlay != null) {
            CompositionLocalProvider(LocalObstructions provides registry, LocalInCard provides true) {
                BoxWithConstraints(Modifier.fillMaxSize().padding(gutter)) { overlay() }
            }
        }
        MapAreaOutline(registry.toldVisible, registry.toldStable) // MyPlay
    }
    ReportAreas(registry, remember { mutableStateOf(false) })
}

/**
 * Sends visible/stable area changes (map-surface px) to the app, deduplicated.
 * MyPlay (M2): also counts what MyPlay's own screen draws over the map ([LocalMapCovers]), and
 * the stable area leaves out every place a card may appear, not only what shows now (MapAreas).
 */
@Composable
private fun ReportAreas(registry: ObstructionRegistry, panState: androidx.compose.runtime.State<Boolean>) {
    val host = LocalTemplateHost.current
    val covers = LocalMapCovers.current
    val minGutter = with(androidx.compose.ui.platform.LocalDensity.current) { MapGeometry.CENTRED_MIN_GUTTER_DP.dp.toPx() }
    LaunchedEffect(registry, host, covers, minGutter) {
        var lastVisible: List<Int>? = null
        var lastStable: List<Int>? = null
        var logged = 0 // MyPlay: lines written to the connection log for this map
        snapshotFlow {
            listOf(registry.map, registry.rects.values.toList(), panState.value,
                covers?.shown?.values?.toList().orEmpty(), covers?.reserved?.values?.toList().orEmpty(), covers?.centred)
        }
            .distinctUntilChanged()
            .collect { _ ->
                val map = registry.map
                val pan = panState.value
                if (map.width <= 0f || map.height <= 0f) return@collect
                val shown = registry.rects.values.map { it.toBox() } + covers?.shown?.values?.map { it.toBox() }.orEmpty()
                val reserved = covers?.reserved?.values?.map { it.toBox() }.orEmpty()
                val areas = MapAreas.compute(map.toBox(), shown, reserved, centred = covers?.centred == true, minGutter = minGutter)
                val area = MapAreas.inSurface(map.toBox(), areas.visible)
                val visibleChanged = area != lastVisible
                if (visibleChanged) {
                    lastVisible = area
                    safeCall("setVisibleArea") { host.setVisibleArea(area[0], area[1], area[2], area[3]) }
                }
                // stable area: the layout with all chrome shown, i.e. outside pan mode
                val stable = MapAreas.inSurface(map.toBox(), areas.stable)
                val stableChanged = !pan && stable != lastStable
                if (stableChanged) {
                    lastStable = stable
                    safeCall("setStableArea") { host.setStableArea(stable[0], stable[1], stable[2], stable[3]) }
                }
                // MyPlay: what the app was told, in the connection log and for the outline.
                registry.toldVisible = lastVisible
                registry.toldStable = lastStable
                if ((visibleChanged || stableChanged) && logged++ < MapAreaDebug.MAX_LINES) {
                    net.prototux.aare.DiagLog.add(MapAreaDebug.line(map.toBox(), area, if (pan) null else stable))
                }
            }
    }
}

private fun Rect.toBox() = MapGeometry.Box(left, top, right, bottom)

internal enum class MapGestureAction { SCALE, DRAG, IGNORE }

internal fun mapGestureAction(pointerCount: Int, hadMultiplePointers: Boolean): MapGestureAction = when {
    pointerCount >= 2 -> MapGestureAction.SCALE
    pointerCount == 1 && !hadMultiplePointers -> MapGestureAction.DRAG
    else -> MapGestureAction.IGNORE
}

/**
 * Map gestures in SurfaceCallback conventions: onScroll gets GestureDetector-style distances
 * (previous minus current), onFling velocities in px/s, onScale a per-event factor around the
 * focus point, onClick a tap position. [onDragStart] lets the scaffold enter pan mode; scrolls
 * and flings are only sent while [canPan] (checked after it, so the first drag that enters pan
 * mode already moves the map). Zoom and taps always go through.
 */
private fun Modifier.mapGestures(host: TemplateHost, canPan: () -> Boolean, onDragStart: () -> Unit): Modifier = pointerInput(host) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val tracker = VelocityTracker()
        tracker.addPosition(down.uptimeMillis, down.position)
        var dragging = false
        var multiTouch = false
        var slop = Offset.Zero
        while (true) {
            val event = awaitPointerEvent()
            val pressed = event.changes.filter { it.pressed }
            if (pressed.isEmpty()) break
            when (mapGestureAction(pressed.size, multiTouch)) {
                MapGestureAction.SCALE -> {
                    if (!dragging) onDragStart()
                    multiTouch = true
                    dragging = true
                    val zoom = event.calculateZoom()
                    val centroid = event.calculateCentroid(useCurrent = true)
                    if (zoom != 1f && zoom.isFinite()) host.mapScale(centroid.x, centroid.y, zoom)
                    // Keep scale and scroll mutually exclusive. Apps commonly synthesize a scale
                    // gesture from onScale and end it when onScroll arrives; forwarding centroid
                    // movement as scroll here therefore cancels every pinch frame.
                }
                MapGestureAction.DRAG -> {
                    val c = pressed[0]
                    tracker.addPosition(c.uptimeMillis, c.position)
                    val delta = c.positionChange()
                    if (!dragging) {
                        slop += delta
                        if (slop.getDistance() > viewConfiguration.touchSlop) {
                            dragging = true
                            onDragStart()
                            if (canPan()) host.mapScroll(-slop.x, -slop.y)
                        }
                    } else if (delta != Offset.Zero && canPan()) {
                        host.mapScroll(-delta.x, -delta.y)
                    }
                }
                // Once a gesture has had two pointers, ignore its remaining one-pointer tail.
                // Sending a drag immediately after scaling can cancel pending scale handling.
                MapGestureAction.IGNORE -> Unit
            }
            event.changes.forEach { it.consume() }
        }
        if (!dragging) {
            host.mapClick(down.position.x, down.position.y)
        } else if (!multiTouch) {
            val v = tracker.calculateVelocity()
            if ((abs(v.x) > 300f || abs(v.y) > 300f) && canPan()) host.mapFling(v.x, v.y)
        }
    }
}

// ---------------------------------------------------------------- card placement

/** Width of the start-side card column: about 40% of the screen, within readable bounds. */
internal fun BoxWithConstraintsScope.panelWidth(): Dp = (maxWidth * 0.42f).coerceIn(320.dp, 480.dp).coerceAtMost(maxWidth)

/** What narrow screens leave free at the bottom end for the map action strip, beside bottom cards. */
internal val MapActionStripRoom = 96.dp

/** Narrow/portrait screens put cards above/below the map instead of beside it. */
internal val BoxWithConstraintsScope.narrow: Boolean get() = maxWidth < 600.dp || maxHeight > maxWidth

/**
 * The floating content card of map templates: start side and full height on wide screens,
 * a bottom sheet-like card on narrow ones (the dashboard's map card, a portrait screen), which
 * leaves the bottom end to the map action strip so its buttons stay reachable.
 */
@Composable
internal fun BoxWithConstraintsScope.MapContentCard(content: @Composable () -> Unit) {
    val r = LocalCarDimens.current.cardRadius
    val m = if (narrow) {
        Modifier.align(Alignment.BottomStart).width(maxWidth - MapActionStripRoom).height(maxHeight * 0.55f)
    } else {
        Modifier.align(Alignment.TopStart).width(panelWidth()).fillMaxHeight()
    }
    CarCard(m.shadow(8.dp, RoundedCornerShape(r)).obstructsMap()) { content() }
}

// ---------------------------------------------------------------- templates

/** MapWithContentTemplate: the app's map with a List/Grid/Pane/Message template in a card. */
@Composable
internal fun MapWithContentTemplateView(t: MapWithContentTemplate) {
    val controller = t.mapController
    MapScaffold(t.actionStrip, controller?.mapActionStrip, controller?.panModeDelegate) {
        val content = t.contentTemplate
        MapContentCard {
            when (content) {
                is ListTemplate, is GridTemplate, is PaneTemplate, is MessageTemplate -> TemplateContent(content)
                else -> UnsupportedTemplate(content)
            }
        }
    }
}

/** PlaceListMapTemplate (deprecated, still common for POI apps): places list over the map. */
@Composable
internal fun PlaceListMapTemplateView(t: PlaceListMapTemplate) {
    MapScaffold(t.actionStrip, null, null) {
        MapContentCard {
            MapListCard(
                header = headerSpec(null, t.title, t.headerAction),
                list = t.itemList,
                loading = t.isLoading,
                limitType = ConstraintManager.CONTENT_LIMIT_TYPE_PLACE_LIST,
                refresh = t.onContentRefreshDelegate,
            )
        }
    }
}

/** PlaceListNavigationTemplate (deprecated): like PlaceListMapTemplate, for navigation apps. */
@Composable
internal fun PlaceListNavigationTemplateView(t: PlaceListNavigationTemplate) {
    MapScaffold(t.actionStrip, t.mapActionStrip, t.panModeDelegate) {
        MapContentCard {
            @Suppress("DEPRECATION")
            MapListCard(
                header = headerSpec(t.header, t.title, t.headerAction),
                list = t.itemList,
                loading = t.isLoading,
                limitType = ConstraintManager.CONTENT_LIMIT_TYPE_PLACE_LIST,
                refresh = t.onContentRefreshDelegate,
            )
        }
    }
}

/** RoutePreviewNavigationTemplate (deprecated): route choices and a big Navigate button. */
@Composable
internal fun RoutePreviewNavigationTemplateView(t: RoutePreviewNavigationTemplate) {
    MapScaffold(t.actionStrip, t.mapActionStrip, t.panModeDelegate) {
        MapContentCard {
            @Suppress("DEPRECATION")
            MapListCard(
                header = headerSpec(t.header, t.title, t.headerAction),
                list = t.itemList,
                loading = t.isLoading,
                limitType = ConstraintManager.CONTENT_LIMIT_TYPE_ROUTE_LIST,
                footer = t.navigateAction?.let { action ->
                    { ActionView(action, ActionLook.Button, Modifier.fillMaxWidth().padding(12.dp), fill = true, forcePrimary = true) }
                },
            )
        }
    }
}

/** MapTemplate (deprecated, replaced by MapWithContentTemplate): a Pane or list over the map. */
@Composable
internal fun MapTemplateView(t: MapTemplate) {
    val controller = t.mapController
    MapScaffold(t.actionStrip, controller?.mapActionStrip, controller?.panModeDelegate) {
        val pane = t.pane
        MapContentCard {
            if (pane != null) {
                Column(Modifier.fillMaxSize()) {
                    val header = headerSpec(t.header)
                    if (!header.isEmpty) HeaderBar(header)
                    PaneContent(pane, Modifier.weight(1f))
                }
            } else {
                MapListCard(headerSpec(t.header), t.itemList, loading = false, limitType = ConstraintManager.CONTENT_LIMIT_TYPE_LIST)
            }
        }
    }
}

/** Header, list (or spinner / empty message) and optional footer, for the map list templates. */
@Composable
private fun MapListCard(
    header: HeaderSpec,
    list: ItemList?,
    loading: Boolean,
    limitType: Int,
    refresh: OnContentRefreshDelegate? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    val host = LocalTemplateHost.current
    Column(Modifier.fillMaxSize()) {
        val extra: (@Composable androidx.compose.foundation.layout.RowScope.() -> Unit)? = refresh?.let { d ->
            {
                Box(
                    Modifier.size(LocalCarDimens.current.touchTarget).clip(CircleShape)
                        .carFocusHighlight(CircleShape)
                        .clickable { safeCall("onContentRefreshRequested") { d.sendContentRefreshRequested(host.done) } },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Refresh, "Refresh", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
                }
            }
        }
        if (!header.isEmpty || extra != null) HeaderBar(header, extra)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                loading -> LoadingBox()
                list == null -> EmptyMessage(null, stringResource(R.string.common_nothing_to_show))
                else -> ItemListView(listOf(ListSection(null, list)), limitType)
            }
        }
        footer?.invoke()
    }
}
