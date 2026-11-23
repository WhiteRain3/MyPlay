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

    /** Copies to a temporary file renamed only once complete, so a copy cut short (a full disk) never becomes the source. */
    internal fun keepSource(shown: File, source: File): Boolean {
        val tmp = File(source.path + ".tmp")
        return runCatching {
            shown.copyTo(tmp, overwrite = true)
            // renameTo replaces the old source in one step on Android; deleting it first could lose it.
            check(tmp.renameTo(source))
            true
        }.getOrElse { tmp.delete(); false }
    }

    /**
     * The file to edit from: the untouched copy. A photo set before the editor existed has no copy
     * yet: the shown wallpaper becomes the copy now, once, so later edits start from it and not
     * from their own results (darkening and blur never pile up). If that copy cannot be made (a full
     * disk), the shown photo is edited as it is rather than said to be missing. Null when there is no photo.
     */
    internal fun sourceFile(shown: File, source: File): File? {
        if (!source.isFile && shown.isFile) keepSource(shown, source)
        return source.takeIf { it.isFile } ?: shown.takeIf { it.isFile }
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

