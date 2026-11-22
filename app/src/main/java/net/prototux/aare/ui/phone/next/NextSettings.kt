package net.prototux.aare.ui.phone.next

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import net.prototux.aare.Settings

/**
 * Settings observed, never copied (plan §2 rule 7, §3.4 rule 9). The value is read through the same
 * getter as today and read again whenever one of [keys] changes, whoever changed it: this page, the car,
 * a look or a loaded backup. With no keys, any change re-reads it (for values built from many keys).
 */
@Composable
fun <T> rememberSetting(vararg keys: String, read: Settings.() -> T): State<T> {
    val ctx = LocalContext.current
    val settings = remember(ctx) { Settings(ctx) }
    val latest = rememberUpdatedState(read)
    val state = remember(settings) { mutableStateOf(settings.read()) }
    val watched = keys.toSet()
    DisposableEffect(settings, watched) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || watched.isEmpty() || key in watched) state.value = latest.value(settings)
        }
        // Held here: SharedPreferences keeps listeners only weakly.
        settings.prefs.registerOnSharedPreferenceChangeListener(listener)
        state.value = latest.value(settings) // a change between the first read and the listener
        onDispose { settings.prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return state
}
