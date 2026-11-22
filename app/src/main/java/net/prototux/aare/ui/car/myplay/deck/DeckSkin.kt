package net.prototux.aare.ui.car.myplay.deck

import android.os.SystemClock
import android.text.format.DateFormat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhoneMissed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.ToggleOff
import androidx.compose.material.icons.rounded.ToggleOn
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.produceState
import net.prototux.aare.media.LibraryRow
import net.prototux.aare.ui.car.myplay.LibraryPage
import net.prototux.aare.ui.car.myplay.NoticeListPage
import net.prototux.aare.ui.car.myplay.PhonePage
import net.prototux.aare.ui.car.myplay.PhoneRow
import net.prototux.aare.ui.car.myplay.SettingsPage
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import net.prototux.aare.ui.car.CarAppEntry
import net.prototux.aare.ui.car.myplay.MUSIC_ART
import net.prototux.aare.ui.car.myplay.MUSIC_LIBRARY
import net.prototux.aare.ui.car.myplay.MUSIC_NEXT
import net.prototux.aare.ui.car.myplay.MUSIC_PLAY
import net.prototux.aare.ui.car.myplay.MUSIC_PREV
import net.prototux.aare.ui.car.myplay.MusicPage
import net.prototux.aare.ui.car.myplay.upNextStop
import net.prototux.aare.ui.car.myplay.orbit.orbitFocus
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import net.prototux.aare.ui.car.CarCall
import net.prototux.aare.ui.car.CarNotice
import net.prototux.aare.ui.car.carFocusHighlight
import net.prototux.aare.ui.car.myplay.CALL_ANSWER
import net.prototux.aare.ui.car.myplay.CALL_DECLINE
import net.prototux.aare.ui.car.myplay.NOTICE_DISMISS
import net.prototux.aare.ui.car.myplay.STOP_CALL
import net.prototux.aare.ui.car.myplay.STOP_HANG_UP
import net.prototux.aare.ui.car.myplay.noticeActionStop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import net.prototux.aare.R
import net.prototux.aare.car.LimitShown
import net.prototux.aare.car.SpeedReading
import net.prototux.aare.ui.car.mediaProgress
import net.prototux.aare.ui.car.myplay.LocalSkinTokens
import net.prototux.aare.ui.car.myplay.LocalStopRinged
import net.prototux.aare.ui.car.myplay.MyPlaySkin
import net.prototux.aare.ui.car.myplay.SkinTokens
import net.prototux.aare.ui.car.myplay.SongSlot
import net.prototux.aare.ui.car.myplay.TripSlot
import net.prototux.aare.ui.car.myplay.TurnSlot
import net.prototux.aare.ui.car.myplay.World
import net.prototux.aare.ui.car.myplay.orbit.Orbit
import net.prototux.aare.ui.car.myplay.orbit.OrbitSkin
import net.prototux.aare.ui.car.myplay.orbit.orbitFocusRounded
import net.prototux.aare.ui.car.rememberPhoneStatus

/**
 * MyPlay (plan phase N, N13): DECK, the second design language: a hand of colour-coded cards
 * fanned in the bottom-right corner, and a large heads-up turn read-out top left.
 *
 * DECK draws its home (the turn and trip, the clock and battery, the speed with its limit, the
 * hand) and, since N14, its pages: each world's card opened full screen (DeckPages.kt). What is
 * not drawn yet is still ORBIT's (the engine and the knob are the same either way).
 */
object DeckSkin : MyPlaySkin by OrbitSkin {
    /** O3 is ORBIT's: DECK (frozen) keeps the rail beside an app. */
    override val appRail get() = true

    /** AL-1 is ORBIT's: DECK (frozen) keeps the default alert card. */
    override val drawsMapAlerts get() = false

    /** O7 is ORBIT's: DECK (frozen, a hand, no dial) keeps all six. */
    override val dial: List<World>? get() = null

    /** O8 is ORBIT's: DECK (frozen) keeps its speed under the status. */
    override val speedInHub get() = false

    override val id = "deck"

    override fun tokens(night: Boolean): SkinTokens = Deck.tokens(night)

    override val drawsFocus get() = true
    override val showsSpeed get() = true

    /** The sheet's five cards, back to front, with Settings (not on the sheet) behind Apps. */
    override val hand = listOf(World.NAVIGATE, World.CALLS, World.CAR, World.SETTINGS, World.APPS, World.MUSIC)

    /** The sheet's screen; sizes scale from it, as ORBIT's do. */
    @Composable
    private fun Scaled(content: @Composable () -> Unit) {
        val c = LocalConfiguration.current
        val d = LocalDensity.current
        val s = OrbitSkin.scale(c.screenWidthDp, c.screenHeightDp)
        CompositionLocalProvider(LocalDensity provides Density(d.density * s, d.fontScale), content = content)
    }

