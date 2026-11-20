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
                CustomiseScreen(
                    pages = listOf(
                        PageLink(Icons.Rounded.Apps, "Apps", "Which apps the car shows", TileTone.VIOLET) {},
                        PageLink(Icons.Rounded.Bluetooth, "Connection", "Wireless, USB", TileTone.BLUE) {},
                        PageLink(Icons.Rounded.Tune, "Advanced", "Log, certificates", TileTone.CORAL) {},
                    ),
                    onPicture = {},
                ) {}
            }
        }
    }

    @Test fun parts() = paparazzi.snapshot {
        MyPlayPhoneTheme { Surface(Modifier.fillMaxSize()) { PhonePartsSample {} } }
    }
}
