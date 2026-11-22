package net.prototux.aare.ui.phone.next

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.ui.car.CarUiState
import net.prototux.aare.ui.car.WALLPAPER_NAMES
import net.prototux.aare.ui.car.Wallpaper
import net.prototux.aare.ui.car.myplay.KEY_ENGINE_LANGUAGE
import net.prototux.aare.ui.car.myplay.KEY_ORBIT_SHADE
import net.prototux.aare.ui.car.myplay.KEY_PANEL_TIMEOUT
import net.prototux.aare.ui.car.myplay.ORBIT_SHADE_NONE
import net.prototux.aare.ui.car.myplay.ORBIT_SHADE_SOFT
import net.prototux.aare.ui.car.myplay.PanelTimeout
import net.prototux.aare.ui.car.myplay.myplayEngineLanguage
import net.prototux.aare.ui.car.myplay.myplayEnginePreview
import net.prototux.aare.ui.theme.myplayMapFilter
import net.prototux.aare.ui.theme.myplayLookColours
import net.prototux.aare.ui.car.myplay.myplayOrbitShade
import net.prototux.aare.ui.car.myplay.myplayPanelTimeout
import net.prototux.aare.ui.phone.CarPicture
import net.prototux.aare.ui.phone.CarPictureSize
import net.prototux.aare.ui.phone.ColourPickerDialog
import net.prototux.aare.ui.phone.FilterSampleMap
import net.prototux.aare.ui.phone.WallpaperEdit
import net.prototux.aare.ui.phone.WallpaperFiles
import net.prototux.aare.ui.phone.dragged
import net.prototux.aare.ui.phone.effectiveDensity
import net.prototux.aare.ui.phone.filteredSampleMap
import net.prototux.aare.ui.phone.myplayLooks
import net.prototux.aare.ui.phone.myplayWallpaperEdit
import net.prototux.aare.ui.saveWallpaper
import net.prototux.aare.ui.theme.CarAccents
import net.prototux.aare.ui.theme.CarControlSize
import net.prototux.aare.ui.theme.CarTextSize
import net.prototux.aare.ui.theme.CarThemes
import net.prototux.aare.ui.theme.MapColourFilter

private val violet get() = NextTab.SCREEN.world

/** The list every Screen page sits in: the gutter, and room at the bottom. */
@Composable
private fun ScreenPage(route: String, content: LazyListScope.() -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().testTag("next-page-$route"),
        state = rememberNextListState(),
        contentPadding = PaddingValues(start = NextSpace.Gutter, end = NextSpace.Gutter, bottom = 24.dp),
        content = content,
    )
}

// ---------------------------------------------------------------- the root

