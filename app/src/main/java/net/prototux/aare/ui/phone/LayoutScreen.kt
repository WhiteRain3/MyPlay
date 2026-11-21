package net.prototux.aare.ui.phone

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Splitscreen
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.prototux.aare.R
import net.prototux.aare.ui.car.myplay.myplayEnginePreview
import net.prototux.aare.ui.car.myplay.myplayEngineLanguage
import net.prototux.aare.ui.car.myplay.MyPlaySkins
import net.prototux.aare.Settings
import net.prototux.aare.ui.car.CarLayout
import net.prototux.aare.ui.car.CarLayoutSpec
import net.prototux.aare.ui.car.CarLayoutSpec.BarSide
import net.prototux.aare.ui.car.CarLayoutSpec.BarStyle
import net.prototux.aare.ui.car.CarLayoutSpec.CardPlace
import net.prototux.aare.ui.car.CarLayoutSpec.SpecCard
import net.prototux.aare.ui.car.changeLayout
import net.prototux.aare.ui.car.chooseLayoutPreset
import net.prototux.aare.ui.car.movedBy
import net.prototux.aare.ui.car.myplayLayoutPreset
import net.prototux.aare.ui.car.myplayLayoutSpec

/** A number that goes up whenever any setting changes, to re-read settings that rules may have changed. */
@Composable
fun rememberSettingsChanges(): Int {
    val ctx = LocalContext.current
    var version by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        val prefs = ctx.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
        val l = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> version++ }
        prefs.registerOnSharedPreferenceChangeListener(l)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(l) }
    }
    return version
}

/** The "hide after" choices: never (null) or a number of seconds. */
val HIDE_CHOICES: List<Int?> = listOf(null, 5, 15, 30, 60)

/** The "hide after" pills: [HIDE_CHOICES], plus the stored value in its place when it is not one of them, so it shows as it is. */
fun hideOptions(stored: Int?): List<Int?> =
    if (stored in HIDE_CHOICES) HIDE_CHOICES else listOf(null) + (HIDE_CHOICES.filterNotNull() + stored!!).sorted()

/** The rows of the layout's card list: the shown ones in their order, then the others. */
fun specCardRows(cards: List<SpecCard>): List<SpecCard> = cards + SpecCard.entries.filter { it !in cards }

/** [cards] with [card] shown (at the end) or hidden; the last one cannot go (the rules keep at least one). */
fun List<SpecCard>.withSpecCard(card: SpecCard, shown: Boolean): List<SpecCard> = when {
    shown -> if (card in this) this else this + card
    size <= 1 -> this
    else -> this - card
}

/**
 * The car screen's layout (plan E6, with phase M): which kind of layout, and for "map behind,
 * cards on top" the presets A to F as pictures, then each of the eight options. Choosing a
 * preset sets all eight; changing one keeps the rest. Reads and writes only through the car
 * side's MyPlayLayoutSettings, which keeps the layout's rules.
 */
@Composable
fun LayoutScreen() {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    var layoutId by remember { mutableStateOf(CarLayout.byId(settings.carLayoutId).id) }
    // Re-read after every change: the rules may have changed other options too.
    val version = rememberSettingsChanges()
    val spec = remember(version) { settings.myplayLayoutSpec }
    val preset = remember(version) { settings.myplayLayoutPreset }
    fun change(f: (CarLayoutSpec) -> CarLayoutSpec) = settings.changeLayout(f)

    Column {
        PartPicture()
        OrbitPreviewSwitch()
        SectionHeading(stringResource(R.string.phone_layout_kind))
        PhoneChoices(
            stringResource(R.string.phone_layout_kind_hint),
            listOf(
                CarLayout.MAP_BEHIND.id to stringResource(R.string.myplay_layout_map_behind),
                CarLayout.TASKBAR.id to stringResource(R.string.setup_layout_taskbar),
                CarLayout.DOCK.id to stringResource(R.string.setup_layout_dock),
                CarLayout.CLASSIC.id to stringResource(R.string.setup_layout_classic),
            ),
            layoutId,
        ) { layoutId = it; settings.carLayoutId = it }
        HintLine(Icons.Rounded.Layers, stringResource(R.string.phone_layout_at_once))
        if (layoutId == CarLayout.MAP_BEHIND.id) MapBehindOptions(spec, preset, { settings.chooseLayoutPreset(it) }, ::change)
    }
}

