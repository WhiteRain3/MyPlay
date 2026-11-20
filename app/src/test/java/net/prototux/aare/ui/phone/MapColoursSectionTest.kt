package net.prototux.aare.ui.phone

import net.prototux.aare.ui.theme.MapColourFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapColoursSectionTest {
    @Test
    fun noFilterLeavesTheSampleMapAsItIs() {
        for (night in listOf(true, false)) {
            assertEquals(SampleMapPart.entries.associateWith { sampleMapColour(it, night) }, filteredSampleMap(MapColourFilter(), night))
        }
    }

    @Test
    fun greyTakesTheColourOut() {
        val route = filteredSampleMap(MapColourFilter.PRESETS.getValue("grey"), night = false).getValue(SampleMapPart.ROUTE)
        val r = (route shr 16) and 0xFF; val g = (route shr 8) and 0xFF; val b = route and 0xFF
        assertEquals(r, g); assertEquals(g, b)
    }

    @Test
    fun darkerNightsOnlyChangeTheNight() {
        val f = MapColourFilter.PRESETS.getValue("dark_night")
        assertEquals(sampleMapColour(SampleMapPart.ROAD, false), filteredSampleMap(f, night = false).getValue(SampleMapPart.ROAD))
        assertNotEquals(sampleMapColour(SampleMapPart.ROAD, true), filteredSampleMap(f, night = true).getValue(SampleMapPart.ROAD))
    }

    @Test
    fun presetsAreRecognisedAndOwnValuesAreNot() {
        MapColourFilter.PRESETS.forEach { (k, f) -> assertEquals(k, mapPresetOf(f)) }
        assertNull(mapPresetOf(MapColourFilter(hueShift = 33)))
    }
}
