package net.prototux.aare.ui.phone.next

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.prototux.aare.BuildConfig
import net.prototux.aare.MyPlayUpdateInstall
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.UpdateChecker
import net.prototux.aare.ui.phone.KEY_UPDATE_ASKED
import net.prototux.aare.ui.phone.NoteBlock
import net.prototux.aare.ui.phone.ReleaseNotes
import net.prototux.aare.ui.phone.myplayUpdateAsked

/** What the Updates page's buttons do. */
interface UpdateActions {
    fun install(version: String)
    fun checkNow()
    fun setAutomatic(on: Boolean)
    fun allowInstalls()
}

/** More › Updates (plan §5): the one home for updates. */
@Composable
fun NextUpdatesPage() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val install by MyPlayUpdateInstall.state.collectAsState()
    val daily by UpdateChecker.available.collectAsState()
    val check by NextUpdateCheck.result.collectAsState()
    val installing by NextUpdateCheck.installing.collectAsState()
    val automatic by rememberSetting("check_updates") { checkUpdates }
    val lastDaily by rememberSetting("update_last_check_ms") { updateLastCheckMs }
    val found = (check as? NextUpdateCheck.Result.Done)?.found ?: daily
    val hero = updateHeroOf(install, found, installing, check == NextUpdateCheck.Result.Checking)
    val actions = remember {
        object : UpdateActions {
            override fun install(version: String) {
                NextUpdateCheck.installing.value = version
                MyPlayUpdateInstall.reset()
                scope.launch(Dispatchers.IO) { MyPlayUpdateInstall.downloadAndInstall(ctx, version) }
            }
            override fun checkNow() { scope.launch(Dispatchers.IO) { NextUpdateCheck.checkNow() } }
            override fun setAutomatic(on: Boolean) {
                Settings(ctx).checkUpdates = on
                UpdateChecker.refresh(ctx)
            }
            override fun allowInstalls() {
                runCatching {
                    ctx.startActivity(Intent(AndroidSettings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}")))
                }
            }
        }
    }
    val lastCheck = maxOf(lastDaily, (check as? NextUpdateCheck.Result.Done)?.at ?: 0L)
    NextUpdatesContent(hero, check, lastCheck, automatic, actions)
}

