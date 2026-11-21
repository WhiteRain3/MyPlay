package net.prototux.aare.car

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import net.prototux.aare.Settings
import kotlin.math.roundToInt

/**
 * MyPlay (plan phase N, N3): the speed the car screen's skins show.
 *
 * The car's own speed when the box sends it ([CarData.State.speed]). Otherwise the phone's
 * location, under the owner's condition of no extra battery drain:
 *  - only while a speed is on the screen (a skin's speed slot is drawn);
 *  - only while a maps app is running and the car is connected;
 *  - only the fixes the phone already makes for others (the passive provider: Waze's own GPS
 *    fixes), never a request of MyPlay's own;
 *  - only with the location permission already given (foreground, never background);
 *  - stopped the moment any of these ends.
 *
 * [SpeedRules] is the pure part; [SpeedFeed] starts and stops the phone's fixes ([PhoneFixes]).
 */
enum class SpeedSourceMode(val id: String) {
    /** The car's speed, else the phone's location under the rules above. */
    AUTO("auto"),
    /** The car's speed only: the phone's location is never used. */
    CAR("car"),
    /** No speed at all. */
    OFF("off");

    companion object {
        fun byId(id: String?) = entries.firstOrNull { it.id == id } ?: AUTO
    }
}

/** MyPlay (N3): where the car screen's speed comes from ([SpeedSourceMode]). */
var Settings.myplaySpeedSource: SpeedSourceMode
    get() = SpeedSourceMode.byId(prefs.getString("myplay_speed_source", null))
    set(v) = prefs.edit { putString("myplay_speed_source", v.id) }

/** A speed to show, in whole km/h, and where it came from. */
data class SpeedReading(val kmh: Int, val from: From) {
    enum class From { CAR, PHONE }
}

object SpeedRules {
    /** A car speed older than this is not shown (cars send some sensors only when they change). */
    const val CAR_FRESH_MS = 3_000L
    /** A phone fix older than this is not shown. */
    const val PHONE_FRESH_MS = 5_000L

    fun kmh(metersPerSecond: Float): Int = (metersPerSecond * 3.6f).roundToInt().coerceAtLeast(0)

    /** The car's speed in m/s when it is fresh, else null. */
    fun carSpeed(car: CarData.State, now: Long): Float? =
        car.speed?.takeIf { car.connected && now - it.atMillis in 0..CAR_FRESH_MS }?.value?.metersPerSecond

    /** Whether the phone's fixes are wanted now: every one of the battery rules holds. */
    fun phoneWanted(mode: SpeedSourceMode, shown: Boolean, connected: Boolean, mapsRunning: Boolean, carFresh: Boolean): Boolean =
        mode == SpeedSourceMode.AUTO && shown && connected && mapsRunning && !carFresh

    /** What to show: the car's speed, else a fresh phone fix (when allowed), else nothing. */
    fun pick(mode: SpeedSourceMode, carMps: Float?, phoneMps: Float?, phoneAt: Long?, now: Long): SpeedReading? = when {
        mode == SpeedSourceMode.OFF -> null
        carMps != null -> SpeedReading(kmh(carMps), SpeedReading.From.CAR)
        mode == SpeedSourceMode.AUTO && phoneMps != null && phoneAt != null && now - phoneAt in 0..PHONE_FRESH_MS ->
            SpeedReading(kmh(phoneMps), SpeedReading.From.PHONE)
        else -> null
    }
}

/** The phone's location fixes, as a speed. [start] returns false when they cannot be had (no permission). */
interface PhoneFixes {
    fun start(onFix: (metersPerSecond: Float, atMillis: Long) -> Unit): Boolean
    fun stop()
}

/**
 * The passive provider: only fixes another app (Waze) already asked for, so MyPlay adds no GPS
 * use of its own. Nothing without the fine location permission. Fixes arrive on the main thread.
 */
class PassiveFixes(context: Context) : PhoneFixes {
    private val app = context.applicationContext
    private val lm = app.getSystemService(LocationManager::class.java)
    private var listener: LocationListener? = null

    @SuppressLint("MissingPermission") // checked just before
    override fun start(onFix: (Float, Long) -> Unit): Boolean {
        if (listener != null) return true
        if (lm == null || ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return false
        val l = LocationListener { loc: Location -> if (loc.hasSpeed()) onFix(loc.speed, System.currentTimeMillis()) }
        return runCatching {
            lm.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, MIN_INTERVAL_MS, 0f, l, Looper.getMainLooper())
            listener = l
            true
        }.onFailure { Log.w(TAG, "passive location", it) }.getOrDefault(false)
    }

    override fun stop() {
        listener?.let { l -> runCatching { lm?.removeUpdates(l) } }
        listener = null
    }

    private companion object {
        const val TAG = "MyPlaySpeed"
        const val MIN_INTERVAL_MS = 1_000L
    }
}

/**
 * Keeps [reading] up to date and the phone's fixes on only while [SpeedRules.phoneWanted].
 * Call [update] on the main thread whenever an input changes (and about once a second, so stale
 * values go), and [release] when the speed leaves the screen.
 */
class SpeedFeed(private val fixes: PhoneFixes, private val clock: () -> Long = System::currentTimeMillis) {
    var reading by mutableStateOf<SpeedReading?>(null)
        private set
    /** Whether the phone's fixes are being listened to (for tests and the connection log). */
    var listening by mutableStateOf(false)
        private set

    private var phoneMps: Float? = null
    private var phoneAt: Long? = null
    private var last: Inputs? = null

    data class Inputs(val mode: SpeedSourceMode, val shown: Boolean, val mapsRunning: Boolean, val car: CarData.State)

    fun update(inputs: Inputs) {
        last = inputs
        val now = clock()
        val carMps = SpeedRules.carSpeed(inputs.car, now)
        val want = SpeedRules.phoneWanted(inputs.mode, inputs.shown, inputs.car.connected, inputs.mapsRunning, carMps != null)
        if (want && !listening) {
            listening = fixes.start { mps, at -> phoneMps = mps; phoneAt = at; last?.let(::update) }
        } else if (!want && listening) {
            stopFixes()
        }
        reading = if (inputs.shown) SpeedRules.pick(inputs.mode, carMps, phoneMps.takeIf { listening }, phoneAt, now) else null
    }

    /** The speed left the screen: stop everything. */
    fun release() {
        stopFixes()
        last = null
        reading = null
    }

    private fun stopFixes() {
        if (listening) fixes.stop()
        listening = false
        phoneMps = null
        phoneAt = null
    }
}
