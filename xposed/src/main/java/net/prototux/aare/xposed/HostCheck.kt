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
    fun hostPackage(hostInfo: Any?): String? = fieldValue(hostInfo, PACKAGE_FIELD, String::class.java) as? String

    /**
     * The value of field [name] of [obj]; if there is no such field (an app may shorten its
     * internal names when it is built, W1-3), the value of the class's only instance field of
     * [type]. Null when neither can be read, or when there is more than one such field to choose
     * from.
     */
    fun fieldValue(obj: Any?, name: String, type: Class<*>): Any? = try {
        val cls = obj?.javaClass ?: return null
        val field = try {
            cls.getDeclaredField(name)
        } catch (_: NoSuchFieldException) {
            cls.declaredFields.filter { it.type == type && !java.lang.reflect.Modifier.isStatic(it.modifiers) }.singleOrNull()
        }
        field?.takeIf { it.type == type }?.apply { isAccessible = true }?.get(obj)
    } catch (_: Throwable) {
        null
    }

    /** The field of `HostInfo` that holds the host's uid. */
    const val UID_FIELD = "mUid"

    /** The uid a `HostInfo` holds, or null when it cannot be read. */
    fun hostUid(hostInfo: Any?): Int? = fieldValue(hostInfo, UID_FIELD, Int::class.javaPrimitiveType!!) as? Int

    /** Whether the host an app is asked about names itself MyPlay. Anything unreadable is "no". */
    fun isMyPlay(hostInfo: Any?): Boolean = hostPackage(hostInfo) == MYPLAY_PACKAGE

    /** The answer for one host: [accepted], and why not when it is not. */
    data class Verdict(val accepted: Boolean, val why: String)

    /**
     * MyPlay (W1-2): whether a host that names itself MyPlay really is MyPlay. A name alone can be
     * written by anyone, so the host's [uid] must belong to MyPlay's package, and that package must
     * be signed with exactly the same certificates as this module (MyPlay and its host shims are
     * signed with the same key). Anything that cannot be read is "no", and then the app's own
     * check decides, as without the module.
     *
     * [packagesForUid] and [certsOf] ask the phone; [moduleCerts] are this module's own signing
     * certificates. Certificates are compared as SHA-256 hex strings.
     */
    fun verdict(
        name: String?,
        uid: Int?,
        packagesForUid: (Int) -> List<String>?,
        certsOf: (String) -> Set<String>?,
        moduleCerts: Set<String>,
    ): Verdict {
        if (name != MYPLAY_PACKAGE) return Verdict(false, "the host is not MyPlay")
        if (uid == null) return Verdict(false, "the host's uid could not be read")
        val packages = packagesForUid(uid) ?: return Verdict(false, "the packages of uid $uid could not be read")
        if (MYPLAY_PACKAGE !in packages) return Verdict(false, "uid $uid is not MyPlay's (it is ${packages.joinToString()})")
        if (moduleCerts.isEmpty()) return Verdict(false, "this module's own signing certificate could not be read")
        val certs = certsOf(MYPLAY_PACKAGE)
        if (certs.isNullOrEmpty()) return Verdict(false, "MyPlay's signing certificate could not be read")
        if (certs != moduleCerts) return Verdict(false, "MyPlay is not signed with the same key as this module")
        return Verdict(true, "MyPlay, signed like this module")
    }
}
