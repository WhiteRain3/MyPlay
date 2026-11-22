package net.prototux.aare.ui.phone.next

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import org.junit.Rule
import org.junit.Test

/**
 * The rebuilt phone app's shell (plan §16 chunk 0) at the design frames' size, 412 × 915 dp at 2×, to
 * set beside the PNGs in docs/design/phone-app. Drawn on the JVM: fonts and system bars differ from a phone.
 */
class NextShellMockups {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(screenWidth = 824, screenHeight = 1830, density = Density.XHIGH, softButtons = false),
        theme = "android:Theme.Material.NoActionBar",
    )

    @Composable
    private fun Shell(route: String?) {
        val owner = object : OnBackPressedDispatcherOwner {
            private val life = LifecycleRegistry.createUnsafe(this as LifecycleOwner).apply { currentState = Lifecycle.State.RESUMED }
            override val lifecycle: Lifecycle get() = life
            override val onBackPressedDispatcher = OnBackPressedDispatcher()
        }
        CompositionLocalProvider(LocalOnBackPressedDispatcherOwner provides owner) { NextApp(start = route) }
    }

    @Test fun home() = paparazzi.snapshot { Shell(null) }
    @Test fun carRoot() = paparazzi.snapshot { Shell("car") }
    @Test fun morePage() = paparazzi.snapshot { Shell("more/advanced") }
    @Test fun about() = paparazzi.snapshot { Shell("more/about") }

    @Test fun parts() = paparazzi.snapshot {
        androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.background(NextColors.Bg)) { NextPartsGallery() }
    }

    private val noTaps = object : HomeActions {
        override fun trust(hero: Hero.Trust) = Unit
        override fun notNow(hero: Hero.Trust) = Unit
        override fun cancel() = Unit
        override fun disconnect() = Unit
        override fun tryAgain() = Unit
        override fun fix(why: Why) = Unit
        override fun preview() = Unit
        override fun setUp(item: SetupItem) = Unit
        override fun dismissUpdate() = Unit
        override fun dismissCertificate() = Unit
    }

    @Composable
    private fun Home(inputs: HomeInputs) {
        val owner = object : OnBackPressedDispatcherOwner {
            private val life = LifecycleRegistry.createUnsafe(this as LifecycleOwner).apply { currentState = Lifecycle.State.RESUMED }
            override val lifecycle: Lifecycle get() = life
            override val onBackPressedDispatcher = OnBackPressedDispatcher()
        }
        CompositionLocalProvider(LocalOnBackPressedDispatcherOwner provides owner) {
            NextApp(start = "home", badges = if (inputs.update != null) setOf(NextTab.MORE) else emptySet()) {
                NextHomeContent(inputs, noTaps) {
                    net.prototux.aare.ui.phone.CarPicture("Car screen", androidx.compose.ui.Modifier.fillMaxWidth(), onPart = null)
                }
            }
        }
    }

    @Test fun homeConnected() = paparazzi.snapshot {
        Home(HomeInputs(Hero.Connected("Sedan", Link.USB, "1080p · 60 fps", "MyPlay's"), emptySet(), DesignInUse.ORBIT))
    }

    @Test fun homeTrust() = paparazzi.snapshot {
        Home(HomeInputs(Hero.Trust("k", "4f2a9c1e07b3d5a8aa", false, Link.USB), emptySet(), DesignInUse.ORBIT))
    }

    @Test fun homeNotConnected() = paparazzi.snapshot {
        Home(HomeInputs(Hero.NotConnected(Why.BLUETOOTH_OFF), setOf(Attention.FINISH_SETUP), DesignInUse.ORBIT,
            setupLeft = listOf(SetupItem.NOTIFICATION_ACCESS, SetupItem.CAR_AUDIO)))
    }

    @Test fun homeUpdate() = paparazzi.snapshot {
        Home(HomeInputs(Hero.Connected("Sedan", Link.USB, "1080p · 60 fps", "MyPlay's"), setOf(Attention.UPDATE), DesignInUse.ORBIT,
            update = net.prototux.aare.UpdateChecker.Update("2.4", "")))
    }

    @Test fun homeFailed() = paparazzi.snapshot {
        Home(HomeInputs(Hero.Failed("The car stopped the video. Code 4"), setOf(Attention.ARCHIVED), DesignInUse.DECK, errorAt = 0L))
    }

    private val noUpdateTaps = object : UpdateActions {
        override fun install(version: String) = Unit
        override fun checkNow() = Unit
        override fun setAutomatic(on: Boolean) = Unit
        override fun allowInstalls() = Unit
    }

    @Composable
    private fun Page(route: String, content: @Composable () -> Unit) {
        val owner = object : OnBackPressedDispatcherOwner {
            private val life = LifecycleRegistry.createUnsafe(this as LifecycleOwner).apply { currentState = Lifecycle.State.RESUMED }
            override val lifecycle: Lifecycle get() = life
            override val onBackPressedDispatcher = OnBackPressedDispatcher()
        }
        // No activity here: a registry that never launches, for the photo picker.
        val registry = object : androidx.activity.result.ActivityResultRegistry() {
            override fun <I, O> onLaunch(requestCode: Int, contract: androidx.activity.result.contract.ActivityResultContract<I, O>, input: I, options: androidx.core.app.ActivityOptionsCompat?) = Unit
        }
        val results = object : androidx.activity.result.ActivityResultRegistryOwner { override val activityResultRegistry = registry }
        CompositionLocalProvider(
            LocalOnBackPressedDispatcherOwner provides owner,
            androidx.activity.compose.LocalActivityResultRegistryOwner provides results,
        ) {
            NextApp(start = route, badges = setOf(NextTab.MORE)) { content() }
        }
    }

    @Test fun updatesReady() = paparazzi.snapshot {
        Page("more/updates") { NextUpdatesContent(UpdateHero.Ready("2.4"), null, 1_700_000_000_000L, true, noUpdateTaps) }
    }

    @Test fun updatesDownloading() = paparazzi.snapshot {
        Page("more/updates") { NextUpdatesContent(UpdateHero.Downloading("2.4", 42), null, 1_700_000_000_000L, true, noUpdateTaps) }
    }

    @Test fun updatesFailed() = paparazzi.snapshot {
        Page("more/updates") {
            NextUpdatesContent(UpdateHero.NeedsPermission("2.4"), null, 1_700_000_000_000L, true, noUpdateTaps)
        }
    }

    @Test fun updatesCurrent() = paparazzi.snapshot {
        Page("more/updates") { NextUpdatesContent(UpdateHero.UpToDate, null, 1_700_000_000_000L, true, noUpdateTaps) }
    }

    @Test fun whatsNew() = paparazzi.snapshot {
        Page("more/whats-new") { NextWhatsNewPage() }
    }

    @Test fun screenRoot() = paparazzi.snapshot { Page("screen") { NextScreenRoot() } }
    @Test fun screenDesign() = paparazzi.snapshot { Page("screen/design") { NextDesignPage() } }
    @Test fun screenSize() = paparazzi.snapshot { Page("screen/size") { NextSizePage() } }
    @Test fun screenWallpaper() = paparazzi.snapshot { Page("screen/wallpaper") { NextWallpaperPage() } }
    @Test fun screenMapColours() = paparazzi.snapshot { Page("screen/map-colours") { NextMapColoursPage() } }
    @Test fun screenEditor() = paparazzi.snapshot { Page("screen/wallpaper/edit") { NextPhotoEditorPage() } }

    @Test fun screenLooks() = paparazzi.snapshot { Page("screen/looks") { NextLooksPage() } }
    @Test fun screenColours() = paparazzi.snapshot { Page("screen/colours") { NextColoursPage() } }
    @Test fun screenOwnColours() = paparazzi.snapshot { Page("screen/colours/own") { NextOwnColoursPage() } }

    @Test fun carRootPage() = paparazzi.snapshot { Page("car") { NextCarRoot() } }
    @Test fun carButtons() = paparazzi.snapshot { Page("car/buttons") { NextButtonsPage() } }
    @Test fun carPad() = paparazzi.snapshot { Page("car/buttons/pad-shortcuts") { NextPadShortcutsPage() } }
    @Test fun carTiming() = paparazzi.snapshot { Page("car/buttons/timing") { NextTimingPage() } }
    @Test fun carTeach() = paparazzi.snapshot { Page("car/buttons/teach") { NextTeachPage() } }

    @Test fun car2Maps() = paparazzi.snapshot { Page("car/maps") { NextMapsPage() } }
    @Test fun car2Speed() = paparazzi.snapshot { Page("car/speed") { NextSpeedPage() } }
    @Test fun car2Sound() = paparazzi.snapshot { Page("car/sound") { NextSoundPage() } }
    @Test fun car2Messages() = paparazzi.snapshot { Page("car/messages") { NextMessagesPage() } }
    @Test fun car2Voice() = paparazzi.snapshot { Page("car/voice") { NextVoicePage() } }

    @Test fun appsRoot() = paparazzi.snapshot { Page("apps") { NextAppsRoot() } }
    @Test fun appsPhoneApps() = paparazzi.snapshot { Page("apps/phone-apps") { NextPhoneAppsPage() } }

    @Test fun moreRootPage() = paparazzi.snapshot { Page("more") { NextMoreRoot() } }
    @Test fun moreBackup() = paparazzi.snapshot { Page("more/backup") { NextBackupPage() } }
    @Test fun moreConnection() = paparazzi.snapshot { Page("more/connection") { NextConnectionPage() } }
}