/**
 * The "map behind, cards on top" layout: the presets A to F as pictures ([preset]: the chosen
 * one, "" for an own mix), then each option of [spec]. [onPreset] chooses a preset; [change]
 * changes options (the rules are kept by the settings).
 */
@Composable
fun MapBehindOptions(spec: CarLayoutSpec, preset: String, onPreset: (String) -> Unit, change: ((CarLayoutSpec) -> CarLayoutSpec) -> Unit) {
    Column {
        SectionHeading(stringResource(R.string.phone_layout_presets))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CarLayoutSpec.PRESETS.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { (name, presetSpec) ->
                        PresetPicture(name, presetSpec, name == preset, Modifier.weight(1f)) { onPreset(name) }
                    }
                }
            }
        }
        HintLine(Icons.Rounded.Layers, stringResource(if (preset.isEmpty()) R.string.phone_layout_own else R.string.phone_layout_preset_hint))

        SectionHeading(stringResource(R.string.phone_layout_map))
        RowGroup(rows = listOf(
            { SwitchRow(Icons.Rounded.Map, stringResource(R.string.phone_layout_map_background), spec.mapBackground, TileTone.GREEN,
                stringResource(R.string.phone_layout_map_background_sub)) { on -> change { it.copy(mapBackground = on) } } },
            {
                // Only with the map behind everything (the layout's rule): otherwise shown off and not switchable.
                if (spec.mapBackground) SwitchRow(Icons.Rounded.Splitscreen, stringResource(R.string.phone_layout_map_under), spec.mapUnderBar, TileTone.GREEN,
                    stringResource(R.string.phone_layout_map_under_sub)) { on -> change { it.copy(mapUnderBar = on) } }
                else SimpleRow(Icons.Rounded.Splitscreen, stringResource(R.string.phone_layout_map_under), TileTone.GREEN,
                    stringResource(R.string.phone_layout_map_under_needs)) { PhoneSwitch(false, null) }
            },
        ))

        SectionHeading(stringResource(R.string.phone_layout_bar))
        MapBehindBar(spec, change)

        SectionHeading(stringResource(R.string.phone_layout_cards))
        PartCard(padding = 16.dp) {
            Column {
                PhoneChoices(stringResource(R.string.phone_layout_card_place), listOf(
                    CardPlace.COLUMN_BY_BAR.key to stringResource(R.string.phone_layout_place_column),
                    CardPlace.CORNERS.key to stringResource(R.string.phone_layout_place_corners),
                    CardPlace.STAGE.key to stringResource(R.string.phone_layout_place_stage),
                    CardPlace.TOP_BANNER_AND_CORNER.key to stringResource(R.string.phone_layout_place_banner),
                ), spec.cardPlace.key) { k -> CardPlace.entries.firstOrNull { it.key == k }?.let { p -> change { it.copy(cardPlace = p) } } }
                SeeThrough(spec.cardSeeThrough) { v -> change { it.copy(cardSeeThrough = v) } }
            }
        }
        Spacer(Modifier.height(12.dp))
        MapBehindCardRows(spec, change)

        SectionHeading(stringResource(R.string.phone_layout_hide))
        PartCard(padding = 16.dp) {
            Column {
                val never = stringResource(R.string.phone_layout_hide_never)
                val seconds = stringResource(R.string.phone_layout_hide_seconds)
                fun options(stored: Int?) = hideOptions(stored).map { (it?.toString() ?: "") to (it?.let { s -> seconds.format(s) } ?: never) }
                PhoneChoices(stringResource(R.string.phone_layout_hide_music), options(spec.hideMusicAfterS), spec.hideMusicAfterS?.toString() ?: "") { v ->
                    change { it.copy(hideMusicAfterS = v.toIntOrNull()) } }
                PhoneChoices(stringResource(R.string.phone_layout_hide_arrival), options(spec.hideArrivalAfterS), spec.hideArrivalAfterS?.toString() ?: "") { v ->
                    change { it.copy(hideArrivalAfterS = v.toIntOrNull()) } }
            }
        }
    }
}

