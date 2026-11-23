package net.prototux.aare.speedlimit

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * MyPlay (plan N19): the speed limit file, format 1. Built from OpenStreetMap by the CI job
 * (Build.kt); read by the car side ([SpeedLimitFile], [Matcher]); downloaded by the phone (which
 * checks the first five bytes: "MPSL" and the format).
 *
 * Little-endian throughout:
 * ```
 *  0  "MPSL", u8 format = 1, u8 0, u16 0
 *  8  i32 originLat, i32 originLon, i32 tile        (all in 1e-5 degrees; a tile is tile × tile)
 * 20  u16 rows, u16 cols
 * 24  rows × cols × (u32 offset, u32 count)         the pieces of each tile, row by row from the south-west
 *     then the pieces: u8 limit (km/h), u8 winter limit (0: none), u8 flags, u8 road class, u8 n,
 *     n × (i16 lat, i16 lon) from the tile's south-west corner (1e-5 degrees)
 * ```
 * A piece is a short stretch of one road with one limit; it is stored in every tile its box
 * touches. Roads with no known limit are left out (the car then shows "no limit known").
 */
object SpeedLimitFormat {
    const val FORMAT = 1
    val MAGIC = byteArrayOf('M'.code.toByte(), 'P'.code.toByte(), 'S'.code.toByte(), 'L'.code.toByte())
    const val HEADER = 24
    /** 0.05 degrees: about 5.5 km north to south. */
    const val TILE_E5 = 5_000
    /** A piece's points stay within this of its tile's corner (fits in an i16 with room to spare). */
    const val MAX_SPAN_E5 = 2 * TILE_E5
    const val MAX_POINTS = 32

    /** Piece flags. */
    const val FORWARD_ONLY = 1
    const val BACKWARD_ONLY = 2
    /** The limit comes from the road's kind or zone, not a sign tagged on it. */
    const val DEFAULTED = 4
    /**
     * Option A (docs/ideas/speed-limits.md): no limit is tagged at all; this one is Lithuania's
     * standard for the road's type and whether it is in a settlement, an estimate. Always set
     * with [DEFAULTED], so a reader that does not know this bit treats it as defaulted.
     */
    const val GUESSED = 8
}

/** One stretch of road with one limit; points in 1e-5 degrees. */
data class Piece(
    val limit: Int,
    val winterLimit: Int,
    val flags: Int,
    val roadClass: Int,
    val lats: IntArray,
    val lons: IntArray,
) {
    init {
        require(lats.size == lons.size && lats.size >= 2) { "a piece needs two points or more" }
        require(limit in 1..250 && winterLimit in 0..250) { "limit out of range" }
    }

    /** The limit on [month] (1..12): Lithuania's winter limits apply November to March. */
    fun limitIn(month: Int): Int = if (winterLimit > 0 && (month >= 11 || month <= 3)) winterLimit else limit

    override fun equals(other: Any?) = other is Piece && limit == other.limit && winterLimit == other.winterLimit &&
        flags == other.flags && roadClass == other.roadClass && lats.contentEquals(other.lats) && lons.contentEquals(other.lons)
    override fun hashCode() = 31 * lats.contentHashCode() + lons.contentHashCode()
}

