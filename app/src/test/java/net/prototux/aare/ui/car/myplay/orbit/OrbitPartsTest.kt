package net.prototux.aare.ui.car.myplay.orbit

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TurnRight
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import net.prototux.aare.ui.car.myplay.World
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** MyPlay (N4): ORBIT's parts, drawn day and night, saved as pictures for people to look at. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w1280dp-h720dp-land-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OrbitPartsTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun everyWorldHasItsOwnAccentAndIcon() {
        for (night in listOf(true, false)) assertEquals(World.entries.size, World.entries.map { Orbit.accent(it, night, null) }.toSet().size)
        assertEquals(World.entries.size, World.entries.map(Orbit::icon).toSet().size)
    }

    /** WCAG contrast of two colours. */
    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance() + 0.05
        val lb = b.luminance() + 0.05
        return maxOf(la, lb) / minOf(la, lb)
    }

    /**
     * Look C (23, 24): the maps app's screens in ORBIT's colours read by day and by night: the
     * accent (buttons, Go now, rings) on the glass at 4.5:1 or more, the words at 7:1, the words on
     * a filled button at 4.5:1.
     */
    @Test fun theMapsAppsColoursReadDayAndNight() {
        val base = androidx.compose.material3.darkColorScheme()
        for (night in listOf(true, false)) {
            val s = Orbit.appScheme(base, night, null)
            assertTrue("night=$night accent on glass", contrast(s.primary, s.surface) >= 4.5)
            assertTrue("night=$night accent on the background", contrast(s.primary, s.background) >= 4.5)
            assertTrue("night=$night words on glass", contrast(s.onSurface, s.surface) >= 7.0)
            assertTrue("night=$night quiet words on glass", contrast(s.onSurfaceVariant, s.surface) >= 4.5)
            assertTrue("night=$night words on a button", contrast(s.onPrimary, s.primary) >= 4.5)
            assertEquals(Orbit.accent(World.NAVIGATE, night, null), s.primary)
            assertEquals(1f, s.surface.alpha)
        }
    }

    @Test fun accentsReadOnTheirGlassDayAndNight() {
        for (night in listOf(true, false)) {
            val g = Orbit.glass(night, null)
            for (w in World.entries) {
                val a = Orbit.accent(w, night, null)
                assertTrue("$w night=$night on glass", contrast(a, g.glass.copy(alpha = 1f)) >= 3.0)
                assertTrue("$w night=$night on the background", contrast(a, g.background) >= 3.0)
                // The focus ring against its halo.
                assertTrue("$w night=$night ring on halo", contrast(a, g.halo) >= 3.0)
                assertTrue("$w night=$night text on a filled disc", contrast(Orbit.onAccent(night, null), a) >= 4.5)
            }
        }
    }

    @Test fun aSectorStaysInsideItsRing() {
        val p = sectorPath(Offset(100f, 100f), 40f, 90f, -120f, -60f).getBounds()
        assertTrue(p.top >= 10f - 0.5f && p.bottom <= 100f - 40f * 0.5f + 0.5f)
        assertTrue(p.left >= 100f - 90f && p.right <= 100f + 90f)
    }

    @Test fun partsNight() = board(night = true, "orbit-parts-night")
    @Test fun partsDay() = board(night = false, "orbit-parts-day")

    private fun board(night: Boolean, name: String) {
        val g = Orbit.glass(night, null)
        val nav = Orbit.accent(World.NAVIGATE, night, null)
        val song = Orbit.accent(World.MUSIC, night, null)
        rule.setContent {
            Column(Modifier.testTag("board").background(g.background).padding(40.dp), verticalArrangement = Arrangement.spacedBy(40.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(48.dp), verticalAlignment = Alignment.CenterVertically) {
                    // The turn bubble.
                    OrbitDisc(132.dp, night, fill = nav, edge = nav) {
                        Icon(Icons.Rounded.TurnRight, null, Modifier.size(78.dp), tint = Orbit.onAccent(night))
                    }
                    Column {
                        Text("350 m", style = Orbit.Type.hero, color = g.text)
                        Text("Hauptstraße", style = Orbit.Type.secondary, color = g.secondary)
                    }
                    // The hub: the trip's ring, focused.
                    OrbitDisc(192.dp, night, Modifier.orbitFocus(true, nav, g.halo)) {
                        OrbitRing(0.62f, 176.dp, 9.dp, nav, g.track) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("14:32", style = Orbit.Type.clock, color = g.text)
                                Text("18 min", style = Orbit.Type.secondary, color = g.secondary)
                            }
                        }
                    }
                    // The song disc.
                    OrbitRing(0.38f, 152.dp, 7.dp, song, g.track) {
                        OrbitDisc(124.dp, night, fill = song) { Icon(Icons.Rounded.MusicNote, null, Modifier.size(56.dp), tint = g.text) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(48.dp), verticalAlignment = Alignment.CenterVertically) {
                    // The dial, Apps lit.
                    Box(Modifier.size(300.dp)) {
                        World.entries.forEachIndexed { i, w ->
                            OrbitSector(i, World.entries.size, w == World.APPS, Orbit.accent(w, night), night, 300.dp, 90.dp)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Night Drive", style = Orbit.Type.name, color = g.text)
                        Text("Blue Harbor", style = Orbit.Type.secondary, color = g.secondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            World.entries.forEach { w ->
                                OrbitDisc(64.dp, night, edge = Orbit.accent(w, night)) { Icon(Orbit.icon(w), null, tint = Orbit.accent(w, night)) }
                            }
                        }
                        Text("Navigate  Music  Calls", style = Orbit.Type.dial, color = g.secondary)
                    }
                }
            }
        }
        rule.waitForIdle()
        val bmp = rule.onNodeWithTag("board").captureToImage().asAndroidBitmap()
        assertTrue(bmp.width > 0 && bmp.height > 0)
        val dir = File("build/reports/myplay-screens").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
