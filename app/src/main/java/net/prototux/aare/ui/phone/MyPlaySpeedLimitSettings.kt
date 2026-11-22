package net.prototux.aare.ui.phone

import androidx.core.content.edit
import net.prototux.aare.Settings
import java.io.File

/*
 * MyPlay (N21): the speed limit file on the phone. The car side (N20) reads the region through
 * [myplaySpeedLimitRegion] and the file through [SpeedLimitFiles.file].
 */

/**
 * The region whose speed limit file is kept ("" = none), e.g. "lithuania". Backed up. Only a region
 * MyPlay knows is ever returned: the value can come from a backup file, and it names a file.
 */
var Settings.myplaySpeedLimitRegion: String
    get() = SpeedLimitFiles.known(prefs.getString(KEY_SPEED_LIMIT_REGION, "") ?: "")
    set(v) {
        val next = SpeedLimitFiles.known(v)
        // #5: every change of region is in the connection log, to tell later what changed it.
        if (next != myplaySpeedLimitRegion) net.prototux.aare.DiagLog.add("speed limits: region set to ${next.ifEmpty { "none" }}")
        prefs.edit { putString(KEY_SPEED_LIMIT_REGION, next) }
    }

/** When the phone last looked for a newer file (ms since 1970), 0 never. Not backed up: it belongs to this phone. */
var Settings.myplaySpeedLimitChecked: Long
    get() = prefs.getLong(KEY_SPEED_LIMIT_CHECKED, 0)
    set(v) = prefs.edit { putLong(KEY_SPEED_LIMIT_CHECKED, v) }

/** Whether the file may also be downloaded on mobile data (the owner's choice; Wi-Fi only by default). Backed up. */
var Settings.myplaySpeedLimitMobile: Boolean
    get() = prefs.getBoolean(KEY_SPEED_LIMIT_MOBILE, false)
    set(v) = prefs.edit { putBoolean(KEY_SPEED_LIMIT_MOBILE, v) }

const val KEY_SPEED_LIMIT_MOBILE = "myplay_speed_limit_mobile"
const val KEY_SPEED_LIMIT_REGION = "myplay_speed_limit_region"
const val KEY_SPEED_LIMIT_CHECKED = "myplay_speed_limit_checked"

/** A region the phone offers, with Geofabrik's extract it is built from. */
data class SpeedLimitRegion(val id: String, val name: String)

/** Where the speed limit files live, and what is known about them. */
object SpeedLimitFiles {
    /** Only Lithuania for now (the owner's region). */
    val REGIONS = listOf(SpeedLimitRegion("lithuania", "Lithuania"))

    /** The file format this version of MyPlay reads (the .json's "format" and the byte after "MPSL"). */
    const val FORMAT = 1
    val MAGIC = byteArrayOf('M'.code.toByte(), 'P'.code.toByte(), 'S'.code.toByte(), 'L'.code.toByte())

    /** [region] if it is one of [REGIONS], else "" (none). */
    fun known(region: String): String = region.takeIf { r -> REGIONS.any { it.id == r } } ?: ""

    fun dir(filesDir: File) = File(filesDir, "speed_limits")
    /** The file the car side reads for [region] (a known region; anything else is refused). */
    fun file(filesDir: File, region: String) = File(dir(filesDir), "${safe(region)}.bin")
    /** Its description, kept beside it (size, date, licence). */
    fun manifestFile(filesDir: File, region: String) = File(dir(filesDir), "${safe(region)}.json")

    private fun safe(region: String): String {
        require(region.matches(Regex("[a-z0-9_-]+"))) { "not a region id" }
        return region
    }

    /** The release the files are published in (rolling, rebuilt monthly; not a myplay-v* tag, so the updater ignores it). */
    private const val DOWNLOADS = "https://github.com/WhiteRain3/MyPlay/releases/download/speed-limits/"
    fun manifestUrl(region: String) = "${DOWNLOADS}speed-limits-$region.json"
    fun fileUrl(region: String) = "${DOWNLOADS}speed-limits-$region.bin"

    /** Looked for again at most this often. */
    const val CHECK_EVERY_MS = 30L * 24 * 60 * 60 * 1000

    /** Larger than any real file by far: anything bigger is refused before it fills the phone. */
    const val MAX_BYTES = 200L * 1024 * 1024
}

/** What a region's .json says about its file. */
data class SpeedLimitManifest(
    val region: String, val format: Int, val built: String, val bytes: Long, val sha256: String, val source: String, val licenceUrl: String,
) {
    companion object {
        /** The manifest in [json], or null when it is not one this version can use. */
        fun parse(json: String?): SpeedLimitManifest? = runCatching {
            val o = org.json.JSONObject(json ?: return null)
            SpeedLimitManifest(
                o.getString("region"), o.getInt("format"), o.getString("built"), o.getLong("bytes"),
                o.getString("sha256").lowercase(), o.optString("source"), o.optString("url_licence"),
            )
        }.getOrNull()?.takeIf {
            it.format == SpeedLimitFiles.FORMAT && it.bytes in 1..SpeedLimitFiles.MAX_BYTES &&
                it.sha256.matches(Regex("[0-9a-f]{64}")) && it.built.matches(Regex("""\d{4}-\d{2}-\d{2}"""))
        }
    }
}

object SpeedLimitCheck {
    /** Whether to look for a newer file now: a region is chosen, and never looked or a month ago. */
    fun due(region: String, lastChecked: Long, now: Long): Boolean =
        region.isNotEmpty() && (lastChecked <= 0 || now - lastChecked >= SpeedLimitFiles.CHECK_EVERY_MS || now < lastChecked)

    /** Whether [remote] should replace what is kept ([local], null if nothing): newer, or nothing kept. */
    fun wanted(local: SpeedLimitManifest?, haveFile: Boolean, remote: SpeedLimitManifest): Boolean =
        !haveFile || local == null || remote.built > local.built

    /** Whether a try counts as the month's look: it reached the release page and got an answer. */
    fun counts(state: SpeedLimitDownload.State): Boolean = state == SpeedLimitDownload.State.Done ||
        state == SpeedLimitDownload.State.UpToDate || state == SpeedLimitDownload.State.NotPublished

    /** Whether [head] starts as a file of this format. */
    fun headerOk(head: ByteArray): Boolean =
        head.size >= 5 && head.copyOfRange(0, 4).contentEquals(SpeedLimitFiles.MAGIC) && head[4].toInt() == SpeedLimitFiles.FORMAT

    /** Whether a downloaded file is the one [m] describes: its size, its SHA-256 and its header. */
    fun verified(m: SpeedLimitManifest, size: Long, sha256: String, head: ByteArray): Boolean =
        size == m.bytes && sha256.equals(m.sha256, ignoreCase = true) && headerOk(head)
}
