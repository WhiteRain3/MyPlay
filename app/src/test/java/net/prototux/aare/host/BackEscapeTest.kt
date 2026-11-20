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
        assertTrue(escape.leaves(1_100, "app", null))
    }
}
