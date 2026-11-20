package net.prototux.aare.ui.phone

import android.content.Context
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * MyPlay's release notes (docs/releases/<version>.md), bundled at build time as assets
 * (release_notes/<version>.md), read and shown on the phone without the network.
 *
 * The notes use a small part of Markdown, and only that is understood: a paragraph that is all
 * bold is a heading; "- " starts a bullet, two more spaces of indent per level; lines indented
 * under a bullet continue it; **bold** and *italic* inside text. Anything else stays as it is.
 */
sealed interface NoteBlock {
    data class Heading(val text: String) : NoteBlock
    data class Paragraph(val text: String) : NoteBlock
    data class Bullet(val level: Int, val text: String) : NoteBlock
}

object ReleaseNotes {
    /** The notes for [version] from the app's assets, or null if this version has none. */
    fun read(context: Context, version: String): String? =
        runCatching { context.assets.open("release_notes/$version.md").use { it.readBytes().decodeToString() } }.getOrNull()

    /** [markdown] as blocks to draw. */
    fun parse(markdown: String): List<NoteBlock> {
        val blocks = mutableListOf<NoteBlock>()
        var paragraph: StringBuilder? = null
        var bullet: Pair<Int, StringBuilder>? = null
        fun flush() {
            paragraph?.let { p ->
                val t = p.toString().trim()
                if (t.isNotEmpty()) blocks += if (isAllBold(t)) NoteBlock.Heading(t.removeSurrounding("**")) else NoteBlock.Paragraph(t)
            }
            bullet?.let { (level, b) -> blocks += NoteBlock.Bullet(level, b.toString().trim()) }
            paragraph = null; bullet = null
        }
        for (raw in markdown.lines()) {
            val line = raw.trimEnd()
            val indent = line.length - line.trimStart().length
            val text = line.trimStart()
            when {
                text.isEmpty() -> flush()
                text.startsWith("- ") || text.startsWith("* ") && !text.startsWith("**") -> {
                    flush()
                    bullet = indent / 2 to StringBuilder(text.substring(2))
                }
                bullet != null && indent > 0 -> bullet!!.second.append(' ').append(text)
                else -> {
                    if (bullet != null) flush()
                    val p = paragraph ?: StringBuilder().also { paragraph = it }
                    if (p.isNotEmpty()) p.append(' ')
                    p.append(text)
                }
            }
        }
        flush()
        return blocks
    }

    private fun isAllBold(t: String) = t.length > 4 && t.startsWith("**") && t.endsWith("**") && !t.substring(2, t.length - 2).contains("**")

    /** **bold** and *italic* inside [text] as styles; the stars themselves are dropped. */
    fun inline(text: String): AnnotatedString = buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end < 0) { append(text.substring(i)); return@buildAnnotatedString }
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(i + 2, end)) }
                    i = end + 2
                }
                text[i] == '*' && i + 1 < text.length && text[i + 1] != ' ' -> {
                    val end = text.indexOf('*', i + 1)
                    if (end < 0) { append(text.substring(i)); return@buildAnnotatedString }
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text.substring(i + 1, end)) }
                    i = end + 1
                }
                else -> { append(text[i]); i++ }
            }
        }
    }

    /**
     * Whether to show "What's new" by itself when the app opens: only after an update to a version
     * that has notes and has not been shown yet; never on a fresh install.
     */
    fun showOnOpen(version: String, seenVersion: String?, hasNotes: Boolean, freshInstall: Boolean): Boolean =
        hasNotes && !freshInstall && seenVersion != version
}
