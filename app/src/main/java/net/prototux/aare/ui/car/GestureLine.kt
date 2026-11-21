package net.prototux.aare.ui.car

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import net.prototux.aare.R
import net.prototux.aare.car.ButtonAction

/**
 * MyPlay (plan R6): a short line at the top of the car screen after a button gesture that does
 * something away from the ring (the next song, play or pause, the assistant), so the driver knows
 * it worked without looking for it. No sound; the setting myplay_gesture_line turns it off.
 *
 * The song's title is read as the line is drawn, so after "Next song" it shows the new song once
 * the music app reports it.
 */
data class GestureLine(val action: ButtonAction, val forward: Boolean, val atMs: Long)

internal object GestureLines {
    /** How long the line stays. */
    const val SHOW_MS = 1_500L

    /** Whether [action] gets a line: only what happens away from the ring. */
    fun shows(action: ButtonAction): Boolean = when (action) {
        ButtonAction.PLAY_PAUSE, ButtonAction.NEXT, ButtonAction.PREVIOUS, ButtonAction.SKIP_SONGS, ButtonAction.ASSISTANT -> true
        else -> false
    }

    /** Which words go first: next or previous song, playing or paused, or the assistant. */
    enum class Words { NEXT, PREVIOUS, PLAYING, PAUSED, ASSISTANT }

    fun words(line: GestureLine, playing: Boolean?): Words? = when (line.action) {
        ButtonAction.NEXT -> Words.NEXT
        ButtonAction.PREVIOUS -> Words.PREVIOUS
        ButtonAction.SKIP_SONGS -> if (line.forward) Words.NEXT else Words.PREVIOUS
        ButtonAction.PLAY_PAUSE -> if (playing == false) Words.PAUSED else Words.PLAYING
        ButtonAction.ASSISTANT -> Words.ASSISTANT
        else -> null
    }

    /** Whether the song's title follows the words. */
    fun withTitle(words: Words) = words != Words.ASSISTANT
}

/** The test tag of the line, for tests. */
internal const val GESTURE_LINE_TAG = "gesture_line"

@Composable
internal fun GestureLineOverlay(state: CarUiState, modifier: Modifier) {
    val line = state.gestureLine ?: return
    var shown by remember(line) { mutableStateOf(true) }
    LaunchedEffect(line) {
        delay(GestureLines.SHOW_MS)
        shown = false
    }
    if (!shown) return
    val np = state.nowPlaying
    val words = GestureLines.words(line, np?.playing) ?: return
    val first = stringResource(when (words) {
        GestureLines.Words.NEXT -> R.string.myplay_gesture_line_next
        GestureLines.Words.PREVIOUS -> R.string.myplay_gesture_line_previous
        GestureLines.Words.PLAYING -> R.string.myplay_gesture_line_playing
        GestureLines.Words.PAUSED -> R.string.myplay_gesture_line_paused
        GestureLines.Words.ASSISTANT -> R.string.myplay_gesture_line_assistant
    })
    val text = if (GestureLines.withTitle(words) && !np?.title.isNullOrBlank()) "$first: ${np!!.title}" else first
    Text(
        text, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(16.dp).clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(horizontal = 20.dp, vertical = 10.dp)
            .testTag(GESTURE_LINE_TAG),
    )
}
