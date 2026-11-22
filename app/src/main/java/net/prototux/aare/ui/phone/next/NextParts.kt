package net.prototux.aare.ui.phone.next

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.prototux.aare.R

/*
 * The rebuilt phone app's parts (plan §16 chunk 1, design frame 77). A row has one shape: an icon disc,
 * a label, at most one help line (or a warning line), and a value or control on the right. A chevron
 * opens a page; ↗ leaves the tab or the app. Changed from default is a hollow ring and a brighter value.
 */

private val CardShape = RoundedCornerShape(NextSpace.CardRadius)

/** A card of rows, divided by hairlines that start after the icon disc ([inset] 70 dp) or at 16 dp. */
@Composable
fun NextCard(modifier: Modifier = Modifier, highlight: Boolean = false, warn: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val border = when {
        warn -> NextColors.Warn.copy(alpha = 0.45f)
        highlight -> NextColors.Int.copy(alpha = 0.28f)
        else -> NextColors.Edge
    }
    Column(
        modifier.fillMaxWidth().clip(CardShape).background(NextColors.Surface).border(1.dp, border, CardShape),
        content = content,
    )
}

/** The hairline between two rows of a card. */
@Composable
fun NextDivider(inset: Dp = 70.dp) {
    Box(Modifier.fillMaxWidth().padding(start = inset).height(1.dp).background(NextColors.Edge))
}

/** Rows in a card with hairlines between them; each row is drawn by its own lambda. */
@Composable
fun NextGroup(rows: List<@Composable () -> Unit>, modifier: Modifier = Modifier, inset: Dp = 70.dp) {
    NextCard(modifier) {
        rows.forEachIndexed { i, row ->
            if (i > 0) NextDivider(inset)
            row()
        }
    }
}

enum class DiscTone { PLAIN, WORLD, WARN }

/** The 40 dp icon disc at the start of a row. */
@Composable
fun NextIconDisc(icon: ImageVector, tone: DiscTone = DiscTone.PLAIN, world: Color = NextColors.Int) {
    val (bg, fg) = when (tone) {
        DiscTone.PLAIN -> NextColors.Surface2 to NextColors.Text2
        DiscTone.WORLD -> world.soft() to world
        DiscTone.WARN -> NextColors.Warn.copy(alpha = 0.15f) to NextColors.Warn
    }
    Box(Modifier.size(NextSpace.IconDisc).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
    }
}

/** "Changed from default": a hollow ring (filled dots mean "needs attention" only). */
@Composable
fun NextChangedMark(modifier: Modifier = Modifier) {
    Box(modifier.size(8.dp).border(1.5.dp, NextColors.Text2, CircleShape))
}

enum class TagKind(val bg: Color, val fg: Color) {
    CAUTION(NextColors.Caution.soft(), NextColors.Caution),
    OK(NextColors.Ok.soft(), NextColors.Ok),
    MUTE(NextColors.Surface2, NextColors.Text2),
    INT(NextColors.Int.soft(), NextColors.Int),
    WARN(NextColors.Warn.copy(alpha = 0.15f), NextColors.Warn),
}

/** A small rounded tag: "Next connection", "Also on the car", "Allowed", "Ready"… */
@Composable
fun NextTag(text: String, kind: TagKind = TagKind.MUTE, icon: ImageVector? = null) {
    Row(
        Modifier.height(22.dp).clip(RoundedCornerShape(50)).background(kind.bg).padding(start = if (icon != null) 6.dp else 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = kind.fg, modifier = Modifier.size(12.dp))
        Text(text, style = NextType.Tag, color = kind.fg, maxLines = 1)
    }
}

/** "From the next connection", always in this one form (plan §9.1 rule 5). */
@Composable
fun NextConnectionTag() = NextTag(stringResource(R.string.next_tag_next_connection_label), TagKind.MUTE, Icons.Rounded.Refresh)

/** "Also on the car": the car-side screens write this setting too (plan §8). */
@Composable
fun AlsoOnCarTag() = NextTag(stringResource(R.string.next_tag_also_on_car_label), TagKind.MUTE)

/** The one warning line of a risky switch (plan §9.1 rule 6), in the warning colour. */
@Composable
fun NextWarnLine(text: String) {
    Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Rounded.WarningAmber, null, tint = NextColors.Warn, modifier = Modifier.size(14.dp))
        Text(text, style = NextType.Help, color = NextColors.Warn)
    }
}

