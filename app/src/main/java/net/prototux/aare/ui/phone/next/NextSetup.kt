package net.prototux.aare.ui.phone.next

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.LocationSearching
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.UpdateChecker
import net.prototux.aare.service.ProjectionService
import net.prototux.aare.ui.phone.myplayUpdateAsked

/** What step 1 asks for, in three plain groups (design frame 72). */
enum class SetupNeed { NEARBY, LOCATION, NOTIFICATIONS }

internal fun permissionsOf(need: SetupNeed): List<String> = when (need) {
    SetupNeed.NEARBY -> buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { add(Manifest.permission.BLUETOOTH_CONNECT); add(Manifest.permission.BLUETOOTH_SCAN) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.NEARBY_WIFI_DEVICES)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) add(Manifest.permission.ACCESS_LOCAL_NETWORK)
    }
    SetupNeed.LOCATION -> listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    SetupNeed.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList()
}

private fun granted(ctx: Context, need: SetupNeed) =
    permissionsOf(need).all { ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED }

/**
 * First-run Setup (plan §10, frames 72–75): full screen, no tab bar, four skippable steps — what
 * connecting needs, sound, "Check for updates?", how to connect. [onDone] when finished or left.
 */
@Composable
fun NextSetup(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    var step by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = step > 0) { step-- }
    val next: () -> Unit = { if (step < 3) step++ else onDone() }
    Column(Modifier.fillMaxSize().background(NextColors.Bg).statusBarsPadding().navigationBarsPadding().testTag("next-setup")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(4) { i ->
                    Box(Modifier.height(10.dp).width(if (i == step) 28.dp else 10.dp).clip(CircleShape)
                        .background(if (i <= step) NextColors.Int else NextColors.Track))
                }
            }
            Text(stringResource(R.string.next_setup_step_label, step + 1, 4), style = NextType.Value, color = NextColors.Text2)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = NextSpace.Gutter),
            horizontalAlignment = Alignment.CenterHorizontally) {
            when (step) {
                0 -> NeedsStep()
                1 -> SoundStep()
                2 -> Head(Icons.Rounded.Download, R.string.next_setup_updates_label, R.string.next_setup_updates_help)
                else -> ConnectStep()
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = NextSpace.Gutter, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            when (step) {
                0 -> {
                    val ticks = rememberResumeTicks()
                    val all = remember(ticks) { SetupNeed.entries.all { granted(ctx, it) } }
                    val ask = rememberConnectPermissions()
                    if (all) NextButton(stringResource(R.string.next_diag_next_label), next, Modifier.fillMaxWidth())
                    else NextButton(stringResource(R.string.next_upd_allow_button_label), ask, Modifier.fillMaxWidth())
                    NextButton(stringResource(R.string.next_setup_skip_label), next, kind = ButtonKind.TEXT)
                }
                1 -> {
                    val state by ProjectionService.state.collectAsState()
                    val ticks = rememberResumeTicks()
                    val best = remember(ticks, state.audioCapture) { soundMethods(ctx, state.audioCapture).best }
                    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
                        if (r.resultCode == android.app.Activity.RESULT_OK && r.data != null) ProjectionService.giveAudioConsent(ctx, r.resultCode, r.data!!)
                        next()
                    }
                    if (best != null) NextButton(stringResource(R.string.next_diag_next_label), next, Modifier.fillMaxWidth())
                    else NextButton(stringResource(R.string.next_car_set_up_label), {
                        val mpm = ctx.getSystemService(MediaProjectionManager::class.java)
                        consent.launch(if (Build.VERSION.SDK_INT >= 34) mpm.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
                            else mpm.createScreenCaptureIntent())
                    }, Modifier.fillMaxWidth())
                    NextButton(stringResource(R.string.next_setup_skip_label), next, kind = ButtonKind.TEXT)
                }
                2 -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // The one question about automatic checks (plan §5): the answer is kept, never asked again.
                    NextButton(stringResource(R.string.next_setup_no_label), {
                        settings.checkUpdates = false; settings.myplayUpdateAsked = true; next()
                    }, Modifier.weight(1f), ButtonKind.GHOST)
                    NextButton(stringResource(R.string.next_setup_yes_label), {
                        settings.checkUpdates = true; settings.myplayUpdateAsked = true; UpdateChecker.refresh(ctx); next()
                    }, Modifier.weight(1f))
                }
                else -> {
                    NextButton(stringResource(R.string.next_setup_done_label), onDone, Modifier.fillMaxWidth())
                    NextButton(stringResource(R.string.next_setup_back_label), { step-- }, kind = ButtonKind.TEXT)
                }
            }
        }
    }
}