@Composable
fun NextUpdatesContent(hero: UpdateHero, check: NextUpdateCheck.Result?, lastCheck: Long, automatic: Boolean, actions: UpdateActions) {
    val nav = LocalNextNavigator.current
    val blue = NextTab.MORE.world
    LazyColumn(
        Modifier.fillMaxSize().testTag("next-page-more/updates"),
        state = rememberNextListState(),
        contentPadding = PaddingValues(start = NextSpace.Gutter, end = NextSpace.Gutter, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                when (hero) {
                    is UpdateHero.Ready -> {
                        NextHeroRing(RingStyle.FULL, blue, Icons.Rounded.Download)
                        Title(stringResource(R.string.next_upd_ready_label, hero.version), stringResource(R.string.next_att_update_help, BuildConfig.VERSION_NAME))
                        Spacer(Modifier.height(16.dp))
                        NextButton(stringResource(R.string.next_upd_install_label, hero.version), { actions.install(hero.version) },
                            Modifier.fillMaxWidth(), icon = Icons.Rounded.Download)
                    }
                    is UpdateHero.Downloading -> {
                        NextHeroRing(RingStyle.PROGRESS, blue, null, hero.percent?.let { "$it%" }, (hero.percent ?: 0) / 100f)
                        Title(stringResource(R.string.next_upd_downloading_label, hero.version), stringResource(R.string.next_upd_confirm_help))
                        Spacer(Modifier.height(16.dp))
                        NextButton(stringResource(R.string.next_upd_installing_label, hero.version), {}, Modifier.fillMaxWidth(), enabled = false)
                    }
                    is UpdateHero.Installing -> {
                        NextHeroRing(RingStyle.SWEEP, blue, Icons.Rounded.Download)
                        Title(stringResource(R.string.next_upd_installing_label, hero.version), stringResource(R.string.next_upd_confirm_help))
                        Spacer(Modifier.height(16.dp))
                        NextButton(stringResource(R.string.next_upd_installing_label, hero.version), {}, Modifier.fillMaxWidth(), enabled = false)
                    }
                    is UpdateHero.Failed, is UpdateHero.NeedsPermission -> {
                        val version = if (hero is UpdateHero.Failed) hero.version else (hero as UpdateHero.NeedsPermission).version
                        NextHeroRing(RingStyle.ERROR, NextColors.Warn, Icons.Outlined.ErrorOutline)
                        Title(stringResource(R.string.next_upd_failed_label, version),
                            if (hero is UpdateHero.Failed) failureWords(hero.reason) else stringResource(R.string.next_upd_needs_permission_help))
                        Spacer(Modifier.height(16.dp))
                        NextButton(stringResource(R.string.next_home_try_again_label), { actions.install(version) }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Refresh)
                    }
                    UpdateHero.Checking -> {
                        NextHeroRing(RingStyle.SWEEP, blue, Icons.Rounded.Refresh)
                        Title(stringResource(R.string.next_upd_checking_label), null)
                    }
                    UpdateHero.UpToDate -> {
                        NextHeroRing(RingStyle.FULL, blue, Icons.Rounded.Check)
                        Title(stringResource(R.string.next_upd_up_to_date_label), stringResource(R.string.next_upd_version_help, BuildConfig.VERSION_NAME))
                    }
                }
            }
        }
        if (hero is UpdateHero.NeedsPermission) item {
            NextCard {
                NextRow(
                    stringResource(R.string.next_upd_allow_label), icon = Icons.Outlined.Shield,
                    note = RowNote.Help(stringResource(R.string.next_upd_allow_help)),
                ) { NextButton(stringResource(R.string.next_upd_allow_button_label), actions::allowInstalls, kind = ButtonKind.GHOST, small = true) }
            }
        }
        item {
            NextGroup(listOf(
                { NextValueRow(stringResource(R.string.next_att_whats_new_label), icon = Icons.Outlined.AutoAwesome) { nav.open("more/whats-new") } },
                {
                    NextRow(stringResource(R.string.next_upd_check_now_label), icon = Icons.Rounded.Refresh, onClick = actions::checkNow,
                        role = androidx.compose.ui.semantics.Role.Button) { NextValue(checkWords(check, lastCheck)) }
                },
                {
                    NextSwitchRow(stringResource(R.string.next_upd_automatic_label), automatic, actions::setAutomatic, Icons.Outlined.NotificationsNone,
                        note = RowNote.Help(stringResource(R.string.next_upd_automatic_help)))
                },
            ))
        }
    }
}

