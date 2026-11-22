package net.prototux.aare.ui.phone

import net.prototux.aare.car.ButtonAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeachButtonSectionTest {
    @Test
    fun theKnobsOwnActionsAndNothingAreNotOffered() {
        assertFalse(ButtonAction.SELECT in TEACHABLE_ACTIONS)
        assertFalse(ButtonAction.SKIP_SONGS in TEACHABLE_ACTIONS)
        assertFalse(ButtonAction.NOTHING in TEACHABLE_ACTIONS)
        assertTrue(ButtonAction.OPEN_NAVIGATION in TEACHABLE_ACTIONS)
        assertTrue(ButtonAction.HOME_OR_APPS in TEACHABLE_ACTIONS)
    }

    @Test
    fun learnedButtonsAreListedByCode() {
        assertEquals(listOf(82 to ButtonAction.OPEN_MEDIA, 65541 to ButtonAction.HOME),
            learnedRows(mapOf(65541 to ButtonAction.HOME, 82 to ButtonAction.OPEN_MEDIA)))
    }
}
