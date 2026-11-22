package net.prototux.aare.host

import androidx.car.app.model.Alert
import androidx.car.app.model.CarText
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MessageInfo
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.RoutingInfo
import androidx.car.app.navigation.model.Trip
import androidx.core.content.edit
import net.prototux.aare.DiagLog
import net.prototux.aare.Settings

/**
 * MyPlay (plan N3, the Waze bench check): does any speed or speed limit reach MyPlay from a maps
 * app? The car library has no field for either (checked 2026-11-21), but an app could put one in
 * its free text: an alert, the trip, the turn or a message.
 *
 * With the hidden setting [myplayDebugNavText] on, each distinct text a hosted maps app sends in
 * those places gives one line in the connection log ([lines]): where it came from and how long it
 * is, never the text itself, so no road, place or address reaches the log (NT-1). Only a part
 * that looks like a speed ([looksLikeSpeed]) is written out, e.g. "50 km/h". Destinations are not
 * read at all: a speed limit is never in one. Only for the check: it reads what the app already
 * sends, changes nothing, and is off by default.
 */
internal object NavTextCheck {
    /** Set from the setting by the car runtime. */
    @Volatile var on = false

    /** At most this many lines per connection to the car, so a long drive does not fill the log. */
    const val MAX_LINES = 200

    private val seen = LinkedHashSet<String>()

    private val SPEED = Regex("""\b\d{1,3}\s?(km/h|kmh|kph|mph)\b""", RegexOption.IGNORE_CASE)

    fun looksLikeSpeed(text: String): Boolean = SPEED.containsMatchIn(text)

    /**
     * One line per text: "nav text <app> <where>: a text of N characters", plus "; looks like a
     * speed: <the speed-like parts>" when it has any. The text itself is never written.
     */
    fun lines(app: String, where: String, texts: List<String>): List<String> =
        texts.map { it.trim() }.filter { it.isNotEmpty() }.distinct().map { t ->
            val speeds = SPEED.findAll(t).map { it.value }.toList()
            "nav text $app $where: a text of ${t.length} characters" +
                if (speeds.isEmpty()) "" else "; looks like a speed: ${speeds.joinToString(", ")}"
        }

    /**
     * AL-4 (the dispatcher, for radars, police and average-speed zones): an alert's whole structure,
     * one line, while the check is on. Alert titles and subtitles are the app's generic words
     * ("In 380 m", "Reduced speed", "Police reported"), so they are written in full; turn and trip
     * texts stay hidden as before. No position is in an alert.
     */
    fun alertStructure(app: String, a: Alert): String = buildString {
        append("nav alert $app: id ${a.id}")
        // AL-6, AL-7: a quote in a text becomes ' and a line break a space, so the shared log's
        // filter finds each text whole and no text spills onto a line of its own.
        fun q(s: String?) = s.orEmpty().replace('"', '\'').replace(Regex("[\\r\\n\\u2028\\u2029]+"), " ")
        append(", title \"").append(q(text(a.title))).append('"')
        text(a.subtitle)?.let { append(", subtitle \"").append(q(it)).append('"') }
        a.icon?.let { i ->
            append(", icon type ${i.type}")
            i.icon?.let { ic -> append(" (${iconKind(ic)})") }
            i.tint?.let { append(", tinted") }
        }
        if (a.actions.isNotEmpty()) append(", actions [").append(a.actions.joinToString(", ") { act -> "\"${q(text(act.title) ?: "?")}\"" }).append(']')
        append(", ${a.durationMillis} ms")
        a.callbackDelegate?.let { append(", has callback") }
    }

    private fun iconKind(ic: androidx.core.graphics.drawable.IconCompat): String = runCatching {
        when (ic.type) {
            androidx.core.graphics.drawable.IconCompat.TYPE_RESOURCE -> "resource ${ic.resPackage}:${ic.resId}"
            androidx.core.graphics.drawable.IconCompat.TYPE_BITMAP, androidx.core.graphics.drawable.IconCompat.TYPE_ADAPTIVE_BITMAP -> "bitmap"
            androidx.core.graphics.drawable.IconCompat.TYPE_URI -> "uri"
            else -> "kind ${ic.type}"
        }
    }.getOrDefault("unknown")

    /** AL-4: logs [alertStructure] once per distinct line, while [on]. Never throws. */
    fun noteAlert(app: String, a: Alert) {
        if (!on) return
        runCatching {
            val line = alertStructure(app, a)
            synchronized(seen) { if (seen.size < MAX_LINES && seen.add(line)) DiagLog.add(line) }
        }
    }

    /** The texts of an alert. */
    fun alertTexts(a: Alert): List<String> = listOfNotNull(text(a.title), text(a.subtitle))

    /** The texts of a trip that could carry a speed: the current road and each step's cue and road. Never the destinations. */
    fun tripTexts(t: Trip): List<String> = buildList {
        text(t.currentRoad)?.let(::add)
        for (s in t.steps) { text(s.cue)?.let(::add); s.road?.let { text(it) }?.let(::add) }
    }

    /** The texts of a navigation template's turn card or message. */
    fun templateTexts(t: Template): List<String> {
        val nav = t as? NavigationTemplate ?: return emptyList()
        return when (val info = nav.navigationInfo) {
            is RoutingInfo -> listOfNotNull(info.currentStep?.let { text(it.cue) }, info.currentStep?.road?.let { text(it) },
                info.nextStep?.let { text(it.cue) })
            is MessageInfo -> listOfNotNull(text(info.title), text(info.text))
            else -> emptyList()
        }
    }

    /** Logs [texts] not logged before, while [on]. Never throws. */
    fun note(app: String, where: String, texts: () -> List<String>) {
        if (!on) return
        runCatching {
            for (line in lines(app, where, texts())) {
                synchronized(seen) {
                    if (seen.size >= MAX_LINES || !seen.add(line)) return@synchronized
                    DiagLog.add(line)
                }
            }
        }
    }

    /** A new connection to the car: log again from the start. */
    fun reset() = synchronized(seen) { seen.clear() }

    private fun text(t: CarText?): String? = t?.toString()?.takeIf { it.isNotBlank() }
}

/** MyPlay (N3 check): log what text maps apps send, to see whether any speed or limit reaches MyPlay. */
var Settings.myplayDebugNavText: Boolean
    get() = prefs.getBoolean("myplay_debug_nav_text", false)
    set(v) = prefs.edit { putBoolean("myplay_debug_nav_text", v) }