/** The "map behind" layout's bar: its look and its side. Shared by the Layout and Bar pages. */
@Composable
fun MapBehindBar(spec: CarLayoutSpec, change: ((CarLayoutSpec) -> CarLayoutSpec) -> Unit) {
    PartCard(padding = 16.dp) {
        Column {
            PhoneChoices(stringResource(R.string.phone_layout_bar_style), listOf(
                BarStyle.SOLID_RAIL.key to stringResource(R.string.phone_layout_style_solid),
                BarStyle.SEE_THROUGH_RAIL.key to stringResource(R.string.phone_layout_style_see_through),
                BarStyle.FLOATING_PILLS.key to stringResource(R.string.phone_layout_style_pills),
                BarStyle.BOTTOM_DOCK.key to stringResource(R.string.phone_layout_style_dock),
                BarStyle.TOP_RIBBON.key to stringResource(R.string.phone_layout_style_ribbon),
            ), spec.barStyle.key) { k -> BarStyle.entries.firstOrNull { it.key == k }?.let { s -> change { it.copy(barStyle = s) } } }
            PhoneChoices(stringResource(R.string.phone_layout_bar_side), listOf(
                BarSide.LEFT.key to stringResource(R.string.setup_choice_left),
                BarSide.RIGHT.key to stringResource(R.string.setup_choice_right),
                BarSide.BOTTOM.key to stringResource(R.string.setup_choice_bottom),
                BarSide.TOP.key to stringResource(R.string.phone_layout_side_top),
            ), spec.barSide.key) { k -> BarSide.entries.firstOrNull { it.key == k }?.let { s -> change { it.copy(barSide = s) } } }
            if (spec.barStyle == BarStyle.BOTTOM_DOCK || spec.barStyle == BarStyle.TOP_RIBBON) {
                Text(stringResource(R.string.phone_layout_side_fixed), color = PhoneColors.Muted, fontSize = 13.sp)
            }
        }
    }
}

/** The "map behind" layout's cards: which, in what order, and one extra at a time. Shared by the Layout and Cards pages. */
@Composable
fun MapBehindCardRows(spec: CarLayoutSpec, change: ((CarLayoutSpec) -> CarLayoutSpec) -> Unit) {
    // Drag a card to a place (the Design reviewer's slots); the rows below do the same with arrows.
    CardSlots(SpecCard.entries, spec.cards, { stringResource(it.title) }, { it.symbol }, { it.tone }) { new ->
        change { it.copy(cards = new) }
    }
    Spacer(Modifier.height(12.dp))
    val cardRows: List<@Composable () -> Unit> = specCardRows(spec.cards).map { card ->
        @Composable {
            val i = spec.cards.indexOf(card)
            val shown = i >= 0
            SimpleRow(card.symbol, stringResource(card.title), card.tone) {
                if (shown) {
                    RoundIconButton(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.common_up), enabled = i > 0, size = 36.dp) {
                        change { it.copy(cards = it.cards.movedBy(i, -1)) } }
                    RoundIconButton(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.common_down), enabled = i < spec.cards.lastIndex, size = 36.dp) {
                        change { it.copy(cards = it.cards.movedBy(i, 1)) } }
                }
                PhoneSwitch(shown, if (shown && spec.cards.size <= 1) null else { on -> change { it.copy(cards = it.cards.withSpecCard(card, on)) } })
            }
        }
    }
    val oneExtra: List<@Composable () -> Unit> = if (spec.cardPlace != CardPlace.STAGE) emptyList() else listOf(@Composable {
        SwitchRow(Icons.Rounded.ViewAgenda, stringResource(R.string.phone_layout_one_extra), spec.oneExtraAtATime, TileTone.VIOLET,
            stringResource(R.string.phone_layout_one_extra_sub)) { on -> change { it.copy(oneExtraAtATime = on) } }
    })
    RowGroup(rows = cardRows + oneExtra)
}

