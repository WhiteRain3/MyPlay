package net.prototux.aare.ui.phone

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.prototux.aare.R
import net.prototux.aare.Settings
import java.io.File

/** The wallpaper's files: the untouched photo kept for editing again, and the one the car shows. */
object WallpaperFiles {
    const val SOURCE = "myplay_wallpaper_source.jpg"
    /** The size the car's wallpaper is written at: the Audi's shape, sharp on a larger screen too. */
    const val OUT_W = 1600
    const val OUT_H = 964

    fun source(context: Context) = File(context.filesDir, SOURCE)
    fun shown(context: Context) = File(context.filesDir, Settings.WALLPAPER_FILE)

    /** Keeps the photo just set as the wallpaper as the untouched copy to edit from. */
    fun keepSource(context: Context): Boolean = keepSource(shown(context), source(context))

    internal fun keepSource(shown: File, source: File): Boolean = runCatching { shown.copyTo(source, overwrite = true); true }.getOrDefault(false)

    /**
     * The file to edit from: the untouched copy. A photo set before the editor existed has no copy
     * yet: the shown wallpaper becomes the copy now, once, so later edits start from it and not
     * from their own results (darkening and blur never pile up). Null when there is no photo.
     */
    internal fun sourceFile(shown: File, source: File): File? {
        if (!source.isFile && shown.isFile) keepSource(shown, source)
        return source.takeIf { it.isFile }
    }

    fun loadSource(context: Context): Bitmap? {
        val f = sourceFile(shown(context), source(context)) ?: return null
        return runCatching { BitmapFactory.decodeFile(f.path) }.getOrNull()
    }

    /** [source] under [edit], at [w] x [h]: placed, blurred, then darkened. */
    fun render(source: Bitmap, edit: WallpaperEdit, w: Int, h: Int): Bitmap {
        val r = sourceRect(source.width, source.height, w.toFloat() / h, edit)
        var out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(
            source, Rect(r.left.toInt(), r.top.toInt(), (r.left + r.width).toInt(), (r.top + r.height).toInt()),
            Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG),
        )
        val radius = blurRadius(edit.blur, w)
        if (radius > 0f) {
            val px = IntArray(w * h).also { out.getPixels(it, 0, w, 0, 0, w, h) }
            out = Bitmap.createBitmap(ImageBlur.blur(px, w, h, radius), w, h, Bitmap.Config.ARGB_8888)
        }
        if (edit.dimPercent > 0) {
            val c = if (out.isMutable) out else out.copy(Bitmap.Config.ARGB_8888, true).also { out = it }
            Canvas(c).drawColor(android.graphics.Color.argb(edit.dimPercent * 255 / 100, 0, 0, 0))
        }
        return out
    }

    /** Writes [bitmap] as the car's wallpaper: to a temporary file, renamed only once complete. */
    fun write(context: Context, bitmap: Bitmap): Boolean {
        val tmp = File(context.filesDir, Settings.WALLPAPER_FILE + ".tmp")
        return runCatching {
            tmp.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
            check(tmp.renameTo(shown(context)))
            true
        }.getOrElse { tmp.delete(); false }
    }
}

/**
 * The wallpaper editor (plan F5): the photo in the car screen's shape, moved with a finger and
 * zoomed with two, then darkened and blurred with two sliders. "Use as wallpaper" writes the file
 * the car already reads; the untouched photo stays, so editing again starts from it, not from the
 * darkened result. [onClose] gets whether a new wallpaper was written.
 */
@Composable
fun WallpaperEditorDialog(onClose: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    val scope = rememberCoroutineScope()
    val source by produceState<Bitmap?>(null) { value = withContext(Dispatchers.IO) { WallpaperFiles.loadSource(ctx) } }
    var edit by remember { mutableStateOf(settings.myplayWallpaperEdit) }
    var saving by remember { mutableStateOf(false) }
    // A small rendering for the screen, redone as the edit changes.
    val preview by produceState<Bitmap?>(null, source, edit) {
        val s = source ?: return@produceState
        value = withContext(Dispatchers.Default) { WallpaperFiles.render(s, edit, 480, 289) }
    }
    Dialog(onDismissRequest = { if (!saving) onClose(false) }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp).clip(RoundedCornerShape(28.dp)).background(PhoneColors.Page)
                .verticalScroll(rememberScrollState()).padding(18.dp),
        ) {
            Text(stringResource(R.string.phone_wallpaper_edit_title), color = PhoneColors.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            BoxWithConstraints(
                Modifier.fillMaxWidth().aspectRatio(CarPictureSize.ASPECT).clip(RoundedCornerShape(18.dp))
                    .border(1.dp, PhoneColors.Border, RoundedCornerShape(18.dp)).background(PhoneColors.Card),
                contentAlignment = Alignment.Center,
            ) {
                val viewW = constraints.maxWidth.toFloat()
                val viewH = constraints.maxHeight.toFloat()
                val s = source
                val p = preview
                if (s == null) Text(stringResource(R.string.phone_wallpaper_edit_none), color = PhoneColors.Muted, fontSize = 14.sp)
                else Box(
                    Modifier.fillMaxSize().pointerInput(s) {
                        detectTransformGestures { _, pan, zoom, _ -> edit = dragged(edit, s.width, s.height, viewW, viewH, pan.x, pan.y, zoom) }
                    },
                ) { if (p != null) Image(p.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds) }
            }
            HintLine(Icons.Rounded.TouchApp, stringResource(R.string.phone_wallpaper_edit_hint))
            Text(stringResource(R.string.phone_wallpaper_edit_dim, edit.dimPercent), color = PhoneColors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Slider(edit.dimPercent.toFloat(), { edit = edit.copy(dimPercent = it.toInt()) }, valueRange = 0f..WallpaperEdit.MAX_DIM.toFloat(),
                colors = sliderColoursFor())
            Text(stringResource(R.string.phone_wallpaper_edit_blur, edit.blur), color = PhoneColors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Slider(edit.blur.toFloat(), { edit = edit.copy(blur = it.toInt()) }, valueRange = 0f..WallpaperEdit.MAX_BLUR.toFloat(),
                steps = WallpaperEdit.MAX_BLUR - 1, colors = sliderColoursFor())
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                PillButton(stringResource(R.string.phone_wallpaper_edit_reset), main = false) { edit = WallpaperEdit() }
                Spacer(Modifier.weight(1f))
                PillButton(stringResource(R.string.common_cancel), main = false) { if (!saving) onClose(false) }
                PillButton(stringResource(if (saving) R.string.phone_wallpaper_edit_saving else R.string.phone_wallpaper_edit_use)) {
                    val s = source ?: return@PillButton
                    if (saving) return@PillButton
                    saving = true
                    val chosen = edit
                    scope.launch {
                        val ok = withContext(Dispatchers.Default) {
                            WallpaperFiles.write(ctx, WallpaperFiles.render(s, chosen, WallpaperFiles.OUT_W, WallpaperFiles.OUT_H))
                        }
                        if (ok) {
                            settings.myplayWallpaperEdit = chosen
                            // Through "none", as the Car screen page does, so the car reloads the file.
                            settings.wallpaper = "none"; settings.wallpaper = Settings.WALLPAPER_CUSTOM
                        }
                        saving = false
                        onClose(ok)
                    }
                }
            }
        }
    }
}

@Composable
private fun sliderColoursFor() = SliderDefaults.colors(thumbColor = PhoneColors.Accent, activeTrackColor = PhoneColors.Accent, inactiveTrackColor = PhoneColors.SwitchOff)
