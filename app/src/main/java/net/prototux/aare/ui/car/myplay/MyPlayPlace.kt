package net.prototux.aare.ui.car.myplay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * MyPlay (plan phase N, N1): where the driver is on MyPlay's own car screen, and how the knob moves
 * them. The engine owns this; a skin (ORBIT, DECK, RACE) only draws it.
 *
 * Home is the map with the corner slots. Pushing there opens the chooser of the six worlds; a turn
 * moves the choice (it wraps), a push enters the world, Back goes up one level. Pure, for tests.
 */
enum class World(val id: String) {
    NAVIGATE("navigate"), MUSIC("music"), CALLS("calls"), APPS("apps"), CAR("car"), SETTINGS("settings"),
}

sealed interface Place {
    data object Home : Place
    /** The world chooser, with [selected] ringed. */
    data class Chooser(val selected: World) : Place
    data class InWorld(val world: World) : Place
}

object Moves {
    /** The push: home opens the chooser on [last] (the world last entered), the chooser enters its world. */
    fun push(p: Place, last: World): Place = when (p) {
        Place.Home -> Place.Chooser(last)
        is Place.Chooser -> Place.InWorld(p.selected)
        is Place.InWorld -> p
    }

    /** A turn of [steps] clicks: moves the chooser's choice, wrapping round; elsewhere nothing here. */
    fun turn(p: Place, steps: Int): Place = when (p) {
        is Place.Chooser -> {
            val n = World.entries.size
            Place.Chooser(World.entries[((p.selected.ordinal + steps) % n + n) % n])
        }
        else -> p
    }

    /** Back: a world goes to the chooser on it, the chooser goes home; null at home (Back is then not ours). */
    fun back(p: Place): Place? = when (p) {
        Place.Home -> null
        is Place.Chooser -> Place.Home
        is Place.InWorld -> Place.Chooser(p.world)
    }
}

/** The engine's state for one car screen. */
class MyPlayScreenModel {
    var place by mutableStateOf<Place>(Place.Home)
    /** The world last entered, where the chooser opens next time. */
    var last by mutableStateOf(World.NAVIGATE)

    fun push() {
        val next = Moves.push(place, last)
        if (next is Place.InWorld) last = next.world
        place = next
    }

    fun turn(steps: Int) { place = Moves.turn(place, steps) }

    /** True when Back was used here. */
    fun back(): Boolean {
        place = Moves.back(place) ?: return false
        return true
    }
}
