package net.prototux.aare.host

/**
 * MyPlay: a way out of a hosted app that ignores Back.
 *
 * Back in a hosted app is handed to the app. An app on its first page does nothing with it, and
 * with a knob and a Back button only, the driver is then stuck (seen on the test bench in Fermata
 * Mirror). So: when Back comes again within [windowMs] and the app still shows the same page, the
 * second Back leaves the app.
 *
 * An app that did go back shows another page, so its second Back is its own again.
 *
 * An app answers Back a little later, not at once. So a second Back that comes sooner than
 * [settleMs] after the one before cannot tell whether the app moved, and goes to the app as well
 * (two quick Backs meant as "back two pages"). Only a Back that comes at least [settleMs] after
 * the one before, still on the same page and within [windowMs], leaves the app.
 */
internal class BackEscape(private val windowMs: Long = WINDOW_MS, private val settleMs: Long = SETTLE_MS) {
    private var lastAt = 0L
    private var lastApp: String? = null
    private var lastPage: String? = null

    /**
     * Called for each Back in hosted app [appId] showing page [pageId] at [nowMs]. True when this
     * Back should leave the app instead of going to it.
     */
    fun leaves(nowMs: Long, appId: String, pageId: String?): Boolean {
        val since = nowMs - lastAt
        val samePage = lastApp == appId && lastPage == pageId
        if (samePage && since in settleMs..windowMs) {
            lastApp = null
            return true
        }
        // Too soon to judge (the app may not have answered the last Back yet): the app gets it,
        // and the window starts again from this Back.
        lastAt = nowMs
        lastApp = appId
        lastPage = pageId
        return false
    }

    companion object {
        const val WINDOW_MS = 2000L
        const val SETTLE_MS = 300L
    }
}
