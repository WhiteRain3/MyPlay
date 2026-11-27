package net.prototux.aare.host

import androidx.core.content.edit
import net.prototux.aare.Settings

/**
 * MyPlay (K8): two rules for the car's app grid, so each entry can be told apart and can start.
 *
 * 1. Nothing from Google's Android Auto app. It offers several car screens of its own, all called
 *    "Android Auto" on the test bench; MyPlay cannot show them ("Could not start Android Auto").
 * 2. When two entries would show the same name, each one from another app shows that app's name in
 *    front: "Google · Phone". MyPlay's own built-in entries keep their plain name.
 */
internal object MyPlayAppList {
    const val GOOGLE_ANDROID_AUTO = "com.google.android.projection.gearhead"

    /** Whether an entry from [packageName] may be in the car's app grid at all. */
    fun canShow(packageName: String): Boolean = packageName != GOOGLE_ANDROID_AUTO

    /**
     * VA-1: whether [packageName]'s media-player entry (its MediaBrowserService, driven by the car's
     * media screen) is shown. Not when the app itself runs on the car screen ([realApps], AARE's
     * "Phone apps"): YouTube's media service only gives an empty library and no picture, so tapping
     * "YouTube" must open the real app. Apps that are only media apps (YouTube Music) keep theirs.
     */
    fun mediaEntryShown(packageName: String, realApps: Set<String>): Boolean = packageName !in realApps

    /** Google's own YouTube app; a patched build (ReVanced, Morphe…) has another package. */
    const val YOUTUBE = "com.google.android.youtube"

    /**
     * VA-18, VA-22: one log line on the phone apps the car can run, so "YouTube isn't on the car" can
     * be read off the log: the switch, root or Shizuku, the allowed apps (and which are hidden or
     * not installed), the ones the car lists, the YouTube app's candidates and which one was taken.
     */
    fun phoneAppsLine(
        on: Boolean,
        launcher: Boolean,
        allowed: Set<String>,
        hidden: Set<String>,
        youTubeApp: String?,
        listed: List<String> = emptyList(),
        missing: Set<String> = emptySet(),
        candidates: List<String> = emptyList(),
    ): String {
        val yt = when {
            youTubeApp == null -> "no YouTube app found"
            youTubeApp !in allowed -> "YouTube app ($youTubeApp) not allowed"
            youTubeApp in hidden -> "YouTube app ($youTubeApp) hidden"
            youTubeApp in missing -> "YouTube app ($youTubeApp) not installed"
            else -> "YouTube app ($youTubeApp) allowed"
        }
        fun list(xs: Collection<String>) = xs.sorted().joinToString(", ", "[", "]")
        return buildString {
            append("phone apps on the car: ${if (on) "on" else "off"}, root/Shizuku ${if (launcher) "yes" else "no"}, ")
            append("${allowed.size} allowed (${(allowed intersect hidden).size} hidden); $yt")
            if (allowed.isNotEmpty()) append("; allowed ${list(allowed)}")
            if ((allowed intersect hidden).isNotEmpty()) append(", hidden ${list(allowed intersect hidden)}")
            if (missing.isNotEmpty()) append(", not installed ${list(missing)}")
            if (on && launcher) append("; on the car ${listed.joinToString(", ", "[", "]")}")
            if (candidates.isNotEmpty()) append("; YouTube candidates ${candidates.distinct().joinToString(", ", "[", "]")}")
        }
    }

    /** VA-2: MovieBox Pro, found by its name (it is sideloaded, and its package differs between builds). */
    fun isMovieBox(label: String): Boolean = label.filterNot { it.isWhitespace() }.contains("moviebox", ignoreCase = true)

    /**
     * VA-4: "the YouTube app": the owner's choice [chosen] when it is installed ([installed],
     * launchable apps, package to name); else, of the apps that open a youtube.com/watch link
     * ([handlers]) and are not plain browsers ([browsers]), a patched build (not Google's own
     * package) before Google's; else an app named YouTube (not Music or Kids), as a patched build
     * may not claim the links. Null when there is none.
     */
    fun youTubeApp(handlers: List<String>, browsers: Set<String>, chosen: String?, installed: Map<String, String>): String? {
        if (chosen != null && chosen in installed) return chosen
        val apps = handlers.filter { it !in browsers }.distinct()
        apps.firstOrNull { it != YOUTUBE }?.let { return it }
        apps.firstOrNull()?.let { return it }
        val named = installed.filter { (_, label) -> isYouTubeName(label) }.keys
        return named.firstOrNull { it != YOUTUBE } ?: named.firstOrNull()
    }

    private fun isYouTubeName(label: String): Boolean {
        val l = label.lowercase()
        return "youtube" in l && "music" !in l && "kids" !in l && "studio" !in l
    }

    /**
     * VA-2: of the launchable apps [installed] (package to name), the video apps put on the car
     * screen as real phone apps once, by default: the phone's YouTube app ([youTubeApp], whatever
     * build) and MovieBox Pro.
     */
    fun videoDefaults(youTubeApp: String?, installed: Map<String, String>): Set<String> =
        installed.filter { (pkg, label) -> pkg == youTubeApp || isMovieBox(label) }.keys

    /** One entry as the naming rule sees it. [appLabel] is the name of the app it belongs to. */
    data class Named(val packageName: String, val label: String, val appLabel: String)

    /**
     * The names to show for [entries], in the same order. Names that no other entry has stay as
     * they are. Where several entries share a name, an entry of another app becomes
     * "appLabel · label", unless the app's name says nothing new (it is the same as the label or
     * empty). Entries of [ownPackage] (MyPlay's built-ins) are never renamed. Names are compared
     * without regard to case or surrounding spaces, so "Settings" and "SETTINGS" count as the same.
     */
    fun names(entries: List<Named>, ownPackage: String): List<String> {
        fun key(s: String) = s.trim().lowercase()
        val counts = entries.groupingBy { key(it.label) }.eachCount()
        return entries.map { e ->
            val clash = (counts[key(e.label)] ?: 0) > 1
            val app = e.appLabel.trim()
            if (!clash || e.packageName == ownPackage || app.isEmpty() || key(app) == key(e.label)) e.label
            else "$app · ${e.label}"
        }
    }
}

/**
 * VA-2: the video apps (YouTube, MovieBox Pro) were put among the phone apps on the car screen once.
 * After that the owner's own list rules: one taken off stays off.
 */
var Settings.myplayVideoAppsAdded: Boolean
    get() = prefs.getBoolean("myplay_video_apps_added", false)
    set(v) = prefs.edit { putBoolean("myplay_video_apps_added", v) }

/**
 * VA-4: the owner's "Video app": the package to treat as the YouTube app (a patched build among
 * several), chosen on the phone's Apps page; null: [MyPlayAppList.youTubeApp] picks.
 */
var Settings.myplayVideoApp: String?
    get() = prefs.getString("myplay_video_app", null)?.takeIf { it.isNotBlank() }
    set(v) = prefs.edit { if (v.isNullOrBlank()) remove("myplay_video_app") else putString("myplay_video_app", v) }
