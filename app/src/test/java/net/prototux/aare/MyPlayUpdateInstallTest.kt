package net.prototux.aare

import net.prototux.aare.MyPlayUpdateInstall.Failure
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MyPlayUpdateInstallTest {
    private fun release(vararg assets: Pair<String, String>) = JSONObject().put("assets", JSONArray().apply {
        assets.forEach { (name, url) -> put(JSONObject().put("name", name).put("browser_download_url", url)) }
    })

    private val base = "https://github.com/WhiteRain3/MyPlay/releases/download/myplay-v0.2.0/"

    @Test
    fun theApkIsTakenOnlyFromMyPlaysOwnReleaseOfThatVersion() {
        assertEquals(base + "MyPlay-0.2.0.apk", MyPlayUpdateInstall.apkUrl(release("MyPlay-0.2.0.apk" to base + "MyPlay-0.2.0.apk"), "0.2.0"))
        assertNull(MyPlayUpdateInstall.apkUrl(release("MyPlay-0.2.0.apk" to "https://evil.example/MyPlay-0.2.0.apk"), "0.2.0"))
        assertNull(MyPlayUpdateInstall.apkUrl(release("MyPlay-0.2.0.apk" to base.replace("v0.2.0", "v0.1.9") + "MyPlay-0.2.0.apk"), "0.2.0"))
        assertNull(MyPlayUpdateInstall.apkUrl(release("MyPlay-hostshims-0.2.0.apk" to base + "MyPlay-hostshims-0.2.0.apk"), "0.2.0"))
        assertNull(MyPlayUpdateInstall.apkUrl(JSONObject(), "0.2.0"))
    }

    @Test
    fun theChecksumIsReadFromTheSumsFile() {
        val h = "a".repeat(64)
        val sums = "${"b".repeat(64)}  myplay-headunit-0.2.0.zip\n$h  MyPlay-0.2.0.apk\n"
        assertEquals(h, MyPlayUpdateInstall.expectedSha256(sums, "MyPlay-0.2.0.apk"))
        assertEquals(h, MyPlayUpdateInstall.expectedSha256("${h.uppercase()} *MyPlay-0.2.0.apk", "MyPlay-0.2.0.apk"))
        assertNull(MyPlayUpdateInstall.expectedSha256("short  MyPlay-0.2.0.apk", "MyPlay-0.2.0.apk"))
        assertNull(MyPlayUpdateInstall.expectedSha256(sums, "MyPlay-0.3.0.apk"))
    }

    @Test
    fun theSignersMustBeExactlyTheInstalledOnes() {
        assertTrue(MyPlayUpdateInstall.sameSigners(setOf("k"), setOf("k")))
        assertFalse(MyPlayUpdateInstall.sameSigners(setOf("k"), setOf("other")))
        assertFalse(MyPlayUpdateInstall.sameSigners(setOf("k"), setOf("k", "other")))
        assertFalse(MyPlayUpdateInstall.sameSigners(emptySet(), emptySet()))
    }

    @Test
    fun everyCheckStopsTheInstall() {
        val pkg = "io.github.whiterain3.myplay"
        fun c(sum: String? = "s", actual: String = "s", filePkg: String? = pkg, ver: String? = "0.2.0", signers: Set<String> = setOf("k")) =
            MyPlayUpdateInstall.check(sum, actual, pkg, filePkg, "0.2.0", ver, setOf("k"), signers)
        assertNull(c())
        assertEquals(Failure.CHECKSUM, c(sum = null))
        assertEquals(Failure.CHECKSUM, c(actual = "x"))
        assertEquals(Failure.NOT_MYPLAY, c(filePkg = "com.other"))
        assertEquals(Failure.NOT_MYPLAY, c(filePkg = null))
        assertEquals(Failure.WRONG_VERSION, c(ver = "0.1.0"))
        assertEquals(Failure.WRONG_SIGNER, c(signers = setOf("x")))
    }
}
