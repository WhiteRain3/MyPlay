package net.prototux.aare.ui.phone

/**
 * The log the owner sends from the Car check page (I4): what the page found, what the car said
 * about itself, and the connection log, with private details taken out so it can be sent to anyone.
 */
object CarCheckLog {
    /** Bluetooth and Wi-Fi addresses (the car's Bluetooth address is how it is told apart). */
    private val MAC = Regex("""\b[0-9A-Fa-f]{2}(?:[:-][0-9A-Fa-f]{2}){5}\b""")
    private val IPV4 = Regex("""\b(?:\d{1,3}\.){3}\d{1,3}\b""")
    private val IPV6 = Regex("""\b(?:[0-9A-Fa-f]{1,4}:){3,7}[0-9A-Fa-f]{1,4}\b""")
    private val EMAIL = Regex("""[\w.+-]+@[\w-]+(?:\.[\w-]+)+""")
    /** A phone number: a plus or nine digits or more (key codes, sizes and counts are shorter). */
    private val PHONE = Regex("""\+\d[\d ()-]{6,}\d|\b\d{9,}\b""")

    /** [line] with addresses, e-mails and phone numbers replaced by what they were. */
    fun withoutPrivate(line: String): String = line
        .replace(MAC, "[address]")
        .replace(EMAIL, "[e-mail]")
        .replace(IPV6, "[address]")
        .replace(IPV4, "[address]")
        .replace(PHONE, "[number]")

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
