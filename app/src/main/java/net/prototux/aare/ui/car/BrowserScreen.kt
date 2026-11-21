package net.prototux.aare.ui.car

import android.annotation.SuppressLint
import android.net.Uri
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.host.CarRuntime
import net.prototux.aare.host.templates.CarKeyboard
import net.prototux.aare.host.templates.CarTextField
import net.prototux.aare.host.templates.KeyboardMode
import net.prototux.aare.host.templates.KeyboardSink
import net.prototux.aare.media.Placeholder

/**
 * A plain web browser on the car screen: a [WebView] rendered inside AARE's own projection. A
 * toolbar on top drives it (back, forward, reload, home and an address field), and because the
 * phone's IME can't appear on the car's virtual display, the address field uses AARE's own
 * [CarKeyboard]. Typing a URL opens it; anything else is a DuckDuckGo search. Tapping a text
 * field in the page opens the same keyboard for that field ([PageInput]).
 *
 * AARE is not a certified Android Auto app, so this is a parked convenience: like the phone-screen
 * mirror it is paused while driving.
 */
@Composable
internal fun BrowserScreen(runtime: CarRuntime, driving: Boolean, startUrl: String, modifier: Modifier) {
    val home = startUrl.ifBlank { Settings.BROWSER_DEFAULT_URL }
    var web by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(home) }
    var canBack by remember { mutableStateOf(false) }
    var canForward by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    // The keyboard for the page's focused text field, or null (#148).
    var pageField by remember { mutableStateOf<KeyboardMode?>(null) }
    // MyPlay (the owner: "I can't full screen the video"): a page's fullscreen view (a video), shown
    // over the whole car screen ([BrowserFullscreen]).
    val full = remember { BrowserFullscreen { on -> runtime.ui.appFullscreen = on } }
    val fullView = full.view
    fun leaveFullscreen() = full.leave()
    // FS-1: driving pauses the browser (its WebView goes): no fullscreen left behind.
    androidx.compose.runtime.LaunchedEffect(driving) { if (driving) full.leave() }
    DisposableEffect(Unit) { onDispose { full.leave() } }

    fun go(query: String) {
        web?.loadUrl(toUrl(query))
        editing = false
    }

    fun closePageField() {
        if (pageField != null) web?.evaluateJavascript(PageInput.BLUR, null)
        pageField = null
    }

    // After a tap on the page: the keyboard for the field it focused, if any.
    fun checkPageField() {
        web?.evaluateJavascript(PageInput.FOCUSED_FIELD) { result ->
            pageField = PageInput.keyboardFor(PageInput.unquote(result))
            if (pageField != null) web?.postDelayed({ web?.evaluateJavascript(PageInput.SHOW_FIELD, null) }, SHOW_FIELD_DELAY_MS)
        }
    }

    DisposableEffect(Unit) {
        // Back closes a keyboard first, then walks the page history.
        val back: () -> Boolean = {
            when {
                fullView != null -> { leaveFullscreen(); true }
                editing -> { editing = false; true }
                pageField != null -> { closePageField(); true }
                web?.canGoBack() == true -> { web?.goBack(); true }
                else -> false
            }
        }
        runtime.screenBack = back
        onDispose { if (runtime.screenBack === back) runtime.screenBack = null }
    }

    Box(modifier.background(Color.Black)) {
        if (driving) {
            Placeholder(stringResource(R.string.browser_paused_driving), loading = false, icon = Icons.Rounded.PauseCircle)
            return@Box
        }
        Column(Modifier.fillMaxSize()) {
            // Toolbar
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow).padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToolButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.common_back), canBack) { web?.goBack() }
                ToolButton(Icons.AutoMirrored.Rounded.ArrowForward, stringResource(R.string.browser_forward), canForward) { web?.goForward() }
                ToolButton(Icons.Rounded.Refresh, stringResource(R.string.browser_reload)) { web?.reload() }
                CarTextField(
                    value = if (editing) address else currentUrl,
                    hint = stringResource(R.string.browser_address_hint),
                    editing = editing,
                    onFocus = { closePageField(); address = currentUrl; editing = true },
                    onClear = { address = "" },
                    leadingIcon = Icons.Rounded.Public,
                    modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                )
                ToolButton(Icons.Rounded.Home, stringResource(R.string.browser_home)) { web?.loadUrl(home) }
            }
            // The page
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        // A video's fullscreen button asks the browser for a fullscreen view: shown
                        // over everything; Back (or the page) closes it.
                        webChromeClient = object : WebChromeClient() {
                            override fun onShowCustomView(view: android.view.View, callback: CustomViewCallback) =
                                full.show(view) { callback.onCustomViewHidden() }
                            override fun onHideCustomView() = full.hidden()
                        }
                        // Videos start on a push of their play button, as on a phone.
                        settings.mediaPlaybackRequiresUserGesture = true
                        webViewClient = object : WebViewClient() {
                            override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                                url?.let { currentUrl = it }
                                canBack = view.canGoBack(); canForward = view.canGoForward()
                                pageField = null
                            }
                        }
                        // Touching the page ends typing in the address bar; a tap (not a scroll)
                        // then checks whether it focused a text field, once the page has handled it.
                        val slop = ViewConfiguration.get(ctx).scaledTouchSlop
                        var downX = 0f
                        var downY = 0f
                        @SuppressLint("ClickableViewAccessibility")
                        setOnTouchListener { v, e ->
                            when (e.actionMasked) {
                                MotionEvent.ACTION_DOWN -> { editing = false; downX = e.x; downY = e.y }
                                MotionEvent.ACTION_UP -> if (kotlin.math.hypot(e.x - downX, e.y - downY) <= slop) {
                                    v.postDelayed({ checkPageField() }, FIELD_CHECK_DELAY_MS)
                                }
                            }
                            false
                        }
                        loadUrl(home)
                        web = this
                    }
                },
                // FS-1: the page's fullscreen view goes with its WebView.
                onRelease = { full.leave(); it.destroy() },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            // Car keyboard for the address field or the page (the phone IME can't show on the car display).
            val field = pageField
            if (editing) {
                val sink = object : KeyboardSink {
                    override fun type(text: String) { address += text }
                    override fun backspace() { address = address.dropLast(1) }
                    override fun submit() { go(address) }
                    override fun hide() { editing = false }
                }
                CarKeyboard(KeyboardMode.Text, sink, submitIcon = Icons.Rounded.Public)
            } else if (field != null) {
                val sink = object : KeyboardSink {
                    override fun type(text: String) { web?.evaluateJavascript(PageInput.insert(text), null) }
                    override fun backspace() { web?.evaluateJavascript(PageInput.BACKSPACE, null) }
                    override fun submit() { web?.evaluateJavascript(PageInput.SUBMIT, null); pageField = null }
                    override fun hide() { closePageField() }
                }
                CarKeyboard(field, sink, submitIcon = Icons.AutoMirrored.Rounded.KeyboardReturn)
            }
        }
        // The page's fullscreen view (a video), over the toolbar and the page.
        fullView?.let { v ->
            AndroidView(
                factory = { ctx -> android.widget.FrameLayout(ctx).apply { setBackgroundColor(android.graphics.Color.BLACK) } },
                update = { frame ->
                    if (v.parent !== frame) {
                        (v.parent as? android.view.ViewGroup)?.removeView(v)
                        frame.removeAllViews()
                        frame.addView(v, android.widget.FrameLayout.LayoutParams(android.widget.FrameLayout.LayoutParams.MATCH_PARENT, android.widget.FrameLayout.LayoutParams.MATCH_PARENT))
                    }
                },
                onRelease = { it.removeAllViews() },
                modifier = Modifier.fillMaxSize().testTag(BROWSER_FULLSCREEN_TAG),
            )
        }
    }
}

