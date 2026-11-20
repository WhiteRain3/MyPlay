package net.prototux.aare.ui.phone

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class BackupPageTest {
    @Test
    fun theFileIsNamedAfterMyPlayAndTheDay() {
        val c = Calendar.getInstance(TimeZone.getDefault()).apply { clear(); set(2026, Calendar.NOVEMBER, 20, 12, 0) }
        assertEquals("myplay-settings-2026-11-20.json", backupFileName(c.time))
    }
}
