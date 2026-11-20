package net.prototux.aare.car

import aap_protobuf.service.ServiceOuterClass.Service
import aap_protobuf.service.inputsource.message.InputReportOuterClass.InputReport
import aap_protobuf.service.media.shared.message.InsetsOuterClass.Insets
import aap_protobuf.service.media.sink.message.VideoConfigurationOuterClass.VideoConfiguration
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** MyPlay (I4, R8): one thing the car's controls sent, as it came. */
sealed interface CarInputEvent {
    /** The car's input channel it came on. */
    val channel: Int
    /** SystemClock.elapsedRealtime() when it arrived. */
    val atMs: Long

    data class Key(override val channel: Int, val code: Int, val down: Boolean, val longPress: Boolean, override val atMs: Long) : CarInputEvent
    data class Turn(override val channel: Int, val code: Int, val delta: Int, override val atMs: Long) : CarInputEvent
    data class Absolute(override val channel: Int, val code: Int, val value: Int, override val atMs: Long) : CarInputEvent
    /** The car's touchpad is sending: once per session. */
    data class Touchpad(override val channel: Int, override val atMs: Long) : CarInputEvent
}

/**
 * MyPlay (I4, R8): the car's input as it arrives, for the phone's "Car check" page, which ticks
 * off each button and knob step while the owner presses them. Unlike [CarInputLog] (the connection
 * log), nothing is added up or capped, and the "log car input" setting does not gate it: nothing
 * is stored here, a collector sees only what arrives while it listens.
 *
 * [events] is emitted on the car's reader thread and never blocks it: when a collector is slow,
 * the oldest events are dropped. [report] is what the car said about itself this session.
 */
object CarInputEvents {
    private val _events = MutableSharedFlow<CarInputEvent>(extraBufferCapacity = BUFFER, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<CarInputEvent> = _events.asSharedFlow()

    private val _report = MutableStateFlow<List<String>>(emptyList())
    /**
     * What the car said about itself this session, ready to show or share: one line per video
     * configuration it offered for its main screen, one per input service ([CarInputLog.serviceLine]),
     * and the configuration chosen once the picture starts. Empty when no car is connected.
     */
    val report: StateFlow<List<String>> = _report.asStateFlow()

    internal const val BUFFER = 64

    @Volatile private var touchpadSeen = false

    /** The session whose report is shown: an older session's [end] leaves a newer one's report alone (I4-4). */
    @Volatile private var owner: Any? = null

    /** A new session ([session]): its report replaces the last one's. */
    @Synchronized
    internal fun start(session: Any, lines: List<String>) {
        owner = session
        touchpadSeen = false
        _report.value = lines
    }

    /** [session]'s picture started with configuration [index]: one "chosen" line, replacing an earlier one (I4-5: not an older session's). */
    @Synchronized
    internal fun chosen(session: Any, index: Int, c: VideoConfiguration) {
        if (owner !== session) return
        _report.update { lines -> lines.filterNot { it.startsWith(CHOSEN) } + chosenLine(index, c) }
    }

    /** [session] ended: its report goes, unless a newer session has started since. */
    @Synchronized
    internal fun end(session: Any) {
        if (owner !== session) return
        owner = null
        _report.value = emptyList()
    }

    /** One input report from the car, on input channel [channel]. Any thread. */
    internal fun report(channel: Int, report: InputReport, atMs: Long) {
        for (e in eventsOf(channel, report, atMs, touchpadAlreadySeen = touchpadSeen)) {
            if (e is CarInputEvent.Touchpad) touchpadSeen = true
            _events.tryEmit(e)
        }
    }

    /** The events in one report, in the order the car sent them (keys, absolute, turns, touchpad). */
    internal fun eventsOf(channel: Int, report: InputReport, atMs: Long, touchpadAlreadySeen: Boolean): List<CarInputEvent> {
        val out = ArrayList<CarInputEvent>()
        if (report.hasKeyEvent()) for (k in report.keyEvent.keysList) out += CarInputEvent.Key(channel, k.keycode, k.down, k.longpress, atMs)
        if (report.hasAbsoluteEvent()) for (a in report.absoluteEvent.dataList) out += CarInputEvent.Absolute(channel, a.keycode, a.value, atMs)
        if (report.hasRelativeEvent()) for (r in report.relativeEvent.dataList) out += CarInputEvent.Turn(channel, r.keycode, r.delta, atMs)
        if (report.hasTouchpadEvent() && !touchpadAlreadySeen) out += CarInputEvent.Touchpad(channel, atMs)
        return out
    }

    /** The report's lines for a session: the main screen's offered configurations, then the input services. */
    internal fun sessionLines(configs: List<VideoConfiguration>, inputServices: List<Service>, used: Service?): List<String> =
        configs.mapIndexed { i, c -> "car offers screen #$i: ${configLine(c)}" } +
            inputServices.map { CarInputLog.serviceLine(it, used = it === used) }

    internal fun chosenLine(index: Int, c: VideoConfiguration) = "$CHOSEN screen #$index, ${configLine(c)}"

    private const val CHOSEN = "chosen:"

    /** Resolution, frame rate, margins, density and the car's insets, as the car sent them. */
    internal fun configLine(c: VideoConfiguration): String {
        val (w, h) = CarSession.resolution(c.codecResolution)
        val fps = c.frameRate.name.removePrefix("VIDEO_FPS_")
        val codec = CarSession.configCodec(c).name.removePrefix("MEDIA_CODEC_VIDEO_")
        val parts = mutableListOf("${w}x$h $fps fps $codec", "margins ${c.widthMargin}x${c.heightMargin}",
            "density ${if (c.density > 0) c.density else "not given"}")
        if (c.realDensity > 0) parts += "real density ${c.realDensity}"
        if (c.hasUiConfig()) {
            val ui = c.uiConfig
            if (ui.hasMargins()) parts += "ui margins ${insets(ui.margins)}"
            if (ui.hasContentInsets()) parts += "content insets ${insets(ui.contentInsets)}"
            if (ui.hasStableContentInsets()) parts += "stable insets ${insets(ui.stableContentInsets)}"
        }
        return parts.joinToString(", ")
    }

    private fun insets(i: Insets) = "top ${i.top} bottom ${i.bottom} left ${i.left} right ${i.right}"
}
