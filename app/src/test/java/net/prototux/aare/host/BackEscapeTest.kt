package net.prototux.aare.host

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackEscapeTest {
    private val escape = BackEscape()

    /** The app ignored the first Back (same page): the second one within two seconds leaves. */
    @Test fun secondBackOnTheSamePageLeaves() {
        assertFalse(escape.leaves(1_000, "app", "page1"))
        assertTrue(escape.leaves(2_500, "app", "page1"))
    }

    /** The app went back (another page): the second Back is the app's again. */
    @Test fun aPageChangeGivesBackToTheApp() {
        assertFalse(escape.leaves(1_000, "app", "search"))
        assertFalse(escape.leaves(1_500, "app", "map"))
    }

    @Test fun tooLateIsAFirstBackAgain() {
        assertFalse(escape.leaves(1_000, "app", "page1"))
        assertFalse(escape.leaves(3_001, "app", "page1"))
        assertTrue(escape.leaves(4_000, "app", "page1"))
    }

    @Test fun exactlyTwoSecondsStillCounts() {
        assertFalse(escape.leaves(1_000, "app", "page1"))
        assertTrue(escape.leaves(3_000, "app", "page1"))
    }

    @Test fun anotherAppStartsOver() {
        assertFalse(escape.leaves(1_000, "a", "page1"))
        assertFalse(escape.leaves(1_200, "b", "page1"))
    }

    /** After leaving, coming back to the app and pressing Back once does not leave at once. */
    @Test fun leavingForgetsTheFirstBack() {
        assertFalse(escape.leaves(1_000, "app", "page1"))
        assertTrue(escape.leaves(1_500, "app", "page1"))
        assertFalse(escape.leaves(1_800, "app", "page1"))
    }

    /** An app with no page yet (null id) behaves the same. */
    @Test fun noPageIdIsAPageLikeAnyOther() {
        assertFalse(escape.leaves(1_000, "app", null))
        assertTrue(escape.leaves(1_400, "app", null))
    }

    /**
     * Two quick Backs meant as "back two pages": the app has not shown its new page yet when the
     * second comes, so the page looks the same. That Back goes to the app, not home.
     */
    @Test fun aSecondBackTooSoonGoesToTheApp() {
        assertFalse(escape.leaves(1_000, "app", "page2"))
        assertFalse(escape.leaves(1_000 + BackEscape.SETTLE_MS - 1, "app", "page2"))
    }

    /** Once the app had time to answer and still shows the same page, the next Back leaves. */
    @Test fun aSecondBackAfterTheAppHadTimeLeaves() {
        assertFalse(escape.leaves(1_000, "app", "page1"))
        assertTrue(escape.leaves(1_000 + BackEscape.SETTLE_MS, "app", "page1"))
    }

    /** After two quick Backs in an app that ignores them, a third one a moment later still gets out. */
    @Test fun aThirdBackAfterTwoQuickOnesLeaves() {
        assertFalse(escape.leaves(1_000, "app", "page1"))
        assertFalse(escape.leaves(1_100, "app", "page1"))
        assertTrue(escape.leaves(1_600, "app", "page1"))
    }

    /** Quick Backs that do move the app through its pages never leave it. */
    @Test fun quickBacksThroughPagesStayInTheApp() {
        assertFalse(escape.leaves(1_000, "app", "page3"))
        assertFalse(escape.leaves(1_100, "app", "page3"))
        assertFalse(escape.leaves(1_500, "app", "page1"))
    }
}
