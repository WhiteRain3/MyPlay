package net.prototux.aare.speedlimit

import java.io.File

/**
 * MyPlay (speed limits, option A): where Lithuania's settlements are, for the default limits of
 * untagged roads (50 inside, 90 outside). Built from the extract's settlement areas
 * (place=city/town/village/hamlet and landuse=residential/commercial/retail/industrial), exported
 * by osmium as GeoJSON, one feature per line (`osmium export -f geojsonseq`).
 *
 * A point is inside when any one area holds it (each area's rings even-odd, so its holes are
 * outside). Areas are indexed by the file's tiles, so a test looks only at the areas near it.
 */
class Settlements private constructor(private val areas: List<Area>, private val places: List<Place> = emptyList()) {
    /**
     * SL-1: a settlement mapped only as a point (place=village on a node, common in rural
     * Lithuania): inside within [radiusE5] of it, so a road through it is not given 90.
     */
    class Place(val lat: Int, val lon: Int, val radiusE5: Int) {
        fun contains(lat: Int, lon: Int): Boolean {
            // East-west degrees are shorter at Lithuania's latitude (cos 55° ≈ 0.57).
            val dLat = (lat - this.lat).toDouble(); val dLon = (lon - this.lon) * 0.57
            return dLat * dLat + dLon * dLon <= radiusE5.toDouble() * radiusE5
        }
    }

    /** One area: its rings (outer and holes alike), points in 1e-5 degrees, and its box. */
    class Area(val rings: List<Pair<IntArray, IntArray>>) {
        val minLat = rings.minOf { it.first.min() }; val maxLat = rings.maxOf { it.first.max() }
        val minLon = rings.minOf { it.second.min() }; val maxLon = rings.maxOf { it.second.max() }

        fun contains(lat: Int, lon: Int): Boolean {
            if (lat < minLat || lat > maxLat || lon < minLon || lon > maxLon) return false
            var inside = false
            for ((lats, lons) in rings) {
                var j = lats.size - 1
                for (i in lats.indices) {
                    // Even-odd: a ray to the east crosses the edge (i, j).
                    if ((lats[i] > lat) != (lats[j] > lat)) {
                        val x = lons[i] + (lat - lats[i]).toDouble() * (lons[j] - lons[i]) / (lats[j] - lats[i])
                        if (lon < x) inside = !inside
                    }
                    j = i
                }
            }
            return inside
        }
    }

    private val tiles = HashMap<Long, MutableList<Area>>().also { index ->
        for (a in areas) {
            for (r in tile(a.minLat)..tile(a.maxLat)) for (c in tile(a.minLon)..tile(a.maxLon)) {
                index.getOrPut(key(r, c)) { ArrayList() } += a
            }
        }
    }

    private val placeTiles = HashMap<Long, MutableList<Place>>().also { index ->
        for (p in places) {
            val rLon = (p.radiusE5 / 0.57).toInt()
            for (r in tile(p.lat - p.radiusE5)..tile(p.lat + p.radiusE5)) for (c in tile(p.lon - rLon)..tile(p.lon + rLon)) {
                index.getOrPut(key(r, c)) { ArrayList() } += p
            }
        }
    }

    val size get() = areas.size
    val placeCount get() = places.size

    /** Whether the point (1e-5 degrees) is inside a settlement: in one of its areas, or near its place point. */
    fun contains(lat: Int, lon: Int): Boolean {
        val k = key(tile(lat), tile(lon))
        return tiles[k]?.any { it.contains(lat, lon) } == true || placeTiles[k]?.any { it.contains(lat, lon) } == true
    }