/** What a row shows under its label: a help line, or a warning line, never both (plan §9.1 rule 2). */
sealed interface RowNote {
    data class Help(val text: String) : RowNote
    data class Warn(val text: String) : RowNote
}

/**
 * The ⓘ beside a label: a 48 dp target that opens a short sheet (plan §9.1 rule 3). TalkBack reads
 * "About {label}".
 */
@Composable
fun NextInfoButton(label: String, info: String) {
    var open by rememberSaveable { mutableStateOf(false) }
    val name = stringResource(R.string.next_about_label, label)
    // 24 dp drawn so the row keeps its height; Compose widens the touch area to 48 dp.
    Box(
        Modifier.size(24.dp).clip(CircleShape)
            .clickable(role = Role.Button) { open = true }
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Outlined.Info, null, tint = NextColors.Text3, modifier = Modifier.size(16.dp)) }
    if (open) NextSheet(title = label, onDismiss = { open = false }) {
        Text(info, style = NextType.Label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight(400)), color = NextColors.Text2)
    }
}

/**
 * The one row shape. [trailing] is the value or control; [onClick] makes the whole row a target.
 * [info] adds ⓘ beside the label; [tag] a tag after it.
 */
@Composable
fun NextRow(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    disc: DiscTone = DiscTone.PLAIN,
    note: RowNote? = null,
    info: String? = null,
    tag: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    role: Role? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val tall = note != null
    Row(
        modifier.fillMaxWidth().heightIn(min = if (tall) NextSpace.RowTall else NextSpace.RowMin)
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, role = role, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (icon != null) NextIconDisc(icon, disc)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Disabled rows stay readable (review finding 8): only the control shows it.
                Text(label, style = NextType.Label, color = NextColors.Text, modifier = Modifier.weight(1f, fill = false))
                if (info != null) Box(Modifier.offset(x = (-4).dp)) { NextInfoButton(label, info) }
                tag?.invoke()
            }
            when (note) {
                is RowNote.Help -> Text(note.text, style = NextType.Help, color = NextColors.Text3, modifier = Modifier.padding(top = 2.dp))
                is RowNote.Warn -> NextWarnLine(note.text)
                null -> Unit
            }
        }
        trailing?.invoke(this)
    }
}

/** A row's value on the right: brighter, with the hollow ring, when changed from default. */
@Composable
fun NextValue(value: String, changed: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (changed) NextChangedMark()
        Text(
            value, style = if (changed) NextType.Value.copy(fontWeight = androidx.compose.ui.text.font.FontWeight(560)) else NextType.Value,
            color = if (changed) NextColors.Text else NextColors.Text2, maxLines = 1,
        )
    }
}

/** A row that opens a page of its own tab: value and chevron. */
@Composable
fun NextValueRow(
    label: String,
    value: String? = null,
    icon: ImageVector? = null,
    changed: Boolean = false,
    note: RowNote? = null,
    info: String? = null,
    tag: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val changedWord = stringResource(R.string.next_changed_label)
    NextRow(
        label, icon = icon, note = note, info = info, tag = tag, onClick = onClick, role = Role.Button,
        modifier = if (changed) Modifier.semantics { stateDescription = changedWord } else Modifier,
    ) {
        if (value != null) NextValue(value, changed) else if (changed) NextChangedMark()
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = NextColors.Text3, modifier = Modifier.size(20.dp))
    }
}

/** A row that leaves the tab or the app: ↗ instead of a chevron. */
@Composable
fun NextLinkRow(label: String, value: String? = null, icon: ImageVector? = null, note: RowNote? = null, onClick: () -> Unit) {
    NextRow(label, icon = icon, note = note, onClick = onClick, role = Role.Button) {
        if (value != null) NextValue(value)
        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = NextColors.Text3, modifier = Modifier.size(18.dp))
    }
}

