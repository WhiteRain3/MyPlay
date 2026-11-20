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

/**
 * Settings backup (plan E8, with F6): save the settings to a file the user picks, or load one back
 * after a confirmation. What is left out and why is SettingsBackup's (the car side's engine);
 * this page is upstream AARE's export/import card (by HyperCriSiS, then upstream) in MyPlay's style.
 * The file is read off the main thread, with the engine's size limit; [onImported] has every page
 * read the new values again.
 */
@Composable
fun BackupPage(onImported: () -> Unit) {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    val scope = rememberCoroutineScope()
    // Saveable: the file picker can outlive the activity, and the confirmation needs the chosen file.
    var pendingImport by rememberSaveable { mutableStateOf<Uri?>(null) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val json = SettingsBackup.export(settings.all).toString(2)
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { ctx.contentResolver.openOutputStream(uri)!!.use { it.write(json.toByteArray()) } }
                    .onFailure { DiagLog.add("settings export failed: ${it.javaClass.simpleName}") }.isSuccess
            }
            if (ok) settings.myplayBackupLastSaved = System.currentTimeMillis()
            Toast.makeText(ctx, if (ok) R.string.phone_backup_saved else R.string.setup_backup_export_failed, Toast.LENGTH_LONG).show()
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImport = uri
    }
    Column {
        SectionHeading(stringResource(R.string.setup_section_backup))
        RowGroup(rows = listOf(
            {
                SimpleRow(Icons.Rounded.Upload, stringResource(R.string.phone_backup_save), TileTone.GREEN, stringResource(R.string.phone_backup_save_sub)) {
                    PillButton(stringResource(R.string.phone_backup_save_button)) { export.launch(backupFileName()) }
                }
            },
            {
                SimpleRow(Icons.Rounded.Download, stringResource(R.string.phone_backup_load), TileTone.BLUE, stringResource(R.string.phone_backup_load_sub)) {
                    PillButton(stringResource(R.string.phone_backup_load_button), main = false) {
                        import.launch(arrayOf("application/json", "text/plain", "*/*"))
                    }
                }
            },
        ))
        HintLine(Icons.Rounded.Download, stringResource(R.string.phone_backup_restores_all))
        HintLine(Icons.Rounded.Image, stringResource(R.string.phone_backup_no_photo))
        HintLine(Icons.Rounded.Info, stringResource(R.string.setup_backup_hint))
    }
    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(stringResource(R.string.setup_backup_import_confirm_title)) },
            text = { Text(stringResource(R.string.setup_backup_import_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    scope.launch {
                        val imported = withContext(Dispatchers.IO) {
                            // On failure, log only the kind: a JSON error's message quotes the whole file.
                            runCatching {
                                val bytes = ctx.contentResolver.openInputStream(uri)!!.use { SettingsBackup.readLimited(it) }
                                SettingsBackup.import(org.json.JSONObject(bytes.toString(Charsets.UTF_8)))
                            }.onFailure { DiagLog.add("settings import failed: ${it.javaClass.simpleName}") }.getOrNull()
                        }
                        if (imported == null) {
                            Toast.makeText(ctx, R.string.setup_backup_import_failed, Toast.LENGTH_LONG).show()
                            return@launch
                        }
                        settings.restore(imported.values)
                        if (imported.skipped.isNotEmpty()) DiagLog.add("settings import skipped ${imported.skipped.size}: ${imported.skipped.joinToString(limit = 10)}")
                        val done = if (imported.skipped.isEmpty()) ctx.getString(R.string.setup_backup_import_done)
                        else ctx.getString(R.string.setup_backup_import_done_skipped, imported.skipped.size)
                        Toast.makeText(ctx, done, Toast.LENGTH_LONG).show()
                        onImported()
                    }
                }) { Text(stringResource(R.string.setup_backup_import_confirm_action)) }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}
