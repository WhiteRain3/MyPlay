package net.prototux.aare.ui.phone.next

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.prototux.aare.BuildConfig
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.ui.AboutLinks
import net.prototux.aare.ui.AboutOss
import net.prototux.aare.ui.AboutThanks
import net.prototux.aare.ui.car.CarLayout
import net.prototux.aare.ui.car.CarLayoutSpec
import net.prototux.aare.ui.car.DashCard
import net.prototux.aare.ui.car.RailWidget
import net.prototux.aare.ui.car.chooseLayoutPreset
import net.prototux.aare.ui.car.myplay.KEY_ENGINE_LANGUAGE
import net.prototux.aare.ui.car.myplay.myplayEngineLanguage
import net.prototux.aare.ui.car.myplay.myplayEnginePreview
import net.prototux.aare.ui.car.myplayLayoutPreset
import net.prototux.aare.ui.phone.BarPage
import net.prototux.aare.ui.phone.CardsPage
import net.prototux.aare.ui.phone.ClockPage
import net.prototux.aare.ui.phone.MapBehindOptions
import net.prototux.aare.ui.phone.StatusIconsPage
import net.prototux.aare.ui.phone.rememberSettingsChanges
import net.prototux.aare.ui.car.changeLayout
import net.prototux.aare.ui.car.myplayLayoutSpec

@Composable
private fun Page(route: String, content: LazyListScope.() -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().testTag("next-page-$route"),
        state = rememberNextListState(),
        contentPadding = PaddingValues(start = NextSpace.Gutter, end = NextSpace.Gutter, bottom = 24.dp),
        content = content,
    )
}

/** The archive's top line (frame 68): a dashed box, so it reads as "kept, not current". */
@Composable
private fun KeptLine() {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(NextSpace.CardRadius))
            .border(1.dp, NextColors.Disabled, RoundedCornerShape(NextSpace.CardRadius)).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(Icons.Outlined.Archive, null, tint = NextColors.Text2)
        Text(stringResource(R.string.next_att_archived_help), style = NextType.Value, color = NextColors.Text2)
    }
    Spacer(Modifier.height(12.dp))
}

/** DESIGN's id for DECK, as stored in myplay_engine_language. */
internal const val DECK_ID = "deck"
internal const val ORBIT_ID = "orbit"

@Composable
private fun DeckCard() {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    val preview by rememberSetting("myplay_engine_preview") { myplayEnginePreview }
    val language by rememberSetting(KEY_ENGINE_LANGUAGE) { myplayEngineLanguage }
    NextCard {
        // DECK is one of MyPlay's own screens: with the older layouts on, the switch waits for that one.
        NextSwitchRow(stringResource(R.string.next_arch_deck_label), language == DECK_ID,
            { settings.myplayEngineLanguage = if (it) DECK_ID else ORBIT_ID }, Icons.Outlined.Tv,
            note = RowNote.Help(stringResource(if (preview) R.string.next_arch_deck_help else R.string.next_arch_deck_needs_help)),
            enabled = preview)
    }
}

/** More › Archive (frame 68): DECK and the older layouts, kept as they were. */
@Composable
fun NextArchivePage() {
    val nav = LocalNextNavigator.current
    Page("more/archive") {
        item { KeptLine() }
        item { DeckCard(); Spacer(Modifier.height(12.dp)) }
        item {
            NextCard {
                NextValueRow(stringResource(R.string.next_more_layouts_label), icon = Icons.Outlined.GridView,
                    note = RowNote.Help(stringResource(R.string.next_arch_layouts_help))) { nav.open("more/archive/layouts") }
            }
        }
    }
}

/** More › Archive › DECK: the same switch, for a link straight to it. */
@Composable
fun NextDeckPage() {
    Page("more/archive/deck") {
        item { KeptLine() }
        item { DeckCard() }
    }
}

/**
 * More › Archive › Older layouts (frame 69): MyPlay's own screen on or off, then every setting the
 * older layouts use, writing the same keys as today. The detailed pages (bar, cards, clock, status
 * icons, the map-behind options) are today's own pages, kept as they were (plan §6).
 */
