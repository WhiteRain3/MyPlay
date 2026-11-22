package net.prototux.aare.ui.phone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.car.Places
import net.prototux.aare.car.SavedPlace
import net.prototux.aare.car.myplayPlaces

/**
 * MyPlay (#5): up to five places of the owner's own, beside Home address, for ORBIT's Places list
 * (a home pad shortcut). Saved as typed (an entry with no address is not kept); never backed up.
 */
@Composable
internal fun PlacesSection(settings: Settings) {
    // Rows being edited, blank ones included; the setting keeps only the ones with an address.
    var rows by remember { mutableStateOf(settings.myplayPlaces) }
    fun save(new: List<SavedPlace>) { rows = new; settings.myplayPlaces = new }
    val colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PhoneColors.Accent, unfocusedBorderColor = PhoneColors.Border)
    Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.phone_places_title), color = PhoneColors.Text, fontSize = 16.sp)
        Text(stringResource(R.string.phone_places_hint), color = PhoneColors.Muted, fontSize = 13.sp)
        rows.forEachIndexed { i, p ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(p.name, { v -> save(rows.toMutableList().also { it[i] = p.copy(name = v.take(Places.NAME_MAX)) }) },
                        singleLine = true, colors = colors, label = { Text(stringResource(R.string.phone_places_name)) },
                        modifier = Modifier.fillMaxWidth().testTag(placeNameTag(i)))
                    OutlinedTextField(p.address, { v -> save(rows.toMutableList().also { it[i] = p.copy(address = v.take(Places.ADDRESS_MAX)) }) },
                        singleLine = true, colors = colors, label = { Text(stringResource(R.string.phone_home_address_hint)) },
                        modifier = Modifier.fillMaxWidth().testTag(placeAddressTag(i)))
                }
                IconButton({ save(rows.toMutableList().also { it.removeAt(i) }) }, Modifier.testTag(placeDeleteTag(i))) {
                    Icon(Icons.Rounded.Delete, stringResource(R.string.phone_places_delete), tint = PhoneColors.Muted)
                }
            }
        }
        if (rows.size < Places.MAX) {
            TextButton({ rows = rows + SavedPlace("", "") }, Modifier.testTag(PLACE_ADD_TAG)) {
                Text(stringResource(R.string.phone_places_add), color = PhoneColors.Accent)
            }
        }
    }
}

internal const val PLACE_ADD_TAG = "phone_place_add"
internal fun placeNameTag(i: Int) = "phone_place_name_$i"
internal fun placeAddressTag(i: Int) = "phone_place_address_$i"
internal fun placeDeleteTag(i: Int) = "phone_place_delete_$i"
