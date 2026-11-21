package net.prototux.aare.ui.phone

import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

/**
 * Pictures of MyPlay's new phone screens, drawn on the JVM by Paparazzi (no phone), so the design
 * can be checked from a cloud session. A real phone can differ (fonts, system bars). Run
 * `./build.sh :app:recordPaparazziDebug -Pmockups --no-configuration-cache --tests '*PhoneScreenMockups*'`;
 * the images land in app/src/test/snapshots/images and are not committed.
 */
class PhoneScreenMockups {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(screenHeight = 4200, softButtons = false),
        theme = "android:Theme.Material.NoActionBar",
    )

    @Test fun customise() = paparazzi.snapshot {
        MyPlayPhoneTheme {
            Surface(Modifier.fillMaxSize()) {
                CustomiseScreen(onPicture = {}, parts = listOf("Bar" to {}, "Cards" to {}, "Wallpaper" to {}, "Colours" to {}), behind = {
                    TileGrid(listOf(
                        TileSpec(BehindTile.UPDATES, Icons.Rounded.SystemUpdate, TileTone.BLUE, "Updates", "0.1.4", attention = true),
                        TileSpec(BehindTile.CONNECTION, Icons.Rounded.Bluetooth, TileTone.GREEN, "Connection", "Waiting"),
                        TileSpec(BehindTile.APPS, Icons.Rounded.Apps, TileTone.VIOLET, "Apps", "12 apps"),
                        TileSpec(BehindTile.SOUND, Icons.AutoMirrored.Rounded.VolumeUp, TileTone.AMBER, "Sound", "Best"),
                        TileSpec(BehindTile.MESSAGES, Icons.Rounded.Forum, TileTone.BLUE, "Messages", "Set up", attention = true),
                        TileSpec(BehindTile.VOICE, Icons.Rounded.Mic, TileTone.PINK, "Voice", "Google"),
                        TileSpec(BehindTile.CERTIFICATES, Icons.Rounded.VerifiedUser, TileTone.GREEN, "Certificate", "MyPlay's"),
                        TileSpec(BehindTile.ADVANCED, Icons.Rounded.Tune, TileTone.CORAL, "Advanced", "Log on"),
                        TileSpec(BehindTile.CAR_DATA, Icons.Rounded.DirectionsCar, TileTone.AMBER, "Car data", "View"),
                        TileSpec(BehindTile.ABOUT, Icons.Rounded.Info, TileTone.VIOLET, "About", "MyPlay"),
                    )) {}
                }) {}
            }
        }
    }

    @Test fun bar() = paparazzi.snapshot {
        MyPlayPhoneTheme { Surface(Modifier.fillMaxSize()) { androidx.compose.foundation.layout.Box(Modifier.padding(20.dp)) { BarPage {} } } }
    }

    @Test fun cards() = paparazzi.snapshot {
        MyPlayPhoneTheme { Surface(Modifier.fillMaxSize()) { androidx.compose.foundation.layout.Box(Modifier.padding(20.dp)) { CardsPage {} } } }
    }

    @Test fun wallpaper() = paparazzi.snapshot {
        MyPlayPhoneTheme { Surface(Modifier.fillMaxSize()) { androidx.compose.foundation.layout.Box(Modifier.padding(20.dp)) {
            // No activity here: a registry that never launches, for the photo picker.
            val registry = object : androidx.activity.result.ActivityResultRegistry() {
                override fun <I, O> onLaunch(requestCode: Int, contract: androidx.activity.result.contract.ActivityResultContract<I, O>, input: I, options: androidx.core.app.ActivityOptionsCompat?) = Unit
            }
            val owner = object : androidx.activity.result.ActivityResultRegistryOwner { override val activityResultRegistry = registry }
            androidx.compose.runtime.CompositionLocalProvider(androidx.activity.compose.LocalActivityResultRegistryOwner provides owner) { WallpaperPage { false } }
        } } }
    }

    @Test fun carCheck() = paparazzi.snapshot {
        MyPlayPhoneTheme { Surface(Modifier.fillMaxSize()) { androidx.compose.foundation.layout.Box(Modifier.padding(20.dp)) {
            val inputs = kotlinx.coroutines.flow.flowOf<CheckInput>(
                CheckInput.Key(3, 4, true, false, 0), CheckInput.Key(3, 4, true, false, 400), CheckInput.Key(3, 4, false, false, 1300),
            )
            CarCheckPage(inputs, listOf("screen 1280x720"), { emptyList() }, connected = false, now = { 2000L })
        } } }
    }

    @Test fun looks() = paparazzi.snapshot {
        MyPlayPhoneTheme { Surface(Modifier.fillMaxSize()) { androidx.compose.foundation.layout.Box(Modifier.padding(20.dp)) {
            val registry = object : androidx.activity.result.ActivityResultRegistry() {
                override fun <I, O> onLaunch(requestCode: Int, contract: androidx.activity.result.contract.ActivityResultContract<I, O>, input: I, options: androidx.core.app.ActivityOptionsCompat?) = Unit
            }
            val owner = object : androidx.activity.result.ActivityResultRegistryOwner { override val activityResultRegistry = registry }
            androidx.compose.runtime.CompositionLocalProvider(androidx.activity.compose.LocalActivityResultRegistryOwner provides owner) { LooksPage() }
        } } }
    }

    @Test fun speedLimits() = paparazzi.snapshot {
        MyPlayPhoneTheme { Surface(Modifier.fillMaxSize()) { androidx.compose.foundation.layout.Box(Modifier.padding(20.dp)) { SpeedLimitPage() } } }
    }

    @Test fun mapColours() = paparazzi.snapshot {
        MyPlayPhoneTheme { Surface(Modifier.fillMaxSize()) { androidx.compose.foundation.layout.Column(Modifier.padding(20.dp)) {
            MapColoursSection(net.prototux.aare.Settings(androidx.compose.ui.platform.LocalContext.current), night = true)
        } } }
    }

    @Test fun colours() = paparazzi.snapshot {
        MyPlayPhoneTheme { Surface(Modifier.fillMaxSize()) { androidx.compose.foundation.layout.Box(Modifier.padding(20.dp)) { ColoursPage() } } }
    }

    /** Every part the car code reports, outlined: checks that the picture can find them. */
    @Test fun pictureParts() = paparazzi.snapshot {
        MyPlayPhoneTheme {
            Surface(Modifier.fillMaxSize()) {
                val ui = androidx.compose.runtime.remember { net.prototux.aare.ui.car.CarUiState().apply { dashboardCards = net.prototux.aare.ui.car.DashCard.DEFAULT } }
                val registry = androidx.compose.runtime.remember { net.prototux.aare.ui.car.CarPartRegistry() }
                androidx.compose.foundation.layout.Box(Modifier.padding(20.dp)) {
                    androidx.compose.runtime.CompositionLocalProvider(net.prototux.aare.ui.car.LocalCarParts provides registry) {
                        net.prototux.aare.ui.theme.CarTheme(night = true, theme = ui.theme) {
                            androidx.compose.foundation.layout.Box(Modifier.size(450.dp, 271.dp)) {
                                net.prototux.aare.ui.car.CarShell(ui, object : net.prototux.aare.ui.car.CarActions {
                                    override fun openApp(app: net.prototux.aare.ui.car.CarAppEntry) = Unit
                                    override fun goHome() = Unit
                                    override fun openLauncher() = Unit
                                    override fun assistant() = Unit
                                    override fun mediaPlayPause() = Unit
                                    override fun mediaNext() = Unit
                                    override fun mediaPrevious() = Unit
                                    override fun dismissNotice(notice: net.prototux.aare.ui.car.CarNotice) = Unit
                                    override fun answerCall() = Unit
                                    override fun endCall() = Unit
                                    override fun navigateTo(place: String) = Unit
                                }, appContent = { _, _ -> }, navMap = { _, _ -> })
                            }
                        }
                    }
                }
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    registry.regions.forEach { (_, r) ->
                        drawRect(androidx.compose.ui.graphics.Color.Red, r.topLeft, r.size, style = androidx.compose.ui.graphics.drawscope.Stroke(4f))
                    }
                }
                androidx.compose.material3.Text(registry.regions.joinToString("\n") { it.first.toString() }, Modifier.padding(top = 320.dp, start = 20.dp))
            }
        }
    }

    /** The older pages' parts in the new style (E7), with texts from the Connection and Car screen pages. */
    @Test fun oldPageParts() = paparazzi.snapshot {
        MyPlayPhoneTheme {
            Surface(Modifier.fillMaxSize()) {
                androidx.compose.foundation.layout.Column(Modifier.padding(20.dp)) {
                    PhoneTopBar("Connection", onBack = {})
                    PhoneSection("Connecting") {
                        PhoneToggle("Wireless", "Connect to wireless head units over Wi-Fi", true) {}
                        PhoneToggle("Resume music", "Start playing again when the car connects", false) {}
                    }
                    androidx.compose.foundation.layout.Spacer(Modifier.size(16.dp))
                    PhoneSection("Look") {
                        PhoneChoices("Colors", listOf("auto" to "Car's day/night", "dark" to "Dark", "light" to "Light"), "dark") {}
                        PhoneChoices("Bar", listOf("auto" to "Driver's side", "left" to "Left", "right" to "Right", "bottom" to "Bottom"), "auto") {}
                    }
                }
            }
        }
    }

    @Test fun layout() = paparazzi.snapshot {
        MyPlayPhoneTheme {
            Surface(Modifier.fillMaxSize()) {
                androidx.compose.foundation.layout.Box(Modifier.padding(20.dp)) {
                    MapBehindOptions(net.prototux.aare.ui.car.CarLayoutSpec.PRESETS.getValue("C"), "C", {}, {})
                }
            }
        }
    }

    @Test fun statusIcons() = paparazzi.snapshot {
        MyPlayPhoneTheme { Surface(Modifier.fillMaxSize()) { androidx.compose.foundation.layout.Box(Modifier.padding(20.dp)) { StatusIconsPage() } } }
    }

    @Test fun clock() = paparazzi.snapshot {
        MyPlayPhoneTheme { Surface(Modifier.fillMaxSize()) { androidx.compose.foundation.layout.Box(Modifier.padding(20.dp)) { ClockPage() } } }
    }

    /** The status strip at its largest (clock and icons Larger), for a narrow phone: see that nothing spills. */
    @Test fun statusZoomLargest() = paparazzi.snapshot {
        MyPlayPhoneTheme {
            Surface(Modifier.fillMaxSize()) {
                androidx.compose.foundation.layout.Box(Modifier.padding(18.dp).size(width = 324.dp, height = 200.dp)) {
                    StatusZoom(net.prototux.aare.ui.car.StatusStyle(percentSign = true, size = net.prototux.aare.ui.car.StatusSize.LARGER,
                        clockSize = net.prototux.aare.ui.car.StatusSize.LARGER, clockBold = true))
                }
            }
        }
    }

    @Test fun size() = paparazzi.snapshot {
        MyPlayPhoneTheme { Surface(Modifier.fillMaxSize()) { androidx.compose.foundation.layout.Box(Modifier.padding(20.dp)) { SizePage() } } }
    }

    @Test fun whatsNew() = paparazzi.snapshot {
        val notes = listOf("../docs/releases/0.1.8.md", "docs/releases/0.1.8.md").map { java.io.File(it) }.first { it.isFile }.readText()
        MyPlayPhoneTheme {
            Surface(Modifier.fillMaxSize()) {
                androidx.compose.foundation.layout.Column(Modifier.padding(20.dp)) { ReleaseNotesText(ReleaseNotes.parse(notes)) }
            }
        }
    }

    @Test fun buttons() = paparazzi.snapshot {
        MyPlayPhoneTheme { Surface(Modifier.fillMaxSize()) { androidx.compose.foundation.layout.Box(Modifier.padding(20.dp)) { ButtonsScreen() } } }
    }

    @Test fun parts() = paparazzi.snapshot {
        MyPlayPhoneTheme { Surface(Modifier.fillMaxSize()) { PhonePartsSample {} } }
    }
}