/** How see-through the cards are, in whole percents; written when the finger lets go. */
@Composable
private fun SeeThrough(value: Int, onChange: (Int) -> Unit) {
    var dragging by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Text(stringResource(R.string.phone_layout_see_through, dragging.toInt()), color = PhoneColors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    Slider(
        // Whole percents, no fixed steps, so a stored value such as 14 % stays what it is.
        dragging, { dragging = kotlin.math.round(it) },
        valueRange = 0f..100f,
        onValueChangeFinished = { onChange(dragging.toInt()) },
        colors = SliderDefaults.colors(thumbColor = PhoneColors.Accent, activeTrackColor = PhoneColors.Accent, inactiveTrackColor = PhoneColors.SwitchOff),
    )
}

/** A preset drawn by the car's own code, small, with its letter; outlined green when it is the chosen one. */
@Composable
private fun PresetPicture(name: String, spec: CarLayoutSpec, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val ctx = LocalContext.current
    // Drawn again when any setting changes (look, wallpaper, bar), like the big picture.
    val version = rememberSettingsChanges()
    val ui = remember(name, spec, version) {
        sampleCarState().apply {
            applyPictureSettings(this, Settings(ctx), ctx)
            layout = CarLayout.MAP_BEHIND
            layoutSpec = spec
        }
    }
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .clip(shape)
            .border(BorderStroke(if (selected) 2.5.dp else 1.dp, if (selected) PhoneColors.Accent else androidx.compose.ui.graphics.Color.Transparent), shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(4.dp),
    ) {
        CarPictureFrame(ui, stringResource(R.string.phone_layout_preset_name, name), onPart = null)
        Text(stringResource(R.string.phone_layout_preset_name, name), Modifier.fillMaxWidth().padding(top = 4.dp),
            color = if (selected) PhoneColors.Accent else PhoneColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

private val SpecCard.symbol: ImageVector
    get() = when (this) {
        SpecCard.NEXT_TURN -> Icons.Rounded.Navigation
        SpecCard.ARRIVAL -> Icons.Rounded.DirectionsCar
        SpecCard.MUSIC -> Icons.Rounded.MusicNote
        SpecCard.CLOCK_STATUS -> Icons.Rounded.AccessTime
    }

private val SpecCard.tone: TileTone
    get() = when (this) {
        SpecCard.NEXT_TURN -> TileTone.GREEN
        SpecCard.ARRIVAL -> TileTone.BLUE
        SpecCard.MUSIC -> TileTone.PINK
        SpecCard.CLOCK_STATUS -> TileTone.AMBER
    }

private val SpecCard.title: Int
    get() = when (this) {
        SpecCard.NEXT_TURN -> R.string.phone_layout_card_turn
        SpecCard.ARRIVAL -> R.string.phone_layout_card_arrival
        SpecCard.MUSIC -> R.string.phone_layout_card_music
        SpecCard.CLOCK_STATUS -> R.string.phone_layout_card_clock
    }


/** N5: try MyPlay's own car screen, ORBIT, while it is being built (myplay_engine_preview, not backed up). */
@Composable
private fun OrbitPreviewSwitch() {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    var on by remember { mutableStateOf(settings.myplayEnginePreview) }
    var language by remember { mutableStateOf(settings.myplayEngineLanguage) }
    Spacer(Modifier.height(12.dp))
    RowGroup(rows = listOf {
        SwitchRow(Icons.Rounded.Layers, stringResource(R.string.phone_orbit_preview_title), on, TileTone.VIOLET,
            stringResource(R.string.phone_orbit_preview_hint)) { on = it; settings.myplayEnginePreview = it }
    })
    // N15: which of MyPlay's own designs; it takes effect at once, without reconnecting.
    if (on) {
        Spacer(Modifier.height(12.dp))
        PhoneChoices(
            stringResource(R.string.phone_engine_language),
            MyPlaySkins.ALL.map { it.id to engineLanguageName(it.id) },
            language,
        ) { id -> language = MyPlaySkins.byId(id).id; settings.myplayEngineLanguage = language }
        if (language == "deck") HintLine(Icons.Rounded.Layers, stringResource(R.string.phone_engine_deck_preview))
    }
}

/** A design's name as the phone shows it: its stored id in capitals (ORBIT, DECK). */
fun engineLanguageName(id: String): String = id.uppercase(java.util.Locale.ROOT)
