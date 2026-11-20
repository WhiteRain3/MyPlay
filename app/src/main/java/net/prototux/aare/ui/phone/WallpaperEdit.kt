package net.prototux.aare.ui.phone

import kotlin.math.max

/**
 * How the owner placed and treated their wallpaper photo (plan F5): [zoom] (1 = the photo just
 * covers the car screen), where the visible part sits ([offsetX], [offsetY], each -1 to 1 across
 * the room the zoom leaves), how much darker ([dimPercent], 0 to 80) and how blurred ([blur], 0 to
 * 10). Stored so the photo can be edited again from its untouched copy.
 */
data class WallpaperEdit(
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val dimPercent: Int = 0,
    val blur: Int = 0,
) {
    /** The same edit with every value inside its range. */
    fun clamped() = WallpaperEdit(
        zoom.coerceIn(1f, MAX_ZOOM), offsetX.coerceIn(-1f, 1f), offsetY.coerceIn(-1f, 1f),
        dimPercent.coerceIn(0, MAX_DIM), blur.coerceIn(0, MAX_BLUR),
    )

    fun encode(): String = "zoom=$zoom;x=$offsetX;y=$offsetY;dim=$dimPercent;blur=$blur"

    companion object {
        /** At most three times: the kept photo is at most 1920 pixels wide, and more would look soft on the car. */
        const val MAX_ZOOM = 3f
        const val MAX_DIM = 80
        const val MAX_BLUR = 10

        /** An edit written by [encode]; anything missing or unreadable takes the default. */
        fun decode(s: String?): WallpaperEdit {
            val m = s.orEmpty().split(';').mapNotNull { p -> p.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0].trim() to it[1].trim() } }.toMap()
            val d = WallpaperEdit()
            return WallpaperEdit(
                m["zoom"]?.toFloatOrNull() ?: d.zoom, m["x"]?.toFloatOrNull() ?: d.offsetX, m["y"]?.toFloatOrNull() ?: d.offsetY,
                m["dim"]?.toIntOrNull() ?: d.dimPercent, m["blur"]?.toIntOrNull() ?: d.blur,
            ).clamped()
        }
    }
}

/** A rectangle of the source photo, in its pixels. */
data class SourceRect(val left: Float, val top: Float, val width: Float, val height: Float)

/**
 * The part of a [srcW] x [srcH] photo that fills a screen of shape [outAspect] (width / height)
 * under [edit]: at zoom 1 the largest centred part of that shape (the photo covers the screen,
 * nothing stretched); zooming in takes a smaller part; the offsets move it within the photo.
 */
fun sourceRect(srcW: Int, srcH: Int, outAspect: Float, edit: WallpaperEdit): SourceRect {
    val e = edit.clamped()
    // The largest part of the screen's shape that fits in the photo.
    val coverW = if (srcW.toFloat() / srcH > outAspect) srcH * outAspect else srcW.toFloat()
    val coverH = coverW / outAspect
    val w = coverW / e.zoom
    val h = coverH / e.zoom
    val roomX = max(0f, srcW - w)
    val roomY = max(0f, srcH - h)
    return SourceRect(roomX / 2 * (1 + e.offsetX), roomY / 2 * (1 + e.offsetY), w, h)
}

/** [edit] after a drag of [dx], [dy] (screen pixels on a preview [viewW] x [viewH]) and a pinch of [zoomBy]. */
fun dragged(edit: WallpaperEdit, srcW: Int, srcH: Int, viewW: Float, viewH: Float, dx: Float, dy: Float, zoomBy: Float): WallpaperEdit {
    val zoomed = edit.copy(zoom = (edit.zoom * zoomBy).coerceIn(1f, WallpaperEdit.MAX_ZOOM))
    val r = sourceRect(srcW, srcH, viewW / viewH, zoomed)
    val roomX = srcW - r.width
    val roomY = srcH - r.height
    // A drag to the right shows more of the photo's left side: the visible part moves left.
    val perPixel = r.width / viewW
    val x = if (roomX > 0) zoomed.offsetX - dx * perPixel / (roomX / 2) else 0f
    val y = if (roomY > 0) zoomed.offsetY - dy * perPixel / (roomY / 2) else 0f
    return zoomed.copy(offsetX = x, offsetY = y).clamped()
}

/** The blur radius in output pixels for blur level [level] on an output [outWidth] pixels wide. */
fun blurRadius(level: Int, outWidth: Int): Float = if (level <= 0) 0f else level * outWidth / 400f
