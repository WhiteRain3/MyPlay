package net.prototux.aare.media

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import kotlinx.coroutines.CoroutineScope
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** MyPlay (N7): one row of a music app's library, as MyPlay's own car screen shows it. */
data class LibraryRow(val id: String, val title: String, val subtitle: String, val browsable: Boolean, val playable: Boolean)

/**
 * MyPlay (N7, plan R4-2): a music app's library (its browse tree) as plain data, so MyPlay's car
 * screen can draw it in its own language and tests can walk it with a fake. The real one
 * ([MediaUiLibrary]) wraps the existing browser state ([MediaUi]) of the app's connection.
 *
 * Values are read from Compose state: reading them in a composition recomposes on change.
 */
interface MediaLibrary {
    /** The app's name, or the open level's title. */
    val title: String
    /** Changes whenever another level opens (the ring then starts at the top). */
    val key: String
    val rows: List<LibraryRow>
    /** Rows are coming (connecting, or a page loading). */
    val loading: Boolean
    /** All rows of the level are loaded. */
    val end: Boolean
    /** Why the level cannot be shown, or null. */
    val error: String?
    /** Back goes up a level inside the library (else Back leaves it). */
    val canGoBack: Boolean

    /** A browsable row opens its level; a playable one starts playing it. Returns true when it started playing. */
    fun open(row: LibraryRow): Boolean
    fun back()
    /** The next page of the open level, if there is one and none is loading. */
    fun loadMore()
    /** The row's artwork, or null. */
    suspend fun art(row: LibraryRow, sizePx: Int): Bitmap? = null

    /** Connects (while the library is on screen) and stops again. */
    fun start() = Unit
    fun stop() = Unit
}

/**
 * The real library of [app]: selects the app in [hub] and follows its connection; the levels,
 * tabs and paging are [MediaUi]'s, shared with the app's own media screen. The root's tabs (an
 * app built for Android Auto has up to four) show as the first level.
 */
internal class MediaUiLibrary(private val hub: DefaultMediaHub, private val app: MediaApp, private val scope: CoroutineScope) : MediaLibrary {
    private var connection by mutableStateOf<MediaConnection?>(null)
    private var job: Job? = null
    /** Inside one of the root's tabs (rather than looking at the tabs). */
    private var inTab by mutableStateOf(false)

    private val ui: MediaUi? get() = connection?.let(hub::uiFor)

    override fun start() {
        if (job != null) return
        hub.select(app)
        job = scope.launch {
            // A new connection replaces the old one's watchers (collectLatest cancels them).
            hub.connection.collectLatest { c ->
                val mine = c?.takeIf { it.app.service == app.service }
                connection = mine
                mine ?: return@collectLatest
                val u = hub.uiFor(mine)
                coroutineScope {
                    launch { mine.status.collect { s -> (s as? MediaConnection.Status.Ready)?.let(u::onReady) } }
                    // Subscribe to the levels on screen, as the app's own media screen does.
                    launch { snapshotFlow { u.watched }.collect { mine.watch(it) } }
                }
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
    }

    private val tabs: List<MediaItem>? get() = ui?.tabs?.takeIf { it.isNotEmpty() && !inTab }

    private val items: List<MediaItem>
        get() = tabs ?: ui?.level?.loader?.items.orEmpty()

    override val title: String
        get() = if (tabs != null) app.label else ui?.level?.title ?: app.label

    override val key: String
        get() = if (tabs != null) "tabs" else "${ui?.tab}:${ui?.level?.id}:${ui?.stack?.size}"

    override val rows: List<LibraryRow>
        get() = items.map { LibraryRow(it.mediaId, it.titleText, it.subtitleText, it.isBrowsableItem, it.isPlayableItem) }

    override val loading: Boolean
        get() {
            val u = ui ?: return true
            if (u.root == null) return true
            if (tabs != null) return false
            val l = u.level?.loader ?: return true
            return l.loading || (l.items.isEmpty() && !l.end && l.error == null)
        }

    override val end: Boolean get() = tabs != null || ui?.level?.loader?.end == true

    override val error: String? get() = if (tabs != null) null else ui?.level?.loader?.error?.message

    override val canGoBack: Boolean get() = inTab && ui?.tabs?.isNotEmpty() == true || ui?.stack?.isNotEmpty() == true

    override fun open(row: LibraryRow): Boolean {
        val u = ui ?: return false
        val t = tabs
        if (t != null) {
            val i = t.indexOfFirst { it.mediaId == row.id }
            if (i >= 0) { u.selectTab(i); inTab = true }
            return false
        }
        val item = items.firstOrNull { it.mediaId == row.id } ?: return false
        u.view = MediaView.BROWSE
        u.open(item)
        return !item.isBrowsableItem && item.isPlayableItem
    }

    override fun back() {
        val u = ui ?: return
        u.view = MediaView.BROWSE
        if (u.stack.isNotEmpty()) u.back() else if (inTab) inTab = false
    }

    override fun loadMore() {
        if (tabs != null) return
        ui?.level?.loader?.loadMore()
    }

    override suspend fun art(row: LibraryRow, sizePx: Int): Bitmap? {
        val item = items.firstOrNull { it.mediaId == row.id } ?: return null
        val md = item.mediaMetadata
        return hub.art.art(md.artworkUri, md.artworkData, connection?.packageName, sizePx)
    }
}
