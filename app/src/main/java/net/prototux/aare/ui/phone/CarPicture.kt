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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.onClick
import net.prototux.aare.ui.car.CarPart
import net.prototux.aare.ui.car.CarPartRegistry
import net.prototux.aare.ui.car.LocalCarParts
import net.prototux.aare.ui.car.RailWidget
import net.prototux.aare.ui.car.HomeMaps
import net.prototux.aare.ui.car.myplayHomeMaps
import net.prototux.aare.ui.car.statusStyle
import net.prototux.aare.ui.car.myplayLayoutSpec
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.prototux.aare.Settings
import net.prototux.aare.car.CarSession
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
import net.prototux.aare.ui.theme.CarControlSize
import net.prototux.aare.ui.theme.CarTextSize
import net.prototux.aare.ui.theme.CarTheme
import net.prototux.aare.ui.theme.LocalCarControlScale
import androidx.compose.ui.unit.Density
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
    const val WIDTH_PX = 1200
    const val HEIGHT_PX = 723
    /** The density Google's Android Auto reported for the Audi; the picture's "Auto". */
    const val AUTO_DPI = 213
    const val WIDTH_DP = 901f
    const val HEIGHT_DP = 543f
    const val ASPECT = WIDTH_DP / HEIGHT_DP

    /**
     * The car screen's size in layout units (dp) at [dpi], as the car code lays out: pixels × 160 / dpi.
     * 0 or a value the car ignores (outside CarSession.SETTING_DPI_RANGE) is Auto.
     */
    fun layoutSizeDp(dpi: Int): Pair<Float, Float> {
        val d = effectiveDensity(dpi).takeIf { it > 0 } ?: AUTO_DPI
        return WIDTH_PX * 160f / d to HEIGHT_PX * 160f / d
    }
}

/** The density setting as the car uses it: a value outside the range it accepts counts as Auto (0). */
fun effectiveDensity(stored: Int): Int = if (stored in CarSession.SETTING_DPI_RANGE) stored else 0

/** Whether the picture is drawn at night, from the car-screen colour setting (automatic is drawn dark). */
fun pictureNight(carTheme: String): Boolean = carTheme != Settings.THEME_LIGHT

/** Whether the bar is on the right in the picture: only when set so (automatic means left on the phone, which doesn't know the car). */
fun pictureRailOnRight(railPosition: String): Boolean = railPosition == Settings.RAIL_RIGHT

/** Which page a part of the picture opens. [WHOLE]: the Car screen page with every setting. */
enum class PicturePart { BAR, CLOCK, STATUS, CARDS, WALLPAPER, APPS, WHOLE }

/** The page for a tapped car-screen part. */
fun picturePartOf(part: CarPart?): PicturePart = when (part) {
    CarPart.Bar -> PicturePart.BAR
    CarPart.Clock -> PicturePart.CLOCK
    CarPart.Status -> PicturePart.STATUS
    CarPart.AppsButton -> PicturePart.APPS
    CarPart.Wallpaper -> PicturePart.WALLPAPER
    is CarPart.DashboardCard, is CarPart.MapBehindCard -> PicturePart.CARDS
    null -> PicturePart.WHOLE
}

/**
 * The part under [point] (both in the same coordinates): the smallest region containing it, so
 * the clock wins over the bar it sits in and a card over the wallpaper behind it. Null when none.
 */
fun partAt(regions: List<Pair<CarPart, Rect>>, point: Offset): Pair<CarPart, Rect>? =
    regions.filter { (_, r) -> r.contains(point) && r.width > 0f && r.height > 0f }.minByOrNull { (_, r) -> r.width * r.height }

/** The made-up maps app the picture shows on its home screen. */
const val SAMPLE_MAPS_PACKAGE = "sample.maps"

/** The home-maps setting for the picture: a chosen maps app is drawn as the picture's own made-up one, so the picture shows a map as the car would. */
fun pictureHomeMaps(stored: String): String =
    if (stored.startsWith(HomeMaps.APP_PREFIX)) HomeMaps.chosen(SAMPLE_MAPS_PACKAGE) else stored

/**
 * Fills [ui] from the settings the same way the car does, without a car. A copy of the look part
 * of CarRuntime.applyScreenSettings (host/, the car's file): when a car-screen setting is added
 * there, add it here too, or the picture will not show it.
 */
internal fun applyPictureSettings(ui: CarUiState, settings: Settings, context: Context) {
    ui.night = pictureNight(settings.carTheme)
    ui.theme = CarThemes.byId(settings.carThemeId).forPhone(context).withAccent(settings.carAccent, context)
    ui.layout = CarLayout.byId(settings.carLayoutId)
    ui.layoutSpec = settings.myplayLayoutSpec
    ui.darkBar = settings.darkBar
    ui.railWidgets = RailWidget.parse(settings.railWidgets)
    ui.statusStyle = settings.statusStyle
    ui.homeMaps = pictureHomeMaps(settings.myplayHomeMaps)
    ui.dashboardCards = DashCard.parse(settings.dashboardCards)
    ui.weatherInBar = settings.showWeather
    ui.weather = if (settings.showWeather) Weather(18, "°C", 1, day = !ui.night) else null
    ui.textScale = CarTextSize.byId(settings.carTextSize).fontScale
    ui.controlScale = CarControlSize.byId(settings.carControlSize).scale
    ui.railBottom = settings.railPosition == Settings.RAIL_BOTTOM
    ui.railOnRight = pictureRailOnRight(settings.railPosition)
    ui.wallpaper = settings.wallpaper
}

