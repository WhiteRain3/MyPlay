package net.prototux.aare.ui.phone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Battery5Bar
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.prototux.aare.R

/**
 * Every shared part on one page, with sample values, for screenshots and the Design reviewer.
 * Changes nothing: every switch and button here only changes this page. Opened with
 * `adb shell am start -n io.github.whiterain3.myplay/net.prototux.aare.ui.MainActivity --ez phone_parts true`.
 */
@Composable
fun PhonePartsSample(onBack: () -> Unit) {
    var icon by remember { mutableStateOf(true) }
    var number by remember { mutableStateOf(true) }
    var sign by remember { mutableStateOf(false) }
    var check by remember { mutableStateOf(true) }
    var colour by remember { mutableStateOf<Color?>(SAMPLE_COLOURS[1]) }
    var saved by remember { mutableStateOf(false) }
    LazyColumn(
        Modifier.fillMaxSize().systemBarsPadding(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    ) {
        item { PhoneTopBar(stringResource(R.string.phone_parts_title), onBack = onBack, onHelp = {}) }
        item { HintLine(Icons.Rounded.TouchApp, stringResource(R.string.phone_parts_hint)) }
        item { SectionHeading(stringResource(R.string.phone_parts_tiles)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SymbolTile(Icons.Rounded.SystemUpdate, "0.1.2", stringResource(R.string.phone_parts_sample_updates), TileTone.BLUE, Modifier.weight(1f), attention = true) { saved = true }
                SymbolTile(Icons.Rounded.Backup, stringResource(R.string.phone_parts_sample_saved), stringResource(R.string.phone_parts_sample_backup), TileTone.GREEN, Modifier.weight(1f)) { saved = true }
                SymbolTile(Icons.Rounded.Bluetooth, stringResource(R.string.phone_parts_sample_waiting), stringResource(R.string.phone_parts_sample_connection), TileTone.CORAL, Modifier.weight(1f)) { saved = true }
            }
        }
        item { Spacer(Modifier.height(10.dp)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SymbolTile(Icons.Rounded.Flag, "3", stringResource(R.string.phone_parts_sample_flags), TileTone.AMBER, Modifier.weight(1f)) { saved = true }
                SymbolTile(Icons.Rounded.Tune, stringResource(R.string.phone_parts_sample_long), stringResource(R.string.phone_parts_sample_quick), TileTone.VIOLET, Modifier.weight(1f)) { saved = true }
                SymbolTile(Icons.Rounded.Refresh, null, stringResource(R.string.phone_parts_sample_system), TileTone.PINK, Modifier.weight(1f)) { saved = true }
            }
        }
        item { SectionHeading(stringResource(R.string.phone_parts_buttons)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigSymbolButton(Glyph.Symbol(Icons.Rounded.Battery5Bar), stringResource(R.string.phone_parts_sample_icon), icon, Modifier.weight(1f)) { icon = !icon; saved = true }
                BigSymbolButton(Glyph.Big("87"), stringResource(R.string.phone_parts_sample_number), number, Modifier.weight(1f)) { number = !number; saved = true }
                BigSymbolButton(Glyph.Big("%"), stringResource(R.string.phone_parts_sample_sign), sign, Modifier.weight(1f)) { sign = !sign; saved = true }
            }
        }
        item { SectionHeading(stringResource(R.string.phone_parts_colour)) }
        item {
            ColourDots(
                SAMPLE_COLOURS, SAMPLE_COLOURS.indices.map { "${it + 1}" }, colour,
                onSelect = { colour = it; saved = true },
                endText = stringResource(R.string.phone_parts_default), onEnd = { colour = null; saved = true },
            )
        }
        item { SectionHeading(stringResource(R.string.phone_parts_rows), end = { PhoneSwitch(check) { check = it } }) }
        item {
            RowGroup(rows = listOf(
                { SimpleRow(Icons.Rounded.SystemUpdate, stringResource(R.string.phone_parts_sample_update), TileTone.BLUE, stringResource(R.string.phone_parts_sample_ready)) { PillButton(stringResource(R.string.phone_parts_sample_install)) { saved = true } } },
                { SwitchRow(Icons.Rounded.Search, stringResource(R.string.phone_parts_sample_check), check) { check = it; saved = true } },
                { SimpleRow(Icons.Rounded.Backup, stringResource(R.string.phone_parts_sample_look), TileTone.GREEN, stringResource(R.string.phone_parts_sample_look_when)) { PillButton(stringResource(R.string.phone_parts_sample_save), main = false) { saved = true } } },
                { SimpleRow(Icons.Rounded.Flag, stringResource(R.string.phone_parts_sample_flags), TileTone.AMBER, stringResource(R.string.phone_parts_sample_changed), onClick = { saved = true }) },
                { SimpleRow(Icons.Rounded.Tune, stringResource(R.string.phone_parts_sample_quick), TileTone.VIOLET, onClick = { saved = true }) { RowValue(stringResource(R.string.phone_parts_sample_two_on)) } },
            ))
        }
        item { Spacer(Modifier.height(20.dp)) }
        item { WideButton(stringResource(R.string.phone_parts_sample_wide), Icons.Rounded.Refresh) { saved = true } }
        item { Spacer(Modifier.height(16.dp)) }
        item {
            Box {
                if (saved) SavedBar(stringResource(R.string.phone_parts_saved), action = stringResource(R.string.phone_parts_undo)) { saved = false }
                else SavedBar(stringResource(R.string.phone_parts_saved_plain))
            }
        }
    }
}

private val SAMPLE_COLOURS = listOf(
    Color.White, PhoneColors.Accent, Color(0xFF4CC9F0), PhoneColors.Warning, PhoneColors.Error, Color(0xFFC7B0FF),
)
