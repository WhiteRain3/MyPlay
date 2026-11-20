package net.prototux.aare.ui.car

import net.prototux.aare.host.templates.MapGeometry.Box
import net.prototux.aare.ui.car.CarLayoutSpec.BarSide
import net.prototux.aare.ui.car.CarLayoutSpec.BarStyle
import net.prototux.aare.ui.car.CarLayoutSpec.CardPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** MyPlay (M3): slot placement for every preset and every combination of bar and card place. */
class SpecPlacementTest {
    /** The Audi's area as the bench uses it (1280 x 720 at 213 dpi, 80 px right margin) in dp, and two other shapes. */
    private val sizes = listOf(900f to 543f, 1280f to 720f, 1920f to 720f, 800f to 1280f)

    private fun inside(a: Box, b: Box) = a.left >= b.left - 0.01f && a.top >= b.top - 0.01f && a.right <= b.right + 0.01f && a.bottom <= b.bottom + 0.01f

    private fun checkRules(spec: CarLayoutSpec, w: Float, h: Float) {
        val p = SpecPlacement.place(spec, w, h)
        val what = "$spec at $w x $h"
        assertTrue(what, inside(p.bar, Box(0f, 0f, w, h)))
        assertTrue(what, p.content.intersect(p.bar) == null)
        assertTrue(what, p.slots.size <= spec.allowed().cards.size)
        p.slots.forEachIndexed { i, s ->
            assertTrue("$what slot $i $s", s.area > 0f)
            assertTrue("$what slot $i $s in ${p.content}", inside(s, p.content))
            assertTrue("$what slot $i over the bar", s.intersect(p.bar) == null)
            p.slots.drop(i + 1).forEach { o -> assertTrue("$what slots overlap: $s $o", s.intersect(o) == null) }
        }
        if (spec.allowed().mapUnderBar) assertEquals(what, Box(0f, 0f, w, h), p.map) else assertEquals(what, p.content, p.map)
    }

    @Test fun everyPresetKeepsTheRulesOnEveryShape() {
        for ((name, spec) in CarLayoutSpec.PRESETS) for ((w, h) in sizes) checkRules(spec, w, h)
    }

    @Test fun everyCombinationKeepsTheRules() {
        val base = CarLayoutSpec.DEFAULT
        for (style in BarStyle.entries) for (side in BarSide.entries) for (place in CardPlace.entries) for (under in listOf(true, false))
            for ((w, h) in sizes) checkRules(base.copy(barStyle = style, barSide = side, cardPlace = place, mapUnderBar = under), w, h)
    }

    @Test fun aLeftRailSitsOnTheLeftEdge() {
        val p = SpecPlacement.place(CarLayoutSpec.PRESETS.getValue("A"), 1280f, 720f)
        assertEquals(Box(0f, 0f, SpecPlacement.BAR, 720f), p.bar)
        assertEquals(Box(SpecPlacement.BAR, 0f, 1280f, 720f), p.content)
    }

    @Test fun columnByTheBarHasAllFourCardsNextToIt() {
        val p = SpecPlacement.place(CarLayoutSpec.PRESETS.getValue("A"), 1280f, 720f)
        assertEquals(4, p.slots.size)
        p.slots.forEach { assertEquals(SpecPlacement.BAR + SpecPlacement.GAP, it.left) }
    }

    @Test fun floatingPillsStayAGapAwayFromTheEdge() {
        val p = SpecPlacement.place(CarLayoutSpec.PRESETS.getValue("E"), 1280f, 720f)
        assertEquals(720f - SpecPlacement.GAP, p.bar.bottom)
        assertEquals(SpecPlacement.GAP, p.bar.left)
    }

    @Test fun theStageLayoutHasOneSlot() =
        assertEquals(1, SpecPlacement.place(CarLayoutSpec.PRESETS.getValue("C"), 1280f, 720f).slots.size)

    @Test fun aRibbonIsThinnerThanARail() =
        assertEquals(SpecPlacement.RIBBON, SpecPlacement.place(CarLayoutSpec.PRESETS.getValue("C"), 1280f, 720f).bar.height)

    @Test fun aSmallScreenGetsFewerCorners() {
        // On a narrow screen two cards side by side would not fit: only the bar-side corners are used.
        val p = SpecPlacement.place(CarLayoutSpec.PRESETS.getValue("D"), 600f, 543f)
        assertEquals(2, p.slots.size)
    }

    // ---- M4: the knob's order ----

    private fun order(preset: String): List<Box> {
        val p = SpecPlacement.place(CarLayoutSpec.PRESETS.getValue(preset), 1280f, 720f)
        val boxes = listOf(p.bar) + p.slots
        return SpecPlacement.readingOrder(boxes).map { boxes[it] }
    }

    @Test fun aLeftRailComesFirstThenTheCardsTopToBottom() {
        val o = order("A")
        assertEquals(SpecPlacement.place(CarLayoutSpec.PRESETS.getValue("A"), 1280f, 720f).bar, o.first())
        assertTrue(o.drop(1).zipWithNext().all { (a, b) -> a.top < b.top })
    }

    @Test fun cornersGoAcrossTheTopRowFirst() {
        // D: rail on the left, cards in the corners: rail, top-left, top-right, bottom-left, bottom-right.
        val o = order("D").drop(1)
        assertEquals(o[0].top, o[1].top)
        assertTrue(o[0].left < o[1].left)
        assertTrue(o[2].top > o[0].top && o[2].left < o[3].left)
    }

    @Test fun aBottomBarComesLast() {
        val p = SpecPlacement.place(CarLayoutSpec.PRESETS.getValue("E"), 1280f, 720f)
        assertEquals(p.bar, order("E").last())
    }

    @Test fun aTopRibbonComesFirst() {
        val p = SpecPlacement.place(CarLayoutSpec.PRESETS.getValue("C"), 1280f, 720f)
        assertEquals(p.bar, order("C").first())
    }

    @Test fun theMapGoesUnderTheBarOnlyWhenAsked() {
        assertEquals(Box(0f, 0f, 1280f, 720f), SpecPlacement.place(CarLayoutSpec.PRESETS.getValue("D"), 1280f, 720f).map)
        assertEquals(Box(SpecPlacement.BAR, 0f, 1280f, 720f), SpecPlacement.place(CarLayoutSpec.PRESETS.getValue("A"), 1280f, 720f).map)
    }
}
