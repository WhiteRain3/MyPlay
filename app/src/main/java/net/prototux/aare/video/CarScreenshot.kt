package net.prototux.aare.video

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.Image
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.prototux.aare.R
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * MyPlay (car screenshot): a picture of exactly what the car shows, taken only when the owner asks
 * (the connection notification's Screenshot action, the Home button, or a car button or home pad
 * key set to "Car screenshot"), and only while a car session shows MyPlay's screen. Saved as a PNG
 * in Pictures/MyPlay through MediaStore, which needs no permission (Android 10 and newer); nothing
 * is taken, kept or logged on its own.
 */
object CarScreenshot {
    /** Takes one frame of the car screen; the frame (or null) goes to the callback on the main thread. */
    fun interface Shooter {
        fun shoot(done: (Bitmap?) -> Unit)
    }

    private const val TAG = "CarScreenshot"
    const val FOLDER = "MyPlay"

    private val main = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor { r -> Thread(r, "car-screenshot").apply { isDaemon = true } }

    @Volatile private var shooter: Shooter? = null
    @Volatile private var saved: (() -> Unit)? = null
    @Volatile private var busy = false

    private val _live = MutableStateFlow(false)
    /** A car session shows its screen: a screenshot can be taken. */
    val live: StateFlow<Boolean> = _live.asStateFlow()

    /**
     * The car session's screen, from when it shows until [detach]. [onSaved] tells the car (a short
     * notice there) that a picture was saved.
     */
    fun attach(s: Shooter, onSaved: (() -> Unit)? = null) {
        shooter = s
        saved = onSaved
        _live.value = true
    }

    /** The screen [s] went away (only if it is still the one attached). */
    fun detach(s: Shooter) {
        if (shooter !== s) return
        shooter = null
        saved = null
        _live.value = false
    }

    /**
     * Takes a screenshot of the car screen and saves it; a toast says how it went. Any thread.
     * False when there is no car screen to take (no live session) or one is already being taken.
     */
    fun take(context: Context): Boolean {
        val app = context.applicationContext
        val s = shooter
        if (s == null) {
            toast(app, R.string.myplay_screenshot_no_car)
            return false
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Before Android 10 a picture in Pictures needs the storage permission, which MyPlay doesn't ask for.
            toast(app, R.string.myplay_screenshot_old_android)
            return false
        }
        if (busy) return false
        busy = true
        main.post {
            runCatching {
                s.shoot { bitmap ->
                    if (bitmap == null) {
                        busy = false
                        toast(app, R.string.myplay_screenshot_failed)
                        return@shoot
                    }
                    io.execute {
                        val ok = runCatching { save(app, bitmap, System.currentTimeMillis()) }
                            .onFailure { Log.w(TAG, "can't save the car screenshot", it) }.getOrDefault(false)
                        bitmap.recycle()
                        busy = false
                        toast(app, if (ok) R.string.myplay_screenshot_saved else R.string.myplay_screenshot_failed)
                        if (ok) main.post { saved?.invoke() }
                    }
                }
            }.onFailure {
                busy = false
                Log.w(TAG, "can't take the car screenshot", it)
                toast(app, R.string.myplay_screenshot_failed)
            }
        }
        return true
    }

    /** "MyPlay-car-20261126-203015.png", local time. */
    fun fileName(atMs: Long): String =
        "MyPlay-car-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(atMs)) + ".png"

    private fun save(context: Context, bitmap: Bitmap, atMs: Long): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName(atMs))
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + FOLDER)
            put(MediaStore.Images.Media.DATE_TAKEN, atMs)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values) ?: return false
        val written = runCatching {
            resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } == true
        }.getOrDefault(false)
        if (!written) {
            runCatching { resolver.delete(uri, null, null) }
            return false
        }
        resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        return true
    }

    private fun toast(context: Context, text: Int) {
        main.post { Toast.makeText(context, text, Toast.LENGTH_SHORT).show() }
    }
}

/** MyPlay (car screenshot): a frame from the display's [Image] to a [Bitmap]. */
internal object CarFrame {
    /** The RGBA_8888 [image], cropped to [crop] (all of it when null or out of bounds). */
    fun toBitmap(image: Image, crop: Rect?): Bitmap {
        val plane = image.planes[0]
        val w = image.width
        val h = image.height
        val packed = pack(plane.buffer, plane.rowStride, plane.pixelStride, w, h)
        val full = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        full.copyPixelsFromBuffer(packed)
        val r = cropIn(crop, w, h) ?: return full
        val cut = Bitmap.createBitmap(full, r.left, r.top, r.width(), r.height())
        if (cut !== full) full.recycle()
        return cut
    }

    /**
     * The pixels of [src] without the row padding an [Image] plane may carry: [height] rows of
     * [width] pixels of [pixelStride] bytes, each row starting [rowStride] bytes after the last.
     */
    fun pack(src: ByteBuffer, rowStride: Int, pixelStride: Int, width: Int, height: Int): ByteBuffer {
        val rowBytes = width * pixelStride
        val out = ByteBuffer.allocate(rowBytes * height)
        val row = ByteArray(rowBytes)
        val from = src.duplicate()
        for (y in 0 until height) {
            from.position(y * rowStride)
            from.get(row, 0, rowBytes)
            out.put(row)
        }
        out.rewind()
        return out
    }

    /** [crop] when it is a non-empty part of a [width] by [height] frame that isn't all of it; else null. */
    fun cropIn(crop: Rect?, width: Int, height: Int): Rect? {
        if (crop == null) return null
        val l = crop.left.coerceIn(0, width)
        val t = crop.top.coerceIn(0, height)
        val r = crop.right.coerceIn(l, width)
        val b = crop.bottom.coerceIn(t, height)
        if (r - l <= 0 || b - t <= 0) return null
        if (l == 0 && t == 0 && r == width && b == height) return null
        return Rect(l, t, r, b)
    }
}
