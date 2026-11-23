package net.prototux.aare.host

import net.prototux.aare.ui.car.myplay.KEY_ENGINE_LANGUAGE
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * MyPlay (LG-1): a setting the car screen reads is applied at once while the car is connected,
 * not only at the next connection: every key [CarRuntime.applyScreenSettings] reads through a
 * `settings.myplay...` property of the new screen is in [CarRuntime.SCREEN_SETTING_KEYS].
 */
class ScreenSettingKeysTest {
    @Test fun theNewScreensLanguageAndPreviewApplyAtOnce() {
        for (key in listOf(KEY_ENGINE_LANGUAGE, "myplay_engine_preview", "myplay_speed_source", "myplay_home_maps", "myplay_map_filter")) {
            assertTrue(key, key in CarRuntime.SCREEN_SETTING_KEYS)
        }
    }

    /** The source of applyScreenSettings names each setting it reads; a new one needs its key in the set. */
    @Test fun everyEngineSettingReadOnApplyIsListened() {
        val src = File("src/main/java/net/prototux/aare/host/CarRuntime.kt").readText()
        val body = src.substringAfter("fun applyScreenSettings()").substringBefore("\n    }\n")
        val read = Regex("""settings\.(myplayEngine\w+)""").findAll(body).map { it.groupValues[1] }.toSet()
        assertTrue("applyScreenSettings reads the engine's settings: $read", read.containsAll(setOf("myplayEnginePreview", "myplayEngineLanguage")))
        val keyOf = mapOf("myplayEnginePreview" to "myplay_engine_preview", "myplayEngineLanguage" to KEY_ENGINE_LANGUAGE)
        for (p in read) assertTrue("$p is read on apply but its key is not listened to", keyOf[p] in CarRuntime.SCREEN_SETTING_KEYS)
    }

    /** AP-1: every setting refreshApps reads from Settings changes the car's app list at once. */
    @Test fun everyAppListSettingIsListened() {
        val src = File("src/main/java/net/prototux/aare/host/CarRuntime.kt").readText()
        val body = src.substringAfter("fun refreshApps()").substringBefore("\n    fun ")
        val keyOf = mapOf("hiddenApps" to "hidden_apps", "phoneScreen" to "phone_screen", "browser" to "browser",
            "carDataApp" to "car_data_app", "standaloneApps" to "standalone_apps", "standaloneAppPackages" to "standalone_app_pkgs",
            "appOrder" to "app_order")
        val read = Regex("""settings\.(\w+)""").findAll(body).map { it.groupValues[1] }.filter { it in keyOf }.toSet()
        assertTrue("refreshApps reads the app list's settings: $read", read.containsAll(setOf("browser", "carDataApp", "standaloneApps", "standaloneAppPackages")))
        for (p in read) assertTrue("$p is read by refreshApps but its key is not listened to", keyOf[p] in CarRuntime.APP_LIST_KEYS)
    }
}
