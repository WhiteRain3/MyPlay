package net.prototux.aare.speedlimit

import java.io.File
import java.security.MessageDigest
import java.time.LocalDate

/**
 * MyPlay (plan N19): Lithuania's speed limits from OpenStreetMap tags. Conservative on purpose:
 * a road whose limit is not tagged and not implied by its kind is left out, so the car shows
 * "no limit known" rather than a guess.
 */
object LithuaniaRules {
    /** Lithuania's limits by zone ("LT:urban" and so on, as OpenStreetMap writes them). */
    private val ZONES = mapOf("lt:urban" to 50, "lt:rural" to 90, "lt:motorway" to 130, "lt:living_street" to 20)
    /** November to March, motorways and expressways: at most 110. */
    const val WINTER_FAST_ROADS = 110

    /** Roads cars drive on, by class (0 motorway .. 5 the rest). Anything else is not a road for cars. */
    fun roadClass(highway: String?): Int? = when (highway?.removeSuffix("_link")) {
        "motorway" -> 0
        "trunk" -> 1
        "primary" -> 2
        "secondary" -> 3
        "tertiary" -> 4
        "unclassified", "residential", "living_street", "service", "road" -> 5
        else -> null
    }

    /** A limit from a maxspeed value: a number (km/h, or mph converted), or a Lithuanian zone. */
    fun parse(value: String?): Int? {
        val v = value?.trim()?.lowercase() ?: return null
        ZONES[v]?.let { return it }
        Regex("""^(\d{1,3})\s*(km/h|kmh|kph)?$""").find(v)?.let { return it.groupValues[1].toInt().takeIf { n -> n in 5..250 } }
        Regex("""^(\d{1,3})\s*mph$""").find(v)?.let { return Math.round(it.groupValues[1].toInt() * 1.609344).toInt().takeIf { n -> n in 5..250 } }
        return null
    }

    /** A winter limit from maxspeed:conditional, such as "110 @ (Nov 1-Mar 31)". */
    fun winterFrom(conditional: String?): Int? {
        val m = Regex("""^\s*(\d{1,3})\s*@\s*\((.*)\)""").find(conditional ?: return null) ?: return null
        val months = m.groupValues[2].lowercase()
        return if (listOf("nov", "dec", "jan", "feb", "mar").any { it in months }) m.groupValues[1].toInt() else null
    }

    /** One limit for one direction of a way (flags as [SpeedLimitFormat]). */
    data class Rule(val limit: Int, val winter: Int, val flags: Int)

    /** Option A: KET's standard limits (cars): in a settlement, outside one, outside on an unpaved road, an expressway. */
    const val URBAN = 50
    const val RURAL = 90
    const val RURAL_UNPAVED = 70
    const val EXPRESSWAY = 120
    private val UNPAVED = setOf("unpaved", "gravel", "fine_gravel", "compacted", "dirt", "earth", "ground", "grass", "sand", "mud", "pebblestone", "woodchips")

    /**
     * Option A: the standard limit for a road with no limit tagged at all, or null (a service road:
     * parking aisles and driveways would only add noise). [inSettlement] is asked only when needed.
     */
    fun standard(tags: Map<String, String>, inSettlement: () -> Boolean): Int? {
        val highway = tags["highway"]?.removeSuffix("_link") ?: return null
        if (highway == "service" || roadClass(highway) == null) return null
        if (tags["motorroad"] == "yes") return EXPRESSWAY
        // A residential street outside any mapped area is still most likely inside the town signs.
        if (highway == "residential") return URBAN
        if (inSettlement()) return URBAN
        return if (tags["surface"]?.lowercase() in UNPAVED) RURAL_UNPAVED else RURAL
    }

