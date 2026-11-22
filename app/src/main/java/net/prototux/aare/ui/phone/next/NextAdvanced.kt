package net.prototux.aare.ui.phone.next

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Subject
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.rounded.Download
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.car.VehicleOverrides
import net.prototux.aare.cert.CertificateDownload
import net.prototux.aare.cert.CertificateStore
import net.prototux.aare.cert.CertificateUpdates
import net.prototux.aare.service.ProjectionService
import net.prototux.aare.host.myplayDebugNavText
import net.prototux.aare.host.templates.myplayDebugMapAreas
import java.text.DateFormat

@Composable
private fun Page(route: String, content: LazyListScope.() -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().testTag("next-page-$route"),
        state = rememberNextListState(),
        contentPadding = PaddingValues(start = NextSpace.Gutter, end = NextSpace.Gutter, bottom = 24.dp),
        content = content,
    )
}

/** The video heights offered, as today (480p to 1440p). */
internal val VIDEO_HEIGHTS = listOf(480, 720, 1080, 1440)

/** More › Advanced (frame 63): video, certificates, the per-car fixes file, Developer. */
@Composable
fun NextAdvancedPage() {
    val ctx = LocalContext.current
    val nav = LocalNextNavigator.current
    val settings = remember { Settings(ctx) }
    val maxH by rememberSetting("max_video_height") { maxVideoHeight }
    val fps60 by rememberSetting("prefer_60fps") { prefer60fps }
    val h264 by rememberSetting("force_h264") { forceH264 }
    val cbr by rememberSetting("constant_bitrate_video") { constantBitrateVideo }
    var certCount by remember { mutableIntStateOf(-1) }
    LaunchedEffect(Unit) { certCount = withContext(Dispatchers.IO) { runCatching { CertificateStore(ctx).entries().size }.getOrDefault(0) } }
    Page("more/advanced") {
        item { NextSection(stringResource(R.string.next_adv_video_label), first = true, extra = { NextConnectionTag() }) }
        item {
            NextGroup(listOf(
                {
                    NextPickerRow(stringResource(R.string.next_adv_resolution_label), VIDEO_HEIGHTS.map { Choice(it, "${it}p") }, maxH,
                        { settings.maxVideoHeight = it }, default = 1080, info = stringResource(R.string.setup_hint_lower_resolution))
                },
                { NextSwitchRow(stringResource(R.string.next_adv_60fps_label), fps60, { settings.prefer60fps = it }, changed = !fps60,
                    info = stringResource(R.string.setup_toggle_60fps_subtitle)) },
                { NextSwitchRow(stringResource(R.string.next_adv_h264_label), h264, { settings.forceH264 = it }, changed = h264,
                    info = stringResource(R.string.setup_toggle_force_h264_subtitle)) },
                { NextSwitchRow(stringResource(R.string.next_adv_cbr_label), cbr, { settings.constantBitrateVideo = it }, changed = cbr,
                    info = stringResource(R.string.setup_toggle_constant_bitrate_subtitle)) },
            ), inset = 16.dp)
        }
        item { NextSection(stringResource(R.string.next_adv_security_label)) }
        item {
            NextCard {
                NextValueRow(stringResource(R.string.next_more_certificates_label),
                    if (certCount < 0) null else pluralStringResource(R.plurals.next_adv_cert_count_label, certCount, certCount),
                    Icons.Outlined.VerifiedUser) { nav.open("more/advanced/certificates") }
            }
        }
        item { NextSection(stringResource(R.string.next_adv_car_fixes_label)) }
        item { CarFixesCard() }
        item {
            Spacer(Modifier.height(12.dp))
            NextCard { NextValueRow(stringResource(R.string.next_more_developer_label), icon = Icons.Outlined.Code) { nav.open("more/advanced/developer") } }
        }
    }
}

/**
 * vehicles.json: a fix for one car's density or margins without a new build. Import and Template
 * both ask before replacing a file that is already there (plan §6).
 */
