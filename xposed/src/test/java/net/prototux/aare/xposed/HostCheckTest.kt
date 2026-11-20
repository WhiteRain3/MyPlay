package net.prototux.aare.xposed

import androidx.car.app.HostInfo
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The hook looks things up by name in another app, where a wrong name fails silently. These tests
 * hold the names against the car library this project builds with (the fault that made AARE's
 * hook dead was a wrong package in the class name).
 */
class HostCheckTest {
    private val loader = javaClass.classLoader!!

    @Test fun theHostCheckTheHookNamesExistsInTheCarLibrary() {
        val validator = Class.forName(HostCheck.HOST_VALIDATOR, false, loader)
        val hostInfo = Class.forName(HostCheck.HOST_INFO, false, loader)
        val method = validator.getDeclaredMethod(HostCheck.IS_VALID_HOST, hostInfo)
        assertEquals(Boolean::class.javaPrimitiveType, method.returnType)
    }

    @Test fun theOldNameIsNotInTheCarLibrary() {
        val found = runCatching { Class.forName("androidx.car.app.HostValidator", false, loader) }.isSuccess
        assertFalse(found)
    }

    @Test fun thePackageNameIsReadFromARealHostInfo() {
        assertEquals("com.example.host", HostCheck.hostPackage(HostInfo("com.example.host", 10123)))
    }

    @Test fun onlyMyPlayIsAccepted() {
        assertTrue(HostCheck.isMyPlay(HostInfo(HostCheck.MYPLAY_PACKAGE, 10123)))
        assertFalse(HostCheck.isMyPlay(HostInfo("com.google.android.projection.gearhead", 10123)))
        assertFalse(HostCheck.isMyPlay(HostInfo(HostCheck.MYPLAY_PACKAGE + ".hostshims", 10123)))
    }

    @Test fun somethingThatIsNotAHostInfoIsNotAccepted() {
        assertNull(HostCheck.hostPackage(null))
        assertNull(HostCheck.hostPackage("io.github.whiterain3.myplay"))
        assertFalse(HostCheck.isMyPlay(null))
        assertFalse(HostCheck.isMyPlay(Any()))
    }

    @Test fun theModulesSettingsAreInTheFormTheOwnersFrameworkLoads() {
        val dir = listOf("src/main/resources/META-INF/xposed", "xposed/src/main/resources/META-INF/xposed").map(::File).first { it.isDirectory }
        val prop = File(dir, "module.prop").readLines().filter { it.isNotBlank() }.associate { it.substringBefore('=') to it.substringAfter('=') }
        assertEquals(mapOf("minApiVersion" to "101", "targetApiVersion" to "101", "staticScope" to "true", "exceptionMode" to "protective"), prop)
        // First version: Waze only, and never Android's own system process.
        assertEquals(listOf("com.waze"), File(dir, "scope.list").readLines().filter { it.isNotBlank() })
        assertEquals(listOf(ModuleMain::class.java.name), File(dir, "java_init.list").readLines().filter { it.isNotBlank() })
    }

    @Test fun theUidIsReadFromARealHostInfo() {
        assertEquals(10123, HostCheck.hostUid(HostInfo("com.example.host", 10123)))
        assertNull(HostCheck.hostUid(null))
        assertNull(HostCheck.hostUid(Any()))
    }

    // ---- W1-2: a host is MyPlay only by uid and signing key, not by name alone ----

    private val myplay = HostCheck.MYPLAY_PACKAGE
    private val key = setOf("5c39f23994ffbdb49e073d1c1393ea0af9768db3ba481b64b735c363ee7943c1")
    private fun verdict(
        name: String? = myplay,
        uid: Int? = 10123,
        packages: List<String>? = listOf(myplay),
        certs: Set<String>? = key,
        module: Set<String> = key,
    ) = HostCheck.verdict(name, uid, { packages }, { certs }, module)

    @Test fun myPlaySignedLikeTheModuleIsAccepted() = assertTrue(verdict().accepted)

    @Test fun anotherNameIsNotAccepted() = assertFalse(verdict(name = "com.google.android.projection.gearhead").accepted)

    @Test fun aUidThatIsNotMyPlaysIsNotAccepted() {
        val v = verdict(packages = listOf("com.example.impostor"))
        assertFalse(v.accepted)
        assertTrue(v.why, v.why.contains("com.example.impostor"))
    }

    @Test fun anotherKeyIsNotAccepted() = assertFalse(verdict(certs = setOf("00".repeat(32))).accepted)

    @Test fun anExtraSignerIsNotAccepted() = assertFalse(verdict(certs = key + "11".repeat(32)).accepted)

    @Test fun anythingUnreadableIsNotAccepted() {
        assertFalse(verdict(uid = null).accepted)
        assertFalse(verdict(packages = null).accepted)
        assertFalse(verdict(certs = null).accepted)
        assertFalse(verdict(certs = emptySet()).accepted)
        assertFalse(verdict(module = emptySet()).accepted)
    }

    /** A shared uid holding MyPlay among others is still MyPlay's uid. */
    @Test fun aUidWithSeveralPackagesIncludingMyPlayCounts() =
        assertTrue(verdict(packages = listOf("com.example.other", myplay)).accepted)

    // ---- W1-3: shortened field names ----

    /** As HostInfo would look if its build shortened the names. */
    @Suppress("unused")
    private class Shortened(private val a: String, private val b: Int) {
        companion object { val c: String = "static, not counted" }
    }

    @Suppress("unused")
    private class TwoStrings(private val a: String, private val b: String, private val c: Int)

    @Test fun shortenedNamesFallBackToTheOnlyFieldOfThatType() {
        val info = Shortened(HostCheck.MYPLAY_PACKAGE, 10123)
        assertEquals(HostCheck.MYPLAY_PACKAGE, HostCheck.hostPackage(info))
        assertEquals(10123, HostCheck.hostUid(info))
        assertTrue(HostCheck.isMyPlay(info))
    }

    @Test fun twoCandidateFieldsMeanNothingIsRead() {
        val info = TwoStrings(HostCheck.MYPLAY_PACKAGE, "other", 10123)
        assertNull(HostCheck.hostPackage(info))
        assertFalse(HostCheck.isMyPlay(info))
        assertEquals(10123, HostCheck.hostUid(info))
    }

    @Test fun theRealNamesStillWin() {
        val info = HostInfo("com.example.host", 10123)
        assertEquals("com.example.host", HostCheck.hostPackage(info))
        assertEquals(10123, HostCheck.hostUid(info))
    }

    @Test fun theAppIdIsMyPlays() = assertEquals("io.github.whiterain3.myplay", HostCheck.MYPLAY_PACKAGE)
}