@Composable
fun NextOlderLayoutsPage() {
    val ctx = LocalContext.current
    val nav = LocalNextNavigator.current
    val settings = remember { Settings(ctx) }
    val preview by rememberSetting("myplay_engine_preview") { myplayEnginePreview }
    val layoutId by rememberSetting("style_layout") { CarLayout.byId(carLayoutId).id }
    val preset by rememberSetting("myplay_layout_preset", "myplay_layout_spec") { myplayLayoutPreset }
    val barItems by rememberSetting("rail_widgets") { RailWidget.parse(railWidgets).size }
    val cards by rememberSetting("dashboard_cards") { DashCard.parse(dashboardCards).size }
    val weather by rememberSetting("show_weather") { showWeather }
    val own = stringResource(R.string.next_arch_own_mix_label)
    Page("more/archive/layouts") {
        item {
            NextCard {
                NextSwitchRow(stringResource(R.string.next_arch_own_screen_label), preview, { settings.myplayEnginePreview = it }, Icons.Outlined.Tv,
                    note = RowNote.Help(stringResource(R.string.next_arch_own_screen_help)), info = stringResource(R.string.phone_orbit_preview_hint))
            }
        }
        item { NextSection(stringResource(R.string.next_arch_layout_label)) }
        item {
            val rows = mutableListOf<@Composable () -> Unit>(
                {
                    NextPickerRow(stringResource(R.string.next_arch_kind_label), listOf(
                        Choice(CarLayout.MAP_BEHIND.id, stringResource(R.string.myplay_layout_map_behind)),
                        Choice(CarLayout.TASKBAR.id, stringResource(R.string.setup_layout_taskbar)),
                        Choice(CarLayout.DOCK.id, stringResource(R.string.setup_layout_dock)),
                        Choice(CarLayout.CLASSIC.id, stringResource(R.string.setup_layout_classic)),
                    ), layoutId, { settings.carLayoutId = it }, info = stringResource(R.string.phone_layout_kind_hint))
                },
            )
            if (layoutId == CarLayout.MAP_BEHIND.id) {
                rows += {
                    NextPickerRow(stringResource(R.string.next_arch_preset_label),
                        CarLayoutSpec.PRESETS.keys.map { Choice(it, stringResource(R.string.next_arch_preset_value_label, it)) } + Choice("", own),
                        preset, { if (it.isNotEmpty()) settings.chooseLayoutPreset(it) })
                }
                rows += { NextValueRow(stringResource(R.string.next_arch_map_behind_label)) { nav.open("more/archive/layouts/map") } }
            }
            rows += { NextValueRow(stringResource(R.string.next_arch_bar_label), "$barItems") { nav.open("more/archive/layouts/bar") } }
            NextGroup(rows, inset = 16.dp)
        }
        item { NextSection(stringResource(R.string.next_arch_cards_clock_label)) }
        item {
            NextGroup(listOf(
                { NextValueRow(stringResource(R.string.next_arch_cards_label), "$cards") { nav.open("more/archive/layouts/cards") } },
                { NextValueRow(stringResource(R.string.next_arch_clock_label)) { nav.open("more/archive/layouts/clock") } },
                { NextValueRow(stringResource(R.string.next_arch_status_label)) { nav.open("more/archive/layouts/status") } },
            ), inset = 16.dp)
        }
        item { NextSection(stringResource(R.string.next_arch_weather_section_label)) }
        item {
            NextCard {
                NextSwitchRow(stringResource(R.string.next_arch_weather_label), weather, { settings.showWeather = it }, Icons.Outlined.WbSunny,
                    note = RowNote.Help(stringResource(R.string.next_arch_weather_help)), info = stringResource(R.string.setup_toggle_weather_subtitle))
            }
        }
    }
}

