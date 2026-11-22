package net.prototux.aare.ui.car.myplay.orbit

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.asImageBitmap
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

    override fun tokens(night: Boolean): SkinTokens = Orbit.tokens(night)

    override val drawsFocus get() = true
    override val showsSpeed get() = true

    /** O3: an app takes the whole screen, grown out of its stop; no rail. */
    override val appRail get() = false

    /**
     * O7 (the owner): the dial holds four worlds, one per pad direction: up Calls, right Apps,
     * down Settings, left Car. Navigate and Music are on the home pad (O6).
     */
    override val dial get() = listOf(World.CALLS, World.APPS, World.SETTINGS, World.CAR)

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
        CompositionLocalProvider(LocalDensity provides Density(d.density * s, d.fontScale), content = content)
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
        val turn by animateFloatAsState(if (parts.turn) 1f else 0f, tween(400), label = "turn shade")
        val song by animateFloatAsState(if (parts.song) 1f else 0f, tween(400), label = "song shade")
        val night = LocalSkinTokens.current.night
        val strength = if (night) PLATE_NIGHT else PLATE_DAY
        // Keep the last place while fading out.
        val last = remember { arrayOfNulls<net.prototux.aare.ui.car.myplay.FocusGraph.Box>(2) }
        parts.turnBox?.let { last[0] = it }
        parts.songBox?.let { last[1] = it }
        Box(modifier) {
            if (turn > 0f) last[0]?.let { b -> Canvas(Modifier.fillMaxSize().testTag(SHADE_TURN_TAG)) { plate(halo, turn * strength, b) } }
            if (song > 0f) last[1]?.let { b -> Canvas(Modifier.fillMaxSize().testTag(SHADE_SONG_TAG)) { plate(halo, song * strength, b) } }
            parts.statusBox?.let { b -> Canvas(Modifier.fillMaxSize().testTag(SHADE_STATUS_TAG)) { plate(halo, strength, b) } }
        }
    }

    /** SH-1: how dark (night) or light (day) a plate's middle is, about. */
    private const val PLATE_NIGHT = 0.5f
    private const val PLATE_DAY = 0.38f

    /**
     * SH-1: a soft plate round [b] (screen pixels): a rounded shape a little wider than the part,
     * its edge blurred away over about 26 dp (a real blur, no bands); [alpha] in the middle.
     */
    private fun androidx.compose.ui.graphics.drawscope.DrawScope.plate(halo: Color, alpha: Float, b: net.prototux.aare.ui.car.myplay.FocusGraph.Box) {
        val pad = 6.dp.toPx()
        val blur = 26.dp.toPx()
        val h = (b.bottom - b.top) + 2 * pad
        val r = minOf(h / 2f, 64.dp.toPx())
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = halo.copy(alpha = alpha.coerceIn(0f, 1f)).toArgb()
            maskFilter = android.graphics.BlurMaskFilter(blur, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
        drawIntoCanvas { c -> c.nativeCanvas.drawRoundRect(b.left - pad, b.top - pad, b.right + pad, b.bottom + pad, r, r, paint) }
    }

    const val SHADE_STATUS_TAG = "orbit_shade_status"

    const val SHADE_TURN_TAG = "orbit_shade_turn"
    const val SHADE_SONG_TAG = "orbit_shade_song"

    @Composable
    override fun Turn(turn: TurnSlot, modifier: Modifier) = Box(modifier) {
        Scaled {
            val g = glass()
            val nav = Orbit.accent(World.NAVIGATE, LocalSkinTokens.current.night)
            Row(Modifier.carPart(CarPart.OrbitTurn), verticalAlignment = Alignment.CenterVertically) {
                OrbitDisc(132.dp, LocalSkinTokens.current.night, Modifier.coversMap().orbitFocus(LocalStopRinged.current, nav, g.halo), fill = nav, edge = nav) {
                    val arrow = turn.arrow
                    if (arrow != null) {
                        val image = remember(arrow) { arrow.asImageBitmap() }
                        Image(image, null, Modifier.size(84.dp), contentScale = ContentScale.Fit)
                    } else {
                        Icon(arrowIcon(turn.direction), null, Modifier.size(78.dp), tint = Orbit.onAccent(LocalSkinTokens.current.night))
                    }
                }
                Spacer(Modifier.width(22.dp))
                Column(Modifier.weight(1f, fill = false)) {
                    turn.distance?.let { Text(it, style = Orbit.Type.hero.floating(g.text), maxLines = 1) }
                    turn.road?.let { Text(it, style = Orbit.Type.secondary.floating(g.secondary), maxLines = 2, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
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
                Box(Modifier.size(52.dp).clip(CircleShape).background(caution.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                    val icon = alert.icon
                    if (icon != null) Image(icon, null, Modifier.size(34.dp), contentScale = ContentScale.Fit)
                    else Icon(Icons.Rounded.Warning, null, Modifier.size(32.dp), tint = caution)
                }
                Column(Modifier.weight(1f, fill = false)) {
                    Text(alert.title, style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, color = g.text), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    alert.subtitle?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = TextStyle(fontSize = 20.sp, color = g.secondary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                alert.actions.forEachIndexed { i, (label, _) ->
                    val id = net.prototux.aare.ui.car.myplay.mapAlertStop(i)
                    Text(
                        label, style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = g.text), maxLines = 1,
                        modifier = stop(id).orbitFocusRounded(ringed == id, accent, g.halo, 24.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp)).background(g.track)
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                    )
                }
            }
        }
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
                            val colour = if (speed?.over == true) Orbit.caution(night) else g.text
                            Text(speed?.kmh?.toString() ?: "–", style = TextStyle(fontSize = 64.sp, lineHeight = 64.sp, fontWeight = FontWeight.ExtraBold, color = colour), maxLines = 1)
                            Text(stringResource(R.string.myplay_speed_unit_kmh), style = TextStyle(fontSize = 18.sp, lineHeight = 20.sp, color = g.secondary), maxLines = 1)
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
        Box(
            modifier.size(84.dp).shadow(6.dp, CircleShape).clip(CircleShape).background(Color.White)
                .border(9.dp, ring, CircleShape).testTag(LIMIT_SIGN_TAG),
            contentAlignment = Alignment.Center,
        ) {
            Text(known?.kmh?.toString() ?: "–", style = TextStyle(fontSize = if ((known?.kmh ?: 0) >= 100) 28.sp else 34.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF111111)), maxLines = 1)
        }
    }

    const val HUB_SPEED_TAG = "orbit_hub_speed"
    const val LIMIT_SIGN_TAG = "orbit_limit_sign"

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

    @Composable
    override fun Song(song: SongSlot, modifier: Modifier) = Box(modifier) {
        Scaled {
            val g = glass()
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
            Row(Modifier.carPart(CarPart.OrbitSong), verticalAlignment = Alignment.CenterVertically) {
                // AR-1: the text takes what room is left after the art's circle (a long title used to
                // squeeze the circle into an oval in the 40 % wide corner).
                Column(Modifier.weight(1f, fill = false).widthIn(max = 392.dp), horizontalAlignment = Alignment.End) {
                    Text(song.title, style = Orbit.Type.name.floating(g.text), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
                    Text(song.subtitle, style = Orbit.Type.secondary.floating(g.secondary), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
                }
                Spacer(Modifier.width(10.dp))
                val musicColour = Orbit.accent(World.MUSIC, LocalSkinTokens.current.night)
                OrbitRing(progress, 152.dp, 7.dp, musicColour, g.track, Modifier.coversMap().testTag(SONG_RING_TAG).progressSemantics(progress).orbitFocus(LocalStopRinged.current, musicColour, g.halo)) {
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

    /** One line of [text] in [style], made smaller when it is wider than [width]. */
    @Composable
    private fun FitText(text: String, style: TextStyle, width: androidx.compose.ui.unit.Dp) {
        val measurer = rememberTextMeasurer()
        val room = with(LocalDensity.current) { width.toPx() }
        val needed = remember(text, style) { measurer.measure(text, style, maxLines = 1).size.width }
        val fit = if (needed > room && needed > 0) room / needed else 1f
        Text(text, style = style.copy(fontSize = style.fontSize * fit, lineHeight = style.lineHeight * fit), maxLines = 1)
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
                Text(time, style = Orbit.Type.clock.floating(g.text), maxLines = 1, modifier = Modifier.carPart(CarPart.Clock))
                // N5-3: no level shown when the phone does not tell it, never "0%".
                rememberPhoneStatus()?.takeIf { it.batteryKnown }?.let { s ->
                    val night = LocalSkinTokens.current.night
                    val colour = if (batteryLow(s.battery, s.charging)) Orbit.warning(night) else nav
                    Row(
                        Modifier.padding(top = 8.dp).testTag(BATTERY_TAG).carPart(CarPart.Status),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(batteryIcon(s.battery, s.charging), null, Modifier.size(30.dp), tint = colour)
                        Text("${s.battery}%", style = TextStyle(fontSize = 28.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold).floating(colour))
                    }
                }
            }
        }
    }

    /** A battery this low (and not charging) shows in the warning colour. */
    const val BATTERY_LOW = 20

    fun batteryLow(level: Int, charging: Boolean) = !charging && level <= BATTERY_LOW

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
                        val colour = if (reading?.over == true) Orbit.caution(night) else g.text
                        Text(reading?.kmh?.toString() ?: "–", style = Orbit.Type.name.copy(color = colour, lineHeight = 36.sp), maxLines = 1)
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
            OrbitDisc(60.dp, night, Modifier.border(3.dp, g.secondary, CircleShape)) {
                Text(
                    (limit as? LimitShown.Known)?.kmh?.toString() ?: "–",
                    style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = g.text), maxLines = 1,
                )
            }
            Text(
                stringResource(if (limit is LimitShown.Known) R.string.myplay_speed_limit_map_data else R.string.myplay_speed_limit_unknown),
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
        var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
        LaunchedEffect(song.playing, song.updatedAtMs) {
            now = SystemClock.elapsedRealtime()
            while (song.playing) {
                delay(500)
                now = SystemClock.elapsedRealtime()
            }
        }
        val progress = if (song.durationMs > 0) mediaProgress(song.positionMs, song.durationMs, song.playing, song.updatedAtMs, now) else null
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
                if (progress != null) {
                    Box(Modifier.padding(top = 12.dp).fillMaxWidth().height(6.dp).clip(CircleShape).background(Color(0x40FFFFFF))
                        .testTag(MUSIC_PROGRESS_TAG).progressSemantics(progress)) {
                        Box(Modifier.fillMaxWidth(progress).fillMaxSize().clip(CircleShape).background(Color.White))
                    }
                }
            }
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

    @Composable
    override fun Notice(notice: CarNotice, hideText: Boolean, ringed: String?, stop: (String) -> Modifier, modifier: Modifier) {
        val night = LocalSkinTokens.current.night
        val g = Orbit.glass(night)
        val accent = Orbit.accent(World.NAVIGATE, night)
        Box(modifier) {
            Scaled {
                // Two rows, at most NOTICE_WIDTH wide: it fits between the turn (top left) and the clock
                // (top right), so it never covers the next turn.
                val card = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
                Column(
                    Modifier.widthIn(max = NOTICE_WIDTH).clip(card).background(g.glass).border(2.dp, g.edge, card).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.size(48.dp).clip(CircleShape).background(g.track), contentAlignment = Alignment.Center) {
                            val icon = notice.icon
                            if (icon != null) {
                                val image = remember(icon) { icon.asImageBitmap() }
                                Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Icon(Icons.Rounded.Notifications, null, Modifier.size(28.dp), tint = g.text)
                            }
                        }
                        Column(Modifier.weight(1f, fill = false)) {
                            Text(notice.title, style = TextStyle(fontSize = 26.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = g.text), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            // While driving a message shows who wrote, not what (as everywhere in the car).
                            Text(if (hideText) stringResource(R.string.notif_new_message_placeholder) else notice.text,
                                style = TextStyle(fontSize = 22.sp, color = g.secondary), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        notice.actions.take(2).forEachIndexed { i, (label, _) ->
                            val id = noticeActionStop(i)
                            val shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp)
                            Text(label, style = TextStyle(fontSize = 22.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = g.text), maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = stop(id).widthIn(max = 180.dp).orbitFocusRounded(ringed == id, accent, g.halo, 26.dp).clip(shape)
                                    .border(2.dp, accent, shape).padding(horizontal = 18.dp, vertical = 10.dp))
                        }
                        OrbitDisc(48.dp, night, stop(NOTICE_DISMISS).orbitFocus(ringed == NOTICE_DISMISS, accent, g.halo)) {
                            Icon(Icons.Rounded.Close, stringResource(R.string.common_dismiss), Modifier.size(26.dp), tint = g.text)
                        }
                    }
                }
            }
        }
    }

    /** The notice's widest, at the sheet's scale: the room between the turn and the clock. */
    private val NOTICE_WIDTH = 480.dp

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
        Box(modifier.background(g.halo.copy(alpha = 0.72f)).orbitRipple(0.5f, 0.4f), contentAlignment = Alignment.Center) {
            Scaled {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(472.dp, 260.dp), contentAlignment = Alignment.Center) {
                        // The sheet's soft rings around the caller.
                        Canvas(Modifier.size(472.dp)) {
                            drawCircle(calls.copy(alpha = 0.14f), 236.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(4.dp.toPx()))
                            drawCircle(calls.copy(alpha = 0.28f), 200.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(4.dp.toPx()))
                        }
                        Box(
                            Modifier.size(240.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF3B4A63), Color(0xFF1D2433))))
                                .border(3.dp, calls, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            val letters = initials(call.name)
                            if (letters != null) Text(letters, style = TextStyle(fontSize = 96.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = Color.White))
                            else Icon(Icons.Rounded.Phone, null, Modifier.size(110.dp), tint = Color.White)
                        }
                    }
                    Text(call.name, style = TextStyle(fontSize = 54.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold).floating(g.text),
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 900.dp))
                    Text(stringResource(R.string.phone_incoming_call), style = TextStyle(fontSize = 28.sp).floating(g.secondary))
                    Spacer(Modifier.height(28.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(212.dp)) {
                        if (canEnd) CallButton(CALL_DECLINE, Icons.Rounded.Close, stringResource(R.string.phone_action_decline), Orbit.warning(night), ringed, calls, night, stop)
                        CallButton(CALL_ANSWER, Icons.Rounded.Phone, stringResource(R.string.phone_action_answer), calls, ringed, calls, night, stop)
                    }
                }
            }
        }
    }

    /** A 128 dp call button filled in its colour, its word under it. */
    @Composable
    private fun CallButton(id: String, icon: ImageVector, label: String, fill: Color, ringed: String, ring: Color, night: Boolean, stop: (String) -> Modifier) {
        val g = Orbit.glass(night)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            OrbitDisc(128.dp, night, stop(id).orbitFocus(ringed == id, ring, g.halo), fill = fill, edge = fill) {
                Icon(icon, label, Modifier.size(56.dp), tint = Color(0xFF06120C))
            }
            Text(label, style = TextStyle(fontSize = 24.sp).floating(g.secondary), modifier = Modifier.padding(top = 8.dp))
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
                        Text(stringResource(R.string.phone_in_call_elapsed, call.name, "${elapsed / 60}:${"%02d".format(elapsed % 60)}"),
                            style = TextStyle(fontSize = 28.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = g.text),
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
        val list = androidx.compose.foundation.lazy.rememberLazyListState()
        LaunchedEffect(ringed, lines.size) { if (lines.isNotEmpty()) list.animateScrollToItem((ringed - 1).coerceAtLeast(0)) }
        Box(modifier.background(g.halo.copy(alpha = 0.7f)).orbitRipple(0.5f, 0.5f)) {
            Scaled {
                Column(Modifier.fillMaxSize().padding(start = 60.dp, top = 40.dp)) {
                    Text(worldName(World.CAR), style = TextStyle(fontSize = 34.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = g.text))
                    if (lines.isEmpty()) {
                        Text(stringResource(R.string.car_data_not_connected), style = Orbit.Type.secondary.floating(g.secondary), modifier = Modifier.padding(start = 80.dp, top = 40.dp))
                        return@Column
                    }
                    Text(stringResource(R.string.myplay_car_hint), style = TextStyle(fontSize = 24.sp, color = g.secondary))
                    androidx.compose.foundation.lazy.LazyColumn(
                        state = list, modifier = Modifier.padding(start = 80.dp, top = 24.dp).width(880.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 20.dp),
                    ) {
                        items(lines.size) { i ->
                            val (label, value) = lines[i]
                            Pill(label, "", Icons.Rounded.DirectionsCar, null, null, false, i == ringed, accent, night, row(i), trailing = value)
                        }
                    }
                }
            }
        }
    }

    @Composable
    override fun SettingsWorld(page: SettingsPage, ringed: Int, row: (Int) -> Modifier, modifier: Modifier) {
        val night = LocalSkinTokens.current.night
        val g = Orbit.glass(night)
        val accent = Orbit.accent(World.SETTINGS, night)
        val list = androidx.compose.foundation.lazy.rememberLazyListState()
        LaunchedEffect(ringed, page.rows.size) { if (page.rows.isNotEmpty()) list.animateScrollToItem((ringed - 1).coerceAtLeast(0)) }
        Box(modifier.background(g.halo.copy(alpha = 0.7f)).orbitRipple(0.5f, 0.5f)) {
            Scaled {
                Column(Modifier.fillMaxSize().padding(start = 60.dp, top = 40.dp)) {
                    Text(worldName(World.SETTINGS), style = TextStyle(fontSize = 34.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = g.text))
                    Text(stringResource(R.string.myplay_settings_hint), style = TextStyle(fontSize = 24.sp, color = g.secondary))
                    androidx.compose.foundation.lazy.LazyColumn(
                        state = list, modifier = Modifier.padding(start = 80.dp, top = 24.dp).width(880.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 20.dp),
                    ) {
                        items(page.rows.size) { i ->
                            val r = page.rows[i]
                            Pill(r.title, r.note.orEmpty(), if (r.switch) (if (r.on) Icons.Rounded.ToggleOn else Icons.Rounded.ToggleOff) else Icons.Rounded.Tune,
                                if (r.switch && r.on) accent else null, null, false, i == ringed, accent, night, row(i), trailing = r.value)
                        }
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
        val list = androidx.compose.foundation.lazy.rememberLazyListState()
        LaunchedEffect(ringed, page.rows.size) { if (page.rows.isNotEmpty()) list.animateScrollToItem((ringed - 1).coerceAtLeast(0)) }
        Box(modifier.background(g.halo.copy(alpha = 0.7f)).orbitRipple(1f, 0f)) {
            Scaled {
                Column(Modifier.fillMaxSize().padding(start = 60.dp, top = 40.dp)) {
                    Text(stringResource(R.string.notif_title), style = TextStyle(fontSize = 34.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = g.text))
                    Text(stringResource(R.string.myplay_notices_hint), style = TextStyle(fontSize = 24.sp, color = g.secondary))
                    androidx.compose.foundation.lazy.LazyColumn(
                        state = list, modifier = Modifier.padding(start = 80.dp, top = 24.dp).width(760.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 20.dp),
                    ) {
                        items(page.rows.size) { i ->
                            val r = page.rows[i]
                            val n = r.notice
                            if (n == null) {
                                Pill(stringResource(R.string.notif_clear_all), "", Icons.Rounded.ClearAll, null, null, false, i == ringed, accent, night, row(i))
                            } else {
                                Pill("${n.title} · ${r.time}", r.text, Icons.Rounded.Notifications, null, n.icon, false, i == ringed, accent, night, row(i), trailing = r.action)
                            }
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
        val list = androidx.compose.foundation.lazy.rememberLazyListState()
        LaunchedEffect(ringed, page.rows.size) { if (page.rows.isNotEmpty()) list.animateScrollToItem((ringed - 1).coerceAtLeast(0)) }
        Box(modifier.background(g.halo.copy(alpha = 0.7f)).orbitRipple(0.5f, 0.5f)) {
            Scaled {
                Column(Modifier.fillMaxSize().padding(start = 60.dp, top = 40.dp)) {
                    Text(worldName(World.CALLS), style = TextStyle(fontSize = 34.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = g.text))
                    Text(stringResource(R.string.myplay_phone_hint), style = TextStyle(fontSize = 24.sp, color = g.secondary))
                    val note = when {
                        !page.permitted -> stringResource(R.string.myplay_phone_permissions)
                        page.loading -> stringResource(R.string.myplay_library_loading)
                        page.rows.size == 1 -> stringResource(R.string.myplay_phone_none)
                        else -> null
                    }
                    note?.let { Text(it, style = Orbit.Type.secondary.floating(g.secondary), modifier = Modifier.padding(start = 80.dp, top = 24.dp).widthIn(max = 900.dp)) }
                    androidx.compose.foundation.lazy.LazyColumn(
                        state = list, modifier = Modifier.padding(start = 80.dp, top = 24.dp).width(640.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 20.dp),
                    ) {
                        items(page.rows.size) { i ->
                            val r = page.rows[i]
                            val (icon, tint) = when (r.kind) {
                                PhoneRow.Kind.RECENT -> Icons.Rounded.Phone to null
                                PhoneRow.Kind.MISSED -> Icons.Rounded.PhoneMissed to Orbit.warning(night)
                                PhoneRow.Kind.FAVOURITE -> Icons.Rounded.Star to null
                                PhoneRow.Kind.PHONE_APP -> Icons.Rounded.Dialpad to null
                            }
                            Pill(r.title, r.subtitle, icon, tint, null, r.kind == PhoneRow.Kind.PHONE_APP, i == ringed, calls, night, row(i))
                        }
                    }
                }
            }
        }
    }

    /** The honeycomb's rows (the sheet: 3, 4, 3), for [appsPerPage] apps. */
    private val ROWS = listOf(3, 4, 3)

    override val appsPerPage get() = ROWS.sum()

    @Composable
    override fun AppsWorld(apps: List<CarAppEntry>, ringed: Int, page: Int, pages: Int, tile: (Int) -> Modifier, modifier: Modifier) {
        val night = LocalSkinTokens.current.night
        val g = Orbit.glass(night)
        val accent = Orbit.accent(World.APPS, night)
        // The map stays behind, dimmed, as the sheet draws it.
        Box(modifier.background(g.halo.copy(alpha = 0.7f)).orbitRipple(0.5f, 0.5f)) {
            Scaled {
                Column(Modifier.padding(start = 60.dp, top = 40.dp)) {
                    Text(worldName(World.APPS), style = TextStyle(fontSize = 34.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = g.text))
                    Text(stringResource(R.string.myplay_apps_hint), style = TextStyle(fontSize = 24.sp, color = g.secondary))
                }
            }
            Box(Modifier.align(Alignment.Center)) {
                Scaled {
                    if (apps.isEmpty()) {
                        Text(stringResource(R.string.myplay_apps_none), style = Orbit.Type.secondary.copy(color = g.secondary))
                        return@Scaled
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        var i = 0
                        for (size in ROWS) {
                            if (i >= apps.size) break
                            Row(horizontalArrangement = Arrangement.spacedBy(44.dp)) {
                                repeat(size) {
                                    val index = i
                                    apps.getOrNull(index)?.let { app -> AppDisc(app, index == ringed, accent, night, tile(index)) }
                                    i++
                                }
                            }
                        }
                    }
                }
            }
            if (pages > 1) {
                Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(pages) { p ->
                        Box(Modifier.size(12.dp).clip(CircleShape).background(if (p == page) accent else g.edge))
                    }
                }
            }
        }
    }

    /** One app in the honeycomb: a disc with its icon, its name under it; the ringed one lit in the Apps colour. */
    @Composable
    private fun AppDisc(app: CarAppEntry, on: Boolean, accent: Color, night: Boolean, modifier: Modifier) {
        val g = Orbit.glass(night)
        Column(modifier.width(160.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            OrbitDisc(
                128.dp, night, Modifier.orbitFocus(on, accent, g.halo),
                fill = if (on) accent.copy(alpha = 0.22f).compositeOver(g.glass) else null,
                edge = if (on) accent else null, edgeWidth = 3.dp,
            ) { AppIcon(app, 64) }
            Text(
                app.label, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                style = TextStyle(fontSize = 25.sp, fontWeight = if (on) androidx.compose.ui.text.font.FontWeight.ExtraBold else androidx.compose.ui.text.font.FontWeight.SemiBold).floating(if (on) g.text else g.secondary),
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }

    /** Test tags. */
    const val BATTERY_TAG = "orbit_battery"
    const val SONG_ART_TAG = "orbit_song_art"
    const val SONG_ART_FALLBACK_TAG = "orbit_song_art_fallback"
    const val TRIP_RING_TAG = "orbit_trip_ring"
    const val SONG_RING_TAG = "orbit_song_ring"

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
