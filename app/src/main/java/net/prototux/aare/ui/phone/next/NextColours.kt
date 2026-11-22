package net.prototux.aare.ui.phone.next

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.ui.phone.ColourPickerDialog
import net.prototux.aare.ui.phone.hardToRead
import net.prototux.aare.ui.phone.label
import net.prototux.aare.ui.phone.roleColour
import net.prototux.aare.ui.phone.side
import net.prototux.aare.ui.phone.with
import net.prototux.aare.ui.theme.CarAccents
import net.prototux.aare.ui.theme.CarThemeSpec
import net.prototux.aare.ui.theme.CarThemes
import net.prototux.aare.ui.theme.ColourRole
import net.prototux.aare.ui.theme.CustomColours
import net.prototux.aare.ui.theme.contrastRatio
import net.prototux.aare.ui.theme.myplayLookColours
import net.prototux.aare.ui.theme.withAccent
import net.prototux.aare.ui.theme.forPhone
import net.prototux.aare.ui.theme.withCustomColours

/** The colours an app screen gets inside ORBIT: the theme, then the accent, then the owner's own. */
@Composable
private fun rememberLook(themeId: String, accent: String, own: CustomColours): CarThemeSpec {
    val ctx = LocalContext.current
    return remember(themeId, accent, own) { CarThemes.byId(themeId).forPhone(ctx).withAccent(accent, ctx).withCustomColours(own) }
}

/** Screen › Colours (frame 13): what app screens inside ORBIT are painted with. */
@OptIn(ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun NextColoursPage() {
    val ctx = LocalContext.current
    val nav = LocalNextNavigator.current
    val settings = remember { Settings(ctx) }
    val themeId by rememberSetting("style_theme", "car_style") { carThemeId }
    val accent by rememberSetting("style_accent") { carAccent }
    val dayNight by rememberSetting("car_theme") { carTheme }
    val own by rememberSetting("myplay_look_colours") { CustomColours.decode(myplayLookColours) }
    val look = rememberLook(themeId, accent, own)
    val night = dayNight != Settings.THEME_LIGHT
    LazyColumn(
        Modifier.fillMaxSize().testTag("next-page-screen/colours"),
        state = rememberNextListState(),
        contentPadding = PaddingValues(start = NextSpace.Gutter, end = NextSpace.Gutter, bottom = 24.dp),
    ) {
        // The app screen stays at the top while the choices scroll (review finding 9).
        stickyHeader {
            Box(Modifier.background(NextColors.Bg).padding(bottom = 8.dp)) {
                AppScreenPreview(if (night) look.night else look.day)
            }
        }
        item { NextSection(stringResource(R.string.next_col_theme_label)) }
        item {
            NextCard {
                FlowRow(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly, maxItemsInEachRow = 4) {
                    CarThemes.available.forEach { t ->
                        ThemeDisc(t, t.id == themeId) { settings.carThemeId = t.id }
                    }
                }
            }
        }
        item { NextSection(stringResource(R.string.next_col_accent_label)) }
        item {
            NextCard {
                FlowRow(
                    Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    NextChip(stringResource(R.string.next_col_themes_label), accent == CarAccents.THEME) { settings.carAccent = CarAccents.THEME }
                    CarAccents.all.forEach { a ->
                        val name = stringResource(a.nameRes)
                        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            NextSwatchSmall(a.dark.primary, name, accent == a.id) { settings.carAccent = a.id }
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        NextChip(stringResource(R.string.next_col_phone_label), accent == CarAccents.PHONE) { settings.carAccent = CarAccents.PHONE }
                    }
                }
                NextDivider(16.dp)
                val count = own.night.size + own.day.size
                NextValueRow(
                    stringResource(R.string.next_screen_colours_own_label),
                    if (count == 0) stringResource(R.string.next_col_none_set_label) else ownSummary(own),
                    Icons.Outlined.Palette, changed = count > 0,
                ) { nav.open("screen/colours/own") }
            }
        }
        item { NextSection(stringResource(R.string.next_col_day_night_label), extra = { AlsoOnCarTag() }) }
        item {
            NextSegmented(
                listOf(
                    Choice(Settings.THEME_DARK, stringResource(R.string.next_col_dark_label)),
                    Choice(Settings.THEME_AUTO, stringResource(R.string.next_col_cars_label)),
                    Choice(Settings.THEME_LIGHT, stringResource(R.string.next_col_light_label)),
                ),
                dayNight, { settings.carTheme = it },
            )
        }
        item {
            NextResetPage({
                val before = listOf(settings.carThemeId, settings.carAccent, settings.carTheme, settings.myplayLookColours)
                settings.carThemeId = "aare"; settings.carAccent = CarAccents.THEME
                settings.carTheme = Settings.THEME_DARK; settings.myplayLookColours = ""
                return@NextResetPage {
                    settings.carThemeId = before[0]; settings.carAccent = before[1]
                    settings.carTheme = before[2]; settings.myplayLookColours = before[3]
                }
            }, enabled = themeId != "aare" || accent != CarAccents.THEME || dayNight != Settings.THEME_DARK || !own.isEmpty)
        }
    }
}

@Composable
private fun ownSummary(own: CustomColours): String = listOfNotNull(
    own.night.size.takeIf { it > 0 }?.let { stringResource(R.string.next_col_night_set_label, it) },
    own.day.size.takeIf { it > 0 }?.let { stringResource(R.string.next_col_day_set_label, it) },
).joinToString(" · ")

/** A theme as a disc: its background and accent split on the diagonal, its name under it. */
@Composable
private fun ThemeDisc(theme: CarThemeSpec, selected: Boolean, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val scheme = remember(theme) { theme.forPhone(ctx).night }
    val name = if (theme.nameRes != 0) stringResource(theme.nameRes) else theme.name
    Column(
        Modifier.width(84.dp).padding(vertical = 6.dp).selectable(selected, role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(48.dp).then(if (selected) Modifier.border(2.dp, NextColors.Int, CircleShape).padding(4.dp) else Modifier)
                .clip(CircleShape),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(scheme.background)
                drawPath(Path().apply { moveTo(size.width, 0f); lineTo(size.width, size.height); lineTo(0f, size.height); close() }, scheme.primary)
            }
        }
        Text(name, style = NextType.Help, color = if (selected) NextColors.Text else NextColors.Text2, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp))
    }
}

