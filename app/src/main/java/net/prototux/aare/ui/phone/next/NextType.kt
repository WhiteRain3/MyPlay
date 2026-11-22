package net.prototux.aare.ui.phone.next

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import net.prototux.aare.R

/**
 * Roboto Flex (OFL), the design's font: one variable file, each weight the type scale uses drawn from it.
 * The file is the design's Latin subset; other letters fall back to the phone's own font.
 */
@OptIn(ExperimentalTextApi::class)
val NextFont: FontFamily = FontFamily(
    listOf(400, 520, 560, 600, 620, 640, 650).map { w ->
        Font(R.font.roboto_flex, weight = FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    },
)

/** The style sheet's type scale (frame 77): size / weight / letter spacing. */
object NextType {
    private fun style(size: Float, weight: Int, spacing: Float = 0f, line: TextUnit = TextUnit.Unspecified) =
        TextStyle(fontFamily = NextFont, fontSize = size.sp, fontWeight = FontWeight(weight), letterSpacing = spacing.sp, lineHeight = line)

    val RootTitle = style(30f, 650, -0.6f)
    val Hero = style(24f, 650, -0.4f)
    val PageTitle = style(22f, 620, -0.3f)
    val Button = style(16f, 620, -0.1f)
    val Label = style(16f, 520, line = 22.sp)
    val Value = style(14f, 400)
    val Help = style(13f, 400, line = 18.sp)
    val Section = style(13f, 600, 0.2f)
    val TabLabel = style(12f, 520)
    val Tag = style(11.5f, 600, 0.1f)
    val Wordmark = style(20f, 650, -0.3f)
    val SheetTitle = style(22f, 640, -0.3f)
}
