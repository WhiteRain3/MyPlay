package net.prototux.aare.ui.car.myplay.deck

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.prototux.aare.ui.car.myplay.World
import net.prototux.aare.ui.car.myplay.orbit.OrbitSkin
import net.prototux.aare.ui.car.myplay.orbit.orbitFocusRounded

/**
 * MyPlay (plan N14): DECK's pages. A world opened is its card scaled up to fill the screen (the
 * sheet's "App list: the Apps card opened full screen"): the card in the world's tint with its
 * thick coloured top edge, a caps label in the world's hue, and its rows or tiles. The map stays
 * behind, dimmed, round the card.
 */

/** Draws [content] at DECK's scale for this screen (as ORBIT's: the sheet is 1280x720). */
@Composable
internal fun DeckScaled(content: @Composable () -> Unit) {
    val c = LocalConfiguration.current
    val d = LocalDensity.current
    val s = OrbitSkin.scale(c.screenWidthDp, c.screenHeightDp)
    CompositionLocalProvider(LocalDensity provides Density(d.density * s, d.fontScale), content = content)
}

/** The page's card: [world]'s tint and edge, its caps [label], a [hint] line, then [body]. */
@Composable
internal fun DeckPage(world: World, label: String, hint: String?, night: Boolean, modifier: Modifier, body: @Composable ColumnScope.() -> Unit) {
    val p = Deck.paper(night)
    val hue = Deck.hue(world, night)
    val shape = RoundedCornerShape(Deck.CORNER)
    Box(modifier.background(p.halo.copy(alpha = 0.6f))) {
        DeckScaled {
            Box(
                Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 28.dp)
                    .shadow(16.dp, shape).clip(shape).background(Deck.cardColour(world, night)),
            ) {
                Box(Modifier.fillMaxWidth().height(Deck.EDGE).background(hue))
                Column(Modifier.fillMaxSize().padding(start = 36.dp, end = 36.dp, top = 30.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Deck.icon(world), null, Modifier.size(32.dp), tint = hue)
                        Spacer(Modifier.width(12.dp))
                        Text(label.uppercase(), style = Deck.Type.label.copy(color = hue), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    hint?.let { Text(it, style = TextStyle(fontSize = 22.sp, color = p.secondary), modifier = Modifier.padding(top = 4.dp)) }
                    body()
                }
            }
        }
    }
}

/** A page's list: the ringed row kept in view with one row of context above it. */
@Composable
internal fun DeckList(ringed: Int, count: Int, keyOf: ((Int) -> Any)? = null, row: @Composable (Int) -> Unit) {
    val list: LazyListState = rememberLazyListState()
    LaunchedEffect(ringed, count) { if (count > 0) list.animateScrollToItem((ringed - 1).coerceAtLeast(0)) }
    LazyColumn(
        state = list, modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        // Room round the rows for the focus outline (it is drawn outside the row).
        verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 24.dp),
    ) {
        items(count, key = keyOf) { i -> row(i) }
    }
}

/** A line under the page's label in place of the rows (nothing yet, no permission, not connected). */
@Composable
internal fun DeckNote(text: String, night: Boolean) =
    Text(text, style = Deck.Type.secondary.copy(color = Deck.paper(night).secondary), modifier = Modifier.padding(top = 36.dp))

/**
 * DECK's list row: a flat slab on the card with its icon (or [bitmap]) in [hue] on the left, the
 * title and [subtitle], a value on the right ([trailing]) and a chevron when it leads on. The
 * ringed one is tinted and outlined in DECK's teal (the sheet's heavy outline).
 */
@Composable
internal fun DeckRow(
    title: String, subtitle: String, icon: ImageVector, hue: Color, night: Boolean, on: Boolean, modifier: Modifier,
    iconTint: Color? = null, bitmap: Bitmap? = null, trailing: String? = null, leadsOn: Boolean = false,
) {
    val p = Deck.paper(night)
    val shape = RoundedCornerShape(10.dp)
    val fill = if (on) hue.copy(alpha = 0.22f).compositeOver(p.card) else p.card.copy(alpha = 0.55f).compositeOver(Deck.cardColour(World.NAVIGATE, night))
    Row(
        modifier.fillMaxWidth().height(92.dp)
            .orbitFocusRounded(on, Deck.hue(World.NAVIGATE, night), p.halo, 10.dp)
            .clip(shape).background(fill).padding(start = 18.dp, end = 22.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(hue.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            if (bitmap != null) {
                val image = remember(bitmap) { bitmap.asImageBitmap() }
                Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(icon, null, Modifier.size(34.dp), tint = iconTint ?: hue)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, color = p.text), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) Text(subtitle, style = TextStyle(fontSize = 22.sp, color = p.secondary), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing?.let { Text(it, style = TextStyle(fontSize = 25.sp, fontWeight = FontWeight.Bold, color = if (on) p.text else hue), maxLines = 1) }
        if (leadsOn) Icon(Icons.Rounded.ChevronRight, null, Modifier.size(34.dp), tint = p.secondary)
    }
}