/** Writes a file from [pieces]. */
object SpeedLimitWriter {
    fun write(pieces: List<Piece>): ByteArray {
        val tile = SpeedLimitFormat.TILE_E5
        val all = pieces
        require(all.isNotEmpty()) { "no pieces" }
        val minLat = Math.floorDiv(all.minOf { it.lats.min() }, tile) * tile
        val minLon = Math.floorDiv(all.minOf { it.lons.min() }, tile) * tile
        val maxLat = all.maxOf { it.lats.max() }
        val maxLon = all.maxOf { it.lons.max() }
        val rows = (maxLat - minLat) / tile + 1
        val cols = (maxLon - minLon) / tile + 1
        require(rows in 1..65535 && cols in 1..65535) { "area too large" }
        val tiles = Array(rows * cols) { ArrayList<Piece>() }
        for (p in all) {
            val r0 = (p.lats.min() - minLat) / tile; val r1 = (p.lats.max() - minLat) / tile
            val c0 = (p.lons.min() - minLon) / tile; val c1 = (p.lons.max() - minLon) / tile
            for (r in r0..r1) for (c in c0..c1) tiles[r * cols + c] += p
        }
        val body = ByteArrayOutputStream()
        val index = IntArray(rows * cols * 2)
        val bodyStart = SpeedLimitFormat.HEADER + rows * cols * 8
        for ((i, list) in tiles.withIndex()) {
            index[2 * i] = bodyStart + body.size()
            index[2 * i + 1] = list.size
            val r = i / cols; val c = i % cols
            val lat0 = minLat + r * tile; val lon0 = minLon + c * tile
            for (p in list) {
                body.write(p.limit); body.write(p.winterLimit); body.write(p.flags); body.write(p.roadClass); body.write(p.lats.size)
                for (k in p.lats.indices) {
                    val dLat = p.lats[k] - lat0; val dLon = p.lons[k] - lon0
                    require(dLat in Short.MIN_VALUE..Short.MAX_VALUE && dLon in Short.MIN_VALUE..Short.MAX_VALUE) { "piece too long for its tile" }
                    body.writeShortLe(dLat); body.writeShortLe(dLon)
                }
            }
        }
        val out = ByteBuffer.allocate(bodyStart + body.size()).order(ByteOrder.LITTLE_ENDIAN)
        out.put(SpeedLimitFormat.MAGIC).put(SpeedLimitFormat.FORMAT.toByte()).put(0).putShort(0)
        out.putInt(minLat).putInt(minLon).putInt(tile).putShort(rows.toShort()).putShort(cols.toShort())
        for (v in index) out.putInt(v)
        out.put(body.toByteArray())
        return out.array()
    }

    private fun ByteArrayOutputStream.writeShortLe(v: Int) { write(v and 0xFF); write((v shr 8) and 0xFF) }
}

/**
 * Reads a file; throws on one that is not format 1. [buffer] can be a memory-mapped file, so a
 * large file is not read into memory. Thread-safe: every read is by absolute position.
 */
class SpeedLimitFile(buffer: ByteBuffer) {
    constructor(bytes: ByteArray) : this(ByteBuffer.wrap(bytes))

    // asReadOnlyBuffer() resets the order to big-endian, so the order is set after it.
    private val buf = buffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
    private val size = buf.limit()
    val originLat: Int
    val originLon: Int
    val tile: Int
    val rows: Int
    val cols: Int

    init {
        require(size >= SpeedLimitFormat.HEADER) { "too short" }
        require((0 until 4).all { buf.get(it) == SpeedLimitFormat.MAGIC[it] } && buf.get(4).toInt() == SpeedLimitFormat.FORMAT) { "not a speed limit file of format 1" }
        originLat = buf.getInt(8); originLon = buf.getInt(12); tile = buf.getInt(16)
        rows = buf.getShort(20).toInt() and 0xFFFF; cols = buf.getShort(22).toInt() and 0xFFFF
        require(tile > 0 && size.toLong() >= SpeedLimitFormat.HEADER + rows.toLong() * cols * 8) { "bad index" }
    }

    /** The pieces stored in the tile holding (lat, lon) and the tiles around it (1e-5 degrees). */
    fun piecesNear(lat: Int, lon: Int): List<Piece> {
        val r = Math.floorDiv(lat - originLat, tile)
        val c = Math.floorDiv(lon - originLon, tile)
        val out = LinkedHashSet<Piece>()
        for (dr in -1..1) for (dc in -1..1) out += tilePieces(r + dr, c + dc)
        return out.toList()
    }

    fun tilePieces(r: Int, c: Int): List<Piece> {
        if (r !in 0 until rows || c !in 0 until cols) return emptyList()
        val i = r * cols + c
        var at = buf.getInt(SpeedLimitFormat.HEADER + 8 * i)
        val count = buf.getInt(SpeedLimitFormat.HEADER + 8 * i + 4)
        require(at >= SpeedLimitFormat.HEADER && count >= 0 && at.toLong() + count * 9L <= size) { "bad tile" }
        val lat0 = originLat + r * tile; val lon0 = originLon + c * tile
        val out = ArrayList<Piece>(count)
        repeat(count) {
            val limit = buf.get(at).toInt() and 0xFF
            val winter = buf.get(at + 1).toInt() and 0xFF
            val flags = buf.get(at + 2).toInt() and 0xFF
            val cls = buf.get(at + 3).toInt() and 0xFF
            val n = buf.get(at + 4).toInt() and 0xFF
            at += 5
            val lats = IntArray(n); val lons = IntArray(n)
            for (k in 0 until n) { lats[k] = lat0 + buf.getShort(at); lons[k] = lon0 + buf.getShort(at + 2); at += 4 }
            out += Piece(limit, winter, flags, cls, lats, lons)
        }
        return out
    }
}
