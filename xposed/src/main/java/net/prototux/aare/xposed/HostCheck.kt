package net.prototux.aare.xposed

/**
 * MyPlay: the names the Waze-type hook depends on, and the question it asks, with nothing from the
 * hooking library in it so that a plain unit test can check them against the car library.
 *
 * An app built with the Car App Library (Waze) asks `HostValidator.isValidHost(HostInfo)` about
 * whoever connects to its car screen, and refuses a host whose name and signing key are not on
 * its list. AARE's module looked for `androidx.car.app.HostValidator`; in the library the class
 * is in the `validation` package, so that hook was never installed.
 */
internal object HostCheck {
    const val MYPLAY_PACKAGE = "io.github.whiterain3.myplay"
    const val HOST_VALIDATOR = "androidx.car.app.validation.HostValidator"
    const val HOST_INFO = "androidx.car.app.HostInfo"
    const val IS_VALID_HOST = "isValidHost"
    /** The field of `HostInfo` that holds the host's package name. */
    const val PACKAGE_FIELD = "mPackageName"

    /** The package name a `HostInfo` holds, or null when it cannot be read. */
    fun hostPackage(hostInfo: Any?): String? = try {
        hostInfo?.javaClass?.getDeclaredField(PACKAGE_FIELD)?.apply { isAccessible = true }?.get(hostInfo) as? String
    } catch (_: Throwable) {
        null
    }

    /** Whether the host an app is asked about is MyPlay. Anything unreadable is "no". */
    fun isMyPlay(hostInfo: Any?): Boolean = hostPackage(hostInfo) == MYPLAY_PACKAGE
}
