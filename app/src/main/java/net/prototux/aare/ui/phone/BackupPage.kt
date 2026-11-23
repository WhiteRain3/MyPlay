package net.prototux.aare.ui.phone

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.prototux.aare.DiagLog
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.SettingsBackup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The Backup tile's value: the day of the last save ("20 Nov"), or null when never saved. */
fun lastSavedText(savedMs: Long?, locale: Locale = Locale.getDefault()): String? =
    savedMs?.let { SimpleDateFormat("d MMM", locale).format(Date(it)) }

/** The name offered for a saved settings file: MyPlay's name and the day, so several can sit side by side. */
fun backupFileName(now: Date = Date()): String = "myplay-settings-" + SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(now) + ".json"

