package net.prototux.aare.ui.phone

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.abs
import kotlin.math.roundToInt
import net.prototux.aare.R

/**
 * Plain ARGB colour arithmetic for the colour picker, free of Android classes so it can be tested.
 * From the owner's earlier project AA-Experiments (core/ColorMath.kt, GPL-3.0), the parts the
 * picker needs; hex parsing is new.
 */
object ColourMath {
    fun red(c: Int) = (c shr 16) and 0xFF
    fun green(c: Int) = (c shr 8) and 0xFF
    fun blue(c: Int) = c and 0xFF
    fun argb(a: Int, r: Int, g: Int, b: Int): Int =
        (a.coerceIn(0, 255) shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

    /** Hue 0-360, saturation and value 0-1; always opaque. */
    fun hsvToColor(hue: Float, saturation: Float, value: Float): Int {
        val h = ((hue % 360f) + 360f) % 360f
        val s = saturation.coerceIn(0f, 1f); val v = value.coerceIn(0f, 1f)
        val c = v * s
        val x = c * (1 - abs((h / 60f) % 2 - 1))
        val m = v - c
        val (r, g, b) = when ((h / 60f).toInt()) {
            0 -> Triple(c, x, 0f); 1 -> Triple(x, c, 0f); 2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c); 4 -> Triple(x, 0f, c); else -> Triple(c, 0f, x)
        }
        return argb(255, ((r + m) * 255).roundToInt(), ((g + m) * 255).roundToInt(), ((b + m) * 255).roundToInt())
    }

    /** Hue (0-360), saturation (0-1) and value (0-1). */
    fun colorToHsv(color: Int): FloatArray {
        val r = red(color) / 255f; val g = green(color) / 255f; val b = blue(color) / 255f
        val max = maxOf(r, g, b); val min = minOf(r, g, b); val delta = max - min
        val hue = when {
            delta == 0f -> 0f
            max == r -> 60f * (((g - b) / delta) % 6f)
            max == g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }.let { if (it < 0f) it + 360f else it }
        return floatArrayOf(hue, if (max == 0f) 0f else delta / max, max)
    }

    /** "RRGGBB". */
    fun toHex(color: Int): String = "%06X".format(color and 0xFFFFFF)

    /** "RRGGBB", with or without "#", as an opaque colour; null for anything else. */
    fun parseHex(text: String): Int? {
        val t = text.trim().removePrefix("#")
        if (t.length != 6 || !t.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
        return (0xFF000000.toInt()) or t.toInt(16)
    }
}

private val QUICK_COLOURS = listOf(
    0xFFFFFFFF, 0xFF000000, 0xFF10131B, 0xFF171B27, 0xFF808080, 0xFFE10600, 0xFFFF2D95, 0xFFFF8A3D,
    0xFFFFB000, 0xFF6EE7A8, 0xFF2DE2E6, 0xFF4285F4, 0xFF8E5BFF,
).map { it.toInt() }

/**
 * A colour picker in MyPlay's style, after AA-Experiments' (ThemeUi.ColorPickerDialog, the owner's
 * earlier project): a field for saturation and brightness, a hue bar, a hex field and quick colours,
 * opaque only (the car screen's colours are solid). [onPick] gets the chosen ARGB colour.
 */
@Composable
fun ColourPickerDialog(title: String, initial: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val start = remember(initial) { ColourMath.colorToHsv(initial) }
    var hue by remember(initial) { mutableFloatStateOf(start[0]) }
    var saturation by remember(initial) { mutableFloatStateOf(start[1]) }
    var value by remember(initial) { mutableFloatStateOf(start[2]) }
    val colour = ColourMath.hsvToColor(hue, saturation, value)
    var hex by remember(initial) { mutableStateOf(ColourMath.toHex(initial)) }
    fun from(c: Int) { val hsv = ColourMath.colorToHsv(c); hue = hsv[0]; saturation = hsv[1]; value = hsv[2]; hex = ColourMath.toHex(c) }
    fun syncHex() { hex = ColourMath.toHex(ColourMath.hsvToColor(hue, saturation, value)) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.clip(RoundedCornerShape(28.dp)).background(PhoneColors.Page).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, color = PhoneColors.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            // Saturation (left to right) and brightness (bottom to top) for the chosen hue.
            Canvas(
                Modifier.fillMaxWidth().aspectRatio(1.5f).clip(RoundedCornerShape(14.dp)).pointerInput(Unit) {
                    awaitEachGesture {
                        fun update(p: Offset) {
                            saturation = (p.x / size.width).coerceIn(0f, 1f); value = 1f - (p.y / size.height).coerceIn(0f, 1f); syncHex()
                        }
                        val down = awaitFirstDown(); update(down.position)
                        drag(down.id) { change -> update(change.position); change.consume() }
                    }
                },
            ) {
                drawRect(Brush.horizontalGradient(listOf(Color.White, Color(ColourMath.hsvToColor(hue, 1f, 1f)))))
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                val thumb = Offset(saturation * size.width, (1f - value) * size.height)
                drawCircle(Color.White, 9.dp.toPx(), thumb, style = Stroke(2.5.dp.toPx()))
                drawCircle(Color.Black, 10.5.dp.toPx(), thumb, style = Stroke(1.dp.toPx()))
            }
            Canvas(
                Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(14.dp)).pointerInput(Unit) {
                    awaitEachGesture {
                        fun update(p: Offset) { hue = (p.x / size.width).coerceIn(0f, 1f) * 360f; syncHex() }
                        val down = awaitFirstDown(); update(down.position)
                        drag(down.id) { change -> update(change.position); change.consume() }
                    }
                },
            ) {
                drawRect(Brush.horizontalGradient((0..6).map { Color(ColourMath.hsvToColor(it * 60f, 1f, 1f)) }))
                val x = hue / 360f * size.width
                drawLine(Color.White, Offset(x, 0f), Offset(x, size.height), 4.dp.toPx())
                drawLine(Color.Black, Offset(x, 0f), Offset(x, size.height), 1.5.dp.toPx())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Swatch(initial, stringResource(R.string.phone_colour_before))
                Swatch(colour, stringResource(R.string.phone_colour_now))
                OutlinedTextField(
                    hex, { typed ->
                        hex = typed.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }.take(6)
                        ColourMath.parseHex(hex)?.let { parsed -> val hsv = ColourMath.colorToHsv(parsed); hue = hsv[0]; saturation = hsv[1]; value = hsv[2] }
                    },
                    Modifier.weight(1f), label = { Text(stringResource(R.string.phone_colour_hex)) }, singleLine = true,
                    isError = ColourMath.parseHex(hex) == null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false, keyboardType = KeyboardType.Ascii),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 16.sp, color = PhoneColors.Text),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PhoneColors.Accent, unfocusedBorderColor = PhoneColors.Border),
                )
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QUICK_COLOURS.forEach { q ->
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Color(q)).border(1.dp, PhoneColors.Border, RoundedCornerShape(10.dp)).clickable { from(q) })
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, androidx.compose.ui.Alignment.End)) {
                PillButton(stringResource(R.string.common_cancel), main = false, onClick = onDismiss)
                PillButton(stringResource(R.string.phone_colour_use)) { onPick(colour) }
            }
        }
    }
}

@Composable
private fun Swatch(c: Int, label: String) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Color(c)).border(1.dp, PhoneColors.Border, RoundedCornerShape(10.dp)))
        Text(label, color = PhoneColors.Muted, fontSize = 11.sp)
    }
}
