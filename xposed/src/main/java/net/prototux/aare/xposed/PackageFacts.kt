package net.prototux.aare.xposed

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.security.MessageDigest

/**
 * MyPlay (W1-2): what the hook needs to know about packages, asked from inside the hooked app's
 * process. Every answer is null or empty when it cannot be read, never an exception.
 */
internal object PackageFacts {

    /** The hooked app's own context, once it has one. */
    private fun context(): Context? = try {
        Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null) as? Context
    } catch (_: Throwable) {
        null
    }

    fun packagesForUid(uid: Int): List<String>? = try {
        context()?.packageManager?.getPackagesForUid(uid)?.toList()
    } catch (_: Throwable) {
        null
    }

    /** The certificates an installed package is signed with now, as SHA-256 hex. Android 9 and newer. */
    fun certsOf(packageName: String): Set<String>? = try {
        if (Build.VERSION.SDK_INT < 28) null
        else context()?.packageManager
            ?.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            ?.signingInfo?.apkContentsSigners?.let(::sha256s)
    } catch (_: Throwable) {
        null
    }

    /** The certificates the APK file at [path] (this module's own) is signed with. Android 9 and newer. */
    fun certsOfFile(path: String?): Set<String> = try {
        if (path == null || Build.VERSION.SDK_INT < 28) emptySet()
        else context()?.packageManager
            ?.getPackageArchiveInfo(path, PackageManager.GET_SIGNING_CERTIFICATES)
            ?.signingInfo?.apkContentsSigners?.let(::sha256s).orEmpty()
    } catch (_: Throwable) {
        emptySet()
    }

    private fun sha256s(signatures: Array<Signature>): Set<String> =
        signatures.map { sig -> MessageDigest.getInstance("SHA-256").digest(sig.toByteArray()).joinToString("") { "%02x".format(it) } }.toSet()
}
