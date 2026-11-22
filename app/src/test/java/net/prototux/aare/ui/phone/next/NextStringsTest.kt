package net.prototux.aare.ui.phone.next

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * Content rules (plan §9.1): a label is at most 30 characters, a help line 60, an ⓘ text 300, counted
 * with placeholders filled with typical values. Every string of the new app is named by its kind.
 */
class NextStringsTest {
    private val values = listOf("src/main/res/values", "app/src/main/res/values").map(::File).first { it.isDirectory }

    private fun strings(file: String): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(values, file))
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length).map { nodes.item(it) as Element }.associate { it.getAttribute("name") to it.textContent }
    }

    /** As shown: escapes undone, placeholders filled ("%1$s" a car name, "%d" a two-digit number). */
    private fun shown(text: String): String = text
        .replace(Regex("""%(\d+\$)?s"""), "Sedan")
        .replace(Regex("""%(\d+\$)?d"""), "12")
        .replace("\\'", "'").replace("\\\"", "\"").replace("\\n", "\n")

    private fun limit(name: String): Int? = when {
        name.endsWith("_label") -> 30
        name.endsWith("_help") -> 60
        name.endsWith("_info") -> 300
        else -> null
    }

    @Test fun everyNewStringIsNamedByItsKind() {
        val unnamed = strings("strings_next.xml").keys.filter { limit(it) == null }
        assertTrue("strings_next.xml names without _label/_help/_info: $unnamed", unnamed.isEmpty())
    }

    @Test fun everyNamedStringFitsItsLimit() {
        val files = listOf("strings_next.xml", "strings_phone.xml", "myplay_strings.xml")
        val tooLong = files.flatMap { f -> strings(f).entries.map { f to it } }.mapNotNull { (f, e) ->
            val max = limit(e.key) ?: return@mapNotNull null
            val len = shown(e.value).length
            if (len > max) "$f ${e.key}: $len > $max" else null
        }
        assertTrue("too long:\n" + tooLong.joinToString("\n"), tooLong.isEmpty())
    }

    @Test fun theFillerWorks() {
        assertTrue(shown("Connecting to %1\$s…") == "Connecting to Sedan…")
        assertTrue(shown("What\\'s new") == "What's new")
    }
}
