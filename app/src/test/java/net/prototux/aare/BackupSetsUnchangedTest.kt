package net.prototux.aare

import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The phone app rebuild (plan §14, §16) never changes what a backup holds: SettingsBackup.ALLOWED,
 * SettingsBackup.BLOCKED and MyPlayBackupKeys.ALLOWED, keys and types, stay as they were.
 *
 * Adding a new setting on purpose (outside the rebuild) changes the fingerprint: then print [lines],
 * check the change is the one intended, and update [FINGERPRINT].
 */
class BackupSetsUnchangedTest {
    private fun lines(): List<String> =
        SettingsBackup.ALLOWED.map { (k, t) -> "allowed $k $t" } +
            SettingsBackup.BLOCKED.map { "blocked $it" } +
            MyPlayBackupKeys.ALLOWED.map { (k, t) -> "myplay $k $t" }

    private fun fingerprint(): String =
        MessageDigest.getInstance("SHA-256").digest(lines().sorted().joinToString("\n").toByteArray())
            .joinToString("") { "%02x".format(it) }

    @Test fun theBackupSetsAreUnchanged() =
        assertEquals("backup sets changed:\n" + lines().sorted().joinToString("\n"), FINGERPRINT, fingerprint())

    private companion object {
        const val FINGERPRINT = "110d910a5699bf4eda01d8cee13556e969f552b3760718f5d9e688053ef3145a"
    }
}
