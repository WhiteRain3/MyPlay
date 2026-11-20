package net.prototux.aare.car

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Test

/** MyPlay (IN-1, R2): Back through KeyInjector and the gesture engine, as CarSession wires them. */
class MyPlayBackKeyTest {
    private var now = 0L
    private val tasks = ArrayList<Pair<Long, () -> Unit>>()

    private fun advance(ms: Long) {
        val end = now + ms
        while (true) {
            val next = tasks.filter { it.first <= end }.minByOrNull { it.first } ?: break
            tasks.remove(next)
            now = next.first
            next.second()
        }
        now = end
    }

    private val scheduler = object : KeyInjector.Scheduler {
        // Back takes no synthetic long press (CAR_LONG_PRESS_ONLY), so nothing is ever posted here.
        override fun postDelayed(task: Runnable, delayMs: Long) = error("unexpected long press for Back")
        override fun cancel(task: Runnable) = Unit
        override fun uptimeMillis() = now
    }

    private val did = ArrayList<String>()
    private val engine = GestureEngine({ GestureEngine.Timings() }, { false }, object : GestureEngine.Clock {
        override fun now() = now
        override fun after(delayMs: Long, task: () -> Unit): () -> Unit {
            val t = (now + delayMs) to task
            tasks += t
            return { tasks.remove(t) }
        }
    }) { g ->
        when (g) {
            is GestureEngine.Gesture.Press -> did += "back"
            is GestureEngine.Gesture.Hold -> did += "home"
            else -> Unit
        }
    }

    // The system's long-press timeout on Android 12 and later.
    private val injector = KeyInjector(400, scheduler, carLongPressOnly = CAR_LONG_PRESS_ONLY) { k ->
        if (k.code == KeyEvent.KEYCODE_BACK) engine.onKey(k.code, k.down, k.longPress)
    }

    private fun hold(ms: Long) {
        injector.onKey(KeyEvent.KEYCODE_BACK, down = true, metaState = 0, carLongPress = false)
        advance(ms)
        injector.onKey(KeyEvent.KEYCODE_BACK, down = false, metaState = 0, carLongPress = false)
    }

    @Test fun backHeldHalfASecondIsStillBack() {
        hold(500)
        assertEquals(listOf("back"), did)
    }

    @Test fun backHeldForTheHoldTimeIsHome() {
        hold(GestureEngine.HOLD_MS)
        assertEquals(listOf("home"), did)
    }

    @Test fun theCarsOwnLongPressFlagStillGoesThrough() {
        injector.onKey(KeyEvent.KEYCODE_BACK, down = true, metaState = 0, carLongPress = false)
        injector.onKey(KeyEvent.KEYCODE_BACK, down = true, metaState = 0, carLongPress = true)
        injector.onKey(KeyEvent.KEYCODE_BACK, down = false, metaState = 0, carLongPress = false)
        assertEquals(listOf("home"), did)
    }
}
