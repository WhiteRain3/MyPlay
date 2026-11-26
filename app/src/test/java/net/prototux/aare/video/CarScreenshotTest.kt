package net.prototux.aare.video

import android.graphics.Rect
import androidx.test.core.app.ApplicationProvider
import net.prototux.aare.car.ButtonAction
import net.prototux.aare.car.ButtonTable
import net.prototux.aare.ui.car.myplay.HomePad
import net.prototux.aare.ui.car.myplay.HomePadAction
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import android.os.Looper

/**
 * MyPlay (car screenshot): taken only while a car screen is attached, never on its own, and on no
 * button or pad key until the owner puts it there.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CarScreenshotTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val attached = ArrayList<CarScreenshot.Shooter>()

    private fun attach(s: CarScreenshot.Shooter) { attached += s; CarScreenshot.attach(s) }

    @After fun tearDown() { attached.forEach(CarScreenshot::detach) }

    @Test fun liveOnlyWhileAScreenIsAttached() {
        assertFalse(CarScreenshot.live.value)
        val s = CarScreenshot.Shooter { it(null) }
        attach(s)
        assertTrue(CarScreenshot.live.value)
        CarScreenshot.detach(s)
        assertFalse(CarScreenshot.live.value)
    }

    @Test fun aStaleScreenDoesNotDetachTheNewOne() {
        val old = CarScreenshot.Shooter { it(null) }
        val now = CarScreenshot.Shooter { it(null) }
        attach(old); attach(now)
        CarScreenshot.detach(old)
        assertTrue(CarScreenshot.live.value)
    }

    @Test fun withoutACarNothingIsTaken() {
        assertFalse(CarScreenshot.take(context))
    }

    @Test fun aMissedFrameLetsTheNextOneBeTaken() {
        var shots = 0
        attach(CarScreenshot.Shooter { shots++; it(null) })
        assertTrue(CarScreenshot.take(context))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, shots)
        assertTrue(CarScreenshot.take(context))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(2, shots)
    }

    @Test fun nothingIsTakenWithoutBeingAsked() {
        var shots = 0
        attach(CarScreenshot.Shooter { shots++; it(null) })
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0, shots)
    }

    @Test fun theCropIsTheVisibleArea() {
        assertEquals(Rect(10, 0, 790, 480), CarFrame.cropIn(Rect(10, 0, 790, 480), 800, 480))
        assertNull("no margins: the whole frame", CarFrame.cropIn(Rect(0, 0, 800, 480), 800, 480))
        assertNull(CarFrame.cropIn(null, 800, 480))
        assertNull("empty", CarFrame.cropIn(Rect(400, 0, 400, 480), 800, 480))
        assertEquals("kept inside the frame", Rect(0, 0, 800, 470), CarFrame.cropIn(Rect(-5, 0, 900, 470), 800, 480))
    }

    @Test fun onNoButtonOrPadKeyByDefault() {
        assertFalse(HomePadAction.CAR_SCREENSHOT in HomePad.DEFAULT.values)
        assertFalse(ButtonAction.CAR_SCREENSHOT in ButtonTable.DEFAULTS.values)
        assertEquals(HomePadAction.CAR_SCREENSHOT, HomePadAction.byId("car_screenshot"))
        assertEquals(ButtonAction.CAR_SCREENSHOT, ButtonAction.byId("car_screenshot"))
    }
}