/** The switch (52 × 32 in a 48 dp target): outlined when off, [NextColors.Int] when on, dashed when disabled. */
@Composable
fun NextSwitch(checked: Boolean, enabled: Boolean = true) {
    val track by animateColorAsState(if (checked && enabled) NextColors.Int else NextColors.Surface2, tween(150), label = "track")
    val thumbX by animateDpAsState(if (checked) 22.dp else 6.dp, tween(150), label = "thumb")
    val thumb by animateDpAsState(if (checked) 24.dp else 16.dp, tween(150), label = "thumbSize")
    Box(
        Modifier.size(52.dp, 32.dp).clip(RoundedCornerShape(16.dp)).background(if (enabled) track else Color.Transparent)
            .drawBehind {
                val w = 2.dp.toPx()
                val r = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
                val color = when {
                    !enabled -> NextColors.Disabled
                    checked -> NextColors.Int
                    else -> NextColors.Outline
                }
                drawRoundRect(
                    color, topLeft = Offset(w / 2, w / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w), cornerRadius = r,
                    style = Stroke(w, pathEffect = if (enabled) null else PathEffect.dashPathEffect(floatArrayOf(6f, 4f))),
                )
            },
    ) {
        val thumbColor = when {
            !enabled -> NextColors.Disabled
            checked -> NextColors.On
            else -> NextColors.Text2
        }
        Box(
            Modifier.offset(x = thumbX, y = (32.dp - thumb) / 2).size(thumb).clip(CircleShape).background(thumbColor),
        )
    }
}

/** A setting that is on or off. The whole row toggles, with a light haptic (plan §11). */
@Composable
fun NextSwitchRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    note: RowNote? = null,
    info: String? = null,
    tag: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    changed: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    val changedWord = stringResource(R.string.next_changed_label)
    NextRow(
        label, icon = icon, note = note, info = info, tag = tag, enabled = enabled,
        modifier = Modifier.toggleable(checked, enabled = enabled, role = Role.Switch) {
            haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
            onChange(it)
        }.then(if (changed) Modifier.semantics { stateDescription = changedWord } else Modifier),
    ) {
        if (changed) NextChangedMark()
        NextSwitch(checked, enabled)
    }
}

/** One choice of a picker or segmented control: the value stored and what the owner reads. */
data class Choice<T>(val value: T, val label: String)

/**
 * A setting with several values: the row shows the current one; tapping opens a sheet of the real
 * values, the current one checked (design frames 30, 64). [default] drives the changed mark.
 */
@Composable
fun <T> NextPickerRow(
    label: String,
    choices: List<Choice<T>>,
    selected: T,
    onPick: (T) -> Unit,
    default: T? = null,
    icon: ImageVector? = null,
    note: RowNote? = null,
    info: String? = null,
    tag: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val current = choices.firstOrNull { it.value == selected }?.label ?: selected.toString()
    val changed = default != null && selected != default
    val changedWord = stringResource(R.string.next_changed_label)
    NextRow(
        label, icon = icon, note = note, info = info, tag = tag, enabled = enabled, onClick = { open = true }, role = Role.Button,
        modifier = if (changed) Modifier.semantics { stateDescription = changedWord } else Modifier,
    ) {
        NextValue(current, changed)
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = NextColors.Text3, modifier = Modifier.size(20.dp))
    }
    if (open) NextSheet(title = label, onDismiss = { open = false }) {
        NextCard(Modifier.padding(top = 4.dp)) {
            choices.forEachIndexed { i, c ->
                if (i > 0) NextDivider(16.dp)
                val on = c.value == selected
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        .selectable(on, role = Role.RadioButton) { onPick(c.value); open = false }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(c.label, style = NextType.Label, color = NextColors.Text, modifier = Modifier.weight(1f))
                    if (on) Icon(Icons.Rounded.Check, null, tint = NextColors.Int)
                }
            }
        }
    }
}

/** Three-step choices (design frame 18): a pill with the chosen part filled. */
@Composable
fun <T> NextSegmented(choices: List<Choice<T>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().height(NextSpace.Target).clip(RoundedCornerShape(50)).background(NextColors.Surface2).padding(4.dp),
    ) {
        for (c in choices) {
            val on = c.value == selected
            Box(
                Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(50))
                    .background(if (on) NextColors.Int else Color.Transparent)
                    .selectable(on, role = Role.RadioButton) { onSelect(c.value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    c.label, style = NextType.Value.copy(fontWeight = androidx.compose.ui.text.font.FontWeight(if (on) 650 else 560)),
                    color = if (on) NextColors.On else NextColors.Text2, maxLines = 1,
                )
            }
        }
    }
}