/** Screen (design frame 08): the large picture, its parts tappable, and every page with its value. */
@Composable
fun NextScreenRoot() {
    val ctx = LocalContext.current
    val nav = LocalNextNavigator.current
    val preview by rememberSetting("myplay_engine_preview", "myplay_orbit_default_done") { myplayEnginePreview }
    val language by rememberSetting(KEY_ENGINE_LANGUAGE) { myplayEngineLanguage }
    val design = designInUse(preview, language)
    // Looks, colours and wallpaper depend on many keys: any change re-reads them.
    val look by rememberSetting { lookInUse((prefs.all as Map<String, *>?).orEmpty(), myplayLooks) }
    val theme by rememberSetting("style_theme", "car_style") { carThemeId }
    val ownColours by rememberSetting("myplay_look_colours") { myplayLookColours.isNotBlank() }
    val wallpaper by rememberSetting("wallpaper") { wallpaper }
    val mapKind by rememberSetting("myplay_map_filter") { mapColoursKind(myplayMapFilter) }
    val text by rememberSetting("car_text_size") { CarTextSize.byId(carTextSize) }
    val density by rememberSetting("car_density") { effectiveDensity(carDensity) }
    val themeName = CarThemes.byId(theme).let { if (it.nameRes != 0) stringResource(it.nameRes) else it.name }
    ScreenPage("screen") {
        item {
            Column {
                CarPicture(
                    stringResource(R.string.next_home_car_screen_label), Modifier.fillMaxWidth(), onPart = null,
                    onCarPart = { part -> routeForPart(part, design)?.let { nav.open(it) } },
                )
                NextNote(stringResource(R.string.next_scr_tap_part_help), Icons.Outlined.TouchApp)
                Spacer(Modifier.height(12.dp))
            }
        }
        item {
            NextGroup(listOf(
                { NextValueRow(stringResource(R.string.next_screen_design_label), designWord(design), Icons.Outlined.Layers) { nav.open("screen/design") } },
                { NextValueRow(stringResource(R.string.next_screen_looks_label), look ?: stringResource(R.string.next_scr_not_saved_label), Icons.Outlined.AutoAwesome) { nav.open("screen/looks") } },
                {
                    NextValueRow(
                        stringResource(R.string.next_screen_colours_label),
                        if (ownColours) stringResource(R.string.next_scr_own_colours_label, themeName) else themeName,
                        Icons.Outlined.Palette, changed = ownColours,
                    ) { nav.open("screen/colours") }
                },
                { NextValueRow(stringResource(R.string.next_screen_wallpaper_label), wallpaperName(wallpaper), Icons.Outlined.Wallpaper, changed = wallpaper != "none") { nav.open("screen/wallpaper") } },
                { NextValueRow(stringResource(R.string.next_screen_map_colours_label), mapPresetName(mapKind), Icons.Outlined.Map, changed = mapKind != "none") { nav.open("screen/map-colours") } },
                {
                    NextValueRow(
                        stringResource(R.string.next_screen_size_label),
                        stringResource(R.string.next_scr_size_values_label, stringResource(text.labelRes), densityWord(density)),
                        Icons.Outlined.TextFields, changed = text != CarTextSize.DEFAULT || density != 0,
                    ) { nav.open("screen/size") }
                },
            ))
        }
    }
}

@Composable
private fun designWord(d: DesignInUse) = stringResource(when (d) {
    DesignInUse.ORBIT -> R.string.next_design_orbit_label
    DesignInUse.DECK -> R.string.next_design_deck_label
    DesignInUse.OLDER -> R.string.next_design_older_label
})

@Composable
private fun wallpaperName(name: String): String = when (name) {
    "none" -> stringResource(R.string.next_scr_none_label)
    Settings.WALLPAPER_CUSTOM -> stringResource(R.string.next_scr_photo_label)
    else -> name.replaceFirstChar(Char::uppercase)
}

@Composable
private fun densityWord(dpi: Int) = if (dpi == 0) stringResource(R.string.next_scr_auto_label) else stringResource(R.string.next_scr_dpi_label, dpi)

@Composable
private fun mapPresetName(kind: String): String = stringResource(when (kind) {
    "none" -> R.string.next_scr_none_label
    "muted" -> R.string.phone_map_preset_muted
    "cool" -> R.string.phone_map_preset_cool
    "warm" -> R.string.phone_map_preset_warm
    "dark_night" -> R.string.phone_map_preset_dark_night
    "grey" -> R.string.phone_map_preset_grey
    else -> R.string.next_scr_custom_label
})

// ---------------------------------------------------------------- Design

