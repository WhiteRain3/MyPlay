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
import net.prototux.aare.speedlimit.Matcher
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

/**
 * A speed to show, in whole km/h, and where it came from. N20: with a speed limit file, also the
 * [limit] from the map data (null: no file, nothing to show) and whether the speed is [over] it by
 * more than the margin (the speed then turns a calm colour; never a sound).
 */
data class SpeedReading(val kmh: Int, val from: From, val limit: LimitShown? = null, val over: Boolean = false) {
    enum class From { CAR, PHONE }
}

/** N20: what the car shows about the speed limit; always labelled as map data that may be wrong. */
sealed interface LimitShown {
    /** [defaulted]: from the road's kind or zone, not a sign tagged on the road. */
    data class Known(val kmh: Int, val defaulted: Boolean = false) : LimitShown
    /** No road matched, or not sure which: "no limit known". */
    data object Unknown : LimitShown
}

/** N20: how far over the limit before the speed changes colour ([SpeedRules.MARGIN_OFF]: never). Backed up. */
const val KEY_SPEED_LIMIT_MARGIN = "myplay_speed_limit_margin"
var Settings.myplaySpeedLimitMargin: Int
    get() = prefs.getInt(KEY_SPEED_LIMIT_MARGIN, SpeedRules.MARGIN_DEFAULT).takeIf { it in SpeedRules.MARGINS } ?: SpeedRules.MARGIN_DEFAULT
    set(v) = prefs.edit { putInt(KEY_SPEED_LIMIT_MARGIN, v) }

object SpeedRules {
    /** A car speed older than this is not shown (cars send some sensors only when they change). */
    const val CAR_FRESH_MS = 3_000L
    /** A phone fix older than this is not shown. */
    const val PHONE_FRESH_MS = 5_000L

    /** N20: the margins offered (km/h over the limit), [MARGIN_OFF] for never. */
    const val MARGIN_OFF = -1
    const val MARGIN_DEFAULT = 9
    val MARGINS = listOf(MARGIN_OFF, 5, 9, 15)

    /** A heading from a fix slower than this (m/s, about 7 km/h) is not trusted. */
    const val HEADING_MIN_MPS = 2f

    fun kmh(metersPerSecond: Float): Int = (metersPerSecond * 3.6f).roundToInt().coerceAtLeast(0)

    /** The car's speed in m/s when it is fresh, else null. */
    fun carSpeed(car: CarData.State, now: Long): Float? =
        car.speed?.takeIf { car.connected && now - it.atMillis in 0..CAR_FRESH_MS }?.value?.metersPerSecond

    /**
     * Whether the phone's fixes are wanted now: every one of the battery rules holds. They are
     * wanted for the speed when the car sends none, and (N20) for the position when a speed limit
     * file is there ([limitsOn]); either way only the passive fixes, never a request of MyPlay's own.
     */
    fun phoneWanted(mode: SpeedSourceMode, shown: Boolean, connected: Boolean, mapsRunning: Boolean, carFresh: Boolean, limitsOn: Boolean = false): Boolean =
        mode == SpeedSourceMode.AUTO && shown && connected && mapsRunning && (!carFresh || limitsOn)

    /**
     * N20 (SL-8): a fix worth a limit lookup: accurate enough for the matcher, and a GPS fix (with
     * a speed or a heading), not another app's network fix.
     */
    fun goodForLimit(fix: PhoneFix): Boolean =
        fix.accuracyM != null && fix.accuracyM <= Matcher.MAX_ACCURACY_M && (fix.metersPerSecond != null || fix.bearingDeg != null)

    /** N20: over the limit by more than [margin] (never with [MARGIN_OFF] or no limit known). */
    fun over(kmh: Int, limit: LimitShown?, margin: Int): Boolean =
        margin != MARGIN_OFF && limit is LimitShown.Known && kmh > limit.kmh + margin

    /** What to show: the car's speed, else a fresh phone fix (when allowed), else nothing. */
    fun pick(mode: SpeedSourceMode, carMps: Float?, phoneMps: Float?, phoneAt: Long?, now: Long): SpeedReading? = when {
        mode == SpeedSourceMode.OFF -> null
        carMps != null -> SpeedReading(kmh(carMps), SpeedReading.From.CAR)
        mode == SpeedSourceMode.AUTO && phoneMps != null && phoneAt != null && now - phoneAt in 0..PHONE_FRESH_MS ->
            SpeedReading(kmh(phoneMps), SpeedReading.From.PHONE)
        else -> null
    }
}

/**
 * One of the phone's location fixes: its speed (null: none), where (degrees), the way it moves
 * (degrees from north; null when not known or too slow to trust) and how good it is (metres).
 */
