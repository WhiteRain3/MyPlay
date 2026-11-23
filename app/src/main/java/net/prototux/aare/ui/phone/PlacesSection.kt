package net.prototux.aare.ui.phone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.LocalParking
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
import net.prototux.aare.car.myplayParkedHere
import net.prototux.aare.car.myplayPlaces

internal const val PLACE_ADD_TAG = "phone_place_add"
internal fun placeNameTag(i: Int) = "phone_place_name_$i"
internal fun placeAddressTag(i: Int) = "phone_place_address_$i"
internal fun placeDeleteTag(i: Int) = "phone_place_delete_$i"

