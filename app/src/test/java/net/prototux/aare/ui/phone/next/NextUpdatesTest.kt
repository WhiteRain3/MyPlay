package net.prototux.aare.ui.phone.next

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import net.prototux.aare.BuildConfig
import net.prototux.aare.MyPlayUpdateInstall.Failure
import net.prototux.aare.MyPlayUpdateInstall.State
import net.prototux.aare.Settings
import net.prototux.aare.UpdateChecker.Update
import net.prototux.aare.ui.phone.NoteBlock
import net.prototux.aare.ui.phone.myplayUpdateAsked
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** More › Updates and What's new (plan §5, §16 chunk 3). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h2400dp-xxhdpi")
class NextUpdatesTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val settings get() = Settings(ApplicationProvider.getApplicationContext())
    private val u = Update("9.9", "")

    @After fun reset() = NextUpdateCheck.resetForTest()

    @Test fun theHeroForEachState() {
        assertEquals(UpdateHero.UpToDate, updateHeroOf(State.Idle, null, null, false))
        assertEquals(UpdateHero.Checking, updateHeroOf(State.Idle, null, null, true))
        assertEquals(UpdateHero.Ready("9.9"), updateHeroOf(State.Idle, u, null, false))
        assertEquals(UpdateHero.Downloading("9.9", 42), updateHeroOf(State.Downloading(42), u, "9.9", false))
        assertEquals(UpdateHero.Installing("9.9"), updateHeroOf(State.Installing, null, "9.9", false))
        assertEquals(UpdateHero.NeedsPermission("9.9"), updateHeroOf(State.NeedsPermission, u, null, false))
        assertEquals(UpdateHero.Failed("9.9", Failure.NETWORK), updateHeroOf(State.Failed(Failure.NETWORK), u, null, false))
        // An install state with no version to name falls back to what is known.
        assertEquals(UpdateHero.UpToDate, updateHeroOf(State.Failed(Failure.NETWORK), null, null, false))
    }

    @Test fun checkNowAsksOnceAndKeepsTheAnswerOutOfSettings() {
        NextUpdateCheck.checkNow(now = { 5L }, fetch = { u })
        assertEquals(NextUpdateCheck.Result.Done(5L, u), NextUpdateCheck.result.value)
        NextUpdateCheck.checkNow(now = { 6L }, fetch = { Update(BuildConfig.VERSION_NAME, "") })
        assertEquals(NextUpdateCheck.Result.Done(6L, null), NextUpdateCheck.result.value)
        NextUpdateCheck.checkNow(fetch = { null })
        assertEquals(NextUpdateCheck.Result.Offline, NextUpdateCheck.result.value)
        assertEquals(null, settings.updateLatestVersion)
    }

    @Test fun checkNowWinsOverTheDailyAnswer() {
        assertEquals(null, foundUpdate(NextUpdateCheck.Result.Done(1L, null), u))
        assertEquals(u, foundUpdate(NextUpdateCheck.Result.Offline, u))
        assertEquals(u, foundUpdate(null, u))
    }

    @Test fun newestFirstComparesNumbers() =
        assertEquals(listOf("0.1.10", "0.1.9", "0.1.5"), newestFirst(listOf("0.1.5", "0.1.10", "0.1.9")))

    @Test fun automaticChecksStayOffByDefault() = assertFalse(settings.checkUpdates)

    private val taps = mutableListOf<String>()
    private val actions = object : UpdateActions {
        override fun install(version: String) { taps += "install $version" }
        override fun checkNow() { taps += "check" }
        override fun setAutomatic(on: Boolean) { taps += "auto $on" }
        override fun allowInstalls() { taps += "allow" }
    }

    @Test fun readyInstallsOnATap() {
        rule.setContent { NextUpdatesContent(UpdateHero.Ready("9.9"), null, 0L, false, actions) }
        rule.onNodeWithText("MyPlay 9.9 is ready").assertExists()
        rule.onNodeWithText("Download and install 9.9").performClick()
        rule.onNodeWithText("Check now").performClick()
        rule.onNode(hasText("Check automatically") and isToggleable()).assertIsOff().performClick()
        assertEquals(listOf("install 9.9", "check", "auto true"), taps)
    }

    @Test fun installsNeedPermissionOnce() {
        rule.setContent { NextUpdatesContent(UpdateHero.NeedsPermission("9.9"), null, 0L, true, actions) }
        rule.onNodeWithText("Couldn't install 9.9").assertExists()
        rule.onNodeWithText("Allow").performClick()
        rule.onNode(hasText("Check automatically") and isToggleable()).assertIsOn()
        assertEquals(listOf("allow"), taps)
    }

    @Test fun offlineSaysSo() {
        rule.setContent { NextUpdatesContent(UpdateHero.UpToDate, NextUpdateCheck.Result.Offline, 0L, false, actions) }
        rule.onNodeWithText("Up to date").assertExists()
        rule.onNodeWithText("No internet").assertExists()
    }

    @Test fun whatsNewAsksOnceAndWritesTheAnswer() {
        val notes = listOf("9.9" to listOf<NoteBlock>(NoteBlock.Bullet(0, "New looks")), "9.8" to listOf<NoteBlock>(NoteBlock.Bullet(0, "Old")))
        rule.setContent { NextWhatsNewPage() }
        rule.onNodeWithText("Check for updates by itself?").assertExists()
        rule.onNodeWithText("Yes").performClick()
        rule.waitForIdle()
        assertTrue(settings.checkUpdates)
        assertTrue(settings.myplayUpdateAsked)
        rule.onNodeWithText("Check for updates by itself?").assertDoesNotExist()
        @Suppress("unused") val keep = notes
    }

    @Test fun noMeansOffAndAskedToo() {
        rule.setContent { NextWhatsNewContent(listOf("9.9" to listOf(NoteBlock.Bullet(0, "New looks"))), askUpdates = true) { yes -> taps += "answer $yes" } }
        rule.onNodeWithText("New looks").assertExists()
        rule.onNodeWithText("No").performClick()
        assertEquals(listOf("answer false"), taps)
    }
}
