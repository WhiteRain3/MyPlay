package net.prototux.aare.ui.car.myplay

import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import net.prototux.aare.Settings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** MyPlay (N10): ORBIT is the car screen by default, and every install is moved to it once. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OrbitDefaultTest {
    private val settings get() = Settings(ApplicationProvider.getApplicationContext())

    @Test fun aNewInstallHasOrbit() {
        assertTrue(settings.myplayEnginePreview)
    }

    @Test fun anInstallThatHadTheOldLayoutIsMovedOnce() {
        // As 0.1.42 stored it: the preview off.
        settings.prefs.edit { putBoolean("myplay_engine_preview", false) }
        assertTrue("moved to ORBIT", settings.myplayEnginePreview)
        // The driver may still choose an older layout afterwards; that choice is kept.
        settings.myplayEnginePreview = false
        assertFalse(settings.myplayEnginePreview)
        assertFalse(Settings(ApplicationProvider.getApplicationContext()).myplayEnginePreview)
    }
}
