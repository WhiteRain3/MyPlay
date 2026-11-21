package net.prototux.aare.ui.theme

import androidx.core.content.edit
import net.prototux.aare.Settings
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * MyPlay (map recolouring, option 1): a colour filter on a hosted maps app's picture, so any maps
 * app (Waze included) can be brought closer to the chosen look.
 *
 * Each look carries its own filter (the setting is one of a look's, MF-3).
 *
 * It works on colours only: it recolours everything the app draws on its map, its own buttons and
 * speed sign included, and cannot tell water from roads. Off unless the owner sets one.
 *
 * The filter is a 4x5 colour matrix ([matrix]) in Android's ColorMatrix layout (rows R, G, B, A;
 * columns R, G, B, A, offset with offsets in 0..255), applied by the map's view while a filter is on.
 *
 * Steps, in order: saturation, hue shift, contrast, brightness, tint, then (at night only) darker.
 */
data class MapColourFilter(
    /** Hue turned by this many degrees, -180..180. */
    val hueShift: Int = 0,
    /** Colourfulness in %, 0 (grey) .. 200; 100 leaves it. */
    val saturation: Int = 100,
    /** Lighter or darker, -100..100 (a fraction of full white added). */
    val brightness: Int = 0,
    /** Contrast in %, 0..200, around middle grey; 100 leaves it. */
    val contrast: Int = 100,
    /** Tint colour (0xRRGGBB) the picture is pulled towards by [tintStrength]. */
    val tint: Int = 0x000000,
    /** How strongly the tint applies, 0..100 %. */
    val tintStrength: Int = 0,
    /** At night, the picture this much darker, 0..90 %. */
    val nightDarker: Int = 0,
) {
    /** Every value inside its range. */
    fun clamped() = copy(
        hueShift = hueShift.coerceIn(-180, 180), saturation = saturation.coerceIn(0, 200),
        brightness = brightness.coerceIn(-100, 100), contrast = contrast.coerceIn(0, 200),
        tint = tint and 0xFFFFFF, tintStrength = tintStrength.coerceIn(0, 100), nightDarker = nightDarker.coerceIn(0, 90),
    )

    /** Whether it changes nothing (then the map is drawn as today, without a filter). */
    fun isIdentity(night: Boolean): Boolean {
        val c = clamped()
        return c.hueShift == 0 && c.saturation == 100 && c.brightness == 0 && c.contrast == 100 &&
            c.tintStrength == 0 && (!night || c.nightDarker == 0)
    }

    /** The 20 values of the colour matrix, or null when it changes nothing. */
    fun matrix(night: Boolean): FloatArray? {
        if (isIdentity(night)) return null
        val c = clamped()
        var m = IDENTITY
        m = concat(saturationMatrix(c.saturation / 100f), m)
        m = concat(hueMatrix(c.hueShift.toFloat()), m)
        m = concat(scaleAround(c.contrast / 100f, 127.5f), m)
        m = concat(offset(c.brightness * 2.55f), m)
        if (c.tintStrength > 0) m = concat(tintMatrix(c.tint, c.tintStrength / 100f), m)
        if (night && c.nightDarker > 0) m = concat(scaleAround(1f - c.nightDarker / 100f, 0f), m)
        return m
    }

    companion object {
        /** N3: the filter the map gets: the look's own when it sets one, else the skin's suggestion, else none. */
        fun chosen(look: MapColourFilter, skin: MapColourFilter?): MapColourFilter =
            if (look.isIdentity(night = true) && skin != null) skin else look

        /** Presets the phone can offer; each look can take one or its own values. */
        val PRESETS: Map<String, MapColourFilter> = linkedMapOf(
            "none" to MapColourFilter(),
            "muted" to MapColourFilter(saturation = 60, contrast = 95),
            "cool" to MapColourFilter(hueShift = 15, saturation = 85, tint = 0x2A4A7A, tintStrength = 15),
            "warm" to MapColourFilter(hueShift = -10, saturation = 90, tint = 0x8A5A2A, tintStrength = 12),
            "dark_night" to MapColourFilter(nightDarker = 35),
            "grey" to MapColourFilter(saturation = 0, contrast = 110),
        )

        /** The matrix that changes nothing. */
        val IDENTITY: FloatArray get() = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )

        /** [a] applied after [b] (a · b), offsets included. */
        internal fun concat(a: FloatArray, b: FloatArray): FloatArray {
            val out = FloatArray(20)
            for (r in 0 until 4) {
                for (c in 0 until 5) {
                    var v = 0f
                    for (k in 0 until 4) v += a[r * 5 + k] * b[k * 5 + c]
                    if (c == 4) v += a[r * 5 + 4]
                    out[r * 5 + c] = v
                }
            }
            return out
        }

        /** Applies [m] to one colour (0..255 each, alpha 255), clamped, for tests and previews. */
        fun apply(m: FloatArray, rgb: Int): Int {
            val i = floatArrayOf(((rgb shr 16) and 0xFF).toFloat(), ((rgb shr 8) and 0xFF).toFloat(), (rgb and 0xFF).toFloat(), 255f)
            fun ch(r: Int) = (m[r * 5] * i[0] + m[r * 5 + 1] * i[1] + m[r * 5 + 2] * i[2] + m[r * 5 + 3] * i[3] + m[r * 5 + 4])
                .let { Math.round(it).coerceIn(0, 255) }
            return (ch(0) shl 16) or (ch(1) shl 8) or ch(2)
        }

        // Rec. 709 luma weights, as Android's ColorMatrix.setSaturation uses.
        private const val LR = 0.2126f
        private const val LG = 0.7152f
        private const val LB = 0.0722f

        private fun saturationMatrix(s: Float): FloatArray {
            val ir = (1 - s) * LR
            val ig = (1 - s) * LG
            val ib = (1 - s) * LB
            return floatArrayOf(
                ir + s, ig, ib, 0f, 0f,
                ir, ig + s, ib, 0f, 0f,
                ir, ig, ib + s, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            )
        }

        /** A hue rotation keeping lightness: the W3C filter effects' hueRotate matrix. */
        private fun hueMatrix(degrees: Float): FloatArray {
            if (degrees == 0f) return IDENTITY
            val a = degrees * PI.toFloat() / 180f
            val c = cos(a)
            val s = sin(a)
            return floatArrayOf(
                0.213f + c * 0.787f - s * 0.213f, 0.715f - c * 0.715f - s * 0.715f, 0.072f - c * 0.072f + s * 0.928f, 0f, 0f,
                0.213f - c * 0.213f + s * 0.143f, 0.715f + c * 0.285f + s * 0.140f, 0.072f - c * 0.072f - s * 0.283f, 0f, 0f,
                0.213f - c * 0.213f - s * 0.787f, 0.715f - c * 0.715f + s * 0.715f, 0.072f + c * 0.928f + s * 0.072f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            )
        }

        /** Scales R, G and B by [k] around [pivot] (contrast around middle grey, darker around black). */
        private fun scaleAround(k: Float, pivot: Float): FloatArray {
            val o = pivot * (1 - k)
            return floatArrayOf(
                k, 0f, 0f, 0f, o,
                0f, k, 0f, 0f, o,
                0f, 0f, k, 0f, o,
                0f, 0f, 0f, 1f, 0f,
            )
        }

        private fun offset(v: Float) = floatArrayOf(
            1f, 0f, 0f, 0f, v,
            0f, 1f, 0f, 0f, v,
            0f, 0f, 1f, 0f, v,
            0f, 0f, 0f, 1f, 0f,
        )

        /** Mixes each colour towards [tint] by [t], keeping its lightness: light stays light. */
        private fun tintMatrix(tint: Int, t: Float): FloatArray {
            val tr = ((tint shr 16) and 0xFF) / 255f
            val tg = ((tint shr 8) and 0xFF) / 255f
            val tb = (tint and 0xFF) / 255f
            // Each output channel: (1 - t) · itself + t · luma · tint channel · 2 (a tint of mid grey keeps lightness).
            fun row(keep: Int, tc: Float): FloatArray {
                val r = FloatArray(5)
                r[keep] = 1 - t
                r[0] += t * LR * tc * 2
                r[1] += t * LG * tc * 2
                r[2] += t * LB * tc * 2
                return r
            }
            return row(0, tr) + row(1, tg) + row(2, tb) + floatArrayOf(0f, 0f, 0f, 1f, 0f)
        }

        /** "key:value,..." with every value named, so later versions can add some. */
        fun encode(f: MapColourFilter): String = listOf(
            "hue" to f.hueShift, "sat" to f.saturation, "bright" to f.brightness, "contrast" to f.contrast,
            "tint" to f.tint, "tint_strength" to f.tintStrength, "night" to f.nightDarker,
        ).joinToString(",") { (k, v) -> if (k == "tint") "$k:${"%06x".format(v and 0xFFFFFF)}" else "$k:$v" }

        /** The filter written by [encode] (a preset's name also works); unknown parts are skipped, values clamped. */
        fun decode(s: String?): MapColourFilter {
            if (s.isNullOrBlank()) return MapColourFilter()
            PRESETS[s.trim()]?.let { return it }
            var f = MapColourFilter()
            for (part in s.split(',')) {
                val (k, v) = part.split(':', limit = 2).takeIf { it.size == 2 }?.map { it.trim() } ?: continue
                f = when (k) {
                    "hue" -> v.toIntOrNull()?.let { f.copy(hueShift = it) } ?: f
                    "sat" -> v.toIntOrNull()?.let { f.copy(saturation = it) } ?: f
                    "bright" -> v.toIntOrNull()?.let { f.copy(brightness = it) } ?: f
                    "contrast" -> v.toIntOrNull()?.let { f.copy(contrast = it) } ?: f
                    "tint" -> v.toIntOrNull(16)?.let { f.copy(tint = it) } ?: f
                    "tint_strength" -> v.toIntOrNull()?.let { f.copy(tintStrength = it) } ?: f
                    "night" -> v.toIntOrNull()?.let { f.copy(nightDarker = it) } ?: f
                    else -> f
                }
            }
            return f.clamped()
        }
    }
}

/**
 * MyPlay: the map colour filter ([MapColourFilter.encode]); empty for none. It is one of a look's
 * settings (F7, MyPlayLooks.KEYS), so each look carries its own filter (MF-3).
 */
var Settings.myplayMapFilter: MapColourFilter
    get() = MapColourFilter.decode(prefs.getString("myplay_map_filter", ""))
    set(v) = prefs.edit {
        if (v.isIdentity(night = true)) remove("myplay_map_filter") else putString("myplay_map_filter", MapColourFilter.encode(v.clamped()))
    }
