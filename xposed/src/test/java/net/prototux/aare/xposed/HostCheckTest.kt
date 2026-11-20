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

    @Test fun theAppIdIsMyPlays() = assertEquals("io.github.whiterain3.myplay", HostCheck.MYPLAY_PACKAGE)
}
