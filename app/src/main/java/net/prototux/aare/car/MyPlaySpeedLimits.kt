package net.prototux.aare.car

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import net.prototux.aare.Settings
import net.prototux.aare.speedlimit.Matcher
import net.prototux.aare.speedlimit.SpeedLimitFile
import net.prototux.aare.ui.phone.SpeedLimitFiles
import net.prototux.aare.ui.phone.myplaySpeedLimitRegion
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.util.Calendar
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * MyPlay (plan N20): the speed limit where the car is, from the file the phone keeps for the
 * chosen region (ui/phone/MyPlaySpeedLimitSettings.kt). The file is memory-mapped, not read into
 * memory, and opened again when the phone replaces it. All the work is on one background thread;
 * answers come back on the main thread. Anything that goes wrong is "no limit known".
 */
class FileSpeedLimits(context: Context) : SpeedLimits {
    private val app = context.applicationContext
    private val settings = Settings(app)
    private val worker: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "MyPlaySpeedLimits").apply { isDaemon = true; priority = Thread.MIN_PRIORITY }
    }
    private val main = Handler(Looper.getMainLooper())

    // Only touched on the worker thread.
    private var opened: Triple<String, Long, Long>? = null
    private var matcher: Matcher? = null

    private fun regionFile(): File? {
        val region = settings.myplaySpeedLimitRegion
        if (region.isEmpty()) return null
        return SpeedLimitFiles.file(app.filesDir, region).takeIf { it.isFile && it.length() > 0 }
    }

    override fun available(): Boolean = runCatching { regionFile() != null }.getOrDefault(false)

    override fun file(): LimitFile = runCatching {
        when {
            settings.myplaySpeedLimitRegion.isEmpty() -> LimitFile.NO_REGION
            regionFile() == null -> LimitFile.NO_FILE
            else -> LimitFile.READY
        }
    }.getOrDefault(LimitFile.NO_FILE)

    override fun lookup(fix: PhoneFix, onResult: (LimitShown) -> Unit) {
        runCatching {
            worker.execute {
                val r = runCatching { match(fix) }.onFailure { Log.w(TAG, "speed limit lookup", it) }.getOrDefault(LimitShown.Unknown)
                main.post { onResult(r) }
            }
        } // refused after close(): nothing to answer
    }

    private fun match(fix: PhoneFix): LimitShown {
        val f = regionFile() ?: return LimitShown.Unknown
        val key = Triple(f.path, f.length(), f.lastModified())
        if (key != opened) {
            matcher = null
            opened = key
            val buffer = RandomAccessFile(f, "r").use { it.channel.map(FileChannel.MapMode.READ_ONLY, 0, it.length()) }
            matcher = Matcher(SpeedLimitFile(buffer))
        }
        val m = matcher ?: return LimitShown.Unknown
        val month = Calendar.getInstance().apply { timeInMillis = fix.atMillis }.get(Calendar.MONTH) + 1
        return when (val r = m.match(fix.lat, fix.lon, fix.bearingDeg, fix.accuracyM, month)) {
            is Matcher.Result.Known -> LimitShown.Known(r.limit, r.defaulted)
            Matcher.Result.Unknown -> LimitShown.Unknown
        }
    }

    override fun close() {
        worker.shutdownNow()
    }

    private companion object {
        const val TAG = "MyPlaySpeedLimits"
    }
}
