package net.prototux.aare.ui.car.myplay.orbit

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.MusicNote
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
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
import net.prototux.aare.ui.car.mediaProgress
import net.prototux.aare.ui.car.myplay.LocalSkinTokens
import net.prototux.aare.ui.car.myplay.LocalStopRinged
import net.prototux.aare.ui.car.myplay.MyPlaySkin
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
            val nav = Orbit.accent(World.NAVIGATE)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OrbitDisc(132.dp, LocalSkinTokens.current.night, Modifier.orbitFocus(LocalStopRinged.current, nav, g.halo), fill = nav, edge = nav) {
                    val arrow = turn.arrow
                    if (arrow != null) {
                        val image = remember(arrow) { arrow.asImageBitmap() }
                        Image(image, null, Modifier.size(84.dp), contentScale = ContentScale.Fit)
                    } else {
                        Icon(arrowIcon(turn.direction), null, Modifier.size(78.dp), tint = Orbit.ON_ACCENT)
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
            val nav = Orbit.accent(World.NAVIGATE)
            OrbitDisc(192.dp, LocalSkinTokens.current.night, Modifier.orbitFocus(LocalStopRinged.current, nav, g.halo)) {
                OrbitRing(trip?.progress, 184.dp, 9.dp, nav, g.track) {
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
            var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(song.playing) {
                while (song.playing) {
                    delay(1_000)
                    now = System.currentTimeMillis()
                }
            }
            val progress = if (song.durationMs > 0) mediaProgress(song.positionMs, song.durationMs, song.playing, song.updatedAtMs, now) else null
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.widthIn(max = 392.dp), horizontalAlignment = Alignment.End) {
                    Text(song.title, style = Orbit.Type.name.floating(g.text), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
                    Text(song.subtitle, style = Orbit.Type.secondary.floating(g.secondary), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
                }
                Spacer(Modifier.width(10.dp))
                OrbitRing(progress, 152.dp, 7.dp, Orbit.SONG, g.track, Modifier.orbitFocus(LocalStopRinged.current, Orbit.SONG, g.halo)) {
                    val art = song.art
                    if (art != null) {
                        val image = remember(art) { art.asImageBitmap() }
                        Image(image, null, Modifier.size(126.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                    } else {
                        Box(Modifier.size(126.dp).clip(CircleShape).background(ART), contentAlignment = Alignment.Center) {
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
            val nav = Orbit.accent(World.NAVIGATE)
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
                Text(time, style = Orbit.Type.clock.floating(g.text), maxLines = 1)
                rememberPhoneStatus()?.let { s ->
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(if (s.charging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryFull, null, Modifier.size(30.dp), tint = nav)
                        // By day the pale accent reads poorly on light glass: the text colour then.
                        val colour = if (LocalSkinTokens.current.night) nav else g.text
                        Text("${s.battery}%", style = TextStyle(fontSize = 28.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold).floating(colour))
                    }
                }
            }
        }
    }

    @Composable
    override fun Speed(reading: SpeedReading?, modifier: Modifier) = Box(modifier) {
        Scaled {
            val g = glass()
            OrbitDisc(104.dp, LocalSkinTokens.current.night, Modifier.padding(top = 12.dp)) {
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
                        OrbitSector(i, worlds.size, w == selected, Orbit.accent(w), night, DIAL, 132.dp)
                        val a = Math.toRadians(-90.0 + 360.0 / worlds.size * i)
                        val on = w == selected
                        Column(
                            Modifier.offset((205 * cos(a)).dp, (205 * sin(a)).dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(Orbit.icon(w), null, Modifier.size(if (on) 52.dp else 44.dp), tint = if (on) Orbit.accent(w) else g.secondary)
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
    private fun WorldDisc(world: World, night: Boolean, line: String? = null) {
        val g = Orbit.glass(night)
        val accent = Orbit.accent(world)
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
