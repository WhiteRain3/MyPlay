package net.prototux.aare.ui.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReleaseNotesTest {
    private val sample = """
        **On the phone**

        - Two new pages, opened by tapping
          the clock:
          - **Status icons**: four sizes.
          - *Clock*: bold or not.
        - The picture follows.

        Nothing in this release has been tried in the car yet.
    """.trimIndent()

    @Test
    fun headingsBulletsAndParagraphsAreFound() {
        assertEquals(
            listOf(
                NoteBlock.Heading("On the phone"),
                NoteBlock.Bullet(0, "Two new pages, opened by tapping the clock:"),
                NoteBlock.Bullet(1, "**Status icons**: four sizes."),
                NoteBlock.Bullet(1, "*Clock*: bold or not."),
                NoteBlock.Bullet(0, "The picture follows."),
                NoteBlock.Paragraph("Nothing in this release has been tried in the car yet."),
            ),
            ReleaseNotes.parse(sample),
        )
    }

    @Test
    fun boldAndItalicLoseTheirStars() {
        assertEquals("Status icons: four sizes.", ReleaseNotes.inline("**Status icons**: four sizes.").text)
        assertEquals("Clock: bold", ReleaseNotes.inline("*Clock*: bold").text)
        assertEquals(1, ReleaseNotes.inline("**a** b").spanStyles.size)
    }

    @Test
    fun anUnclosedStarIsKeptAsItIs() {
        assertEquals("5 * 3", ReleaseNotes.inline("5 * 3").text)
        assertEquals("**open", ReleaseNotes.inline("**open").text)
    }

    @Test
    fun shownOnceAfterAnUpdateNeverOnAFreshInstall() {
        assertTrue(ReleaseNotes.showOnOpen("0.1.12", "0.1.11", hasNotes = true, freshInstall = false))
        assertTrue(ReleaseNotes.showOnOpen("0.1.12", null, hasNotes = true, freshInstall = false))
        assertFalse(ReleaseNotes.showOnOpen("0.1.12", "0.1.12", hasNotes = true, freshInstall = false))
        assertFalse(ReleaseNotes.showOnOpen("0.1.12", null, hasNotes = true, freshInstall = true))
        assertFalse(ReleaseNotes.showOnOpen("0.1.12", "0.1.11", hasNotes = false, freshInstall = false))
    }

    @Test
    fun everyBundledNoteParses() {
        val dir = listOf("../docs/releases", "docs/releases").map { File(it) }.first { it.isDirectory }
        dir.listFiles { f -> f.extension == "md" }!!.forEach { f ->
            assertTrue(f.name, ReleaseNotes.parse(f.readText()).isNotEmpty())
        }
    }
}