    /**
     * The limits of a way with [tags], one per direction it applies to; empty when not known.
     * [fill] (option A): a road with nothing tagged gets its [standard] limit, marked GUESSED.
     */
    fun rules(tags: Map<String, String>, fill: Boolean = false, inSettlement: () -> Boolean = { false }): List<Rule> {
        val highway = tags["highway"]
        val cls = roadClass(highway) ?: return emptyList()
        var defaulted = false
        var guessed = false
        val forward = parse(tags["maxspeed:forward"])
        val backward = parse(tags["maxspeed:backward"])
        val main = parse(tags["maxspeed"]) ?: listOf("maxspeed:type", "source:maxspeed", "zone:maxspeed")
            .firstNotNullOfOrNull { parse(tags[it]) }?.also { defaulted = true }
            ?: when (highway) {
                "motorway" -> 130
                "living_street" -> 20
                else -> null
            }?.also { defaulted = true }
            ?: (if (fill && forward == null && backward == null) standard(tags, inSettlement) else null)?.also { defaulted = true; guessed = true }
        val winterTag = winterFrom(tags["maxspeed:conditional"])
        fun winterOf(limit: Int): Int = winterTag ?: if (cls <= 1 && limit > WINTER_FAST_ROADS) WINTER_FAST_ROADS else 0
        val base = (if (defaulted) SpeedLimitFormat.DEFAULTED else 0) or (if (guessed) SpeedLimitFormat.GUESSED else 0)
        if (forward != null || backward != null) {
            return listOfNotNull(
                (forward ?: main)?.let { Rule(it, winterOf(it), SpeedLimitFormat.FORWARD_ONLY or if (forward == null) base else 0) },
                (backward ?: main)?.let { Rule(it, winterOf(it), SpeedLimitFormat.BACKWARD_ONLY or if (backward == null) base else 0) },
            )
        }
        val limit = main ?: return emptyList()
        val oneway = when (tags["oneway"]) {
            "yes", "true", "1" -> SpeedLimitFormat.FORWARD_ONLY
            "-1", "reverse" -> SpeedLimitFormat.BACKWARD_ONLY
            else -> if (highway == "motorway" || highway == "motorway_link" || tags["junction"] == "roundabout") SpeedLimitFormat.FORWARD_ONLY else 0
        }
        return listOf(Rule(limit, winterOf(limit), base or oneway))
    }
}

/**
 * Reads ways from osmium's OPL text with node locations
 * (`osmium add-locations-to-ways -f opl`): `w<id> ... T<tags> N<n1>x<lon>y<lat>,...`.
 */
object Opl {
    data class Way(val tags: Map<String, String>, val lats: IntArray, val lons: IntArray)

    fun way(line: String): Way? {
        if (!line.startsWith("w")) return null
        var tags = emptyMap<String, String>()
        val lats = ArrayList<Int>(); val lons = ArrayList<Int>()
        for (field in line.split(' ')) {
            when {
                field.startsWith("T") -> tags = parseTags(field.substring(1))
                field.startsWith("N") -> for (node in field.substring(1).split(',')) {
                    val x = node.indexOf('x'); val y = node.indexOf('y')
                    if (x < 0 || y < 0) continue // a node without a location (outside the extract)
                    val lon = node.substring(x + 1, y).toDoubleOrNull() ?: continue
                    val lat = node.substring(y + 1).toDoubleOrNull() ?: continue
                    lats += Math.round(lat * 1e5).toInt(); lons += Math.round(lon * 1e5).toInt()
                }
            }
        }
        if (lats.size < 2) return null
        return Way(tags, lats.toIntArray(), lons.toIntArray())
    }

    /** "k=v,k2=v2", with OPL's escapes (%hex% for a character). */
    fun parseTags(s: String): Map<String, String> = if (s.isEmpty()) emptyMap() else s.split(',').mapNotNull { kv ->
        val i = kv.indexOf('=')
        if (i <= 0) null else unescape(kv.substring(0, i)) to unescape(kv.substring(i + 1))
    }.toMap()

    fun unescape(s: String): String {
        if ('%' !in s) return s
        val out = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '%') {
                val end = s.indexOf('%', i + 1)
                if (end > i) {
                    s.substring(i + 1, end).toIntOrNull(16)?.let { out.appendCodePoint(it) }
                    i = end + 1
                    continue
                }
            }
            out.append(c); i++
        }
        return out.toString()
    }
}

/** Cuts a way into [Piece]s short enough for the file: at most MAX_POINTS points, within MAX_SPAN_E5. */
object Pieces {
    /** [settlements] (option A): fill in standard limits, urban where the way's middle is inside one; null: as before, tagged limits only. */
    fun of(way: Opl.Way, settlements: Settlements? = null): List<Piece> {
        val rules = LithuaniaRules.rules(way.tags, fill = settlements != null) {
            val m = way.lats.size / 2
            settlements?.contains(way.lats[m], way.lons[m]) == true
        }
        if (rules.isEmpty()) return emptyList()
        val cls = LithuaniaRules.roadClass(way.tags["highway"]) ?: return emptyList()
        // Long straight segments get points in between, so no piece outgrows its tile.
        val lats = ArrayList<Int>(); val lons = ArrayList<Int>()
        for (k in way.lats.indices) {
            if (k > 0) {
                val dLat = way.lats[k] - way.lats[k - 1]; val dLon = way.lons[k] - way.lons[k - 1]
                val steps = (maxOf(Math.abs(dLat), Math.abs(dLon)) / SpeedLimitFormat.TILE_E5)
                for (s in 1..steps) {
                    lats += way.lats[k - 1] + dLat * s / (steps + 1); lons += way.lons[k - 1] + dLon * s / (steps + 1)
                }
            }
            lats += way.lats[k]; lons += way.lons[k]
        }
        val chunks = ArrayList<Pair<IntArray, IntArray>>()
        var start = 0
        while (start < lats.size - 1) {
            var end = start + 1
            var minLat = minOf(lats[start], lats[end]); var maxLat = maxOf(lats[start], lats[end])
            var minLon = minOf(lons[start], lons[end]); var maxLon = maxOf(lons[start], lons[end])
            while (end + 1 < lats.size && end + 1 - start < SpeedLimitFormat.MAX_POINTS) {
                val nLat = lats[end + 1]; val nLon = lons[end + 1]
                if (maxOf(maxLat, nLat) - minOf(minLat, nLat) > SpeedLimitFormat.MAX_SPAN_E5 ||
                    maxOf(maxLon, nLon) - minOf(minLon, nLon) > SpeedLimitFormat.MAX_SPAN_E5) break
                end++
                minLat = minOf(minLat, nLat); maxLat = maxOf(maxLat, nLat); minLon = minOf(minLon, nLon); maxLon = maxOf(maxLon, nLon)
            }
            chunks += lats.subList(start, end + 1).toIntArray() to lons.subList(start, end + 1).toIntArray()
            start = end // the next piece starts where this one ends
        }
        return rules.flatMap { r -> chunks.map { (la, lo) -> Piece(r.limit, r.winter, r.flags, cls, la, lo) } }
    }
}

