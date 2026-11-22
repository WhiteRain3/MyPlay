package net.prototux.aare.car

import android.location.Location
import net.prototux.aare.MyPlayBackupKeys
import net.prototux.aare.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** MyPlay (#11): where I parked. */
@RunWith(RobolectricTestRunner::class)
class ParkedHereTest {
    private val now = 1_000_000_000L
    private fun fix(lat: Double, lng: Double, ageMs: Long) = Location("t").apply { latitude = lat; longitude = lng; time = now - ageMs }

    @Test fun theCarsFixFirstThenThePhonesIfFresh() {
        assertEquals(ParkedSpot(54.1, 25.1, now), ParkedHere.pick(fix(54.1, 25.1, 1_000), fix(54.9, 25.9, 1_000), now))
        assertEquals(ParkedSpot(54.9, 25.9, now), ParkedHere.pick(fix(54.1, 25.1, 10 * 60_000), fix(54.9, 25.9, 1_000), now))
        assertNull(ParkedHere.pick(fix(54.1, 25.1, 10 * 60_000), null, now))
        assertNull(ParkedHere.pick(null, null, now))
    }

    @Test fun storedAndLinks() {
        val s = ParkedSpot(54.687157, 25.279652, now)
        assertEquals(s, ParkedHere.decode(ParkedHere.encode(s)))
        assertNull(ParkedHere.decode("junk"))
        assertEquals("google.navigation:q=54.687157,25.279652&mode=w", ParkedHere.walkingUri(s).toString())
        assertEquals("geo:54.687157,25.279652?q=54.687157,25.279652", ParkedHere.geoUri(s).toString())
    }

    @Test fun offByDefaultKeepsNothingAndTurningOffForgets() {
        val settings = Settings(RuntimeEnvironment.getApplication())
        assertFalse(settings.myplayParkedHere)
        ParkedHere.driveEnded(RuntimeEnvironment.getApplication(), settings, fix(54.1, 25.1, 0), null, now)
        assertFalse(settings.prefs.contains(KEY_PARKED_SPOT))
        settings.myplayParkedHere = true
        ParkedHere.driveEnded(RuntimeEnvironment.getApplication(), settings, fix(54.1, 25.1, 0), null, now)
        assertEquals(ParkedSpot(54.1, 25.1, now), ParkedHere.decode(settings.prefs.getString(KEY_PARKED_SPOT, null)))
        settings.myplayParkedHere = false
        assertFalse(settings.prefs.contains(KEY_PARKED_SPOT))
    }

    @Test fun neverBackedUp() {
        assertFalse(KEY_PARKED_HERE in MyPlayBackupKeys.ALLOWED)
        assertFalse(KEY_PARKED_SPOT in MyPlayBackupKeys.ALLOWED)
    }
}
