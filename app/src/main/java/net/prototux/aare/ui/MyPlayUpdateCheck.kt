package net.prototux.aare.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.prototux.aare.BuildConfig
import net.prototux.aare.R
import net.prototux.aare.UpdateChecker

/** What one tap on "Check now" found. */
private sealed interface CheckResult {
    data object Checking : CheckResult
    data object UpToDate : CheckResult
    data object Failed : CheckResult
    data class Found(val update: UpdateChecker.Update) : CheckResult
}

/**
 * MyPlay's "Check now" button, under the update switch in Connection settings. One tap makes one
 * request to GitHub for this fork's latest release ([UpdateChecker.fetchLatest]), whether the
 * switch is on or off, and shows the answer here. Nothing is asked without a tap.
 */
@Composable
internal fun MyPlayCheckNow() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<CheckResult?>(null) }
    Column(Modifier.fillMaxWidth()) {
        OutlinedButton(
            enabled = result != CheckResult.Checking,
            onClick = {
                result = CheckResult.Checking
                scope.launch {
                    val latest = withContext(Dispatchers.IO) { UpdateChecker.fetchLatest() }
                    result = when {
                        latest == null -> CheckResult.Failed
                        UpdateChecker.isNewer(latest.version, BuildConfig.VERSION_NAME) -> CheckResult.Found(latest)
                        else -> CheckResult.UpToDate
                    }
                }
            },
        ) { Text(stringResource(R.string.myplay_update_check_now)) }
        Spacer(Modifier.height(4.dp))
        when (val r = result) {
            null -> Text(stringResource(R.string.myplay_update_check_now_hint), style = MaterialTheme.typography.bodySmall)
            CheckResult.Checking -> Text(stringResource(R.string.myplay_update_checking), style = MaterialTheme.typography.bodySmall)
            CheckResult.UpToDate -> Text(stringResource(R.string.myplay_update_none, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodySmall)
            CheckResult.Failed -> Text(stringResource(R.string.myplay_update_failed), style = MaterialTheme.typography.bodySmall)
            is CheckResult.Found -> Text(
                stringResource(R.string.myplay_update_found, r.update.version, BuildConfig.VERSION_NAME),
                Modifier.clickable { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(r.update.url))) } },
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
