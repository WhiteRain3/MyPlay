package net.prototux.aare.ui.phone.next

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import net.prototux.aare.R
import net.prototux.aare.Settings
import java.text.Normalizer

/** A setting that only shows while [label]'s switch is on: found anyway, with "Turn on X first" (plan §7). */
class SearchParent(val label: Int, val on: (Settings) -> Boolean)

/** One thing search finds: a page (by its title) or a setting row on [route]. */
data class SearchEntry(val label: Int, val route: String, val synonyms: Int? = null, val parent: SearchParent? = null)

/**
 * The index (plan §7): every page from the route table, plus the settings registered here once.
 * Synonyms are string resources (comma separated), so they are translated with the rest.
 */
object NextSearchIndex {
    private val SYNONYMS: Map<String, Int> = mapOf(
        "screen/map-colours" to R.string.next_syn_map_colours_info,
        "screen/size" to R.string.next_syn_size_info,
        "screen/wallpaper" to R.string.next_syn_wallpaper_info,
        "screen/colours" to R.string.next_syn_colours_info,
        "car/buttons" to R.string.next_syn_buttons_info,
        "car/speed" to R.string.next_syn_speed_info,
        "more/advanced/sound" to R.string.next_syn_sound_info,
        "car/voice" to R.string.next_syn_voice_info,
        "car/maps" to R.string.next_syn_maps_info,
        "more/connection" to R.string.next_syn_connection_info,
        "more/backup" to R.string.next_syn_backup_info,
        "more/updates" to R.string.next_syn_updates_info,
        "more/diagnostics" to R.string.next_syn_diagnostics_info,
        "more/advanced/certificates" to R.string.next_syn_certificates_info,
        "more/advanced/developer" to R.string.next_syn_developer_info,
    )

    private val server = SearchParent(R.string.huserver_label) { it.headUnitServer }
    private val reply = SearchParent(R.string.next_msg_reply_label) { it.autoReply }

    /** Settings rows, by the label their row shows. */
    val SETTINGS: List<SearchEntry> = listOf(
        SearchEntry(R.string.next_adv_resolution_label, "more/advanced", R.string.next_syn_resolution_info),
        SearchEntry(R.string.next_adv_60fps_label, "more/advanced"),
        SearchEntry(R.string.next_adv_h264_label, "more/advanced"),
        SearchEntry(R.string.next_adv_cbr_label, "more/advanced"),
        SearchEntry(R.string.next_cert_auto_label, "more/advanced/certificates"),
        SearchEntry(R.string.huserver_label, "more/advanced/developer", R.string.next_syn_developer_info),
        SearchEntry(R.string.next_dev_network_label, "more/advanced/developer", parent = server),
        SearchEntry(R.string.next_dev_once_label, "more/advanced/developer", parent = server),
        SearchEntry(R.string.next_dev_mirror_label, "more/advanced/developer", parent = server),
        SearchEntry(R.string.next_dev_localhost_label, "more/advanced/developer", parent = server),
        SearchEntry(R.string.next_dev_map_areas_label, "more/advanced/developer"),
        SearchEntry(R.string.next_dev_nav_text_label, "more/advanced/developer"),
        SearchEntry(R.string.next_dev_parts_label, "more/advanced/developer"),
        SearchEntry(R.string.setup_app_entry_phone, "apps"),
        SearchEntry(R.string.next_apps_phone_screen_label, "apps/phone-screen"),
        SearchEntry(R.string.next_apps_browser_label, "apps/browser"),
        SearchEntry(R.string.next_apps_youtube_web_label, "apps"),
        SearchEntry(R.string.next_apps_no_sleep_label, "apps/phone-apps"),
        SearchEntry(R.string.next_arch_deck_label, "more/archive"),
        SearchEntry(R.string.next_arch_own_screen_label, "more/archive/layouts"),
        SearchEntry(R.string.next_arch_kind_label, "more/archive/layouts"),
        SearchEntry(R.string.next_arch_preset_label, "more/archive/layouts"),
        SearchEntry(R.string.next_arch_weather_label, "more/archive/layouts"),
        SearchEntry(R.string.next_car_line_label, "car/buttons"),
        SearchEntry(R.string.next_car_info_home_label, "car/buttons/pad-shortcuts"),
        SearchEntry(R.string.next_maps_follow_label, "car/maps"),
        SearchEntry(R.string.next_maps_car_gps_label, "car/maps"),
        SearchEntry(R.string.next_maps_parked_label, "car/maps"),
        SearchEntry(R.string.next_speed_mobile_label, "car/speed"),
        SearchEntry(R.string.next_speed_fill_label, "car/speed"),
        SearchEntry(R.string.next_speed_warnings_label, "car/speed"),
        SearchEntry(R.string.next_speed_pulse_label, "car/speed"),
        SearchEntry(R.string.next_sound_switch_label, "car", R.string.next_syn_sound_info),
        SearchEntry(R.string.next_sound_resume_label, "more/advanced/sound"),
        SearchEntry(R.string.next_msg_banners_label, "car/messages"),
        SearchEntry(R.string.next_msg_groups_label, "car/messages"),
        SearchEntry(R.string.next_msg_aloud_label, "car/messages"),
        SearchEntry(R.string.next_msg_dnd_label, "car/messages"),
        SearchEntry(R.string.next_msg_reply_label, "car/messages"),
        SearchEntry(R.string.next_msg_reply_text_label, "car/messages", parent = reply),
        SearchEntry(R.string.next_msg_speaker_label, "car/messages"),
        SearchEntry(R.string.next_diag_show_in_car_label, "more/diagnostics/car-data"),
        SearchEntry(R.string.next_diag_verbose_label, "more/diagnostics/log"),
        SearchEntry(R.string.next_diag_car_input_label, "more/diagnostics/log"),
        SearchEntry(R.string.next_more_wireless_label, "more/connection"),
        SearchEntry(R.string.next_more_bt_label, "more/connection"),
        SearchEntry(R.string.next_upd_automatic_label, "more/updates"),
        SearchEntry(R.string.next_col_theme_label, "screen/colours"),
        SearchEntry(R.string.next_col_accent_label, "screen/colours"),
        SearchEntry(R.string.next_scr_density_label, "screen/size"),
    )

