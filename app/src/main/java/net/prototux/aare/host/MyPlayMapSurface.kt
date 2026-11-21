package net.prototux.aare.host

import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import net.prototux.aare.ui.theme.MapColourFilter

/** N3: the map colour filter the screen's skin suggests (MapColourFilter.chosen), or null. */
internal val LocalSkinMapFilter = staticCompositionLocalOf<MapColourFilter?> { null }

/**
 * MyPlay (map recolouring, option 1): a hosted maps app's map drawn through a colour filter.
 *
 * The usual map surface is a SurfaceView, which the system puts on screen itself, so nothing MyPlay
 * draws can recolour it. While a filter is on, the map goes to a TextureView instead, drawn with a
 * colour-matrix layer paint ([matrix], see ui/theme/MapColourFilter.kt). That costs one more copy
 * of the picture per frame, which is why it is used only while a filter is on.
 *
 * Touch and the knob are not affected: gestures are read by the map scaffold drawn over this, not
 * by the surface.
 */
@Composable
internal fun FilteredMapSurface(conn: CarAppConnection, dpi: Int, matrix: FloatArray, modifier: Modifier) {
    val owner = remember { Any() }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    private var surface: Surface? = null

                    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
                        surface = Surface(texture).also { conn.surfaceAvailable(owner, it, width, height, dpi) }
                    }

                    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
                        surface?.let { conn.surfaceAvailable(owner, it, width, height, dpi) }
                    }

                    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                        conn.surfaceDestroyed(owner)
                        surface?.release()
                        surface = null
                        return true
                    }

                    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
                }
            }
        },
        update = { view -> view.setLayerPaint(Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) }) },
    )
    DisposableEffect(owner) { onDispose { conn.surfaceDestroyed(owner) } }
}