/**
 * MyPlay: a page's fullscreen view (a video) and how to tell the page it was closed. [onChange]
 * says whether one shows (the frame hides its rail meanwhile). [leave] closes it from our side
 * (Back, driving, the WebView going); [hidden] is the page closing it itself.
 */
internal class BrowserFullscreen(private val onChange: (Boolean) -> Unit) {
    var view by mutableStateOf<android.view.View?>(null)
        private set
    private var closed: (() -> Unit)? = null

    /** The page asks for fullscreen; a second ask while one shows is refused. */
    fun show(v: android.view.View, onClosed: () -> Unit) {
        if (view != null) { runCatching(onClosed); return }
        view = v
        closed = onClosed
        onChange(true)
    }

    /** We close it: the page is told (if its WebView is still there to hear it; FS-1). */
    fun leave() {
        if (view == null) return
        val c = closed
        view = null
        closed = null
        onChange(false)
        c?.let { runCatching(it) }
    }

    /** The page closed it. */
    fun hidden() {
        if (view == null) return
        view = null
        closed = null
        onChange(false)
    }
}

/** MyPlay: the page's fullscreen view, while it shows (tests). */
internal const val BROWSER_FULLSCREEN_TAG = "browser_fullscreen"

@Composable
private fun ToolButton(icon: ImageVector, desc: String, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.carFocusHighlight(CircleShape)) {
        Icon(icon, desc, tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

/** The page handles a tap (focus, click) before we ask it what has the focus. */
private const val FIELD_CHECK_DELAY_MS = 150L

/** Time for the keyboard to take its place before the field is scrolled back into view. */
private const val SHOW_FIELD_DELAY_MS = 300L

/** A URL to open: an address as-is (adding https:// when it has no scheme), or a DuckDuckGo search. */
private fun toUrl(query: String): String {
    val q = query.trim()
    return when {
        q.startsWith("http://") || q.startsWith("https://") -> q
        q.isNotEmpty() && !q.contains(' ') && q.contains('.') -> "https://$q"
        else -> "https://duckduckgo.com/?q=" + Uri.encode(q)
    }
}
