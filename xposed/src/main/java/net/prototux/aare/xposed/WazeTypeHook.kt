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
 *   "<app>: MyPlay accepted as host"      the app asked about MyPlay and got yes (logged once)
 */
internal object WazeTypeHook {
    const val TAG = "MyPlayHostShims"

    fun install(xposed: XposedInterface, classLoader: ClassLoader, app: String) {
        try {
            val validator = classLoader.loadClass(HostCheck.HOST_VALIDATOR)
            val hostInfo = classLoader.loadClass(HostCheck.HOST_INFO)
            val method = validator.getDeclaredMethod(HostCheck.IS_VALID_HOST, hostInfo)
            var told = false
            xposed.hook(method).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept { chain ->
                val mine = try { HostCheck.isMyPlay(chain.getArg(0)) } catch (_: Throwable) { false }
                if (!mine) return@intercept chain.proceed()
                if (!told) {
                    told = true
                    Log.i(TAG, "$app: MyPlay accepted as host")
                }
                true
            }
            Log.i(TAG, "$app: host check hooked")
        } catch (t: Throwable) {
            Log.w(TAG, "$app: host check not hooked: $t")
        }
    }
}
