package net.prototux.aare.launch

import android.content.Context
import android.os.Handler
import android.os.Looper

/**
 * MyPlay (VA-8): the phone apps hosted on the car screen outlive their screen for a while. Left (the
 * map, the home) or hidden while driving, an app's display is kept with no picture going to it, so
 * coming back carries on where it was instead of starting the app (and its video) again. One app is
 * kept at a time, for [keepMs]; the one left before goes. Main thread.
 */
class HostedApps<L : HostedApps.Launcher>(
    private val newLauncher: () -> L,
    private val keepMs: Long = KEEP_MS,
    private val handler: Handler = Handler(Looper.getMainLooper()),
) {
    /** What [HostedApps] needs of a launcher ([AppLauncher]). */
    interface Launcher {
        val connected: Boolean
        fun detach()
        fun release()
    }

    /** One app's launcher and its display, once [displayId] says it started. */
    class Hosted<L : Launcher>(val packageName: String, val launcher: L) {
        @Volatile var displayId = -1
        val started: Boolean get() = displayId >= 0 && launcher.connected
    }

    private var kept: Hosted<L>? = null
    private val dropKept = Runnable { kept?.launcher?.release(); kept = null }

    /** The app [packageName] for its screen: the one kept, if it is that app, or a new one. */
    fun take(packageName: String): Hosted<L> {
        handler.removeCallbacks(dropKept)
        kept?.let { k ->
            kept = null
            if (k.packageName == packageName) return k
            k.launcher.release()
        }
        return Hosted(packageName, newLauncher())
    }

    /** [h]'s screen went away: its display is kept, without a picture, for [keepMs]; not started: released now. */
    fun leave(h: Hosted<L>) {
        if (!h.started) { h.launcher.release(); return }
        h.launcher.detach()
        kept?.takeIf { it !== h }?.launcher?.release()
        kept = h
        handler.removeCallbacks(dropKept)
        handler.postDelayed(dropKept, keepMs)
    }

    /** The package kept without a screen, if any. */
    val keptPackage: String? get() = kept?.packageName

    /** The car went away: nothing is kept. */
    fun releaseAll() {
        handler.removeCallbacks(dropKept)
        kept?.launcher?.release()
        kept = null
    }

    companion object {
        /** The car's: real launchers. */
        fun of(context: Context) = HostedApps({ AppLauncher(context) })

        /** How long a left app stays: back within this, it carries on. */
        const val KEEP_MS = 2 * 60_000L
    }
}
