package net.prototux.aare.ui.theme

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.activity.ComponentActivity
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import net.prototux.aare.media.titleText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The owner: Poweramp's song titles were black and unreadable on the car screen, whatever the
 * theme. Text with no colour of its own takes the theme's text colour (LocalContentColor), not
 * Compose's default black; and a title an app sends with a colour span of its own is plain text.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CarThemeContentColourTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun uncolouredTextTakesTheThemesTextColour() {
        var night: Color? = null; var nightBackground: Color? = null
        var day: Color? = null; var dayBackground: Color? = null
        rule.setContent {
            CarTheme(night = true, theme = CarThemes.byId("aare")) {
                night = LocalContentColor.current; nightBackground = MaterialTheme.colorScheme.onBackground
            }
            CarTheme(night = false, theme = CarThemes.byId("aare")) {
                day = LocalContentColor.current; dayBackground = MaterialTheme.colorScheme.onBackground
            }
        }
        rule.waitForIdle()
        assertEquals(nightBackground, night)
        assertEquals(dayBackground, day)
        assertNotEquals("never black by night", Color.Black, night)
    }

    @Test fun aTitleWithABlackColourSpanIsPlainText() {
        val title = SpannableString("Night Drive").apply { setSpan(ForegroundColorSpan(android.graphics.Color.BLACK), 0, length, 0) }
        val item = MediaItem.Builder().setMediaId("1").setMediaMetadata(MediaMetadata.Builder().setTitle(title).build()).build()
        val text: CharSequence = item.titleText
        assertEquals("Night Drive", text.toString())
        assertFalse("no spans carried", text is Spanned)
    }
}
