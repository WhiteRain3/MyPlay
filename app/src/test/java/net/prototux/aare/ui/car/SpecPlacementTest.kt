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
    private val sizes = listOf(900f to 543f, 1280f to 720f, 1440f to 720f, 1920f to 720f, 2560f to 720f, 800f to 1280f)

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

    // ---- M6: by the screen's shape ----

    @Test fun shapesAreRecognised() {
        assertEquals(SpecPlacement.Shape.SMALL, SpecPlacement.shapeOf(900f, 543f))
        assertEquals(SpecPlacement.Shape.SMALL, SpecPlacement.shapeOf(800f, 1280f))
        assertEquals(SpecPlacement.Shape.NORMAL, SpecPlacement.shapeOf(1280f, 720f))
        assertEquals(SpecPlacement.Shape.WIDE, SpecPlacement.shapeOf(1440f, 720f))
        assertEquals(SpecPlacement.Shape.VERY_WIDE, SpecPlacement.shapeOf(1920f, 720f))
    }

    /** The Audi's screen: few, narrow cards. */
    @Test fun theAudisScreenGetsAtMostThreeNarrowCards() {
        for ((name, spec) in CarLayoutSpec.PRESETS) {
            val p = SpecPlacement.place(spec, 900f, 543f)
            assertTrue(name, p.slots.size <= 3)
            p.slots.filter { it.height < p.content.height * 0.5f }.forEach { assertTrue("$name $it", it.width <= 260f + 0.01f || it.width >= p.content.width * 0.8f) }
        }
    }

    /**
     * M6-1: on the Audi's screen (901 x 543 dp) the cards that are shown fill the column; no hole is
     * left where a dropped card would have been.
     */
    @Test fun theShownCardsFillTheColumnOnTheAudisScreen() {
        val p = SpecPlacement.place(CarLayoutSpec.PRESETS.getValue("A"), 901f, 543f)
        assertEquals(3, p.slots.size)
        assertEquals(p.content.bottom - SpecPlacement.GAP, p.slots.last().bottom, 0.01f)
    }

    @Test fun theShownCardsFillTheRowAlongABottomBar() {
        val spec = CarLayoutSpec.PRESETS.getValue("A").copy(barStyle = BarStyle.SOLID_RAIL, barSide = BarSide.BOTTOM)
        val p = SpecPlacement.place(spec, 901f, 543f)
        assertEquals(3, p.slots.size)
        assertEquals(p.content.right - SpecPlacement.GAP, p.slots.last().right, 0.01f)
    }

    /** A very wide screen: the corner cards sit side by side at the far end, the middle stays free. */
    @Test fun veryWideScreensPutCardsSideBySide() {
        val p = SpecPlacement.place(CarLayoutSpec.PRESETS.getValue("D"), 1920f, 720f)
        assertEquals(4, p.slots.size)
        val left = p.slots.minOf { it.left }
        assertTrue("cards at the far end: $left", left > p.content.left + p.content.width / 2)
        assertEquals(p.slots[0].top, p.slots[2].top)
        assertTrue(p.slots[0].right < p.slots[2].left)
    }

    @Test fun veryWideColumnByTheBarIsTwoColumns() {
        val p = SpecPlacement.place(CarLayoutSpec.PRESETS.getValue("A"), 1920f, 720f)
        assertEquals(2, p.slots.map { it.left }.distinct().size)
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

    // ---- M3-3: the free area for the maps buttons ----

    @Test fun theFreeAreaIsClearOfTheBarAndEveryCard() {
        for ((name, spec) in CarLayoutSpec.PRESETS) for ((w, h) in sizes) {
            val p = SpecPlacement.place(spec, w, h)
            val free = SpecPlacement.freeArea(p)
            assertTrue("$name $w x $h: $free", free.area > 0f)
            assertTrue("$name over the bar", free.intersect(p.bar) == null)
            p.slots.forEach { assertTrue("$name $w x $h: $free over $it", free.intersect(it) == null) }
        }
    }

    @Test fun theMapGoesUnderTheBarOnlyWhenAsked() {
        assertEquals(Box(0f, 0f, 1280f, 720f), SpecPlacement.place(CarLayoutSpec.PRESETS.getValue("D"), 1280f, 720f).map)
        assertEquals(Box(SpecPlacement.BAR, 0f, 1280f, 720f), SpecPlacement.place(CarLayoutSpec.PRESETS.getValue("A"), 1280f, 720f).map)
    }
}
