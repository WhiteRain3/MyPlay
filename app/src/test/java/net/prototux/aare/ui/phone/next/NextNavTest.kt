package net.prototux.aare.ui.phone.next

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NextNavTest {
    @Test fun everyRouteHasOneTab() {
        for (r in NextRoutes.ALL) assertTrue(r, NextRoutes.tabOf(r) != null)
        assertEquals(NextTab.MORE, NextRoutes.tabOf(NextRoutes.car("abc")))
        assertNull(NextRoutes.tabOf("nowhere"))
        assertNull(NextRoutes.tabOf("more/cars/"))
        assertNull(NextRoutes.tabOf("more/cars/a/b"))
    }

    @Test fun backPopsWithinTheTabThenGoesHomeThenCloses() {
        var s = NavState().selectTab(NextTab.CAR).open("car/buttons").open("car/buttons/teach")
        s = s.back()!!
        assertEquals("car/buttons", s.route)
        s = s.back()!!
        assertEquals("car", s.route)
        s = s.back()!!
        assertEquals(NextTab.HOME, s.tab)
        assertNull(s.back())
    }

    @Test fun switchingTabsKeepsEachStack() {
        val s = NavState().selectTab(NextTab.CAR).open("car/speed").selectTab(NextTab.MORE).open("more/backup")
            .selectTab(NextTab.CAR)
        assertEquals("car/speed", s.route)
        assertEquals("more/backup", s.selectTab(NextTab.MORE).route)
    }

    @Test fun tappingTheCurrentTabGoesToItsRoot() {
        val s = NavState().selectTab(NextTab.CAR).open("car/speed").selectTab(NextTab.CAR)
        assertEquals("car", s.route)
        assertTrue(s.atRoot)
    }

    @Test fun aCrossTabLinkReturnsOnceToWhereItWasTapped() {
        var s = NavState().selectTab(NextTab.CAR).open("car/maps").open("screen/map-colours")
        assertEquals(NextTab.SCREEN, s.tab)
        assertEquals("screen/map-colours", s.route)
        s = s.back()!!
        assertEquals(NextTab.CAR, s.tab)
        assertEquals("car/maps", s.route)
        // The Screen tab behaves normally afterwards: its root again.
        assertEquals("screen", s.selectTab(NextTab.SCREEN).route)
    }

    @Test fun aLinkInsideTheOtherTabGoesBackNormallyAfterTheReturn() {
        var s = NavState().open("more/updates") // from Home
        s = s.open("more/whats-new")
        s = s.back()!!
        assertEquals(NextTab.MORE, s.tab)
        assertEquals("more/updates", s.route)
        s = s.back()!!
        assertEquals(NextTab.HOME, s.tab)
    }

    @Test fun deepLinkBuildsTheParentPages() {
        val s = NavState().deepLink("more/advanced/certificates")
        assertEquals(NextTab.MORE, s.tab)
        assertEquals(listOf("more", "more/advanced", "more/advanced/certificates"), s.stack().map { it.route })
        assertEquals(NextTab.HOME, NavState().selectTab(NextTab.CAR).deepLink("bogus").tab)
    }

    @Test fun unknownRoutesDoNothing() {
        val s = NavState().selectTab(NextTab.CAR)
        assertEquals(s, s.open("car/nope"))
    }

    @Test fun openingTheSamePageTwiceDoesNotStackIt() {
        val s = NavState().selectTab(NextTab.CAR).open("car/speed").open("car/speed")
        assertEquals(2, s.stack().size)
    }

    @Test fun savesAndRestores() {
        val s = NavState().selectTab(NextTab.CAR).open("car/maps").open("screen/map-colours")
            .selectTab(NextTab.MORE).open(NextRoutes.car("Sedan 1"))
        assertEquals(s, NavState.restore(s.save()))
        assertEquals(NavState(), NavState.restore(null))
        assertEquals(NavState(), NavState.restore(listOf("WAT")))
        // A route saved under the wrong tab is dropped; the root is always there.
        val odd = NavState.restore(listOf("CAR", "#CAR", "more/about|", "car/speed|"))
        assertEquals(listOf("car", "car/speed"), odd.stack().map { it.route })
    }
}

class NextTapsTest {
}
