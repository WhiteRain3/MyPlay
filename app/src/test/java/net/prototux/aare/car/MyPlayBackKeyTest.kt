package net.prototux.aare.car

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Test

/** MyPlay (IN-1): Back through KeyInjector and HeldKey, as CarSession wires them. */
class MyPlayBackKeyTest {
    private var now = 0L
    private val tasks = ArrayList<Pair<Long, Runnable>>()
    private val scheduler = object : KeyInjector.Scheduler {
        override fun postDelayed(task: Runnable, delayMs: Long) { tasks += now + delayMs to task }
        override fun cancel(task: Runnable) { tasks.removeAll { it.second === task } }
        override fun uptimeMillis() = now
    }

    private fun advance(ms: Long) {
        now += ms
        tasks.filter { it.first <= now }.forEach { tasks.remove(it); it.second.run() }
    }

    private val did = ArrayList<String>()
    private val back = HeldKey({ now }, onPress = { did += "back" }, onHold = { did += "home" })
    // The system's long-press timeout on Android 12 and later.
    private val injector = KeyInjector(400, scheduler, carLongPressOnly = CAR_LONG_PRESS_ONLY) { k ->
        if (k.code == KeyEvent.KEYCODE_BACK) back.onKey(k.down, k.longPress)
    }

    private fun hold(ms: Long, carFlagOnUp: Boolean = false) {
        injector.onKey(KeyEvent.KEYCODE_BACK, down = true, metaState = 0, carLongPress = false)
        advance(ms)
        injector.onKey(KeyEvent.KEYCODE_BACK, down = false, metaState = 0, carLongPress = carFlagOnUp)
    }

    @Test fun backHeldHalfASecondIsStillBack() {
        hold(500)
        assertEquals(listOf("back"), did)
    }

    @Test fun backHeldLongerThanHeldKeysHoldIsHome() {
        hold(HeldKey.HOLD_MS)
        assertEquals(listOf("home"), did)
    }

    @Test fun theCarsOwnLongPressFlagStillGoesThrough() {
        injector.onKey(KeyEvent.KEYCODE_BACK, down = true, metaState = 0, carLongPress = false)
        injector.onKey(KeyEvent.KEYCODE_BACK, down = true, metaState = 0, carLongPress = true)
        injector.onKey(KeyEvent.KEYCODE_BACK, down = false, metaState = 0, carLongPress = false)
        assertEquals(listOf("home"), did)
    }
}
