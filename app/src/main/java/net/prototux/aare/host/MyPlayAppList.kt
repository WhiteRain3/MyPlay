package net.prototux.aare.host

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
