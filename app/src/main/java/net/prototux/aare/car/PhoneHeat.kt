package net.prototux.aare.car

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

/**
 * MyPlay (drive warnings, ideas 2026-11-22): the phone's own heat. Projecting wirelessly, making
 * the car's video and running Waze together warm a phone, and a hot phone slows itself down, which
 * shows on the car screen as stutter that looks like a MyPlay fault. So every change of the phone's
 * heat level goes in the connection log, and the first time in a drive it is hot, a short line on
 * the car screen says so. Nothing is changed automatically. Android 10 and newer; no permission.
 */
internal object PhoneHeatRule {
    /** From this level on (Android's "severe"), the phone is slowing itself down. */
    const val HOT = PowerManager.THERMAL_STATUS_SEVERE

    fun hot(status: Int) = status >= HOT

    /** The level in the log's words. */
    fun name(status: Int): String = when (status) {
        PowerManager.THERMAL_STATUS_NONE -> "normal"
        PowerManager.THERMAL_STATUS_LIGHT -> "light"
        PowerManager.THERMAL_STATUS_MODERATE -> "moderate"
        PowerManager.THERMAL_STATUS_SEVERE -> "severe (slowing down)"
        PowerManager.THERMAL_STATUS_CRITICAL -> "critical"
        PowerManager.THERMAL_STATUS_EMERGENCY -> "emergency"
        PowerManager.THERMAL_STATUS_SHUTDOWN -> "shutting down"
        else -> "unknown ($status)"
    }
}

/** Per drive: whether the line was already shown. Main thread only. */
internal class PhoneHeatTracker {
    private var announced = false
    private var last: Int? = null

    /** A new level: whether it is new (for the log) and whether the line should show now. */
    fun update(status: Int): Pair<Boolean, Boolean> {
        val changed = status != last
        last = status
        val announce = PhoneHeatRule.hot(status) && !announced
        if (announce) announced = true
        return changed to announce
    }
}

/** Listens to the phone's heat level on the main thread between [start] and [stop]. */
internal class PhoneHeatWatch(context: Context, private val onLevel: (Int) -> Unit) {
    private val app = context.applicationContext
    private var listener: Any? = null

    fun start() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || listener != null) return
        listener = runCatching { startQ() }.getOrNull()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun startQ(): PowerManager.OnThermalStatusChangedListener? {
        val pm = app.getSystemService(PowerManager::class.java) ?: return null
        // Android calls it once at once with the current level, then on each change.
        val l = PowerManager.OnThermalStatusChangedListener { onLevel(it) }
        pm.addThermalStatusListener(ContextCompat.getMainExecutor(app), l)
        return l
    }

    fun stop() {
        val l = listener ?: return
        listener = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) runCatching {
            app.getSystemService(PowerManager::class.java)?.removeThermalStatusListener(l as PowerManager.OnThermalStatusChangedListener)
        }
    }
}
