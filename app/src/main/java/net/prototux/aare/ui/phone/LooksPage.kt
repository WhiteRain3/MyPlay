package net.prototux.aare.ui.phone

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.prototux.aare.DiagLog
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.SettingsBackup
import net.prototux.aare.ui.theme.CarThemes
import net.prototux.aare.ui.theme.CustomColours
import net.prototux.aare.ui.theme.forPhone
import net.prototux.aare.ui.theme.withAccent
import net.prototux.aare.ui.theme.withCustomColours

/** The name offered for a shared look's file. */
fun lookFileName(name: String): String =
    "myplay-look-" + name.lowercase().map { if (it.isLetterOrDigit()) it else '-' }.joinToString("").trim('-').ifEmpty { "mine" } + ".json"

/** A look file's name without its ending, offered as the look's name when the file has none. */
fun nameFromFile(file: String?): String = file.orEmpty().substringAfterLast('/').substringBeforeLast('.').removePrefix("myplay-look-")

/** The look's own colours that read badly over its theme, by day or by night (see [hardToRead]). */
fun lookHardToRead(look: Look, ctx: Context): Boolean {
    val v = look.values
    val own = CustomColours.decode(v["myplay_look_colours"] as? String)
    if (own.isEmpty) return false
    val spec = CarThemes.byId(v["style_theme"] as? String ?: "aare").forPhone(ctx)
        .withAccent(v["style_accent"] as? String ?: "theme", ctx).withCustomColours(own)
    return listOf(true, false).any { night ->
        val scheme = if (night) spec.night else spec.day
        hardToRead({ roleColour(scheme, it) }, own.side(night).keys).isNotEmpty()
    }
}

/** Three night colours of [look] as the car would paint them: background, card, accent. */
internal fun lookDots(look: Look, ctx: Context): List<Color> {
    val v = look.values
    val scheme = CarThemes.byId(v["style_theme"] as? String ?: "aare").forPhone(ctx)
        .withAccent(v["style_accent"] as? String ?: "theme", ctx)
        .withCustomColours(CustomColours.decode(v["myplay_look_colours"] as? String)).night
    return listOf(scheme.background, scheme.surfaceContainer, scheme.primary)
}

