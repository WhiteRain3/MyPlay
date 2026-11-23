package net.prototux.aare.ui.phone.next

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.prototux.aare.R
import net.prototux.aare.audio.CaptureMethods
import rikka.shizuku.Shizuku

/**
 * How Phone apps can launch apps for the car, for its card (one step at a time). Root first (the
 * launcher uses su directly), then Shizuku; UNSUPPORTED when neither can work on this phone.
 */
enum class ShizukuStep { INSTALL, START, ALLOW, READY, ROOT_READY, ROOT_ALLOW, OLD_SHIZUKU, OLD_ANDROID }

/** Shizuku's own package, to tell "not installed" from "not running". */
internal const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
internal const val SHIZUKU_DOWNLOAD = "https://shizuku.rikka.app/download/"
private const val SHIZUKU_REQUEST = 43

fun shizukuStep(
    installed: Boolean, running: Boolean, allowed: Boolean,
    android12: Boolean = true, rootGranted: Boolean = false, suPresent: Boolean = false, preV11: Boolean = false,
): ShizukuStep = when {
    // The launcher's root and Shizuku paths both need Android 12 (CaptureMethods' gates).
    !android12 -> ShizukuStep.OLD_ANDROID
    rootGranted -> ShizukuStep.ROOT_READY
    suPresent -> ShizukuStep.ROOT_ALLOW
    running && preV11 -> ShizukuStep.OLD_SHIZUKU
    running && allowed -> ShizukuStep.READY
    running -> ShizukuStep.ALLOW
    installed -> ShizukuStep.START
    else -> ShizukuStep.INSTALL
}

/** An su binary on the usual paths: a rooted phone, whether or not MyPlay was allowed yet. */
internal fun suPresent(): Boolean =
    listOf("/system/bin", "/system/xbin", "/sbin", "/su/bin", "/debug_ramdisk", "/system_ext/bin")
        .any { runCatching { java.io.File(it, "su").exists() }.getOrDefault(false) }

/** Tests set this: a computer running them may have its own su. */
internal var suPresentCheck: () -> Boolean = ::suPresent

internal fun shizukuStepNow(ctx: Context): ShizukuStep {
    val installed = runCatching { ctx.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0) }.isSuccess
    val running = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
    // Review PH-1: "allowed" is Shizuku's own answer, not shizukuAvailable() (which also folds in the gates).
    val allowed = running && runCatching { Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED }.getOrDefault(false)
    val preV11 = running && runCatching { Shizuku.isPreV11() }.getOrDefault(false)
    return shizukuStep(installed, running, allowed,
        android12 = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S,
        rootGranted = CaptureMethods.rootAvailable(ctx), suPresent = suPresentCheck(), preV11 = preV11)
}

/** Re-reads the step on resume and when Shizuku answers the Allow question. */
@Composable
internal fun rememberShizukuStep(): ShizukuStep {
    val ctx = LocalContext.current
    val resumed = rememberResumeTicks()
    var answered by remember { mutableIntStateOf(0) }
    val rootAnswered by rememberSetting("root_audio") { rootAudio }
    DisposableEffect(Unit) {
        val l = Shizuku.OnRequestPermissionResultListener { _, _ -> answered++ }
        runCatching { Shizuku.addRequestPermissionResultListener(l) }
        onDispose { runCatching { Shizuku.removeRequestPermissionResultListener(l) } }
    }
    return remember(resumed, answered, rootAnswered) { shizukuStepNow(ctx) }
}

internal fun askShizuku() { runCatching { Shizuku.requestPermission(SHIZUKU_REQUEST) } }

private val SHORT_STEPS = listOf(
    ShizukuStep.INSTALL to R.string.next_shz_step_install_label,
    ShizukuStep.START to R.string.next_shz_step_start_label,
    ShizukuStep.ALLOW to R.string.next_shz_step_allow_label,
)

