package net.prototux.aare.ui.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SpeedLimitTest {
    private val sha = "a".repeat(64)
    private fun json(format: Int = 1, built: String = "2026-11-01", bytes: Long = 1000, sha256: String = sha) =
        """{"region":"lithuania","format":$format,"built":"$built","bytes":$bytes,"sha256":"$sha256",""" +
            """"source":"© OpenStreetMap contributors, ODbL 1.0","url_licence":"https://www.openstreetmap.org/copyright"}"""

    @Test
    fun aManifestIsReadAsAgreed() {
        val m = SpeedLimitManifest.parse(json())!!
        assertEquals("lithuania", m.region); assertEquals(1000L, m.bytes); assertEquals("2026-11-01", m.built)
        assertEquals("https://www.openstreetmap.org/copyright", m.licenceUrl)
    }

    @Test
    fun aManifestThisVersionCannotUseIsRefused() {
        assertNull(SpeedLimitManifest.parse(json(format = 2)))
        assertNull(SpeedLimitManifest.parse(json(bytes = 0)))
        assertNull(SpeedLimitManifest.parse(json(bytes = SpeedLimitFiles.MAX_BYTES + 1)))
        assertNull(SpeedLimitManifest.parse(json(sha256 = "xyz")))
        assertNull(SpeedLimitManifest.parse(json(built = "November")))
        assertNull(SpeedLimitManifest.parse("not json"))
        assertNull(SpeedLimitManifest.parse(null))
    }

    @Test
    fun theCheckIsMonthlyAndOnlyWithARegion() {
        val month = SpeedLimitFiles.CHECK_EVERY_MS
        assertFalse(SpeedLimitCheck.due("", 0, 1_000))
        assertTrue(SpeedLimitCheck.due("lithuania", 0, 1_000))
        assertFalse(SpeedLimitCheck.due("lithuania", 1_000, 1_000 + month - 1))
        assertTrue(SpeedLimitCheck.due("lithuania", 1_000, 1_000 + month))
        assertTrue(SpeedLimitCheck.due("lithuania", 5_000, 1_000)) // the clock went back
    }

    @Test
    fun aFileIsDownloadedWhenMissingOrNewer() {
        val old = SpeedLimitManifest.parse(json(built = "2026-10-01"))!!
        val new = SpeedLimitManifest.parse(json(built = "2026-11-01"))!!
        assertTrue(SpeedLimitCheck.wanted(null, false, new))
        assertTrue(SpeedLimitCheck.wanted(new, false, new)) // its file was lost
        assertTrue(SpeedLimitCheck.wanted(old, true, new))
        assertFalse(SpeedLimitCheck.wanted(new, true, new))
        assertFalse(SpeedLimitCheck.wanted(new, true, old))
    }

    @Test
    fun aDownloadIsKeptOnlyWhenSizeHashAndHeaderMatch() {
        val m = SpeedLimitManifest.parse(json())!!
        val head = byteArrayOf('M'.code.toByte(), 'P'.code.toByte(), 'S'.code.toByte(), 'L'.code.toByte(), 1)
        assertTrue(SpeedLimitCheck.verified(m, 1000, sha.uppercase(), head))
        assertFalse(SpeedLimitCheck.verified(m, 999, sha, head))
        assertFalse(SpeedLimitCheck.verified(m, 1000, "b".repeat(64), head))
        assertFalse(SpeedLimitCheck.verified(m, 1000, sha, head.copyOf().also { it[4] = 2 }))
        assertFalse(SpeedLimitCheck.verified(m, 1000, sha, "PK\u0003\u0004\u0001".toByteArray()))
        assertFalse(SpeedLimitCheck.headerOk(byteArrayOf()))
    }

    @Test
    fun filesAndAddressesAreWhereTheCarSideLooks() {
        val dir = File("/data/x/files")
        assertEquals(File(dir, "speed_limits/lithuania.bin"), SpeedLimitFiles.file(dir, "lithuania"))
        assertEquals(File(dir, "speed_limits/lithuania.json"), SpeedLimitFiles.manifestFile(dir, "lithuania"))
        assertEquals("https://github.com/WhiteRain3/MyPlay/releases/download/speed-limits/speed-limits-lithuania.json", SpeedLimitFiles.manifestUrl("lithuania"))
        assertEquals("https://github.com/WhiteRain3/MyPlay/releases/download/speed-limits/speed-limits-lithuania.bin", SpeedLimitFiles.fileUrl("lithuania"))
    }

    @Test
    fun everyStateHasWordsOrNone() {
        assertNull(speedLimitStatus(SpeedLimitDownload.State.Idle))
        listOf(SpeedLimitDownload.State.Checking, SpeedLimitDownload.State.Downloading(5), SpeedLimitDownload.State.UpToDate,
            SpeedLimitDownload.State.Done, SpeedLimitDownload.State.NeedsWifi, SpeedLimitDownload.State.Failed, SpeedLimitDownload.State.NotPublished).forEach {
            assertTrue(speedLimitStatus(it) != null)
        }
    }

    @Test
    fun onlyKnownRegionsNameAFile() {
        assertEquals("lithuania", SpeedLimitFiles.known("lithuania"))
        assertEquals("", SpeedLimitFiles.known("../shared_prefs/settings"))
        assertEquals("", SpeedLimitFiles.known("atlantis"))
        val dir = File("/data/x/files")
        listOf("../shared_prefs/x", "a/b", "", "..").forEach { bad ->
            assertTrue(runCatching { SpeedLimitFiles.file(dir, bad) }.isFailure)
            assertTrue(runCatching { SpeedLimitFiles.manifestFile(dir, bad) }.isFailure)
        }
    }

    @Test
    fun onlyAnAnsweredLookWaitsAMonth() {
        assertTrue(SpeedLimitCheck.counts(SpeedLimitDownload.State.Done))
        assertTrue(SpeedLimitCheck.counts(SpeedLimitDownload.State.UpToDate))
        assertTrue(SpeedLimitCheck.counts(SpeedLimitDownload.State.NotPublished))
        assertFalse(SpeedLimitCheck.counts(SpeedLimitDownload.State.Failed))
    }
}

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [35])
class SpeedLimitRegionSettingTest {
    @Test
    fun aRegionFromABackupThatIsNotKnownReadsAsNone() {
        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val s = net.prototux.aare.Settings(ctx)
        s.prefs.edit().putString(KEY_SPEED_LIMIT_REGION, "../shared_prefs/settings").commit()
        assertEquals("", s.myplaySpeedLimitRegion)
        s.myplaySpeedLimitRegion = "lithuania"
        assertEquals("lithuania", s.myplaySpeedLimitRegion)
    }
}

class SpeedSourceChoiceTest {
    @Test
    fun everySourceIsOfferedOnce() {
        assertEquals(net.prototux.aare.car.SpeedSourceMode.entries.toSet(), SPEED_SOURCE_NAMES.map { it.first }.toSet())
        assertEquals(SPEED_SOURCE_NAMES.size, SPEED_SOURCE_NAMES.map { it.first }.distinct().size)
    }
}
