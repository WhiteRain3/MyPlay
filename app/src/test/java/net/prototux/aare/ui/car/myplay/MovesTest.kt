package net.prototux.aare.ui.car.myplay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** MyPlay (N1): the engine's screen model. */
class MovesTest {
    @Test fun pushOpensTheChooserOnTheLastWorldThenEnters() {
        assertEquals(Place.Chooser(World.MUSIC), Moves.push(Place.Home, World.MUSIC))
        assertEquals(Place.InWorld(World.CALLS), Moves.push(Place.Chooser(World.CALLS), World.MUSIC))
        assertEquals(Place.InWorld(World.CAR), Moves.push(Place.InWorld(World.CAR), World.MUSIC))
    }

    @Test fun turnsMoveTheChoiceAndWrap() {
        assertEquals(Place.Chooser(World.MUSIC), Moves.turn(Place.Chooser(World.NAVIGATE), 1))
        assertEquals(Place.Chooser(World.SETTINGS), Moves.turn(Place.Chooser(World.NAVIGATE), -1))
        assertEquals(Place.Chooser(World.NAVIGATE), Moves.turn(Place.Chooser(World.SETTINGS), 1))
        assertEquals(Place.Chooser(World.CALLS), Moves.turn(Place.Chooser(World.NAVIGATE), 2 + 6 * 3))
        assertEquals(Place.Home, Moves.turn(Place.Home, 1))
    }

    @Test fun backGoesUpOneLevelAndIsNotOursAtHome() {
        assertEquals(Place.Chooser(World.APPS), Moves.back(Place.InWorld(World.APPS)))
        assertEquals(Place.Home, Moves.back(Place.Chooser(World.APPS)))
        assertNull(Moves.back(Place.Home))
    }

    @Test fun theModelRemembersTheLastWorld() {
        val m = MyPlayScreenModel()
        m.push(); m.turn(1); m.push()
        assertEquals(Place.InWorld(World.MUSIC), m.place)
        assertTrue(m.back()); assertTrue(m.back())
        assertEquals(Place.Home, m.place)
        assertFalse(m.back())
        m.push()
        assertEquals(Place.Chooser(World.MUSIC), m.place)
    }
}
