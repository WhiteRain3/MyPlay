package net.prototux.aare

import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** MyPlay: its own settings move in a settings backup. */
class MyPlayBackupKeysTest {
    @Test fun myPlaysSettingsAreAllowed() {
        for ((key, type) in MyPlayBackupKeys.ALLOWED) assertEquals(key, type, SettingsBackup.ALLOWED[key])
    }

    @Test fun noneOfThemIsBlocked() =
        assertTrue(MyPlayBackupKeys.ALLOWED.keys.intersect(SettingsBackup.BLOCKED).isEmpty())

    @Test fun theyRoundTripThroughAFile() {
        val all = mapOf<String, Any?>(
            "myplay_layout_spec" to "map=1;bar=pills",
            "myplay_status_color" to 0xFFFFB300.toInt(),
            "myplay_clock_bold" to true,
        )
        val text = SettingsBackup.export(all).toString()
        assertEquals(all, SettingsBackup.import(JSONObject(text)).values)
    }

    /**
     * Every "myplay_" setting read anywhere in the app is in the list (or deliberately left out
     * below), so a setting added later cannot silently be missing from backups.
     */
    @Test fun everyMyPlaySettingInTheSourceIsListed() {
        val root = listOf("src/main/java", "app/src/main/java").map(::File).first { it.isDirectory }
        val read = Regex(""""(myplay_[a-z0-9_]+)"""")
        // Every "myplay_" literal, not only those on a prefs line (F6-2): a key kept in a constant
        // is found too. Prefixes (ending in "_") and values that are not keys are left aside.
        val keys = root.walkTopDown().filter { it.isFile && it.extension == "kt" }
            .flatMap { f -> read.findAll(f.readText()).map { it.groupValues[1] } }
            .filterNot { it.endsWith("_") }
            .toSet() - NOT_KEYS
        assertTrue("found no keys under $root", keys.isNotEmpty())
        val missing = keys - MyPlayBackupKeys.ALLOWED.keys - NOT_BACKED_UP
        assertTrue("MyPlay settings missing from MyPlayBackupKeys: $missing", missing.isEmpty())
    }

    private companion object {
        /** MyPlay settings left out of backups on purpose, each with its reason. */
        val NOT_BACKED_UP: Set<String> = setOf(
            "myplay_backup_last_saved", // when this phone last saved a backup: belongs to the phone, not to the settings
            "myplay_whats_new_seen", // which release notes this install has shown: belongs to the install
            "myplay_wallpaper_edit", // how the wallpaper photo is placed and treated: the photo is not in backups either
            "myplay_speed_limit_checked", // when this phone last looked for a newer speed limit file (N21): belongs to the phone
            "myplay_debug_map_areas", // a check outline on the car screen (MF-4): should not travel to a new phone
            "myplay_debug_nav_text", // the N3 bench check's log of maps apps' text: a check, should not travel to a new phone
            "myplay_learn_button", // the action the next car key will be taught (O11): a moment's state
            "myplay_learn_button_at", // when that action was armed (O11-1): a moment's state
            "myplay_engine_preview", // the hidden preview of phase N's car screen (N1): a test switch, not a choice to keep
        )

        /** "myplay_" strings that are not settings keys. */
        val NOT_KEYS: Set<String> = setOf(
            "myplay_map_behind", // a value of car_layout (CarLayout.MAP_BEHIND), not a key
        )
    }
}
