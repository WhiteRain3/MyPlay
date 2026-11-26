package net.prototux.aare.host

/**
 * VA-5: a video on the car screen in the real app (never the phone's screen mirrored): a link
 * shared to "Play on car" on the phone, or "Open on car" from the car's music panel for the
 * video app that is playing. Pure parts here; [CarRuntime.openVideo] does it.
 */
object CarVideo {
    /** A link to open in [packageName] on the car screen; null [link]: just the app. [atMs]: when asked (elapsedRealtime). */
    data class Link(val packageName: String, val link: String?, val atMs: Long = 0)

    /** VA-12: how long a link waits (driving) before it is dropped, so it can't open much later. */
    const val WAIT_MS = 10 * 60_000L

    private val URL = Regex("""(?:https?://|vnd\.youtube:)[^\s<>"']+""", RegexOption.IGNORE_CASE)
    private val YOUTUBE_HOSTS = setOf("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com", "youtu.be")
    private val VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")

    /** The first link in shared [text] ("Watch this: https://youtu.be/…"), trimmed of closing punctuation. */
    fun linkIn(text: String?): String? = text?.let { URL.find(it)?.value?.trimEnd('.', ',', ')', ']', '!', '?') }

    /** Whether [link] is a YouTube video or page. */
    fun isYouTube(link: String): Boolean {
        if (link.startsWith("vnd.youtube:", ignoreCase = true)) return true
        val host = link.substringAfter("://", "").substringBefore('/').substringBefore('?').substringBefore(':').lowercase()
        return host in YOUTUBE_HOSTS
    }

    /**
     * Which app opens [link] on the car: the YouTube app for a YouTube link ([youTubeApp]), else the
     * first of the apps allowed on the car screen ([allowed]) that opens it ([handlers], in the
     * phone's order). Null when none of them can.
     */
    fun appFor(link: String, youTubeApp: String?, allowed: Set<String>, handlers: List<String>): String? =
        if (isYouTube(link)) youTubeApp?.takeIf { it in allowed }
        else handlers.firstOrNull { it in allowed }

    /**
     * VA-6: a link to the video a media session is playing, from its media id when that is a
     * YouTube video id (or already a link); null when it says nothing usable.
     */
    fun linkForMediaId(mediaId: String?): String? {
        val id = mediaId?.trim().orEmpty()
        if (id.isEmpty()) return null
        if (linkIn(id) == id) return id
        return if (VIDEO_ID.matches(id)) "https://youtu.be/$id" else null
    }
}
