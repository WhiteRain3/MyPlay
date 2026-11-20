package net.prototux.aare.ui.car

import net.prototux.aare.ui.car.CarLayoutSpec.BarSide
import net.prototux.aare.ui.car.CarLayoutSpec.BarStyle
import net.prototux.aare.ui.car.CarLayoutSpec.CardPlace
import net.prototux.aare.ui.car.CarLayoutSpec.SpecCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** MyPlay (M1): the layout as data, against the table in docs/plans/myplay.md, phase M. */
class CarLayoutSpecTest {
    private val p = CarLayoutSpec.PRESETS
    private val allFour = listOf(SpecCard.NEXT_TURN, SpecCard.ARRIVAL, SpecCard.MUSIC, SpecCard.CLOCK_STATUS)

    @Test fun thereAreSixPresetsAToF() = assertEquals(listOf("A", "B", "C", "D", "E", "F"), p.keys.toList())

    @Test fun presetsMatchTheTable() {
        // | Preset | map | under | bar | side | cards | see | which | hide |
        fun check(name: String, under: Boolean, bar: BarStyle, side: BarSide, place: CardPlace, see: Int, oneExtra: Boolean, hideMusic: Int?) {
            val s = p.getValue(name)
            assertTrue(name, s.mapBackground)
            assertEquals(name, under, s.mapUnderBar)
            assertEquals(name, bar, s.barStyle)
            assertEquals(name, side, s.barSide)
            assertEquals(name, place, s.cardPlace)
            assertEquals(name, see, s.cardSeeThrough)
            assertEquals(name, allFour, s.cards)
            assertEquals(name, oneExtra, s.oneExtraAtATime)
            assertEquals(name, hideMusic, s.hideMusicAfterS)
            assertNull(name, s.hideArrivalAfterS)
        }
        check("A", false, BarStyle.SOLID_RAIL, BarSide.LEFT, CardPlace.COLUMN_BY_BAR, 14, false, null)
        check("B", false, BarStyle.BOTTOM_DOCK, BarSide.BOTTOM, CardPlace.CORNERS, 14, false, null)
        check("C", false, BarStyle.TOP_RIBBON, BarSide.TOP, CardPlace.STAGE, 14, true, null)
        check("D", true, BarStyle.SEE_THROUGH_RAIL, BarSide.LEFT, CardPlace.CORNERS, 40, false, null)
        check("E", true, BarStyle.FLOATING_PILLS, BarSide.BOTTOM, CardPlace.CORNERS, 40, false, 15)
        check("F", true, BarStyle.FLOATING_PILLS, BarSide.RIGHT, CardPlace.TOP_BANNER_AND_CORNER, 40, false, null)
    }

    @Test fun everyPresetKeepsTheRules() = p.forEach { (name, s) -> assertEquals(name, s, s.allowed()) }

    @Test fun everyPresetRoundTripsThroughTheSettingsText() =
        p.forEach { (name, s) -> assertEquals(name, s, CarLayoutSpec.decode(CarLayoutSpec.encode(s))) }

    @Test fun theDefaultIsDUntilTheOwnerPicks() {
        assertEquals("D", CarLayoutSpec.DEFAULT_PRESET)
        assertEquals(p.getValue("D"), CarLayoutSpec.decode(null))
        assertEquals(p.getValue("D"), CarLayoutSpec.decode(""))
    }

    @Test fun aDockOnTheLeftMovesToTheBottom() =
        assertEquals(BarSide.BOTTOM, p.getValue("A").copy(barStyle = BarStyle.BOTTOM_DOCK).allowed().barSide)

    @Test fun aRibbonAtTheBottomMovesToTheTop() =
        assertEquals(BarSide.TOP, p.getValue("E").copy(barStyle = BarStyle.TOP_RIBBON).allowed().barSide)

    @Test fun otherBarsKeepTheirSide() {
        for (style in listOf(BarStyle.SOLID_RAIL, BarStyle.SEE_THROUGH_RAIL, BarStyle.FLOATING_PILLS)) {
            for (side in BarSide.entries) assertEquals("$style $side", side, p.getValue("A").copy(barStyle = style, barSide = side).allowed().barSide)
        }
    }

    @Test fun noMapMeansNothingGoesUnderTheBar() = assertFalse(p.getValue("D").copy(mapBackground = false).allowed().mapUnderBar)

    @Test fun oneExtraAtATimeOnlyWithTheStageCard() = assertFalse(p.getValue("C").copy(cardPlace = CardPlace.CORNERS).allowed().oneExtraAtATime)

    @Test fun amountsAreKeptInRange() {
        val s = p.getValue("D").copy(cardSeeThrough = 140, hideMusicAfterS = 2, hideArrivalAfterS = 600).allowed()
        assertEquals(100, s.cardSeeThrough)
        assertEquals(CarLayoutSpec.HIDE_MIN_S, s.hideMusicAfterS)
        assertEquals(CarLayoutSpec.HIDE_MAX_S, s.hideArrivalAfterS)
        assertEquals(0, p.getValue("D").copy(cardSeeThrough = -5).allowed().cardSeeThrough)
    }

    @Test fun cardsAppearOnceAndNeverNone() {
        assertEquals(listOf(SpecCard.MUSIC, SpecCard.NEXT_TURN), p.getValue("D").copy(cards = listOf(SpecCard.MUSIC, SpecCard.NEXT_TURN, SpecCard.MUSIC)).allowed().cards)
        assertEquals(listOf(SpecCard.NEXT_TURN), p.getValue("D").copy(cards = emptyList()).allowed().cards)
    }

    @Test fun unknownOrMissingOptionsTakeTheFallback() {
        val d = p.getValue("D")
        val s = CarLayoutSpec.decode("bar=hovercraft;see=abc;side=left;cards=music,warp", d)
        assertEquals(d.barStyle, s.barStyle)
        assertEquals(d.cardSeeThrough, s.cardSeeThrough)
        assertEquals(listOf(SpecCard.MUSIC), s.cards)
        assertEquals(d.mapUnderBar, s.mapUnderBar)
    }

    @Test fun decodingKeepsTheRules() =
        assertEquals(BarSide.BOTTOM, CarLayoutSpec.decode("bar=bottom_dock;side=left").barSide)

    @Test fun neverHidingIsKept() {
        val e = p.getValue("E")
        assertNull(CarLayoutSpec.decode(CarLayoutSpec.encode(e.copy(hideMusicAfterS = null))).hideMusicAfterS)
    }
}
