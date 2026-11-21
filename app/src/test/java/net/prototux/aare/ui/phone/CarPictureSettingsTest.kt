package net.prototux.aare.ui.phone

import androidx.test.core.app.ApplicationProvider
import net.prototux.aare.Settings
import net.prototux.aare.car.SpeedSourceMode
import net.prototux.aare.car.myplaySpeedSource
import net.prototux.aare.ui.car.CarUiState
import net.prototux.aare.ui.car.myplay.myplayEnginePreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CarPictureSettingsTest {
    @Test
    fun thePictureShowsOrbitWhenTriedButNeverReadsTheLocation() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val s = Settings(ctx)
        s.myplayEnginePreview = true
        s.myplaySpeedSource = SpeedSourceMode.AUTO
        val ui = CarUiState()
        applyPictureSettings(ui, s, ctx)
        assertTrue(ui.enginePreview)
        assertEquals(SpeedSourceMode.OFF, ui.speedSource)
    }
}