/**
 * Option A: settlement areas (place=city|town|village|hamlet, administrative settlement boundaries,
 * landuse=residential|commercial|retail|industrial), from `osmium export -f geojsonseq`: each line
 * a feature with a Polygon or MultiPolygon. Only outer rings count (a hole in a town is rare and
 * still most likely inside its signs). Points are 1e-5 degrees, as everywhere here. Indexed by
 * tile, so a lookup tests only the rings whose box touches its tile.
 */
class Settlements(rings: List<Ring>) {
    class Ring(val lats: IntArray, val lons: IntArray) {
        val minLat = lats.min(); val maxLat = lats.max(); val minLon = lons.min(); val maxLon = lons.max()

        /** Ray casting; on the edge counts either way. */
        fun contains(lat: Int, lon: Int): Boolean {
            if (lat < minLat || lat > maxLat || lon < minLon || lon > maxLon) return false
            var inside = false
            var j = lats.size - 1
            for (i in lats.indices) {
                val yi = lats[i].toLong(); val yj = lats[j].toLong(); val xi = lons[i].toLong(); val xj = lons[j].toLong()
                if ((yi > lat) != (yj > lat)) {
                    // x of the edge at lat, compared without dividing: (lon - xi) * (yj - yi) < (xj - xi) * (lat - yi)
                    val lhs = (lon - xi) * (yj - yi); val rhs = (xj - xi) * (lat - yi)
                    if (if (yj > yi) lhs < rhs else lhs > rhs) inside = !inside
                }
                j = i
            }
            return inside
        }
    }

    private val tiles = HashMap<Long, MutableList<Ring>>()
    val size = rings.size

    init {
        for (r in rings) for (t in tilesOf(r.minLat, r.maxLat, r.minLon, r.maxLon)) tiles.getOrPut(t) { ArrayList() } += r
    }

    fun contains(lat: Int, lon: Int): Boolean = tiles[key(lat / TILE, lon / TILE)]?.any { it.contains(lat, lon) } == true

    private fun tilesOf(minLat: Int, maxLat: Int, minLon: Int, maxLon: Int) = buildList {
        for (r in minLat / TILE..maxLat / TILE) for (c in minLon / TILE..maxLon / TILE) add(key(r, c))
    }

    private fun key(r: Int, c: Int) = (r.toLong() shl 32) or (c.toLong() and 0xFFFFFFFFL)

