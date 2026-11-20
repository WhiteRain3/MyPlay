package net.prototux.aare.xposed

import android.util.Log
import io.github.libxposed.api.XposedInterface

/**
 * MyPlay: the one hook of this first version. In an app built with the Car App Library (Waze),
 * `HostValidator.isValidHost(HostInfo)` answers yes when the host is MyPlay. For every other host
 * the app's own check runs unchanged, so a session with Google's Android Auto is not affected.
 *
 * It fails open: if the class or method is not there (an app may have shortened the names when it
 * was built), or anything goes wrong, nothing is hooked or the app's own answer is used. The
 * phone's log says which, under the tag [TAG]:
 *   "<app>: host check hooked"            the hook is in place
 *   "<app>: host check not hooked: ..."   it is not, with the reason
 *   "<app>: MyPlay accepted as host"      the app asked about MyPlay and got yes (once per uid)
 *   "<app>: MyPlay not accepted: ..."     a host named itself MyPlay but is not it (W1-2), with why
 *
 * A host is MyPlay only when its uid belongs to MyPlay's package and that package is signed with
 * the same key as this module; a name alone is not enough (see [HostCheck.verdict]).
 */
internal object WazeTypeHook {
    const val TAG = "MyPlayHostShims"

    fun install(xposed: XposedInterface, classLoader: ClassLoader, app: String) {
        try {
            val validator = classLoader.loadClass(HostCheck.HOST_VALIDATOR)
            val hostInfo = classLoader.loadClass(HostCheck.HOST_INFO)
            val method = validator.getDeclaredMethod(HostCheck.IS_VALID_HOST, hostInfo)
            // This module's own signing certificates, read once from its APK file when first needed.
            val modulePath = try { xposed.moduleApplicationInfo?.sourceDir } catch (_: Throwable) { null }
            // Not `by lazy`: a first read can fail (Waze's process has no context yet) and must be
            // tried again, not remembered as empty (W1-7a). Kept once it gave an answer.
            val moduleCertsKnown = java.util.concurrent.atomic.AtomicReference<Set<String>>(emptySet())
            fun moduleCerts(): Set<String> = moduleCertsKnown.get().ifEmpty { PackageFacts.certsOfFile(modulePath).also { moduleCertsKnown.set(it) } }
            // Each uid is judged once: its package and key do not change while this app runs. Only
            // definite answers are kept (W1-7); "could not be read" is asked again next time.
            val judged = java.util.concurrent.ConcurrentHashMap<Int, Boolean>()
            val told = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String, Boolean>())
            xposed.hook(method).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                val info = chain.getArg(0)
                val accepted = try {
                    if (!HostCheck.isMyPlay(info)) false
                    else {
                        val uid = HostCheck.hostUid(info)
                        val known = uid?.let { judged[it] }
                        known ?: HostCheck.verdict(HostCheck.hostPackage(info), uid, PackageFacts::packagesForUid,
                            PackageFacts::certsOf, moduleCerts()).let { v ->
                            // Each different answer is logged once, so a repeated unreadable one does not fill the log.
                            if (told.add(v.why)) Log.i(TAG, if (v.accepted) "$app: MyPlay accepted as host" else "$app: MyPlay not accepted: ${v.why}")
                            if (uid != null && v.definite) judged[uid] = v.accepted
                            v.accepted
                        }
                    }
                } catch (_: Throwable) {
                    false
                }
                if (accepted) true else chain.proceed()
            }
            Log.i(TAG, "$app: host check hooked")
        } catch (t: Throwable) {
            Log.w(TAG, "$app: host check not hooked: $t")
        }
    }
}
