package net.prototux.aare.ui.phone

/**
 * The log the owner sends from the Car check page (I4): what the page found, what the car said
 * about itself, and the connection log, with the private details it is known to hold taken out
 * (the page asks the owner to read it before sending all the same).
 */
object CarCheckLog {
    /** Bluetooth and Wi-Fi addresses (the car's Bluetooth address is how it is told apart): masked but for their last two bytes. */
    private val MAC = Regex("""\b[0-9A-Fa-f]{2}(?:[:-][0-9A-Fa-f]{2}){5}\b""")
    private val IPV4 = Regex("""\b(?:\d{1,3}\.){3}\d{1,3}\b""")
    /** Written out, or shortened with "::" (which needs one at least). */
    private val IPV6 = Regex("""\b(?:[0-9A-Fa-f]{1,4}:){3,7}[0-9A-Fa-f]{1,4}\b|(?:\b[0-9A-Fa-f]{1,4})?(?::[0-9A-Fa-f]{1,4})*::(?:[0-9A-Fa-f]{1,4}(?::[0-9A-Fa-f]{1,4})*)?\b""")
    private val EMAIL = Regex("""[\w.+-]+@[\w-]+(?:\.[\w-]+)+""")
    /** Digits in groups split by spaces, dashes, dots or brackets, maybe after a plus: a phone number when 7 digits or more (not times, sizes or rates). */
    private val PHONE = Regex("""(?<![\w.:/])\+?\(?\d[\d ().-]{4,}\d(?![\w:/])(?! ?(?:ms|bytes|B|Hz|kbps)\b)""")
    private val DATE = Regex("""\d{4}-\d{2}-\d{2}""")
    /** A vehicle identification number: 17 capitals and digits, both. */
    private val VIN = Regex("""\b(?=[A-HJ-NPR-Z0-9]*[A-HJ-NPR-Z])(?=[A-HJ-NPR-Z0-9]*\d)[A-HJ-NPR-Z0-9]{17}\b""")

    /**
     * Names older connection logs hold as they are (the car side no longer writes them): who a
     * message was auto-replied to, a Bluetooth device's name and the car's Wi-Fi network name (in
     * quotes on wireless lines). The certificate's
     * and the head unit's names in other lines stay: they name the car, not a person.
     */
    private val NAMES: List<Pair<Regex, String>> = listOf(
        Regex("""(auto-replied to ).*""") to "$1[name]",
        Regex("""(wireless: connecting to ).*""") to "$1[device]",
        Regex("""(wireless: ).+?( offers Android Auto| connected over Bluetooth| already connected over Bluetooth)""") to "$1[device]$2",
    )
    private val QUOTED = Regex(""""[^"]*"""")

    /**
     * The maps app text check (host/NavTextCheck): its lines hold street names and the destination,
     * so only the speeds in them are kept ("nav text <app> <where>: [text hidden; speeds: 50 km/h]").
     */
    private val NAV_TEXT = Regex("""(nav text \S+ (?:turn card|alert|trip): ).*""")
    private val SPEED = Regex("""\b\d{1,3}\s?(km/h|kmh|kph|mph)\b""", RegexOption.IGNORE_CASE)

    private fun navText(line: String): String = line.replace(NAV_TEXT) { m ->
        val speeds = SPEED.findAll(m.value.removePrefix(m.groupValues[1])).map { it.value }.toList()
        m.groupValues[1] + if (speeds.isEmpty()) "[text hidden]" else "[text hidden; speeds: ${speeds.joinToString()}]"
    }

    /** [line] with names, addresses, e-mails, phone numbers and VINs replaced by what they were. */
    fun withoutPrivate(line: String): String {
        var out = navText(line)
        for ((pattern, replacement) in NAMES) out = out.replace(pattern, replacement)
        if (out.contains("wireless: ")) out = out.replace(QUOTED, "\"[network]\"")
        return out
            // The last two bytes stay, so lines about the same device can still be told apart.
            .replace(MAC) { m -> "XX:XX:XX:XX:" + m.value.takeLast(5).replace('-', ':') }
            .replace(EMAIL, "[e-mail]")
            .replace(IPV6, "[address]")
            .replace(IPV4, "[address]")
            .replace(VIN, "[VIN]")
            .replace(PHONE) { m ->
                if (m.value.count(Char::isDigit) >= 7 && !DATE.containsMatchIn(m.value)) "[number]" else m.value
            }
    }

    /** Android's share sheet carries text in one message; well below its limit, so no app cuts it. */
    const val MAX_CHARS = 300_000

    /**
     * The text to share: [summary] and [report] first (short, always whole), then the newest lines
     * of [log] that fit in [maxChars], all without private details.
     */
    fun shareText(summary: List<String>, report: List<String>, log: List<String>, maxChars: Int = MAX_CHARS): String {
        val head = buildString {
            appendLine("MyPlay car check")
            summary.forEach { appendLine(withoutPrivate(it)) }
            appendLine()
            appendLine("What the car reported")
            if (report.isEmpty()) appendLine("(nothing: not connected)") else report.forEach { appendLine(withoutPrivate(it)) }
            appendLine()
            appendLine("Connection log")
        }
        val room = maxChars - head.length
        val kept = ArrayDeque<String>()
        var size = 0
        for (line in log.asReversed()) {
            val clean = withoutPrivate(line)
            if (size + clean.length + 1 > room) break
            kept.addFirst(clean)
            size += clean.length + 1
        }
        val cut = if (kept.size < log.size) "(${log.size - kept.size} older lines left out)\n" else ""
        return head + cut + kept.joinToString("\n")
    }
}
