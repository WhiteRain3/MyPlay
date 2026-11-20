package net.prototux.aare.host.templates

import net.prototux.aare.host.templates.MapGeometry.Box
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MyPlay (M2): the areas a maps app is told, for the kinds of arrangement the planned layouts
 * produce (plan M1's options 4 and 5). Screen 1280 x 720, the map filling it.
 */
class MapAreasTest {
    private val map = Box(0f, 0f, 1280f, 720f)
    private fun inside(inner: Box, outer: Box) =
        inner.left >= outer.left && inner.top >= outer.top && inner.right <= outer.right && inner.bottom <= outer.bottom

    @Test fun nothingOverTheMapIsTheWholeMap() {
        val a = MapAreas.compute(map, emptyList(), emptyList())
        assertEquals(map, a.visible)
        assertEquals(map, a.stable)
    }

    /** Today's layouts give no reserved places: stable and visible are the same, as before. */
    @Test fun withoutReservedPlacesStableIsVisible() {
        val rail = Box(0f, 0f, 96f, 720f)
        val a = MapAreas.compute(map, listOf(rail), emptyList())
        assertEquals(Box(96f, 0f, 1280f, 720f), a.visible)
        assertEquals(a.visible, a.stable)
    }

    /** A: a rail on the left and a column of cards beside it. */
    @Test fun railAndColumnLeaveTheRightPart() {
        val rail = Box(0f, 0f, 96f, 720f)
        val column = Box(96f, 0f, 456f, 720f)
        val a = MapAreas.compute(map, listOf(rail, column), emptyList())
        assertEquals(Box(456f, 0f, 1280f, 720f), a.visible)
    }

    /** The music card's place is reserved while no music plays: the stable area leaves it out already. */
    @Test fun aReservedSlotShrinksOnlyTheStableArea() {
        val rail = Box(0f, 0f, 96f, 720f)
        val musicSlot = Box(96f, 400f, 456f, 720f)
        val a = MapAreas.compute(map, listOf(rail), listOf(rail, musicSlot))
        assertEquals(Box(96f, 0f, 1280f, 720f), a.visible)
        assertTrue(a.stable.left >= 456f || a.stable.bottom <= 400f)
        assertTrue(inside(a.stable, a.visible))
    }

    /** When the music card then shows, the visible area becomes what the stable area already was. */
    @Test fun whenTheReservedCardShowsVisibleMeetsStable() {
        val rail = Box(0f, 0f, 96f, 720f)
        val musicSlot = Box(96f, 400f, 456f, 720f)
        val before = MapAreas.compute(map, listOf(rail), listOf(rail, musicSlot))
        val after = MapAreas.compute(map, listOf(rail, musicSlot), listOf(rail, musicSlot))
        assertEquals(before.stable, after.stable)
        assertEquals(after.stable, after.visible)
    }

    /** B: a bottom dock and cards in the corners above it. */
    @Test fun bottomDockAndCornerCards() {
        val dock = Box(0f, 620f, 1280f, 720f)
        val topLeft = Box(16f, 16f, 336f, 196f)
        val a = MapAreas.compute(map, listOf(dock, topLeft), emptyList())
        assertEquals(620f, a.visible.bottom)
        assertTrue(a.visible.area > 0f)
        assertTrue(a.visible.intersect(dock) == null && a.visible.intersect(topLeft) == null)
    }

    /** C: a ribbon at the top and one stage card on the right. */
    @Test fun topRibbonAndStageCard() {
        val ribbon = Box(0f, 0f, 1280f, 72f)
        val stage = Box(880f, 72f, 1280f, 720f)
        val a = MapAreas.compute(map, listOf(ribbon, stage), emptyList())
        assertEquals(Box(0f, 72f, 880f, 720f), a.visible)
    }

    /** The stable area is never larger than the visible one, however the rectangles fall. */
    @Test fun stableAlwaysFitsInsideVisible() {
        val shownSets = listOf(listOf(Box(0f, 0f, 400f, 300f)), listOf(Box(900f, 0f, 1280f, 720f), Box(0f, 600f, 600f, 720f)))
        val reservedSets = listOf(listOf(Box(500f, 200f, 800f, 720f)), listOf(Box(0f, 0f, 1280f, 100f), Box(300f, 300f, 700f, 500f)))
        for (shown in shownSets) for (reserved in reservedSets) {
            val a = MapAreas.compute(map, shown, reserved)
            assertTrue("$a", inside(a.stable, a.visible))
        }
    }

    /** M2-2: the stable area never touches a reserved place, and always nests in the visible one. */
    @Test fun stableIsClearOfEveryReservedPlace() {
        val shownSets = listOf(emptyList(), listOf(Box(0f, 0f, 96f, 720f)), listOf(Box(900f, 0f, 1280f, 720f), Box(0f, 600f, 600f, 720f)))
        val reservedSets = listOf(listOf(Box(500f, 200f, 800f, 720f)), listOf(Box(0f, 0f, 1280f, 100f), Box(300f, 300f, 700f, 500f)), listOf(Box(96f, 400f, 456f, 720f)))
        for (shown in shownSets) for (reserved in reservedSets) {
            val a = MapAreas.compute(map, shown, reserved)
            assertTrue("$a", inside(a.stable, a.visible))
            for (r in reserved) assertTrue("$a touches $r", a.stable.intersect(r) == null)
        }
    }

    // ---- M2-1: the registry moves a place between "covers now" and "may cover" ----

    @Test fun aCardAppearingInItsReservedSlotIsReported() {
        val reg = MapCoverRegistry()
        val slot = androidx.compose.ui.geometry.Rect(96f, 400f, 456f, 720f)
        reg.place("music", slot, now = false)
        assertTrue(reg.shown.isEmpty())
        assertEquals(slot, reg.reserved["music"])
        // The card appears exactly where its slot was: no new position, only "now" changes.
        reg.setNow("music", true)
        assertEquals(slot, reg.shown["music"])
        reg.setNow("music", false)
        assertTrue(reg.shown.isEmpty())
        assertEquals(slot, reg.reserved["music"])
    }

    @Test fun nothingToMoveBeforeAPlaceIsKnown() {
        val reg = MapCoverRegistry()
        reg.setNow("bar", true)
        assertTrue(reg.shown.isEmpty() && reg.reserved.isEmpty())
    }

    @Test fun removingForgetsBoth() {
        val reg = MapCoverRegistry()
        reg.place("bar", androidx.compose.ui.geometry.Rect(0f, 0f, 96f, 720f), now = true)
        reg.remove("bar")
        assertTrue(reg.shown.isEmpty() && reg.reserved.isEmpty())
    }

    @Test fun areasAreGivenInTheMapsOwnPixels() =
        assertEquals(listOf(360, 0, 1184, 620), MapAreas.inSurface(Box(96f, 100f, 1280f, 720f), Box(456f, 100f, 1280f, 720f)))
}
