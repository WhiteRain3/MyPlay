package net.prototux.aare.ui.car.myplay.orbit

import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Search
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.pluralStringResource
import net.prototux.aare.host.templates.coversMap
import androidx.compose.ui.graphics.toArgb

import androidx.compose.ui.graphics.nativeCanvas

import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

import net.prototux.aare.ui.car.myplay.BackdropParts

import androidx.compose.animation.core.tween

import androidx.compose.animation.core.animateFloatAsState

import android.os.SystemClock
import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Battery0Bar
import androidx.compose.material.icons.rounded.Battery1Bar
import androidx.compose.material.icons.rounded.Battery2Bar
import androidx.compose.material.icons.rounded.Battery3Bar
import androidx.compose.material.icons.rounded.Battery4Bar
import androidx.compose.material.icons.rounded.Battery5Bar
import androidx.compose.material.icons.rounded.Battery6Bar
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhoneMissed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.ToggleOff
import androidx.compose.material.icons.rounded.ToggleOn
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Straight
import androidx.compose.material.icons.rounded.TurnLeft
import androidx.compose.material.icons.rounded.TurnRight
import androidx.compose.material.icons.rounded.TurnSharpLeft
import androidx.compose.material.icons.rounded.TurnSharpRight
import androidx.compose.material.icons.rounded.TurnSlightLeft
import androidx.compose.material.icons.rounded.TurnSlightRight
import androidx.compose.material.icons.rounded.UTurnLeft
import androidx.compose.material.icons.rounded.UTurnRight
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import net.prototux.aare.R
import net.prototux.aare.car.SpeedReading
import net.prototux.aare.ui.car.AppIcon
import net.prototux.aare.ui.car.CarPart
import net.prototux.aare.ui.car.carPart
import net.prototux.aare.ui.car.CarAppEntry
import net.prototux.aare.ui.car.mediaProgress
import net.prototux.aare.ui.car.myplay.LocalRoadIce
import net.prototux.aare.ui.car.myplay.LocalOutsideTemp
import net.prototux.aare.ui.car.myplay.LimitPulse
import net.prototux.aare.ui.car.myplay.LocalLimitPulse
import net.prototux.aare.ui.car.myplay.LocalSkinTokens
import net.prototux.aare.ui.car.myplay.LocalStopRinged
import net.prototux.aare.ui.car.myplay.MUSIC_ART
import net.prototux.aare.ui.car.myplay.MUSIC_LIBRARY
import net.prototux.aare.ui.car.myplay.MUSIC_NEXT
import net.prototux.aare.ui.car.myplay.MUSIC_PLAY
import net.prototux.aare.ui.car.myplay.MUSIC_PREV
import net.prototux.aare.ui.car.myplay.PANEL_APP
import net.prototux.aare.ui.car.myplay.PANEL_NEXT
import net.prototux.aare.ui.car.myplay.PANEL_PLAY
import net.prototux.aare.ui.car.myplay.PANEL_PREV
import net.prototux.aare.ui.car.myplay.PANEL_SEEK
import net.prototux.aare.ui.car.myplay.MusicPage
import net.prototux.aare.ui.car.myplay.SettingsPage
import net.prototux.aare.ui.car.myplay.NoticeListPage
import net.prototux.aare.ui.car.myplay.PhonePage
import net.prototux.aare.ui.car.myplay.PhoneRow
import net.prototux.aare.ui.car.myplay.CALL_ANSWER
import net.prototux.aare.ui.car.myplay.CALL_DECLINE
import net.prototux.aare.ui.car.myplay.STOP_CALL
import net.prototux.aare.ui.car.myplay.STOP_HANG_UP
import net.prototux.aare.ui.car.CarCall
import net.prototux.aare.ui.car.CarNotice
import net.prototux.aare.ui.car.myplay.NOTICE_DISMISS
import net.prototux.aare.ui.car.myplay.noticeActionStop
import net.prototux.aare.ui.car.myplay.LibraryPage
import net.prototux.aare.media.LibraryRow
import net.prototux.aare.ui.car.myplay.MyPlaySkin
import net.prototux.aare.ui.car.myplay.upNextStop
import net.prototux.aare.ui.car.myplay.SkinTokens
import net.prototux.aare.ui.car.myplay.Slot
import net.prototux.aare.ui.car.myplay.SongSlot
import net.prototux.aare.ui.car.myplay.TripSlot
import net.prototux.aare.ui.car.myplay.TurnDirection
import net.prototux.aare.ui.car.myplay.TurnSlot
import net.prototux.aare.ui.car.myplay.World
import net.prototux.aare.ui.car.rememberPhoneStatus
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.isSpecified
import net.prototux.aare.car.LimitShown

/**
 * MyPlay (plan phase N, N5): ORBIT, MyPlay's first car screen language, from the Design reviewer's
 * sheet (language 1). The home is the map with four things floating in its corners: the turn
 * bubble (top left), the clock (top right), the hub, whose ring is the trip (bottom left), and the
 * song as a round disc (bottom right). Pushing the hub opens the dial of six worlds around the
 * middle; it ripples out of the hub.
 *
 * Every size is the sheet's at 1280x720 and scales with the screen ([Scaled]), so the corners keep
 * the same share of the map on a smaller screen (the Audi's 901x543).
 */
object OrbitSkin : MyPlaySkin {
    override val id = "orbit"

    @Composable override fun tokens(night: Boolean): SkinTokens = Orbit.tokens(night)

    override val drawsFocus get() = true
    override val showsSpeed get() = true

    /** O3: an app takes the whole screen, grown out of its stop; no rail. */
    override val appRail get() = false

    /**
     * HN-3 (the owner, 2026-11-23): the dial is strictly four ways: up Navigate (the maps app
     * itself), right Calls, down Settings, left Music; Apps is in the middle, opened by pushing
     * again. Car is not one of the owner's four, so ORBIT's dial no longer reaches it.
     */
    override val dial get() = listOf(World.NAVIGATE, World.CALLS, World.SETTINGS, World.MUSIC)
    override val dialCentre get() = World.APPS
    override val actionsWheel get() = true
    override val tapRingsFirst get() = true
    override val callAtBottom get() = true

    /** The sheet's screen; sizes scale from it. */
    private const val SHEET_W = 1280f
    private const val SHEET_H = 720f

    /** The scale of this screen against the sheet's, within reason. */
    fun scale(widthDp: Int, heightDp: Int): Float = minOf(widthDp / SHEET_W, heightDp / SHEET_H).coerceIn(0.6f, 1.25f)

    /** Draws [content] at ORBIT's scale for this screen. */
    @Composable
    private fun Scaled(content: @Composable () -> Unit) {
        val c = LocalConfiguration.current
        val d = LocalDensity.current
        val s = scale(c.screenWidthDp, c.screenHeightDp)
        // TH-3: the owner's Control size makes ORBIT's discs and targets bigger or smaller; the text
        // keeps the owner's Text size (the font scale is divided back), so the two do not stack.
        val controls = net.prototux.aare.ui.theme.LocalCarControlScale.current.coerceIn(0.5f, 2f)
        CompositionLocalProvider(LocalDensity provides Density(d.density * s * controls, d.fontScale / controls), content = content)
    }

    @Composable
    private fun glass() = Orbit.glass(LocalSkinTokens.current.night)

    /** Text over the map gets a soft shadow in the halo colour, so it reads on any map. */
    @Composable
    private fun TextStyle.floating(color: Color): TextStyle =
        copy(color = color, shadow = Shadow(glass().halo.copy(alpha = 0.85f), Offset(0f, 2f), 6f))

