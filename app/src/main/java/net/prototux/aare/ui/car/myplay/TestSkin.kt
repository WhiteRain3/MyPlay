package net.prototux.aare.ui.car.myplay

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.prototux.aare.car.SpeedReading
import net.prototux.aare.ui.car.CarAppEntry
import net.prototux.aare.ui.car.CarCall
import net.prototux.aare.ui.car.CarNotice
import net.prototux.aare.media.LibraryRow
import android.graphics.Bitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import net.prototux.aare.R
import net.prototux.aare.car.LimitShown

/**
 * MyPlay (N1): a plain skin for the engine's tests and the hidden preview: grey boxes with text,
 * the chosen one outlined. It shows that every slot is placed and filled, nothing more. Not a
 * design language; ORBIT (N4, N5) is the first real one.
 */
object TestSkin : MyPlaySkin {
    override val id = "test"

    override fun tokens(night: Boolean) = if (night) {
        SkinTokens(Color(0xFF101214), Color(0xE0262A2E), Color(0xFFEDEFF1), Color(0xFFA0A6AD), Color(0xFF4FC3F7), Color(0xFF00222E), corner = 12.dp)
    } else {
        SkinTokens(Color(0xFFF3F4F6), Color(0xE0FFFFFF), Color(0xFF15181B), Color(0xFF5A6169), Color(0xFF0277BD), Color.White, corner = 12.dp, night = false)
    }

    @Composable
    private fun Box(modifier: Modifier, content: @Composable () -> Unit) {
        val t = LocalSkinTokens.current
        Column(modifier.clip(RoundedCornerShape(t.corner)).background(t.surface).padding(12.dp)) { content() }
    }

