package net.prototux.aare.ui.car

import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** MyPlay (the owner's fullscreen video, FS-1): the browser's fullscreen state, without a WebView. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BrowserFullscreenTest {
    private val changes = mutableListOf<Boolean>()
    private val full = BrowserFullscreen { changes += it }
    private fun view() = View(ApplicationProvider.getApplicationContext())

    @Test fun aVideoGoesFullscreenAndBackLeavesIt() {
        var told = 0
        val v = view()
        full.show(v) { told++ }
        assertSame(v, full.view)
        assertEquals(listOf(true), changes)
        full.leave()
        assertNull(full.view)
        assertEquals("the page is told", 1, told)
        assertEquals(listOf(true, false), changes)
        full.leave() // nothing more
        assertEquals(1, told)
        assertEquals(listOf(true, false), changes)
    }

    @Test fun aSecondRequestIsRefused() {
        val first = view()
        var refused = 0
        full.show(first) {}
        full.show(view()) { refused++ }
        assertSame(first, full.view)
        assertEquals(1, refused)
        assertEquals(listOf(true), changes)
    }

    /** FS-1: driving (or the WebView going) leaves fullscreen; a dead WebView's callback must not throw out. */
    @Test fun leavingWithADeadPageIsSafe() {
        full.show(view()) { throw IllegalStateException("WebView destroyed") }
        full.leave()
        assertNull(full.view)
        assertEquals(listOf(true, false), changes)
    }

    @Test fun thePageClosingItItselfIsNotToldBack() {
        var told = 0
        full.show(view()) { told++ }
        full.hidden()
        assertNull(full.view)
        assertEquals(0, told)
        assertEquals(listOf(true, false), changes)
    }
}
