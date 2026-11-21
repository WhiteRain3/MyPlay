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

    /** The limits of a way with [tags], one per direction it applies to; empty when not known. */
    fun rules(tags: Map<String, String>): List<Rule> {
        val highway = tags["highway"]
        val cls = roadClass(highway) ?: return emptyList()
        var defaulted = false
        val main = parse(tags["maxspeed"]) ?: listOf("maxspeed:type", "source:maxspeed", "zone:maxspeed")
            .firstNotNullOfOrNull { parse(tags[it]) }?.also { defaulted = true }
            ?: when (highway) {
                "motorway" -> 130
                "living_street" -> 20
                else -> null
            }?.also { defaulted = true }
        val forward = parse(tags["maxspeed:forward"])
        val backward = parse(tags["maxspeed:backward"])
        val winterTag = winterFrom(tags["maxspeed:conditional"])
        fun winterOf(limit: Int): Int = winterTag ?: if (cls <= 1 && limit > WINTER_FAST_ROADS) WINTER_FAST_ROADS else 0
        val base = if (defaulted) SpeedLimitFormat.DEFAULTED else 0
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
    fun of(way: Opl.Way): List<Piece> {
        val rules = LithuaniaRules.rules(way.tags)
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
 * The CI job's tool: `myplay-speedlimit <region> <roads.opl> <out dir> [built yyyy-mm-dd]` writes
 * `speed-limits-<region>.bin` and its `.json` (what the phone checks before it keeps the file).
 */
fun main(args: Array<String>) {
    require(args.size >= 3) { "usage: myplay-speedlimit <region> <roads.opl> <out dir> [built yyyy-mm-dd]" }
    val region = args[0]
    require(region.matches(Regex("[a-z0-9_-]+"))) { "not a region id" }
    val pieces = ArrayList<Piece>()
    var ways = 0
    File(args[1]).bufferedReader().useLines { lines ->
        for (line in lines) {
            val way = Opl.way(line) ?: continue
            ways++
            pieces += Pieces.of(way)
        }
    }
    val bytes = SpeedLimitWriter.write(pieces)
    val out = File(args[2]).apply { mkdirs() }
    File(out, "speed-limits-$region.bin").writeBytes(bytes)
    val built = args.getOrNull(3) ?: LocalDate.now().toString()
    File(out, "speed-limits-$region.json").writeText(Manifest.json(region, built, bytes))
    println("$region: $ways ways, ${pieces.size} pieces, ${bytes.size} bytes")
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
