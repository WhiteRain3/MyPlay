package net.prototux.aare.ui.phone

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.prototux.aare.MyPlayUpdateInstall
import net.prototux.aare.MyPlayUpdateInstall.Failure
import net.prototux.aare.MyPlayUpdateInstall.State
import net.prototux.aare.R

/** "Download and install" for [version], with what is happening under it (plan I2). Nothing happens without a tap. */
@Composable
fun UpdateInstallButton(version: String) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by MyPlayUpdateInstall.state.collectAsState()
    val busy = state is State.Downloading || state == State.Checking || state == State.Installing
    Column {
        PillButton(stringResource(if (busy) R.string.phone_update_busy else R.string.phone_update_install, version)) {
            if (busy) return@PillButton
            MyPlayUpdateInstall.reset()
            scope.launch { withContext(Dispatchers.IO) { MyPlayUpdateInstall.downloadAndInstall(ctx, version) } }
        }
        val line = when (val s = state) {
            State.Idle -> stringResource(R.string.phone_update_install_hint)
            is State.Downloading -> s.percent?.let { stringResource(R.string.phone_update_downloading_pct, it) } ?: stringResource(R.string.phone_update_downloading)
            State.Checking -> stringResource(R.string.phone_update_checking)
            State.Installing -> stringResource(R.string.phone_update_installing)
            State.NeedsPermission -> stringResource(R.string.phone_update_permission)
            is State.Failed -> stringResource(when (s.reason) {
                Failure.NETWORK -> R.string.phone_update_fail_network
                Failure.NO_APK -> R.string.phone_update_fail_no_apk
                Failure.CHECKSUM -> R.string.phone_update_fail_checksum
                Failure.NOT_MYPLAY -> R.string.phone_update_fail_not_myplay
                Failure.WRONG_VERSION -> R.string.phone_update_fail_version
                Failure.WRONG_SIGNER -> R.string.phone_update_fail_signer
                Failure.INSTALL -> R.string.phone_update_fail_install
            })
        }
        Spacer(Modifier.height(4.dp))
        Text(line, color = if (state is State.Failed) PhoneColors.Warning else PhoneColors.Muted, fontSize = 13.sp)
    }
}