enum class ButtonKind { PRIMARY, GHOST, TEXT, DANGER }

/** A button: one primary per screen, others ghost or text (design frame 77). */
@Composable
fun NextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.PRIMARY,
    icon: ImageVector? = null,
    small: Boolean = false,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(50)
    val (bg, fg) = when {
        !enabled && kind == ButtonKind.PRIMARY -> NextColors.Surface2 to NextColors.Text3
        kind == ButtonKind.PRIMARY -> NextColors.Int to NextColors.On
        kind == ButtonKind.GHOST -> Color.Transparent to NextColors.Text
        kind == ButtonKind.DANGER -> Color.Transparent to NextColors.Warn
        else -> Color.Transparent to NextColors.Int
    }
    Row(
        modifier.heightIn(min = if (small) NextSpace.ButtonSmall else NextSpace.Button).minimumInteractiveComponentSize()
            .clip(shape).background(bg)
            .then(if (kind == ButtonKind.GHOST) Modifier.border(1.5.dp, NextColors.Edge2, shape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = if (kind == ButtonKind.TEXT || kind == ButtonKind.DANGER) 12.dp else if (small) 16.dp else 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
        Text(text, style = if (small) NextType.Button.copy(fontSize = NextType.Value.fontSize) else NextType.Button, color = fg, maxLines = 1)
    }
}

/** A section heading inside a page: 20 dp above, 8 below. */
@Composable
fun NextSection(text: String, first: Boolean = false, extra: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = if (first) 4.dp else 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text, style = NextType.Section, color = NextColors.Text2, modifier = Modifier.semantics { heading() })
        extra?.invoke()
    }
}

/** A short line under a card (≤ 60 characters), with an optional icon. */
@Composable
fun NextNote(text: String, icon: ImageVector? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = NextColors.Text3, modifier = Modifier.size(16.dp))
        Text(text, style = NextType.Help, color = NextColors.Text3)
    }
}

/** A check box (design frame 59): outlined, filled with [NextColors.Int] when ticked. */
@Composable
fun NextCheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, note: RowNote? = null) {
    NextRow(label, note = note, modifier = Modifier.toggleable(checked, role = Role.Checkbox, onValueChange = onChange)) {
        Box(
            Modifier.size(24.dp).clip(RoundedCornerShape(6.dp))
                .background(if (checked) NextColors.Int else Color.Transparent)
                .border(2.dp, if (checked) NextColors.Int else NextColors.Outline, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) { if (checked) Icon(Icons.Rounded.Check, null, tint = NextColors.On, modifier = Modifier.size(18.dp)) }
    }
}

/**
 * A colour disc to choose (accents, presets). Colour alone says nothing to TalkBack, so each swatch
 * carries its [name] (review "Second pass" item 6).
 */
@Composable
fun NextSwatch(color: Color, name: String, selected: Boolean, onClick: () -> Unit, caption: String? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(NextSpace.Target)
                .then(if (selected) Modifier.border(2.dp, NextColors.Int, CircleShape).padding(5.dp) else Modifier)
                .clip(CircleShape).background(color)
                .selectable(selected, role = Role.RadioButton, onClick = onClick)
                .semantics { contentDescription = name },
        )
        if (caption != null) Text(caption, style = NextType.Help.copy(fontSize = NextType.TabLabel.fontSize), color = NextColors.Text2,
            modifier = Modifier.padding(top = 8.dp).clearAndSetSemantics {})
    }
}

/** The empty state of a list (design frame 52): faint rings and one line. */
@Composable
fun NextEmpty(text: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(120.dp)) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            listOf(0.48f to 0.14f, 0.34f to 0.22f, 0.2f to 0.3f).forEach { (r, a) ->
                drawCircle(NextColors.Text3.copy(alpha = a), radius = size.minDimension * r, style = Stroke(2.dp.toPx(), pathEffect = dash))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(text, style = NextType.Label, color = NextColors.Text2, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
    }
}

