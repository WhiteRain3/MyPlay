package net.prototux.aare.ui.phone.next

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The shell (plan §3.4, §16 chunk 0): stacks per tab, Back, saved scroll, deep links. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h915dp-xxhdpi")
class NextAppTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    /** The Car root as a long list; its rows open Speed. Other routes are placeholders. */
    private var carList: LazyListState? = null

    @Composable
    private fun TestPage(route: String) {
        if (route == "car") {
            val nav = LocalNextNavigator.current
            val state = rememberNextListState().also { carList = it }
            LazyColumn(Modifier.fillMaxSize().testTag("car-list"), state = state) {
                items(100) { i ->
                    Text("row $i", Modifier.fillMaxWidth().height(64.dp).clickable { nav.open("car/speed") }.testTag("car-row-$i"))
                }
            }
        } else NextPlaceholder(route) {}
    }

    private fun page(route: String) = rule.onNodeWithTag("next-page-$route")
    private fun tab(t: NextTab) {
        rule.onNodeWithTag("next-tab-${t.root}").performClick()
        rule.waitForIdle()
    }
    private fun row(route: String) = rule.onNodeWithTag("next-row-$route").performClick()
    private fun back() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    @Test fun switchingTabsKeepsEachTabsPage() {
        rule.setContent { NextApp { TestPage(it) } }
        tab(NextTab.MORE); row("more/backup"); page("more/backup").assertExists()
        tab(NextTab.SCREEN); row("screen/size")
        tab(NextTab.MORE); page("more/backup").assertExists()
        tab(NextTab.SCREEN); page("screen/size").assertExists()
    }

    @Test fun backPopsThenGoesHome() {
        rule.setContent { NextApp { TestPage(it) } }
        tab(NextTab.MORE); row("more/advanced"); row("more/advanced/developer")
        back(); page("more/advanced").assertExists()
        back(); page("more").assertExists()
        back(); page("home").assertExists()
        // At Home's root Back is the system's: the shell no longer takes it.
        rule.runOnUiThread { assertTrue(!rule.activity.onBackPressedDispatcher.hasEnabledCallbacks()) }
    }

    @Test fun theBackButtonOnThePageDoesTheSame() {
        rule.setContent { NextApp { TestPage(it) } }
        tab(NextTab.MORE); row("more/updates")
        rule.onNodeWithTag("next-back").performClick()
        page("more").assertExists()
    }

    @Test fun backReturnsToTheSameScrollPosition() {
        rule.setContent { NextApp { TestPage(it) } }
        tab(NextTab.CAR)
        rule.runOnUiThread { runBlocking { carList!!.scrollToItem(40) } }
        rule.onNodeWithTag("car-row-41").performClick()
        page("car/speed").assertExists()
        back()
        rule.onNodeWithTag("car-list").assertExists()
        assertEquals(40, carList!!.firstVisibleItemIndex)
    }

    @Test fun anotherTabAndBackKeepsTheScrollPosition() {
        rule.setContent { NextApp { TestPage(it) } }
        tab(NextTab.CAR)
        rule.runOnUiThread { runBlocking { carList!!.scrollToItem(25) } }
        tab(NextTab.APPS)
        tab(NextTab.CAR)
        assertEquals(25, carList!!.firstVisibleItemIndex)
    }

    @Test fun tappingTheTabAtItsRootScrollsToTheTop() {
        rule.setContent { NextApp { TestPage(it) } }
        tab(NextTab.CAR)
        rule.runOnUiThread { runBlocking { carList!!.scrollToItem(30) } }
        tab(NextTab.CAR)
        rule.waitForIdle()
        assertEquals(0, carList!!.firstVisibleItemIndex)
    }

    @Test fun tappingTheTabOnAPageGoesToItsRoot() {
        rule.setContent { NextApp { TestPage(it) } }
        tab(NextTab.MORE); row("more/cars")
        tab(NextTab.MORE)
        page("more").assertExists()
    }

    @Test fun mapsLinkToMapColoursReturnsToMaps() {
        rule.setContent { NextApp(deepLink = "car/maps") { if (it == "car") NextPlaceholder(it) {} else TestPage(it) } }
        page("car/maps").assertExists()
        row("screen/map-colours")
        page("screen/map-colours").assertExists()
        back()
        page("car/maps").assertExists()
    }

    @Test fun aDeepLinkOpensItsTabWithItsParents() {
        var link by mutableStateOf<String?>("more/advanced/certificates")
        rule.setContent { NextApp(deepLink = link, onDeepLinkUsed = { link = null }) { TestPage(it) } }
        page("more/advanced/certificates").assertExists()
        assertEquals(null, link)
        back(); page("more/advanced").assertExists()
    }

    @Test fun anUnknownDeepLinkOpensHome() {
        rule.setContent { NextApp(deepLink = "nowhere") { TestPage(it) } }
        page("home").assertExists()
    }

    @Test fun placeAndScrollSurviveRecreationAndProcessDeath() {
        val restore = StateRestorationTester(rule)
        restore.setContent { NextApp { TestPage(it) } }
        tab(NextTab.SCREEN); row("screen/colours")
        tab(NextTab.CAR)
        rule.runOnUiThread { runBlocking { carList!!.scrollToItem(33) } }
        restore.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("car-list").assertExists()
        assertEquals(33, carList!!.firstVisibleItemIndex)
        tab(NextTab.SCREEN)
        page("screen/colours").assertExists()
        back(); page("screen").assertExists()
    }
}
