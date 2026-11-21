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
    // N14: DECK's pages (DeckPages.kt). Each world's card opened to fill the screen.

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
