package net.prototux.aare.ui.phone

import net.prototux.aare.car.MirroredKeys.TurnMode
import org.junit.Assert.assertEquals
import org.junit.Test

class MirrorTurnChoiceTest {
    @Test
    fun anAppsTurnIsStoredAndTheDefaultIsNot() {
        val one = withMirrorTurn("", "com.example.tv", TurnMode.LEFT_RIGHT)
        assertEquals("com.example.tv=left_right", one)
        val two = withMirrorTurn(one, "com.example.music", TurnMode.LEFT_RIGHT)
        assertEquals("com.example.music=left_right;com.example.tv=left_right", two)
        assertEquals("com.example.music=left_right", withMirrorTurn(two, "com.example.tv", TurnMode.UP_DOWN))
    }
}
