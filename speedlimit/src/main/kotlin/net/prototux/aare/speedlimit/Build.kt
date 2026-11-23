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
                // The dispatcher's rule for every default: when in doubt, lower. 110 is the motorway's
                // winter limit, never above the real one; a sign tagged on the road says more.
                "motorway" -> LOWEST_FAST
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
        return listOf(Rule(limit, winterOf(limit), base or oneway(tags)))
    }

    /** The direction flag of a way that has one limit both ways. */
    fun oneway(tags: Map<String, String>): Int {
        val highway = tags["highway"]
        return when (tags["oneway"]) {
            "yes", "true", "1" -> SpeedLimitFormat.FORWARD_ONLY
            "-1", "reverse" -> SpeedLimitFormat.BACKWARD_ONLY
            else -> if (highway == "motorway" || highway == "motorway_link" || tags["junction"] == "roundabout") SpeedLimitFormat.FORWARD_ONLY else 0
        }
    }

    /**
     * Option A's defaults, by the rules of the road (KET). The rule for all of them: when in doubt,
     * the lower one, never a limit higher than may be real (docs/ideas/speed-limits.md).
     * Outside settlements on gravel or earth: 70.
     */
    const val UNPAVED = 70
    const val URBAN = 50
    const val RURAL = 90
    /**
     * An untagged expressway (motorroad=yes) or motorway: 110, the lowest these roads have (their
     * winter limit), so a default is never above the real limit (SL-2, SL-3).
     */
    const val LOWEST_FAST = 110

    /**
     * The winter limit (November to March) for a default [limit] on a road of class [cls] with
     * [tags]: motorways and expressways (whatever their class, SL-2) at most 110.
     */
    fun defaultWinter(limit: Int, cls: Int, tags: Map<String, String>): Int =
        if ((cls <= 1 || tags["motorroad"] == "yes") && limit > WINTER_FAST_ROADS) WINTER_FAST_ROADS else 0

    /** OpenStreetMap's surfaces that are not asphalt, concrete or paving. */
    private val UNPAVED_SURFACES = setOf(
        "unpaved", "gravel", "fine_gravel", "compacted", "dirt", "earth", "ground", "grass", "sand",
        "mud", "pebblestone", "rock", "woodchips", "grass_paver",
    )

    /**
     * Option A: the limit the rules of the road (KET) give a road that has no limit tagged and none
     * implied by its kind ([rules] empty), at a point [inSettlement] or not. Null where there is no
     * sensible default: service roads (parking aisles, driveways) and anything not for cars.
     */
    fun defaultLimit(tags: Map<String, String>, inSettlement: Boolean): Int? {
        val highway = tags["highway"]?.removeSuffix("_link")
        if (roadClass(highway) == null || highway == "service") return null
        // SL-3: a motorway (or its link) with nothing tagged: the motorway's limit, wherever it is.
        if (highway == "motorway") return LOWEST_FAST
        if (inSettlement) return URBAN
        // A residential street outside any mapped settlement area: most likely inside the town signs.
        if (highway == "residential") return URBAN
        if (tags["surface"]?.lowercase() in UNPAVED_SURFACES) return UNPAVED
        if (tags["motorroad"] == "yes") return LOWEST_FAST
        return RURAL
    }

    /** Where a way's limit comes from, for the build's coverage table. */
    enum class Source { SIGNED, KIND, AREA, NONE }
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
    /**
     * The pieces of [way]. Its tagged (or kind's) limits first; otherwise, when [settlements] are
     * given (option A), Lithuania's default at each piece's middle (50 inside a settlement, 90
     * outside, 70 unpaved), marked DEFAULTED.
     */
    fun of(way: Opl.Way, settlements: Settlements? = null): List<Piece> {
        val rules = LithuaniaRules.rules(way.tags)
        val cls = LithuaniaRules.roadClass(way.tags["highway"]) ?: return emptyList()
        if (rules.isEmpty()) {
            if (settlements == null) return emptyList()
            // GUESSED (with DEFAULTED, for readers that know only that bit): an estimate, unlike the
            // legally defined zone and kind defaults; the car draws it dashed and can leave it out.
            val flags = SpeedLimitFormat.DEFAULTED or SpeedLimitFormat.GUESSED or LithuaniaRules.oneway(way.tags)
            return chunks(way).mapNotNull { (la, lo) ->
                val mid = la.size / 2
                val limit = LithuaniaRules.defaultLimit(way.tags, settlements.contains(la[mid], lo[mid])) ?: return@mapNotNull null
                val winter = LithuaniaRules.defaultWinter(limit, cls, way.tags)
                Piece(limit, winter, flags, cls, la, lo)
            }
        }
        return rules.flatMap { r -> chunks(way).map { (la, lo) -> Piece(r.limit, r.winter, r.flags, cls, la, lo) } }
    }

    /** Where [way]'s limit comes from (for the coverage table). */
    fun source(way: Opl.Way, settlements: Settlements?): LithuaniaRules.Source {
        val rules = LithuaniaRules.rules(way.tags)
        return when {
            rules.any { (it.flags and SpeedLimitFormat.DEFAULTED) == 0 } -> LithuaniaRules.Source.SIGNED
            rules.isNotEmpty() -> LithuaniaRules.Source.KIND
            settlements != null && LithuaniaRules.defaultLimit(way.tags, false) != null -> LithuaniaRules.Source.AREA
            else -> LithuaniaRules.Source.NONE
        }
    }

    /** [way]'s points, with points added on long segments, cut into chunks that fit their tiles. */
    private fun chunks(way: Opl.Way): List<Pair<IntArray, IntArray>> {
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
        return chunks
    }
}

