package net.prototux.aare.car

import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.prototux.aare.Settings
import net.prototux.aare.host.CarRuntime
import net.prototux.aare.media.MediaApp
import net.prototux.aare.media.MediaHub
import net.prototux.aare.media.PlaybackInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/**
 * MyPlay (Preview): the car's input path on a CarRuntime without a car, as CarSession runs it:
 * the screen guard for media keys, plain keys to the screen, the knob's turns, remaps, learned
 * buttons, the button table's press / double / triple, and the car's own keys.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CarInputsTest {
    private class FakeMedia : MediaHub {
        var playPauses = 0; var nexts = 0; var previouses = 0; var pauses = 0
        override val apps = MutableStateFlow<List<MediaApp>>(emptyList())
        override val playback: StateFlow<PlaybackInfo?> = MutableStateFlow(null)
        override fun select(app: MediaApp) {}
        override fun playPause() { playPauses++ }
        override fun next() { nexts++ }
        override fun previous() { previouses++ }
        override fun pause() { pauses++ }
        @Composable override fun Screen(app: MediaApp, driving: Boolean, modifier: Modifier) {}
        override fun release() {}
    }

    private class FakeHost : CarInputs.Host {
        override var hasScreen = true
        override var closed = false
        val events = ArrayList<KeyEvent>()
        var exits = 0
        override fun dispatchKey(event: KeyEvent) { events += event }
        override fun screenBack() = false
        override fun exitToCar() { exits++ }
    }

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val settings = Settings(context)
    private val media = FakeMedia()
    private val host = FakeHost()
    private val main = Handler(Looper.getMainLooper())

    /** Built after a test's settings: CarInputs reads them when made, as a session does. */
    private val inputs by lazy {
        val runtime = CarRuntime(context, CoroutineScope(Dispatchers.Unconfined), settings, media, ownsMedia = false, primary = false)
        CarInputs(context, settings, runtime, media, main, host)
    }

    private fun tap(code: Int) { inputs.key(code, true); inputs.key(code, false) }
    private fun idle(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
    private fun settle() = idle(settings.gestureTimings.doubleMs * 3 + 50)
    private fun codes() = host.events.map { it.keyCode to it.action }

    @Test fun aMediaKeyWaitsForTheCarScreen() {
        host.hasScreen = false
        tap(KeyEvent.KEYCODE_MEDIA_NEXT)
        assertEquals(0, media.nexts)
        host.hasScreen = true
        tap(KeyEvent.KEYCODE_MEDIA_NEXT)
        assertEquals(1, media.nexts)
    }

    @Test fun aPlainKeyGoesToTheScreenAsItself() {
        tap(KeyEvent.KEYCODE_DPAD_UP)
        assertEquals(listOf(KeyEvent.KEYCODE_DPAD_UP to KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP to KeyEvent.ACTION_UP), codes())
    }

    @Test fun theKnobsTurnsMoveTheFocusWithTab() {
        inputs.rotary(2)
        assertEquals(4, host.events.size)
        assertTrue(host.events.all { it.keyCode == KeyEvent.KEYCODE_TAB && !it.isShiftPressed })
        host.events.clear()
        inputs.rotary(-1)
        assertEquals(listOf(KeyEvent.KEYCODE_TAB to KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB to KeyEvent.ACTION_UP), codes())
        assertTrue(host.events.all { it.isShiftPressed })
    }

    @Test fun aRemapWinsOverTheKeysOwnAction() {
        settings.carKeyMap = mapOf(KeyEvent.KEYCODE_MEDIA_NEXT to CarKeyAction.PREVIOUS.id)
        tap(KeyEvent.KEYCODE_MEDIA_NEXT)
        assertEquals(0, media.nexts)
        assertEquals(1, media.previouses)
    }

    @Test fun aLearnedButtonDoesItsActionOnRelease() {
        settings.myplayLearnedButtons = mapOf(KeyEvent.KEYCODE_BUTTON_1 to ButtonAction.PLAY_PAUSE)
        inputs.key(KeyEvent.KEYCODE_BUTTON_1, true)
        assertEquals(0, media.playPauses)
        inputs.key(KeyEvent.KEYCODE_BUTTON_1, false)
        assertEquals(1, media.playPauses)
        assertTrue("nothing reaches the screen", host.events.isEmpty())
    }

    @Test fun aKnobPushPressesTheRingsItemOnceNoSecondPushCame() {
        tap(KeyEvent.KEYCODE_DPAD_CENTER)
        assertTrue("waits for a second push", host.events.isEmpty())
        settle()
        assertEquals(listOf(KeyEvent.KEYCODE_DPAD_CENTER to KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER to KeyEvent.ACTION_UP), codes())
    }

    @Test fun twoKnobPushesAreTheNextSong() {
        tap(KeyEvent.KEYCODE_DPAD_CENTER); tap(KeyEvent.KEYCODE_DPAD_CENTER)
        settle()
        assertEquals(1, media.nexts)
        assertTrue(host.events.isEmpty())
    }

    @Test fun threeKnobPushesAreThePreviousSong() {
        repeat(3) { tap(KeyEvent.KEYCODE_DPAD_CENTER) }
        settle()
        assertEquals(1, media.previouses)
        assertEquals(0, media.nexts)
    }

    @Test fun theWheelsClickIsPlayAndPause() {
        tap(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        settle()
        assertEquals(1, media.playPauses)
    }

    @Test fun theCarsRadioKeyHandsTheScreenBack() {
        tap(CarInputs.KEYCODE_RADIO)
        assertEquals(1, host.exits)
    }

    @Test fun theControlsCodesAreThisCarsButtonTable() {
        val codes = inputs.controlCodes()
        assertEquals(KeyEvent.KEYCODE_DPAD_CENTER, codes[ButtonTable.Control.KNOB_PUSH])
        assertEquals(KeyEvent.KEYCODE_BACK, codes[ButtonTable.Control.BACK])
        assertEquals(KeyEvent.KEYCODE_MENU, codes[ButtonTable.Control.MENU])
    }

    @Test fun onlyAPreviewRuntimeCanPretendASpeed() {
        val preview = CarRuntime(context, CoroutineScope(Dispatchers.Unconfined), settings, media, ownsMedia = false, primary = false)
        preview.pretendSpeed(72.4f)
        assertEquals(72, preview.ui.pretendSpeed?.kmh)
        preview.pretendSpeed(null)
        assertEquals(null, preview.ui.pretendSpeed?.kmh)
        val session = CarRuntime(context, CoroutineScope(Dispatchers.Unconfined), settings, media, ownsMedia = false, primary = true)
        session.pretendSpeed(50f)
        assertEquals(null, session.ui.pretendSpeed)
    }

    @Test fun aClosedHostRunsNoGestureLeftWaiting() {
        tap(KeyEvent.KEYCODE_DPAD_CENTER)
        host.closed = true
        settle()
        assertTrue(host.events.isEmpty())
    }
}
