package net.prototux.aare.ui.phone.next

import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.prototux.aare.Settings
import net.prototux.aare.car.CarInputEvents
import net.prototux.aare.ui.phone.CheckInput
import net.prototux.aare.ui.phone.CheckStep
import net.prototux.aare.ui.phone.StepCapture
import net.prototux.aare.ui.phone.carSendsHolds
import net.prototux.aare.ui.phone.myplayCarSendsHolds
import net.prototux.aare.ui.phone.toCheckInput

/**
 * The Car check's run (plan §4), kept for the whole process rather than by the page: moving between
 * pages and tabs keeps it. Whether a run was going on is also kept in the settings, so after the
 * process was killed Home can say "Car check stopped" ([stoppedEarlier]).
 */
object CarCheckRun {
    /** The step on screen; [CheckStep.entries].size once every step is done. */
    private val _step = MutableStateFlow(0)
    val step: StateFlow<Int> = _step.asStateFlow()
    private val _captures = MutableStateFlow<Map<CheckStep, StepCapture>>(emptyMap())
    val captures: StateFlow<Map<CheckStep, StepCapture>> = _captures.asStateFlow()
    private val _running = MutableStateFlow(false)
    /** A run is going on in this process (started and not finished). */
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    private val _finishedAt = MutableStateFlow<Long?>(null)
    /** When the last run in this process went through every step (wall clock), for "Done {date}". */
    val finishedAt: StateFlow<Long?> = _finishedAt.asStateFlow()

    val done: Boolean get() = _step.value >= CheckStep.entries.size

    /**
     * Starts listening (or keeps listening) to the car; everything that arrives goes to the step on screen.
     * A finished run starts again from the first step; a stopped one goes on from its step.
     */
    fun start(settings: Settings, inputs: Flow<CheckInput> = CarInputEvents.events.map { it.toCheckInput() }) {
        if (job?.isActive == true) return
        if (done) reset()
        _running.value = true
        markRunning(settings, true)
        job = scope.launch { inputs.collect { add(it) } }
    }

    /** One input, for the step on screen; nothing once all are done. */
    fun add(input: CheckInput) {
        val s = CheckStep.entries.getOrNull(_step.value) ?: return
        _captures.value = _captures.value + (s to (_captures.value[s] ?: StepCapture()).with(input))
    }

    /** On to the next step; an empty capture records "nothing happened". */
    fun next(settings: Settings) {
        val s = CheckStep.entries.getOrNull(_step.value) ?: return
        if (_captures.value[s] == null) _captures.value = _captures.value + (s to StepCapture())
        _step.value = _step.value + 1
        // #1: what the hold steps saw, for the Buttons page's note.
        carSendsHolds(_captures.value)?.let { settings.myplayCarSendsHolds = it }
        if (done) { _finishedAt.value = System.currentTimeMillis(); finish(settings) }
    }

    /** The step on screen again, from nothing. */
    fun again() {
        val s = CheckStep.entries.getOrNull(_step.value) ?: return
        _captures.value = _captures.value - s
    }

    fun goTo(index: Int) { _step.value = index.coerceIn(0, CheckStep.entries.size) }

    private fun finish(settings: Settings) {
        job?.cancel(); job = null
        _running.value = false
        markRunning(settings, false)
    }

    /** Stops without finishing (Home's "stopped" card's own Start again clears this). */
    fun stop(settings: Settings) { finish(settings) }

    fun reset() {
        _step.value = 0
        _captures.value = emptyMap()
    }

    /** True when the settings say a run was going on but this process has none: the process was killed. */
    fun stoppedEarlier(settings: Settings): Boolean = !_running.value && settings.prefs.getBoolean(KEY_RUNNING, false)

    /** Forgets that a run was stopped early (once the owner has seen it or started again). */
    fun clearStopped(settings: Settings) { if (!_running.value) markRunning(settings, false) }

    private fun markRunning(settings: Settings, on: Boolean) = settings.prefs.edit { if (on) putBoolean(KEY_RUNNING, true) else remove(KEY_RUNNING) }

    /** Not backed up: a moment's state of this phone. */
    const val KEY_RUNNING = "myplay_car_check_running"

    internal fun resetForTest() { job?.cancel(); job = null; _running.value = false; _finishedAt.value = null; reset() }
}

/** What the Car check is doing, for More's and Diagnostics' row and Home's card (plan §4, §9.3). */
sealed interface CarCheckStatus {
    data object NotRun : CarCheckStatus
    data class InProgress(val step: Int, val total: Int) : CarCheckStatus
    /** Stopped part way: left in this process, or the process was killed during a run. */
    data object Stopped : CarCheckStatus
    data class Done(val at: Long?) : CarCheckStatus
}

fun carCheckStatus(step: Int, total: Int, running: Boolean, anyCaptured: Boolean, stoppedEarlier: Boolean, finishedAt: Long?): CarCheckStatus = when {
    running -> CarCheckStatus.InProgress(step.coerceAtMost(total - 1), total)
    stoppedEarlier -> CarCheckStatus.Stopped
    step >= total -> CarCheckStatus.Done(finishedAt)
    anyCaptured || step > 0 -> CarCheckStatus.Stopped
    else -> CarCheckStatus.NotRun
}