/**
 * Joins pieces that continue one another (one ends where the next begins) and say the same thing
 * (limit, winter limit, flags, road class) into one, while the result still fits a piece (at most
 * MAX_POINTS points, within MAX_SPAN_E5): OpenStreetMap cuts roads into many short ways, and each
 * piece costs a header and repeats the point it shares. Direction is kept (a piece is only ever
 * followed by one that starts at its end), so one-way pieces stay right.
 */
object Joiner {
    private data class Key(val limit: Int, val winter: Int, val flags: Int, val roadClass: Int)

    fun join(pieces: List<Piece>): List<Piece> {
        val out = ArrayList<Piece>(pieces.size)
        for ((key, group) in pieces.groupBy { Key(it.limit, it.winterLimit, it.flags, it.roadClass) }) {
            fun start(p: Piece) = (p.lats.first().toLong() shl 32) or (p.lons.first().toLong() and 0xffffffffL)
            fun end(p: Piece) = (p.lats.last().toLong() shl 32) or (p.lons.last().toLong() and 0xffffffffL)
            val byStart = HashMap<Long, ArrayDeque<Int>>()
            group.forEachIndexed { i, p -> byStart.getOrPut(start(p)) { ArrayDeque() }.addLast(i) }
            val ends = group.mapTo(HashSet()) { end(it) }
            val used = BooleanArray(group.size)
            // Chains start where no piece of the group ends (then whatever is left: loops).
            val order = group.indices.sortedBy { if (start(group[it]) in ends) 1 else 0 }
            for (i in order) {
                if (used[i]) continue
                used[i] = true
                val lats = ArrayList<Int>(); val lons = ArrayList<Int>()
                group[i].lats.forEach { lats += it }; group[i].lons.forEach { lons += it }
                while (true) {
                    val at = (lats.last().toLong() shl 32) or (lons.last().toLong() and 0xffffffffL)
                    val queue = byStart[at] ?: break
                    while (queue.isNotEmpty() && used[queue.first()]) queue.removeFirst()
                    val j = queue.firstOrNull() ?: break
                    val next = group[j]
                    if (lats.size + next.lats.size - 1 > SpeedLimitFormat.MAX_POINTS) break
                    val la = lats + next.lats.drop(1); val lo = lons + next.lons.drop(1)
                    if (la.max() - la.min() > SpeedLimitFormat.MAX_SPAN_E5 || lo.max() - lo.min() > SpeedLimitFormat.MAX_SPAN_E5) break
                    used[j] = true
                    queue.removeFirst()
                    for (k in 1 until next.lats.size) { lats += next.lats[k]; lons += next.lons[k] }
                }
                out += Piece(key.limit, key.winter, key.flags, key.roadClass, lats.toIntArray(), lons.toIntArray())
            }
        }
        return out
    }
}

/**
 * The build's coverage table, by road class: how many ways have a signed limit, one from their
 * kind or zone, one from option A's defaults, or none. Printed to the CI log.
 */
class Coverage {
    private val counts = Array(6) { IntArray(LithuaniaRules.Source.entries.size) }

    fun add(roadClass: Int, source: LithuaniaRules.Source) { counts[roadClass][source.ordinal]++ }

    fun count(roadClass: Int, source: LithuaniaRules.Source) = counts[roadClass][source.ordinal]

    fun table(): String = buildString {
        val names = listOf("motorway", "trunk", "primary", "secondary", "tertiary", "minor")
        appendLine("class       ways    signed    kind    area    none   with a limit")
        for (c in 0..5) {
            val row = counts[c]
            val all = row.sum()
            val with = all - row[LithuaniaRules.Source.NONE.ordinal]
            val share = if (all == 0) 0 else Math.round(100.0 * with / all).toInt()
            appendLine(String.format(java.util.Locale.ROOT, "%-9s %7d %9d %7d %7d %7d   %3d %%", names[c], all, row[0], row[1], row[2], row[3], share))
        }
        // SL-4: what "area" means, so the log is read right.
        appendLine("area: a default by the rules of the road, an estimate marked \"probably\": 50 in a settlement")
        appendLine("(its area, or near its place point), 90 outside, 70 unpaved; a residential street is assumed")
        appendLine("to be in a settlement even outside any mapped one; service roads get none.")
    }
}

/**
 * The CI job's tool: `myplay-speedlimit <region> <roads.opl> <out dir> [built yyyy-mm-dd]` writes
 * `speed-limits-<region>.bin` and its `.json` (what the phone checks before it keeps the file).
 */
fun main(args: Array<String>) {
    require(args.size >= 3) { "usage: myplay-speedlimit <region> <roads.opl> <out dir> [built yyyy-mm-dd] [settlements.geojsonseq]" }
    val region = args[0]
    require(region.matches(Regex("[a-z0-9_-]+"))) { "not a region id" }
    // Option A: with the settlement areas, untagged roads get Lithuania's default limits.
    val settlements = args.getOrNull(4)?.let { Settlements.read(File(it)) }
    settlements?.let { println("$region: ${it.size} settlement areas, ${it.placeCount} settlement points") }
    val pieces = ArrayList<Piece>()
    val coverage = Coverage()
    var ways = 0
    File(args[1]).bufferedReader().useLines { lines ->
        for (line in lines) {
            val way = Opl.way(line) ?: continue
            ways++
            LithuaniaRules.roadClass(way.tags["highway"])?.let { coverage.add(it, Pieces.source(way, settlements)) }
            pieces += Pieces.of(way, settlements)
        }
    }
    print(coverage.table())
    val before = pieces.size
    val joined = Joiner.join(pieces)
    pieces.clear(); pieces += joined
    println("$region: $before pieces joined into ${pieces.size}")
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