/** A numbered step: done (tick), the one to do now (bright), or later (dim). */
@Composable
private fun StepLine(number: Int, text: String, state: Int /* -1 done, 0 now, 1 later */, help: String? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(if (state == 0) NextColors.Int else NextColors.Surface2),
            contentAlignment = Alignment.Center,
        ) {
            if (state < 0) Icon(Icons.Rounded.Check, null, tint = NextColors.Ok, modifier = Modifier.size(16.dp))
            else Text("$number", style = NextType.Tag, color = if (state == 0) NextColors.On else NextColors.Text2)
        }
        Column(Modifier.weight(1f)) {
            Text(text, style = NextType.Label.copy(fontWeight = if (state == 0) FontWeight(600) else FontWeight(450)),
                color = if (state > 0) NextColors.Text3 else NextColors.Text)
            if (help != null) Text(help, style = NextType.Help, color = NextColors.Text3, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/**
 * Apps › Phone apps without Shizuku (or root): what to do, as three short steps with the one to do
 * now marked, the button for that step, and "How to set up" for the full help page.
 */
@Composable
fun ShizukuCard(step: ShizukuStep) {
    val ctx = LocalContext.current
    val nav = LocalNextNavigator.current
    when (step) {
        ShizukuStep.ROOT_READY -> {
            NextCard(modifier = Modifier.testTag("next-shizuku-card")) {
                NextRow(stringResource(R.string.next_shz_root_ready_label), icon = Icons.Rounded.Check, disc = DiscTone.WORLD,
                    note = RowNote.Help(stringResource(R.string.next_shz_root_ready_help)))
            }
            return
        }
        ShizukuStep.ROOT_ALLOW -> {
            // A rooted phone: one question, through su itself (the same check as Sound's), no Shizuku.
            NextCard(warn = true, modifier = Modifier.testTag("next-shizuku-card")) {
                NextRow(stringResource(R.string.next_shz_root_allow_title_label), icon = Icons.Outlined.Lock, disc = DiscTone.WARN,
                    note = RowNote.Help(stringResource(R.string.next_shz_root_allow_help)))
                Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
                    NextButton(stringResource(R.string.next_shz_root_allow_label), {
                        CaptureMethods.checkRoot { granted ->
                            android.os.Handler(android.os.Looper.getMainLooper()).post { if (granted) net.prototux.aare.Settings(ctx).rootAudio = true }
                        }
                    }, Modifier.fillMaxWidth(), small = true)
                }
            }
            return
        }
        ShizukuStep.OLD_ANDROID, ShizukuStep.OLD_SHIZUKU -> {
            NextCard(warn = true, modifier = Modifier.testTag("next-shizuku-card")) {
                NextRow(stringResource(if (step == ShizukuStep.OLD_ANDROID) R.string.next_shz_old_android_label else R.string.next_shz_old_shizuku_label),
                    icon = Icons.Outlined.Lock, disc = DiscTone.WARN,
                    note = RowNote.Help(stringResource(if (step == ShizukuStep.OLD_ANDROID) R.string.next_shz_old_android_help else R.string.next_shz_old_shizuku_help)))
                if (step == ShizukuStep.OLD_SHIZUKU) Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
                    NextButton(stringResource(R.string.next_shz_get_label), { ctx.startSafely(Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_DOWNLOAD))) },
                        Modifier.fillMaxWidth(), small = true, icon = Icons.AutoMirrored.Outlined.OpenInNew)
                }
            }
            return
        }
        else -> Unit
    }
    NextCard(warn = true, modifier = Modifier.testTag("next-shizuku-card")) {
        NextRow(stringResource(R.string.next_apps_needs_root_label), icon = Icons.Outlined.Lock, disc = DiscTone.WARN,
            note = RowNote.Help(stringResource(R.string.next_apps_needs_root_help)))
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)) {
            SHORT_STEPS.forEachIndexed { i, (s, text) -> StepLine(i + 1, stringResource(text), s.ordinal.compareTo(step.ordinal)) }
        }
        Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp, top = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            when (step) {
                ShizukuStep.INSTALL -> NextButton(stringResource(R.string.next_shz_get_label), {
                    ctx.startSafely(Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_DOWNLOAD)))
                }, Modifier.weight(1f), small = true, icon = Icons.AutoMirrored.Outlined.OpenInNew)
                ShizukuStep.START -> NextButton(stringResource(R.string.next_shz_open_label), {
                    ctx.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)?.let { ctx.startSafely(it) }
                }, Modifier.weight(1f), small = true, icon = Icons.AutoMirrored.Outlined.OpenInNew)
                ShizukuStep.ALLOW -> NextButton(stringResource(R.string.next_upd_allow_button_label), { askShizuku() }, Modifier.weight(1f), small = true)
                else -> Unit
            }
            NextButton(stringResource(R.string.next_shz_how_label), { nav.open("apps/phone-apps/shizuku") }, Modifier.weight(1f),
                ButtonKind.GHOST, small = true, icon = Icons.AutoMirrored.Outlined.HelpOutline)
        }
    }
}

/** Apps › Phone apps › How to set up Shizuku: the full steps, in the app, with the step to do now marked. */
@Composable
fun NextShizukuHelpPage() {
    val ctx = LocalContext.current
    val step = rememberShizukuStep()
    val steps = listOf(
        Triple(ShizukuStep.INSTALL, R.string.next_shz_h1_label, R.string.next_shz_h1_help),
        Triple(ShizukuStep.START, R.string.next_shz_h2_label, R.string.next_shz_h2_help),
        Triple(ShizukuStep.START, R.string.next_shz_h3_label, R.string.next_shz_h3_help),
        Triple(ShizukuStep.START, R.string.next_shz_h4_label, R.string.next_shz_h4_help),
        Triple(ShizukuStep.ALLOW, R.string.next_shz_h5_label, R.string.next_shz_h5_help),
    )
    LazyColumn(
        Modifier.fillMaxSize().testTag("next-page-apps/phone-apps/shizuku"),
        state = rememberNextListState(),
        contentPadding = PaddingValues(start = NextSpace.Gutter, end = NextSpace.Gutter, bottom = 24.dp),
    ) {
        item {
            if (step == ShizukuStep.READY) {
                NextCard { NextRow(stringResource(R.string.next_shz_ready_label), icon = Icons.Rounded.Check, disc = DiscTone.WORLD) }
                Spacer(Modifier.height(12.dp))
            }
            NextCard {
                Column(Modifier.padding(16.dp)) {
                    steps.forEachIndexed { i, (s, label, help) ->
                        StepLine(i + 1, stringResource(label), s.ordinal.compareTo(step.ordinal), stringResource(help))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NextButton(stringResource(R.string.next_shz_get_label), { ctx.startSafely(Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_DOWNLOAD))) },
                    Modifier.weight(1f), ButtonKind.GHOST, small = true, icon = Icons.AutoMirrored.Outlined.OpenInNew)
                NextButton(stringResource(R.string.next_shz_dev_label), {
                    ctx.startSafely(Intent(AndroidSettings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                }, Modifier.weight(1f), ButtonKind.GHOST, small = true, icon = Icons.AutoMirrored.Outlined.OpenInNew)
            }
            if (step == ShizukuStep.ALLOW) {
                Spacer(Modifier.height(12.dp))
                NextButton(stringResource(R.string.next_upd_allow_button_label), { askShizuku() }, Modifier.fillMaxWidth())
            }
            NextNote(stringResource(R.string.next_shz_restart_help))
            NextNote(stringResource(R.string.next_shz_root_help))
        }
    }
}
