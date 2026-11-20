package net.prototux.aare.ui.phone

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.prototux.aare.Settings
import net.prototux.aare.car.NextEvent
import net.prototux.aare.car.Weather
import net.prototux.aare.ui.car.CarActions
import net.prototux.aare.ui.car.CarAppEntry
import net.prototux.aare.ui.car.CarAppKind
import net.prototux.aare.ui.car.CarLayout
import net.prototux.aare.ui.car.CarNotice
import net.prototux.aare.ui.car.CarShell
import net.prototux.aare.ui.car.CarUiState
import net.prototux.aare.ui.car.DashCard
import net.prototux.aare.ui.car.NowPlaying
import net.prototux.aare.ui.theme.CarTheme
import net.prototux.aare.ui.theme.CarThemes
import net.prototux.aare.ui.theme.forPhone
import net.prototux.aare.ui.theme.withAccent

/**
 * The car screen drawn small on the phone: the car's own drawing code ([CarShell]) with made-up
 * apps and music, in the look the settings choose now. It follows the settings as they change,
 * so a change shows here at once. The car screen it imitates is the Audi's as Google's Android
 * Auto reported it: 1200 x 723 pixels at 213 dpi (about 901 x 543 dp).
 */
object CarPictureSize {
    const val WIDTH_DP = 901f
    const val HEIGHT_DP = 543f
    const val ASPECT = WIDTH_DP / HEIGHT_DP
}

/** Whether the picture is drawn at night, from the car-screen colour setting (automatic is drawn dark). */
fun pictureNight(carTheme: String): Boolean = carTheme != Settings.THEME_LIGHT

/** Whether the bar is on the right in the picture: only when set so (automatic means left on the phone, which doesn't know the car). */
fun pictureRailOnRight(railPosition: String): Boolean = railPosition == Settings.RAIL_RIGHT

/** Fills [ui] from the settings the same way the car does (CarRuntime.applyScreenSettings), without a car. */
internal fun applyPictureSettings(ui: CarUiState, settings: Settings, context: Context) {
    ui.night = pictureNight(settings.carTheme)
    ui.theme = CarThemes.byId(settings.carThemeId).forPhone(context).withAccent(settings.carAccent, context)
    ui.layout = CarLayout.byId(settings.carLayoutId)
    ui.darkBar = settings.darkBar
    ui.dashboardCards = DashCard.parse(settings.dashboardCards)
    ui.weatherInBar = settings.showWeather
    ui.weather = if (settings.showWeather) Weather(18, "°C", 1, day = !ui.night) else null
    ui.railBottom = settings.railPosition == Settings.RAIL_BOTTOM
    ui.railOnRight = pictureRailOnRight(settings.railPosition)
    ui.wallpaper = settings.wallpaper
}

/** Made-up apps, music and an appointment, so every card has something to show. Nothing here is real. */
private fun sampleCarState(): CarUiState = CarUiState().apply {
    val maps = sampleApp("maps", "Maps", CarAppKind.NAVIGATION, 0xFF34A853.toInt())
    val music = sampleApp("music", "Music", CarAppKind.MEDIA, 0xFFE8453C.toInt())
    apps += listOf(
        maps, music,
        sampleApp("podcasts", "Podcasts", CarAppKind.MEDIA, 0xFF8E44AD.toInt()),
        sampleApp("messages", "Messages", CarAppKind.MESSAGING, 0xFF1A73E8.toInt()),
        sampleApp("phone", "Phone", CarAppKind.PHONE, 0xFF0F9D58.toInt()),
        sampleApp("settings", "Settings", CarAppKind.SETTINGS, 0xFF607D8B.toInt()),
    )
    activeNavAppId = maps.id
    activeMediaAppId = music.id
    nowPlaying = NowPlaying(music.id, "Night Drive", "The Synthwaves", sampleArt(), playing = true,
        positionMs = 95_000, durationMs = 240_000, updatedAtMs = 0)
    nextEvent = NextEvent("Coffee", startMs = System.currentTimeMillis() + 45 * 60_000, allDay = false, location = "")
}

private fun sampleApp(id: String, label: String, kind: CarAppKind, color: Int): CarAppEntry {
    val b = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
    val c = Canvas(b)
    c.drawCircle(48f, 48f, 48f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = android.graphics.Color.WHITE; textSize = 52f; textAlign = Paint.Align.CENTER }
    c.drawText(label.take(1), 48f, 66f, text)
    return CarAppEntry(id, "sample.$id", label, b, kind)
}

