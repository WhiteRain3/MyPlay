package net.prototux.aare.ui.car

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
data class GestureLine(
    val action: ButtonAction,
    val forward: Boolean,
    val atMs: Long,
    /** R6-1: the song and whether it played when the gesture came, to tell a report of its effect from an old one. */
    val titleBefore: String? = null,
    val playingBefore: Boolean? = null,
    /** MyPlay (drive warnings): a line of its own words instead of a gesture's ([action] is then NOTHING). */
    val text: String? = null,
    /** How long the line stays. */
    val showMs: Long = GestureLines.SHOW_MS,
) {
    companion object {
        /** A warning's line (ice, a hot phone): its own words, a little longer than a gesture's. */
        fun info(text: String, atMs: Long) = GestureLine(ButtonAction.NOTHING, forward = true, atMs = atMs, text = text, showMs = GestureLines.INFO_MS)
    }
}

internal object GestureLines {
    /** How long the line stays. */
    const val SHOW_MS = 1_500L

    /** How long a warning's own line stays (GestureLine.info). */
    const val INFO_MS = 4_000L

    /** Whether [action] gets a line: only what happens away from the ring. */
    fun shows(action: ButtonAction): Boolean = when (action) {
        ButtonAction.PLAY_PAUSE, ButtonAction.NEXT, ButtonAction.PREVIOUS, ButtonAction.SKIP_SONGS, ButtonAction.ASSISTANT -> true
        else -> false
    }

    /** Which words go first: next or previous song, playing or paused, or the assistant. */
    enum class Words { NEXT, PREVIOUS, PLAYING, PAUSED, ASSISTANT }

    /**
     * R6-1: playing or not as the line should say it: the app's report once it differs from before
     * the gesture, until then what the toggle will do.
     */
    fun playingNow(line: GestureLine, reported: Boolean?): Boolean? =
        if (line.playingBefore != null && reported == line.playingBefore) !line.playingBefore else reported

    /** R6-1: the title to show: for a song change only once the app reports a different song. */
    fun title(line: GestureLine, reported: String?): String? = reported?.takeIf { it.isNotBlank() }?.takeIf {
        when (line.action) {
            ButtonAction.NEXT, ButtonAction.PREVIOUS, ButtonAction.SKIP_SONGS -> it != line.titleBefore
            else -> true
        }
    }

    fun words(line: GestureLine, playing: Boolean?): Words? = when (line.action) {
        ButtonAction.NEXT -> Words.NEXT
        ButtonAction.PREVIOUS -> Words.PREVIOUS
        ButtonAction.SKIP_SONGS -> if (line.forward) Words.NEXT else Words.PREVIOUS
        ButtonAction.PLAY_PAUSE -> if (playingNow(line, playing) == false) Words.PAUSED else Words.PLAYING
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
    // R6-2: the line is cleared when its time is up, so a screen drawn again later never shows an old one.
    LaunchedEffect(line) {
        delay(line.showMs)
        if (state.gestureLine === line) state.gestureLine = null
    }
    // R6-3: a ringing call's card takes the top of the screen; the line waits.
    if (state.call?.ringing == true) return
    val text = line.text ?: run {
        val np = state.nowPlaying
        val words = GestureLines.words(line, np?.playing) ?: return
        val first = stringResource(when (words) {
            GestureLines.Words.NEXT -> R.string.myplay_gesture_line_next
            GestureLines.Words.PREVIOUS -> R.string.myplay_gesture_line_previous
            GestureLines.Words.PLAYING -> R.string.myplay_gesture_line_playing
            GestureLines.Words.PAUSED -> R.string.myplay_gesture_line_paused
            GestureLines.Words.ASSISTANT -> R.string.myplay_gesture_line_assistant
        })
        val title = GestureLines.title(line, np?.title)
        if (GestureLines.withTitle(words) && title != null) "$first: $title" else first
    }
    Text(
        text, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(16.dp).clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(horizontal = 20.dp, vertical = 10.dp)
            .testTag(GESTURE_LINE_TAG),
    )
}

/** R6-3: the notice and the gesture line at the top, one under the other, never on top of each other. */
@Composable
internal fun TopLines(state: CarUiState, actions: CarActions, modifier: Modifier) {
    androidx.compose.foundation.layout.Column(modifier, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        NoticeOverlay(state, actions, Modifier)
        GestureLineOverlay(state, Modifier)
    }
}