@Composable
private fun CarFixesCard() {
    val ctx = LocalContext.current
    val file = remember { VehicleOverrides.file(ctx) }
    var count by remember { mutableIntStateOf(VehicleOverrides.ruleCount(ctx)) }
    var confirm by rememberSaveable { mutableStateOf<String?>(null) } // "import" or "template"
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching { ctx.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } } }
            .onFailure { Toast.makeText(ctx, it.message, Toast.LENGTH_SHORT).show() }
        count = VehicleOverrides.ruleCount(ctx)
    }
    fun doImport() = import.launch(arrayOf("application/json", "text/plain", "*/*"))
    fun doTemplate() {
        runCatching { file.parentFile?.mkdirs(); file.writeText(VehicleOverrides.template()) }
            .onFailure { Toast.makeText(ctx, it.message, Toast.LENGTH_SHORT).show() }
        count = VehicleOverrides.ruleCount(ctx)
        Toast.makeText(ctx, file.absolutePath, Toast.LENGTH_LONG).show()
    }
    NextCard {
        NextRow(stringResource(R.string.next_adv_overrides_label), icon = Icons.Outlined.Build,
            note = RowNote.Help(file.name), info = stringResource(R.string.vehicles_hint)) {
            NextValue(pluralStringResource(R.plurals.next_adv_rules_label, count, count))
        }
        Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NextButton(stringResource(R.string.next_adv_import_label), { if (file.exists()) confirm = "import" else doImport() },
                Modifier.weight(1f), ButtonKind.GHOST, Icons.Outlined.Description, small = true)
            NextButton(stringResource(R.string.next_adv_template_label), { if (file.exists()) confirm = "template" else doTemplate() },
                Modifier.weight(1f), ButtonKind.GHOST, small = true)
        }
    }
    confirm?.let { what ->
        NextConfirm(
            stringResource(R.string.next_adv_replace_title_label), stringResource(R.string.next_adv_replace_info, count),
            stringResource(R.string.next_adv_replace_label),
            onConfirm = { confirm = null; if (what == "import") doImport() else doTemplate() }, onDismiss = { confirm = null }, danger = true,
        )
    }
}

// ---------------------------------------------------------------- Certificates

/** A certificate's name in plain words: the store's own names are for the log. */
@Composable
private fun certName(e: CertificateStore.Entry): String = when {
    e.name == CertificateStore.BUILT_IN -> stringResource(R.string.next_cert_built_in_label)
    e.name == CertificateStore.GENERATED -> stringResource(R.string.next_cert_generated_label)
    e.name.startsWith(CertificateStore.DOWNLOADED_PREFIX) -> stringResource(R.string.next_cert_downloaded_label)
    else -> e.name
}