/** A small named colour disc (accents): TalkBack reads its [name] (review "Second pass" item 6). */
@Composable
private fun NextSwatchSmall(color: Color, name: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(32.dp).then(if (selected) Modifier.border(2.dp, NextColors.Int, CircleShape).padding(3.dp) else Modifier)
            .clip(CircleShape).background(color).selectable(selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = name },
    )
}

/** An app screen inside ORBIT in [scheme]'s colours (frame 13): "Now playing" and "Up next". */
@Composable
private fun AppScreenPreview(scheme: ColorScheme) {
    val shape = RoundedCornerShape(20.dp)
    Box(Modifier.fillMaxWidth().height(200.dp).clip(shape).background(scheme.background).border(1.dp, NextColors.Edge, shape).padding(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1.4f)) {
                Text(stringResource(R.string.next_col_now_playing_label), style = NextType.Tag, color = scheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)).background(scheme.primary))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.width(80.dp).height(8.dp).clip(CircleShape).background(scheme.onSurface))
                        Box(Modifier.width(56.dp).height(6.dp).clip(CircleShape).background(scheme.onSurfaceVariant))
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.size(30.dp).clip(CircleShape).background(scheme.primary), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.PlayArrow, null, tint = scheme.onPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(8.dp)).background(scheme.surfaceContainer))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.next_col_up_next_label), style = NextType.Tag, color = scheme.onSurfaceVariant)
                repeat(3) {
                    Row(Modifier.fillMaxWidth().height(30.dp).clip(RoundedCornerShape(8.dp)).background(scheme.surfaceContainer).padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(18.dp).clip(RoundedCornerShape(4.dp)).background(scheme.primary.copy(alpha = 1f - it * 0.25f)))
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(scheme.onSurfaceVariant))
                    }
                }
            }
        }
        Box(Modifier.align(Alignment.BottomStart)) { NextTag(stringResource(R.string.next_col_live_label), TagKind.MUTE) }
    }
}