data class PhoneFix(
    val metersPerSecond: Float?, val lat: Double, val lon: Double, val bearingDeg: Float?, val accuracyM: Float?, val atMillis: Long,
)

/** The phone's location fixes. [start] returns false when they cannot be had (no permission). */
interface PhoneFixes {
    fun start(onFix: (PhoneFix) -> Unit): Boolean
    fun stop()
}

/**
 * N20: the limit where a fix is, from the speed limit file of the region chosen on the phone.
 * [available] may touch the disk: call it off the main thread. [lookup] works off the main thread
 * and answers on it.
 */
interface SpeedLimits {
    /** Whether there is a file to read (a region is chosen and its file is there). */
    fun available(): Boolean
    fun lookup(fix: PhoneFix, onResult: (LimitShown) -> Unit)
    fun close()
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
    override fun start(onFix: (PhoneFix) -> Unit): Boolean {
        if (listener != null) return true
        if (lm == null || ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return false
        val l = LocationListener { loc: Location -> onFix(loc.toFix(System.currentTimeMillis())) }
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

internal fun Location.toFix(at: Long): PhoneFix {
    val mps = if (hasSpeed()) speed else null
    return PhoneFix(
        mps, latitude, longitude,
        if (hasBearing() && mps != null && mps >= SpeedRules.HEADING_MIN_MPS) bearing else null,
        if (hasAccuracy()) accuracy else null, at,
    )
}

/**
 * Keeps [reading] up to date and the phone's fixes on only while [SpeedRules.phoneWanted].
 * Call [update] on the main thread whenever an input changes (and about once a second, so stale
 * values go), and [release] when the speed leaves the screen.
 */
class SpeedFeed(
    private val fixes: PhoneFixes,
    private val limits: SpeedLimits? = null,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    var reading by mutableStateOf<SpeedReading?>(null)
        private set
    /** Whether the phone's fixes are being listened to (for tests and the connection log). */
    var listening by mutableStateOf(false)
        private set

    private var phoneMps: Float? = null
    private var phoneAt: Long? = null
    private var last: Inputs? = null
    private var limit: LimitShown? = null
    private var limitAt: Long? = null
    /** Answers to an older lookup, or from before a stop, are dropped. */
    private var lookups = 0

    /** [limitsOn]: a speed limit file is there to read (N20); [margin]: as [myplaySpeedLimitMargin]. */
    data class Inputs(
        val mode: SpeedSourceMode, val shown: Boolean, val mapsRunning: Boolean, val car: CarData.State,
        val limitsOn: Boolean = false, val margin: Int = SpeedRules.MARGIN_DEFAULT,
    )

    fun update(inputs: Inputs) {
        last = inputs
        val now = clock()
        val carMps = SpeedRules.carSpeed(inputs.car, now)
        val limitsOn = inputs.limitsOn && limits != null
        val want = SpeedRules.phoneWanted(inputs.mode, inputs.shown, inputs.car.connected, inputs.mapsRunning, carMps != null, limitsOn)
        if (want && !listening) {
            listening = fixes.start(::onFix)
        } else if (!want && listening) {
            stopFixes()
        }
        if (!limitsOn) forgetLimit()
        val base = if (inputs.shown) SpeedRules.pick(inputs.mode, carMps, phoneMps.takeIf { listening }, phoneAt, now) else null
        reading = if (base == null || !limitsOn || inputs.mode != SpeedSourceMode.AUTO) base else {
            // A limit only from a fresh fix; otherwise not known.
            val shown = limit.takeIf { listening && limitAt?.let { now - it in 0..SpeedRules.PHONE_FRESH_MS } == true } ?: LimitShown.Unknown
            base.copy(limit = shown, over = SpeedRules.over(base.kmh, shown, inputs.margin))
        }
    }

    private fun onFix(fix: PhoneFix) {
        // SL-7: the passive provider also passes on other apps' network fixes, which have no speed:
        // they leave the last speed alone (it goes stale on its own).
        if (fix.metersPerSecond != null) {
            phoneMps = fix.metersPerSecond
            phoneAt = fix.atMillis
        }
        val l = limits
        // SL-8: only a fix good enough to match asks for a limit; any other leaves the last answer
        // until it goes stale.
        if (l != null && last?.limitsOn == true && SpeedRules.goodForLimit(fix)) {
            val n = ++lookups
            l.lookup(fix) { r ->
                if (n == lookups && listening) {
                    limit = r
                    limitAt = fix.atMillis
                    last?.let(::update)
                }
            }
        }
        last?.let(::update)
    }

    private fun forgetLimit() {
        lookups++
        limit = null
        limitAt = null
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
        forgetLimit()
    }
}
