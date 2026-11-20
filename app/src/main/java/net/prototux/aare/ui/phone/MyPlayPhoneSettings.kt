package net.prototux.aare.ui.phone

import androidx.core.content.edit
import net.prototux.aare.Settings

/*
 * MyPlay's phone-app settings, as extension properties so Settings.kt is not edited (keys start
 * with "myplay_"; see MyPlayBackupKeys for which ones travel in a settings backup).
 */

/** When the settings were last saved to a file (ms since 1970), or null if never. Not in backups: it belongs to this phone. */
var Settings.myplayBackupLastSaved: Long?
    get() = if (prefs.contains(KEY_BACKUP_LAST_SAVED)) prefs.getLong(KEY_BACKUP_LAST_SAVED, 0) else null
    set(v) = prefs.edit { if (v == null) remove(KEY_BACKUP_LAST_SAVED) else putLong(KEY_BACKUP_LAST_SAVED, v) }

internal const val KEY_BACKUP_LAST_SAVED = "myplay_backup_last_saved"
