package net.prototux.aare.car

import android.os.Handler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * MyPlay (Preview, the dispatcher 2026-11-23): a speed the phone's interactive Preview pretends the
 * car drives at, so the speed rules (the hub's speed, the limit's colours, the drive warnings that
 * read it) can be tried without a car. It is a source of its own: a car state ([car]) that only
 * the Preview's speed source reads. It never writes [CarData], so a real session never sees it,
 * and nothing about it is logged.
 *
 * While a speed is set, it is sent again every [REFRESH_MS] with the time it was sent, as a car
 * that sends its speed often would: the speed rules drop a car speed older than
 * [SpeedRules.CAR_FRESH_MS]. Main thread only.
 */
class PretendSpeed(
    private val main: Handler,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val state = MutableStateFlow(CarData.State())

    /** The pretend car: connected and moving at [kmh] while one is set; else no car at all. */
    val car: StateFlow<CarData.State> = state.asStateFlow()

    /** The speed pretended now, in km/h; null: none (the Preview's car stands still with no speed). */
    var kmh: Int? = null
        private set

    private val tick = Runnable { send() }

    /** Pretend [kmh] (km/h, clamped to 0..[MAX_KMH]), or stop pretending with null. */
    fun set(kmh: Int?) {
        this.kmh = kmh?.coerceIn(0, MAX_KMH)
        send()
    }

    /** Stop pretending and stop the refresh (the Preview closed). */
    fun stop() = set(null)

    private fun send() {
        main.removeCallbacks(tick)
        val k = kmh
        state.value = if (k == null) CarData.State() else CarData.State(
            connected = true,
            speed = CarData.Timed(CarData.Speed(k / 3.6f, cruiseEngaged = null, cruiseSetSpeed = null), clock()),
        )
        if (k != null) main.postDelayed(tick, REFRESH_MS)
    }

    companion object {
        /** Well inside [SpeedRules.CAR_FRESH_MS], so the pretend speed never goes stale. */
        const val REFRESH_MS = 1_000L
        const val MAX_KMH = 250
    }
}