@Composable
private fun Title(title: String, sub: String?) {
    Text(title, style = NextType.Hero, color = NextColors.Text, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 14.dp))
    if (sub != null) Text(sub, style = NextType.Value.copy(fontSize = NextType.Label.fontSize), color = NextColors.Text2,
        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun checkWords(check: NextUpdateCheck.Result?, lastCheck: Long): String = when {
    check == NextUpdateCheck.Result.Checking -> stringResource(R.string.next_upd_checking_label)
    check == NextUpdateCheck.Result.Offline -> stringResource(R.string.next_upd_offline_label)
    check is NextUpdateCheck.Result.Done && System.currentTimeMillis() - check.at < 60_000 -> stringResource(R.string.next_upd_checked_now_label)
    lastCheck > 0 -> stringResource(R.string.next_upd_checked_label, DateFormat.getTimeInstance(DateFormat.SHORT).format(lastCheck))
    else -> ""
}

@Composable
private fun failureWords(f: MyPlayUpdateInstall.Failure): String = stringResource(when (f) {
    MyPlayUpdateInstall.Failure.NETWORK -> R.string.next_upd_fail_network_help
    MyPlayUpdateInstall.Failure.NO_APK -> R.string.next_upd_fail_no_apk_help
    MyPlayUpdateInstall.Failure.CHECKSUM -> R.string.next_upd_fail_checksum_help
    MyPlayUpdateInstall.Failure.NOT_MYPLAY -> R.string.next_upd_fail_not_myplay_help
    MyPlayUpdateInstall.Failure.WRONG_VERSION -> R.string.next_upd_fail_version_help
    MyPlayUpdateInstall.Failure.WRONG_SIGNER -> R.string.next_upd_fail_signer_help
    MyPlayUpdateInstall.Failure.INSTALL -> R.string.next_upd_fail_install_help
})

// ---------------------------------------------------------------- What's new

/** More › Updates › What's new (design frame 48): each version's notes, newest first, from the app itself. */
@Composable
fun NextWhatsNewPage() {
    val ctx = LocalContext.current
    val notes = remember {
        val versions = runCatching { ctx.assets.list("release_notes")?.toList() }.getOrNull().orEmpty()
            .filter { it.endsWith(".md") }.map { it.removeSuffix(".md") }
        newestFirst(versions).take(WHATS_NEW_VERSIONS).mapNotNull { v -> ReleaseNotes.read(ctx, v)?.let { v to ReleaseNotes.parse(it) } }
    }
    val asked by rememberSetting(KEY_UPDATE_ASKED, "check_updates") { myplayUpdateAsked || checkUpdates }
    NextWhatsNewContent(notes, askUpdates = !asked) { yes ->
        val s = Settings(ctx)
        s.checkUpdates = yes
        s.myplayUpdateAsked = true
        UpdateChecker.refresh(ctx)
    }
}

/** How many versions What's new lists. */
private const val WHATS_NEW_VERSIONS = 8

@Composable
fun NextWhatsNewContent(notes: List<Pair<String, List<NoteBlock>>>, askUpdates: Boolean, onAnswer: (Boolean) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().testTag("next-page-more/whats-new"),
        state = rememberNextListState(),
        contentPadding = PaddingValues(start = NextSpace.Gutter, end = NextSpace.Gutter, bottom = 24.dp),
    ) {
        notes.forEachIndexed { i, (version, blocks) ->
            item { NextSection(version, first = i == 0) }
            item { NotesCard(blocks, newest = i == 0) }
            // The one question about update checks, after the newest notes (plan §5).
            if (i == 0 && askUpdates) item {
                Spacer(Modifier.height(12.dp))
                AskUpdates(onAnswer)
            }
        }
    }
}

@Composable
private fun NotesCard(blocks: List<NoteBlock>, newest: Boolean) {
    val icon = if (newest) Icons.Outlined.AutoAwesome else Icons.Rounded.Check
    NextCard {
        blocks.forEachIndexed { i, b ->
            if (i > 0) NextDivider(if (b is NoteBlock.Bullet && b.level == 0) 52.dp else 16.dp)
            when (b) {
                is NoteBlock.Heading -> Text(ReleaseNotes.inline(b.text), style = NextType.Label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight(620)),
                    color = NextColors.Text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp))
                is NoteBlock.Paragraph -> Text(ReleaseNotes.inline(b.text), style = NextType.Help, color = NextColors.Text3,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                is NoteBlock.Bullet -> Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp + (b.level * 20).dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(Modifier.padding(top = 2.dp)) { Icon(icon, null, tint = NextColors.Text2, modifier = Modifier.size(18.dp)) }
                    Text(ReleaseNotes.inline(b.text), style = NextType.Value, color = NextColors.Text)
                }
            }
        }
    }
}

@Composable
private fun AskUpdates(onAnswer: (Boolean) -> Unit) {
    NextCard(highlight = true) {
        NextRow(
            stringResource(R.string.next_upd_ask_label), icon = Icons.Outlined.NotificationsNone,
            note = RowNote.Help(stringResource(R.string.next_upd_automatic_help)),
        )
        Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NextButton(stringResource(R.string.next_upd_no_label), { onAnswer(false) }, Modifier.weight(1f), ButtonKind.GHOST, small = true)
            NextButton(stringResource(R.string.next_upd_yes_label), { onAnswer(true) }, Modifier.weight(1f), small = true)
        }
    }
}