    @Composable
    override fun Turn(turn: TurnSlot, modifier: Modifier) = Box(modifier) {
        val t = LocalSkinTokens.current
        turn.distance?.let { Text(it, color = t.onSurface, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
        turn.road?.let { Text(it, color = t.dim, fontSize = 18.sp, maxLines = 2) }
    }

    @Composable
    override fun Hub(trip: TripSlot?, modifier: Modifier) = Box(modifier) {
        val t = LocalSkinTokens.current
        Text("MENU", color = t.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        trip?.arrival?.let { Text(it, color = t.accent, fontSize = 24.sp) }
        trip?.distanceLeft?.let { Text(it, color = t.dim, fontSize = 16.sp) }
    }

    @Composable
    override fun Song(song: SongSlot, modifier: Modifier) = Box(modifier) {
        val t = LocalSkinTokens.current
        Text(song.title, color = t.onSurface, fontSize = 20.sp, maxLines = 1)
        Text(song.subtitle, color = t.dim, fontSize = 16.sp, maxLines = 1)
    }

    @Composable
    override fun Status(modifier: Modifier) = Box(modifier) {
        val t = LocalSkinTokens.current
        val context = LocalContext.current
        Text(DateFormat.getTimeFormat(context).format(System.currentTimeMillis()), color = t.onSurface, fontSize = 22.sp)
    }

    override val showsSpeed get() = true

    @Composable
    override fun Speed(reading: SpeedReading?, modifier: Modifier) = Column(modifier) {
        Text("${reading?.kmh ?: "–"} km/h", color = LocalSkinTokens.current.onSurface, fontSize = 22.sp,
            modifier = if (reading?.over == true) Modifier.testTag(SPEED_OVER_TAG) else Modifier)
        when (val l = reading?.limit) {
            is LimitShown.Known -> Text("limit ${l.kmh} · ${stringResource(R.string.myplay_speed_limit_map_data)}", color = LocalSkinTokens.current.onSurface)
            LimitShown.Unknown -> Text(stringResource(R.string.myplay_speed_limit_unknown), color = LocalSkinTokens.current.onSurface)
            null -> Unit
        }
    }

    @Composable
    override fun AppsWorld(apps: List<CarAppEntry>, ringed: Int, page: Int, pages: Int, tile: (Int) -> Modifier, modifier: Modifier) {
        val t = LocalSkinTokens.current
        Column(modifier.background(t.background.copy(alpha = 0.8f)).padding(24.dp)) {
            Text("APPS ${page + 1}/$pages", color = t.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            apps.chunked(5).forEachIndexed { row, chunk ->
                Row {
                    chunk.forEachIndexed { col, app ->
                        val i = row * 5 + col
                        Text(app.label, color = if (i == ringed) t.accent else t.onSurface, fontSize = 18.sp,
                            modifier = tile(i).padding(12.dp))
                    }
                }
            }
        }
    }

    @Composable
    override fun MusicWorld(page: MusicPage, ringed: String, stop: (String) -> Modifier, modifier: Modifier) {
        val t = LocalSkinTokens.current
        @Composable
        fun item(id: String, label: String) =
            Text(label, color = if (id == ringed) t.accent else t.onSurface, fontSize = 18.sp, modifier = stop(id).padding(12.dp))
        Column(modifier.background(t.background.copy(alpha = 0.8f)).padding(24.dp)) {
            val song = page.song
            if (song != null) {
                item(MUSIC_ART, song.title)
                Row {
                    item(MUSIC_PREV, "PREV")
                    item(MUSIC_PLAY, if (song.playing) "PAUSE" else "PLAY")
                    item(MUSIC_NEXT, "NEXT")
                    item(MUSIC_LIBRARY, "LIBRARY")
                }
            } else {
                Text("NOTHING PLAYING", color = t.dim, fontSize = 18.sp)
                item(MUSIC_LIBRARY, "LIBRARY")
            }
            for (u in page.transport.upNext) item(upNextStop(u.queueId), u.title)
        }
    }

    override val drawsMapAlerts get() = true

    @Composable
    override fun MapAlertCard(alert: MapAlert, ringed: String?, stop: (String) -> Modifier, modifier: Modifier) {
        val t = LocalSkinTokens.current
        Row(modifier.background(t.background.copy(alpha = 0.8f)).padding(12.dp)) {
            Text(alert.title + (alert.subtitle?.let { " · $it" } ?: ""), color = t.onSurface, fontSize = 18.sp, modifier = Modifier.padding(12.dp))
            alert.actions.forEachIndexed { i, (label, _) ->
                val id = mapAlertStop(i)
                Text(label, color = if (id == ringed) t.accent else t.onSurface, fontSize = 18.sp, modifier = stop(id).padding(12.dp))
            }
        }
    }

    @Composable
    override fun MusicPanel(page: MusicPage, ringed: String, stop: (String) -> Modifier, modifier: Modifier) {
        val t = LocalSkinTokens.current
        @Composable
        fun item(id: String, label: String) =
            Text(label, color = if (id == ringed) t.accent else t.onSurface, fontSize = 18.sp, modifier = stop(id).padding(12.dp))
        Column(modifier.background(t.background.copy(alpha = 0.8f)).padding(12.dp)) {
            Row {
                item(PANEL_APP, "APP")
                item(PANEL_PREV, "PREV")
                item(PANEL_PLAY, if (page.song?.playing == true) "PAUSE" else "PLAY")
                item(PANEL_NEXT, "NEXT")
            }
            if (page.transport.canSeek) item(PANEL_SEEK, "SEEK")
        }
    }

    @Composable
    override fun Library(page: LibraryPage, ringed: Int, art: suspend (LibraryRow, Int) -> Bitmap?, row: (Int) -> Modifier, modifier: Modifier) {
        val t = LocalSkinTokens.current
        Column(modifier.background(t.background.copy(alpha = 0.8f)).padding(24.dp)) {
            Text(page.title, color = t.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (page.rows.isEmpty()) Text(if (page.loading) "LOADING" else page.error ?: "EMPTY", color = t.dim, fontSize = 18.sp)
            page.rows.forEachIndexed { i, r ->
                Text(r.title + if (r.browsable) " >" else "", color = if (i == ringed) t.accent else t.onSurface, fontSize = 18.sp,
                    modifier = row(i).padding(8.dp))
            }
        }
    }

    @Composable
    override fun Ringing(call: CarCall, canEnd: Boolean, ringed: String, stop: (String) -> Modifier, modifier: Modifier) {
        val t = LocalSkinTokens.current
        Column(modifier.background(t.background.copy(alpha = 0.9f)).padding(24.dp)) {
            Text("CALL ${call.name}", color = t.onSurface, fontSize = 20.sp)
            Row {
                Text("ANSWER", color = if (ringed == CALL_ANSWER) t.accent else t.onSurface, modifier = stop(CALL_ANSWER).padding(12.dp))
                if (canEnd) Text("DECLINE", color = if (ringed == CALL_DECLINE) t.accent else t.onSurface, modifier = stop(CALL_DECLINE).padding(12.dp))
            }
        }
    }

    @Composable
    override fun InCall(call: CarCall, canEnd: Boolean, ringed: String?, stop: (String) -> Modifier, modifier: Modifier) {
        val t = LocalSkinTokens.current
        Row(modifier.background(t.surface)) {
            Text("IN CALL ${call.name}", color = t.onSurface, modifier = stop(STOP_CALL).padding(8.dp))
            if (canEnd) Text("HANG UP", color = t.onSurface, modifier = stop(STOP_HANG_UP).padding(8.dp))
        }
    }

    @Composable
    override fun PhoneWorld(page: PhonePage, ringed: Int, row: (Int) -> Modifier, modifier: Modifier) {
        val t = LocalSkinTokens.current
        Column(modifier.background(t.background.copy(alpha = 0.8f)).padding(24.dp)) {
            Text(if (page.permitted) "CALLS" else "NO PERMISSION", color = t.onSurface, fontSize = 20.sp)
            page.rows.forEachIndexed { i, r ->
                Text(r.title, color = if (i == ringed) t.accent else t.onSurface, fontSize = 18.sp, modifier = row(i).padding(8.dp))
            }
        }
    }

    @Composable
    override fun Notice(notice: CarNotice, hideText: Boolean, ringed: String?, stop: (String) -> Modifier, modifier: Modifier) {
        val t = LocalSkinTokens.current
        Row(modifier.background(t.surface)) {
            Text(notice.title + if (hideText) "" else ": " + notice.text, color = t.onSurface, modifier = Modifier.padding(8.dp))
            notice.actions.take(2).forEachIndexed { i, (label, _) -> Text(label, color = t.accent, modifier = stop(noticeActionStop(i)).padding(8.dp)) }
            Text("X", color = t.onSurface, modifier = stop(NOTICE_DISMISS).padding(8.dp))
        }
    }

    @Composable
    override fun Bell(count: Int, ringed: Boolean, modifier: Modifier) {
        Text("BELL $count", color = if (ringed) LocalSkinTokens.current.accent else LocalSkinTokens.current.onSurface, modifier = modifier.padding(8.dp))
    }

    @Composable
    override fun NoticeList(page: NoticeListPage, ringed: Int, row: (Int) -> Modifier, modifier: Modifier) {
        val t = LocalSkinTokens.current
        Column(modifier.background(t.background.copy(alpha = 0.9f)).padding(24.dp)) {
            page.rows.forEachIndexed { i, r ->
                val n = r.notice
                Text(if (n == null) "CLEAR ALL" else "${n.title}: ${r.text}" + (r.action?.let { " [$it]" } ?: ""),
                    color = if (i == ringed) t.accent else t.onSurface, fontSize = 18.sp, modifier = row(i).padding(8.dp))
            }
        }
    }

    @Composable
    override fun CarWorld(lines: List<Pair<String, String>>, ringed: Int, row: (Int) -> Modifier, modifier: Modifier) {
        val t = LocalSkinTokens.current
        Column(modifier.background(t.background.copy(alpha = 0.9f)).padding(24.dp)) {
            if (lines.isEmpty()) Text("NO CAR", color = t.dim)
            lines.forEachIndexed { i, (label, value) ->
                Text("$label: $value", color = if (i == ringed) t.accent else t.onSurface, fontSize = 18.sp, modifier = row(i).padding(8.dp))
            }
        }
    }

    @Composable
    override fun SettingsWorld(page: SettingsPage, ringed: Int, row: (Int) -> Modifier, modifier: Modifier) {
        val t = LocalSkinTokens.current
        Column(modifier.background(t.background.copy(alpha = 0.9f)).padding(24.dp)) {
            page.rows.forEachIndexed { i, r ->
                Text("${r.title}: ${r.value}" + (r.note?.let { " ($it)" } ?: ""), color = if (i == ringed) t.accent else t.onSurface, fontSize = 18.sp, modifier = row(i).padding(8.dp))
            }
        }
    }

    @Composable
    override fun Chooser(selected: World, modifier: Modifier) = Box(modifier) {
        val t = LocalSkinTokens.current
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (w in World.entries) {
                val on = w == selected
                Text(
                    w.name, color = if (on) t.onAccent else t.onSurface, fontSize = 18.sp,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (on) t.accent else Color.Transparent)
                        .border(1.dp, t.dim, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }

    @Composable
    override fun WorldPlaceholder(world: World, modifier: Modifier) = Box(modifier) {
        val t = LocalSkinTokens.current
        Text(world.name, color = t.onSurface, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

/** N20: on the speed while it is over the limit by more than the margin (tests). */
internal const val SPEED_OVER_TAG = "speed_over"
