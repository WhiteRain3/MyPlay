package net.prototux.aare.ui.car

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.prototux.aare.R

/** The phone's network and battery, as its status bar would show them. */
data class PhoneStatusInfo(
    /** Mobile signal, 0 to 4 bars; null without a SIM or service. */
    val signal: Int?,
    /** The phone's internet goes through Wi-Fi (not the car's own network, which has none). */
    val wifi: Boolean,
    val battery: Int,
    val charging: Boolean,
) {
    companion object {
        fun read(context: Context): PhoneStatusInfo {
            val bat = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = bat?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = bat?.getIntExtra(BatteryManager.EXTRA_SCALE, 100)?.takeIf { it > 0 } ?: 100
            val plugged = (bat?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
            val cm = context.getSystemService(ConnectivityManager::class.java)
            val caps = runCatching { cm.getNetworkCapabilities(cm.activeNetwork) }.getOrNull()
            val tm = context.getSystemService(TelephonyManager::class.java)
            // TelephonyManager.signalStrength is Android 9+: no bars before.
            val signal = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) runCatching { tm.signalStrength?.level }.getOrNull() else null)
                ?.takeIf { tm.simState == TelephonyManager.SIM_STATE_READY }
            return PhoneStatusInfo(
                signal = signal,
                wifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true,
                battery = if (level >= 0) level * 100 / scale else 0,
                charging = plugged,
            )
        }
    }
}

/**
 * Polls the phone's network and battery status every 15 s, for [PhoneStatusIcons]. The rail calls
 * it once and hands the result to both its battery widget and [NetworkStatusIcons], so one rail
 * runs one poll.
 */
@Composable
internal fun rememberPhoneStatus(): PhoneStatusInfo? {
    val context = LocalContext.current
    var status by remember { mutableStateOf<PhoneStatusInfo?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            status = withContext(Dispatchers.Default) { runCatching { PhoneStatusInfo.read(context) }.getOrNull() }
            delay(15_000)
        }
    }
    return status
}

/** Signal bars, Wi-Fi and battery, for the rail under the clock. */
@Composable
fun PhoneStatusIcons() {
    val s = rememberPhoneStatus() ?: return
    Row(Modifier.carPart(CarPart.Status), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        NetworkIcons(s)
        BatteryIcons(s)
    }
}

/** Signal bars and Wi-Fi only (no battery): the rail's network status, shown next to its battery widget. */
@Composable
internal fun NetworkStatusIcons(status: PhoneStatusInfo?) {
    val s = status ?: return
    if (!LocalStatusStyle.current.signal) return
    Row(Modifier.carPart(CarPart.Status), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        NetworkIcons(s)
    }
}

/** MyPlay (F4): the status icons' colour: the style's, else the theme's. */
@Composable
private fun statusColor(): Color = LocalStatusStyle.current.color?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface

/** Wi-Fi and signal, if the style shows them (F4). */
@Composable
private fun NetworkIcons(s: PhoneStatusInfo) {
    val look = LocalStatusStyle.current
    if (!look.signal) return
    val fg = statusColor()
    val dim = fg.copy(alpha = 0.25f)
    if (s.wifi) Icon(Icons.Rounded.Wifi, stringResource(R.string.phone_status_wifi), tint = fg, modifier = Modifier.size(16.dp * look.size.scale))
    SignalBars(s.signal, fg, dim, look.size.scale)
}

/** The battery as the style says (F4): gauge, number with or without %, charging bolt, colours. */
@Composable
internal fun BatteryIcons(s: PhoneStatusInfo) {
    val look = LocalStatusStyle.current
    val fg = statusColor()
    val dim = fg.copy(alpha = 0.25f)
    val fill = when (look.batteryLevel(s.battery, s.charging)) {
        StatusStyle.BatteryLevel.LOW -> MaterialTheme.colorScheme.error
        StatusStyle.BatteryLevel.MEDIUM -> BATTERY_AMBER
        StatusStyle.BatteryLevel.HIGH -> BATTERY_GREEN
        StatusStyle.BatteryLevel.NORMAL -> fg
    }
    if (look.batteryIcon) BatteryGauge(s.battery, s.charging, fill, dim, look.size.scale)
    if (s.charging) Icon(Icons.Rounded.Bolt, stringResource(R.string.phone_status_charging), tint = fg, modifier = Modifier.size(14.dp * look.size.scale))
    else look.batteryText(s.battery)?.let { text ->
        val base = MaterialTheme.typography.labelSmall
        Text(text, style = base.copy(fontSize = base.fontSize * look.size.scale), color = fg, maxLines = 1)
    }
}

private val BATTERY_AMBER = Color(0xFFFFB300)
private val BATTERY_GREEN = Color(0xFF43A047)

@Composable
internal fun SignalBars(level: Int?, fg: Color, dim: Color, scale: Float = 1f) {
    Canvas(Modifier.size(16.dp * scale)) {
        val bar = size.width / 5.5f
        for (i in 0 until 4) {
            val h = size.height * (i + 1) / 4
            drawRoundRect(
                color = if (level != null && i < level) fg else dim,
                topLeft = Offset(i * bar * 1.45f, size.height - h),
                size = Size(bar, h),
                cornerRadius = CornerRadius(bar / 3),
            )
        }
    }
}

@Composable
/** [fill] is the level's colour (MyPlay F4: chosen by BatteryIcons from the status style). */
internal fun BatteryGauge(percent: Int, charging: Boolean, fill: Color, dim: Color, scale: Float = 1f) {
    Canvas(Modifier.size(width = 22.dp * scale, height = 12.dp * scale)) {
        val tip = size.width * 0.08f
        val body = Size(size.width - tip, size.height)
        val stroke = 1.5.dp.toPx()
        drawRoundRect(dim, size = body, cornerRadius = CornerRadius(3.dp.toPx()), style = Stroke(stroke))
        drawRect(dim, topLeft = Offset(body.width, size.height * 0.3f), size = Size(tip, size.height * 0.4f))
        val inset = stroke * 1.5f
        drawRoundRect(fill, topLeft = Offset(inset, inset),
            size = Size((body.width - 2 * inset) * percent.coerceIn(0, 100) / 100f, body.height - 2 * inset),
            cornerRadius = CornerRadius(1.5.dp.toPx()))
    }
}