/** One of today's older-layout pages, unchanged, inside the new app's page frame. */
@Composable
fun NextKeptPage(route: String) {
    val nav = LocalNextNavigator.current
    Column(Modifier.fillMaxSize().testTag("next-page-$route").verticalScroll(rememberScrollState())
        .padding(start = NextSpace.Gutter, end = NextSpace.Gutter, bottom = 24.dp)) {
        KeptLine()
        when (route) {
            "more/archive/layouts/map" -> {
                val ctx = LocalContext.current
                val settings = remember { Settings(ctx) }
                val version = rememberSettingsChanges()
                val spec = remember(version) { settings.myplayLayoutSpec }
                val preset = remember(version) { settings.myplayLayoutPreset }
                MapBehindOptions(spec, preset, { settings.chooseLayoutPreset(it) }) { f -> settings.changeLayout(f) }
            }
            "more/archive/layouts/bar" -> BarPage { nav.back() }
            "more/archive/layouts/cards" -> CardsPage { nav.back() }
            "more/archive/layouts/clock" -> ClockPage()
            "more/archive/layouts/status" -> StatusIconsPage()
        }
    }
}

// ---------------------------------------------------------------- About

/** More › About (frame 70): the version, What's new, the links, thanks, licences, the map data notice. */
@Composable
fun NextAboutPage() {
    val ctx = LocalContext.current
    val nav = LocalNextNavigator.current
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    fun open(url: String) = ctx.startSafely(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    Page("more/about") {
        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                NextMark(72.dp)
                Text(stringResource(R.string.next_about_name_label), style = NextType.Hero, color = NextColors.Text, modifier = Modifier.padding(top = 16.dp))
                Text(stringResource(R.string.next_about_version_label, BuildConfig.VERSION_NAME), style = NextType.Value, color = NextColors.Text2)
            }
        }
        item {
            NextGroup(listOf(
                { NextValueRow(stringResource(R.string.next_more_whats_new_label), icon = Icons.Outlined.AutoAwesome) { nav.open("more/whats-new") } },
                { NextLinkRow(stringResource(R.string.next_about_project_label), icon = Icons.Outlined.Language) { open(AboutLinks.REPOSITORY) } },
                { NextLinkRow(stringResource(R.string.next_about_report_label), icon = Icons.Outlined.ReportProblem) { open(AboutLinks.ISSUES) } },
                { NextLinkRow(stringResource(R.string.next_about_upstream_label), icon = Icons.Outlined.Info) { open(AboutLinks.UPSTREAM) } },
                { NextValueRow(stringResource(R.string.next_about_thanks_label), icon = Icons.Outlined.FavoriteBorder) { sheet = "thanks" } },
                { NextValueRow(stringResource(R.string.next_about_licences_label), icon = Icons.Outlined.Description) { sheet = "oss" } },
                { NextValueRow(stringResource(R.string.next_about_map_data_label), "OpenStreetMap", Icons.Outlined.Map) { sheet = "osm" } },
            ))
            Spacer(Modifier.height(12.dp))
        }
        item {
            NextNote(stringResource(R.string.myplay_about_fork))
        }
    }
    when (sheet) {
        "thanks" -> NextSheet(stringResource(R.string.next_about_thanks_label), { sheet = null }) {
            LazyColumn(Modifier.height(480.dp)) {
                AboutThanks.GROUPS.forEach { g ->
                    item { NextSection(stringResource(g.titleRes)) }
                    item { Handles(g.handles) { open(AboutLinks.profile(it)) } }
                }
            }
        }
        "oss" -> NextSheet(stringResource(R.string.next_about_licences_label), { sheet = null }) {
            LazyColumn(Modifier.height(480.dp)) {
                AboutOss.GROUPS.forEach { g ->
                    item { NextSection(stringResource(g.titleRes)) }
                    g.components.forEach { c ->
                        item {
                            NextRow(c.name, onClick = { open(c.url) }) { NextTag(c.licence, TagKind.MUTE) }
                        }
                    }
                }
            }
        }
        "osm" -> NextSheet(stringResource(R.string.next_about_map_data_label), { sheet = null }) {
            Text(stringResource(R.string.phone_osm_notice), style = NextType.Value, color = NextColors.Text2)
            Spacer(Modifier.height(16.dp))
            NextButton(stringResource(R.string.next_about_osm_open_label), { open("https://www.openstreetmap.org/copyright") }, Modifier.fillMaxWidth(), ButtonKind.GHOST)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Handles(handles: List<String>, onClick: (String) -> Unit) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        handles.forEach { h ->
            Text("@$h", style = NextType.Tag, color = NextColors.Int,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(NextColors.Surface2).clickable { onClick(h) }
                    .padding(horizontal = 10.dp, vertical = 8.dp))
        }
    }
}
