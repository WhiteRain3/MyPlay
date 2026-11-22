package net.prototux.aare.ui.phone.next

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import net.prototux.aare.BuildConfig
import net.prototux.aare.Settings
import net.prototux.aare.UpdateChecker
import net.prototux.aare.ui.phone.ReleaseNotes
import net.prototux.aare.ui.phone.myplayWhatsNewSeen
import net.prototux.aare.cert.CertificateUpdates
import net.prototux.aare.service.ProjectionService
import net.prototux.aare.ui.phone.SpeedLimitDownload

/**
 * The new app as MainActivity shows it: [NextApp] with the real pages, and what opening the app has
 * always done (start the idle service, look for the car, updates, certificates and the speed file).
 */
@Composable
fun NextAppHost(deepLink: String?, onDeepLinkUsed: () -> Unit, onOldApp: () -> Unit) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        ProjectionService.startIdleIfEnabled(ctx)
        ProjectionService.probeUsb(ctx)
        ProjectionService.probeWireless(ctx)
        UpdateChecker.refresh(ctx)
        CertificateUpdates.refresh(ctx)
        SpeedLimitDownload.checkIfDue(ctx)
    }
    // What's new once after an update, never on a fresh install (today's rule, ReleaseNotes.showOnOpen).
    var whatsNew by rememberSaveable { mutableStateOf(whatsNewOnOpen(ctx)) }
    val update by UpdateChecker.available.collectAsState()
    NextApp(
        deepLink = deepLink ?: "more/whats-new".takeIf { whatsNew },
        onDeepLinkUsed = { if (deepLink != null) onDeepLinkUsed() else whatsNew = false },
        onOldApp = onOldApp,
        // An update waiting: the dot on More (plan §4).
        badges = if (update != null) setOf(NextTab.MORE) else emptySet(),
    ) { NextPage(it, onOldApp) }
}

/** True once after an update to a version with notes; marks them seen. A fresh install only marks them. */
private fun whatsNewOnOpen(ctx: Context): Boolean {
    val settings = Settings(ctx)
    val version = BuildConfig.VERSION_NAME
    val pkg = runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0) }.getOrNull()
    val fresh = pkg != null && pkg.firstInstallTime == pkg.lastUpdateTime
    val show = ReleaseNotes.showOnOpen(version, settings.myplayWhatsNewSeen, ReleaseNotes.read(ctx, version) != null, fresh)
    if (show || fresh) settings.myplayWhatsNewSeen = version
    return show
}
