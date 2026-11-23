package net.prototux.aare.host.templates

import androidx.activity.ComponentActivity
import androidx.car.app.OnDoneCallback
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ItemList
import androidx.car.app.model.Row
import androidx.car.app.navigation.model.RoutePreviewNavigationTemplate
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.onNodeWithTag
import net.prototux.aare.ui.theme.CarTheme
import net.prototux.aare.ui.theme.CarThemes
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * MyPlay (GO-1, the owner: Waze's route preview needs a knob turn to reach "Go now"): the screen's
 * primary button gives CarAppScreen its focus requester, so a new screen starts with the ring on it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w1280dp-h720dp-land-mdpi")
class PrimaryFocusTest {
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

    /** A route row's title must carry its duration (the car library checks). */
    private fun timed(text: String, seconds: Long) = android.text.SpannableString(text).apply {
        setSpan(androidx.car.app.model.DurationSpan.create(seconds), 0, length, android.text.Spanned.SPAN_INCLUSIVE_INCLUSIVE)
    }

    private fun routes() = ItemList.Builder()
        .addItem(Row.Builder().setTitle(timed("24 min", 24 * 60L)).addText("Via A1").build())
        .addItem(Row.Builder().setTitle(timed("27 min", 27 * 60L)).addText("Via B2").build())
        .setOnSelectedListener { }
        .build()

    /** [go]: the routes and the Go button; else still loading (the library allows no Go button then). */
    private fun preview(go: Boolean) = RoutePreviewNavigationTemplate.Builder()
        .apply { if (go) setItemList(routes()) else setLoading(true) }
        .setTitle("Home")
        .setActionStrip(ActionStrip.Builder().addAction(Action.Builder().setTitle("Settings").setOnClickListener { }.build()).build())
        .apply { if (go) setNavigateAction(Action.Builder().setTitle("Go now").setOnClickListener { }.build()) }
        .build()

    private fun show(t: RoutePreviewNavigationTemplate, primary: PrimaryFocus, wheel: Boolean = false) = rule.setContent {
        CarTheme(night = true, theme = CarThemes.byId("aare")) {
            CompositionLocalProvider(LocalTemplateHost provides host, LocalPrimaryFocus provides primary, LocalWheelLists provides wheel) {
                @Suppress("DEPRECATION")
                RoutePreviewNavigationTemplateView(t)
            }
        }
    }

    @Test fun theRoutePreviewsGoButtonTakesTheRing() {
        val primary = PrimaryFocus()
        show(preview(go = true), primary)
        rule.waitForIdle()
        assertNotNull("Go now registered", primary.holder)
        rule.runOnIdle { primary.requester.requestFocus() }
        rule.onNodeWithText("Go now").assertIsFocused()
    }

    /** No primary button (the routes still loading; the strip's small buttons never count): nothing registers. */
    @Test fun noPrimaryButtonLeavesTheUsualStart() {
        val primary = PrimaryFocus()
        show(preview(go = false), primary)
        rule.waitForIdle()
        assertNull(primary.holder)
    }

    /**
     * Look C, step 4 (ORBIT): Go now is a big disc to the right of the routes, not a footer button;
     * it still takes the ring when the screen opens (GO-1).
     */
    @Test fun onOrbitGoNowIsADiscBesideTheRoutesAndTakesTheRing() {
        val primary = PrimaryFocus()
        show(preview(go = true), primary, wheel = true)
        rule.waitForIdle()
        val disc = rule.onNodeWithTag(GO_DISC_TAG).fetchSemanticsNode().boundsInRoot
        val routes = rule.onNodeWithText("24 min", substring = true).fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue("the disc $disc is right of the routes $routes", disc.left > routes.right)
        org.junit.Assert.assertEquals("a disc: as wide as it is high", disc.width, disc.height, 2f)
        assertNotNull("Go now registered", primary.holder)
        rule.runOnIdle { primary.requester.requestFocus() }
        rule.onNodeWithText("Go now").assertIsFocused()
    }

    /** Look C, step 4: a picture of the route preview on ORBIT (build/reports/myplay-screens). */
    @Test @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun routePreviewPicture() {
        show(preview(go = true), PrimaryFocus(), wheel = true)
        rule.waitForIdle()
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        java.io.File(java.io.File("build/reports/myplay-screens").apply { mkdirs() }, "route-preview-wheel-1280x720.png").outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
}
