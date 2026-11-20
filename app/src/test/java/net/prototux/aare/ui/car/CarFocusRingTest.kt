package net.prototux.aare.ui.car

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MyPlay: everything the knob can stop on, on the car screen, must show the focus ring.
 *
 * This reads the source of the car screens and fails when something tappable has no
 * `carFocusHighlight`. It cannot see the screen, so it checks the text only:
 *  - a `.clickable` (or `.combinedClickable`, `.selectable`, `.toggleable`) must have
 *    `carFocusHighlight(` earlier on the same line or on one of the two lines above;
 *  - a Material control that takes focus by itself (`IconButton(`, `Switch(`, `Slider(` and the
 *    like) must have `carFocusHighlight(` among its arguments, unless it is switched off as a stop
 *    (`onClick = null`, `onCheckedChange = null`).
 * A place that is tappable on purpose without a ring goes in [ALLOWED], with the reason.
 */
class CarFocusRingTest {

    @Test fun everyTappableThingOnTheCarScreenHasARing() {
        val root = sourceRoot()
        val files = CAR_SCREEN_DIRS.flatMap { File(root, it).walkTopDown().filter { f -> f.isFile && f.extension == "kt" } }
        assertTrue("found only ${files.size} source files under $root", files.size > 20)
        val faults = files.flatMap { f ->
            val name = f.relativeTo(root).path.replace(File.separatorChar, '/')
            ringFaults(f.readText()).filterNot { fault -> ALLOWED[name].orEmpty().any { it in fault } }.map { "$name: $it" }
        }
        assertEquals("tappable without carFocusHighlight:\n" + faults.joinToString("\n"), 0, faults.size)
    }

    @Test fun aClickableWithoutARingIsReported() {
        assertEquals(1, ringFaults("Box(Modifier.size(4.dp).clickable { go() })").size)
        assertEquals(1, ringFaults("Box(Modifier.size(4.dp)\n    .combinedClickable(onClick = go))").size)
    }

    @Test fun aRingOnTheSameLineOrJustAboveIsEnough() {
        assertEquals(0, ringFaults("Box(Modifier.carFocusHighlight(CircleShape).clickable { go() })").size)
        assertEquals(0, ringFaults("Box(Modifier\n    .carFocusHighlight(CircleShape)\n    .clickable { go() })").size)
    }

    @Test fun aRingFarAboveDoesNotCount() {
        assertEquals(1, ringFaults("Box(Modifier.carFocusHighlight(CircleShape).clickable { a() })\n\n\nBox(Modifier.clickable { b() })").size)
    }

    @Test fun importsAndCommentsAreNotTappable() {
        assertEquals(0, ringFaults("import androidx.compose.foundation.clickable\n// its own `.clickable` here\n * a `.clickable` there").size)
    }

    @Test fun materialControlsNeedARingUnlessTheyAreNotAStop() {
        assertEquals(1, ringFaults("IconButton(onClick = go) { Icon(a, b) }").size)
        assertEquals(1, ringFaults("Switch(\n    checked = on,\n    onCheckedChange = set,\n)").size)
        assertEquals(0, ringFaults("IconButton(onClick = go, modifier = Modifier.carFocusHighlight(CircleShape)) { Icon(a, b) }").size)
        assertEquals(0, ringFaults("Switch(checked = on, onCheckedChange = null)").size)
        assertEquals(0, ringFaults("RadioButton(selected = on, onClick = null)").size)
        assertEquals(0, ringFaults("private fun ToolButton(icon: ImageVector) {").size)
    }

    private fun sourceRoot(): File =
        listOf("src/main/java/net/prototux/aare", "app/src/main/java/net/prototux/aare").map(::File).first { it.isDirectory }

    private companion object {
        /** Where the car screens live, under the app's source root. */
        val CAR_SCREEN_DIRS = listOf("ui/car", "host", "media", "mirror")

        /** File to the text of each tappable place that has no ring on purpose. Say why. */
        val ALLOWED: Map<String, List<String>> = emptyMap()

        val TAPPABLE = Regex("""\.(clickable|combinedClickable|selectable|toggleable)\s*[({]""")
        val CONTROL = Regex("""(?<![\w.])(IconButton|FilledIconButton|Button|TextButton|FilledTonalButton|OutlinedButton|Switch|Slider|Checkbox|RadioButton)\(""")
        val NOT_A_STOP = Regex("""on(Click|CheckedChange)\s*=\s*null""")
        const val RING = "carFocusHighlight("

        /** The tappable places in [source] that have no ring: one line of text each. */
        fun ringFaults(source: String): List<String> {
            val lines = source.lines()
            val faults = mutableListOf<String>()
            lines.forEachIndexed { i, line ->
                val code = line.trim()
                if (code.startsWith("import ") || code.startsWith("//") || code.startsWith("*") || code.startsWith("/*")) return@forEachIndexed
                TAPPABLE.findAll(line).forEach { m ->
                    val before = lines.subList(maxOf(0, i - 2), i).joinToString("\n") + "\n" + line.substring(0, m.range.first)
                    if (RING !in before) faults += "line ${i + 1}: $code"
                }
                CONTROL.findAll(line).forEach { m ->
                    if (line.substring(0, m.range.first).trimEnd().endsWith("fun")) return@forEach
                    val call = callText(lines, i, m.range.last)
                    if (RING !in call && !NOT_A_STOP.containsMatchIn(call)) faults += "line ${i + 1}: $code"
                }
            }
            return faults
        }

        /** The arguments of the call whose opening bracket is at [open] on line [lineIndex]. */
        fun callText(lines: List<String>, lineIndex: Int, open: Int): String {
            val text = lines.drop(lineIndex).joinToString("\n")
            var depth = 0
            for (at in open until text.length) {
                when (text[at]) {
                    '(' -> depth++
                    ')' -> if (--depth == 0) return text.substring(open, at + 1)
                }
            }
            return text.substring(open)
        }
    }
}
