package net.prototux.aare.ui.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperEditTest {
    private val audi = 1200f / 723f

    @Test
    fun atZoomOneAWidePhotoIsCutAtTheSidesAndCentred() {
        val r = sourceRect(4000, 2000, audi, WallpaperEdit())
        assertEquals(2000f, r.height, 0.5f)
        assertEquals(2000f * audi, r.width, 0.5f)
        assertEquals((4000 - r.width) / 2, r.left, 0.5f)
        assertEquals(0f, r.top, 0.01f)
    }

    @Test
    fun aTallPhotoIsCutAtTopAndBottom() {
        val r = sourceRect(1000, 2000, audi, WallpaperEdit())
        assertEquals(1000f, r.width, 0.5f)
        assertEquals(1000f / audi, r.height, 0.5f)
        assertEquals(0f, r.left, 0.01f)
    }

    @Test
    fun zoomingInTakesASmallerPartAndOffsetsReachTheEdges() {
        val r = sourceRect(4000, 2000, audi, WallpaperEdit(zoom = 2f, offsetX = -1f, offsetY = 1f))
        assertEquals(1000f, r.height, 0.5f)
        assertEquals(0f, r.left, 0.01f)
        assertEquals(2000f - r.height, r.top, 0.5f)
    }

    @Test
    fun theResultNeverLeavesThePhoto() {
        listOf(WallpaperEdit(zoom = 9f, offsetX = 3f, offsetY = -3f), WallpaperEdit(zoom = 0.2f)).forEach { e ->
            val r = sourceRect(3000, 1500, audi, e)
            assertTrue(r.left >= 0 && r.top >= 0 && r.left + r.width <= 3000.5f && r.top + r.height <= 1500.5f)
        }
    }

    @Test
    fun aDragRightShowsMoreOfTheLeftSide() {
        val start = WallpaperEdit(zoom = 2f)
        val moved = dragged(start, 4000, 2000, 400f, 241f, dx = 50f, dy = 0f, zoomBy = 1f)
        assertTrue(moved.offsetX < start.offsetX)
    }

    @Test
    fun anEditRoundTripsAndBadValuesAreClamped() {
        val e = WallpaperEdit(1.5f, 0.25f, -0.5f, 40, 3)
        assertEquals(e, WallpaperEdit.decode(e.encode()))
        assertEquals(WallpaperEdit(WallpaperEdit.MAX_ZOOM, 1f, -1f, 80, 10), WallpaperEdit.decode("zoom=99;x=7;y=-7;dim=200;blur=50"))
        assertEquals(WallpaperEdit(), WallpaperEdit.decode("rubbish"))
    }

    @Test
    fun noBlurAtLevelZero() {
        assertEquals(0f, blurRadius(0, 1600), 0f)
        assertEquals(20f, blurRadius(5, 1600), 0.01f)
    }

    @Test
    fun theBlurKeepsTheImageSizeAndEvensOutColour() {
        val px = IntArray(16) { if (it % 2 == 0) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() }
        val out = ImageBlur.blur(px, 4, 4, 2f)
        assertEquals(16, out.size)
        val g = (out[5] shr 8) and 0xFF
        assertTrue("$g", g in 60..200)
    }

    @Test
    fun anOlderPhotoBecomesTheUntouchedCopyOnceAndEditsStartFromIt() {
        val dir = kotlin.io.path.createTempDirectory("wp").toFile()
        val shown = java.io.File(dir, "wallpaper.jpg").apply { writeText("original") }
        val source = java.io.File(dir, WallpaperFiles.SOURCE)
        // First edit: no copy yet, the shown photo becomes it.
        assertEquals(source, WallpaperFiles.sourceFile(shown, source))
        assertEquals("original", source.readText())
        // The edit is written over the shown photo; the next edit still starts from the copy.
        shown.writeText("edited once")
        assertEquals("original", WallpaperFiles.sourceFile(shown, source)!!.readText())
        shown.writeText("edited twice")
        assertEquals("original", WallpaperFiles.sourceFile(shown, source)!!.readText())
        dir.deleteRecursively()
    }

    @Test
    fun noPhotoNoSource() {
        val dir = kotlin.io.path.createTempDirectory("wp").toFile()
        assertEquals(null, WallpaperFiles.sourceFile(java.io.File(dir, "wallpaper.jpg"), java.io.File(dir, WallpaperFiles.SOURCE)))
        dir.deleteRecursively()
    }

    @Test
    fun aFailedCopyLeavesNoSourceBehind() {
        val dir = kotlin.io.path.createTempDirectory("wp").toFile()
        val source = java.io.File(dir, WallpaperFiles.SOURCE)
        // Nothing to copy: the copy fails, and neither the source nor its temporary file is left.
        assertEquals(false, WallpaperFiles.keepSource(java.io.File(dir, "missing.jpg"), source))
        assertEquals(false, source.exists())
        assertEquals(false, java.io.File(source.path + ".tmp").exists())
        dir.deleteRecursively()
    }

    @Test
    fun anOldSourceIsReplacedWhole() {
        val dir = kotlin.io.path.createTempDirectory("wp").toFile()
        val source = java.io.File(dir, WallpaperFiles.SOURCE).apply { writeText("old") }
        val shown = java.io.File(dir, "shown.jpg").apply { writeText("new") }
        assertEquals(true, WallpaperFiles.keepSource(shown, source))
        assertEquals("new", source.readText())
        assertEquals(false, java.io.File(source.path + ".tmp").exists())
        dir.deleteRecursively()
    }
}
