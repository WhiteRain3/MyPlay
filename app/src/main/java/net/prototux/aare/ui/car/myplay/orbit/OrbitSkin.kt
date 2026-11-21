package net.prototux.aare.ui.car.myplay.orbit

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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhoneMissed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
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
import net.prototux.aare.ui.car.myplay.MusicPage
import net.prototux.aare.ui.car.myplay.PhonePage
import net.prototux.aare.ui.car.myplay.PhoneRow
import net.prototux.aare.ui.car.myplay.CALL_ANSWER
import net.prototux.aare.ui.car.myplay.CALL_DECLINE
import net.prototux.aare.ui.car.myplay.STOP_CALL
import net.prototux.aare.ui.car.myplay.STOP_HANG_UP
import net.prototux.aare.ui.car.CarCall
import net.prototux.aare.ui.car.myplay.LibraryPage
import net.prototux.aare.media.LibraryRow
import net.prototux.aare.ui.car.myplay.MyPlaySkin
import net.prototux.aare.ui.car.myplay.upNextStop
import net.prototux.aare.ui.car.myplay.SkinTokens
import net.prototux.aare.ui.car.myplay.SongSlot
import net.prototux.aare.ui.car.myplay.TripSlot
import net.prototux.aare.ui.car.myplay.TurnDirection
import net.prototux.aare.ui.car.myplay.TurnSlot
import net.prototux.aare.ui.car.myplay.World
import net.prototux.aare.ui.car.rememberPhoneStatus
import kotlin.math.cos
import kotlin.math.sin

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
    override fun Backdrop(modifier: Modifier) {
        val halo = glass().halo
        Canvas(modifier) {
            fun shade(cx: Float, cy: Float, r: Float) = drawRect(
                Brush.radialGradient(listOf(halo.copy(alpha = 0.78f), Color.Transparent), Offset(size.width * cx, size.height * cy), size.width * r),
            )
            shade(0.12f, 0.08f, 0.46f)
            shade(0.92f, 0.94f, 0.44f)
        }
    }

    @Composable
    override fun Turn(turn: TurnSlot, modifier: Modifier) = Box(modifier) {
        Scaled {
            val g = glass()
            val nav = Orbit.accent(World.NAVIGATE, LocalSkinTokens.current.night)
            Row(Modifier.carPart(CarPart.OrbitTurn), verticalAlignment = Alignment.CenterVertically) {
                OrbitDisc(132.dp, LocalSkinTokens.current.night, Modifier.orbitFocus(LocalStopRinged.current, nav, g.halo), fill = nav, edge = nav) {
                    val arrow = turn.arrow
                    if (arrow != null) {
                        val image = remember(arrow) { arrow.asImageBitmap() }
                        Image(image, null, Modifier.size(84.dp), contentScale = ContentScale.Fit)
                    } else {
                        Icon(arrowIcon(turn.direction), null, Modifier.size(78.dp), tint = Orbit.onAccent(LocalSkinTokens.current.night))
                    }
                }
                Spacer(Modifier.width(22.dp))
                Column {
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
                Column(Modifier.widthIn(max = 392.dp), horizontalAlignment = Alignment.End) {
                    Text(song.title, style = Orbit.Type.name.floating(g.text), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
                    Text(song.subtitle, style = Orbit.Type.secondary.floating(g.secondary), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
                }
                Spacer(Modifier.width(10.dp))
                val musicColour = Orbit.accent(World.MUSIC, LocalSkinTokens.current.night)
                OrbitRing(progress, 152.dp, 7.dp, musicColour, g.track, Modifier.testTag(SONG_RING_TAG).progressSemantics(progress).orbitFocus(LocalStopRinged.current, musicColour, g.halo)) {
                    val art = song.art
                    if (art != null) {
                        val image = remember(art) { art.asImageBitmap() }
                        Image(image, null, Modifier.size(126.dp).clip(CircleShape).testTag(SONG_ART_TAG), contentScale = ContentScale.Crop)
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
            OrbitDisc(104.dp, LocalSkinTokens.current.night, Modifier.padding(top = 12.dp).carPart(CarPart.OrbitSpeed)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(reading?.kmh?.toString() ?: "–", style = Orbit.Type.name.copy(color = g.text, lineHeight = 36.sp), maxLines = 1)
                    Text(stringResource(R.string.myplay_speed_unit_kmh), style = TextStyle(fontSize = 18.sp, color = g.secondary), maxLines = 1)
                }
            }
        }
    }

    @Composable
    override fun Chooser(selected: World, modifier: Modifier) = Box(modifier.orbitRipple(0f, 1f)) {
        Scaled {
            val g = glass()
            val night = LocalSkinTokens.current.night
            val worlds = World.entries
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
                Text(stringResource(R.string.myplay_dial_hint), style = TextStyle(fontSize = 24.sp).floating(g.secondary))
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
        Box(modifier.size(320.dp).orbitFocusRounded(on, accent, g.halo, 78.dp).clip(shape)) {
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
        on: Boolean, accent: Color, night: Boolean, modifier: Modifier,
    ) {
        val g = Orbit.glass(night)
        val shape = androidx.compose.foundation.shape.RoundedCornerShape(52.dp)
        Row(
            modifier.fillMaxWidth().height(104.dp).orbitFocusRounded(on, accent, g.halo, 52.dp).clip(shape).background(g.glass)
                .border(3.dp, if (on) accent else g.edge, shape).padding(start = 14.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Box(Modifier.size(76.dp).clip(CircleShape).background(if (on) accent else g.track), contentAlignment = Alignment.Center) {
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
            if (leadsOn) Icon(Icons.Rounded.ChevronRight, null, Modifier.size(36.dp), tint = g.secondary)
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