/** Screen › Design (frame 10): ORBIT in use, the RACE slot, ORBIT's own settings. */
@Composable
fun NextDesignPage() {
    val ctx = LocalContext.current
    val nav = LocalNextNavigator.current
    val preview by rememberSetting("myplay_engine_preview", "myplay_orbit_default_done") { myplayEnginePreview }
    val language by rememberSetting(KEY_ENGINE_LANGUAGE) { myplayEngineLanguage }
    val shade by rememberSetting(KEY_ORBIT_SHADE) { myplayOrbitShade }
    val panels by rememberSetting(KEY_PANEL_TIMEOUT) { myplayPanelTimeout }
    val orbit = designInUse(preview, language) == DesignInUse.ORBIT
    ScreenPage("screen/design") {
        item {
            DesignCard(
                stringResource(R.string.next_design_orbit_label), stringResource(R.string.next_scr_orbit_help),
                selected = orbit, tag = if (orbit) stringResource(R.string.next_scr_in_use_label) else null, enabled = true,
                picture = { CarPicture(stringResource(R.string.next_home_car_screen_label), Modifier.fillMaxWidth(), onPart = null) },
            ) {
                // Back to ORBIT from DECK or an older layout: both settings, as Archive's switches would.
                val s = Settings(ctx)
                s.myplayEnginePreview = true
                s.myplayEngineLanguage = "orbit"
            }
            Spacer(Modifier.height(12.dp))
        }
        item {
            DesignCard(
                stringResource(R.string.next_scr_race_label), stringResource(R.string.next_scr_race_help),
                selected = false, tag = stringResource(R.string.next_scr_coming_label), enabled = false, picture = null,
            ) {}
            Spacer(Modifier.height(12.dp))
        }
        item {
            NextGroup(listOf(
                {
                    NextPickerRow(
                        stringResource(R.string.next_scr_shade_label),
                        listOf(Choice(ORBIT_SHADE_SOFT, stringResource(R.string.myplay_setting_orbit_shade_soft)),
                            Choice(ORBIT_SHADE_NONE, stringResource(R.string.myplay_setting_orbit_shade_none))),
                        shade, { Settings(ctx).myplayOrbitShade = it }, default = ORBIT_SHADE_SOFT, icon = Icons.Outlined.Layers,
                    )
                },
                {
                    NextPickerRow(
                        stringResource(R.string.next_scr_panels_label),
                        PanelTimeout.CHOICES.map { s ->
                            Choice(s, if (s == PanelTimeout.NEVER) stringResource(R.string.next_scr_never_label) else stringResource(R.string.next_scr_seconds_label, s))
                        },
                        panels, { Settings(ctx).myplayPanelTimeout = it }, default = PanelTimeout.DEFAULT, icon = Icons.Outlined.Timer,
                        tag = { AlsoOnCarTag() },
                    )
                },
            ))
        }
        item {
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.Button) { nav.open("more/archive") }.padding(vertical = 4.dp),
            ) { NextNote(stringResource(R.string.next_scr_archive_help), Icons.Outlined.Archive) }
        }
    }
}

@Composable
private fun DesignCard(
    name: String, help: String, selected: Boolean, tag: String?, enabled: Boolean,
    picture: (@Composable () -> Unit)?, onSelect: () -> Unit,
) {
    val shape = RoundedCornerShape(NextSpace.CardRadius)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(NextColors.Surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) NextColors.Int else NextColors.Edge, shape)
            .selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect),
    ) {
        if (picture != null) Box(Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp)) { picture() }
        NextRow(name, note = RowNote.Help(help), trailing = { if (tag != null) NextTag(tag, if (selected) TagKind.INT else TagKind.MUTE) })
    }
}

// ---------------------------------------------------------------- Size

/** Screen › Size (frames 18, 19): text and control size, and how much fits (Auto or a fixed dpi). */
@Composable
fun NextSizePage() {
    val ctx = LocalContext.current
    val text by rememberSetting("car_text_size") { CarTextSize.byId(carTextSize).id }
    val controls by rememberSetting("car_control_size") { CarControlSize.byId(carControlSize).id }
    val stored by rememberSetting("car_density") { carDensity }
    val density = effectiveDensity(stored)
    var picking by rememberSaveable { mutableStateOf(false) }
    val settings = remember { Settings(ctx) }
    ScreenPage("screen/size") {
        item { CarPicture(stringResource(R.string.next_home_car_screen_label), Modifier.fillMaxWidth(), onPart = null) }
        item {
            SizeChoice(stringResource(R.string.next_scr_text_size_label), text, text != CarTextSize.DEFAULT.id,
                CarTextSize.entries.map { Choice(it.id, stringResource(it.labelRes)) }) { settings.carTextSize = it }
        }
        item {
            SizeChoice(stringResource(R.string.next_scr_control_size_label), controls, controls != CarControlSize.DEFAULT.id,
                CarControlSize.entries.map { Choice(it.id, stringResource(it.labelRes)) }) { settings.carControlSize = it }
        }
        item {
            NextSection(stringResource(R.string.next_scr_fits_label), extra = { NextConnectionTag() })
            NextSegmented(
                listOf(Choice(false, stringResource(R.string.next_scr_auto_label)), Choice(true, stringResource(R.string.next_scr_fixed_label))),
                density != 0,
                { fixed -> if (fixed) picking = true else settings.carDensity = 0 },
            )
        }
        if (density != 0) item {
            Spacer(Modifier.height(12.dp))
            NextCard {
                NextValueRow(stringResource(R.string.next_scr_density_label), stringResource(R.string.next_scr_dpi_label, density), changed = true) { picking = true }
            }
        }
        item {
            NextNote(stringResource(R.string.next_scr_fixed_help), Icons.Outlined.Info)
            NextResetPage({
                val before = Triple(settings.carTextSize, settings.carControlSize, settings.carDensity)
                settings.carTextSize = CarTextSize.DEFAULT.id
                settings.carControlSize = CarControlSize.DEFAULT.id
                settings.carDensity = 0
                return@NextResetPage {
                    settings.carTextSize = before.first
                    settings.carControlSize = before.second
                    settings.carDensity = before.third
                }
            }, enabled = text != CarTextSize.DEFAULT.id || controls != CarControlSize.DEFAULT.id || stored != 0)
        }
    }
    if (picking) NextSheet(stringResource(R.string.next_scr_fits_label), { picking = false }, sub = stringResource(R.string.next_scr_fits_help)) {
        NextCard {
            FIXED_DENSITIES.forEachIndexed { i, dpi ->
                if (i > 0) NextDivider(16.dp)
                NextRow(
                    stringResource(R.string.next_scr_dpi_label, dpi), onClick = { settings.carDensity = dpi; picking = false }, role = Role.RadioButton,
                ) { if (dpi == density) Icon(Icons.Rounded.Check, null, tint = NextColors.Int) }
            }
        }
    }
}

