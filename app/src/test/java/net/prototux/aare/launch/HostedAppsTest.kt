package net.prototux.aare.launch

import android.os.Handler
import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/** VA-8: an app left on the car is kept a while, then let go; one at a time; never one that didn't start. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HostedAppsTest {
    private class Fake : HostedApps.Launcher {
        override var connected = true
        var detached = 0
        var released = 0
        override fun detach() { detached++ }
        override fun release() { released++ }
    }

    private val apps = HostedApps({ Fake() }, keepMs = 1_000, handler = Handler(Looper.getMainLooper()))

    private fun started(pkg: String) = apps.take(pkg).also { it.displayId = 7 }

    @Test fun comingBackCarriesOn() {
        val yt = started("yt")
        apps.leave(yt)
        assertEquals(1, yt.launcher.detached)
        assertEquals("yt", apps.keptPackage)
        assertSame(yt, apps.take("yt"))
        assertEquals(0, yt.launcher.released)
    }

    @Test fun keptOnlyAWhile() {
        val yt = started("yt")
        apps.leave(yt)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1_001))
        assertEquals(1, yt.launcher.released)
        assertNull(apps.keptPackage)
        assertNotSame(yt, apps.take("yt"))
    }

    @Test fun oneAppKeptAtATime() {
        val yt = started("yt")
        apps.leave(yt)
        val box = apps.take("box")
        assertEquals("another app: the kept one goes", 1, yt.launcher.released)
        box.displayId = 3
        apps.leave(box)
        assertEquals("box", apps.keptPackage)
    }

    @Test fun anAppThatNeverStartedIsNotKept() {
        val yt = apps.take("yt")
        apps.leave(yt)
        assertEquals(1, yt.launcher.released)
        assertNull(apps.keptPackage)
    }

    @Test fun theCarGoingLetsAllGo() {
        val yt = started("yt")
        apps.leave(yt)
        apps.releaseAll()
        assertEquals(1, yt.launcher.released)
        assertNull(apps.keptPackage)
    }
}