/** More › Advanced › Certificates (frames 65–66): the list, Import, Download, Update automatically. */
@Composable
fun NextCertificatesPage() {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    val certs = remember { CertificateStore(ctx) }
    var entries by remember { mutableStateOf<List<CertificateStore.Entry>>(emptyList()) }
    val scope = rememberCoroutineScope()
    fun reload() { scope.launch { entries = withContext(Dispatchers.IO) { certs.entries() } } }
    LaunchedEffect(Unit) { reload() }
    // Saveable: the system picker can outlive the activity, and the key picker's result needs the certificate.
    var pickCert by rememberSaveable { mutableStateOf<Uri?>(null) }
    var p12 by rememberSaveable { mutableStateOf<Uri?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var remove by remember { mutableStateOf<CertificateStore.Entry?>(null) }
    val auto by rememberSetting("cert_auto_update") { certAutoUpdate }
    val keyPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { keyUri ->
        val certUri = pickCert ?: return@rememberLauncherForActivityResult
        if (keyUri == null) return@rememberLauncherForActivityResult
        error = runCatching {
            val cert = ctx.contentResolver.openInputStream(certUri)!!.use { it.readBytes().decodeToString() }
            val key = ctx.contentResolver.openInputStream(keyUri)!!.use { it.readBytes().decodeToString() }
            certs.importPem(displayName(ctx, certUri), cert, key)
            CertificateUpdates.refresh(ctx)
        }.exceptionOrNull()?.message
        reload()
    }
    val certPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        // Document URIs (msf:1234) carry no extension: ask the provider for the file name and type.
        val name = displayName(ctx, uri)
        val type = ctx.contentResolver.getType(uri)
        if (name.endsWith(".p12", true) || name.endsWith(".pfx", true) || type == "application/x-pkcs12") p12 = uri
        else { pickCert = uri; keyPicker.launch(arrayOf("*/*")) }
    }
    Page("more/advanced/certificates") {
        item {
            NextCard {
                entries.forEachIndexed { i, e ->
                    if (i > 0) NextDivider()
                    val until = stringResource(R.string.next_cert_until_help, DateFormat.getDateInstance(DateFormat.MEDIUM).format(e.notAfter))
                    NextRow(certName(e), icon = if (e.removable) Icons.Outlined.Key else Icons.Outlined.VerifiedUser,
                        note = if (e.notAfter.before(java.util.Date())) RowNote.Warn(stringResource(R.string.next_cert_expired_help)) else RowNote.Help(until)) {
                        if (e.removable) NextButton(stringResource(R.string.next_cert_remove_label), { remove = e }, kind = ButtonKind.DANGER, small = true)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NextButton(stringResource(R.string.next_cert_import_label), { certPicker.launch(arrayOf("*/*")) }, Modifier.weight(1f),
                    ButtonKind.GHOST, Icons.Outlined.Description)
                NextButton(stringResource(if (downloading) R.string.next_cert_downloading_label else R.string.next_cert_download_label), {
                    downloading = true; error = null
                    scope.launch {
                        error = runCatching {
                            val d = withContext(Dispatchers.IO) { CertificateDownload.fetch() }
                            withContext(Dispatchers.IO) { certs.importDownloaded(d) }
                            CertificateUpdates.refresh(ctx)
                        }.exceptionOrNull()?.let { certDownloadError(ctx, it) }
                        reload()
                        downloading = false
                    }
                }, Modifier.weight(1f), ButtonKind.GHOST, Icons.Rounded.Download, enabled = !downloading)
            }
            error?.let { NextWarnLine(it) }
            Spacer(Modifier.height(12.dp))
        }
        item {
            NextCard {
                NextSwitchRow(stringResource(R.string.next_cert_auto_label), auto, { settings.certAutoUpdate = it; CertificateUpdates.refresh(ctx) },
                    Icons.Outlined.Sync, info = stringResource(R.string.cert_auto_update_subtitle))
            }
            NextNote(stringResource(R.string.next_cert_why_info))
        }
    }
    p12?.let { uri ->
        // The password is not kept: after rotation the dialog opens empty (plan §3.4 rule 8).
        NextTextDialog(stringResource(R.string.next_cert_password_title_label), "", stringResource(R.string.next_cert_password_label),
            stringResource(R.string.next_cert_import_label), onDismiss = { p12 = null }, password = true) { password ->
            error = runCatching { certs.importPkcs12(displayName(ctx, uri), uri, password.toCharArray()); CertificateUpdates.refresh(ctx) }
                .exceptionOrNull()?.let { ctx.getString(R.string.next_cert_password_wrong_help) }
            p12 = null
            reload()
        }
    }
    remove?.let { e ->
        NextConfirm(stringResource(R.string.next_cert_remove_title_label), stringResource(R.string.next_cert_remove_info),
            stringResource(R.string.next_cert_remove_label),
            onConfirm = {
                remove = null
                scope.launch { withContext(Dispatchers.IO) { certs.remove(e) }; CertificateUpdates.refresh(ctx); reload() }
            }, onDismiss = { remove = null }, danger = true)
    }
}

/** What to say when Download fails. */
private fun certDownloadError(ctx: Context, e: Throwable): String = when (e) {
    is CertificateDownload.Unavailable -> ctx.getString(R.string.cert_download_unavailable, CertificateDownload.SOURCE)
    is CertificateDownload.Invalid -> ctx.getString(R.string.cert_download_invalid, e.message)
    is java.io.IOException -> ctx.getString(R.string.cert_download_network, e.message ?: e.toString())
    else -> e.message ?: e.toString()
}

/** The picked file's name as the provider shows it, for naming an imported certificate. */
private fun displayName(ctx: Context, uri: Uri): String =
    runCatching {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/') ?: ctx.getString(R.string.cert_imported_fallback)

// ---------------------------------------------------------------- Developer

/**
 * More › Advanced › Developer (frame 67): the head unit server and its four options, each risky one
 * with its warning line; the map-area outline, the maps-text log, the parts sample, and a link to the
 * detailed protocol log, which lives in the Connection log only (plan §6, review 12).
 */
@Composable
fun NextDeveloperPage() {
    val ctx = LocalContext.current
    val nav = LocalNextNavigator.current
    val settings = remember { Settings(ctx) }
    val server by rememberSetting("hu_server") { headUnitServer }
    // The service turns the network option off when a session ends: observed, so that shows at once.
    val lan by rememberSetting("hu_server_lan") { headUnitServerLan }
    val lanOnce by rememberSetting("hu_server_lan_once") { headUnitServerLanOnce }
    val mirror by rememberSetting("head_unit_server_mirror") { headUnitServerMirror }
    val localhost by rememberSetting("hu_server_localhost") { headUnitServerLocalhost }
    val mapAreas by rememberSetting("myplay_debug_map_areas") { myplayDebugMapAreas }
    val navText by rememberSetting("myplay_debug_nav_text") { myplayDebugNavText }
    Page("more/advanced/developer") {
        item {
            val rows = mutableListOf<@Composable () -> Unit>(
                {
                    NextSwitchRow(stringResource(R.string.huserver_label), server, {
                        settings.headUnitServer = it
                        // Off only closes the listener; the service goes away by itself when it has nothing left.
                        ProjectionService.startIdleIfEnabled(ctx)
                    }, Icons.Outlined.Terminal, info = stringResource(R.string.huserver_toggle_subtitle))
                },
            )
            if (server) {
                rows += {
                    NextSwitchRow(stringResource(R.string.next_dev_network_label), lan, { settings.headUnitServerLan = it; ProjectionService.startIdleIfEnabled(ctx) },
                        note = RowNote.Warn(stringResource(R.string.next_dev_network_help)), info = stringResource(R.string.huserver_toggle_network_subtitle),
                        lead = { Spacer(Modifier.size(NextSpace.IconDisc)) })
                }
                rows += {
                    NextSwitchRow(stringResource(R.string.next_dev_once_label), lanOnce, { settings.headUnitServerLanOnce = it },
                        info = stringResource(R.string.huserver_toggle_lanonce_subtitle), lead = { Spacer(Modifier.size(NextSpace.IconDisc)) })
                }
                rows += {
                    NextSwitchRow(stringResource(R.string.next_dev_mirror_label), mirror, { settings.headUnitServerMirror = it },
                        note = RowNote.Warn(stringResource(R.string.next_dev_mirror_help)), info = stringResource(R.string.huserver_toggle_mirror_subtitle),
                        lead = { Spacer(Modifier.size(NextSpace.IconDisc)) })
                }
                rows += {
                    NextSwitchRow(stringResource(R.string.next_dev_localhost_label), localhost, { settings.headUnitServerLocalhost = it; ProjectionService.startIdleIfEnabled(ctx) },
                        note = RowNote.Warn(stringResource(R.string.next_dev_localhost_help)), info = stringResource(R.string.huserver_toggle_localhost_subtitle),
                        lead = { Spacer(Modifier.size(NextSpace.IconDisc)) })
                }
            }
            NextGroup(rows)
            Spacer(Modifier.height(12.dp))
        }
        item {
            NextGroup(listOf(
                { NextSwitchRow(stringResource(R.string.next_dev_map_areas_label), mapAreas, { settings.myplayDebugMapAreas = it }, Icons.Outlined.Layers,
                    info = stringResource(R.string.phone_map_areas_hint)) },
                { NextSwitchRow(stringResource(R.string.next_dev_nav_text_label), navText, { settings.myplayDebugNavText = it }, Icons.AutoMirrored.Outlined.Subject,
                    info = stringResource(R.string.phone_nav_text_hint)) },
                { NextValueRow(stringResource(R.string.next_dev_parts_label), icon = Icons.Outlined.Widgets) { nav.open("more/advanced/developer/parts") } },
                {
                    NextLinkRow(stringResource(R.string.next_diag_verbose_label), icon = Icons.Outlined.CropFree,
                        note = RowNote.Help(stringResource(R.string.next_dev_lives_in_log_help))) { nav.open("more/diagnostics/log") }
                },
            ))
        }
    }
}