@Composable
private fun SizeChoice(label: String, value: String, changed: Boolean, choices: List<Choice<String>>, onPick: (String) -> Unit) {
    NextSection(label, extra = if (changed) ({ NextChangedMark() }) else null)
    NextSegmented(choices, value, onPick)
}

// ---------------------------------------------------------------- Wallpaper

/** Screen › Wallpaper (frame 15): none, the built-in gradients or a photo, with a live preview. */
@Composable
fun NextWallpaperPage() {
    val ctx = LocalContext.current
    val nav = LocalNextNavigator.current
    val scope = rememberCoroutineScope()
    val chosen by rememberSetting("wallpaper") { wallpaper }
    val edit by rememberSetting("myplay_wallpaper_edit") { myplayWallpaperEdit }
    var photoVersion by rememberSaveable { mutableIntStateOf(0) }
    val failed = stringResource(R.string.setup_wallpaper_failed)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            if (withContext(Dispatchers.IO) { saveWallpaper(ctx, uri) && WallpaperFiles.keepSource(ctx) }) {
                val s = Settings(ctx)
                // Through "none", so the car reloads the photo even if one was already set.
                s.wallpaper = "none"; s.wallpaper = Settings.WALLPAPER_CUSTOM
                s.myplayWallpaperEdit = WallpaperEdit()
                photoVersion++
                // A new photo goes straight to the editor, to place it in the car screen's shape.
                nav.open("screen/wallpaper/edit")
            } else Toast.makeText(ctx, failed, Toast.LENGTH_SHORT).show()
        }
    }
    val photo by produceState<Bitmap?>(null, photoVersion, edit) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                BitmapFactory.decodeFile(WallpaperFiles.shown(ctx).path, BitmapFactory.Options().apply { inSampleSize = 2 })
            }.getOrNull()
        }
    }
    val hasPhoto = remember(photoVersion) { WallpaperFiles.shown(ctx).isFile }
    ScreenPage("screen/wallpaper") {
        item {
            Box(
                Modifier.fillMaxWidth().aspectRatio(CarPictureSize.ASPECT).clip(RoundedCornerShape(20.dp))
                    .border(1.dp, NextColors.Edge, RoundedCornerShape(20.dp)).background(NextColors.Surface),
            ) {
                WallpaperDrawing(chosen, photo, Modifier.fillMaxSize())
                Box(Modifier.align(Alignment.BottomStart).padding(10.dp)) { NextTag(stringResource(R.string.next_scr_live_label), TagKind.MUTE) }
            }
            Spacer(Modifier.height(12.dp))
        }
        item {
            NextCard {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    (listOf("none") + WALLPAPER_NAMES + Settings.WALLPAPER_CUSTOM).forEach { name ->
                        WallpaperSwatch(name, photo, name == chosen) {
                            val s = Settings(ctx)
                            when {
                                name != Settings.WALLPAPER_CUSTOM -> s.wallpaper = name
                                hasPhoto -> { s.wallpaper = "none"; s.wallpaper = Settings.WALLPAPER_CUSTOM }
                                else -> picker.launch("image/*")
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        if (chosen == Settings.WALLPAPER_CUSTOM) item {
            NextGroup(listOf(
                {
                    NextValueRow(
                        stringResource(R.string.next_scr_edit_photo_label),
                        if (edit.dimPercent > 0) stringResource(R.string.next_scr_darker_value_label, edit.dimPercent) else null,
                        Icons.Outlined.Tune, changed = edit != WallpaperEdit(),
                    ) { nav.open("screen/wallpaper/edit") }
                },
                { NextValueRow(stringResource(R.string.next_scr_other_photo_label), icon = Icons.Outlined.Image) { picker.launch("image/*") } },
            ))
            NextNote(stringResource(R.string.next_scr_photo_private_help), Icons.Outlined.Info)
        }
    }
}

@Composable
private fun WallpaperDrawing(name: String, photo: Bitmap?, modifier: Modifier) {
    when {
        name == "none" -> Box(modifier.background(NextColors.Bg))
        name == Settings.WALLPAPER_CUSTOM && photo != null -> Image(photo.asImageBitmap(), null, modifier, contentScale = ContentScale.Crop)
        name == Settings.WALLPAPER_CUSTOM -> Box(modifier.background(NextColors.Surface2))
        // The car's own drawing of a built-in wallpaper.
        else -> Wallpaper(remember(name) { CarUiState().apply { wallpaper = name } }, modifier)
    }
}

@Composable
private fun WallpaperSwatch(name: String, photo: Bitmap?, selected: Boolean, onClick: () -> Unit) {
    val label = when (name) {
        "none" -> stringResource(R.string.next_scr_none_label)
        Settings.WALLPAPER_CUSTOM -> stringResource(R.string.next_scr_photo_label)
        else -> name.replaceFirstChar(Char::uppercase)
    }
    val shape = RoundedCornerShape(16.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Box(
            Modifier.size(56.dp).clip(shape)
                .border(if (selected) 2.dp else 1.dp, if (selected) NextColors.Int else NextColors.Edge2, shape)
                .padding(if (selected) 3.dp else 0.dp).clip(RoundedCornerShape(13.dp))
                .selectable(selected, role = Role.RadioButton, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            WallpaperDrawing(name, photo, Modifier.fillMaxSize())
            if (name == "none") Icon(Icons.Rounded.Close, null, tint = NextColors.Text2)
            if (name == Settings.WALLPAPER_CUSTOM && photo == null) Icon(Icons.Outlined.Image, null, tint = NextColors.Text2)
        }
        Text(label, style = NextType.Help, color = if (selected) NextColors.Text else NextColors.Text2, modifier = Modifier.padding(top = 6.dp))
    }
}

/** Screen › Wallpaper › Edit the photo (frame 16): full screen, drag and pinch, Darker and Blur. */
@Composable
fun NextPhotoEditorPage() {
    val ctx = LocalContext.current
    val nav = LocalNextNavigator.current
    val scope = rememberCoroutineScope()
    val settings = remember { Settings(ctx) }
    var edit by rememberSaveable(stateSaver = Saver<WallpaperEdit, String>({ it.encode() }, { WallpaperEdit.decode(it) })) {
        mutableStateOf(settings.myplayWallpaperEdit)
    }
    var saving by remember { mutableStateOf(false) }
    val source by produceState<Bitmap?>(null) { value = withContext(Dispatchers.IO) { WallpaperFiles.loadSource(ctx) } }
    val preview by produceState<Bitmap?>(null, source, edit) {
        val s = source ?: return@produceState
        value = withContext(Dispatchers.Default) { WallpaperFiles.render(s, edit, 480, 289) }
    }
    val close = { nav.back() }
    Column(Modifier.fillMaxSize().background(NextColors.Bg).statusBarsPadding().navigationBarsPadding().testTag("next-page-screen/wallpaper/edit")) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(start = 4.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            val closeWord = stringResource(R.string.next_scr_close_label)
            Box(
                Modifier.size(NextSpace.Target).clip(CircleShape).clickable(role = Role.Button, onClick = close)
                    .semantics { contentDescription = closeWord },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Close, null, tint = NextColors.Text2) }
            Text(stringResource(R.string.next_screen_wallpaper_edit_label), style = NextType.PageTitle, color = NextColors.Text, modifier = Modifier.weight(1f))
            NextButton(stringResource(R.string.next_scr_start_again_label), { edit = WallpaperEdit() }, kind = ButtonKind.TEXT, small = true)
        }
        Column(Modifier.weight(1f).padding(horizontal = NextSpace.Gutter)) {
            BoxWithConstraints(
                Modifier.fillMaxWidth().aspectRatio(CarPictureSize.ASPECT).clip(RoundedCornerShape(4.dp))
                    .border(2.dp, NextColors.Text, RoundedCornerShape(4.dp)).background(NextColors.Surface),
                contentAlignment = Alignment.Center,
            ) {
                val viewW = constraints.maxWidth.toFloat()
                val viewH = constraints.maxHeight.toFloat()
                val s = source
                if (s == null) Text(stringResource(R.string.phone_wallpaper_edit_none), style = NextType.Value, color = NextColors.Text2)
                else Box(
                    Modifier.fillMaxSize().pointerInput(s) {
                        detectTransformGestures { _, pan, zoom, _ -> edit = dragged(edit, s.width, s.height, viewW, viewH, pan.x, pan.y, zoom) }
                    },
                ) { preview?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds) } }
            }
            NextNote(stringResource(R.string.next_scr_drag_help), Icons.Outlined.TouchApp)
            Spacer(Modifier.height(12.dp))
            NextCard {
                NextSlider(
                    stringResource(R.string.next_scr_darker_label), "${edit.dimPercent} %", edit.dimPercent.toFloat(), 0f..WallpaperEdit.MAX_DIM.toFloat(),
                    { edit = edit.copy(dimPercent = it.toInt()) }, ends = "0 %" to "${WallpaperEdit.MAX_DIM} %", changed = edit.dimPercent != 0,
                )
                NextDivider(16.dp)
                NextSlider(
                    stringResource(R.string.next_scr_blur_label), "${edit.blur}", edit.blur.toFloat(), 0f..WallpaperEdit.MAX_BLUR.toFloat(),
                    { edit = edit.copy(blur = it.toInt()) }, steps = WallpaperEdit.MAX_BLUR - 1, ends = "0" to "${WallpaperEdit.MAX_BLUR}",
                    changed = edit.blur != 0,
                )
            }
        }
        NextButton(
            stringResource(if (saving) R.string.next_scr_saving_label else R.string.next_scr_use_label),
            onClick = {
                val s = source ?: return@NextButton
                if (saving) return@NextButton
                saving = true
                val chosen = edit
                scope.launch {
                    val ok = withContext(Dispatchers.Default) {
                        WallpaperFiles.write(ctx, WallpaperFiles.render(s, chosen, WallpaperFiles.OUT_W, WallpaperFiles.OUT_H))
                    }
                    if (ok) {
                        settings.myplayWallpaperEdit = chosen
                        // Through "none", so the car reloads the file.
                        settings.wallpaper = "none"; settings.wallpaper = Settings.WALLPAPER_CUSTOM
                    }
                    saving = false
                    if (ok) close()
                }
            },
            modifier = Modifier.fillMaxWidth().padding(NextSpace.Gutter),
            enabled = source != null && !saving,
        )
    }
}

// ---------------------------------------------------------------- Map colours

/** Screen › Map colours (frame 17): before and after, presets, the six values, Reset. */
@OptIn(ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NextMapColoursPage() {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    val saved by rememberSetting("myplay_map_filter") { myplayMapFilter }
    // The slider being dragged shows its value at once; the setting is written when the finger lifts.
    var dragging by remember { mutableStateOf<MapColourFilter?>(null) }
    val filter = dragging ?: saved
    var pickingTint by rememberSaveable { mutableStateOf(false) }
    val default = MapColourFilter()
    fun save(f: MapColourFilter) { settings.myplayMapFilter = f.clamped(); dragging = null }
    val kind = mapColoursKind(filter)
    LazyColumn(
        Modifier.fillMaxSize().testTag("next-page-screen/map-colours"),
        state = rememberNextListState(),
        contentPadding = PaddingValues(start = NextSpace.Gutter, end = NextSpace.Gutter, bottom = 24.dp),
    ) {
        // The before/after stays at the top while the values scroll under it (design frame 17).
        stickyHeader {
            Box(Modifier.background(NextColors.Bg).padding(bottom = 8.dp)) {
                NextCard {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterSampleMap(filteredSampleMap(default, false), stringResource(R.string.next_scr_before_help), Modifier.weight(1f))
                        FilterSampleMap(filteredSampleMap(filter, false), stringResource(R.string.next_scr_after_help), Modifier.weight(1f))
                    }
                }
            }
        }
        item { NextSection(stringResource(R.string.next_scr_preset_label)) }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MapColourFilter.PRESETS.forEach { (key, f) -> NextChip(mapPresetName(key), key == kind) { save(f) } }
            }
        }
        item { NextSection(stringResource(R.string.next_scr_fine_tune_label)) }
        item {
            NextCard {
                fun drag(f: MapColourFilter) { dragging = f }
                NextSlider(stringResource(R.string.next_scr_hue_label), "${filter.hueShift}°", filter.hueShift.toFloat(), -180f..180f,
                    { drag(filter.copy(hueShift = Math.round(it))) }, { dragging?.let { save(it) } }, ends = "-180°" to "180°", changed = filter.hueShift != default.hueShift)
                NextDivider(16.dp)
                NextSlider(stringResource(R.string.next_scr_colourful_label), "${filter.saturation} %", filter.saturation.toFloat(), 0f..200f,
                    { drag(filter.copy(saturation = Math.round(it))) }, { dragging?.let { save(it) } }, ends = "0 %" to "200 %", changed = filter.saturation != default.saturation)
                NextDivider(16.dp)
                NextSlider(stringResource(R.string.next_scr_brightness_label), "${filter.brightness}", filter.brightness.toFloat(), -100f..100f,
                    { drag(filter.copy(brightness = Math.round(it))) }, { dragging?.let { save(it) } }, ends = "-100" to "100", changed = filter.brightness != default.brightness)
                NextDivider(16.dp)
                NextSlider(stringResource(R.string.next_scr_contrast_label), "${filter.contrast} %", filter.contrast.toFloat(), 0f..200f,
                    { drag(filter.copy(contrast = Math.round(it))) }, { dragging?.let { save(it) } }, ends = "0 %" to "200 %", changed = filter.contrast != default.contrast)
                NextDivider(16.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        NextSlider(stringResource(R.string.next_scr_tint_label), "${filter.tintStrength} %", filter.tintStrength.toFloat(), 0f..100f,
                            { drag(filter.copy(tintStrength = Math.round(it))) }, { dragging?.let { save(it) } }, ends = "0 %" to "100 %",
                            changed = filter.tintStrength != default.tintStrength)
                    }
                    val tintWord = stringResource(R.string.next_scr_tint_colour_label)
                    Box(
                        Modifier.padding(end = 16.dp).size(36.dp).clip(CircleShape).background(Color(0xFF000000.toInt() or filter.tint))
                            .border(1.dp, NextColors.Edge2, CircleShape).clickable(role = Role.Button) { pickingTint = true }
                            .semantics { contentDescription = tintWord },
                    )
                }
                NextDivider(16.dp)
                NextSlider(stringResource(R.string.next_scr_night_label), "${filter.nightDarker} %", filter.nightDarker.toFloat(), 0f..90f,
                    { drag(filter.copy(nightDarker = Math.round(it))) }, { dragging?.let { save(it) } }, ends = "0 %" to "90 %", changed = filter.nightDarker != default.nightDarker)
            }
        }
        item {
            NextResetPage({
                val before = settings.myplayMapFilter
                settings.myplayMapFilter = default
                return@NextResetPage { settings.myplayMapFilter = before }
            }, enabled = kind != "none")
        }
    }
    if (pickingTint) {
        ColourPickerDialog(stringResource(R.string.next_scr_tint_colour_label), 0xFF000000.toInt() or filter.tint, onDismiss = { pickingTint = false }) { c ->
            pickingTint = false
            save(filter.copy(tint = c and 0xFFFFFF, tintStrength = if (filter.tintStrength == 0) 20 else filter.tintStrength))
        }
    }
}
