package net.prototux.aare.ui.phone.next

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.prototux.aare.BuildConfig
import net.prototux.aare.MyPlayUpdateInstall
import net.prototux.aare.UpdateChecker

/** What the Updates page's hero shows (design frames 44–47). */
sealed interface UpdateHero {
    data class Ready(val version: String) : UpdateHero
    data class Downloading(val version: String, val percent: Int?) : UpdateHero
    data class Installing(val version: String) : UpdateHero
    data class Failed(val version: String, val reason: MyPlayUpdateInstall.Failure) : UpdateHero
    data class NeedsPermission(val version: String) : UpdateHero
    data object Checking : UpdateHero
    data object UpToDate : UpdateHero
}

/**
 * The hero for the install's [install] state, the update [found] (by the daily check or Check now),
 * the version an install was started for ([installing]), and whether Check now is running.
 */
fun updateHeroOf(install: MyPlayUpdateInstall.State, found: UpdateChecker.Update?, installing: String?, checking: Boolean): UpdateHero {
    val v = installing ?: found?.version
    if (v != null) when (install) {
        is MyPlayUpdateInstall.State.Downloading -> return UpdateHero.Downloading(v, install.percent)
        MyPlayUpdateInstall.State.Checking, MyPlayUpdateInstall.State.Installing -> return UpdateHero.Installing(v)
        MyPlayUpdateInstall.State.NeedsPermission -> return UpdateHero.NeedsPermission(v)
        is MyPlayUpdateInstall.State.Failed -> return UpdateHero.Failed(v, install.reason)
        MyPlayUpdateInstall.State.Idle -> Unit
    }
    return when {
        found != null -> UpdateHero.Ready(found.version)
        checking -> UpdateHero.Checking
        else -> UpdateHero.UpToDate
    }
}

/**
 * "Check now" (plan §5): one request to GitHub per tap, whether or not the daily check is on. Its
 * answer lives as long as the app, not in settings, so the daily check's cache is untouched.
 */
object NextUpdateCheck {
    sealed interface Result {
        data object Checking : Result
        data class Done(val at: Long, val found: UpdateChecker.Update?) : Result
        data object Offline : Result
    }

    private val _result = MutableStateFlow<Result?>(null)
    val result: StateFlow<Result?> = _result.asStateFlow()

    /** The version an install was started for from this page, so its progress can name it. */
    val installing = MutableStateFlow<String?>(null)

    /** Asks GitHub once; call it off the main thread. */
    fun checkNow(now: () -> Long = System::currentTimeMillis, fetch: () -> UpdateChecker.Update? = UpdateChecker::fetchLatest) {
        if (_result.value == Result.Checking) return
        _result.value = Result.Checking
        val latest = fetch()
        _result.value = when {
            latest == null -> Result.Offline
            UpdateChecker.isNewer(latest.version, BuildConfig.VERSION_NAME) -> Result.Done(now(), latest)
            else -> Result.Done(now(), null)
        }
    }

    internal fun resetForTest() { _result.value = null; installing.value = null }
}

/** The newest [versions] first, numbers compared as numbers ("0.1.10" after "0.1.9"). */
fun newestFirst(versions: List<String>): List<String> =
    versions.sortedWith { a, b -> if (UpdateChecker.isNewer(a, b)) -1 else if (UpdateChecker.isNewer(b, a)) 1 else 0 }
