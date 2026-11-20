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
        val keys = root.walkTopDown().filter { it.isFile && it.extension == "kt" }
            .flatMap { f -> f.readLines().filter { "prefs." in it }.flatMap { line -> read.findAll(line).map { it.groupValues[1] } } }
            .toSet()
        assertTrue("found no keys under $root", keys.isNotEmpty())
        val missing = keys - MyPlayBackupKeys.ALLOWED.keys - NOT_BACKED_UP
        assertTrue("MyPlay settings missing from MyPlayBackupKeys: $missing", missing.isEmpty())
    }

    private companion object {
        /** MyPlay settings left out of backups on purpose, each with its reason. None yet. */
        val NOT_BACKED_UP: Set<String> = emptySet()
    }
}
