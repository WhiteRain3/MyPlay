package net.prototux.aare

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The connection log shown in the app. Kept on disk, so what happened in the car (a wireless
 * attempt, a refused certificate…) can still be read on the phone afterwards. Thread-safe.
 */
object DiagLog {
    private const val TAG = "Aare"
    // MyPlay: 1000 (AARE keeps 300), so the car's button lines (car/CarInputLog, at most 120 per
    // connection) cannot push out the certificate, Wi-Fi and connection lines of the same drive.
    private const val MAX_LINES = 1000
    /** The file is cut back to [MAX_LINES] lines once it is larger than this; well above what they take. */
    private const val REWRITE_ABOVE_BYTES = 512 * 1024

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines
    private var file: File? = null

    /** Loads the saved log. Called by [AareApp]; lines added before go to logcat only. */
    @Synchronized
    fun init(context: Context) {
        if (file != null) return
        val f = File(context.filesDir, "connection.log")
        file = f
        _lines.value = runCatching { f.readLines() }.getOrDefault(emptyList()).takeLast(MAX_LINES)
    }

    @Synchronized
    fun add(line: String) {
        Log.i(TAG, line)
        val stamp = SimpleDateFormat("dd/MM HH:mm:ss", Locale.US).format(Date())
        val all = (_lines.value + "$stamp  $line").takeLast(MAX_LINES)
        _lines.value = all
        val f = file ?: return
        runCatching {
            // Rewrite now and then to drop old lines; append otherwise.
            if (all.size == MAX_LINES && f.length() > REWRITE_ABOVE_BYTES) f.writeText(all.joinToString("\n", postfix = "\n"))
            else f.appendText("$stamp  $line\n")
        }
    }

    @Synchronized
    fun clear() {
        _lines.value = emptyList()
        file?.let { runCatching { it.writeText("") } }
    }
}
