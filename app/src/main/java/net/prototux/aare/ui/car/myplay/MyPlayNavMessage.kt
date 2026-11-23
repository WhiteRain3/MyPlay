package net.prototux.aare.ui.car.myplay

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Look C (designs 18, 19): what the hosted maps app's navigation template says in place of a turn:
 * a MessageInfo (arrived: its [title], [text] and [icon]) or the way being worked out
 * ([loading]: RoutingInfo.isLoading, or a MessageInfo with no image). A skin that draws it
 * ([MyPlaySkin.drawsNavMessages]) shows it in the turn's place, so the app's own card is left out.
 */
data class NavMessage(
    val title: String?,
    val text: String?,
    val icon: ImageBitmap?,
    val loading: Boolean,
) {
    /** The turn slot that shows it: the title where the distance goes, the text where the road goes. */
    fun asTurn(): TurnSlot = TurnSlot(title, text, message = this)
}

/**
 * Where the hosted maps app's navigation template hands its message while the skin draws the turn
 * (provided by the engine round the home map, as [LocalLaneSink]); null: the app's own card shows it.
 */
val LocalNavMessageSink = staticCompositionLocalOf<((NavMessage?) -> Unit)?> { null }
