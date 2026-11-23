package net.prototux.aare.ui.phone.next

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import net.prototux.aare.R

/** Each route's title, and the pages a placeholder lists so every route can be reached while they are built. */
object NextPages {
    @StringRes
    fun title(route: String): Int? = TITLES[route]

    @StringRes
    fun tabLabel(tab: NextTab): Int = when (tab) {
        NextTab.HOME -> R.string.next_tab_home_label
        NextTab.SCREEN -> R.string.next_tab_screen_label
        NextTab.CAR -> R.string.next_tab_car_label
        NextTab.APPS -> R.string.next_tab_apps_label
        NextTab.MORE -> R.string.next_tab_more_label
    }

    /** The routes one level below [route]. */
    fun children(route: String): List<String> =
        NextRoutes.ALL.filter { it.substringBeforeLast('/', "") == route }

    /** Links to another tab's page (plan §3.4 rule 2): Maps links to Map colours. */
    fun links(route: String): List<String> = when (route) {
        "car/maps" -> listOf("screen/map-colours")
        else -> emptyList()
    }

    private val TITLES: Map<String, Int> = mapOf(
        "home" to R.string.next_home_label,
        "screen" to R.string.next_tab_screen_label,
        "screen/design" to R.string.next_screen_design_label,
        "screen/looks" to R.string.next_screen_looks_label,
        "screen/colours" to R.string.next_screen_colours_label,
        "screen/colours/own" to R.string.next_screen_colours_own_label,
        "screen/wallpaper" to R.string.next_screen_wallpaper_label,
        "screen/wallpaper/edit" to R.string.next_screen_wallpaper_edit_label,
        "screen/map-colours" to R.string.next_screen_map_colours_label,
        "screen/size" to R.string.next_screen_size_label,
        "car" to R.string.next_tab_car_label,
        "car/buttons" to R.string.next_car_buttons_label,
        "car/buttons/pad-shortcuts" to R.string.next_car_pad_shortcuts_label,
        "car/buttons/timing" to R.string.next_car_timing_label,
        "car/buttons/teach" to R.string.next_car_teach_label,
        "car/maps" to R.string.next_car_maps_label,
        "car/speed" to R.string.next_car_speed_label,
        "car/messages" to R.string.next_car_messages_label,
        "car/voice" to R.string.next_car_voice_label,
        "apps" to R.string.next_tab_apps_label,
        "apps/phone-apps" to R.string.next_apps_phone_apps_label,
        "apps/phone-apps/shizuku" to R.string.next_shz_title_label,
        "apps/phone-screen" to R.string.next_apps_phone_screen_label,
        "apps/browser" to R.string.next_apps_browser_label,
        "more" to R.string.next_tab_more_label,
        "more/updates" to R.string.next_more_updates_label,
        "more/whats-new" to R.string.next_more_whats_new_label,
        "more/cars" to R.string.next_more_cars_label,
        "more/connection" to R.string.next_more_connection_label,
        "more/backup" to R.string.next_more_backup_label,
        "more/diagnostics" to R.string.next_more_diagnostics_label,
        "more/diagnostics/car-check" to R.string.next_more_car_check_label,
        "more/diagnostics/car-data" to R.string.next_more_car_data_label,
        "more/diagnostics/log" to R.string.next_more_log_label,
        "more/advanced" to R.string.next_more_advanced_label,
        "more/advanced/sound" to R.string.next_car_sound_label,
        "more/advanced/certificates" to R.string.next_more_certificates_label,
        "more/advanced/developer" to R.string.next_more_developer_label,
        "more/advanced/developer/parts" to R.string.next_dev_parts_label,
        "more/archive" to R.string.next_more_archive_label,
        "more/archive/deck" to R.string.next_more_deck_label,
        "more/archive/layouts" to R.string.next_more_layouts_label,
        "more/archive/layouts/map" to R.string.next_arch_map_behind_label,
        "more/archive/layouts/bar" to R.string.next_arch_bar_label,
        "more/archive/layouts/cards" to R.string.next_arch_cards_label,
        "more/archive/layouts/clock" to R.string.next_arch_clock_label,
        "more/archive/layouts/status" to R.string.next_arch_status_label,
        "more/about" to R.string.next_more_about_label,
    )
}

/** The page for [route]: the built ones, then a placeholder for the rest. */
@Composable
fun NextPage(route: String) {
    when (route) {
        "home" -> NextHome()
        "screen" -> NextScreenRoot()
        "screen/design" -> NextDesignPage()
        "screen/looks" -> NextLooksPage()
        "screen/colours" -> NextColoursPage()
        "screen/colours/own" -> NextOwnColoursPage()
        "screen/size" -> NextSizePage()
        "screen/wallpaper" -> NextWallpaperPage()
        "screen/wallpaper/edit" -> NextPhotoEditorPage()
        "screen/map-colours" -> NextMapColoursPage()
        "car" -> NextCarRoot()
        "car/buttons" -> NextButtonsPage()
        "car/buttons/pad-shortcuts" -> NextPadShortcutsPage()
        "car/buttons/timing" -> NextTimingPage()
        "car/buttons/teach" -> NextTeachPage()
        "car/maps" -> NextMapsPage()
        "car/speed" -> NextSpeedPage()
        "car/messages" -> NextMessagesPage()
        "car/voice" -> NextVoicePage()
        "apps" -> NextAppsRoot()
        "apps/phone-apps" -> NextPhoneAppsPage()
        "apps/phone-apps/shizuku" -> NextShizukuHelpPage()
        "apps/phone-screen" -> NextPhoneScreenPage()
        "apps/browser" -> NextBrowserPage()
        "more" -> NextMoreRoot()
        "more/cars" -> NextCarsPage()
        "more/connection" -> NextConnectionPage()
        "more/backup" -> NextBackupPage()
        "more/diagnostics" -> NextDiagnosticsPage()
        "more/diagnostics/car-check" -> NextCarCheckPage()
        "more/diagnostics/car-data" -> NextCarDataPage()
        "more/diagnostics/log" -> NextLogPage()
        "more/advanced" -> NextAdvancedPage()
        "more/advanced/sound" -> NextSoundPage()
        "more/advanced/certificates" -> NextCertificatesPage()
        "more/advanced/developer" -> NextDeveloperPage()
        "more/archive" -> NextArchivePage()
        "more/archive/deck" -> NextDeckPage()
        "more/archive/layouts" -> NextOlderLayoutsPage()
        "more/archive/layouts/map", "more/archive/layouts/bar", "more/archive/layouts/cards",
        "more/archive/layouts/clock", "more/archive/layouts/status" -> NextKeptPage(route)
        "more/about" -> NextAboutPage()
        "more/advanced/developer/parts" -> { val nav = LocalNextNavigator.current; net.prototux.aare.ui.phone.PhonePartsSample { nav.back() } }
        "more/updates" -> NextUpdatesPage()
        "more/whats-new" -> NextWhatsNewPage()
        else -> if (route.startsWith(NextRoutes.CAR_PREFIX)) NextCarPage(route.removePrefix(NextRoutes.CAR_PREFIX)) else NextPlaceholder(route)
    }
}
