package net.prototux.aare.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import net.prototux.aare.Settings
import net.prototux.aare.service.ProjectionService
import net.prototux.aare.ui.phone.MyPlayPhoneTheme
import net.prototux.aare.ui.phone.PhonePartsSample
import net.prototux.aare.ui.phone.next.NextAppHost
import net.prototux.aare.ui.phone.next.NextDeepLink
import kotlin.math.roundToInt

/** The phone side: the phone app (plan §16, the rebuilt one since chunk 14). */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Always dark (the owner's choice), so the phone's own bars get light symbols whatever the phone is set to.
        enableEdgeToEdge(SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        applyDebugExtras(intent)
        showParts = intent?.getBooleanExtra(EXTRA_PHONE_PARTS, false) == true
        // Only a fresh start follows the extra: after rotation or process death the saved place wins.
        if (savedInstanceState == null) route = NextDeepLink.routeOf(intent)
        setContent {
            MyPlayPhoneTheme {
                Surface(Modifier.fillMaxSize()) {
                    if (showParts) {
                        BackHandler { showParts = false }
                        PhonePartsSample { showParts = false }
                    } else {
                        NextAppHost(route, onDeepLinkUsed = { route = null })
                    }
                }
            }
        }
    }

    /** A page to open, from MyPlay's own PendingIntents only ([NextDeepLink]); the app clears it once shown. */
    private var route by mutableStateOf<String?>(null)

    /** The sample page of every shared phone part, for screenshots ([EXTRA_PHONE_PARTS]). */
    private var showParts by mutableStateOf(false)

    // singleTask: when MyPlay is already open, ./headunit.sh's intent arrives here.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_PHONE_PARTS, false)) showParts = true
        NextDeepLink.routeOf(intent)?.let { route = it }
        if (applyDebugExtras(intent)) ProjectionService.startIdleIfEnabled(this)
    }

    /** Debug builds only: lets ./headunit.sh switch the head unit server on over adb. Returns true if applied. */
    private fun applyDebugExtras(intent: Intent?): Boolean {
        if (!net.prototux.aare.BuildConfig.DEBUG || intent == null) return false
        if (!intent.hasExtra("hu_server")) return false
        Settings(this).headUnitServer = intent.getBooleanExtra("hu_server", false)
        return true
    }
}

/** Intent extra (true) that opens the sample page of every shared phone part; it changes no setting. */
private const val EXTRA_PHONE_PARTS = "phone_parts"

/**
 * Whether a permission the request just denied can no longer be asked for (main setup row). Since Android 11,
 * backing out of the dialog also denies with no rationale, as before any request: so it counts as refused for
 * good only if the rationale went from shown to gone (refused a second time), or it was asked for in an earlier
 * request and still has none (blocked back then).
 */
internal fun permissionBlocked(rationaleBefore: Boolean, rationaleAfter: Boolean, askedBefore: Boolean): Boolean =
    !rationaleAfter && (rationaleBefore || askedBefore)

/** Copies the chosen photo into AARE's files, at most 1920 pixels on its longest side, as the car's wallpaper. */
internal fun saveWallpaper(context: android.content.Context, uri: Uri): Boolean {
    // Written to a temporary file and renamed over the current one only once complete, so a photo
    // that can't be read or written (a full disk) leaves the current wallpaper alone.
    val tmp = java.io.File(context.filesDir, Settings.WALLPAPER_FILE + ".tmp")
    val ok = runCatching {
        val bmp = decodeWallpaper(context, uri, 1920)
        tmp.outputStream().use { check(bmp.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
        check(tmp.renameTo(java.io.File(context.filesDir, Settings.WALLPAPER_FILE)))
    }.isSuccess
    if (!ok) tmp.delete()
    return ok
}

/**
 * Decodes [uri] upright (cameras store portrait photos sideways with an EXIF orientation tag) and
 * already scaled down towards [maxPx], so a 200 MP photo is never decoded at full size.
 */
private fun decodeWallpaper(context: android.content.Context, uri: Uri, maxPx: Int): Bitmap {
    val resolver = context.contentResolver
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // ImageDecoder applies the EXIF orientation itself.
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE // compress() needs a software bitmap
            val (w, h) = info.size.width to info.size.height
            val scale = maxPx.toFloat() / maxOf(w, h)
            if (scale < 1f) decoder.setTargetSize((w * scale).roundToInt().coerceAtLeast(1), (h * scale).roundToInt().coerceAtLeast(1))
        }
    }
    // Android 8: a power-of-two sample size, then the exact scale and the EXIF rotation in one matrix.
    // Each pass opens its own stream: a content stream can be read only once.
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) sample *= 2
    val src = resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
        ?: error("can't decode the photo")
    val orientation = runCatching {
        resolver.openInputStream(uri)!!.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    val scale = minOf(1f, maxPx.toFloat() / maxOf(src.width, src.height))
    val m = Matrix().apply {
        postScale(scale, scale)
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(-90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(-90f)
        }
    }
    val out = Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    if (out !== src) src.recycle()
    return out
}
