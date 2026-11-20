package net.prototux.aare.ui.phone

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.prototux.aare.DiagLog
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.SettingsBackup
import net.prototux.aare.ui.theme.CarThemes
import net.prototux.aare.ui.theme.CustomColours
import net.prototux.aare.ui.theme.forPhone
import net.prototux.aare.ui.theme.withAccent
import net.prototux.aare.ui.theme.withCustomColours

/** The name offered for a shared look's file. */
fun lookFileName(name: String): String =
    "myplay-look-" + name.lowercase().map { if (it.isLetterOrDigit()) it else '-' }.joinToString("").trim('-').ifEmpty { "mine" } + ".json"

/** A look file's name without its ending, offered as the look's name when the file has none. */
fun nameFromFile(file: String?): String = file.orEmpty().substringAfterLast('/').substringBeforeLast('.').removePrefix("myplay-look-")

/** Three night colours of [look] as the car would paint them: background, card, accent. */
private fun lookDots(look: Look, ctx: Context): List<Color> {
    val v = look.values
    val scheme = CarThemes.byId(v["style_theme"] as? String ?: "aare").forPhone(ctx)
        .withAccent(v["style_accent"] as? String ?: "theme", ctx)
        .withCustomColours(CustomColours.decode(v["myplay_look_colours"] as? String)).night
    return listOf(scheme.background, scheme.surfaceContainer, scheme.primary)
}

/**
 * Whole looks (F7): the built-in ones and the owner's own, chosen with one tap. The owner can save
 * the look now set under a name, share one as a file (the settings backup's format, holding the
 * look only) and open such a file to add it to their looks.
 */
