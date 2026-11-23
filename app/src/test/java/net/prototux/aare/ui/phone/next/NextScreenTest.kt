package net.prototux.aare.ui.phone.next

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import net.prototux.aare.Settings
import net.prototux.aare.ui.car.CarPart
import net.prototux.aare.ui.car.myplay.myplayOrbitShade
import net.prototux.aare.ui.car.myplay.myplayPanelTimeout
import net.prototux.aare.ui.phone.MyPlayLooks
import net.prototux.aare.ui.theme.MapColourFilter
import net.prototux.aare.ui.theme.myplayMapFilter
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Screen I (plan §16 chunk 4): the picture's parts per design, and pages writing today's keys. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h2400dp-xxhdpi")
class NextScreenTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val settings get() = Settings(ApplicationProvider.getApplicationContext())

    @Test fun orbitsPartsOpenWhatTheyShow() {
        val o = DesignInUse.ORBIT
        assertEquals("screen/design", routeForPart(CarPart.OrbitHub, o))
        assertEquals("car/sound", routeForPart(CarPart.OrbitSong, o))
        assertEquals("car/maps", routeForPart(CarPart.OrbitTurn, o))
        assertEquals("car/speed", routeForPart(CarPart.OrbitSpeed, o))
        assertEquals("screen/wallpaper", routeForPart(CarPart.Wallpaper, o))
        assertEquals("apps", routeForPart(CarPart.AppsButton, o))
        assertEquals(null, routeForPart(null, o))
    }

    @Test fun archivedDesignsPartsOpenArchive() {
        assertEquals("more/archive/deck", routeForPart(CarPart.OrbitHub, DesignInUse.DECK))
        assertEquals("more/archive/layouts", routeForPart(CarPart.OrbitHub, DesignInUse.OLDER))
        for (d in listOf(DesignInUse.DECK, DesignInUse.OLDER)) {
            assertEquals("more/archive/layouts", routeForPart(CarPart.Bar, d))
            assertEquals("more/archive/layouts", routeForPart(CarPart.Clock, d))
            assertEquals("more/archive/layouts", routeForPart(CarPart.Status, d))
            assertEquals("car/speed", routeForPart(CarPart.OrbitSpeed, d))
            assertEquals("screen/wallpaper", routeForPart(CarPart.Wallpaper, d))
            assertEquals("apps", routeForPart(CarPart.AppsButton, d))
        }
    }

    @Test fun rowValues() {
        assertEquals("MyPlay", lookInUse(emptyMap<String, Any>(), emptyList()))
        assertEquals("Midnight", lookInUse(MyPlayLooks.BUILT_IN[1].values, emptyList()))
        assertEquals(null, lookInUse(mapOf("style_accent" to "pink"), emptyList()))
        // "Midnight (changed)": one part away from a look names it, with the changed mark.
        assertEquals("Midnight" to false, lookNearest(MyPlayLooks.BUILT_IN[1].values, emptyList()))
        assertEquals("Midnight" to true, lookNearest(MyPlayLooks.BUILT_IN[1].values + ("style_accent" to "pink"), emptyList()))
        assertEquals("none", mapColoursKind(MapColourFilter()))
        assertEquals("muted", mapColoursKind(MapColourFilter.PRESETS.getValue("muted")))
        assertEquals("custom", mapColoursKind(MapColourFilter(hueShift = 33)))
    }

    @Test fun sizeWritesTodaysValues() {
        rule.setContent { NextSizePage() }
        rule.onNode(hasText("Larger")).let { rule.onAllNodes(hasText("Larger"))[0].performClick() }
        assertEquals("larger", settings.carTextSize)
        rule.onNodeWithText("Fixed").performClick()
        rule.onNodeWithText("213 dpi").performClick()
        assertEquals(213, settings.carDensity)
        rule.onNodeWithText("Auto").performClick()
        assertEquals(0, settings.carDensity)
    }

    @Test fun resetPutsSizeBackAndUndoRestores() {
        settings.carTextSize = "largest"; settings.carDensity = 160
        rule.setContent { NextApp(start = "screen/size") { NextSizePage() } }
        rule.onNodeWithText("Reset this page").performClick()
        rule.waitForIdle()
        assertEquals("default", settings.carTextSize)
        assertEquals(0, settings.carDensity)
        rule.onNodeWithText("Undo").performClick()
        rule.waitForIdle()
        assertEquals("largest", settings.carTextSize)
        assertEquals(160, settings.carDensity)
    }

    @Test fun designWritesTodaysValues() {
        rule.setContent { NextDesignPage() }
        rule.onNodeWithText("Close panels after").performClick()
        rule.onNodeWithText("15 s").performClick()
        assertEquals(15, settings.myplayPanelTimeout)
        rule.onNodeWithText("Shade behind ORBIT's parts").performClick()
        rule.onNodeWithText("None").performClick()
        assertEquals("none", settings.myplayOrbitShade)
    }

    @Test fun mapColoursPresetIsExactlyTodaysPreset() {
        rule.setContent { NextMapColoursPage() }
        rule.onNodeWithText("Cool").performClick()
        assertEquals(MapColourFilter.PRESETS.getValue("cool"), settings.myplayMapFilter)
    }
}
