package net.prototux.aare.ui.car.myplay

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap

/**
 * MyPlay (AL-1, the owner's photo: Waze's alert card covered ORBIT's turn): an alert from the maps
 * app on the home (a car-library Alert: "In 380 m · Reduced speed · Close"), handed to the engine
 * so the skin draws it in its own style and place instead of the default card. [actions] run the
 * app's action and dismiss the alert; [key] tells one alert from the next.
 */
data class MapAlert(
    val key: Int,
    val title: String,
    val subtitle: String?,
    val icon: ImageBitmap?,
    val actions: List<Pair<String, () -> Unit>>,
)

/**
 * Where the home's map hands its app's alert (null: none now). Provided by the engine round the
 * home map while the skin draws alerts itself ([MyPlaySkin.drawsMapAlerts]); unset, the map draws
 * its own default card as before.
 */
val LocalMapAlertSink = staticCompositionLocalOf<((MapAlert?) -> Unit)?> { null }

/** A map alert's action button (its test tag and home stop). */
fun mapAlertStop(i: Int) = "map_alert_action_$i"
