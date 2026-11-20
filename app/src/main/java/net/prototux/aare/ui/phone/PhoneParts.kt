package net.prototux.aare.ui.phone

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.prototux.aare.R

/*
 * The shared parts of MyPlay's phone screens, drawn after the owner's chosen design (the
 * "merged" pictures: Customise, Battery, Behind the scenes). Sizes are the pictures' sizes.
 */

/** The longest value a symbol tile shows before it is shortened with "…". */
const val TILE_VALUE_MAX = 10

/** A tile's value as shown: trimmed, one line, at most [max] characters, "…" when shortened, "–" when empty. */
fun tileValueText(value: String?, max: Int = TILE_VALUE_MAX): String {
    val v = value.orEmpty().replace(Regex("\\s+"), " ").trim()
    if (v.isEmpty()) return "–"
    if (v.length <= max) return v
    return v.take((max - 1).coerceAtLeast(1)).trimEnd() + "…"
}

/** Which of [colours] is [current], compared as stored colour values; null when none is (shown as "Default"). */
fun selectedColourIndex(colours: List<Color>, current: Color?): Int? {
    if (current == null) return null
    val argb = current.toArgb()
    return colours.indexOfFirst { it.toArgb() == argb }.takeIf { it >= 0 }
}

/** Page title row: an optional round back button, the title, and an optional round help button. */
@Composable
fun PhoneTopBar(title: String, onBack: (() -> Unit)? = null, onHelp: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.common_back), onBack)
            Spacer(Modifier.width(12.dp))
        }
        Text(title, Modifier.weight(1f), color = PhoneColors.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (onHelp != null) RoundIconButton(Icons.AutoMirrored.Rounded.HelpOutline, stringResource(R.string.phone_parts_help), onHelp)
    }
}

/** The round grey button of the design's top row (back, help). */
@Composable
fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(PhoneColors.Circle).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = PhoneColors.CircleIcon, modifier = Modifier.size(24.dp)) }
}

/** A section heading: small capitals, then a thin line to the right edge. */
@Composable
fun SectionHeading(text: String, modifier: Modifier = Modifier, end: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp, start = 2.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), color = PhoneColors.Muted, fontSize = 13.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(1.dp).background(PhoneColors.Border))
        if (end != null) { Spacer(Modifier.width(10.dp)); end() }
    }
}

/**
 * A symbol tile (the "Behind the scenes" tiles on Customise): a symbol at the top, the current
 * value big, the tile's name small under it, and a green dot when it needs attention.
 */
@Composable
fun SymbolTile(
    icon: ImageVector,
    value: String?,
    label: String,
    tone: TileTone,
    modifier: Modifier = Modifier,
    attention: Boolean = false,
    onClick: () -> Unit,
) {
    val shown = tileValueText(value)
    Column(
        modifier
            .height(116.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(tone.background)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = tone.content, modifier = Modifier.size(28.dp))
            Spacer(Modifier.weight(1f))
            if (attention) {
                Box(
                    Modifier.size(15.dp).clip(CircleShape).background(PhoneColors.Accent.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) { Box(Modifier.size(9.dp).clip(CircleShape).background(PhoneColors.Accent)) }
            }
        }
        Column {
            Text(shown, color = tone.content, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, color = tone.content.copy(alpha = 0.7f), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** What a big symbol button shows: a symbol, or a short text drawn big (such as "87" or "%"). */
sealed interface Glyph {
    data class Symbol(val icon: ImageVector) : Glyph
    data class Big(val text: String) : Glyph
}

/**
 * A big symbol button (Icon / Number / Sign on the Battery page): green outline, tint and dot
 * when on, grey when off. Tapping it switches it.
 */
@Composable
fun BigSymbolButton(glyph: Glyph, label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val tint = if (selected) PhoneColors.Accent else PhoneColors.Dim
    Box(
        modifier
            .height(96.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) PhoneColors.AccentCard else PhoneColors.Card)
            .border(BorderStroke(1.5.dp, if (selected) PhoneColors.Accent else PhoneColors.Border), RoundedCornerShape(24.dp))
            .clickable(role = Role.Switch, onClick = onClick)
            .semantics(mergeDescendants = true) { this.selected = selected },
    ) {
        if (selected) Box(Modifier.align(Alignment.TopEnd).padding(top = 11.dp, end = 13.dp).size(10.dp).clip(CircleShape).background(PhoneColors.Accent))
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.height(36.dp), contentAlignment = Alignment.Center) {
                when (glyph) {
                    is Glyph.Symbol -> Icon(glyph.icon, null, tint = tint, modifier = Modifier.size(34.dp))
                    is Glyph.Big -> Text(glyph.text, color = tint, fontSize = 32.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(label, color = if (selected) PhoneColors.AccentSoftText else PhoneColors.Dim, fontSize = 12.sp, maxLines = 1)
        }
    }
}

/** A dark rounded box with a thin border, as behind the size row, the colour dots and the row groups. */
@Composable
fun PartCard(modifier: Modifier = Modifier, padding: Dp = 0.dp, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(PhoneColors.Card)
            .border(BorderStroke(1.5.dp, PhoneColors.Border), RoundedCornerShape(24.dp))
            .padding(padding),
    ) { content() }
}

/** Simple rows stacked in one card, with thin lines between them. */
@Composable
fun RowGroup(modifier: Modifier = Modifier, rows: List<@Composable () -> Unit>) {
    PartCard(modifier) {
        Column {
            rows.forEachIndexed { i, row ->
                if (i > 0) HorizontalDivider(thickness = 1.dp, color = PhoneColors.Divider)
                row()
            }
        }
    }
}

/**
 * A simple row: a symbol in a small coloured square, a title, an optional line under it, and
 * at the end whatever [end] draws (a switch, a value, a button). With [onClick] and nothing at
 * the end, an arrow shows that it opens a page.
 */
@Composable
fun SimpleRow(
    icon: ImageVector,
    title: String,
    tone: TileTone = TileTone.BLUE,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    end: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(tone.background), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tone.content, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = PhoneColors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) Text(subtitle, color = PhoneColors.Muted, fontSize = 13.sp)
        }
        Spacer(Modifier.width(10.dp))
        when {
            end != null -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { end() }
            onClick != null -> Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = PhoneColors.Muted)
        }
    }
}