@Composable
private fun Head(icon: androidx.compose.ui.graphics.vector.ImageVector, title: Int, help: Int) {
    Spacer(Modifier.height(24.dp))
    NextHeroRing(RingStyle.FULL, NextTab.HOME.world, icon)
    Text(stringResource(title), style = NextType.Hero, color = NextColors.Text, textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 20.dp).semantics { heading() })
    Text(stringResource(help), style = NextType.Value, color = NextColors.Text2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun NeedsStep() {
    val ctx = LocalContext.current
    val ticks = rememberResumeTicks()
    Spacer(Modifier.height(24.dp))
    NextHeroRing(RingStyle.FULL, NextTab.HOME.world, Icons.Outlined.LocationSearching)
    Text(stringResource(R.string.next_setup_find_label), style = NextType.Hero, color = NextColors.Text, modifier = Modifier.padding(top = 20.dp).semantics { heading() })
    Text(stringResource(R.string.next_setup_find_help), style = NextType.Value, color = NextColors.Text2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
    Spacer(Modifier.height(24.dp))
    val allowed = stringResource(R.string.next_setup_allowed_label)
    NextCard {
        SetupNeed.entries.forEachIndexed { i, need ->
            if (i > 0) NextDivider()
            val ok = remember(ticks) { granted(ctx, need) }
            val (label, help, icon) = when (need) {
                SetupNeed.NEARBY -> Triple(R.string.next_setup_nearby_label, R.string.next_setup_nearby_help, Icons.Outlined.Bluetooth)
                SetupNeed.LOCATION -> Triple(R.string.next_setup_location_label, R.string.next_setup_location_help, Icons.Outlined.LocationOn)
                SetupNeed.NOTIFICATIONS -> Triple(R.string.next_setup_notifications_label, R.string.next_setup_notifications_help, Icons.Outlined.Notifications)
            }
            NextRow(stringResource(label), icon = icon, note = RowNote.Help(stringResource(help))) { if (ok) NextTag(allowed, TagKind.OK) }
        }
    }
}

@Composable
private fun SoundStep() {
    val ctx = LocalContext.current
    val state by ProjectionService.state.collectAsState()
    val ticks = rememberResumeTicks()
    val best = remember(ticks, state.audioCapture) { soundMethods(ctx, state.audioCapture).best }
    Head(Icons.AutoMirrored.Outlined.VolumeUp, R.string.next_setup_sound_label, R.string.next_setup_sound_help)
    NextCard {
        if (best != null) NextRow(stringResource(R.string.next_sound_ready_label), icon = Icons.AutoMirrored.Outlined.VolumeUp,
            note = RowNote.Help(stringResource(R.string.next_sound_ready_help))) { NextTag(stringResource(R.string.next_setup_allowed_label), TagKind.OK) }
        else NextRow(stringResource(R.string.next_sound_capture_label), icon = Icons.Outlined.PhoneAndroid,
            note = RowNote.Help(stringResource(R.string.next_setup_capture_help))) { NextTag(stringResource(R.string.next_setup_best_here_label), TagKind.MUTE) }
    }
    if (best == null) Box(Modifier.fillMaxWidth()) { NextNote(stringResource(R.string.next_setup_better_help), Icons.Outlined.Info) }
}

@Composable
private fun ConnectStep() {
    Head(Icons.Outlined.DirectionsCar, R.string.next_setup_connect_label, R.string.next_setup_connect_help)
    NextCard {
        NextRow(stringResource(R.string.next_home_usb_label), icon = Icons.Outlined.Usb, note = RowNote.Help(stringResource(R.string.next_setup_usb_help)))
        NextDivider()
        NextRow(stringResource(R.string.next_home_wireless_label), icon = Icons.Outlined.Wifi, note = RowNote.Help(stringResource(R.string.next_setup_wireless_help)))
    }
}
