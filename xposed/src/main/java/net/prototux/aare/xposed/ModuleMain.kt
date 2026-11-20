package net.prototux.aare.xposed

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

/**
 * libxposed entry point (listed in META-INF/xposed/java_init.list). MyPlay: installs the hooks in
 * each app the module is switched on for, once that app's own classes are ready (onPackageReady,
 * as in the owner's working module). Nothing is hooked in Android's system process.
 *
 * Every hook here is a compatibility shim for AARE itself (package [AARE_PACKAGE]), on a phone its
 * owner has rooted: it lets AARE stand in for Google's Android Auto host where an app or the system
 * only recognises Google's. Nothing targets another app's data or another user, and every grant is
 * gated on AARE's own package or uid, so an app the user didn't install AARE for is left untouched.
 *
 * The framework instantiates this with the no-arg constructor and attaches itself, so the hooking
 * methods inherited from [XposedModule] (such as `hook`) are usable from the callbacks below.
 */
class ModuleMain : XposedModule() {

    // MyPlay, first version: only the Waze-type hook (WazeTypeHook.kt), in the apps the module is
    // switched on for. AARE's other hooks are still in this folder but are not installed: the
    // Google Maps and media hooks (AppProcessHooks.kt, MediaHooks.kt) wait until the Waze one is
    // proven, and the hooks into Android's system process (SystemHooks.kt) are left out because a
    // wrong one can stop the phone from starting.
    override fun onPackageReady(param: PackageReadyParam) {
        try {
            // One classloader per process is enough; skip extra loads of the same package.
            if (!param.isFirstPackage) return
            // Don't hook MyPlay's own process: it already knows it is the host.
            if (param.packageName == AARE_PACKAGE) return
            WazeTypeHook.install(this, param.classLoader, param.packageName)
        } catch (t: Throwable) {
            Log.w(WazeTypeHook.TAG, "not installed: $t")
        }
    }

    companion object {
        const val AARE_PACKAGE = HostCheck.MYPLAY_PACKAGE
    }
}