    @Composable
    override fun Backdrop(parts: BackdropParts, modifier: Modifier) {
        val halo = glass().halo
        // O4 (the owner: "the darkening still shows when Waze isn't navigating"): each corner's shade
        // only behind its part, fading in and out with it; the map is clear where nothing is drawn.
        // SH-1 (the owner: "can definitely be done better"): no wide corner gradients any more, but a
        // soft plate that hugs each part (the turn, the song, the clock), feathered at its edge;
        // nothing in the map's free middle. None at all when the driver chose so.
        if (!parts.soft) return
        // SM-1: the fades are read where the plates are drawn, not here: while one runs only its plate
        // is drawn again, instead of the whole backdrop composed again on every frame. Whether a plate is
        // there at all is read through derivedStateOf, which changes twice per fade, not 24 times.
        val turn = animateFloatAsState(if (parts.turn) 1f else 0f, tween(400), label = "turn shade")
        val song = animateFloatAsState(if (parts.song) 1f else 0f, tween(400), label = "song shade")
        val turnOn by remember { derivedStateOf { turn.value > 0f } }
        val songOn by remember { derivedStateOf { song.value > 0f } }
        val night = LocalSkinTokens.current.night
        val strength = if (night) PLATE_NIGHT else PLATE_DAY
        // Keep the last place while fading out.
        val last = remember { arrayOfNulls<net.prototux.aare.ui.car.myplay.FocusGraph.Box>(2) }
        parts.turnBox?.let { last[0] = it }
        parts.songBox?.let { last[1] = it }
        // SH-3: one paint and one blur for every plate, made once per blur size (the plates are drawn
        // one after the other on the main thread; each sets its own colour), not on every frame.
        val blur = with(LocalDensity.current) { 26.dp.toPx() }
        val paint = remember(blur) {
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                maskFilter = android.graphics.BlurMaskFilter(blur, android.graphics.BlurMaskFilter.Blur.NORMAL)
            }
        }
        Box(modifier) {
            // SM-1: each plate on a layer of its own (graphicsLayer), so its blur is drawn again only
            // when the plate itself changes, not whenever something else on the home is redrawn.
            if (turnOn) last[0]?.let { b -> Canvas(Modifier.fillMaxSize().graphicsLayer().testTag(SHADE_TURN_TAG)) { plate(paint, halo, turn.value * strength, b) } }
            if (songOn) last[1]?.let { b -> Canvas(Modifier.fillMaxSize().graphicsLayer().testTag(SHADE_SONG_TAG)) { plate(paint, halo, song.value * strength, b) } }
            parts.statusBox?.let { b -> Canvas(Modifier.fillMaxSize().graphicsLayer().testTag(SHADE_STATUS_TAG)) { plate(paint, halo, strength, b) } }
        }
    }

    /** SH-1: how dark (night) or light (day) a plate's middle is, about. */
    private const val PLATE_NIGHT = 0.5f
    private const val PLATE_DAY = 0.38f

    /**
     * SH-1: a soft plate round [b] (screen pixels): a rounded shape a little wider than the part,
     * its edge blurred away over about 26 dp (a real blur, no bands); [alpha] in the middle.
     */
    private fun androidx.compose.ui.graphics.drawscope.DrawScope.plate(paint: android.graphics.Paint, halo: Color, alpha: Float, b: net.prototux.aare.ui.car.myplay.FocusGraph.Box) {
        val pad = 6.dp.toPx()
        val h = (b.bottom - b.top) + 2 * pad
        val r = minOf(h / 2f, 64.dp.toPx())
        paint.color = halo.copy(alpha = alpha.coerceIn(0f, 1f)).toArgb()
        drawIntoCanvas { c -> c.nativeCanvas.drawRoundRect(b.left - pad, b.top - pad, b.right + pad, b.bottom + pad, r, r, paint) }
    }

    const val SHADE_STATUS_TAG = "orbit_shade_status"

    const val SHADE_TURN_TAG = "orbit_shade_turn"

    /** Look C (18, 19): the turn's spinner, a message's icon and its title. */
    const val TURN_SPINNER_TAG = "orbit_turn_spinner"
    const val TURN_MESSAGE_ICON_TAG = "orbit_turn_message_icon"
    const val TURN_MESSAGE_TITLE_TAG = "orbit_turn_message_title"

    override val drawsNavMessages get() = true

    @Composable
    override fun mapsAppScheme(base: androidx.compose.material3.ColorScheme, night: Boolean) = Orbit.appScheme(base, night, LocalOrbitThemed.current)
    const val SHADE_SONG_TAG = "orbit_shade_song"

    @Composable
    override fun Turn(turn: TurnSlot, modifier: Modifier) = Box(modifier) {
        Scaled {
            val g = glass()
            val nav = Orbit.accent(World.NAVIGATE, LocalSkinTokens.current.night)
            // Look C (18, 19): a message from the maps app in the turn's place: its image in the
            // accent disc, or, while the way is worked out (or it sends no image), the spinner on glass.
            val message = turn.message
            val spinning = message != null && (message.loading || message.icon == null)
            Row(Modifier.carPart(CarPart.OrbitTurn), verticalAlignment = Alignment.Top) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OrbitDisc(132.dp, LocalSkinTokens.current.night, Modifier.coversMap().orbitFocus(LocalStopRinged.current, nav, g.halo),
                    fill = if (spinning) null else nav, edge = if (spinning) null else nav) {
                    val arrow = turn.arrow
                    if (spinning) {
                        OrbitSpinner(92.dp, 8.dp, nav, g.track, Modifier.testTag(TURN_SPINNER_TAG))
                    } else if (message?.icon != null) {
                        Image(message.icon, null, Modifier.size(66.dp).testTag(TURN_MESSAGE_ICON_TAG), contentScale = ContentScale.Fit,
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Orbit.onAccent(LocalSkinTokens.current.night)))
                    } else if (arrow != null) {
                        val image = remember(arrow) { arrow.asImageBitmap() }
                        Image(image, null, Modifier.size(84.dp), contentScale = ContentScale.Fit)
                    } else {
                        Icon(arrowIcon(turn.direction), null, Modifier.size(78.dp), tint = Orbit.onAccent(LocalSkinTokens.current.night))
                    }
                }
                // The lanes for this turn, when the maps app sends them: under the disc, never wider than
                // it, so they stay left of the map's free area (MC-1) and are not a part that counts.
                if (turn.lanes.isNotEmpty()) Lanes(turn.lanes, nav, g, Modifier.padding(top = 10.dp))
              }
                Spacer(Modifier.width(22.dp))
                // LN-1: the text stays centred on the disc; the lanes reach below it without moving it.
                Box(Modifier.weight(1f, fill = false).heightIn(min = 132.dp), contentAlignment = Alignment.CenterStart) {
                  Column {
                    // Look C (18, 19): a message's title is smaller than a distance (46, 40 while loading), on up to two lines.
                    val titleStyle = when {
                        message == null -> Orbit.Type.hero
                        spinning -> Orbit.Type.hero.copy(fontSize = 40.sp, lineHeight = 44.sp)
                        else -> Orbit.Type.hero.copy(fontSize = 46.sp, lineHeight = 50.sp)
                    }
                    turn.distance?.let { Text(it, style = titleStyle.floating(g.text), maxLines = if (message == null) 1 else 2, overflow = TextOverflow.Ellipsis,
                        modifier = if (message != null) Modifier.testTag(TURN_MESSAGE_TITLE_TAG) else Modifier) }
                    turn.road?.let { Text(it, style = Orbit.Type.secondary.floating(g.secondary), maxLines = 2, overflow = TextOverflow.Ellipsis) }
                  }
                }
            }
        }
    }

    /**
     * The lanes as small arrows on a glass pill as wide as the turn disc at most: the lanes to take
     * in Navigate's colour, the others dimmed. The arrows shrink to fit up to [LaneHints.MAX] lanes.
     */
    @Composable
    private fun Lanes(lanes: List<net.prototux.aare.ui.car.myplay.LaneHint>, accent: Color, g: Orbit.Glass, modifier: Modifier) {
        val gap = 3.dp
        val size = laneArrowSize(lanes.size)
        Row(
            modifier.widthIn(max = LANES_WIDTH).clip(CircleShape).background(g.glass).border(1.5.dp, g.edge, CircleShape)
                .padding(horizontal = 8.dp, vertical = 4.dp).testTag(LANES_TAG),
            horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically,
        ) {
            lanes.forEachIndexed { i, lane ->
                Icon(
                    net.prototux.aare.host.templates.laneVector(lane.shape), null,
                    Modifier.size(size).testTag(laneTag(i, lane.recommended)),
                    tint = if (lane.recommended) accent else g.secondary.copy(alpha = 0.45f),
                )
            }
        }
    }

    /** The widest the lanes get: the turn disc's width. */
    val LANES_WIDTH = 132.dp
    const val LANES_TAG = "orbit_lanes"
    fun laneTag(i: Int, recommended: Boolean) = "orbit_lane_${i}_" + if (recommended) "take" else "other"

    /** Each arrow's size for [n] lanes: 26 dp, smaller when many lanes must fit inside [LANES_WIDTH]. */
    fun laneArrowSize(n: Int): androidx.compose.ui.unit.Dp {
        if (n <= 0) return 26.dp
        val room = LANES_WIDTH.value - 16f - 3f * (n - 1)
        return (room / n).coerceIn(11f, 26f).dp
    }

    /** The arrow for a turn MyPlay can draw; otherwise only the plain navigation arrow, never a guessed direction. */
    fun arrowIcon(d: TurnDirection?): ImageVector = when (d) {
        TurnDirection.STRAIGHT -> Icons.Rounded.Straight
        TurnDirection.SLIGHT_LEFT -> Icons.Rounded.TurnSlightLeft
        TurnDirection.LEFT -> Icons.Rounded.TurnLeft
        TurnDirection.SHARP_LEFT -> Icons.Rounded.TurnSharpLeft
        TurnDirection.SLIGHT_RIGHT -> Icons.Rounded.TurnSlightRight
        TurnDirection.RIGHT -> Icons.Rounded.TurnRight
        TurnDirection.SHARP_RIGHT -> Icons.Rounded.TurnSharpRight
        TurnDirection.U_TURN_LEFT -> Icons.Rounded.UTurnLeft
        TurnDirection.U_TURN_RIGHT -> Icons.Rounded.UTurnRight
        null -> Icons.Rounded.Navigation
    }

    /**
     * MC-1 (the owner: Waze's free area centred, the song's text may lie over the map): the free
     * area is centred on the screen, and only ORBIT's hard parts count: the hub, the clock and
     * battery, and the turn's and the song's discs (marked in [Turn] and [Song]). The texts beside
     * the discs, the alert pill and the music panel (both come and go) do not.
     */
    override val mapAreaCentred get() = true
    override fun coversMap(slot: Slot): Boolean = slot == Slot.HUB || slot == Slot.STATUS || slot == Slot.SPEED

    /** AL-1: ORBIT draws the maps app's alerts itself, compact, in the free lower middle. */
    override val drawsMapAlerts get() = true

    /**
     * AL-1 (the owner's photo: Waze's wide grey alert card covered the turn): the alert as a compact
     * glass pill at the bottom, between the hub and the song (MC-2: the engine places it, clear of
     * the middle where the maps app's arrow is):
     * its icon (or a caution sign), the title and the subtitle on one line each, and its buttons as
     * small pills the knob reaches.
     */
    @Composable
    override fun MapAlertCard(alert: net.prototux.aare.ui.car.myplay.MapAlert, ringed: String?, stop: (String) -> Modifier, modifier: Modifier) = Box(modifier) {
        Scaled {
            val night = LocalSkinTokens.current.night
            val g = Orbit.glass(night)
            val caution = Orbit.caution(night)
            val accent = Orbit.accent(World.NAVIGATE, night)
            val pill = androidx.compose.foundation.shape.RoundedCornerShape(40.dp)
            Row(
                Modifier.widthIn(max = 600.dp).clip(pill).background(g.glass).border(2.dp, g.edge, pill)
                    .padding(start = 14.dp, end = 18.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Look C (design 20): the alert's time left runs as a ring round its icon, so the driver
                // sees it will go by itself; none for an alert with no time limit.
                AlertCountdown(alert, caution, g.track) {
                    Box(Modifier.size(52.dp).clip(CircleShape).background(caution.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                        val icon = alert.icon
                        if (icon != null) Image(icon, null, Modifier.size(34.dp), contentScale = ContentScale.Fit)
                        else Icon(Icons.Rounded.Warning, null, Modifier.size(32.dp), tint = caution)
                    }
                }
                Column(Modifier.weight(1f, fill = false)) {
                    Text(alert.title, style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, color = g.text), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    alert.subtitle?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = TextStyle(fontSize = 20.sp, color = g.secondary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                alert.actions.forEachIndexed { i, (label, _) ->
                    val id = net.prototux.aare.ui.car.myplay.mapAlertStop(i)
                    // Polish: a long label (the app chooses it) is cut with "…" rather than squeezing the
                    // alert's own title out of the pill.
                    Text(
                        label, style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = g.text), maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = stop(id).widthIn(max = ALERT_BUTTON_MAX).orbitFocusRounded(ringed == id, accent, g.halo, 24.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp)).background(g.track)
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }

    /**
     * Look C (design 20): [content] (the alert's icon) inside a ring that empties, clockwise from the
     * top, over the alert's time left. The time is read only where the arc is drawn, so the pill is
     * not composed again while it runs. An alert with no time limit gets no ring.
     */
    @Composable
    private fun AlertCountdown(alert: net.prototux.aare.ui.car.myplay.MapAlert, colour: Color, track: Color, content: @Composable () -> Unit) {
        val start = net.prototux.aare.ui.car.myplay.alertTimeLeft(alert, SystemClock.uptimeMillis())
        if (start == null) { content(); return }
        val left = remember(alert.key) { androidx.compose.animation.core.Animatable(start) }
        LaunchedEffect(alert.key) {
            val from = net.prototux.aare.ui.car.myplay.alertTimeLeft(alert, SystemClock.uptimeMillis()) ?: return@LaunchedEffect
            left.snapTo(from)
            left.animateTo(0f, androidx.compose.animation.core.tween((from * alert.durationMs).toInt(), easing = androidx.compose.animation.core.LinearEasing))
        }
        Box(
            Modifier.size(ALERT_RING).testTag(ALERT_COUNTDOWN_TAG).drawBehind {
                val w = 5.dp.toPx()
                val inset = w / 2
                val arc = androidx.compose.ui.geometry.Size(size.width - w, size.height - w)
                drawArc(track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(w))
                val v = left.value
                if (v > 0f) drawArc(colour, -90f, 360f * v, false, Offset(inset, inset), arc, style = Stroke(w, cap = StrokeCap.Round))
            },
            contentAlignment = Alignment.Center,
        ) { content() }
    }

    /** Look C (design 10): parked search typed on the letter ring. */
    override val letterRing: (@Composable (net.prototux.aare.ui.car.myplay.LetterRingInput, Modifier) -> Unit)
        get() = { input, modifier -> LetterRingView(input, modifier) }

    /**
     * Look C (design 10): a ring of letters (A to Z, then space, delete and Search) round the text
     * typed so far. Turn the knob to a key (left and right on the pad jump five), push to type it; down or Search
     * searches, Back closes it. A tap types the key tapped. The middle shows the text, the key the
     * knob is on, and how many places the app has found so far.
     */
    @Composable
    private fun LetterRingView(input: net.prototux.aare.ui.car.myplay.LetterRingInput, modifier: Modifier) {
        val keys = net.prototux.aare.ui.car.myplay.LetterRing.KEYS
        var at by remember { androidx.compose.runtime.mutableIntStateOf(0) }
        val focus = remember { androidx.compose.ui.focus.FocusRequester() }
        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
        // LR-2: while something over the app holds the knob (a ringing call, a notice with buttons), Back is its.
        androidx.activity.compose.BackHandler(enabled = !input.held()) { input.hide() }
        // LR-4: the taps' gesture coroutines keep the first lambda they saw; they reach the latest
        // input (a new one comes with every key typed) through this.
        val inputNow by androidx.compose.runtime.rememberUpdatedState(input)
        fun press(i: Int) { at = i; net.prototux.aare.ui.car.myplay.LetterRing.press(keys[i], inputNow) }
        Box(
            modifier.fillMaxSize().testTag(LETTER_RING_TAG).focusRequester(focus).focusable()
                .onPreviewKeyEvent { e ->
                    if (e.type != androidx.compose.ui.input.key.KeyEventType.KeyDown) return@onPreviewKeyEvent when (e.key) {
                        Key.Tab, Key.DirectionLeft, Key.DirectionRight, Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.DirectionDown -> true
                        else -> false
                    }
                    when (e.key) {
                        Key.Tab -> { at = net.prototux.aare.ui.car.myplay.LetterRing.move(at, if (e.isShiftPressed) -1 else 1); true }
                        // The pad crosses the ring faster than the knob: a jump of PAD_JUMP keys.
                        Key.DirectionRight -> { at = net.prototux.aare.ui.car.myplay.LetterRing.move(at, net.prototux.aare.ui.car.myplay.LetterRing.PAD_JUMP); true }
                        Key.DirectionLeft -> { at = net.prototux.aare.ui.car.myplay.LetterRing.move(at, -net.prototux.aare.ui.car.myplay.LetterRing.PAD_JUMP); true }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { press(at); true }
                        Key.DirectionDown -> { input.submit(); true }
                        else -> false
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Scaled {
                val night = LocalSkinTokens.current.night
                val g = Orbit.glass(night)
                val accent = Orbit.accent(World.NAVIGATE, night)
                val onAccent = Orbit.onAccent(night)
                Box(Modifier.size(LETTER_RING).clip(CircleShape).background(g.glass).border(2.dp, g.edge, CircleShape), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(400.dp).border(2.dp, g.edge, CircleShape))
                    keys.forEachIndexed { i, key ->
                        val a = Math.toRadians(net.prototux.aare.ui.car.myplay.LetterRing.angleDeg(i).toDouble())
                        val on = i == at
                        val disc = when {
                            on -> 76.dp
                            key is net.prototux.aare.ui.car.myplay.LetterRing.Key.Letter -> 52.dp
                            else -> 48.dp
                        }
                        val fill = when {
                            on || key == net.prototux.aare.ui.car.myplay.LetterRing.Key.Search -> accent
                            key is net.prototux.aare.ui.car.myplay.LetterRing.Key.Letter -> Color.Transparent
                            else -> g.track
                        }
                        val content = if (on || key == net.prototux.aare.ui.car.myplay.LetterRing.Key.Search) onAccent else g.text
                        Box(
                            Modifier.offset((LETTER_RADIUS.value * kotlin.math.cos(a)).dp, (LETTER_RADIUS.value * kotlin.math.sin(a)).dp)
                                .size(disc).let { if (on) it.orbitFocus(true, accent, g.halo) else it }
                                .clip(CircleShape).background(fill).testTag(letterKeyTag(i))
                                // LR-1: a tap types the key; not a focus stop (the ring owns the focus and its own ring).
                                .pointerInput(i) { detectTapGestures { press(i) } },
                            contentAlignment = Alignment.Center,
                        ) {
                            when (key) {
                                is net.prototux.aare.ui.car.myplay.LetterRing.Key.Letter ->
                                    Text(key.c.toString(), style = TextStyle(fontSize = if (on) 38.sp else 28.sp, fontWeight = FontWeight.ExtraBold, color = content), maxLines = 1)
                                net.prototux.aare.ui.car.myplay.LetterRing.Key.Space ->
                                    Icon(Icons.Rounded.SpaceBar, stringResource(R.string.myplay_letter_ring_space), Modifier.size(if (on) 38.dp else 26.dp), tint = content)
                                net.prototux.aare.ui.car.myplay.LetterRing.Key.Delete ->
                                    Icon(Icons.AutoMirrored.Rounded.Backspace, stringResource(R.string.myplay_letter_ring_delete), Modifier.size(if (on) 34.dp else 24.dp), tint = content)
                                net.prototux.aare.ui.car.myplay.LetterRing.Key.Search ->
                                    Icon(Icons.Rounded.Search, stringResource(R.string.myplay_letter_ring_search), Modifier.size(if (on) 38.dp else 26.dp), tint = content)
                            }
                        }
                    }
                    Column(Modifier.width(320.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(30.dp)).background(g.glass).border(2.dp, accent, RoundedCornerShape(30.dp))
                                .padding(horizontal = 18.dp).testTag(LETTER_RING_TEXT_TAG),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Search, null, Modifier.size(26.dp), tint = g.secondary)
                            Spacer(Modifier.width(10.dp))
                            if (input.query.isEmpty()) Text(input.hint, style = TextStyle(fontSize = 26.sp, color = g.secondary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            else Text(input.query + "|", style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, color = g.text), maxLines = 1, overflow = TextOverflow.StartEllipsis)
                        }
                        Spacer(Modifier.height(10.dp))
                        when (val k = keys[at]) {
                            is net.prototux.aare.ui.car.myplay.LetterRing.Key.Letter ->
                                Text(k.c.toString(), style = TextStyle(fontSize = 96.sp, lineHeight = 100.sp, fontWeight = FontWeight.ExtraBold, color = accent), maxLines = 1)
                            else -> Icon(
                                when (k) {
                                    net.prototux.aare.ui.car.myplay.LetterRing.Key.Space -> Icons.Rounded.SpaceBar
                                    net.prototux.aare.ui.car.myplay.LetterRing.Key.Delete -> Icons.AutoMirrored.Rounded.Backspace
                                    else -> Icons.Rounded.Search
                                }, null, Modifier.size(84.dp).padding(vertical = 8.dp), tint = accent,
                            )
                        }
                        Text(stringResource(R.string.myplay_letter_ring_push), style = TextStyle(fontSize = 20.sp, color = g.secondary), maxLines = 1)
                        input.found?.let { n ->
                            Text(
                                pluralStringResource(R.plurals.myplay_letter_ring_found, n, n),
                                style = TextStyle(fontSize = 20.sp, color = g.secondary), maxLines = 1,
                                modifier = Modifier.padding(top = 6.dp).testTag(LETTER_RING_FOUND_TAG),
                            )
                        }
                    }
                }
            }
        }
    }

    /** Look C (design 10): the ring's size and the letters' radius, in the design's pixels. */
    val LETTER_RING = 620.dp
    val LETTER_RADIUS = 252.dp
    const val LETTER_RING_TAG = "orbit_letter_ring"
    const val LETTER_RING_TEXT_TAG = "orbit_letter_ring_text"
    const val LETTER_RING_FOUND_TAG = "orbit_letter_ring_found"
    fun letterKeyTag(i: Int) = "orbit_letter_key_$i"

    /** Look C: the countdown ring round an alert's icon (design 20's 84 px ring, less the pill's padding). */
    val ALERT_RING = 66.dp
    const val ALERT_COUNTDOWN_TAG = "orbit_alert_countdown"

    /** HN-2: the hints round the hub (OrbitHints.kt). */
    @Composable
    override fun PadHints(hints: net.prototux.aare.ui.car.myplay.PadHints, modifier: Modifier) = Box(modifier) {
        Scaled { OrbitPadHints(hints, hints.speedShown, LocalSkinTokens.current.night, Modifier) }
    }

    /** O8: the speed (and the limit) is in the hub, with the trip, not a part of its own. */
    override val speedInHub get() = true

    /**
     * O8 (the owner: fewer separate parts; "I would like to actually see the speed limit in
     * number"): the hub with the speed in its middle (amber when over the limit by the margin), the
     * trip's arrival and time left under it, and the limit as a road-sign ring with its number on
     * the hub's edge (grey with a dash when no limit is known).
     */
    @Composable
    override fun HubSpeed(trip: TripSlot?, speed: SpeedReading?, modifier: Modifier) = Box(modifier) {
        Scaled {
            val g = glass()
            val night = LocalSkinTokens.current.night
            val nav = Orbit.accent(World.NAVIGATE, night)
            Box(Modifier.size(212.dp)) {
                OrbitDisc(192.dp, night, Modifier.align(Alignment.BottomStart).carPart(CarPart.OrbitHub).orbitFocus(LocalStopRinged.current, nav, g.halo)) {
                    OrbitRing(trip?.progress, 184.dp, 9.dp, nav, g.track, Modifier.testTag(TRIP_RING_TAG).progressSemantics(trip?.progress)) {
                        Column(Modifier.testTag(HUB_SPEED_TAG).carPart(CarPart.OrbitSpeed), horizontalAlignment = Alignment.CenterHorizontally) {
                            // Option A: over an estimated limit, the caution colour is softer (never stronger than a sign's).
                            val colour = if (speed?.over == true) overColour(speed.limit, night, g.text) else g.text
                            Text(speed?.kmh?.toString() ?: "–", style = TextStyle(fontSize = 64.sp, lineHeight = 64.sp, fontWeight = FontWeight.ExtraBold, color = colour, fontFeatureSettings = "tnum"), maxLines = 1)
                            Text(stringResource(R.string.myplay_speed_unit_kmh), style = TextStyle(fontSize = 18.sp, lineHeight = 20.sp, color = g.secondary), maxLines = 1)
                            // SP-1: where the speed comes from, in the picture and with the Developer check.
                            if (net.prototux.aare.ui.car.myplay.LocalSpeedSourceMark.current && speed != null) {
                                Text(stringResource(if (speed.from == SpeedReading.From.CAR) R.string.myplay_speed_from_car else R.string.myplay_speed_from_gps), maxLines = 1,
                                    style = TextStyle(fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold, color = g.secondary),
                                    modifier = Modifier.testTag(SPEED_SOURCE_TAG))
                            }
                            // O8-1 (the Reviewer: "arrival · time left" got too small to read at a glance):
                            // one value only, the time left (else the distance, else the arrival), larger.
                            (trip?.timeLeft ?: trip?.distanceLeft ?: trip?.arrival)?.let {
                                FitText(it, TextStyle(fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, color = g.text), 150.dp)
                            }
                        }
                    }
                }
                speed?.limit?.let { LimitSign(it, night, Modifier.align(Alignment.TopEnd)) }
            }
        }
    }

    /** O8: the limit as a road sign: a red ring, white inside, the number in black; grey with a dash when unknown. */
    @Composable
    private fun LimitSign(limit: LimitShown, night: Boolean, modifier: Modifier) {
        val known = limit as? LimitShown.Known
        val ring = if (known != null) Color(0xFFD32F2F) else Orbit.glass(night).secondary
        // Option A, SL-1: a defaulted limit (the road's kind or place, no sign tagged) in a dashed ring: an estimate, not a sign.
        val guessed = known?.defaulted == true
        Box(
            modifier.limitPulse(known?.kmh).size(84.dp).shadow(6.dp, CircleShape).clip(CircleShape).background(limitSignFill(night))
                .then(if (guessed) Modifier.dashedRing(9.dp, ring) else Modifier.border(9.dp, ring, CircleShape))
                .testTag(if (guessed) LIMIT_SIGN_GUESSED_TAG else LIMIT_SIGN_TAG),
            contentAlignment = Alignment.Center,
        ) {
            Text(known?.kmh?.toString() ?: "–", style = TextStyle(fontSize = if ((known?.kmh ?: 0) >= 100) 28.sp else 34.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF111111)), maxLines = 1)
        }
    }

    /** The speed's colour over the limit: the caution amber, or halfway to the text colour when the limit is an estimate (never stronger). */
    fun overColour(limit: LimitShown?, night: Boolean, text: Color): Color =
        if ((limit as? LimitShown.Known)?.defaulted == true) androidx.compose.ui.graphics.lerp(Orbit.caution(night), text, 0.45f) else Orbit.caution(night)

    /** Option A: a ring of dashes [width] wide inside the edge, for an estimated limit. */
    private fun Modifier.dashedRing(width: androidx.compose.ui.unit.Dp, colour: Color): Modifier = drawWithContent {
        drawContent()
        val w = width.toPx()
        val r = size.minDimension / 2 - w / 2
        val dash = (2 * Math.PI.toFloat() * r) / 16f
        drawCircle(colour, r, center, style = Stroke(w, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(dash * 0.6f, dash * 0.4f))))
    }

    /**
     * #7: one soft pulse of the limit sign when the limit changes (LimitPulse's rules), drawn only:
     * the sign's place and size for the layout stay the same, so neither the map's free area nor the
     * hints round the hub move. No loop, no sound.
     */
    @Composable
    private fun Modifier.limitPulse(kmh: Int?): Modifier {
        if (!LocalLimitPulse.current) return this
        val rule = remember { LimitPulse() }
        val scale = remember { androidx.compose.animation.core.Animatable(1f) }
        LaunchedEffect(kmh) {
            if (rule.update(kmh, System.currentTimeMillis())) {
                val half = LimitPulse.PULSE_MS / 2
                scale.animateTo(LimitPulse.SCALE, androidx.compose.animation.core.tween(half))
                scale.animateTo(1f, androidx.compose.animation.core.tween(half))
            }
        }
        return this.graphicsLayer { scaleX = scale.value; scaleY = scale.value }
    }

    /**
     * Polish #2: the limit sign's inside: white by day; at night a light grey (85 %), so it does not
     * glare on the dark map. The red ring and the black number stay at full strength.
     */
    fun limitSignFill(night: Boolean): Color = if (night) LIMIT_FILL_NIGHT else Color.White
    val LIMIT_FILL_NIGHT = Color(0xFFD9D9D9)

    const val HUB_SPEED_TAG = "orbit_hub_speed"
    const val SPEED_SOURCE_TAG = "orbit_speed_source"
    const val LIMIT_SIGN_TAG = "orbit_limit_sign"
    /** Option A, SL-1: the limit sign when the limit is an estimate (DEFAULTED: dashed ring). */
    const val LIMIT_SIGN_GUESSED_TAG = "orbit_limit_sign_guessed"

    @Composable
    override fun Hub(trip: TripSlot?, modifier: Modifier) = Box(modifier) {
        Scaled {
            val g = glass()
            val nav = Orbit.accent(World.NAVIGATE, LocalSkinTokens.current.night)
            OrbitDisc(192.dp, LocalSkinTokens.current.night, Modifier.carPart(CarPart.OrbitHub).orbitFocus(LocalStopRinged.current, nav, g.halo)) {
                OrbitRing(trip?.progress, 184.dp, 9.dp, nav, g.track, Modifier.testTag(TRIP_RING_TAG).progressSemantics(trip?.progress)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val arrival = trip?.arrival
                        if (arrival != null) {
                            // "10:31 AM" in 12 hour style is wider than the ring: shrink it to fit.
                            FitText(arrival, Orbit.Type.clock.copy(color = g.text), 150.dp)
                            (trip.timeLeft ?: trip.distanceLeft)?.let { Text(it, style = TextStyle(fontSize = 24.sp, color = g.secondary), maxLines = 1) }
                        } else {
                            Icon(Icons.Rounded.DonutLarge, null, Modifier.size(64.dp), tint = nav)
                            Text(stringResource(R.string.myplay_hub_menu), style = TextStyle(fontSize = 24.sp, color = g.secondary))
                        }
                    }
                }
            }
        }
    }

    override val statusInSong get() = true

    /**
     * Look C (35, the owner's option C): the song's corner carries the status. The art in its ring,
     * the time as a badge on the ring's top, the outside temperature (and the ice sign) and the
     * battery (only when low or charging) as pills at its left side. The song's name is not in the
     * corner any more (the sheet draws none); the music panel and the Music world still show it.
     */
    @Composable
    override fun Song(song: SongSlot, modifier: Modifier) = Box(modifier) {
        Scaled {
            val g = glass()
            val musicColour = Orbit.accent(World.MUSIC, LocalSkinTokens.current.night)
            StatusCorner(badge = true, Modifier.carPart(CarPart.OrbitSong)) {
                SongRing(song, musicColour, g.track, Modifier.testTag(SONG_RING_TAG).orbitFocus(LocalStopRinged.current, musicColour, g.halo)
                    .semantics { contentDescription = listOf(song.title, song.subtitle).filter { it.isNotBlank() }.joinToString(", ") }) {
                    val art = song.art
                    if (art != null) {
                        val image = remember(art) { art.asImageBitmap() }
                        Image(image, null, Modifier.requiredSize(126.dp).aspectRatio(1f).clip(CircleShape).testTag(SONG_ART_TAG), contentScale = ContentScale.Crop)
                    } else {
                        // No artwork from the player: the sheet's gradient with a note.
                        Box(Modifier.size(126.dp).clip(CircleShape).background(ART).testTag(SONG_ART_FALLBACK_TAG), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.MusicNote, null, Modifier.size(56.dp), tint = Color.White)
                        }
                    }
                }
            }
        }
    }

    /** Look C (35, 35-status-c-nomusic): nothing playing: the empty ring with the time in its middle, the pills beside it. */
    @Composable
    override fun NoSong(modifier: Modifier) = Box(modifier) {
        Scaled {
            val g = glass()
            StatusCorner(badge = false, Modifier) {
                OrbitRing(null, SONG_RING, 7.dp, g.track, g.track, Modifier.testTag(NO_SONG_TAG)) {
                    OrbitDisc(126.dp, LocalSkinTokens.current.night) {
                        ClockText(TextStyle(fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 40.sp), Alignment.Center)
                    }
                }
            }
        }
    }

    /** The song's ring, as the sheet. */
    private val SONG_RING = 152.dp
    /**
     * Look C (35): the status corner's fixed size: the ring, the badge over its top, the pills' room
     * at its left. SC-1: 208 wide, no wider than the clock column it replaced reached in (the free area
     * is centred, so every dp here is taken from the left side too); a wide pill may reach a little
     * past its left edge, over the map.
     */
    private val CORNER_W = 208.dp
    private val CORNER_H = SONG_RING + 26.dp
    /** Where the pills' middles are: a little left of the ring's edge, above and below its middle (the sheet). */
    private val PILL_X = CORNER_W - SONG_RING - 6.dp
    private val TEMP_Y = 26.dp + SONG_RING / 2 - 34.dp
    private val BATTERY_Y = 26.dp + SONG_RING / 2 + 40.dp
    private val PILL_H = 42.dp
    /** SC-3: the widest a pill gets: centred on [PILL_X], it ends at the corner's left edge. */
    private val PILL_MAX = PILL_X * 2
    private val PILL_PAD = 10.dp
    private val PILL_ICON = 22.dp

    /**
     * Look C (35): the corner's parts. Always [CORNER_W] by [CORNER_H] whatever shows (the badge,
     * a pill or none), so the map's free area never moves when the battery or the temperature comes
     * or goes; that whole box is what covers the map.
     */
    @Composable
    private fun StatusCorner(badge: Boolean, modifier: Modifier, ring: @Composable () -> Unit) {
        val g = glass()
        val night = LocalSkinTokens.current.night
        Box(modifier.size(CORNER_W, CORNER_H).coversMap().testTag(STATUS_CORNER_TAG)) {
            Box(Modifier.align(Alignment.BottomEnd)) { ring() }
            if (badge) {
                Box(Modifier.align(Alignment.TopEnd).width(SONG_RING), contentAlignment = Alignment.TopCenter) {
                    StatusPill(52.dp, Modifier.testTag(CLOCK_BADGE_TAG)) {
                        ClockText(TextStyle(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 34.sp), Alignment.Center)
                    }
                }
            }
            // The temperature and the ice sign, at the ring's left. DW-1: its place is kept (empty when warm).
            val outside = LocalOutsideTemp.current
            val ice = LocalRoadIce.current
            PillPlace(TEMP_Y, Modifier.testTag(ICE_SLOT_TAG)) {
                if (ice || outside.degrees != null) TempPill(outside.degrees, ice)
            }
            // The battery, only when low (red) or charging. N5-3: never when the phone does not tell it.
            val battery = rememberPhoneStatus()?.takeIf { it.batteryKnown && batteryShown(it.battery, it.charging) }
            battery?.let { b ->
                PillPlace(BATTERY_Y) { BatteryPill(b.battery, b.charging) }
            }
        }
    }

    /** Look C (35): the outside temperature, with the ice sign (and in the warning colour) when icy. */
    @Composable
    internal fun TempPill(degrees: Int?, ice: Boolean) {
        val night = LocalSkinTokens.current.night
        val colour = if (ice) Orbit.warning(night) else glass().text
        StatusPill(PILL_H, Modifier.widthIn(max = PILL_MAX).testTag(TEMP_PILL_TAG), PILL_PAD) {
            if (ice) Icon(Icons.Rounded.AcUnit, stringResource(R.string.myplay_warning_ice_sign), Modifier.size(PILL_ICON).testTag(ICE_TAG), tint = colour)
            degrees?.let {
                // SC-3: a long reading ("-12°" with the ice sign) is made smaller, never wider than the pill.
                Box(Modifier.testTag(OUTSIDE_TEMP_TAG)) {
                    FitText("$it°", PILL_TEXT.copy(fontSize = 24.sp, lineHeight = 24.sp, color = colour),
                        PILL_MAX - PILL_PAD * 2 - if (ice) PILL_ICON + 6.dp else 0.dp)
                }
            }
        }
    }

    /** Look C (35): the battery, red when low; shown only when low or charging ([batteryShown]). */
    @Composable
    internal fun BatteryPill(level: Int, charging: Boolean) {
        val night = LocalSkinTokens.current.night
        val colour = if (batteryLow(level, charging)) Orbit.warning(night) else Orbit.accent(World.NAVIGATE, night)
        StatusPill(PILL_H, Modifier.widthIn(max = PILL_MAX).testTag(BATTERY_TAG).carPart(CarPart.Status), PILL_PAD) {
            Icon(batteryIcon(level, charging), null, Modifier.size(PILL_ICON), tint = colour)
            // SC-3: "100%" while charging is made smaller, never wider than the pill.
            Box(Modifier.testTag(BATTERY_NUMBER_TAG)) {
                FitText("$level%", PILL_TEXT.copy(fontSize = 22.sp, lineHeight = 22.sp, color = colour), PILL_MAX - PILL_PAD * 2 - PILL_ICON - 6.dp)
            }
        }
    }

    private val PILL_TEXT = TextStyle(fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum")

    /**
     * A pill's place: centred on ([PILL_X], [y]), [PILL_MAX] wide. SC-3: a pill is never wider, so it
     * never draws past the corner's left edge over the map (outside what the corner says it covers).
     */
    @Composable
    private fun BoxScope.PillPlace(y: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
        Box(
            modifier.align(Alignment.TopStart).offset(y = y - PILL_H / 2).width(PILL_MAX).height(PILL_H),
            contentAlignment = Alignment.Center,
        ) { content() }
    }

    /** Look C (35): a small dark pill with the glass's edge, its parts in a row. */
    @Composable
    private fun StatusPill(height: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier, pad: androidx.compose.ui.unit.Dp = 14.dp, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
        val g = glass()
        val shape = RoundedCornerShape(50)
        Row(
            modifier.height(height).clip(shape).background(g.glass.copy(alpha = 1f)).border(2.dp, g.edge, shape).padding(horizontal = pad),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), content = content,
        )
    }

    /**
     * The time, ticking on the minute; it keeps the width of "88:88" (figures of one width), so the
     * badge never changes size at 9:59 -> 10:00.
     */
    @Composable
    private fun ClockText(style: TextStyle, align: Alignment) {
        val g = glass()
        val context = LocalContext.current
        var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(1_000 - now % 1_000 + 5)
                now = System.currentTimeMillis()
            }
        }
        val time = DateFormat.format(if (DateFormat.is24HourFormat(context)) "H:mm" else "h:mm", now).toString()
        val clockStyle = style.copy(color = g.text, fontFeatureSettings = "tnum")
        val measurer = rememberTextMeasurer()
        val width = remember(measurer, clockStyle) { measurer.measure("88:88", clockStyle).size.width }
        Box(Modifier.width(with(LocalDensity.current) { width.toDp() }), contentAlignment = align) {
            Text(time, style = clockStyle, maxLines = 1, modifier = Modifier.carPart(CarPart.Clock).testTag(CLOCK_TAG))
        }
    }

    /**
     * The song's ring with its progress. SM-3: the once-a-second clock lives here, so each tick
     * composes only the ring again, not the whole song corner (the title, the artist, the art).
     */
    @Composable
    private fun SongRing(song: SongSlot, colour: Color, track: Color, modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
        // The player's position is sampled on SystemClock.elapsedRealtime() (PlaybackInfo.sampledAtMs),
        // so "now" must be on the same clock, not the wall clock.
        var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
        LaunchedEffect(song.playing, song.updatedAtMs) {
            now = SystemClock.elapsedRealtime()
            while (song.playing) {
                delay(1_000)
                now = SystemClock.elapsedRealtime()
            }
        }
        val progress = if (song.durationMs > 0) mediaProgress(song.positionMs, song.durationMs, song.playing, song.updatedAtMs, now) else null
        OrbitRing(progress, 152.dp, 7.dp, colour, track, modifier.progressSemantics(progress), content)
    }

    /** One line of [text] in [style], made smaller when it is wider than [width]. */
    @Composable
    private fun FitText(text: String, style: TextStyle, width: androidx.compose.ui.unit.Dp) {
        val measurer = rememberTextMeasurer()
        val room = with(LocalDensity.current) { width.toPx() }
        val needed = remember(text, style) { measurer.measure(text, style, maxLines = 1).size.width }
        val fit = if (needed > room && needed > 0) room / needed else 1f
        // SC-4: a style without a line height (or size) keeps it unspecified; Unspecified * fit throws.
        Text(text, style = style.copy(
            fontSize = if (style.fontSize.isSpecified) style.fontSize * fit else style.fontSize,
            lineHeight = if (style.lineHeight.isSpecified) style.lineHeight * fit else style.lineHeight,
        ), maxLines = 1)
    }

    /** The sheet's artwork stand-in when a song has none. */
    private val ART = Brush.linearGradient(listOf(Color(0xFF4CC9F0), Color(0xFF8F5BFF), Color(0xFFFF5A8A)))

    @Composable
    override fun Status(modifier: Modifier) = Box(modifier) {
        Scaled {
            val g = glass()
            val nav = Orbit.accent(World.NAVIGATE, LocalSkinTokens.current.night)
            val context = LocalContext.current
            var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(Unit) {
                while (true) {
                    delay(1_000 - now % 1_000 + 5)
                    now = System.currentTimeMillis()
                }
            }
            val time = DateFormat.format(if (DateFormat.is24HourFormat(context)) "H:mm" else "h:mm", now).toString()
            Column(horizontalAlignment = Alignment.End) {
                // Polish: the clock keeps one width (as wide as "88:88", figures of one width), so the turn
                // of the minute or the hour (9:59 -> 10:00) never changes the column's width, and with it
                // the map's free area; right-aligned as before.
                val clockStyle = Orbit.Type.clock.copy(fontFeatureSettings = "tnum")
                val clockMeasurer = rememberTextMeasurer()
                val clockWidth = remember(clockMeasurer) { clockMeasurer.measure("88:88", clockStyle).size.width }
                Box(Modifier.width(with(LocalDensity.current) { clockWidth.toDp() }), contentAlignment = Alignment.CenterEnd) {
                    Text(time, style = clockStyle.floating(g.text), maxLines = 1, modifier = Modifier.carPart(CarPart.Clock).testTag(CLOCK_TAG))
                }
                // N5-3: no level shown when the phone does not tell it, never "0%".
                val battery = rememberPhoneStatus()?.takeIf { it.batteryKnown }
                val ice = LocalRoadIce.current
                run {
                    val night = LocalSkinTokens.current.night
                    Row(
                        Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Drive warnings: 4 °C or less outside. DW-1: its place is always kept (empty when
                        // warm), so the column's size, and with it the map's free area, never changes because of it.
                        // Polish #5: the outside temperature, when the car reports it; its place kept all drive
                        // (as wide as "-88°") so the column never changes width when a reading comes or goes.
                        val outside = LocalOutsideTemp.current
                        if (outside.reserve) {
                            val tempStyle = TextStyle(fontSize = 28.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontFeatureSettings = "tnum")
                            val measurer = rememberTextMeasurer()
                            val widest = remember(measurer) { measurer.measure("-88°", tempStyle).size.width }
                            Box(Modifier.width(with(LocalDensity.current) { widest.toDp() }).testTag(OUTSIDE_TEMP_TAG), contentAlignment = Alignment.CenterEnd) {
                                outside.degrees?.let { Text("$it°", style = tempStyle.floating(if (ice) Orbit.warning(night) else g.text), maxLines = 1) }
                            }
                        }
                        Box(Modifier.size(30.dp).testTag(ICE_SLOT_TAG)) {
                            if (ice) Icon(Icons.Rounded.AcUnit, stringResource(R.string.myplay_warning_ice_sign), Modifier.size(30.dp).testTag(ICE_TAG), tint = Orbit.warning(night))
                        }
                        battery?.let { s ->
                            val colour = if (batteryLow(s.battery, s.charging)) Orbit.warning(night) else nav
                            Row(
                                Modifier.testTag(BATTERY_TAG).carPart(CarPart.Status),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(batteryIcon(s.battery, s.charging), null, Modifier.size(30.dp), tint = colour)
                                // Polish #3: the number keeps the width of "100%" (figures of one width), so the
                                // column, and the map's free area with it, never shifts as the level changes.
                                val numberStyle = TextStyle(fontSize = 28.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontFeatureSettings = "tnum")
                                val measurer = rememberTextMeasurer()
                                val widest = remember(measurer) { measurer.measure("100%", numberStyle).size.width }
                                Box(Modifier.width(with(LocalDensity.current) { widest.toDp() }), contentAlignment = Alignment.CenterEnd) {
                                    Text("${s.battery}%", style = numberStyle.floating(colour), maxLines = 1, modifier = Modifier.testTag(BATTERY_NUMBER_TAG))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** A battery this low (and not charging) shows in the warning colour. */
    const val BATTERY_LOW = 20

    fun batteryLow(level: Int, charging: Boolean) = !charging && level <= BATTERY_LOW

    /** Look C (35): the corner shows the battery only when it matters: low, or charging. */
    fun batteryShown(level: Int, charging: Boolean) = charging || level <= BATTERY_LOW

    /** The battery icon with as many bars as the level (0 to 6, full at 95 % and up). */
    fun batteryIcon(level: Int, charging: Boolean): ImageVector = when {
        charging -> Icons.Rounded.BatteryChargingFull
        level >= 95 -> Icons.Rounded.BatteryFull
        else -> when ((level.coerceIn(0, 100) * 7) / 100) {
            0 -> Icons.Rounded.Battery0Bar
            1 -> Icons.Rounded.Battery1Bar
            2 -> Icons.Rounded.Battery2Bar
            3 -> Icons.Rounded.Battery3Bar
            4 -> Icons.Rounded.Battery4Bar
            5 -> Icons.Rounded.Battery5Bar
            else -> Icons.Rounded.Battery6Bar
        }
    }

    @Composable
    override fun Speed(reading: SpeedReading?, modifier: Modifier) = Box(modifier) {
        Scaled {
            val g = glass()
            val night = LocalSkinTokens.current.night
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OrbitDisc(104.dp, night, Modifier.padding(top = 12.dp).carPart(CarPart.OrbitSpeed)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // N20: over the limit by more than the margin, the number turns a calm amber; no sound.
                        val colour = if (reading?.over == true) overColour(reading.limit, night, g.text) else g.text
                        Text(reading?.kmh?.toString() ?: "–", style = Orbit.Type.name.copy(color = colour, lineHeight = 36.sp, fontFeatureSettings = "tnum"), maxLines = 1)
                        Text(stringResource(R.string.myplay_speed_unit_kmh), style = TextStyle(fontSize = 18.sp, color = g.secondary), maxLines = 1)
                    }
                }
                reading?.limit?.let { SpeedLimit(it, night) }
            }
        }
    }

    /**
     * N20: the limit from the map data, under the speed: a plain ring (not made to look like a road
     * sign: it is advice, and may be wrong) and always its label.
     */
    @Composable
    private fun SpeedLimit(limit: LimitShown, night: Boolean) {
        val g = Orbit.glass(night)
        Column(Modifier.padding(top = 8.dp).widthIn(max = 170.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val guessed = (limit as? LimitShown.Known)?.defaulted == true
            OrbitDisc(60.dp, night, Modifier.limitPulse((limit as? LimitShown.Known)?.kmh)
                .then(if (guessed) Modifier.dashedRing(3.dp, g.secondary) else Modifier.border(3.dp, g.secondary, CircleShape))) {
                Text(
                    (limit as? LimitShown.Known)?.kmh?.toString() ?: "–",
                    style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = g.text), maxLines = 1,
                )
            }
            Text(
                stringResource(when {
                    guessed -> R.string.myplay_speed_limit_guessed
                    limit is LimitShown.Known -> R.string.myplay_speed_limit_map_data
                    else -> R.string.myplay_speed_limit_unknown
                }),
                // SL-10: as large as the speed's unit, the smallest text ORBIT draws there.
                style = TextStyle(fontSize = 18.sp, lineHeight = 20.sp, textAlign = TextAlign.Center).floating(g.secondary),
                maxLines = 2, modifier = Modifier.padding(top = 4.dp),
            )
        }
    }

    @Composable
    override fun Chooser(selected: World, modifier: Modifier) = Box(modifier.orbitRipple(0f, 1f)) {
        Scaled {
            val g = glass()
            val night = LocalSkinTokens.current.night
            val worlds = dial ?: World.entries
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(DIAL), contentAlignment = Alignment.Center) {
                    worlds.forEachIndexed { i, w ->
                        OrbitSector(i, worlds.size, w == selected, Orbit.accent(w, night), night, DIAL, 132.dp)
                        val a = Math.toRadians(-90.0 + 360.0 / worlds.size * i)
                        val on = w == selected
                        Column(
                            Modifier.offset((205 * cos(a)).dp, (205 * sin(a)).dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(Orbit.icon(w), null, Modifier.size(if (on) 52.dp else 44.dp), tint = if (on) Orbit.accent(w, night) else g.secondary)
                            Text(worldName(w), style = Orbit.Type.dial.copy(color = if (on) g.text else g.secondary), maxLines = 1)
                        }
                    }
                    WorldDisc(selected, night)
                }
                // O7: no line under the dial any more (the owner): each world is a pad direction.
            }
        }
    }

    private val DIAL = 552.dp

    @Composable
    override fun MusicWorld(page: MusicPage, ringed: String, stop: (String) -> Modifier, modifier: Modifier) {
        val night = LocalSkinTokens.current.night
        val g = Orbit.glass(night)
        val accent = Orbit.accent(World.MUSIC, night)
        Box(modifier.background(g.halo.copy(alpha = 0.62f)).orbitRipple(0.5f, 0.5f)) {
            Scaled {
                // The sheet's places at 1280x720, scaled with the screen.
                Box(Modifier.fillMaxSize()) {
                    val song = page.song
                    if (song == null) {
                        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            OrbitDisc(200.dp, night, fill = g.glass) { Icon(Icons.Rounded.MusicNote, null, Modifier.size(96.dp), tint = accent) }
                            Text(stringResource(R.string.myplay_music_nothing), style = Orbit.Type.name.floating(g.text), modifier = Modifier.padding(top = 16.dp))
                            Spacer(Modifier.size(24.dp))
                            MusicButton(MUSIC_LIBRARY, Icons.Rounded.LibraryMusic, 76.dp, false, ringed, accent, night, stop)
                        }
                        return@Box
                    }
                    ArtBox(song, accent, ringed == MUSIC_ART, night, stop(MUSIC_ART).offset(140.dp, 110.dp))
                    // The buttons in an arc under the artwork.
                    MusicButton(MUSIC_PREV, Icons.Rounded.SkipPrevious, 76.dp, false, ringed, accent, night, stop, Modifier.offset(165.dp - 38.dp, 548.dp - 38.dp))
                    MusicButton(MUSIC_PLAY, if (song.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, 100.dp, true, ringed, accent, night, stop, Modifier.offset(262.dp - 50.dp, 586.dp - 50.dp))
                    MusicButton(MUSIC_NEXT, Icons.Rounded.SkipNext, 76.dp, false, ringed, accent, night, stop, Modifier.offset(376.dp - 38.dp, 586.dp - 38.dp))
                    MusicButton(MUSIC_LIBRARY, Icons.Rounded.LibraryMusic, 76.dp, false, ringed, accent, night, stop, Modifier.offset(472.dp - 38.dp, 548.dp - 38.dp))
                    if (page.transport.upNext.isNotEmpty()) {
                        Column(Modifier.offset(640.dp, 110.dp).width(480.dp)) {
                            Text(stringResource(R.string.myplay_music_up_next).uppercase(), style = TextStyle(fontSize = 22.sp, letterSpacing = 2.sp).floating(g.secondary),
                                modifier = Modifier.padding(bottom = 14.dp))
                            page.transport.upNext.forEachIndexed { i, u ->
                                val id = upNextStop(u.queueId)
                                Row(
                                    stop(id).padding(bottom = 22.dp).orbitFocusRounded(ringed == id, accent, g.halo, 36.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp),
                                ) {
                                    Box(Modifier.size(72.dp).clip(CircleShape).background(UP_NEXT[i % UP_NEXT.size]))
                                    Column {
                                        Text(u.title, style = TextStyle(fontSize = 34.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold).floating(g.text), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (u.subtitle.isNotBlank()) Text(u.subtitle, style = TextStyle(fontSize = 25.sp).floating(g.secondary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * O1: the music panel above the song disc: a glass card with the music app, previous,
     * play/pause (the big one, in the Music colour) and next, and the progress bar under them,
     * which seeks with left and right while it holds the ring.
     */
    @Composable
    override fun MusicPanel(page: MusicPage, ringed: String, stop: (String) -> Modifier, modifier: Modifier) = Box(modifier) {
        Scaled {
            val night = LocalSkinTokens.current.night
            val g = Orbit.glass(night)
            val accent = Orbit.accent(World.MUSIC, night)
            val song = page.song
            val card = androidx.compose.foundation.shape.RoundedCornerShape(56.dp)
            Column(
                Modifier.padding(bottom = 18.dp).clip(card).background(g.glass).border(2.dp, g.edge, card)
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MusicButton(PANEL_APP, Icons.Rounded.LibraryMusic, 64.dp, false, ringed, accent, night, stop)
                    MusicButton(PANEL_PREV, Icons.Rounded.SkipPrevious, 76.dp, false, ringed, accent, night, stop)
                    MusicButton(PANEL_PLAY, if (song?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, 96.dp, true, ringed, accent, night, stop)
                    MusicButton(PANEL_NEXT, Icons.Rounded.SkipNext, 76.dp, false, ringed, accent, night, stop)
                }
                if (song != null && page.transport.canSeek) PanelProgress(song, ringed == PANEL_SEEK, accent, night, stop(PANEL_SEEK))
            }
        }
    }

    /** O1: the panel's progress bar; ringed, it is outlined and seeks with left and right. */
    @Composable
    private fun PanelProgress(song: SongSlot, on: Boolean, accent: Color, night: Boolean, modifier: Modifier) {
        val g = Orbit.glass(night)
        var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
        LaunchedEffect(song.playing, song.updatedAtMs) {
            now = SystemClock.elapsedRealtime()
            while (song.playing) {
                delay(500)
                now = SystemClock.elapsedRealtime()
            }
        }
        val progress = if (song.durationMs > 0) mediaProgress(song.positionMs, song.durationMs, song.playing, song.updatedAtMs, now) else 0f
        Box(modifier.width(372.dp).height(36.dp).orbitFocusRounded(on, accent, g.halo, 18.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(if (on) 10.dp else 6.dp).clip(CircleShape).background(g.track)
                .testTag(PANEL_PROGRESS_TAG).progressSemantics(progress)) {
                Box(Modifier.fillMaxWidth(progress).fillMaxSize().clip(CircleShape).background(accent))
            }
        }
    }

    const val PANEL_PROGRESS_TAG = "orbit_panel_progress"

    /** The sheet's round colour for each next song (the queue sends no artwork). */
    private val UP_NEXT = listOf(
        Brush.linearGradient(listOf(Color(0xFFFFC24A), Color(0xFFFF5A8A))),
        Brush.linearGradient(listOf(Color(0xFF4CC9F0), Color(0xFF5FD39A))),
        Brush.linearGradient(listOf(Color(0xFF8F5BFF), Color(0xFF4CC9F0))),
    )

    /** The artwork box ("the one square"): the art, the title, artist and the progress bar over its bottom. */
    @Composable
    private fun ArtBox(song: SongSlot, accent: Color, on: Boolean, night: Boolean, modifier: Modifier) {
        val g = Orbit.glass(night)
        val shape = androidx.compose.foundation.shape.RoundedCornerShape(78.dp)
        Box(modifier.requiredSize(320.dp).orbitFocusRounded(on, accent, g.halo, 78.dp).clip(shape)) {
            val art = song.art
            if (art != null) {
                val image = remember(art) { art.asImageBitmap() }
                Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().background(ART), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.MusicNote, null, Modifier.size(128.dp), tint = Color.White)
                }
            }
            Column(
                Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE006080D))))
                    .padding(start = 28.dp, end = 28.dp, top = 50.dp, bottom = 22.dp),
            ) {
                Text(song.title, style = TextStyle(fontSize = 38.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = Color.White), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.subtitle, style = TextStyle(fontSize = 26.sp, color = Color(0xFFC4CBDC)), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (song.durationMs > 0) ArtProgress(song)
            }
        }
    }

    /**
     * The artwork's progress bar. SM-3: its twice-a-second clock lives here, so each tick composes
     * only the bar again, not the artwork, the title and the artist.
     */
    @Composable
    private fun ArtProgress(song: SongSlot) {
        var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
        LaunchedEffect(song.playing, song.updatedAtMs) {
            now = SystemClock.elapsedRealtime()
            while (song.playing) {
                delay(500)
                now = SystemClock.elapsedRealtime()
            }
        }
        val progress = mediaProgress(song.positionMs, song.durationMs, song.playing, song.updatedAtMs, now)
        Box(Modifier.padding(top = 12.dp).fillMaxWidth().height(6.dp).clip(CircleShape).background(Color(0x40FFFFFF))
            .testTag(MUSIC_PROGRESS_TAG).progressSemantics(progress)) {
            Box(Modifier.fillMaxWidth(progress).fillMaxSize().clip(CircleShape).background(Color.White))
        }
    }

    /** One of the music buttons: a glass disc, or the big one filled in the world's colour. */
    @Composable
    private fun MusicButton(
        id: String, icon: ImageVector, size: androidx.compose.ui.unit.Dp, big: Boolean, ringed: String, accent: Color, night: Boolean,
        stop: (String) -> Modifier, modifier: Modifier = Modifier,
    ) {
        val g = Orbit.glass(night)
        OrbitDisc(
            size, night, modifier.then(stop(id)).orbitFocus(ringed == id, accent, g.halo),
            fill = if (big) accent else g.glass, edge = if (big) accent else g.edge,
        ) { Icon(icon, null, Modifier.size(if (big) 46.dp else 36.dp), tint = if (big) Orbit.onAccent(night) else g.text) }
    }

    const val MUSIC_PROGRESS_TAG = "orbit_music_progress"

    /**
     * Polish #1: the short line at the top (a gesture's, the ice's, the phone's heat) in ORBIT's own
     * glass: the notice card's glass, edge and title type, as a pill.
     */
    override val gestureLine: (@Composable (String, Modifier) -> Unit) = { text, modifier ->
        val g = Orbit.glass(LocalSkinTokens.current.night)
        Box(modifier) {
            Scaled {
                Text(
                    text, style = TextStyle(fontSize = 26.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = g.text),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 12.dp).widthIn(max = NOTICE_WIDTH).clip(CircleShape).background(g.glass)
                        .border(2.dp, g.edge, CircleShape).padding(horizontal = 24.dp, vertical = 12.dp),
                )
            }
        }
    }

    @Composable
    override fun Notice(notice: CarNotice, hideText: Boolean, ringed: String?, stop: (String) -> Modifier, modifier: Modifier) {
        val night = LocalSkinTokens.current.night
        val g = Orbit.glass(night)
        val accent = Orbit.accent(World.NAVIGATE, night)
        // Look C (designs 27, 28): the alert's glass and buttons, NOTICE_WIDTH wide at the top of the free
        // area, between the turn and the clock. A message's disc is Calls' green; others Navigate's.
        val disc = Orbit.accent(if (notice.message) World.CALLS else World.NAVIGATE, night)
        // Shifted outside the slot's own modifier, so the map's free area and the knob see where it is drawn.
        val c = LocalConfiguration.current
        Box(Modifier.offset(x = NOTICE_SHIFT * scale(c.screenWidthDp, c.screenHeightDp)).then(modifier)) {
            Scaled {
                val card = androidx.compose.foundation.shape.RoundedCornerShape(36.dp)
                Column(
                    // The design's place: right of the middle (x 470-970 on the sheet), clear of the turn's distance.
                    Modifier.width(NOTICE_WIDTH).clip(card).background(g.glass).border(2.dp, g.edge, card)
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.size(64.dp).clip(CircleShape).background(disc), contentAlignment = Alignment.Center) {
                            val icon = notice.icon
                            if (icon != null) {
                                val image = remember(icon) { icon.asImageBitmap() }
                                Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Icon(if (notice.message) Icons.Rounded.ChatBubbleOutline else Icons.Rounded.Notifications, null,
                                    Modifier.size(32.dp), tint = Orbit.onAccent(night))
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(notice.title, style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = g.text), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            // While driving a message shows who wrote, not what (as everywhere in the car).
                            Text(if (hideText) stringResource(R.string.myplay_notice_hidden) else notice.text,
                                style = TextStyle(fontSize = 22.sp, color = g.secondary), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    // The buttons: the first action filled in the accent, the others and Dismiss on the glass's track.
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val labels = notice.actions.take(2).mapIndexed { i, (label, _) -> noticeActionStop(i) to label } +
                            (NOTICE_DISMISS to stringResource(R.string.common_dismiss))
                        labels.forEachIndexed { i, (id, label) ->
                            val filled = i == 0 && id != NOTICE_DISMISS
                            val shape = androidx.compose.foundation.shape.RoundedCornerShape(30.dp)
                            Box(
                                stop(id).weight(1f, fill = false).height(60.dp).orbitFocusRounded(ringed == id, accent, g.halo, 30.dp).clip(shape)
                                    .background(if (filled) accent else g.track).padding(horizontal = if (labels.size > 2) 16.dp else 24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(label, style = TextStyle(fontSize = if (labels.size > 2) 21.sp else 24.sp, fontWeight = FontWeight.ExtraBold, color = if (filled) Orbit.onAccent(night) else g.text),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }

    /** The notice's widest, at the sheet's scale: the room between the turn and the clock. */
    private val NOTICE_WIDTH = 500.dp
    /** Look C: the notice sits this far right of the middle (the design's x 470-970 on 1280). */
    private val NOTICE_SHIFT = 80.dp

    /** A call's time: m:ss, and h:mm:ss from an hour on ("1:05:03", not "65:03"). */
    fun callTime(seconds: Long): String {
        val s = seconds.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) String.format(java.util.Locale.ROOT, "%d:%02d:%02d", h, m, sec) else String.format(java.util.Locale.ROOT, "%d:%02d", m, sec)
    }

    /** Up to two letters for a caller's disc; null for a number (then a phone icon). */
    fun initials(name: String): String? {
        val words = name.trim().split(Regex("\\s+")).filter { w -> w.firstOrNull()?.isLetter() == true }
        if (words.isEmpty()) return null
        return words.take(2).joinToString("") { it.first().uppercase() }
    }

    @Composable
    override fun Ringing(call: CarCall, canEnd: Boolean, ringed: String, stop: (String) -> Modifier, modifier: Modifier) {
        val night = LocalSkinTokens.current.night
        val g = Orbit.glass(night)
        val calls = Orbit.accent(World.CALLS, night)
        // Look C (design 30): centred in the free area over the map, which stays (no dimming): while
        // navigating the turn, lanes and hub stay readable. The caller's initials in Calls' green ring,
        // the name, and two big discs: Answer (it holds the ring) and Decline a turn away.
        Box(modifier, contentAlignment = Alignment.Center) {
            Scaled {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(200.dp).border(6.dp, calls, CircleShape).padding(14.dp).clip(CircleShape).background(calls), contentAlignment = Alignment.Center) {
                        val letters = initials(call.name)
                        if (letters != null) Text(letters, style = TextStyle(fontSize = 64.sp, fontWeight = FontWeight.ExtraBold, color = Orbit.onAccent(night)))
                        else Icon(Icons.Rounded.Phone, null, Modifier.size(80.dp), tint = Orbit.onAccent(night))
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(call.name, style = TextStyle(fontSize = 46.sp, fontWeight = FontWeight.ExtraBold).floating(g.text),
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 680.dp))
                    Text(stringResource(R.string.phone_incoming_call), style = TextStyle(fontSize = 26.sp).floating(g.secondary))
                    Spacer(Modifier.height(28.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(80.dp)) {
                        CallButton(CALL_ANSWER, Icons.Rounded.Phone, stringResource(R.string.phone_action_answer), calls, ringed, calls, night, stop)
                        if (canEnd) CallButton(CALL_DECLINE, Icons.Rounded.CallEnd, stringResource(R.string.phone_action_decline), Orbit.warning(night), ringed, calls, night, stop)
                    }
                }
            }
        }
    }

    /** A 112 dp call button filled in its colour, its word under it. */
    @Composable
    private fun CallButton(id: String, icon: ImageVector, label: String, fill: Color, ringed: String, ring: Color, night: Boolean, stop: (String) -> Modifier) {
        val g = Orbit.glass(night)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            OrbitDisc(112.dp, night, stop(id).orbitFocus(ringed == id, ring, g.halo), fill = fill, edge = fill) {
                Icon(icon, label, Modifier.size(52.dp), tint = if (fill == ring) Orbit.onAccent(night) else Color.White)
            }
            Text(label, style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold).floating(g.text), modifier = Modifier.padding(top = 8.dp))
        }
    }

    @Composable
    override fun InCall(call: CarCall, canEnd: Boolean, ringed: String?, stop: (String) -> Modifier, modifier: Modifier) {
        val night = LocalSkinTokens.current.night
        val g = Orbit.glass(night)
        val calls = Orbit.accent(World.CALLS, night)
        val elapsed by androidx.compose.runtime.produceState(0L, call.sinceMs) {
            while (true) { value = ((System.currentTimeMillis() - call.sinceMs) / 1000).coerceAtLeast(0); delay(1_000) }
        }
        Box(modifier) {
            Scaled {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val pill = androidx.compose.foundation.shape.RoundedCornerShape(38.dp)
                    Row(
                        stop(STOP_CALL).orbitFocusRounded(ringed == STOP_CALL, calls, g.halo, 38.dp).height(76.dp).clip(pill).background(g.glass)
                            .border(2.dp, calls, pill).padding(start = 10.dp, end = 24.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(Modifier.size(56.dp).clip(CircleShape).background(calls), contentAlignment = Alignment.Center) {
                            val letters = initials(call.name)
                            if (letters != null) Text(letters, style = TextStyle(fontSize = 24.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = Orbit.onAccent(night)))
                            else Icon(Icons.Rounded.Phone, null, tint = Orbit.onAccent(night))
                        }
                        Text(stringResource(R.string.phone_in_call_elapsed, call.name, callTime(elapsed.toLong())),
                            style = TextStyle(fontSize = 28.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = g.text, fontFeatureSettings = "tnum"),
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 460.dp))
                    }
                    if (canEnd) {
                        val red = Orbit.warning(night)
                        OrbitDisc(76.dp, night, stop(STOP_HANG_UP).orbitFocus(ringed == STOP_HANG_UP, red, g.halo), fill = red, edge = red) {
                            Icon(Icons.Rounded.CallEnd, stringResource(R.string.phone_action_hang_up), Modifier.size(36.dp), tint = Color.White)
                        }
                    }
                }
            }
        }
    }

    @Composable
    override fun Library(page: LibraryPage, ringed: Int, art: suspend (LibraryRow, Int) -> android.graphics.Bitmap?, row: (Int) -> Modifier, modifier: Modifier) {
        val night = LocalSkinTokens.current.night
        val g = Orbit.glass(night)
        val accent = Orbit.accent(World.MUSIC, night)
        val list = androidx.compose.foundation.lazy.rememberLazyListState()
        // The ringed row stays in view, one row of context above it.
        LaunchedEffect(ringed, page.rows.size) { if (page.rows.isNotEmpty()) list.animateScrollToItem((ringed - 1).coerceAtLeast(0)) }
        Box(modifier.background(g.halo.copy(alpha = 0.7f))) {
            Scaled {
                Column(Modifier.fillMaxSize().padding(start = 60.dp, top = 40.dp)) {
                    Text(page.title, style = TextStyle(fontSize = 34.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = g.text), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(R.string.myplay_library_hint), style = TextStyle(fontSize = 24.sp, color = g.secondary))
                    if (page.rows.isEmpty()) {
                        Text(
                            if (page.loading) stringResource(R.string.myplay_library_loading) else page.error ?: stringResource(R.string.myplay_library_empty),
                            style = Orbit.Type.secondary.floating(g.secondary), modifier = Modifier.padding(start = 80.dp, top = 60.dp),
                        )
                        return@Column
                    }
                    androidx.compose.foundation.lazy.LazyColumn(
                        state = list, modifier = Modifier.padding(start = 80.dp, top = 24.dp).width(640.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 20.dp),
                    ) {
                        // L-1: apps repeat ids (a song twice in a playlist, overlapping pages): the key
                        // must stay unique, or the list throws. As the app's own screen keys them.
                        items(page.rows.size, key = { "${it}_${page.rows[it].id}" }) { i ->
                            LibraryPill(page.rows[i], i == ringed, accent, night, art, row(i))
                        }
                    }
                }
            }
        }
    }

    /** One row of the library: a pill with the row's artwork, a folder or a note. */
    @Composable
    private fun LibraryPill(r: LibraryRow, on: Boolean, accent: Color, night: Boolean, art: suspend (LibraryRow, Int) -> android.graphics.Bitmap?, modifier: Modifier) {
        val px = with(LocalDensity.current) { 76.dp.roundToPx() }
        val bitmap by androidx.compose.runtime.produceState<android.graphics.Bitmap?>(null, r.id) { value = runCatching { art(r, px) }.getOrNull() }
        Pill(r.title, r.subtitle, if (r.browsable) Icons.Rounded.Folder else Icons.Rounded.MusicNote, null, bitmap, r.browsable, on, accent, night, modifier)
    }

    /**
     * ORBIT's list row (the sheet's "Where to?" rows): a glass pill with a round disc (a picture, or
     * [icon] in [iconTint]), the title and subtitle, a chevron when it leads on; the ringed one in [accent].
     */
    @Composable
    private fun Pill(
        title: String, subtitle: String, icon: ImageVector, iconTint: Color?, bitmap: android.graphics.Bitmap?, leadsOn: Boolean,
        on: Boolean, accent: Color, night: Boolean, modifier: Modifier, trailing: String? = null,
    ) {
        val g = Orbit.glass(night)
        val shape = androidx.compose.foundation.shape.RoundedCornerShape(52.dp)
        Row(
            modifier.fillMaxWidth().height(104.dp).orbitFocusRounded(on, accent, g.halo, 52.dp).clip(shape).background(g.glass)
                .border(3.dp, if (on) accent else g.edge, shape).padding(start = 14.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Box(Modifier.requiredSize(76.dp).clip(CircleShape).background(if (on) accent else g.track), contentAlignment = Alignment.Center) {
                if (bitmap != null) {
                    val image = remember(bitmap) { bitmap.asImageBitmap() }
                    Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(icon, null, Modifier.size(40.dp), tint = if (on) Orbit.onAccent(night) else iconTint ?: g.text)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = TextStyle(fontSize = 34.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = g.text), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotBlank()) Text(subtitle, style = TextStyle(fontSize = 24.sp, color = g.secondary), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            trailing?.let { Text(it, style = TextStyle(fontSize = 24.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = accent), maxLines = 1) }
            if (leadsOn) Icon(Icons.Rounded.ChevronRight, null, Modifier.size(36.dp), tint = g.secondary)
        }
    }

    @Composable
    override fun CarWorld(lines: List<Pair<String, String>>, ringed: Int, row: (Int) -> Modifier, modifier: Modifier) {
        val night = LocalSkinTokens.current.night
        val g = Orbit.glass(night)
        val accent = Orbit.accent(World.CAR, night)
        Box(modifier.background(g.halo.copy(alpha = 0.7f)).orbitRipple(0.5f, 0.5f)) {
            Scaled {
                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(start = 60.dp, top = 40.dp)) {
                        Text(worldName(World.CAR), style = TextStyle(fontSize = 34.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = g.text))
                        if (lines.isEmpty()) {
                            Text(stringResource(R.string.car_data_not_connected), style = Orbit.Type.secondary.floating(g.secondary), modifier = Modifier.padding(start = 80.dp, top = 40.dp))
                            return@Column
                        }
                        Text(stringResource(R.string.myplay_car_hint), style = TextStyle(fontSize = 24.sp, color = g.secondary))
                    }
                    // Look C, step 3: the orbit wheel.
                    OrbitWheel(lines.size, ringed, accent, night) { i, on, place ->
                        val (label, value) = lines[i]
                        Pill(label, "", Icons.Rounded.DirectionsCar, null, null, false, on, accent, night, place.then(row(i)), trailing = value)
                    }
                }
            }
        }
    }

    @Composable
    override fun SettingsWorld(page: SettingsPage, ringed: Int, row: (Int) -> Modifier, modifier: Modifier) =
        ListWorld(World.SETTINGS, stringResource(R.string.myplay_settings_hint), page, ringed, row, modifier) { _, r ->
            if (r.switch) (if (r.on) Icons.Rounded.ToggleOn else Icons.Rounded.ToggleOff) else Icons.Rounded.Tune
        }

    /** #5: the Navigate world's list: the maps app first, then Home and the saved places. */
    @Composable
    override fun PlacesWorld(page: SettingsPage, ringed: Int, row: (Int) -> Modifier, modifier: Modifier) =
        ListWorld(World.NAVIGATE, stringResource(R.string.myplay_places_hint), page, ringed, row, modifier) { i, _ ->
            if (i == 0) Icons.Rounded.Navigation else Icons.Rounded.Place
        }

    /** A world's list of pills (Settings, Navigate's places): its name, a hint, one pill per row. */
    @Composable
    private fun ListWorld(
        world: World, hint: String, page: SettingsPage, ringed: Int, row: (Int) -> Modifier, modifier: Modifier,
        icon: (Int, net.prototux.aare.ui.car.myplay.SettingRow) -> ImageVector,
    ) {
        val night = LocalSkinTokens.current.night
        val g = Orbit.glass(night)
        val accent = Orbit.accent(world, night)
        Box(modifier.background(g.halo.copy(alpha = 0.7f)).orbitRipple(0.5f, 0.5f)) {
            Scaled {
                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(start = 60.dp, top = 40.dp)) {
                        Text(worldName(world), style = TextStyle(fontSize = 34.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = g.text))
                        Text(hint, style = TextStyle(fontSize = 24.sp, color = g.secondary))
                    }
                    // Look C, step 3: the orbit wheel (the ringed row in the middle, one either side).
                    OrbitWheel(page.rows.size, ringed, accent, night) { i, on, place ->
                        val r = page.rows[i]
                        Pill(r.title, r.note.orEmpty(), icon(i, r),
                            if (r.switch && r.on) accent else null, null, false, on, accent, night, place.then(row(i)), trailing = r.value)
                    }
                }
            }
        }
    }

    @Composable
    override fun Bell(count: Int, ringed: Boolean, modifier: Modifier) {
        val night = LocalSkinTokens.current.night
        val g = Orbit.glass(night)
        Box(modifier) {
            Scaled {
                Box(Modifier.padding(top = 12.dp)) {
                    OrbitDisc(72.dp, night, Modifier.orbitFocus(ringed, Orbit.accent(World.NAVIGATE, night), g.halo)) {
                        Icon(Icons.Rounded.Notifications, stringResource(R.string.notif_title), Modifier.size(38.dp), tint = g.text)
                    }
                    Box(
                        Modifier.align(Alignment.TopEnd).size(30.dp).clip(CircleShape).background(Orbit.warning(night)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(if (count > 9) "9+" else "$count", style = TextStyle(fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = Color.White))
                    }
                }
            }
        }
    }

    @Composable
    override fun NoticeList(page: NoticeListPage, ringed: Int, row: (Int) -> Modifier, modifier: Modifier) {
        val night = LocalSkinTokens.current.night
        val g = Orbit.glass(night)
        val accent = Orbit.accent(World.NAVIGATE, night)
        Box(modifier.background(g.halo.copy(alpha = 0.7f)).orbitRipple(1f, 0f)) {
            Scaled {
                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(start = 60.dp, top = 40.dp)) {
                        Text(stringResource(R.string.notif_title), style = TextStyle(fontSize = 34.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = g.text))
                        Text(stringResource(R.string.myplay_notices_hint), style = TextStyle(fontSize = 24.sp, color = g.secondary))
                    }
                    // Look C, step 3: the orbit wheel; Clear all is its last row.
                    OrbitWheel(page.rows.size, ringed, accent, night) { i, on, place ->
                        val r = page.rows[i]
                        val n = r.notice
                        if (n == null) {
                            Pill(stringResource(R.string.notif_clear_all), "", Icons.Rounded.ClearAll, null, null, false, on, accent, night, place.then(row(i)))
                        } else {
                            Pill("${n.title} · ${r.time}", r.text, Icons.Rounded.Notifications, null, n.icon, false, on, accent, night, place.then(row(i)), trailing = r.action)
                        }
                    }
                }
            }
        }
    }

    @Composable
    override fun PhoneWorld(page: PhonePage, ringed: Int, row: (Int) -> Modifier, modifier: Modifier) {
        val night = LocalSkinTokens.current.night
        val g = Orbit.glass(night)
        val calls = Orbit.accent(World.CALLS, night)
        Box(modifier.background(g.halo.copy(alpha = 0.7f)).orbitRipple(0.5f, 0.5f)) {
            Scaled {
                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(start = 60.dp, top = 40.dp)) {
                        Text(worldName(World.CALLS), style = TextStyle(fontSize = 34.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = g.text))
                        Text(stringResource(R.string.myplay_phone_hint), style = TextStyle(fontSize = 24.sp, color = g.secondary))
                    }
                    // Look C, step 3: the orbit wheel; a note (no permission, loading, no calls) under it.
                    OrbitWheel(page.rows.size, ringed, calls, night) { i, on, place ->
                        val r = page.rows[i]
                        val (icon, tint) = when (r.kind) {
                            PhoneRow.Kind.RECENT -> Icons.Rounded.Phone to null
                            PhoneRow.Kind.MISSED -> Icons.Rounded.PhoneMissed to Orbit.warning(night)
                            PhoneRow.Kind.FAVOURITE -> Icons.Rounded.Star to null
                            PhoneRow.Kind.PHONE_APP -> Icons.Rounded.Dialpad to null
                        }
                        Pill(r.title, r.subtitle, icon, tint, null, r.kind == PhoneRow.Kind.PHONE_APP, on, calls, night, place.then(row(i)))
                    }
                    val note = when {
                        !page.permitted -> stringResource(R.string.myplay_phone_permissions)
                        page.loading -> stringResource(R.string.myplay_library_loading)
                        page.rows.size == 1 -> stringResource(R.string.myplay_phone_none)
                        else -> null
                    }
                    note?.let { Text(it, style = Orbit.Type.secondary.floating(g.secondary), modifier = Modifier.align(Alignment.BottomStart).padding(start = 340.dp, bottom = 30.dp).widthIn(max = 640.dp)) }
                }
            }
        }
    }

    /** Look C (33): eight apps round the ring, a page at a time. */
    const val APPS_ON_RING = 8

    override val appsPerPage get() = APPS_ON_RING

    /** Look C (33): the ring's disc, and the circle the apps sit on (the sheet's 1280x720). */
    private val APPS_DISC = 600.dp
    private val APPS_RADIUS = 220.dp
    private val APP_SIZE = 96.dp
    private val APP_RINGED = 120.dp

    /** Where app [index] of a page sits on the ring, in degrees (0 = right, clockwise; the first at the top). */
    fun appAngleDeg(index: Int): Float = -90f + 360f / APPS_ON_RING * index

    /**
     * Look C (33-apps-ring): the page's apps round a glass disc in the middle of the map, clockwise
     * from the top, as the knob goes; the ringed one larger in the Apps colour's ring, its name in
     * the middle with "Push to open", and the pages as dots under it. The map stays clear round it.
     */
    @Composable
    override fun AppsWorld(apps: List<CarAppEntry>, ringed: Int, page: Int, pages: Int, tile: (Int) -> Modifier, modifier: Modifier) {
        val night = LocalSkinTokens.current.night
        val g = Orbit.glass(night)
        val accent = Orbit.accent(World.APPS, night)
        Box(modifier.orbitRipple(0.5f, 0.5f), contentAlignment = Alignment.Center) {
            Scaled {
                Box(Modifier.requiredSize(APPS_DISC).testTag(APPS_RING_TAG), contentAlignment = Alignment.Center) {
                    OrbitDisc(APPS_DISC, night)
                    if (apps.isEmpty()) {
                        Text(stringResource(R.string.myplay_apps_none), style = Orbit.Type.secondary.copy(color = g.secondary))
                        return@Box
                    }
                    apps.take(APPS_ON_RING).forEachIndexed { i, app ->
                        val a = Math.toRadians(appAngleDeg(i).toDouble())
                        val on = i == ringed
                        Box(
                            Modifier.offset(x = APPS_RADIUS * cos(a).toFloat(), y = APPS_RADIUS * sin(a).toFloat()),
                            contentAlignment = Alignment.Center,
                        ) {
                            OrbitDisc(
                                if (on) APP_RINGED else APP_SIZE, night,
                                tile(i).orbitFocus(on, accent, g.halo).graphicsLayer { alpha = if (on) 1f else 0.85f }.semantics { contentDescription = app.label },
                                edge = if (on) accent else null, edgeWidth = 3.dp,
                            ) { AppIcon(app, if (on) 72 else 58) }
                        }
                    }
                    Column(Modifier.width(280.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        apps.getOrNull(ringed)?.let { app ->
                            Text(app.label, style = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = g.text),
                                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.testTag(APPS_RING_NAME_TAG))
                        }
                        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(22.dp).border(2.5.dp, g.secondary, CircleShape), contentAlignment = Alignment.Center) {
                                Box(Modifier.size(7.dp).clip(CircleShape).background(g.secondary))
                            }
                            Text(stringResource(R.string.myplay_apps_ring_push), style = TextStyle(fontSize = 20.sp, color = g.secondary), maxLines = 1)
                        }
                        if (pages > 1) {
                            Row(Modifier.padding(top = 18.dp).testTag(APPS_PAGES_TAG), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                repeat(pages) { p ->
                                    Box(Modifier.height(12.dp).width(if (p == page) 30.dp else 12.dp).clip(CircleShape).background(if (p == page) accent else g.track))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** Test tags. */
    const val BATTERY_TAG = "orbit_battery"
    /** The widest a map alert's button gets (its label is cut with "…" past it). */
    val ALERT_BUTTON_MAX = 200.dp
    /** The clock's time, in its fixed-width box. */
    const val CLOCK_TAG = "orbit_clock"
    /** Polish #3: the battery's number, in its fixed-width box. */
    const val BATTERY_NUMBER_TAG = "orbit_battery_number"
    /** Drive warnings: the ice sign beside the battery. */
    const val ICE_TAG = "orbit_ice"
    /** Polish #5: the outside temperature's place under the clock. */
    const val OUTSIDE_TEMP_TAG = "orbit_outside_temp"
    /** DW-1: the ice sign's place, kept whether it shows or not. */
    const val ICE_SLOT_TAG = "orbit_ice_slot"
    const val SONG_ART_TAG = "orbit_song_art"
    const val SONG_ART_FALLBACK_TAG = "orbit_song_art_fallback"
    const val TRIP_RING_TAG = "orbit_trip_ring"
    const val SONG_RING_TAG = "orbit_song_ring"
    /** Look C (33): the Apps world's ring, the ringed app's name in its middle, and its page dots. */
    const val APPS_RING_TAG = "orbit_apps_ring"
    const val APPS_RING_NAME_TAG = "orbit_apps_ring_name"
    const val APPS_PAGES_TAG = "orbit_apps_pages"
    /** Look C (35): the song's corner with the status (fixed size; what covers the map). */
    const val STATUS_CORNER_TAG = "orbit_status_corner"
    /** Look C (35): the time's badge on the song's ring. */
    const val CLOCK_BADGE_TAG = "orbit_clock_badge"
    /** SC-3: the temperature's pill. */
    const val TEMP_PILL_TAG = "orbit_temp_pill"
    /** Look C (35): the empty ring with nothing playing. */
    const val NO_SONG_TAG = "orbit_no_song"

    /** A ring's progress for accessibility (and the tests); nothing when it is not known. */
    private fun Modifier.progressSemantics(p: Float?): Modifier =
        if (p == null) this else semantics { progressBarRangeInfo = ProgressBarRangeInfo(p, 0f..1f) }

    @Composable
    private fun WorldDisc(world: World, night: Boolean, line: String? = null) {
        val g = Orbit.glass(night)
        val accent = Orbit.accent(world, night)
        OrbitDisc(240.dp, night, fill = g.glass.copy(alpha = 0.95f), edge = accent, edgeWidth = 3.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Orbit.icon(world), null, Modifier.size(76.dp), tint = accent)
                Text(worldName(world), style = TextStyle(fontSize = 34.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = g.text))
                line?.let { Text(it, style = TextStyle(fontSize = 20.sp, color = g.secondary), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp)) }
            }
        }
    }

    @Composable
    override fun WorldPlaceholder(world: World, modifier: Modifier) = Box(modifier.orbitRipple(0.5f, 0.5f)) {
        Scaled { WorldDisc(world, LocalSkinTokens.current.night, stringResource(R.string.myplay_world_not_yet)) }
    }

    @Composable
    fun worldName(w: World): String = stringResource(
        when (w) {
            World.NAVIGATE -> R.string.myplay_world_navigate
            World.MUSIC -> R.string.myplay_world_music
            World.CALLS -> R.string.myplay_world_calls
            World.APPS -> R.string.myplay_world_apps
            World.CAR -> R.string.myplay_world_car
            World.SETTINGS -> R.string.myplay_world_settings
        },
    )
}