    /** Pages first (by their title), then settings. */
    fun entries(): List<SearchEntry> =
        NextRoutes.ALL.mapNotNull { r -> NextPages.title(r)?.let { SearchEntry(it, r, SYNONYMS[r]) } } + SETTINGS
}

/** Lower case, no accents: "Écran" finds "ecran". */
internal fun searchFold(s: String): String =
    Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

/**
 * The entries whose label or synonyms hold every word of [query], best first: a label that starts
 * with the query, then a label that holds it, then a synonym. The same label on the same page once.
 */
fun searchMatches(query: String, entries: List<SearchEntry>, text: (Int) -> String): List<SearchEntry> {
    val words = searchFold(query).split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return emptyList()
    val q = searchFold(query.trim())
    return entries.mapNotNull { e ->
        val label = searchFold(text(e.label))
        val syn = e.synonyms?.let { searchFold(text(it)) }.orEmpty()
        val all = "$label $syn"
        if (!words.all { it in all }) null
        else e to when { label.startsWith(q) -> 0; words.all { it in label } -> 1; else -> 2 }
    }.sortedBy { it.second }.map { it.first }.distinctBy { it.label to it.route }
}

/** "Car › Messages and calls": the tab, then the page when it isn't the tab's root. */
@Composable
private fun pathOf(route: String): String {
    val tab = NextRoutes.tabOf(route) ?: return route
    val tabName = stringResource(NextPages.tabLabel(tab))
    return if (route == tab.root) tabName else "$tabName › ${NextPages.title(route)?.let { stringResource(it) } ?: route}"
}

/**
 * Search (frame 71): the field in the top bar's place, the results below, the tab bar left as it is.
 * A result is a cross-tab link ([onOpen]); Back or the arrow closes search.
 */
@Composable
fun NextSearch(onOpen: (String) -> Unit, onClose: () -> Unit, initial: String = "") {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    var query by rememberSaveable { mutableStateOf(initial) }
    BackHandler(onBack = onClose)
    val entries = remember { NextSearchIndex.entries() }
    val results = remember(query) { searchMatches(query, entries) { ctx.getString(it) } }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (initial.isEmpty()) runCatching { focus.requestFocus() } }
    Column(Modifier.fillMaxSize().testTag("next-search")) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().height(72.dp).padding(start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val back = stringResource(R.string.next_back_label)
            Box(Modifier.size(NextSpace.Target).clip(CircleShape).clickable(role = Role.Button, onClick = onClose)
                .semantics { contentDescription = back }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = NextColors.Text2)
            }
            val hint = stringResource(R.string.next_search_hint_label)
            BasicTextField(query, { query = it.take(60) },
                Modifier.weight(1f).height(52.dp).focusRequester(focus).testTag("next-search-field")
                    .border(1.5.dp, NextColors.Int, RoundedCornerShape(26.dp)).padding(horizontal = 18.dp)
                    .semantics { contentDescription = hint },
                textStyle = NextType.Label.copy(color = NextColors.Text), singleLine = true, cursorBrush = SolidColor(NextColors.Int),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) Text(hint, style = NextType.Label, color = NextColors.Text3)
                        inner()
                    }
                })
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = NextSpace.Gutter, end = NextSpace.Gutter, bottom = 24.dp)) {
            if (query.isNotBlank()) item { NextSection(stringResource(R.string.next_search_results_label), first = true) }
            if (query.isNotBlank() && results.isEmpty()) item { NextEmpty(stringResource(R.string.next_search_none_help)) }
            if (results.isNotEmpty()) item {
                NextCard {
                    results.forEachIndexed { i, e ->
                        if (i > 0) NextDivider()
                        val off = e.parent?.let { !it.on(settings) } == true
                        NextValueRow(stringResource(e.label), icon = null,
                            note = RowNote.Help(pathOf(e.route)),
                            tag = if (off) ({ NextTag(stringResource(R.string.next_search_turn_on_label, stringResource(e.parent!!.label)), TagKind.MUTE, Icons.Outlined.Info) }) else null,
                        ) { onOpen(e.route) }
                    }
                }
            }
        }
    }
}
