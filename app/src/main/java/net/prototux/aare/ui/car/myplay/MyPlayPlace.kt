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

    /** A turn of [steps] clicks: moves the chooser's choice among [worlds], wrapping round; elsewhere nothing here. */
    fun turn(p: Place, steps: Int, worlds: List<World> = World.entries): Place = when (p) {
        // HN-3: from the dial's middle (a world not round it), a click forward is the first world, back the last.
        is Place.Chooser -> {
            val n = worlds.size
            val i = worlds.indexOf(p.selected)
            if (i < 0 && steps != 0) Place.Chooser(worlds[((if (steps > 0) steps - 1 else steps) % n + n) % n])
            else Place.Chooser(worlds[((i.coerceAtLeast(0) + steps) % n + n) % n])
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

/**
 * N13: a hand of cards (DECK): a knob click moves the front card one along [hand]; null past
 * either end (the ring then leaves the hand for the other home stops). Pure.
 */
object HandMoves {
    fun turn(hand: List<World>, front: World, step: Int): World? = hand.getOrNull(hand.indexOf(front) + step)

    /** Back round to the other end, when there is no other stop to go to. */
    fun wrap(hand: List<World>, step: Int): World = if (step > 0) hand.first() else hand.last()
}

/** The engine's state for one car screen. */
/** N6: the Apps world's pages and the knob's walk through all its apps. Pure. */
object AppsPages {
    fun pages(count: Int, perPage: Int): Int = if (count <= 0) 1 else (count + perPage - 1) / perPage
    fun pageOf(index: Int, perPage: Int): Int = if (index < 0) 0 else index / perPage

    /** The app [steps] knob clicks from [index] among [count], wrapping round all pages. */
    fun knob(index: Int, steps: Int, count: Int): Int = if (count <= 0) 0 else ((index + steps) % count + count) % count
}

class MyPlayScreenModel(
    /** N13: the skin's hand (DECK), or null for the hub and its dial (ORBIT). */
    val hand: List<World>? = null,
    /** O7: the dial's worlds (ORBIT: four, one per pad direction), or null for all six. */
    val dial: List<World>? = null,
    /** HN-3: the world in the dial's middle (ORBIT: Apps): the dial opens on it, a push opens it. */
    val centre: World? = null,
) {
    var place by mutableStateOf<Place>(Place.Home)
    /** The world last entered, where the chooser opens next time. */
    var last by mutableStateOf(World.NAVIGATE)
    /** N13: the hand's front card at home (the last card at first: DECK's sheet has Music in front). */
    var front by mutableStateOf(hand?.lastOrNull() ?: World.NAVIGATE)

    /** HN-3: the world shown was opened from the dial (Back is then the dial), not from the hand or the home pad (Back is home). */
    private var fromDial = false

    /** N13: open [world] from the hand (or, O6, the home pad). */
    fun enter(world: World) {
        last = world
        front = world
        place = Place.InWorld(world)
        fromDial = false
    }

    fun push() {
        // O7: the dial opens on the world last entered when it is on the dial, else on its first.
        val next = Moves.push(place, centre ?: if (dial == null || last in dial) last else dial.first())
        if (next is Place.InWorld) { last = next.world; fromDial = true }
        place = next
    }

    fun turn(steps: Int) { place = Moves.turn(place, steps, dial ?: World.entries) }

    /** O7: open [world] from the dial (a pad direction), as a push on it would. */
    fun choose(world: World) {
        last = world
        place = Place.InWorld(world)
        fromDial = true
    }

    /** True when Back was used here. */
    fun back(): Boolean {
        val p = place
        // N13: with a hand there is no dial: a world goes straight home, its card in front.
        if (hand != null && p is Place.InWorld) { front = p.world; place = Place.Home; return true }
        // O7, HN-3: with a dial, a world opened from the home pad (not from the dial) goes straight home.
        if (dial != null && p is Place.InWorld && !fromDial) { place = Place.Home; return true }
        place = Moves.back(place) ?: return false
        return true
    }
}
