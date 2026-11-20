package net.prototux.aare.ui.car

import androidx.core.content.edit
import net.prototux.aare.Settings

/**
 * MyPlay (K9): which maps app the home screen's map shows.
 *
 * AARE shows the maps app used last, so opening one from a button quietly changed the home screen
 * (on the bench: Fermata's "Unlock the phone" panel, later a black rectangle). Now it is a
 * setting: the one used last (as before), always a chosen one, or none, so the maps buttons are
 * always offered. And a maps app that shows nothing on the home screen is left out until it is
 * opened again, so the buttons come back instead of an empty card.
 */
internal object HomeMaps {
    const val LAST = "last"
    const val ASK = "ask"
    const val APP_PREFIX = "app:"

    fun chosen(packageName: String) = APP_PREFIX + packageName

    /**
     * The app id for the home screen's map, or null for the maps buttons. [setting] is [LAST],
     * [ASK] or [chosen]; [lastUsedId] the maps app used last; [showsNothing] the apps that showed
     * nothing there.
     */
    fun appId(setting: String, lastUsedId: String?, apps: List<CarAppEntry>, showsNothing: Set<String>): String? {
        val id = when {
            setting == ASK -> null
            setting.startsWith(APP_PREFIX) -> setting.removePrefix(APP_PREFIX).let { pkg ->
                apps.firstOrNull { it.kind == CarAppKind.NAVIGATION && it.packageName == pkg }?.id
            }
            else -> lastUsedId
        }
        return id?.takeIf { it !in showsNothing }
    }

    /** How long a maps app has to show something on the home screen before the buttons come back. */
    const val WAIT_MS = 15_000L
    /** K9-7: how long a maps app left off the home screen is watched for a late screen. */
    const val LATE_WATCH_MS = 5 * 60_000L
}

/** MyPlay (K9): "last", "ask" or "app:<package>". */
var Settings.myplayHomeMaps: String
    get() = prefs.getString("myplay_home_maps", null) ?: HomeMaps.LAST
    set(v) = prefs.edit { putString("myplay_home_maps", v) }
