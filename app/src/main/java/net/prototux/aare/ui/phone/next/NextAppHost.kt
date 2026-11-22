package net.prototux.aare.ui.phone.next

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import net.prototux.aare.UpdateChecker
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
    val update by UpdateChecker.available.collectAsState()
    NextApp(
        deepLink = deepLink, onDeepLinkUsed = onDeepLinkUsed, onOldApp = onOldApp,
        // An update waiting: the dot on More (plan §4).
        badges = if (update != null) setOf(NextTab.MORE) else emptySet(),
    ) { NextPage(it, onOldApp) }
}
