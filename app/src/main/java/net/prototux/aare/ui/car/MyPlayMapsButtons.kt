package net.prototux.aare.ui.car

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import net.prototux.aare.R

/**
 * MyPlay: the maps buttons on the home screen's map card, when no maps app is open.
 *
 * AARE drew up to four in a row that could not scroll. On a narrow card the row was wider than
 * the card: names broke over several lines and the buttons past the edge were cut off but could
 * still take the knob's focus. Here only the buttons that fit are drawn, each name on one line,
 * and when some are left out a "More" button opens the app list.
 */
@Composable
internal fun MapsButtons(apps: List<CarAppEntry>, onOpen: (CarAppEntry) -> Unit, onMore: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val measurer = rememberTextMeasurer()
        val style = MaterialTheme.typography.labelLarge
        val density = LocalDensity.current
        val more = stringResource(R.string.myplay_maps_more)
        val available = constraints.maxWidth
        val shown = remember(apps, available, style, density, more) {
            fun textWidth(text: String) = measurer.measure(text, style, maxLines = 1).size.width
            with(density) {
                mapsButtonsThatFit(
                    widths = apps.map { APP_CHIP_EXTRA.roundToPx() + textWidth(it.label) },
                    moreWidth = MORE_CHIP_PADDING.roundToPx() * 2 + textWidth(more),
                    spacing = MAPS_BUTTON_GAP.roundToPx(),
                    available = available,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(MAPS_BUTTON_GAP), verticalAlignment = Alignment.CenterVertically) {
            apps.take(shown).forEachIndexed { i, app ->
                // The last one may shrink (its name ends in "…") when even one button is too wide.
                AppChip(app, if (i == shown - 1) Modifier.weight(1f, fill = false) else Modifier) { onOpen(app) }
            }
            if (shown < apps.size) {
                Row(
                    Modifier.height(64.dp).clip(MaterialTheme.shapes.extraLarge).background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .carFocusHighlight(MaterialTheme.shapes.extraLarge).clickable(onClick = onMore).padding(horizontal = MORE_CHIP_PADDING),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(more, style = style, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

/** A maps button beyond its name: 10 dp, the 44 dp icon, 12 dp, then 20 dp after the name (see AppChip). */
private val APP_CHIP_EXTRA = 86.dp
private val MORE_CHIP_PADDING = 22.dp
private val MAPS_BUTTON_GAP = 16.dp

/**
 * How many of the buttons (their [widths], in order) to draw in a row [available] wide with
 * [spacing] between buttons. All of them when they fit. Otherwise as many as fit together with
 * the "More" button ([moreWidth]), and never fewer than one, which is then drawn narrower.
 */
internal fun mapsButtonsThatFit(widths: List<Int>, moreWidth: Int, spacing: Int, available: Int): Int {
    if (widths.isEmpty()) return 0
    if (widths.sum() + spacing * (widths.size - 1) <= available) return widths.size
    if (widths.size == 1) return 1
    var used = moreWidth
    var count = 0
    for (w in widths) {
        if (used + spacing + w > available) break
        used += spacing + w
        count++
    }
    return count.coerceIn(1, widths.size - 1)
}
