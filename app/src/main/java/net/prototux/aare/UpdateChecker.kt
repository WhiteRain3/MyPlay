package net.prototux.aare

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.prototux.aare.service.ProjectionService
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * Looks on GitHub for a newer MyPlay release (this fork's own releases, WhiteRain3/MyPlay) and, if
 * there is one, surfaces it as a warning on the home screen (no popup). Off by default; while off
 * in Connection settings it never touches the network by itself. The check is one unauthenticated
 * HTTPS request to the public releases API, at most once a day, never while a car is connected, and
 * silent on any failure. The request carries nothing about the user or the phone: no version, no
 * identifier, only the fixed User-Agent "MyPlay". The result is cached so the warning shows at once
 * on the next start without asking again. The "Check now" button (MyPlayUpdateCheck) makes the same
 * single request when tapped.
 */
object UpdateChecker {
    private const val TAG = "AareUpdate"
    private const val LATEST = "https://api.github.com/repos/WhiteRain3/MyPlay/releases/latest"
    private const val RELEASES = "https://github.com/WhiteRain3/MyPlay/releases/latest"
    /** Every page the app opens for an update starts with this. */
    private const val REPOSITORY_PAGES = "https://github.com/WhiteRain3/MyPlay/"
    /** MyPlay releases are tagged myplay-v1.2.3. */
    private const val TAG_PREFIX = "myplay-v"
    private const val DAY_MS = 24 * 60 * 60 * 1000L

    /** A release newer than the installed one. [version] has no leading "v". */
    data class Update(val version: String, val url: String)

    private val _available = MutableStateFlow<Update?>(null)
    val available: StateFlow<Update?> = _available.asStateFlow()

    @Volatile private var checking = false

    /**
     * Refreshes the home-screen warning. Returns at once: it shows any cached result and, if due,
     * asks GitHub on a background thread. Safe to call on every app open.
     */
    fun refresh(context: Context) {
        val settings = Settings(context.applicationContext)
        if (!settings.checkUpdates || BuildConfig.DEBUG) { _available.value = null; return }
        _available.value = cached(settings)
        // Don't add traffic while a car session is up; the cached warning is enough.
        if (ProjectionService.state.value.connected) return
        val now = System.currentTimeMillis()
        if (checking || now - settings.updateLastCheckMs < DAY_MS) return
        checking = true
        thread(isDaemon = true, name = "aare-update-check") {
            try {
                fetchLatest()?.let { latest ->
                    settings.updateLastCheckMs = System.currentTimeMillis()
                    settings.updateLatestVersion = latest.version
                    settings.updateLatestUrl = latest.url
                    _available.value = cached(settings)
                }
            } finally {
                checking = false
            }
        }
    }

    /** Stops showing the current update; it comes back only for a version newer than this one. */
    fun dismiss(context: Context) {
        val settings = Settings(context.applicationContext)
        settings.updateLatestVersion?.let { settings.updateDismissedVersion = it }
        _available.value = null
    }

    /** The warning to show now, from the cache, the installed version and what the user dismissed. */
    private fun cached(settings: Settings): Update? {
        val latest = settings.updateLatestVersion ?: return null
        if (!isNewer(latest, BuildConfig.VERSION_NAME)) return null
        if (latest == settings.updateDismissedVersion) return null
        return Update(latest, releasePage(settings.updateLatestUrl))
    }

    internal fun fetchLatest(): Update? = runCatching {
        val c = URL(LATEST).openConnection() as HttpURLConnection
        c.connectTimeout = 8_000
        c.readTimeout = 8_000
        c.setRequestProperty("Accept", "application/vnd.github+json")
        c.setRequestProperty("User-Agent", "MyPlay")
        try {
            if (c.responseCode != HttpURLConnection.HTTP_OK) return null
            val json = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
            val version = versionFromTag(json.optString("tag_name")) ?: return null
            Update(version, releasePage(json.optString("html_url")))
        } finally {
            c.disconnect()
        }
    }.onFailure { Log.i(TAG, "update check: ${it.message}") }.getOrNull()

    /**
     * The address to open for a release: [url] if it is a page of MyPlay's own repository on GitHub,
     * otherwise the fixed "latest release" page. So an answer naming any other site is never opened.
     */
    internal fun releasePage(url: String?): String =
        url?.takeIf { it.startsWith(REPOSITORY_PAGES, ignoreCase = true) } ?: RELEASES

    /**
     * The version in a MyPlay release tag ("myplay-v1.2.3" gives "1.2.3"), or null for any other
     * tag: a release that is not tagged as a MyPlay release is never offered as an update.
     */
    internal fun versionFromTag(tag: String): String? {
        val t = tag.trim()
        if (!t.startsWith(TAG_PREFIX)) return null
        val version = t.removePrefix(TAG_PREFIX)
        return version.takeIf { it.firstOrNull()?.isDigit() == true && parts(it).isNotEmpty() }
    }

    /**
     * True when [latest] is a strictly higher semantic version than [installed], comparing numbers
     * so "1.10.0" beats "1.9.0". A missing part counts as 0 ("1.2" == "1.2.0").
     */
    internal fun isNewer(latest: String, installed: String): Boolean {
        val a = parts(latest)
        val b = parts(installed)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    private fun parts(v: String): List<Int> =
        v.trim().removePrefix("v").takeWhile { it.isDigit() || it == '.' }
            .split('.').mapNotNull { it.toIntOrNull() }
}