/** Screen › Colours › Your own colours (frame 14): seven parts, night and day apart, each with undo. */
@Composable
fun NextOwnColoursPage() {
    val ctx = LocalContext.current
    val undo = LocalNextUndo.current
    val settings = remember { Settings(ctx) }
    val themeId by rememberSetting("style_theme", "car_style") { carThemeId }
    val accent by rememberSetting("style_accent") { carAccent }
    val own by rememberSetting("myplay_look_colours") { CustomColours.decode(myplayLookColours) }
    var night by rememberSaveable { mutableStateOf(true) }
    var picking by rememberSaveable { mutableStateOf<String?>(null) }
    val look = rememberLook(themeId, accent, own)
    val scheme = if (night) look.night else look.day
    val mine = own.side(night)
    fun save(c: CustomColours) { settings.myplayLookColours = c.encode() }
    val cleared = stringResource(R.string.next_col_cleared_label)
    LazyColumn(
        Modifier.fillMaxSize().testTag("next-page-screen/colours/own"),
        state = rememberNextListState(),
        contentPadding = PaddingValues(start = NextSpace.Gutter, end = NextSpace.Gutter, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            NextSegmented(listOf(Choice(true, stringResource(R.string.next_col_night_label)), Choice(false, stringResource(R.string.next_col_day_label))), night, { night = it })
        }
        val bad = hardToRead({ roleColour(scheme, it) }, mine.keys)
        if (bad.isNotEmpty()) item {
            NextCard(warn = true) {
                bad.forEach { (fg, bg) ->
                    val ratio = contrastRatio(roleColour(scheme, fg), roleColour(scheme, bg))
                    NextRow(
                        stringResource(R.string.next_col_hard_label), icon = Icons.Rounded.WarningAmber, disc = DiscTone.WARN,
                        note = RowNote.Help(stringResource(R.string.next_col_hard_help, stringResource(fg.label), stringResource(bg.label), "%.1f".format(ratio))),
                    )
                }
            }
        }
        item {
            NextCard {
                ColourRole.entries.forEachIndexed { i, role ->
                    if (i > 0) NextDivider()
                    val colour = roleColour(scheme, role)
                    val yours = role in mine
                    val hex = "#%06X".format(colour and 0xFFFFFF)
                    NextRow(
                        stringResource(role.label),
                        lead = { Box(Modifier.size(NextSpace.IconDisc).clip(CircleShape).background(Color(colour)).border(1.dp, NextColors.Edge2, CircleShape)) },
                        note = RowNote.Help(stringResource(if (yours) R.string.next_col_yours_help else R.string.next_col_looks_own_help, hex)),
                        onClick = { picking = role.id }, role = Role.Button,
                        trailing = {
                            if (yours) {
                                val back = stringResource(R.string.next_col_back_to_look_label, stringResource(role.label))
                                Box(
                                    Modifier.minimumInteractiveComponentSize().clip(CircleShape)
                                        .clickable(role = Role.Button) { save(own.with(night, role, null)) }
                                        .semantics { contentDescription = back },
                                    contentAlignment = Alignment.Center,
                                ) { Icon(Icons.Rounded.Undo, null, tint = NextColors.Text2) }
                            }
                        },
                    )
                }
            }
        }
        if (!own.isEmpty) item {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                NextButton(stringResource(R.string.next_col_clear_all_label), {
                    val before = settings.myplayLookColours
                    save(CustomColours.NONE)
                    undo.show(cleared) { settings.myplayLookColours = before }
                }, kind = ButtonKind.DANGER)
            }
        }
    }
    picking?.let { id ->
        val role = ColourRole.byId(id) ?: return@let
        ColourPickerDialog(stringResource(role.label), roleColour(scheme, role), onDismiss = { picking = null }) { c ->
            save(own.with(night, role, c)); picking = null
        }
    }
}

