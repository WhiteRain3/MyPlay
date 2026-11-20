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
            assertEquals(look.name, look.values, MyPlayLooks.current(look.values, look.name).values) // nothing at its default
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
        assertEquals("cyan", s.prefs.getString("style_accent", null))
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LookDefaultsTest {
    @Test
    fun theDefaultsAreWhatSettingsReadsWhenNothingIsStored() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = ctx.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        fun read(): List<Any?> {
            val s = Settings(ctx)
            return listOf(s.carThemeId, s.carAccent, s.carLayoutId, s.railPosition, s.darkBar, s.wallpaper, s.railWidgets, s.showWeather,
                prefs.getString("myplay_look_colours", ""),
                net.prototux.aare.ui.car.CarLayoutSpec.decode(prefs.getString("myplay_layout_spec", null)),
                prefs.getString("myplay_layout_preset", net.prototux.aare.ui.car.CarLayoutSpec.DEFAULT_PRESET),
                prefs.getBoolean("myplay_status_battery_icon", true), prefs.getBoolean("myplay_status_battery_number", true),
                prefs.getBoolean("myplay_status_percent_sign", false), net.prototux.aare.ui.car.StatusSize.byKey(prefs.getString("myplay_status_size", null)),
                prefs.getBoolean("myplay_status_color_by_level", false), prefs.getBoolean("myplay_status_signal", true),
                net.prototux.aare.ui.car.StatusSize.byKey(prefs.getString("myplay_clock_size", null)), prefs.getBoolean("myplay_clock_bold", false))
        }
        val unset = read()
        MyPlayLooks.choose(Settings(ctx), Look("defaults", MyPlayLooks.DEFAULTS))
        assertEquals(MyPlayLooks.DEFAULTS.keys, prefs.all.keys)
        assertEquals(unset, read())
        // And stored at their defaults, they are no difference from MyPlay's own look.
        assertTrue(MyPlayLooks.isCurrent(prefs.all, MyPlayLooks.BUILT_IN.first()))
    }

    @Test
    fun aLookHoldingDefaultsStillMatches() {
        val look = Look("file", mapOf("style_accent" to "red", "dark_bar" to false))
        assertTrue(MyPlayLooks.isCurrent(mapOf("style_accent" to "red"), look))
    }

    @Test
    fun anOlderCarStyleKeepsTheThemeBesideIt() {
        val look = MyPlayLooks.current(mapOf("car_style" to "carplay", "style_theme" to "aare"), "x")
        assertEquals(mapOf("car_style" to "carplay", "style_theme" to "aare"), look.values)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LookHardToReadTest {
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun aLookWithoutItsOwnColoursIsFine() {
        MyPlayLooks.BUILT_IN.forEach { assertFalse(it.name, lookHardToRead(it, ctx)) }
    }

    @Test
    fun grey_textOnGreyIsHardToRead() {
        val look = Look("x", mapOf("myplay_look_colours" to "night.background=FF777777;night.text=FF787878"))
        assertTrue(lookHardToRead(look, ctx))
        assertFalse(lookHardToRead(Look("y", mapOf("myplay_look_colours" to "day.background=FFFFFFFF;day.text=FF000000")), ctx))
    }
}