/** A simple row whose end is a switch; tapping anywhere on the row switches it. */
@Composable
fun SwitchRow(icon: ImageVector, title: String, checked: Boolean, tone: TileTone = TileTone.BLUE, subtitle: String? = null, onChange: (Boolean) -> Unit) {
    SimpleRow(icon, title, tone, subtitle, onClick = { onChange(!checked) }) {
        PhoneSwitch(checked, onChange)
    }
}

/** The design's switch: green with a dark knob when on, grey with a white knob when off. */
@Composable
fun PhoneSwitch(checked: Boolean, onChange: ((Boolean) -> Unit)?) {
    Switch(
        checked, onChange,
        colors = SwitchDefaults.colors(
            checkedTrackColor = PhoneColors.Accent,
            checkedThumbColor = PhoneColors.OnAccent,
            checkedBorderColor = PhoneColors.Accent,
            uncheckedTrackColor = PhoneColors.SwitchOff,
            uncheckedThumbColor = Color.White,
            uncheckedBorderColor = PhoneColors.SwitchOff,
        ),
    )
}

/** A small pill button for a row's end: green for the main action, grey for the others. */
@Composable
fun PillButton(text: String, main: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (main) PhoneColors.Accent else PhoneColors.QuietButton)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(text, color = if (main) PhoneColors.OnAccent else PhoneColors.QuietButtonText, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** A row's value at its end, in grey, such as "3 changed". */
@Composable
fun RowValue(text: String) {
    Text(text, color = PhoneColors.Muted, fontSize = 14.sp, maxLines = 1)
}

/** The big green button across the bottom of a screen (Restart on the Customise picture). */
@Composable
fun WideButton(text: String, icon: ImageVector? = null, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(PhoneColors.Accent)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = PhoneColors.OnAccent); Spacer(Modifier.width(8.dp)) }
        Text(text, color = PhoneColors.OnAccent, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/**
 * A row of colour dots; the chosen one has a white ring. [names] are read out by TalkBack.
 * [endText] (such as "Default") sits at the right and is tappable when [onEnd] is given.
 */
@Composable
fun ColourDots(
    colours: List<Color>,
    names: List<String>,
    current: Color?,
    onSelect: (Color) -> Unit,
    endText: String? = null,
    onEnd: (() -> Unit)? = null,
) {
    val chosen = selectedColourIndex(colours, current)
    PartCard(padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // The dots take what the end text leaves, so "Default" is never squeezed.
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                colours.forEachIndexed { i, c ->
                    val ring = if (i == chosen) Modifier.border(3.dp, Color.White, CircleShape) else Modifier
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.RadioButton) { onSelect(c) }
                            .semantics { contentDescription = names.getOrElse(i) { "" }; selected = i == chosen }
                            .then(ring)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(c),
                    )
                }
            }
            if (endText != null) {
                Text(
                    endText,
                    Modifier.then(if (onEnd != null) Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onEnd) else Modifier).padding(6.dp),
                    color = if (chosen == null) PhoneColors.Text else PhoneColors.Muted, fontSize = 13.sp,
                    maxLines = 1, softWrap = false,
                )
            }
        }
    }
}

/** The "Saved" bar at the bottom of a page: a message, and an optional green action on the right. */
@Composable
fun SavedBar(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(PhoneColors.SavedBar)
            .padding(start = 20.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, Modifier.weight(1f), color = PhoneColors.SavedText, fontSize = 14.sp, maxLines = 2)
        if (action != null && onAction != null) {
            Box(
                Modifier.clip(RoundedCornerShape(20.dp)).background(PhoneColors.Accent).clickable(role = Role.Button, onClick = onAction)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) { Text(action, color = PhoneColors.OnAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

/** A hint line with a small symbol, as under the car picture ("Tap a part of the screen to change it"). */
@Composable
fun HintLine(icon: ImageVector, text: String) {
    Row(Modifier.padding(horizontal = 4.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = PhoneColors.Muted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = PhoneColors.Muted, fontSize = 13.5.sp)
    }
}
