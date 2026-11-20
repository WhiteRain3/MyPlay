package net.prototux.aare.ui.phone

import androidx.test.core.app.ApplicationProvider
import net.prototux.aare.Settings
import net.prototux.aare.SettingsBackup
import net.prototux.aare.ui.car.CarLayoutSpec
import net.prototux.aare.ui.car.WALLPAPER_NAMES
import net.prototux.aare.ui.theme.CarAccents
import net.prototux.aare.ui.theme.CarThemes
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MyPlayLooksTest {
    private val settings get() = Settings(ApplicationProvider.getApplicationContext())

    @Test
    fun everySettingOfALookMovesInAFile() {
        assertEquals(emptySet<String>(), MyPlayLooks.KEYS - SettingsBackup.ALLOWED.keys)
    }

    @Test
    fun builtInLooksUseRealValuesAndUniqueNames() {
        assertEquals(MyPlayLooks.BUILT_IN.size, MyPlayLooks.BUILT_IN.map { it.name }.distinct().size)
        assertTrue(MyPlayLooks.BUILT_IN.first().values.isEmpty())
        MyPlayLooks.BUILT_IN.forEach { look ->
            assertTrue(look.values.keys.all { it in MyPlayLooks.KEYS })
            (look.values["style_theme"] as String?)?.let { id -> assertEquals(id, CarThemes.byId(id).id) }
            (look.values["style_accent"] as String?)?.let { id -> assertTrue(CarAccents.byId(id) != null) }
            (look.values["wallpaper"] as String?)?.let { assertTrue(it in WALLPAPER_NAMES) }
            (look.values["myplay_layout_preset"] as String?)?.let { p ->
                assertEquals(CarLayoutSpec.PRESETS.getValue(p), CarLayoutSpec.decode(look.values["myplay_layout_spec"] as String))
            }
        }
    }

    @Test
    fun choosingALookSetsItAndPutsTheRestBack() {
        val s = settings
        s.prefs.edit().putString("myplay_clock_size", "large").putBoolean("rail_right", true).putString("browser_url", "x").commit()
        val ocean = MyPlayLooks.BUILT_IN.first { it.name == "Ocean" }
        MyPlayLooks.choose(s, ocean)
        assertEquals("carplay", s.prefs.getString("style_theme", null))
        assertFalse(s.prefs.contains("myplay_clock_size"))
        assertFalse(s.prefs.contains("rail_right"))
        assertEquals("x", s.prefs.getString("browser_url", null)) // not part of a look
        assertTrue(MyPlayLooks.isCurrent(s.prefs.all, ocean))
        assertFalse(MyPlayLooks.isCurrent(s.prefs.all, MyPlayLooks.BUILT_IN.first()))
    }

    @Test
    fun theCurrentLookTakesOnlyLookSettings() {
        val look = MyPlayLooks.current(mapOf("style_theme" to "night", "browser_url" to "x", "dark_bar" to null), "  Mine\n ")
        assertEquals(Look("Mine", mapOf("style_theme" to "night")), look)
    }

    @Test
    fun aLookFileRoundTripsWithItsName() {
        val look = Look("Mine", mapOf("style_theme" to "night", "dark_bar" to true, "myplay_clock_color" to 0xFF112233.toInt()))
        val back = MyPlayLooks.fromFile(JSONObject(MyPlayLooks.toFile(look).toString()), "file")
        assertEquals(look, back)
    }

    @Test
    fun aFileTakesOnlyTheLookAndNeverAPhotoItDoesNotHold() {
        val backup = SettingsBackup.export(mapOf("style_theme" to "night", "browser_url" to "x", "wallpaper" to Settings.WALLPAPER_CUSTOM))
        val look = MyPlayLooks.fromFile(backup, "backup.json")
        assertEquals(Look("backup.json", mapOf("style_theme" to "night")), look)
    }

    @Test
    fun savedLooksKeepTheirPhotoAndSkipBrokenEntries() {
        val mine = Look("Mine", mapOf("wallpaper" to Settings.WALLPAPER_CUSTOM))
        val text = MyPlayLooks.encode(listOf(mine)).replace("[", "[{\"x\":1},")
        assertEquals(listOf(mine), MyPlayLooks.decode(text))
        assertEquals(emptyList<Look>(), MyPlayLooks.decode("not json"))
        assertEquals(emptyList<Look>(), MyPlayLooks.decode(null))
    }

    @Test
    fun savingUnderTheSameNameReplaces() {
        val a = Look("A", emptyMap())
        val a2 = Look("A", mapOf("dark_bar" to true))
        assertEquals(listOf(Look("B", emptyMap()), a2), MyPlayLooks.saved(listOf(a, Look("B", emptyMap())), a2))
    }

    @Test
    fun savedLooksAreStored() {
        val s = settings
        s.myplayLooks = listOf(Look("Mine", mapOf("style_accent" to "red")))
        assertEquals(listOf(Look("Mine", mapOf("style_accent" to "red"))), s.myplayLooks)
    }
}

class LookFileNameTest {
    @Test
    fun fileNamesAreSafeAndComeBackAsNames() {
        assertEquals("myplay-look-high-contrast.json", lookFileName("High contrast"))
        assertEquals("myplay-look-mine.json", lookFileName("/../"))
        assertEquals("high-contrast", nameFromFile("primary:Download/myplay-look-high-contrast.json"))
        assertEquals("", nameFromFile(null))
    }
}
