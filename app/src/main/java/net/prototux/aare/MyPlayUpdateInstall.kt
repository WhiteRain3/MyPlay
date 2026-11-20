package net.prototux.aare

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * MyPlay (plan I2): downloads a newer release from WhiteRain3/MyPlay on GitHub and installs it,
 * only when the owner taps "Download and install", and only after checking it:
 * - the file comes from MyPlay's own release of that version on GitHub (asset MyPlay-<v>.apk);
 * - its SHA-256 matches the release's SHA256SUMS;
 * - it is MyPlay (same package), that version, and signed by the same key as the installed app.
 * Any failed check stops it, deletes the file and says which check failed. Android itself then
 * asks the owner to confirm the install (and, the first time, to allow MyPlay to install apps).
 * No data about the phone is sent: three plain HTTPS requests with the User-Agent "MyPlay".
 */
object MyPlayUpdateInstall {
    private const val API = "https://api.github.com/repos/WhiteRain3/MyPlay/releases/tags/"
    private const val DOWNLOADS = "https://github.com/WhiteRain3/MyPlay/releases/download/"
    private const val MAX_APK_BYTES = 150L * 1024 * 1024
    private const val ACTION_DONE = "io.github.whiterain3.myplay.UPDATE_INSTALLED"

    /** Where an install stands, for the page that started it. */
    sealed interface State {
        data object Idle : State
        data class Downloading(val percent: Int?) : State
        data object Checking : State
        /** Android's own install screen is up (or about to be). */
        data object Installing : State
        /** MyPlay may not install apps yet: the owner allows it in the screen this opened, then taps again. */
        data object NeedsPermission : State
        data class Failed(val reason: Failure) : State
    }

    enum class Failure { NETWORK, NO_APK, CHECKSUM, NOT_MYPLAY, WRONG_VERSION, WRONG_SIGNER, INSTALL }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    /** A version used in addresses and file names: digits and dots only ("0.2.0"). */
    internal fun isPlainVersion(v: String): Boolean = v.matches(Regex("\\d+(\\.\\d+)*"))

    /** The APK of [version] in a release's JSON, only at MyPlay's own download address for that version. */
    internal fun apkUrl(release: JSONObject, version: String): String? = assetUrl(release, "MyPlay-$version.apk", version)

    /** The SHA256SUMS file of the same release. */
    internal fun sumsUrl(release: JSONObject, version: String): String? = assetUrl(release, "SHA256SUMS", version)

    private fun assetUrl(release: JSONObject, name: String, version: String): String? {
        val assets = release.optJSONArray("assets") ?: return null
        val prefix = "$DOWNLOADS${"myplay-v"}$version/"
        for (i in 0 until assets.length()) {
            val a = assets.optJSONObject(i) ?: continue
            if (a.optString("name") != name) continue
            val url = a.optString("browser_download_url")
            return url.takeIf { it == prefix + name }
        }
        return null
    }

    /** The SHA-256 (lower-case hex) the SHA256SUMS text gives for [fileName], or null. */
    internal fun expectedSha256(sums: String, fileName: String): String? =
        sums.lines().map { it.trim() }.firstNotNullOfOrNull { line ->
            val parts = line.split(Regex("\\s+"), limit = 2)
            if (parts.size == 2 && parts[1].removePrefix("*") == fileName && parts[0].matches(Regex("[0-9a-fA-F]{64}"))) parts[0].lowercase() else null
        }

    /** Whether the new file's signers are exactly the installed app's: none missing, none added, at least one. */
    internal fun sameSigners(installed: Set<String>, candidate: Set<String>): Boolean = candidate.isNotEmpty() && candidate == installed

    /** Which check a downloaded file fails, or null when it passes them all. */
    internal fun check(
        expectedSum: String?, actualSum: String, ownPackage: String, filePackage: String?, version: String, fileVersion: String?,
        installedSigners: Set<String>, fileSigners: Set<String>,
    ): Failure? = when {
        expectedSum == null || expectedSum != actualSum -> Failure.CHECKSUM
        filePackage != ownPackage -> Failure.NOT_MYPLAY
        fileVersion != version -> Failure.WRONG_VERSION
        !sameSigners(installedSigners, fileSigners) -> Failure.WRONG_SIGNER
        else -> null
    }