    @Composable
    private fun paper() = Deck.paper(LocalSkinTokens.current.night)

    /** Text over the map gets a soft shadow, so it reads on any map. */
    @Composable
    private fun TextStyle.floating(color: Color): TextStyle =
        copy(color = color, shadow = Shadow(paper().halo.copy(alpha = 0.85f), Offset(0f, 2f), 6f))

    /** The focus: a heavy outline in Navigate's teal round the whole part (the sheet's). */
    @Composable
    private fun Modifier.deckFocus(on: Boolean): Modifier {
        val night = LocalSkinTokens.current.night
        return orbitFocusRounded(on, Deck.hue(World.NAVIGATE, night), paper().halo, Deck.CORNER)
    }

    @Composable
    override fun Turn(turn: TurnSlot, modifier: Modifier) = Box(modifier) {
        Scaled {
            val p = paper()
            val nav = Deck.hue(World.NAVIGATE, LocalSkinTokens.current.night)
            Row(Modifier.deckFocus(LocalStopRinged.current).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                val arrow = turn.arrow
                if (arrow != null) {
                    val image = remember(arrow) { arrow.asImageBitmap() }
                    Image(image, null, Modifier.size(112.dp), contentScale = ContentScale.Fit)
                } else {
                    Icon(OrbitSkin.arrowIcon(turn.direction), null, Modifier.size(120.dp), tint = nav)
                }
                Spacer(Modifier.width(24.dp))
                Column {
                    turn.distance?.let { Text(it, style = Deck.Type.hero.floating(p.text), maxLines = 1) }
                    turn.road?.let { Text(it, style = Deck.Type.secondary.floating(p.secondary), maxLines = 2, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
    }

    @Composable
    override fun Trip(trip: TripSlot, modifier: Modifier) = Box(modifier) {
        Scaled {
            val p = paper()
            Row(Modifier.padding(top = 20.dp), verticalAlignment = Alignment.Bottom) {
                trip.arrival?.let { Text(it, style = TextStyle(fontSize = 38.sp, fontWeight = FontWeight.Bold).floating(p.text), maxLines = 1) }
                val rest = listOfNotNull(trip.timeLeft, trip.distanceLeft).joinToString(" · ")
                if (rest.isNotEmpty()) Text(rest, style = Deck.Type.secondary.floating(p.secondary), maxLines = 1, modifier = Modifier.padding(start = 16.dp, bottom = 3.dp))
            }
        }
    }

    @Composable
    override fun Status(modifier: Modifier) = Box(modifier) {
        Scaled {
            val p = paper()
            val night = LocalSkinTokens.current.night
            val nav = Deck.hue(World.NAVIGATE, night)
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
                Text(time, style = Deck.Type.clock.floating(p.text), maxLines = 1)
                // As ORBIT (N5-3): no level when the phone does not tell it, never "0%".
                rememberPhoneStatus()?.takeIf { it.batteryKnown }?.let { s ->
                    val colour = if (OrbitSkin.batteryLow(s.battery, s.charging)) Orbit.warning(night) else nav
                    Row(Modifier.padding(top = 6.dp).testTag(DECK_BATTERY_TAG), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(OrbitSkin.batteryIcon(s.battery, s.charging), null, Modifier.size(30.dp), tint = colour)
                        Text("${s.battery}%", style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold).floating(colour))
                    }
                }
            }
        }
    }

    @Composable
    override fun Speed(reading: SpeedReading?, modifier: Modifier) = Box(modifier) {
        Scaled {
            val p = paper()
            val night = LocalSkinTokens.current.night
            val shape = RoundedCornerShape(Deck.CORNER)
            Column(Modifier.padding(top = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                // A small card with Navigate's edge; over the limit the number turns a calm amber (N20), no sound.
                Box(Modifier.size(116.dp, 104.dp).shadow(8.dp, shape).clip(shape).background(Deck.cardColour(World.NAVIGATE, night))) {
                    Box(Modifier.fillMaxWidth().height(Deck.EDGE).background(Deck.hue(World.NAVIGATE, night)))
                    Column(Modifier.align(Alignment.Center).padding(top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        val colour = if (reading?.over == true) Orbit.caution(night) else p.text
                        Text(reading?.kmh?.toString() ?: "–", style = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold, lineHeight = 46.sp, color = colour), maxLines = 1)
                        Text(stringResource(R.string.myplay_speed_unit_kmh), style = TextStyle(fontSize = 18.sp, color = p.secondary), maxLines = 1)
                    }
                }
                reading?.limit?.let { limit ->
                    // The map's limit: a plain outlined square (advice, not a road sign), always labelled.
                    Column(Modifier.padding(top = 8.dp).widthIn(max = 170.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(60.dp).clip(shape).background(p.card.copy(alpha = 0.92f)).border(3.dp, p.secondary, shape), contentAlignment = Alignment.Center) {
                            Text((limit as? LimitShown.Known)?.kmh?.toString() ?: "–", style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = p.text), maxLines = 1)
                        }
                        Text(
                            stringResource(if (limit is LimitShown.Known) R.string.myplay_speed_limit_map_data else R.string.myplay_speed_limit_unknown),
                            style = TextStyle(fontSize = 18.sp, lineHeight = 20.sp, textAlign = TextAlign.Center).floating(p.secondary),
                            maxLines = 2, modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }

    /**
     * The hand's room at 1280x720 (the sheet's), and one card. The fan is kept so that its farthest
     * card stays right of the screen's middle at every size ("nothing over the middle of the map"):
     * the back card's far corner is about 512 dp left of the hand's right edge, inside [HAND_W].
     */
    private val HAND_W = 520.dp
    private val HAND_H = 380.dp
    private val CARD_W = 300.dp
    private val CARD_H = 320.dp
    /** Each card behind the front one is turned this much further round the fan, about a point below the cards. */
    private const val FAN_STEP_DEG = -7f
    private const val FAN_PIVOT_Y = 1.3f

    @Composable
    override fun Hand(front: World, song: SongSlot?, ringed: Boolean, pick: (World) -> Unit, modifier: Modifier) = Box(modifier) {
        Scaled {
            val night = LocalSkinTokens.current.night
            val n = hand.size
            val f = hand.indexOf(front)
            // How far behind the front card each card is: the one a turn back brings is just behind it.
            val depth = hand.indices.associate { i -> hand[i] to ((f - i) % n + n) % n }
            Box(Modifier.size(HAND_W, HAND_H)) {
                for (w in hand.sortedByDescending { depth.getValue(it) }) key(w) {
                    val k = depth.getValue(w)
                    val angle by animateFloatAsState(FAN_STEP_DEG * k, tween(250), label = "fan")
                    val isFront = k == 0
                    Card(
                        w, isFront, ringed && isFront, song, night,
                        Modifier.align(Alignment.BottomEnd)
                            .graphicsLayer { rotationZ = angle; transformOrigin = TransformOrigin(0.5f, FAN_PIVOT_Y) }
                            // Every card, the front one too, so a tap on the front card never falls
                            // through to a card behind it (whose unrotated box can reach under it).
                            .pointerInput(w) { detectTapGestures { pick(w) } }
                            .testTag(deckCardTag(w)),
                    )
                }
            }
        }
    }

    @Composable
    private fun Card(world: World, front: Boolean, ringed: Boolean, song: SongSlot?, night: Boolean, modifier: Modifier) {
        val shape = RoundedCornerShape(Deck.CORNER)
        val hue = Deck.hue(world, night)
        Box(modifier.size(CARD_W, CARD_H).deckFocus(ringed).shadow(14.dp, shape).clip(shape).background(Deck.cardColour(world, night))) {
            if (front && world == World.MUSIC && song != null) MusicFront(song) else if (front) WorldFront(world, night)
            else Icon(Deck.icon(world), null, Modifier.padding(start = 18.dp, top = 24.dp).size(34.dp), tint = hue)
            Box(Modifier.fillMaxWidth().height(Deck.EDGE).background(hue))
        }
    }

    @Composable
    private fun WorldFront(world: World, night: Boolean) {
        val p = Deck.paper(night)
        val hue = Deck.hue(world, night)
        Column(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 30.dp, bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Deck.icon(world), null, Modifier.size(30.dp), tint = hue)
                Spacer(Modifier.width(10.dp))
                Text(OrbitSkin.worldName(world).uppercase(), style = Deck.Type.label.copy(color = hue), maxLines = 1)
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Icon(Deck.icon(world), null, Modifier.size(112.dp), tint = hue)
            }
            Text(stringResource(hint(world)), style = Deck.Type.secondary.copy(color = p.secondary), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }

    private fun hint(world: World): Int = when (world) {
        World.NAVIGATE -> R.string.myplay_deck_hint_navigate
        World.CALLS -> R.string.myplay_deck_hint_calls
        World.CAR -> R.string.myplay_deck_hint_car
        World.SETTINGS -> R.string.myplay_deck_hint_settings
        World.APPS -> R.string.myplay_deck_hint_apps
        World.MUSIC -> R.string.myplay_music_nothing
    }

    /** The sheet's artwork stand-in when a song has none (as ORBIT's). */
    private val ART = Brush.linearGradient(listOf(Color(0xFF4CC9F0), Color(0xFF8F5BFF), Color(0xFFFF5A8A)))

    @Composable
    private fun MusicFront(song: SongSlot) {
        // The player's position is on SystemClock.elapsedRealtime() (as ORBIT's song ring).
        var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
        LaunchedEffect(song.playing, song.updatedAtMs) {
            now = SystemClock.elapsedRealtime()
            while (song.playing) {
                delay(1_000)
                now = SystemClock.elapsedRealtime()
            }
        }
        val progress = if (song.durationMs > 0) mediaProgress(song.positionMs, song.durationMs, song.playing, song.updatedAtMs, now) else null
        Box(Modifier.fillMaxSize()) {
            val art = song.art
            if (art != null) {
                val image = remember(art) { art.asImageBitmap() }
                Image(image, null, Modifier.fillMaxSize().testTag(DECK_ART_TAG), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().background(ART), contentAlignment = Alignment.Center) {
                    Icon(net.prototux.aare.ui.car.myplay.deck.Deck.icon(World.MUSIC), null, Modifier.padding(bottom = 70.dp).size(120.dp), tint = Color.White)
                }
            }
            // The title on a shade at the bottom, always white (it sits on the artwork).
            Column(
                Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
                    .padding(start = 22.dp, end = 22.dp, top = 40.dp, bottom = 20.dp),
            ) {
                Text(song.title, style = Deck.Type.title.copy(color = Color.White, fontSize = 34.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.subtitle, style = Deck.Type.secondary.copy(color = Color(0xFFE3E6EE), fontSize = 25.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (progress != null) {
                    Box(Modifier.padding(top = 12.dp).fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0x55FFFFFF))) {
                        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(6.dp).background(Deck.hue(World.MUSIC, true)).testTag(DECK_PROGRESS_TAG))
                    }
                }
            }
        }
    }
    // N14: DECK's call, notice, bell and the rail beside an app.

    @Composable
    override fun Ringing(call: CarCall, canEnd: Boolean, ringed: String, stop: (String) -> Modifier, modifier: Modifier) {
        val night = LocalSkinTokens.current.night
        val p = Deck.paper(night)
        val green = Deck.hue(World.CALLS, night)
        val shape = RoundedCornerShape(Deck.CORNER)
        // The sheet: "Incoming call: the Phone card takes over".
        Box(modifier.background(p.halo.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
            DeckScaled {
                Box(Modifier.width(620.dp).shadow(18.dp, shape).clip(shape).background(Deck.cardColour(World.CALLS, night))) {
                    Box(Modifier.fillMaxWidth().height(Deck.EDGE).background(green))
                    Column(Modifier.padding(start = 34.dp, end = 34.dp, top = 34.dp, bottom = 30.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Phone, null, Modifier.size(28.dp), tint = green)
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.phone_incoming_call).uppercase(), style = Deck.Type.label.copy(color = green, fontSize = 22.sp), maxLines = 1)
                        }
                        Text(call.name, style = TextStyle(fontSize = 60.sp, fontWeight = FontWeight.Bold, color = p.text), maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 8.dp))
                        Spacer(Modifier.height(26.dp))
                        CallKey(CALL_ANSWER, Icons.Rounded.Phone, stringResource(R.string.phone_action_answer), green, filled = true, ringed, night, stop)
                        if (canEnd) {
                            Spacer(Modifier.height(14.dp))
                            CallKey(CALL_DECLINE, Icons.Rounded.Close, stringResource(R.string.phone_action_decline), Orbit.warning(night), filled = false, ringed, night, stop)
                        }
                    }
                }
            }
        }
    }

    /** A full-width call button: Accept filled in green, Decline outlined in red (the sheet's). */
    @Composable
    private fun CallKey(id: String, icon: ImageVector, label: String, colour: Color, filled: Boolean, ringed: String, night: Boolean, stop: (String) -> Modifier) {
        val shape = RoundedCornerShape(10.dp)
        val text = if (filled) (if (night) Color(0xFF052E16) else Color.White) else colour
        Row(
            stop(id).fillMaxWidth().height(84.dp).deckFocus(ringed == id).clip(shape)
                .then(if (filled) Modifier.background(colour) else Modifier.border(3.dp, colour, shape)),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, Modifier.size(34.dp), tint = text)
            Spacer(Modifier.width(12.dp))
            Text(label, style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, color = text), maxLines = 1)
        }
    }

    @Composable
    override fun InCall(call: CarCall, canEnd: Boolean, ringed: String?, stop: (String) -> Modifier, modifier: Modifier) {
        val night = LocalSkinTokens.current.night
        val p = Deck.paper(night)
        val green = Deck.hue(World.CALLS, night)
        val elapsed by produceState(0L, call.sinceMs) {
            while (true) { value = ((System.currentTimeMillis() - call.sinceMs) / 1000).coerceAtLeast(0); delay(1_000) }
        }
        Box(modifier) {
            DeckScaled {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val shape = RoundedCornerShape(Deck.CORNER)
                    Box(stop(STOP_CALL).height(76.dp).deckFocus(ringed == STOP_CALL).shadow(10.dp, shape).clip(shape).background(Deck.cardColour(World.CALLS, night))) {
                        // The edge is drawn over the strip's own width (matchParentSize), so it never widens it.
                        Box(Modifier.matchParentSize()) { Box(Modifier.fillMaxWidth().height(5.dp).background(green)) }
                        Row(Modifier.fillMaxHeight().padding(start = 18.dp, end = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Rounded.Phone, null, Modifier.size(30.dp), tint = green)
                            Text(stringResource(R.string.phone_in_call_elapsed, call.name, "${elapsed / 60}:${"%02d".format(elapsed % 60)}"),
                                style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = p.text),
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 460.dp))
                        }
                    }
                    if (canEnd) {
                        val red = Orbit.warning(night)
                        Box(stop(STOP_HANG_UP).size(76.dp).deckFocus(ringed == STOP_HANG_UP).clip(shape).background(red), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.CallEnd, stringResource(R.string.phone_action_hang_up), Modifier.size(36.dp), tint = Color.White)
                        }
                    }
                }
            }
        }
    }

    @Composable
    override fun Notice(notice: CarNotice, hideText: Boolean, ringed: String?, stop: (String) -> Modifier, modifier: Modifier) {
        val night = LocalSkinTokens.current.night
        val p = Deck.paper(night)
        val hue = Deck.hue(World.NAVIGATE, night)
        val shape = RoundedCornerShape(Deck.CORNER)
        Box(modifier) {
            DeckScaled {
                // At most 480 wide (as ORBIT's): it fits between the turn read-out and the clock.
                Box(Modifier.widthIn(max = 480.dp).shadow(12.dp, shape).clip(shape).background(Deck.cardColour(World.NAVIGATE, night))) {
                    Box(Modifier.matchParentSize()) { Box(Modifier.fillMaxWidth().height(5.dp).background(hue)) }
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(hue.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                                val icon = notice.icon
                                if (icon != null) {
                                    val image = remember(icon) { icon.asImageBitmap() }
                                    Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                } else Icon(Icons.Rounded.Notifications, null, Modifier.size(28.dp), tint = hue)
                            }
                            Column(Modifier.weight(1f, fill = false)) {
                                Text(notice.title, style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, color = p.text), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                // While driving a message shows who wrote, not what (as everywhere in the car).
                                Text(if (hideText) stringResource(R.string.notif_new_message_placeholder) else notice.text,
                                    style = TextStyle(fontSize = 22.sp, color = p.secondary), maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            val key = RoundedCornerShape(8.dp)
                            notice.actions.take(2).forEachIndexed { i, (label, _) ->
                                val id = noticeActionStop(i)
                                Text(label, style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = p.text), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = stop(id).widthIn(max = 180.dp).deckFocus(ringed == id).clip(key).border(2.dp, hue, key).padding(horizontal = 16.dp, vertical = 9.dp))
                            }
                            Box(stop(NOTICE_DISMISS).size(46.dp).deckFocus(ringed == NOTICE_DISMISS).clip(key).background(p.card), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Close, stringResource(R.string.common_dismiss), Modifier.size(26.dp), tint = p.text)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    override fun Bell(count: Int, ringed: Boolean, modifier: Modifier) {
        val night = LocalSkinTokens.current.night
        val p = Deck.paper(night)
        val shape = RoundedCornerShape(Deck.CORNER)
        Box(modifier) {
            DeckScaled {
                Box(Modifier.padding(top = 12.dp)) {
                    Box(Modifier.size(68.dp).deckFocus(ringed).shadow(8.dp, shape).clip(shape).background(Deck.cardColour(World.NAVIGATE, night)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Notifications, stringResource(R.string.notif_title), Modifier.size(36.dp), tint = p.text)
                    }
                    Box(Modifier.align(Alignment.TopEnd).padding(top = 0.dp).size(28.dp).clip(RoundedCornerShape(6.dp)).background(Orbit.warning(night)), contentAlignment = Alignment.Center) {
                        Text(if (count > 9) "9+" else "$count", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White))
                    }
                }
            }
        }
    }

    @Composable
    override fun AppRail(home: () -> Unit, modifier: Modifier) {
        val night = LocalSkinTokens.current.night
        val p = Deck.paper(night)
        val hue = Deck.hue(World.NAVIGATE, night)
        Column(modifier.width(net.prototux.aare.ui.car.myplay.APP_RAIL_WIDTH).background(Deck.cardColour(World.NAVIGATE, night)), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().height(5.dp).background(hue))
            Box(
                Modifier.padding(top = 18.dp).size(58.dp).clip(RoundedCornerShape(10.dp)).background(hue)
                    .carFocusHighlight(RoundedCornerShape(10.dp))
                    .clickable(onClickLabel = stringResource(R.string.myplay_frame_home)) { home() }
                    .testTag(net.prototux.aare.ui.car.myplay.FRAME_HOME_TAG),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Home, stringResource(R.string.myplay_frame_home), Modifier.size(32.dp), tint = if (night) Color(0xFF04221D) else Color.White) }
            Spacer(Modifier.weight(1f))
            Box(Modifier.padding(bottom = 18.dp).size(10.dp).clip(RoundedCornerShape(2.dp)).background(p.secondary.copy(alpha = 0.4f)))
        }
    }

    // N14: DECK's pages (DeckPages.kt). Each world's card opened to fill the screen.

    /** The sheet's app list: five tiles a row, two rows a page. */
    private const val APPS_COLUMNS = 5
    override val appsPerPage get() = 2 * APPS_COLUMNS

    @Composable
    override fun AppsWorld(apps: List<CarAppEntry>, ringed: Int, page: Int, pages: Int, tile: (Int) -> Modifier, modifier: Modifier) {
        val night = LocalSkinTokens.current.night
        val p = Deck.paper(night)
        val hue = Deck.hue(World.APPS, night)
        DeckPage(World.APPS, OrbitSkin.worldName(World.APPS), stringResource(R.string.myplay_apps_hint), night, modifier) {
            if (apps.isEmpty()) {
                DeckNote(stringResource(R.string.myplay_apps_none), night)
                return@DeckPage
            }
            Column(Modifier.fillMaxWidth().weight(1f).padding(top = 26.dp), verticalArrangement = Arrangement.spacedBy(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                apps.chunked(APPS_COLUMNS).forEachIndexed { r, chunk ->
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        chunk.forEachIndexed { c, app ->
                            val i = r * APPS_COLUMNS + c
                            AppTile(app, i == ringed, hue, night, tile(i))
                        }
                    }
                }
            }
            if (pages > 1) {
                Row(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(pages) { n -> Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(if (n == page) hue else p.secondary.copy(alpha = 0.4f))) }
                }
            }
        }
    }

    /** One app: a tinted tile with its icon and name; the ringed one in the Apps hue with the teal outline. */
    @Composable
    private fun AppTile(app: CarAppEntry, on: Boolean, hue: Color, night: Boolean, modifier: Modifier) {
        val p = Deck.paper(night)
        val shape = RoundedCornerShape(12.dp)
        Column(
            modifier.size(196.dp, 168.dp).deckFocus(on).clip(shape)
                .background(if (on) hue.copy(alpha = 0.30f).compositeOver(p.card) else p.card.copy(alpha = 0.6f).compositeOver(Deck.cardColour(World.APPS, night))),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            net.prototux.aare.ui.car.AppIcon(app, 64)
            Text(
                app.label, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                style = TextStyle(fontSize = 23.sp, fontWeight = FontWeight.Bold, color = p.text), modifier = Modifier.padding(top = 12.dp, start = 8.dp, end = 8.dp),
            )
        }
    }

    @Composable
    override fun MusicWorld(page: MusicPage, ringed: String, stop: (String) -> Modifier, modifier: Modifier) {
        val night = LocalSkinTokens.current.night
        val p = Deck.paper(night)
        val hue = Deck.hue(World.MUSIC, night)
        DeckPage(World.MUSIC, OrbitSkin.worldName(World.MUSIC), null, night, modifier) {
            val song = page.song
            if (song == null) {
                DeckNote(stringResource(R.string.myplay_music_nothing), night)
                Box(Modifier.padding(top = 24.dp)) { MusicKey(MUSIC_LIBRARY, Icons.Rounded.LibraryMusic, false, ringed, hue, night, stop) }
                return@DeckPage
            }
            Row(Modifier.fillMaxSize().padding(top = 22.dp), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MusicArt(song, ringed == MUSIC_ART, stop(MUSIC_ART))
                    // The sheet's floating pill of buttons under the artwork.
                    Row(
                        // A shaped background, not a clip: the keys' focus halo reaches outside the pill (N14-1).
                        Modifier.padding(top = 18.dp).background(p.halo.copy(alpha = if (night) 0.55f else 0.10f), RoundedCornerShape(44.dp)).padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MusicKey(MUSIC_PREV, Icons.Rounded.SkipPrevious, false, ringed, hue, night, stop)
                        MusicKey(MUSIC_PLAY, if (song.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, true, ringed, hue, night, stop)
                        MusicKey(MUSIC_NEXT, Icons.Rounded.SkipNext, false, ringed, hue, night, stop)
                        MusicKey(MUSIC_LIBRARY, Icons.Rounded.LibraryMusic, false, ringed, hue, night, stop)
                    }
                }
                if (page.transport.upNext.isNotEmpty()) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.myplay_music_up_next).uppercase(), style = Deck.Type.label.copy(color = hue, fontSize = 20.sp))
                        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            page.transport.upNext.forEachIndexed { i, u ->
                                val id = upNextStop(u.queueId)
                                DeckRow(u.title, u.subtitle, Icons.Rounded.MusicNote, hue, night, ringed == id, stop(id), trailing = "${i + 1}")
                            }
                        }
                    }
                }
            }
        }
    }

    /** The artwork box: the art, the title, artist and the progress bar over its bottom (left and right seek here, R5-5). */
    @Composable
    private fun MusicArt(song: SongSlot, on: Boolean, modifier: Modifier) {
        var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
        LaunchedEffect(song.playing, song.updatedAtMs) {
            now = SystemClock.elapsedRealtime()
            while (song.playing) {
                delay(500)
                now = SystemClock.elapsedRealtime()
            }
        }
        val progress = if (song.durationMs > 0) mediaProgress(song.positionMs, song.durationMs, song.playing, song.updatedAtMs, now) else null
        val shape = RoundedCornerShape(Deck.CORNER)
        Box(modifier.size(330.dp).deckFocus(on).clip(shape)) {
            val art = song.art
            if (art != null) {
                val image = remember(art) { art.asImageBitmap() }
                Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().background(ART), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.MusicNote, null, Modifier.padding(bottom = 70.dp).size(120.dp), tint = Color.White)
                }
            }
            Column(
                Modifier.align(Alignment.BottomStart).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
                    .padding(start = 22.dp, end = 22.dp, top = 40.dp, bottom = 20.dp),
            ) {
                Text(song.title, style = Deck.Type.title.copy(color = Color.White, fontSize = 34.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.subtitle, style = Deck.Type.secondary.copy(color = Color(0xFFE3E6EE), fontSize = 25.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (progress != null) {
                    Box(Modifier.padding(top = 12.dp).fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0x55FFFFFF)).testTag(DECK_MUSIC_PROGRESS_TAG)) {
                        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(6.dp).background(Deck.hue(World.MUSIC, true)))
                    }
                }
            }
        }
    }

    /** One of the music buttons: a round key on the pill, the big one (play) filled in the Music hue. */
    @Composable
    private fun MusicKey(id: String, icon: ImageVector, big: Boolean, ringed: String, hue: Color, night: Boolean, stop: (String) -> Modifier) {
        val p = Deck.paper(night)
        val size = if (big) 84.dp else 68.dp
        Box(
            stop(id).size(size).orbitFocus(ringed == id, Deck.hue(World.NAVIGATE, night), p.halo).clip(CircleShape)
                .background(if (big) hue else p.card),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, Modifier.size(if (big) 44.dp else 34.dp), tint = if (big) (if (night) Color(0xFF1A1033) else Color.White) else p.text) }
    }


    @Composable
    override fun SettingsWorld(page: SettingsPage, ringed: Int, row: (Int) -> Modifier, modifier: Modifier) {
        val night = LocalSkinTokens.current.night
        val hue = Deck.hue(World.SETTINGS, night)
        DeckPage(World.SETTINGS, OrbitSkin.worldName(World.SETTINGS), stringResource(R.string.myplay_settings_hint), night, modifier) {
            DeckList(ringed, page.rows.size) { i ->
                val r = page.rows[i]
                DeckRow(
                    r.title, r.note.orEmpty(), if (r.switch) (if (r.on) Icons.Rounded.ToggleOn else Icons.Rounded.ToggleOff) else Icons.Rounded.Tune,
                    hue, night, i == ringed, row(i), trailing = r.value,
                )
            }
        }
    }

    @Composable
    override fun CarWorld(lines: List<Pair<String, String>>, ringed: Int, row: (Int) -> Modifier, modifier: Modifier) {
        val night = LocalSkinTokens.current.night
        val hue = Deck.hue(World.CAR, night)
        DeckPage(World.CAR, OrbitSkin.worldName(World.CAR), stringResource(R.string.myplay_car_hint).takeIf { lines.isNotEmpty() }, night, modifier) {
            if (lines.isEmpty()) DeckNote(stringResource(R.string.car_data_not_connected), night)
            else DeckList(ringed, lines.size) { i ->
                val (label, value) = lines[i]
                DeckRow(label, "", Deck.icon(World.CAR), hue, night, i == ringed, row(i), trailing = value)
            }
        }
    }

    @Composable
    override fun PhoneWorld(page: PhonePage, ringed: Int, row: (Int) -> Modifier, modifier: Modifier) {
        val night = LocalSkinTokens.current.night
        val hue = Deck.hue(World.CALLS, night)
        DeckPage(World.CALLS, OrbitSkin.worldName(World.CALLS), stringResource(R.string.myplay_phone_hint), night, modifier) {
            val note = when {
                !page.permitted -> stringResource(R.string.myplay_phone_permissions)
                page.loading -> stringResource(R.string.myplay_library_loading)
                page.rows.size == 1 -> stringResource(R.string.myplay_phone_none)
                else -> null
            }
            note?.let { DeckNote(it, night) }
            DeckList(ringed, page.rows.size) { i ->
                val r = page.rows[i]
                val (icon, tint) = when (r.kind) {
                    PhoneRow.Kind.RECENT -> Icons.Rounded.Phone to null
                    PhoneRow.Kind.MISSED -> Icons.Rounded.PhoneMissed to Orbit.warning(night)
                    PhoneRow.Kind.FAVOURITE -> Icons.Rounded.Star to null
                    PhoneRow.Kind.PHONE_APP -> Icons.Rounded.Dialpad to null
                }
                DeckRow(r.title, r.subtitle, icon, hue, night, i == ringed, row(i), iconTint = tint, leadsOn = r.kind == PhoneRow.Kind.PHONE_APP)
            }
        }
    }

    @Composable
    override fun NoticeList(page: NoticeListPage, ringed: Int, row: (Int) -> Modifier, modifier: Modifier) {
        val night = LocalSkinTokens.current.night
        val hue = Deck.hue(World.NAVIGATE, night)
        DeckPage(World.NAVIGATE, stringResource(R.string.notif_title), stringResource(R.string.myplay_notices_hint), night, modifier) {
            DeckList(ringed, page.rows.size) { i ->
                val r = page.rows[i]
                val n = r.notice
                if (n == null) DeckRow(stringResource(R.string.notif_clear_all), "", Icons.Rounded.ClearAll, hue, night, i == ringed, row(i))
                else DeckRow("${n.title} · ${r.time}", r.text, Icons.Rounded.Notifications, hue, night, i == ringed, row(i), bitmap = n.icon, trailing = r.action)
            }
        }
    }

    @Composable
    override fun Library(page: LibraryPage, ringed: Int, art: suspend (LibraryRow, Int) -> android.graphics.Bitmap?, row: (Int) -> Modifier, modifier: Modifier) {
        val night = LocalSkinTokens.current.night
        val hue = Deck.hue(World.MUSIC, night)
        DeckPage(World.MUSIC, page.title, stringResource(R.string.myplay_library_hint), night, modifier) {
            if (page.rows.isEmpty()) {
                DeckNote(if (page.loading) stringResource(R.string.myplay_library_loading) else page.error ?: stringResource(R.string.myplay_library_empty), night)
                return@DeckPage
            }
            // L-1: apps repeat ids; the key stays unique (as ORBIT's).
            DeckList(ringed, page.rows.size, keyOf = { "${it}_${page.rows[it].id}" }) { i ->
                val r = page.rows[i]
                val px = with(LocalDensity.current) { 60.dp.roundToPx() }
                val bitmap by produceState<android.graphics.Bitmap?>(null, r.id) { value = runCatching { art(r, px) }.getOrNull() }
                DeckRow(r.title, r.subtitle, if (r.browsable) Icons.Rounded.Folder else Icons.Rounded.MusicNote, hue, night, i == ringed, row(i),
                    bitmap = bitmap, leadsOn = r.browsable)
            }
        }
    }
}


/** A card of the hand (tests). */
internal fun deckCardTag(w: World) = "deck_card_${w.id}"
internal const val DECK_ART_TAG = "deck_art"
internal const val DECK_PROGRESS_TAG = "deck_progress"
internal const val DECK_BATTERY_TAG = "deck_battery"
internal const val DECK_MUSIC_PROGRESS_TAG = "deck_music_progress"
