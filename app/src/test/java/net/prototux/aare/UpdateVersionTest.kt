package net.prototux.aare

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateVersionTest {
    @Test fun higherVersionIsNewer() = assertTrue(UpdateChecker.isNewer("1.3.0", "1.2.0"))
    @Test fun sameVersionIsNotNewer() = assertFalse(UpdateChecker.isNewer("1.2.0", "1.2.0"))
    @Test fun olderVersionIsNotNewer() = assertFalse(UpdateChecker.isNewer("1.2.0", "1.3.0"))

    /** Numeric, not lexical: 10 > 9. */
    @Test fun minorIsComparedNumerically() = assertTrue(UpdateChecker.isNewer("1.10.0", "1.9.0"))

    /** A leading "v" on the tag is ignored. */
    @Test fun leadingVIsIgnored() = assertTrue(UpdateChecker.isNewer("v1.2.1", "1.2.0"))

    /** A missing component counts as 0. */
    @Test fun missingPartCountsAsZero() {
        assertFalse(UpdateChecker.isNewer("1.2", "1.2.0"))
        assertTrue(UpdateChecker.isNewer("1.2.1", "1.2"))
    }

    @Test fun majorBumpBeatsEverything() = assertTrue(UpdateChecker.isNewer("2.0.0", "1.9.9"))

    /** MyPlay's release tags: "myplay-v" and then the version. */
    @Test fun myPlayTagGivesItsVersion() {
        assertEquals("0.1.0", UpdateChecker.versionFromTag("myplay-v0.1.0"))
        assertEquals("1.10.2", UpdateChecker.versionFromTag(" myplay-v1.10.2\n"))
    }

    /** Anything else (upstream's "v1.5.0", an empty tag, no number) is not a MyPlay release. */
    @Test fun otherTagsAreNotMyPlayReleases() {
        assertNull(UpdateChecker.versionFromTag("v1.5.0"))
        assertNull(UpdateChecker.versionFromTag("1.5.0"))
        assertNull(UpdateChecker.versionFromTag(""))
        assertNull(UpdateChecker.versionFromTag("myplay-v"))
        assertNull(UpdateChecker.versionFromTag("myplay-vnext"))
        assertNull(UpdateChecker.versionFromTag("myplay-1.0.0"))
    }

    @Test fun myPlayTagVersionCompares() =
        assertTrue(UpdateChecker.isNewer(UpdateChecker.versionFromTag("myplay-v0.2.0")!!, "0.1.9"))

    /** Only pages of MyPlay's own repository are opened; anything else gives the fixed page. */
    @Test fun releasePageStaysInMyPlaysRepository() {
        val fixed = "https://github.com/WhiteRain3/MyPlay/releases/latest"
        val own = "https://github.com/WhiteRain3/MyPlay/releases/tag/myplay-v0.1.0"
        assertEquals(own, UpdateChecker.releasePage(own))
        assertEquals(fixed, UpdateChecker.releasePage(null))
        assertEquals(fixed, UpdateChecker.releasePage(""))
        assertEquals(fixed, UpdateChecker.releasePage("https://example.com/WhiteRain3/MyPlay/"))
        assertEquals(fixed, UpdateChecker.releasePage("https://github.com/WhiteRain3/MyPlayOther/releases"))
        assertEquals(fixed, UpdateChecker.releasePage("https://github.com/prototux/AARE/releases/latest"))
        assertEquals(fixed, UpdateChecker.releasePage("http://github.com/WhiteRain3/MyPlay/releases"))
    }
}
