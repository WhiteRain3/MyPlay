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
 * With the hidden setting [myplayDebugNavText] on, every distinct text a hosted maps app sends in
 * those places goes into the connection log once ([lines]), marked when it looks like a speed
 * ([looksLikeSpeed]). Only for the check: it reads what the app already sends, changes nothing,
 * and is off by default.
 */
internal object NavTextCheck {
    /** Set from the setting by the car runtime. */
    @Volatile var on = false

    /** At most this many lines per connection to the car, so a long drive does not fill the log. */
    const val MAX_LINES = 200

    private val seen = LinkedHashSet<String>()

    private val SPEED = Regex("""\b\d{1,3}\s?(km/h|kmh|kph|mph)\b""", RegexOption.IGNORE_CASE)

    fun looksLikeSpeed(text: String): Boolean = SPEED.containsMatchIn(text)

    /** One line per text, "nav text <app> <where>: <text>", marked when it looks like a speed. */
    fun lines(app: String, where: String, texts: List<String>): List<String> =
        texts.map { it.trim() }.filter { it.isNotEmpty() }.distinct().map { t ->
            "nav text $app $where: $t" + if (looksLikeSpeed(t)) "  [looks like a speed]" else ""
        }

    /** The texts of an alert. */
    fun alertTexts(a: Alert): List<String> = listOfNotNull(text(a.title), text(a.subtitle))

    /** The texts of a trip: the current road, each step's cue and road, each destination. */
    fun tripTexts(t: Trip): List<String> = buildList {
        text(t.currentRoad)?.let(::add)
        for (s in t.steps) { text(s.cue)?.let(::add); s.road?.let { text(it) }?.let(::add) }
        for (d in t.destinations) { text(d.name)?.let(::add); text(d.address)?.let(::add) }
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
