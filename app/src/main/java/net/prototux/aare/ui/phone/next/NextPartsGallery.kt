package net.prototux.aare.ui.phone.next

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Every part of the rebuilt app on one page, for checking against the style sheet (frame 77) and for
 * the accessibility test. Sample words only; it stores nothing.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NextPartsGallery() {
    var lights by rememberSaveable { mutableStateOf(true) }
    var fps by rememberSaveable { mutableStateOf(false) }
    var passenger by rememberSaveable { mutableStateOf(false) }
    var margin by rememberSaveable { mutableIntStateOf(5) }
    var theme by rememberSaveable { mutableStateOf("car") }
    var accent by rememberSaveable { mutableIntStateOf(0) }
    var alerts by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        Modifier.fillMaxSize().testTag("next-gallery"),
        contentPadding = PaddingValues(horizontal = NextSpace.Gutter, vertical = 8.dp),
    ) {
        item { NextSection("Rows", first = true) }
        item {
            NextGroup(listOf(
                { NextValueRow("Colours", "AARE · own colours", Icons.Outlined.Palette, changed = true) {} },
                { NextSwitchRow("Follow the car's lights", lights, { lights = it }, Icons.Outlined.LightMode) },
                { NextPickerRow("Warn when over by", listOf(Choice(0, "Off"), Choice(5, "5 km/h"), Choice(9, "9 km/h"), Choice(15, "15 km/h")),
                    margin, { margin = it }, default = 0, icon = Icons.Outlined.Speed, tag = { AlsoOnCarTag() },
                    note = RowNote.Help("Car first, then phone GPS")) },
                { NextSwitchRow("Prefer 60 fps", fps, { fps = it }, tag = { NextConnectionTag() }) },
                { NextSwitchRow("Passenger mode", passenger, { passenger = it }, Icons.Outlined.People,
                    note = RowNote.Warn("The driver must not use the screen"), info = "Lets the car screen work while driving. Only for a passenger.") },
                { NextSwitchRow("Phone apps", false, {}, Icons.Outlined.DirectionsCar, enabled = false,
                    note = RowNote.Help("Needs root or Shizuku")) },
                { NextLinkRow("Project page", icon = Icons.Outlined.Language) {} },
            ))
        }
        item { NextSection("Segmented") }
        item {
            NextSegmented(listOf(Choice("car", "Car's"), Choice("dark", "Dark"), Choice("light", "Light")), theme, { theme = it })
        }
        item { NextSection("Buttons") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NextButton("Install", {})
                NextButton("Disconnect", {}, kind = ButtonKind.GHOST)
            }
        }
        item { NextResetPage({ {} }) }
        item { NextSection("Tags") }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NextConnectionTag(); AlsoOnCarTag(); NextTag("In use", TagKind.INT); NextTag("Allowed", TagKind.OK); NextTag("Ready", TagKind.CAUTION)
            }
        }
        item { NextSection("Swatches") }
        item {
            val accents = listOf("Theme's" to Color(0xFF9DB4E8), "Amber" to Color(0xFFFFC24A), "Red" to Color(0xFFFF6B6B), "Green" to Color(0xFF6EE7A8))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                accents.forEachIndexed { i, (name, c) -> NextSwatch(c, name, accent == i, { accent = i }, caption = name) }
            }
        }
        item { NextSection("Check box") }
        item { NextCard { NextCheckRow("Include alert texts", alerts, { alerts = it }, RowNote.Help("May contain street names")) } }
        item { NextSection("Empty") }
        item { NextEmpty("Cars appear here after the first connection.") }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
