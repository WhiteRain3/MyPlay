package net.prototux.aare.ui.phone

import android.content.Context
import android.net.ConnectivityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import net.prototux.aare.DiagLog
import net.prototux.aare.Settings
import net.prototux.aare.SettingsBackup
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

/**
 * MyPlay (N21): fetches the speed limit file of the chosen region from MyPlay's own release page,
 * only on a network that is not metered (Wi-Fi), when the app or the page is opened and a month
 * has passed, or when the owner asks. No background job. Nothing but the file is asked for, so
 * nobody learns where the car is.
 */
object SpeedLimitDownload {
    sealed interface State {
        data object Idle : State
        data object Checking : State
        data class Downloading(val percent: Int?) : State
        data object UpToDate : State
        data object Done : State
        data object NeedsWifi : State
        data object Failed : State
        /** MyPlay's release page has no file for the region (yet). */
        data object NotPublished : State
    }

    private class NotFound : Exception()

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()
    private val busy = AtomicBoolean(false)

    /** Whether the phone is on a network that is not metered (Wi-Fi, usually). */
    fun unmetered(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        return cm.activeNetwork != null && !cm.isActiveNetworkMetered
    }

    /** The kept file's description for [region], or null when there is none. */
    fun kept(context: Context, region: String): SpeedLimitManifest? =
        SpeedLimitManifest.parse(runCatching { SpeedLimitFiles.manifestFile(context.filesDir, region).readText() }.getOrNull())
            ?.takeIf { SpeedLimitFiles.file(context.filesDir, region).isFile }

    /** The look a month after the last one (called when the app or the page opens); quiet when it is not due or not on Wi-Fi. */
    suspend fun checkIfDue(context: Context) {
        val settings = Settings(context)
        if (!SpeedLimitCheck.due(settings.myplaySpeedLimitRegion, settings.myplaySpeedLimitChecked, System.currentTimeMillis())) return
        if (!unmetered(context)) return
        update(context, force = false)
    }

    /** Looks for the region's file now and downloads it when newer or missing ([force]: the owner asked). */
    suspend fun update(context: Context, force: Boolean) {
        val settings = Settings(context)
        val region = settings.myplaySpeedLimitRegion
        if (region.isEmpty()) return
        if (!unmetered(context)) { if (force) _state.value = State.NeedsWifi; return }
        if (!busy.compareAndSet(false, true)) return
        try {
            _state.value = State.Checking
            val ok = withContext(Dispatchers.IO) { fetch(context, region) }
            settings.myplaySpeedLimitChecked = System.currentTimeMillis()
            _state.value = ok
        } finally { busy.set(false) }
    }

    /** Removes the region's file and its description. */
    fun delete(context: Context, region: String) {
        SpeedLimitFiles.file(context.filesDir, region).delete()
        SpeedLimitFiles.manifestFile(context.filesDir, region).delete()
        _state.value = State.Idle
    }

    private fun fetch(context: Context, region: String): State {
        val text = try {
            String(get(SpeedLimitFiles.manifestUrl(region)), Charsets.UTF_8)
        } catch (e: NotFound) {
            return State.NotPublished
        } catch (e: Exception) {
            DiagLog.add("speed limits: could not read the file's description (${e.javaClass.simpleName})")
            return State.Failed
        }
        val remote = SpeedLimitManifest.parse(text)?.takeIf { it.region == region } ?: return State.Failed
        val local = kept(context, region)
        if (!SpeedLimitCheck.wanted(local, SpeedLimitFiles.file(context.filesDir, region).isFile, remote)) return State.UpToDate
        val dir = SpeedLimitFiles.dir(context.filesDir).apply { mkdirs() }
        val tmp = File(dir, "$region.bin.tmp")
        try {
            if (!download(SpeedLimitFiles.fileUrl(region), tmp, remote.bytes)) return State.Failed
            val head = tmp.inputStream().use { s -> ByteArray(5).also { s.read(it) } }
            if (!SpeedLimitCheck.verified(remote, tmp.length(), sha256(tmp), head)) {
                DiagLog.add("speed limits: the downloaded file did not match its description; not kept")
                return State.Failed
            }
            if (!tmp.renameTo(SpeedLimitFiles.file(context.filesDir, region))) return State.Failed
            val manifest = File(dir, "$region.json.tmp")
            manifest.writeText(org.json.JSONObject().put("region", remote.region).put("format", remote.format).put("built", remote.built)
                .put("bytes", remote.bytes).put("sha256", remote.sha256).put("source", remote.source).put("url_licence", remote.licenceUrl).toString())
            manifest.renameTo(SpeedLimitFiles.manifestFile(context.filesDir, region))
            DiagLog.add("speed limits: $region file of ${remote.built} kept (${remote.bytes} bytes)")
            return State.Done
        } catch (e: Exception) {
            DiagLog.add("speed limits: download failed (${e.javaClass.simpleName})")
            return State.Failed
        } finally { tmp.delete() }
    }

    private fun open(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 10_000; readTimeout = 20_000
        setRequestProperty("User-Agent", "MyPlay")
    }

    private fun get(url: String): ByteArray {
        val c = open(url)
        try {
            if (c.responseCode == HttpURLConnection.HTTP_NOT_FOUND) throw NotFound()
            check(c.responseCode == HttpURLConnection.HTTP_OK)
            return c.inputStream.use { SettingsBackup.readLimited(it, 64 * 1024) }
        } finally { c.disconnect() }
    }

    private fun download(url: String, to: File, expected: Long): Boolean {
        val c = open(url)
        try {
            if (c.responseCode != HttpURLConnection.HTTP_OK) return false
            var done = 0L
            c.inputStream.use { input ->
                to.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        done += n
                        if (done > expected || done > SpeedLimitFiles.MAX_BYTES) return false
                        out.write(buf, 0, n)
                        _state.value = State.Downloading((done * 100 / expected).toInt())
                    }
                }
            }
            return done == expected
        } finally { c.disconnect() }
    }

    private fun sha256(f: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { s -> val buf = ByteArray(64 * 1024); while (true) { val n = s.read(buf); if (n < 0) break; md.update(buf, 0, n) } }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
