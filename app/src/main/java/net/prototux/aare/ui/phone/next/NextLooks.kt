package net.prototux.aare.ui.phone.next

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.prototux.aare.DiagLog
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.SettingsBackup
import net.prototux.aare.ui.phone.Look
import net.prototux.aare.ui.phone.MyPlayLooks
import net.prototux.aare.ui.phone.lookDots
import net.prototux.aare.ui.phone.lookFileName
import net.prototux.aare.ui.phone.lookHardToRead
import net.prototux.aare.ui.phone.myplayLooks
import net.prototux.aare.ui.phone.nameFromFile

/** Screen › Looks (frames 11, 12): built-in and saved looks; each is applied through a sheet. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NextLooksPage() {
    val ctx = LocalContext.current
    val undo = LocalNextUndo.current
    val scope = rememberCoroutineScope()
    val settings = remember { Settings(ctx) }
    // Any change can change which look is in use.
    val all by rememberSetting { (prefs.all as Map<String, *>?).orEmpty() }
    val mine by rememberSetting("myplay_looks") { myplayLooks }
    var applying by rememberSaveable { mutableStateOf<String?>(null) }
    var naming by rememberSaveable { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf<String?>(null) }
    var sharing by rememberSaveable { mutableStateOf<String?>(null) }
    var opened by remember { mutableStateOf<Look?>(null) }
    val looks = MyPlayLooks.BUILT_IN + mine
    val unsaved = looks.none { MyPlayLooks.isCurrent(all, it) }
    val deleted = stringResource(R.string.next_looks_deleted_label)

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
                // On failure only the kind is logged: a JSON error's message quotes the whole file.
                runCatching {
                    val bytes = ctx.contentResolver.openInputStream(uri)!!.use { SettingsBackup.readLimited(it) }
                    MyPlayLooks.fromFile(org.json.JSONObject(bytes.toString(Charsets.UTF_8)), nameFromFile(uri.lastPathSegment))
                }.onFailure { DiagLog.add("look import failed: ${it.javaClass.simpleName}") }.getOrNull()
            }
            if (look == null) Toast.makeText(ctx, R.string.phone_looks_open_failed, Toast.LENGTH_LONG).show() else opened = look
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().testTag("next-page-screen/looks"),
        state = rememberNextListState(),
        contentPadding = PaddingValues(start = NextSpace.Gutter, end = NextSpace.Gutter, bottom = 24.dp),
    ) {
        if (unsaved) item { NextNote(stringResource(R.string.next_looks_unsaved_help), Icons.Outlined.Info) }
        item { NextSection(stringResource(R.string.next_looks_built_in_label)) }
        item {
            LookGrid(MyPlayLooks.BUILT_IN.map { look ->
                @Composable { LookCard(look, MyPlayLooks.isCurrent(all, look), menu = null) { applying = look.name } }
            })
        }
        item { NextSection(stringResource(R.string.next_looks_yours_label)) }
        item {
            LookGrid(mine.map { look ->
                @Composable {
                    LookCard(look, MyPlayLooks.isCurrent(all, look), menu = LookMenu(
                        rename = { renaming = look.name },
                        share = { sharing = look.name; share.launch(lookFileName(look.name)) },
                        delete = {
                            val before = settings.myplayLooks
                            settings.myplayLooks = before.filter { it.name != look.name }
                            undo.show(deleted) { settings.myplayLooks = before }
                        },
                    )) { applying = look.name }
                }
            } + if (unsaved) listOf(@Composable { SaveCard { naming = true } }) else emptyList())
        }
        item {
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                NextButton(stringResource(R.string.next_looks_open_label), { open.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    kind = ButtonKind.TEXT, icon = Icons.Outlined.FileOpen)
            }
            NextNote(stringResource(R.string.next_scr_photo_private_help), Icons.Outlined.Info)
        }
    }

    applying?.let { name ->
        val look = looks.firstOrNull { it.name == name } ?: run { applying = null; return@let }
        ApplySheet(look, all, unsaved, onDismiss = { applying = null }) {
            applying = null
            MyPlayLooks.choose(settings, look)
        }
    }
    fun nameError(taken: Collection<String>): (String) -> String? = { raw ->
        val clean = MyPlayLooks.cleanName(raw)
        when {
            MyPlayLooks.BUILT_IN.any { it.name.equals(clean, ignoreCase = true) } -> ctx.getString(R.string.next_looks_name_built_in_help)
            taken.any { it.equals(clean, ignoreCase = true) } -> ctx.getString(R.string.next_looks_name_taken_help)
            else -> null
        }
    }
    if (naming) NextTextDialog(
        stringResource(R.string.next_looks_save_label), "", stringResource(R.string.next_looks_name_label),
        stringResource(R.string.next_looks_save_button_label), onDismiss = { naming = false },
        maxLength = MyPlayLooks.MAX_NAME, error = nameError(emptyList()),
    ) { raw ->
        naming = false
        settings.myplayLooks = MyPlayLooks.saved(mine, MyPlayLooks.current(all, MyPlayLooks.cleanName(raw)))
    }
    renaming?.let { old ->
        NextTextDialog(
            stringResource(R.string.next_looks_rename_label), old, stringResource(R.string.next_looks_name_label),
            stringResource(R.string.next_looks_save_button_label), onDismiss = { renaming = null },
            maxLength = MyPlayLooks.MAX_NAME, error = nameError(mine.map { it.name }.filter { it != old }),
        ) { raw ->
            renaming = null
            val clean = MyPlayLooks.cleanName(raw)
            settings.myplayLooks = mine.map { if (it.name == old) it.copy(name = clean) else it }
        }
    }
    opened?.let { look ->
        NextTextDialog(
            stringResource(R.string.next_looks_add_label), look.name, stringResource(R.string.next_looks_name_label),
            stringResource(R.string.next_looks_save_button_label), onDismiss = { opened = null },
            sub = if (lookHardToRead(look, ctx)) stringResource(R.string.next_looks_hard_help) else null,
            maxLength = MyPlayLooks.MAX_NAME, error = nameError(emptyList()),
        ) { raw ->
            opened = null
            settings.myplayLooks = MyPlayLooks.saved(mine, look.copy(name = MyPlayLooks.cleanName(raw)))
        }
    }
}

private class LookMenu(val rename: () -> Unit, val share: () -> Unit, val delete: () -> Unit)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LookGrid(cards: List<@Composable () -> Unit>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        cards.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { Box(Modifier.weight(1f)) { it() } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** A look as a card: a ring, a line and a disc in its night colours, its name, and "In use". */
@Composable
private fun LookCard(look: Look, inUse: Boolean, menu: LookMenu?, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val dots = remember(look) { lookDots(look, ctx) }
    val shape = RoundedCornerShape(NextSpace.CardRadius)
    var open by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().clip(shape).background(NextColors.Surface)
            .border(if (inUse) 2.dp else 1.dp, if (inUse) NextColors.Int else NextColors.Edge, shape)
            .clickable(role = Role.Button, onClick = onClick).semantics { selected = inUse }.padding(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(16.dp)).background(dots[0]).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(34.dp).border(3.dp, dots[2], CircleShape))
            Box(Modifier.padding(horizontal = 8.dp).width(26.dp).height(4.dp).clip(CircleShape).background(dots[2].copy(alpha = 0.7f)))
            Box(Modifier.size(38.dp).clip(CircleShape).background(dots[2]))
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(look.name, style = NextType.Label, color = NextColors.Text, maxLines = 1, modifier = Modifier.weight(1f))
            if (inUse) NextTag(stringResource(R.string.next_scr_in_use_label), TagKind.MUTE)
            if (menu != null) {
                val more = stringResource(R.string.next_looks_menu_label, look.name)
                Box {
                    Box(
                        Modifier.minimumInteractiveComponentSize().clip(CircleShape).clickable(role = Role.Button) { open = true }
                            .semantics { contentDescription = more },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.MoreVert, null, tint = NextColors.Text2) }
                    DropdownMenu(open, { open = false }, containerColor = NextColors.Menu) {
                        DropdownMenuItem({ Text(stringResource(R.string.next_looks_rename_label), color = NextColors.Text) },
                            { open = false; menu.rename() }, leadingIcon = { Icon(Icons.Outlined.DriveFileRenameOutline, null, tint = NextColors.Text2) })
                        DropdownMenuItem({ Text(stringResource(R.string.next_looks_share_label), color = NextColors.Text) },
                            { open = false; menu.share() }, leadingIcon = { Icon(Icons.Outlined.Share, null, tint = NextColors.Text2) })
                        DropdownMenuItem({ Text(stringResource(R.string.next_looks_delete_label), color = NextColors.Warn) },
                            { open = false; menu.delete() }, leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = NextColors.Warn) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveCard(onClick: () -> Unit) {
    val shape = RoundedCornerShape(NextSpace.CardRadius)
    Column(
        Modifier.fillMaxWidth().height(138.dp).clip(shape).border(1.5.dp, NextColors.Outline, shape)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(40.dp).border(1.5.dp, NextColors.Text2, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Add, null, tint = NextColors.Text2)
        }
        Text(stringResource(R.string.next_looks_save_label), style = NextType.Value, color = NextColors.Text2, modifier = Modifier.padding(top = 10.dp))
    }
}

/** Before a look is applied (plan §6.1): what it changes, and a confirm when it switches the design. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ApplySheet(look: Look, all: Map<String, *>, unsaved: Boolean, onDismiss: () -> Unit, onApply: () -> Unit) {
    val changes = remember(look, all) { lookChanges(all, look) }
    val design = remember(look, all) { lookSwitchesDesign(all, look) }
    NextSheet(stringResource(R.string.next_looks_use_label, look.name), onDismiss,
        sub = stringResource(if (changes.isEmpty()) R.string.next_looks_no_change_help else R.string.next_looks_changes_help)) {
        if (changes.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            changes.forEach { NextTag(lookPartWord(it), TagKind.MUTE) }
        }
        if (design != null) {
            Spacer(Modifier.height(16.dp))
            NextCard {
                NextRow(
                    stringResource(R.string.next_looks_uses_label, design.uppercase()), icon = Icons.Rounded.WarningAmber, disc = DiscTone.WARN,
                    note = RowNote.Help(stringResource(R.string.next_looks_switches_help, if (design == "deck") "ORBIT" else "DECK")),
                )
            }
        }
        if (unsaved) NextNote(stringResource(R.string.next_looks_unsaved_help), Icons.Outlined.Info)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NextButton(stringResource(android.R.string.cancel), onDismiss, Modifier.weight(1f), ButtonKind.GHOST)
            NextButton(stringResource(R.string.next_looks_use_it_label), onApply, Modifier.weight(1f))
        }
    }
}

@Composable
private fun lookPartWord(p: LookPart): String = stringResource(when (p) {
    LookPart.DESIGN -> R.string.next_screen_design_label
    LookPart.COLOURS -> R.string.next_screen_colours_label
    LookPart.WALLPAPER -> R.string.next_screen_wallpaper_label
    LookPart.MAP_COLOURS -> R.string.next_screen_map_colours_label
    LookPart.OLDER_LAYOUTS -> R.string.next_looks_older_label
})

