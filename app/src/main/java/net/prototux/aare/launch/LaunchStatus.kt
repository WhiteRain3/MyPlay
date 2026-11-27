package net.prototux.aare.launch

/**
 * MyPlay (VA-17): how starting a phone app on the car screen went, so the car says it instead of
 * showing something else silently (before, an app that refused the car's display left the phone's
 * screen showing, which looked like mirroring). Pure.
 */
object LaunchStatus {
    // From the privileged host (ILaunchHost.status).
    const val NO_CONTEXT = "no_context"
    const val NO_DISPLAY = "no_display"
    const val LAUNCH_FAILED = "launch_failed"
    const val LAUNCHED = "launched"
    const val ON_CAR = "on_car"
    const val ON_PHONE = "on_phone"

    // From the app side (AppLauncher), before the host runs.
    const val NO_ROOT_OR_SHIZUKU = "no_root_or_shizuku"
    const val NOT_CONNECTED = "not_connected"

    /** What the car screen shows for [status], or null when all is well (or not known yet). */
    enum class Problem { NEEDS_ROOT, NOT_CONNECTED, NO_DISPLAY, LAUNCH_FAILED, ON_PHONE }

    /**
     * VA-20: a status with how it was tried ([method]: root or Shizuku) and the concrete error, as
     * "code|method: detail"; [problem] reads the code, [detail] the rest.
     */
    fun with(code: String, method: String, detail: String): String = "$code|$method: ${detail.replace('\n', ' ').trim()}"

    /** VA-20: the "method: error" part of [status], at most [MAX_DETAIL] characters; null when there is none. */
    fun detail(status: String?): String? = status?.substringAfter('|', "")?.trim()?.takeIf { it.isNotEmpty() }
        ?.let { if (it.length > MAX_DETAIL) it.take(MAX_DETAIL - 1) + "…" else it }

    private const val MAX_DETAIL = 180

    fun problem(status: String?): Problem? = when (status?.substringBefore('|')) {
        NO_ROOT_OR_SHIZUKU -> Problem.NEEDS_ROOT
        NOT_CONNECTED -> Problem.NOT_CONNECTED
        NO_CONTEXT, NO_DISPLAY -> Problem.NO_DISPLAY
        LAUNCH_FAILED -> Problem.LAUNCH_FAILED
        ON_PHONE -> Problem.ON_PHONE
        else -> null
    }
}
