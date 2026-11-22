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
}
