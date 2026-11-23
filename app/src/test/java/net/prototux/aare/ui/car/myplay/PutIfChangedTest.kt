package net.prototux.aare.ui.car.myplay

import androidx.compose.runtime.mutableStateMapOf
import org.junit.Assert.assertSame
import org.junit.Test

class PutIfChangedTest {
    @Test fun anEqualBoxIsNotWrittenAgain() {
        val boxes = mutableStateMapOf<String, FocusGraph.Box>()
        val first = FocusGraph.Box(1f, 2f, 3f, 4f)
        boxes.putIfChanged("a", first)
        boxes.putIfChanged("a", FocusGraph.Box(1f, 2f, 3f, 4f))
        assertSame(first, boxes["a"])
    }

    @Test fun aMovedBoxIsWritten() {
        val boxes = mutableStateMapOf<String, FocusGraph.Box>()
        boxes.putIfChanged("a", FocusGraph.Box(1f, 2f, 3f, 4f))
        val moved = FocusGraph.Box(5f, 2f, 7f, 4f)
        boxes.putIfChanged("a", moved)
        assertSame(moved, boxes["a"])
    }
}
