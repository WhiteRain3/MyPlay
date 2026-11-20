package net.prototux.aare.ui.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import net.prototux.aare.R

/** Where a dragged card was let go: on a slot (0 is the first), on the "not shown" tray, or elsewhere. */
sealed interface CardDrop {
    data class Slot(val index: Int) : CardDrop
    data object Tray : CardDrop
    data object Nowhere : CardDrop
}

/**
 * [cards] after [card] is dropped at [drop]. On a slot it goes to that place (the others move up
 * or down; a slot past the end means last); on the tray it is hidden, unless it is the only card
 * shown (one must stay). Anywhere else nothing changes.
 */
fun <T> dropCard(cards: List<T>, card: T, drop: CardDrop): List<T> = when (drop) {
    is CardDrop.Slot -> {
        val without = cards - card
        without.toMutableList().apply { add(drop.index.coerceIn(0, without.size), card) }
    }
    CardDrop.Tray -> if (card in cards && cards.size > 1) cards - card else cards
    CardDrop.Nowhere -> cards
}

/**
 * The cards as places to drag them to (the Design reviewer's "tap a card, drag it to a dashed
 * slot"): one slot per card in order, empty ones dashed, and a tray of the cards not shown. Drag a
 * card onto a slot to put it there, or onto the tray to hide it. The rows with arrows stay the way
 * to do the same without dragging (and with the knob or TalkBack).
 */
@Composable
fun <T> CardSlots(
    all: List<T>,
    shown: List<T>,
    label: @Composable (T) -> String,
    symbol: (T) -> ImageVector,
    tone: (T) -> TileTone,
    onChange: (List<T>) -> Unit,
) {
    val slotRects = remember { mutableStateMapOf<Int, Rect>() }
    var trayRect by remember { mutableStateOf(Rect.Zero) }
    fun dropAt(point: Offset): CardDrop {
        slotRects.entries.firstOrNull { it.value.contains(point) }?.let { return CardDrop.Slot(it.key) }
        return if (trayRect.contains(point)) CardDrop.Tray else CardDrop.Nowhere
    }
    val hidden = all.filter { it !in shown }
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            all.indices.forEach { i ->
                val card = shown.getOrNull(i)
                Box(
                    Modifier.weight(1f).aspectRatio(1.1f)
                        .onGloballyPositioned { slotRects[i] = it.boundsInRoot() }
                        .dashedBorder(card == null),
                    contentAlignment = Alignment.Center,
                ) {
                    if (card == null) Icon(Icons.Rounded.Add, null, tint = PhoneColors.Dim)
                    else DraggableCard(card, label(card), symbol(card), tone(card), Modifier.fillMaxSize()) { at ->
                        onChange(dropCard(shown, card, dropAt(at)))
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).onGloballyPositioned { trayRect = it.boundsInRoot() }
                .clip(RoundedCornerShape(18.dp)).background(PhoneColors.Card).padding(8.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (hidden.isEmpty()) Text(stringResource(R.string.phone_slots_tray_empty), color = PhoneColors.Dim, fontSize = 13.sp, modifier = Modifier.padding(8.dp))
            else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                hidden.forEach { card ->
                    DraggableCard(card, label(card), symbol(card), tone(card), Modifier.size(width = 84.dp, height = 56.dp)) { at ->
                        onChange(dropCard(shown, card, dropAt(at)))
                    }
                }
            }
        }
        HintLine(Icons.Rounded.Add, stringResource(R.string.phone_slots_hint))
    }
}

/** A card that follows the finger while dragged and reports where it was let go (window coordinates). */
@Composable
private fun <T> DraggableCard(card: T, label: String, symbol: ImageVector, tone: TileTone, modifier: Modifier, onDrop: (Offset) -> Unit) {
    var offset by remember(card) { mutableStateOf(Offset.Zero) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    var touch by remember { mutableStateOf(Offset.Zero) }
    val dragging = offset != Offset.Zero
    Column(
        modifier
            .zIndex(if (dragging) 1f else 0f)
            .onGloballyPositioned { origin = it.positionInRoot() }
            .graphicsLayer { translationX = offset.x; translationY = offset.y; if (dragging) { scaleX = 1.08f; scaleY = 1.08f } }
            .clip(RoundedCornerShape(14.dp))
            .background(tone.background)
            .pointerInputDrag(
                onStart = { touch = it },
                onDrag = { offset += it },
                onEnd = { onDrop(origin + touch + offset); offset = Offset.Zero },
                onCancel = { offset = Offset.Zero },
            )
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(symbol, null, tint = tone.content, modifier = Modifier.size(22.dp))
        Text(label, color = tone.content, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2)
    }
}

private fun Modifier.pointerInputDrag(onStart: (Offset) -> Unit, onDrag: (Offset) -> Unit, onEnd: () -> Unit, onCancel: () -> Unit): Modifier =
    this.then(Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = onStart,
            onDragEnd = onEnd,
            onDragCancel = onCancel,
            onDrag = { change, amount -> change.consume(); onDrag(amount) },
        )
    })

/** A dashed outline for an empty slot, a thin solid one for a filled slot. */
private fun Modifier.dashedBorder(dashed: Boolean): Modifier =
    if (!dashed) this.border(1.dp, PhoneColors.Border, RoundedCornerShape(14.dp))
    else drawBehind {
        drawRoundRect(
            PhoneColors.Dim, cornerRadius = CornerRadius(14.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))),
        )
    }