    /** Downloads, checks and installs [version]. Call off the main thread; follow [state]. */
    fun downloadAndInstall(context: Context, version: String) {
        val app = context.applicationContext
        if (!isPlainVersion(version)) { _state.value = State.Failed(Failure.NO_APK); return }
        // Claim the run at once (two taps in the same moment start only one).
        val before = _state.value
        if (before is State.Downloading || before == State.Checking || before == State.Installing) return
        if (!_state.compareAndSet(before, State.Downloading(null))) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !app.packageManager.canRequestPackageInstalls()) {
            _state.value = State.NeedsPermission
            runCatching {
                app.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, android.net.Uri.parse("package:${app.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            return
        }
        val dir = File(app.cacheDir, "updates").apply { deleteRecursively(); mkdirs() }
        val apk = File(dir, "MyPlay-$version.apk")
        fun fail(f: Failure) { apk.delete(); _state.value = State.Failed(f) }
        _state.value = State.Downloading(null)
        val release = runCatching { JSONObject(get(API + "myplay-v" + version).decodeToString()) }.getOrNull() ?: return fail(Failure.NETWORK)
        val apkUrl = apkUrl(release, version) ?: return fail(Failure.NO_APK)
        val sums = sumsUrl(release, version)?.let { u -> runCatching { get(u, 64 * 1024).decodeToString() }.getOrNull() }
        if (!runCatching { download(apkUrl, apk) }.getOrDefault(false)) return fail(Failure.NETWORK)
        _state.value = State.Checking
        val info = archiveInfo(app.packageManager, apk)
        val installed = runCatching { packageInfo(app.packageManager, app.packageName) }.getOrNull()
        check(
            sums?.let { expectedSha256(it, apk.name) }, sha256(apk), app.packageName, info?.packageName, version, info?.versionName,
            installed?.let(::signers).orEmpty(), info?.let(::signers).orEmpty(),
        )?.let { return fail(it) }
        _state.value = State.Installing
        if (!runCatching { install(app, apk) }.getOrDefault(false)) fail(Failure.INSTALL)
    }

    /** Back to idle (after a failure was read, or the permission was granted). */
    fun reset() { if (_state.value !is State.Downloading && _state.value != State.Checking) _state.value = State.Idle }

    private fun open(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 10_000; readTimeout = 20_000
        setRequestProperty("User-Agent", "MyPlay")
        setRequestProperty("Accept", if (url.startsWith("https://api.github.com/")) "application/vnd.github+json" else "application/octet-stream")
    }

    private fun get(url: String, limit: Int = 512 * 1024): ByteArray {
        val c = open(url)
        try {
            check(c.responseCode == HttpURLConnection.HTTP_OK)
            return c.inputStream.use { SettingsBackup.readLimited(it, limit) }
        } finally { c.disconnect() }
    }

    private fun download(url: String, to: File): Boolean {
        val c = open(url)
        try {
            if (c.responseCode != HttpURLConnection.HTTP_OK) return false
            val total = c.contentLengthLong.takeIf { it > 0 }
            if (total != null && total > MAX_APK_BYTES) return false
            var done = 0L
            c.inputStream.use { input ->
                to.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        done += n
                        if (done > MAX_APK_BYTES) return false
                        out.write(buf, 0, n)
                        _state.value = State.Downloading(total?.let { (done * 100 / it).toInt() })
                    }
                }
            }
            return true
        } finally { c.disconnect() }
    }

    private fun sha256(f: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { input -> val buf = ByteArray(64 * 1024); while (true) { val n = input.read(buf); if (n < 0) break; md.update(buf, 0, n) } }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    @Suppress("DEPRECATION")
    private fun archiveInfo(pm: PackageManager, apk: File): PackageInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pm.getPackageArchiveInfo(apk.path, PackageManager.GET_SIGNING_CERTIFICATES)
        else pm.getPackageArchiveInfo(apk.path, PackageManager.GET_SIGNATURES)

    @Suppress("DEPRECATION")
    private fun packageInfo(pm: PackageManager, pkg: String): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
        else pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES)

    /** SHA-256 of each signing certificate the package carries now. */
    @Suppress("DEPRECATION")
    private fun signers(info: PackageInfo): Set<String> {
        val sigs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val s = info.signingInfo ?: return emptySet()
            if (s.hasMultipleSigners()) s.apkContentsSigners else s.signingCertificateHistory?.takeLast(1)?.toTypedArray()
        } else info.signatures
        return sigs.orEmpty().map { sig -> MessageDigest.getInstance("SHA-256").digest(sig.toByteArray()).joinToString("") { "%02x".format(it) } }.toSet()
    }

    /** Hands [apk] to Android's package installer, which asks the owner to confirm. */
    private fun install(context: Context, apk: File): Boolean {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
        }
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            session.openWrite("MyPlay.apk", 0, apk.length()).use { out -> apk.inputStream().use { it.copyTo(out) }; session.fsync(out) }
            ContextCompat.registerReceiver(context, object : BroadcastReceiver() {
                override fun onReceive(c: Context, intent: Intent) {
                    when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
                        PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                            @Suppress("DEPRECATION")
                            val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                            if (confirm != null) runCatching { c.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        }
                        PackageInstaller.STATUS_SUCCESS -> { _state.value = State.Idle; runCatching { c.unregisterReceiver(this) } }
                        else -> { _state.value = State.Failed(Failure.INSTALL); apk.delete(); runCatching { c.unregisterReceiver(this) } }
                    }
                }
            }, IntentFilter(ACTION_DONE), ContextCompat.RECEIVER_NOT_EXPORTED)
            val pending = PendingIntent.getBroadcast(
                context, id, Intent(ACTION_DONE).setPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0),
            )
            session.commit(pending.intentSender)
        }
        return true
    }
}