    companion object {
        private fun tile(e5: Int) = Math.floorDiv(e5, SpeedLimitFormat.TILE_E5)
        private fun key(r: Int, c: Int) = (r.toLong() shl 32) or (c.toLong() and 0xffffffffL)

        fun of(areas: List<Area>, places: List<Place> = emptyList()) = Settlements(areas, places)

        /**
         * SL-1: how far a settlement mapped only as a point reaches, by its kind (1e-5 degrees of
         * latitude, about 1.1 m each): a hamlet or a village 700 m (SL-6), a town 1.5 km, a city 3 km.
         * Generous rather than tight: inside means 50, the lower guess.
         */
        fun radiusOf(place: String?): Int? = when (place) {
            "hamlet", "village" -> 630
            "town" -> 1_350
            "city" -> 2_700
            else -> null
        }

        /**
         * Reads osmium's GeoJSON sequence: Polygon and MultiPolygon features as areas, Point
         * features with a place tag as places; anything else is skipped.
         */
        fun read(file: File): Settlements {
            val areas = ArrayList<Area>(); val places = ArrayList<Place>()
            file.bufferedReader().useLines { lines ->
                for (line in lines) area(line)?.let { areas += it } ?: place(line)?.let { places += it }
            }
            return of(areas, places)
        }

        /** One GeoJSON Point feature with a place=hamlet/village/town/city tag to a [Place], or null. */
        fun place(line: String): Place? {
            val text = line.trim().trimStart('\u001e')
            if (!Regex("\"type\"\\s*:\\s*\"Point\"").containsMatchIn(text)) return null
            val radius = radiusOf(Regex("\"place\"\\s*:\\s*\"([a-z_]+)\"").find(text)?.groupValues?.get(1)) ?: return null
            val m = Regex("\"coordinates\"\\s*:\\s*\\[\\s*([-+0-9.eE]+)\\s*,\\s*([-+0-9.eE]+)").find(text) ?: return null
            val lon = m.groupValues[1].toDoubleOrNull() ?: return null
            val lat = m.groupValues[2].toDoubleOrNull() ?: return null
            return Place(Math.round(lat * 1e5).toInt(), Math.round(lon * 1e5).toInt(), radius)
        }

        /** One GeoJSON feature line to an area (its rings), or null. */
        fun area(line: String): Area? {
            val text = line.trim().trimStart('\u001e')
            val type = Regex("\"type\"\\s*:\\s*\"(Polygon|MultiPolygon)\"").find(text)?.groupValues?.get(1) ?: return null
            val at = text.indexOf("\"coordinates\"").takeIf { it >= 0 } ?: return null
            val start = text.indexOf('[', at).takeIf { it >= 0 } ?: return null
            val value = Nested(text, start).parse() as? List<*> ?: return null
            val polygons = if (type == "Polygon") listOf(value) else value
            val rings = ArrayList<Pair<IntArray, IntArray>>()
            for (polygon in polygons) for (ring in polygon as? List<*> ?: continue) {
                val points = (ring as? List<*>)?.mapNotNull { p ->
                    val xy = p as? List<*> ?: return@mapNotNull null
                    val lon = xy.getOrNull(0) as? Double ?: return@mapNotNull null
                    val lat = xy.getOrNull(1) as? Double ?: return@mapNotNull null
                    Math.round(lat * 1e5).toInt() to Math.round(lon * 1e5).toInt()
                } ?: continue
                if (points.size >= 3) rings += points.map { it.first }.toIntArray() to points.map { it.second }.toIntArray()
            }
            return if (rings.isEmpty()) null else Area(rings)
        }
    }

    /** Nested JSON arrays of numbers (GeoJSON coordinates) from [start]: lists of lists of Double. */
    private class Nested(private val s: String, private var i: Int) {
        fun parse(): Any? {
            skip()
            if (i >= s.length) return null
            if (s[i] == '[') {
                i++
                val out = ArrayList<Any?>()
                while (true) {
                    skip()
                    if (i >= s.length) return out
                    if (s[i] == ']') { i++; return out }
                    if (s[i] == ',') { i++; continue }
                    out += parse()
                }
            }
            val from = i
            while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++
            if (i == from) { i++; return null }
            return s.substring(from, i).toDoubleOrNull()
        }

        private fun skip() { while (i < s.length && s[i].isWhitespace()) i++ }
    }
}