@Composable
fun LooksPage() {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    val scope = rememberCoroutineScope()
    var mine by remember { mutableStateOf(settings.myplayLooks) }
    // Redrawn after each choice, so the tick follows the settings.
    var changes by remember { mutableIntStateOf(0) }
    val all = remember(changes) { settings.prefs.all.orEmpty() }
    var asking by rememberSaveable { mutableStateOf<String?>(null) } // a look to choose while the current one is not saved
    var naming by rememberSaveable { mutableStateOf(false) }
    var sharing by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    var opened by remember { mutableStateOf<Look?>(null) }
    fun keep(looks: List<Look>) { mine = looks; settings.myplayLooks = looks }
    val looks = MyPlayLooks.BUILT_IN + mine
    val unsaved = looks.none { MyPlayLooks.isCurrent(all, it) }
    fun choose(look: Look) { MyPlayLooks.choose(settings, look); changes++ }

    val share = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val look = looks.firstOrNull { it.name == sharing }
        sharing = null
        if (uri == null || look == null) return@rememberLauncherForActivityResult
        val json = MyPlayLooks.toFile(look).toString(2)
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { ctx.contentResolver.openOutputStream(uri)!!.use { it.write(json.toByteArray()) } }
                    .onFailure { DiagLog.add("look export failed: ${it.javaClass.simpleName}") }.isSuccess
            }
            Toast.makeText(ctx, if (ok) R.string.phone_looks_shared else R.string.setup_backup_export_failed, Toast.LENGTH_LONG).show()
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val look = withContext(Dispatchers.IO) {
                // On failure, log only the kind: a JSON error's message quotes the whole file.
                runCatching {
                    val bytes = ctx.contentResolver.openInputStream(uri)!!.use { SettingsBackup.readLimited(it) }
                    MyPlayLooks.fromFile(org.json.JSONObject(bytes.toString(Charsets.UTF_8)), nameFromFile(uri.lastPathSegment))
                }.onFailure { DiagLog.add("look import failed: ${it.javaClass.simpleName}") }.getOrNull()
            }
            if (look == null) Toast.makeText(ctx, R.string.phone_looks_open_failed, Toast.LENGTH_LONG).show() else opened = look
        }
    }

    Column {
        PartPicture()
        SectionHeading(stringResource(R.string.phone_looks_built_in))
        RowGroup(rows = MyPlayLooks.BUILT_IN.map { look ->
            { LookRow(look, MyPlayLooks.isCurrent(all, look), lookDots(look, ctx), onShare = { sharing = look.name; share.launch(lookFileName(look.name)) }) {
                if (unsaved) asking = look.name else choose(look)
            } }
        })
        SectionHeading(stringResource(R.string.phone_looks_yours))
        if (mine.isNotEmpty()) {
            RowGroup(rows = mine.map { look ->
                { LookRow(look, MyPlayLooks.isCurrent(all, look), lookDots(look, ctx), onShare = { sharing = look.name; share.launch(lookFileName(look.name)) },
                    onDelete = { deleting = look.name }) { if (unsaved) asking = look.name else choose(look) } }
            })
            Spacer(Modifier.height(10.dp))
        }
        if (unsaved) HintLine(Icons.Rounded.Info, stringResource(R.string.phone_looks_unsaved))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton(stringResource(R.string.phone_looks_save)) { naming = true }
            PillButton(stringResource(R.string.phone_looks_open), main = false) { open.launch(arrayOf("application/json", "text/plain", "*/*")) }
        }
        HintLine(Icons.Rounded.Image, stringResource(R.string.phone_looks_no_photo))
        HintLine(Icons.Rounded.Info, stringResource(R.string.phone_looks_what))
    }

    asking?.let { name ->
        val look = looks.firstOrNull { it.name == name }
        if (look == null) { asking = null; return@let }
        AlertDialog(
            onDismissRequest = { asking = null },
            title = { Text(stringResource(R.string.phone_looks_replace_title, look.name)) },
            text = { Text(stringResource(R.string.phone_looks_replace_text)) },
            confirmButton = { TextButton(onClick = { asking = null; choose(look) }) { Text(stringResource(R.string.phone_looks_replace_action)) } },
            dismissButton = { TextButton(onClick = { asking = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
    if (naming) {
        NameDialog(stringResource(R.string.phone_looks_save_title), "", onDismiss = { naming = false }) { name ->
            naming = false
            keep(MyPlayLooks.saved(mine, MyPlayLooks.current(settings.prefs.all.orEmpty(), name)))
            changes++
        }
    }
    opened?.let { look ->
        NameDialog(stringResource(R.string.phone_looks_add_title), look.name, onDismiss = { opened = null }) { name ->
            opened = null
            keep(MyPlayLooks.saved(mine, look.copy(name = name)))
        }
    }
    deleting?.let { name ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.phone_looks_delete_title, name)) },
            text = { Text(stringResource(R.string.phone_looks_delete_text)) },
            confirmButton = { TextButton(onClick = { deleting = null; keep(mine.filter { it.name != name }) }) { Text(stringResource(R.string.phone_looks_delete_action)) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

/** Asks for a look's name. A built-in look's name cannot be taken; one of the owner's is replaced. */
@Composable
private fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    val clean = MyPlayLooks.cleanName(name)
    val builtIn = MyPlayLooks.BUILT_IN.any { it.name.equals(clean, ignoreCase = true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                name, { name = it.take(MyPlayLooks.MAX_NAME) }, singleLine = true,
                label = { Text(stringResource(R.string.phone_looks_name)) },
                isError = builtIn,
                supportingText = if (builtIn) ({ Text(stringResource(R.string.phone_looks_name_built_in)) }) else null,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PhoneColors.Accent, unfocusedBorderColor = PhoneColors.Border),
            )
        },
        confirmButton = { TextButton(onClick = { onDone(clean) }, enabled = clean.isNotEmpty() && !builtIn) { Text(stringResource(R.string.phone_looks_save_action)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

/** One look: its colours, its name, a tick when it is the look now set, and share (and delete for the owner's own). */
@Composable
private fun LookRow(look: Look, chosen: Boolean, dots: List<Color>, onShare: () -> Unit, onDelete: (() -> Unit)? = null, onChoose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.RadioButton, onClick = onChoose).semantics { selected = chosen }
            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.clip(RoundedCornerShape(14.dp)).background(dots[0]).border(1.dp, PhoneColors.Border, RoundedCornerShape(14.dp))
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) { dots.drop(1).forEach { Box(Modifier.size(12.dp).clip(CircleShape).background(it)) } }
        Spacer(Modifier.width(14.dp))
        Text(look.name, Modifier.weight(1f), color = PhoneColors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        if (chosen) Icon(Icons.Rounded.CheckCircle, null, tint = PhoneColors.Accent)
        IconButton(onClick = onShare) { Icon(Icons.Rounded.Share, stringResource(R.string.phone_looks_share, look.name), tint = PhoneColors.Muted) }
        if (onDelete != null) IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, stringResource(R.string.phone_looks_delete, look.name), tint = PhoneColors.Muted) }
    }
}
