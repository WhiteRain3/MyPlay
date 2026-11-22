package net.prototux.aare.ui.car.myplay

import androidx.compose.runtime.compositionLocalOf

/**
 * MyPlay (drive warnings): whether the skin's status part shows the ice sign (the car reports 4 °C
 * or less outside and the warnings are on; car/MyPlayDriveWarnings.kt). Provided by the engine round
 * [MyPlaySkin.Status]; a skin that draws no sign ignores it.
 */
val LocalRoadIce = compositionLocalOf { false }