/** A bottom sheet in the design's shape: rises, scrim behind, closes on Back or a drag down. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NextSheet(title: String, onDismiss: () -> Unit, sub: String? = null, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = NextSpace.SheetRadius, topEnd = NextSpace.SheetRadius),
        containerColor = NextColors.Sheet,
        contentColor = NextColors.Text,
        scrimColor = NextColors.Scrim,
        dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 18.dp).size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF3A4652))) },
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp).navigationBarsPadding()) {
            Text(title, style = NextType.SheetTitle, color = NextColors.Text, modifier = Modifier.semantics { heading() })
            if (sub != null) Text(sub, style = NextType.Value, color = NextColors.Text2, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

/**
 * The confirm for what can't be undone (plan §10: load backup, remove certificate, replace vehicles.json).
 * The safe choice is never smaller than the risky one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NextConfirm(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit, danger: Boolean = false) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.clip(RoundedCornerShape(NextSpace.SheetRadius)).background(NextColors.Sheet)
                .border(1.dp, NextColors.Edge2, RoundedCornerShape(NextSpace.SheetRadius)).padding(24.dp),
        ) {
            Text(title, style = NextType.SheetTitle.copy(fontSize = NextType.Hero.fontSize.times(0.875f)), color = NextColors.Text,
                modifier = Modifier.semantics { heading() })
            Text(text, style = NextType.Value.copy(lineHeight = NextType.Label.lineHeight), color = NextColors.Text2, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NextButton(stringResource(android.R.string.cancel), onDismiss, Modifier.weight(1f), ButtonKind.GHOST)
                NextButton(confirm, onConfirm, Modifier.weight(1f), if (danger) ButtonKind.GHOST else ButtonKind.PRIMARY)
            }
        }
    }
}

/** Shows an undo snackbar: [message], and [undo] if the owner taps Undo in time (plan §10). */
fun interface NextUndo {
    fun show(message: String, undo: () -> Unit)
}

val LocalNextUndo = staticCompositionLocalOf { NextUndo { _, _ -> } }

/** The snackbar host the shell places above the tab bar; [undoLabel] is "Undo". */
@Composable
fun rememberNextUndo(host: SnackbarHostState, undoLabel: String): NextUndo {
    val scope = rememberCoroutineScope()
    return remember(host, undoLabel) {
        NextUndo { message, undo ->
            scope.launch {
                host.currentSnackbarData?.dismiss()
                if (host.showSnackbar(message, undoLabel, duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) undo()
            }
        }
    }
}

/** The undo snackbar's look (design frame 51): light, with the action in bold. */
@Composable
fun NextSnackbarHost(host: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(host, modifier) { data ->
        Row(
            Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(NextColors.Snack)
                .padding(start = 18.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(data.visuals.message, style = NextType.Value.copy(fontWeight = androidx.compose.ui.text.font.FontWeight(520)),
                color = NextColors.On, modifier = Modifier.weight(1f))
            data.visuals.actionLabel?.let { action ->
                Box(
                    Modifier.minimumInteractiveComponentSize().clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button) { data.performAction() }.padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(action, style = NextType.Button.copy(fontSize = NextType.Value.fontSize), color = NextColors.SnackAction) }
            }
        }
    }
}

/**
 * "Reset this page" at the bottom of each Screen and Car page (plan §11). [reset] puts the page's
 * settings back to their defaults and returns how to put them back; the snackbar offers that.
 */
@Composable
fun NextResetPage(reset: () -> (() -> Unit), enabled: Boolean = true) {
    val undo = LocalNextUndo.current
    val message = stringResource(R.string.next_reset_done_label)
    Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
        NextButton(
            stringResource(R.string.next_reset_page_label), onClick = { val back = reset(); undo.show(message, back) },
            kind = ButtonKind.TEXT, icon = Icons.Rounded.RestartAlt, enabled = enabled,
        )
    }
}

/** The MyPlay mark: a ring with a planet on it (design frame 01). */
@Composable
fun NextMark(size: Dp = 28.dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension / 32f
        drawCircle(NextTab.HOME.world, radius = 11f * s, style = Stroke(3f * s))
        drawCircle(NextColors.Text, radius = 4.5f * s)
        drawCircle(NextColors.Bg, radius = 3.6f * s + 2f * s, center = Offset(25f * s, 9f * s))
        drawCircle(NextTab.SCREEN.world, radius = 3.6f * s, center = Offset(25f * s, 9f * s))
    }
}