private fun sampleArt(): Bitmap {
    val b = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
    val p = Paint().apply { shader = LinearGradient(0f, 0f, 128f, 128f, 0xFFFF7E5F.toInt(), 0xFF6A3093.toInt(), Shader.TileMode.CLAMP) }
    Canvas(b).drawRect(0f, 0f, 128f, 128f, p)
    return b
}

/** The picture does nothing when its parts are pressed: the whole picture is one button (or, later, one per part). */
private object NoCarActions : CarActions {
    override fun openApp(app: CarAppEntry) = Unit
    override fun goHome() = Unit
    override fun openLauncher() = Unit
    override fun assistant() = Unit
    override fun mediaPlayPause() = Unit
    override fun mediaNext() = Unit
    override fun mediaPrevious() = Unit
    override fun dismissNotice(notice: CarNotice) = Unit
    override fun answerCall() = Unit
    override fun endCall() = Unit
    override fun navigateTo(place: String) = Unit
}

/**
 * The live picture of the car screen, as wide as it is given. [onTap] runs when it is tapped;
 * the car screen inside never sees the touch. [description] is what TalkBack reads.
 */
@Composable
fun CarPicture(description: String, modifier: Modifier = Modifier, onTap: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    val ui = remember { sampleCarState() }
    // Any change to the settings file redraws the picture (the same file the car listens to).
    var changes by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> changes++ }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    LaunchedEffect(changes) {
        applyPictureSettings(ui, settings, context)
        ui.wallpaperImage = if (ui.wallpaper == Settings.WALLPAPER_CUSTOM) withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(java.io.File(context.filesDir, Settings.WALLPAPER_FILE).path) }.getOrNull()
        } else null
    }
    CarPictureFrame(ui, description, modifier, onTap)
}

/** Draws [ui] at the car's size, scaled down to the width it is given. */
@Composable
internal fun CarPictureFrame(ui: CarUiState, description: String, modifier: Modifier = Modifier, onTap: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .aspectRatio(CarPictureSize.ASPECT)
            .clip(shape)
            .border(BorderStroke(1.dp, Color(0xFF2F3650)), shape)
            .background(PhoneColors.Card)
            .clearAndSetSemantics { contentDescription = description; role = Role.Button },
    ) {
        val scale = maxWidth.value / CarPictureSize.WIDTH_DP
        // Laid out at the car's own size and centred, then scaled about its centre to fill this box.
        Box(
            Modifier
                .requiredSize(CarPictureSize.WIDTH_DP.dp, CarPictureSize.HEIGHT_DP.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale },
        ) {
            CarTheme(night = ui.night, theme = ui.theme) {
                CarShell(ui, NoCarActions, appContent = { _, m -> SampleMap(ui.night, m) }, navMap = { _, m -> SampleMap(ui.night, m) })
            }
        }
        // Over everything, so the car screen inside never gets the touch.
        Box(Modifier.fillMaxSize().clickable(onClick = onTap))
    }
}

/** A drawn stand-in for a map: streets, a route and the car. */
@Composable
private fun SampleMap(night: Boolean, modifier: Modifier) {
    val ground = if (night) Color(0xFF1B2330) else Color(0xFFE8EAED)
    val street = if (night) Color(0xFF34404F) else Color.White
    androidx.compose.foundation.Canvas(modifier.fillMaxSize().background(ground)) {
        val w = size.width
        val h = size.height
        for (i in 1..5) drawLine(street, Offset(0f, h * i / 6f), Offset(w, h * i / 6f - 30f), strokeWidth = 14f)
        for (i in 1..4) drawLine(street, Offset(w * i / 5f, 0f), Offset(w * i / 5f + 40f, h), strokeWidth = 14f)
        val route = Path().apply { moveTo(w * 0.5f, h); lineTo(w * 0.52f, h * 0.55f); lineTo(w * 0.8f, h * 0.5f) }
        drawPath(route, Color(0xFF4285F4), style = Stroke(width = 16f))
        drawCircle(Color.White, 18f, Offset(w * 0.505f, h * 0.85f))
        drawCircle(Color(0xFF4285F4), 12f, Offset(w * 0.505f, h * 0.85f))
    }
}
