package net.prototux.aare.ui.phone

import androidx.test.core.app.ApplicationProvider
import net.prototux.aare.Settings
import net.prototux.aare.car.SpeedSourceMode
import net.prototux.aare.car.myplaySpeedSource
import net.prototux.aare.ui.car.CarUiState
import net.prototux.aare.ui.car.myplay.myplayEnginePreview
import net.prototux.aare.ui.car.myplay.myplayEngineLanguage
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CarPictureLanguageTest {
    @Test
    fun thePictureDrawsTheChosenDesign() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val s = Settings(ctx)
        s.myplayEngineLanguage = "deck"
        val ui = CarUiState()
        applyPictureSettings(ui, s, ctx)
        assertEquals("deck", ui.engineLanguage)
        assertEquals("DECK", engineLanguageName("deck"))
    }
}

/** TH-1c: the phone's picture takes the owner's theme on ORBIT, as the car does. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CarPictureThemeTest {
    @Test
    fun thePictureTakesTheChosenTheme() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val s = Settings(ctx)
        val ui = CarUiState()
        applyPictureSettings(ui, s, ctx)
        assertEquals(false, ui.orbitThemed)
        s.carThemeId = "retro"
        applyPictureSettings(ui, s, ctx)
        assertTrue(ui.orbitThemed)
    }
}
