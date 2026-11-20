package net.prototux.aare.ui.phone

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.ui.car.CarUiState
import net.prototux.aare.ui.car.WALLPAPER_NAMES
import net.prototux.aare.ui.car.Wallpaper

/** The wallpaper choices in the order the page shows them: none, the built-in ones, then the user's photo. */
fun wallpaperChoices(builtIn: List<String> = WALLPAPER_NAMES): List<String> = listOf("none") + builtIn + Settings.WALLPAPER_CUSTOM

/**
 * The car's wallpaper (plan E5): none, one of the built-in gradients, or a photo. Same setting
 * as the Car screen page. [savePhoto] copies a chosen photo into MyPlay's files (slow: it is run
 * off the main thread) and says whether that worked.
 */
@Composable
fun WallpaperPage(savePhoto: (Uri) -> Boolean) {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    var chosen by remember { mutableStateOf(settings.wallpaper) }
    var photoVersion by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val failed = stringResource(R.string.setup_wallpaper_failed)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            if (withContext(Dispatchers.IO) { savePhoto(uri) && WallpaperFiles.keepSource(ctx) }) {
                // Through "none", as the Car screen page does, so the car reloads the photo even if one was already set.
                settings.wallpaper = "none"; settings.wallpaper = Settings.WALLPAPER_CUSTOM
                settings.myplayWallpaperEdit = WallpaperEdit()
                chosen = Settings.WALLPAPER_CUSTOM
                photoVersion++
                // A new photo goes straight to the editor, to place it in the car screen's shape.
                editing = true
            } else {
                Toast.makeText(ctx, failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val photo by produceState<Bitmap?>(null, photoVersion) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                BitmapFactory.decodeFile(java.io.File(ctx.filesDir, Settings.WALLPAPER_FILE).path, opts)
            }.getOrNull()
        }
    }
    Column {
        PartPicture()
        SectionHeading(stringResource(R.string.setup_choice_wallpaper))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            wallpaperChoices().chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { name ->
                        WallpaperSwatch(name, photo, name == chosen, Modifier.weight(1f)) {
                            if (name == Settings.WALLPAPER_CUSTOM) picker.launch("image/*")
                            else { chosen = name; settings.wallpaper = name }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        if (chosen == Settings.WALLPAPER_CUSTOM) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(stringResource(R.string.phone_wallpaper_edit_button)) { editing = true }
                PillButton(stringResource(R.string.phone_wallpaper_other_photo), main = false) { picker.launch("image/*") }
            }
        }
    }
    if (editing) WallpaperEditorDialog { saved -> editing = false; if (saved) photoVersion++ }
}

@Composable
private fun WallpaperSwatch(name: String, photo: Bitmap?, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val label = when (name) {
        "none" -> stringResource(R.string.setup_wallpaper_none)
        Settings.WALLPAPER_CUSTOM -> stringResource(R.string.setup_wallpaper_photo)
        else -> name.replaceFirstChar(Char::uppercase)
    }
    Column(
        modifier.clip(shape).clickable(role = Role.RadioButton, onClick = onClick).semantics { this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(CarPictureSize.ASPECT)
                .clip(shape)
                .background(PhoneColors.Card)
                .border(BorderStroke(if (selected) 2.5.dp else 1.5.dp, if (selected) PhoneColors.Accent else PhoneColors.Border), shape),
            contentAlignment = Alignment.Center,
        ) {
            when {
                name == "none" -> Icon(Icons.Rounded.Block, null, tint = PhoneColors.Dim, modifier = Modifier.size(28.dp))
                name == Settings.WALLPAPER_CUSTOM && photo != null ->
                    Image(photo.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                name == Settings.WALLPAPER_CUSTOM -> Icon(Icons.Rounded.Image, null, tint = PhoneColors.Dim, modifier = Modifier.size(28.dp))
                // The car's own drawing of a built-in wallpaper.
                else -> Wallpaper(remember(name) { CarUiState().apply { wallpaper = name } }, Modifier.fillMaxSize())
            }
        }
        Text(label, color = if (selected) PhoneColors.Accent else PhoneColors.Muted, fontSize = 13.sp,
            modifier = Modifier.height(24.dp).fillMaxWidth().background(androidx.compose.ui.graphics.Color.Transparent),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
