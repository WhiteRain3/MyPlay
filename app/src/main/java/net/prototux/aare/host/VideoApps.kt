package net.prototux.aare.host

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import net.prototux.aare.Settings

/**
 * VA-4: the phone's video apps as installed, never by a fixed package: the YouTube app is whatever
 * opens YouTube links (a patched build such as Morphe before Google's), or the owner's choice
 * ([myplayVideoApp]). Package manager work: call it off the main thread.
 */
internal object VideoApps {
    private val YOUTUBE_LINKS = listOf("https://www.youtube.com/watch?v=0", "https://youtu.be/0", "vnd.youtube:0")
    private const val ANY_PAGE = "https://example.com/"

    /** The launchable apps, package to name. */
    fun launchable(context: Context): Map<String, String> = runCatching {
        val pm = context.packageManager
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .associate { it.activityInfo.packageName to it.loadLabel(pm).toString() }
    }.getOrDefault(emptyMap())

    /** The phone's YouTube app (see [MyPlayAppList.youTubeApp]); null when none is installed. */
    fun youTubeApp(context: Context, settings: Settings, installed: Map<String, String> = launchable(context)): String? {
        val handlers = YOUTUBE_LINKS.flatMap { handlersOf(context, it) }
        val browsers = handlersOf(context, ANY_PAGE).toSet()
        return MyPlayAppList.youTubeApp(handlers, browsers, settings.myplayVideoApp, installed)
    }

    private fun handlersOf(context: Context, link: String): List<String> = runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link)).addCategory(Intent.CATEGORY_BROWSABLE)
        context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL).map { it.activityInfo.packageName }
    }.getOrDefault(emptyList())
}