    companion object {
        /** 0.02 degrees. */
        const val TILE = 2_000

        /** The outer rings of one GeoJSON feature line (a Polygon or MultiPolygon); none for anything else. */
        fun ringsOf(line: String): List<Ring> {
            val at = line.indexOf("\"coordinates\"")
            if (at < 0) return emptyList()
            val multi = line.indexOf("\"MultiPolygon\"") >= 0
            val poly = multi || line.indexOf("\"Polygon\"") >= 0
            if (!poly) return emptyList()
            // Walk the nested arrays: a polygon is depth 1 in a Polygon, 2 in a MultiPolygon; its rings
            // are one deeper and their points one deeper again. A polygon's first ring is its outer one.
            val polyDepth = if (multi) 2 else 1
            val ringDepth = polyDepth + 1
            val out = ArrayList<Ring>()
            var depth = 0
            var ringIndex = -1
            val lats = ArrayList<Int>(); val lons = ArrayList<Int>()
            var i = line.indexOf('[', at)
            while (i in line.indices) {
                when (line[i]) {
                    '[' -> {
                        depth++
                        when (depth) {
                            polyDepth -> ringIndex = -1
                            ringDepth -> { ringIndex++; lats.clear(); lons.clear() }
                            ringDepth + 1 -> {
                                val end = line.indexOf(']', i)
                                val nums = line.substring(i + 1, end).split(',')
                                val lon = nums.getOrNull(0)?.trim()?.toDoubleOrNull(); val lat = nums.getOrNull(1)?.trim()?.toDoubleOrNull()
                                if (lon != null && lat != null && ringIndex == 0) { lats += Math.round(lat * 1e5).toInt(); lons += Math.round(lon * 1e5).toInt() }
                                depth--
                                i = end
                            }
                        }
                    }
                    ']' -> {
                        if (depth == ringDepth && ringIndex == 0 && lats.size >= 4) out += Ring(lats.toIntArray(), lons.toIntArray())
                        depth--
                        if (depth == 0) break
                    }
                }
                i++
            }
            return out
        }

        fun read(file: File): Settlements = Settlements(file.bufferedReader().useLines { lines -> lines.flatMap { ringsOf(it) }.toList() })
    }
}

/** Option A: how many ways of each class have a tagged limit, a defaulted one, a guessed one, or none, for the CI log. */
class Coverage {
    private val counts = Array(6) { IntArray(4) }

    fun add(cls: Int?, rules: List<LithuaniaRules.Rule>) {
        if (cls == null) return
        val f = rules.firstOrNull()?.flags
        val col = when {
            f == null -> 3
            f and SpeedLimitFormat.GUESSED != 0 -> 2
            f and SpeedLimitFormat.DEFAULTED != 0 -> 1
            else -> 0
        }
        counts[cls][col]++
    }

    fun table(): String = buildString {
        append("class        tagged  default  guessed     none\n")
        val names = listOf("motorway", "trunk", "primary", "secondary", "tertiary", "other")
        for ((c, row) in counts.withIndex()) append("%-10s %8d %8d %8d %8d\n".format(names[c], row[0], row[1], row[2], row[3]))
    }
}

/**
 * The CI job's tool: `myplay-speedlimit <region> <roads.opl> <out dir> [built yyyy-mm-dd] [settlements.geojsonseq]`
 * writes `speed-limits-<region>.bin` and its `.json` (what the phone checks before it keeps the file).
 * With the settlements file (option A), roads with no tagged limit get Lithuania's standard one,
 * marked GUESSED; without it, as before. A per-class coverage table goes to the log.
 */
fun main(args: Array<String>) {
    require(args.size >= 3) { "usage: myplay-speedlimit <region> <roads.opl> <out dir> [built yyyy-mm-dd] [settlements.geojsonseq]" }
    val region = args[0]
    require(region.matches(Regex("[a-z0-9_-]+"))) { "not a region id" }
    val settlements = args.getOrNull(4)?.let { Settlements.read(File(it)) }
    settlements?.let { println("$region: ${it.size} settlement rings") }
    val pieces = ArrayList<Piece>()
    val coverage = Coverage()
    var ways = 0
    File(args[1]).bufferedReader().useLines { lines ->
        for (line in lines) {
            val way = Opl.way(line) ?: continue
            ways++
            val wayPieces = Pieces.of(way, settlements)
            pieces += wayPieces
            val cls = LithuaniaRules.roadClass(way.tags["highway"])
            coverage.add(cls, if (wayPieces.isEmpty()) emptyList() else listOf(LithuaniaRules.Rule(0, 0, wayPieces.first().flags)))
        }
    }
    val bytes = SpeedLimitWriter.write(pieces)
    val out = File(args[2]).apply { mkdirs() }
    File(out, "speed-limits-$region.bin").writeBytes(bytes)
    val built = args.getOrNull(3) ?: LocalDate.now().toString()
    File(out, "speed-limits-$region.json").writeText(Manifest.json(region, built, bytes))
    println("$region: $ways ways, ${pieces.size} pieces, ${bytes.size} bytes")
    print(coverage.table())
}

/** The .json beside a file, as the phone reads it (SpeedLimitManifest). */
object Manifest {
    const val SOURCE = "OpenStreetMap contributors, via Geofabrik's extract"
    const val LICENCE_URL = "https://www.openstreetmap.org/copyright"

    fun json(region: String, built: String, bytes: ByteArray): String {
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        return """{"region":"$region","format":${SpeedLimitFormat.FORMAT},"built":"$built","bytes":${bytes.size},""" +
            """"sha256":"$sha","source":"$SOURCE","url_licence":"$LICENCE_URL","licence":"ODbL 1.0"}""" + "\n"
    }
}
