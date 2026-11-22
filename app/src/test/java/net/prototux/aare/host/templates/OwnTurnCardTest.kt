package net.prototux.aare.host.templates

import androidx.activity.ComponentActivity
import androidx.car.app.OnDoneCallback
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Distance
import androidx.car.app.navigation.model.Maneuver
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.RoutingInfo
import androidx.car.app.navigation.model.Step
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import net.prototux.aare.ui.theme.CarTheme
import net.prototux.aare.ui.theme.CarThemes
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** MyPlay (O9): where MyPlay's own screen shows the next turn, the maps app's turn card is left out. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w1280dp-h720dp-land-mdpi")
class OwnTurnCardTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val host = object : TemplateHost {
        override val driving = false
        override val night = true
        override val appLabel = "Maps"
        override fun contentLimit(type: Int) = 6
        override val done = object : OnDoneCallback {}
        override fun back() = Unit
        override fun loadIcon(icon: CarIcon, sizePx: Int): ImageBitmap? = null
        override fun showParkedOnlyMessage() = Unit
        @androidx.compose.runtime.Composable override fun MapSurface(modifier: Modifier) = Unit
        override fun setVisibleArea(left: Int, top: Int, right: Int, bottom: Int) = Unit
        override fun setStableArea(left: Int, top: Int, right: Int, bottom: Int) = Unit
        override fun mapScroll(dx: Float, dy: Float) = Unit
        override fun mapFling(vx: Float, vy: Float) = Unit
        override fun mapScale(focusX: Float, focusY: Float, factor: Float) = Unit
        override fun mapClick(x: Float, y: Float) = Unit
    }

    private val template = NavigationTemplate.Builder()
        .setNavigationInfo(
            RoutingInfo.Builder().setCurrentStep(
                Step.Builder("Main Street").setManeuver(Maneuver.Builder(Maneuver.TYPE_TURN_NORMAL_RIGHT).build()).build(),
                Distance.create(300.0, Distance.UNIT_METERS),
            ).build(),
        )
        .setActionStrip(androidx.car.app.model.ActionStrip.Builder().addAction(androidx.car.app.model.Action.APP_ICON).build())
        .build()

    private fun show(own: Boolean) = rule.setContent {
        CarTheme(night = true, theme = CarThemes.byId("aare")) {
            CompositionLocalProvider(LocalTemplateHost provides host, LocalOwnTurnShown provides own) {
                NavigationTemplateView(template)
            }
        }
    }

    @Test fun theAppsTurnCardShowsInTheAppItself() {
        show(own = false)
        rule.onNodeWithText("Main Street", substring = true).assertExists()
    }

    @Test fun theAppsTurnCardIsLeftOutWhereMyPlayShowsTheTurn() {
        show(own = true)
        rule.onNodeWithText("Main Street", substring = true).assertDoesNotExist()
    }
}