/** Made-up apps, music and an appointment, so every card has something to show. Nothing here is real. */
internal fun sampleCarState(): CarUiState = CarUiState().apply {
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
    // Paused, so the progress bar stays where it is (playing, the car moves it on from updatedAtMs).
    nowPlaying = NowPlaying(music.id, "Night Drive", "The Synthwaves", sampleArt(), playing = false,
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
 * The live picture of the car screen, as wide as it is given. With [onPart], each part (bar,
 * clock, cards, wallpaper, apps button) can be tapped on its own: it is outlined while pressed
 * and [onPart] gets it. Without, it is only a picture. The car screen inside never sees the
 * touch. [description] is what TalkBack reads; for TalkBack the picture is one button that opens
 * [PicturePart.WHOLE].
 */
@Composable
fun CarPicture(description: String, modifier: Modifier = Modifier, onPart: ((PicturePart) -> Unit)?) {
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
    CarPictureFrame(ui, description, modifier, onPart, densityDpi = remember(changes) { settings.carDensity })
}

/** Draws [ui] at the car's size, scaled down to the width it is given, with its parts tappable when [onPart] is set. */
@Composable
internal fun CarPictureFrame(ui: CarUiState, description: String, modifier: Modifier = Modifier, onPart: ((PicturePart) -> Unit)?, densityDpi: Int = 0) {
    val (carW, carH) = CarPictureSize.layoutSizeDp(densityDpi)
    val shape = RoundedCornerShape(20.dp)
    // Where the car code drew each part, in the window's coordinates (the scaling included).
    val registry = remember { CarPartRegistry() }
    var frame by remember { mutableStateOf(Rect.Zero) }
    var pressed by remember { mutableStateOf<Rect?>(null) }
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .aspectRatio(CarPictureSize.ASPECT)
            .clip(shape)
            .border(BorderStroke(1.dp, Color(0xFF2F3650)), shape)
            .background(PhoneColors.Card)
            // Not boundsInRoot: that is cut to what shows, and the picture can be half scrolled away.
            .onGloballyPositioned { frame = Rect(it.positionInRoot(), Size(it.size.width.toFloat(), it.size.height.toFloat())) }
            .clearAndSetSemantics {
                contentDescription = description
                if (onPart != null) { role = Role.Button; onClick { onPart(PicturePart.WHOLE); true } }
            },
    ) {
        val scale = maxWidth.value / carW
        // Laid out at the car's own size and centred, then scaled about its centre to fill this box.
        Box(
            Modifier
                .requiredSize(carW.dp, carH.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale },
        ) {
            // Text and control size as the car applies them (CarRuntime.Root).
            val base = LocalDensity.current
            CompositionLocalProvider(
                LocalCarParts provides registry,
                LocalCarControlScale provides ui.controlScale,
                LocalDensity provides Density(base.density, base.fontScale * ui.textScale),
            ) {
                CarTheme(night = ui.night, theme = ui.theme) {
                    CarShell(ui, NoCarActions, appContent = { _, m -> SampleMap(ui.night, m) }, navMap = { _, m -> SampleMap(ui.night, m) })
                }
            }
        }
        // Over everything, so the car screen inside never gets the touch.
        Box(
            Modifier.fillMaxSize().pointerInput(onPart) {
                if (onPart == null) {
                    // Being hit is enough: the car screen underneath (a sibling) then gets no touch.
                    // Nothing is consumed, so a drag that starts here still scrolls the page.
                    awaitPointerEventScope { while (true) awaitPointerEvent() }
                } else {
                    detectTapGestures(
                        onPress = { at ->
                            val hit = partAt(registry.regions, frame.topLeft + at)
                            pressed = hit?.second?.translate(-frame.topLeft) ?: Rect(Offset.Zero, frame.size)
                            tryAwaitRelease()
                            pressed = null
                        },
                        onTap = { at -> onPart(picturePartOf(partAt(registry.regions, frame.topLeft + at)?.first)) },
                    )
                }
            },
        ) {
            pressed?.let { r ->
                val density = LocalDensity.current
                with(density) {
                    Box(
                        Modifier
                            .offset(r.left.toDp() - 3.dp, r.top.toDp() - 3.dp)
                            .size(r.width.toDp() + 6.dp, r.height.toDp() + 6.dp)
                            .border(2.dp, PhoneColors.Accent, RoundedCornerShape(10.dp)),
                    )
                }
            }
        }
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
