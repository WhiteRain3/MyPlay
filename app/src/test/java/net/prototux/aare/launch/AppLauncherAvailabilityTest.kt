package net.prototux.aare.launch

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * VA-19: an installed su lets the car try phone apps, but never counts as a working launcher for
 * the phone's Phone apps page (its root/Shizuku card) or the once-only add.
 */
class AppLauncherAvailabilityTest {
    @Test fun suPresentRootRefusedNoShizuku() {
        // AppLauncher.available: su is never counted, so the phone page still shows the root/Shizuku card.
        assertFalse(AppLauncher.decide(shizuku = false, root = false, su = false))
        // AppLauncher.mayTry: the car lists phone apps and shows the refusal.
        assertTrue(AppLauncher.decide(shizuku = false, root = false, su = true))
    }

    @Test fun aWorkingLauncherCounts() {
        assertTrue(AppLauncher.decide(shizuku = true, root = false, su = false))
        assertTrue(AppLauncher.decide(shizuku = false, root = true, su = false))
    }
}
