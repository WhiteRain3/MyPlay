package net.prototux.aare.car

import androidx.test.core.app.ApplicationProvider
import net.prototux.aare.Settings
import net.prototux.aare.ui.phone.SpeedLimitFiles
import net.prototux.aare.ui.phone.myplaySpeedLimitRegion
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** MyPlay (SL-13): the connection log's line on what the car side sees of the speed limit file (no position). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SpeedLimitFileLineTest {
    private val app get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test fun theLineSaysWhatTheCarSideSees() {
        val limits = FileSpeedLimits(app)
        try {
            assertEquals("speed limits: car side: no region chosen", limits.describe())
            Settings(app).myplaySpeedLimitRegion = "lithuania"
            assertEquals("speed limits: car side: no lithuania file at speed_limits/lithuania.bin", limits.describe())
            val f = SpeedLimitFiles.file(app.filesDir, "lithuania").apply { parentFile?.mkdirs(); writeBytes(ByteArray(1234)) }
            assertEquals("speed limits: car side sees the lithuania file, 1234 bytes", limits.describe())
            f.delete()
        } finally { limits.close() }
    }
}
