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

/** The version whose "What's new" was last shown, or null. Not in backups: it belongs to this install. */
var Settings.myplayWhatsNewSeen: String?
    get() = prefs.getString(KEY_WHATS_NEW_SEEN, null)
    set(v) = prefs.edit { if (v == null) remove(KEY_WHATS_NEW_SEEN) else putString(KEY_WHATS_NEW_SEEN, v) }

internal const val KEY_WHATS_NEW_SEEN = "myplay_whats_new_seen"

/** How the wallpaper photo was placed, darkened and blurred (plan F5). Not in backups: the photo itself is not either. */
var Settings.myplayWallpaperEdit: WallpaperEdit
    get() = WallpaperEdit.decode(prefs.getString(KEY_WALLPAPER_EDIT, null))
    set(v) = prefs.edit { putString(KEY_WALLPAPER_EDIT, v.clamped().encode()) }

internal const val KEY_WALLPAPER_EDIT = "myplay_wallpaper_edit"

/**
 * #1: whether the car sends holds, as the Car check last saw it (null: not checked). Not in
 * backups: it belongs to this car and the check that saw it.
 */
var Settings.myplayCarSendsHolds: Boolean?
    get() = if (prefs.contains(KEY_CAR_SENDS_HOLDS)) prefs.getBoolean(KEY_CAR_SENDS_HOLDS, true) else null
    set(v) = prefs.edit { if (v == null) remove(KEY_CAR_SENDS_HOLDS) else putBoolean(KEY_CAR_SENDS_HOLDS, v) }

internal const val KEY_CAR_SENDS_HOLDS = "myplay_car_sends_holds"

/**
 * The hidden switch to the rebuilt phone app (plan §16). Off by default; turned on by 7 taps on the old
 * About page's version line, or in debug builds by the adb extra `new_phone_app`. Not in backups: it
 * belongs to this install while the new app is a preview.
 */
var Settings.myplayNewPhoneApp: Boolean
    get() = prefs.getBoolean(KEY_NEW_PHONE_APP, false)
    set(v) = prefs.edit { putBoolean(KEY_NEW_PHONE_APP, v) }

internal const val KEY_NEW_PHONE_APP = "myplay_new_phone_app"

/**
 * The owner was asked once whether to check for updates by itself (plan §5): asked in the first
 * What's new of the new app, or in Setup. The answer goes to check_updates; this only remembers that
 * the question was asked. Not in backups: it belongs to this install.
 */
var Settings.myplayUpdateAsked: Boolean
    get() = prefs.getBoolean(KEY_UPDATE_ASKED, false)
    set(v) = prefs.edit { putBoolean(KEY_UPDATE_ASKED, v) }

internal const val KEY_UPDATE_ASKED = "myplay_update_asked"
