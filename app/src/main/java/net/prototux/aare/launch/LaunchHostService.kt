package net.prototux.aare.launch

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import org.lsposed.hiddenapibypass.HiddenApiBypass
import kotlin.system.exitProcess

/**
 * Puts an ordinary phone app on the car screen, from a process with adb-level (Shizuku) or root
 * rights. It creates a virtual display that renders into the car screen's [Surface], launches the
 * chosen app there, and injects the car's touches and keys into that display.
 *
 * DEVICE-TESTED, NOT verified here: creating a display an arbitrary app can run on
 * (VIRTUAL_DISPLAY_FLAG_TRUSTED and friends) and injecting input into it use hidden framework APIs
 * whose exact shape and permission requirements vary by Android version and by whether this runs as
 * root (su) or shell (Shizuku). Everything is wrapped so a failure logs and degrades rather than
 * crashing, but the privileged path WILL need on-device iteration. This is the scrcpy approach,
 * adapted to feed AARE's car screen.
 */
class LaunchHostService : ILaunchHost.Stub {
    private val injectedContext: Context?
    @Volatile private var display: VirtualDisplay? = null
    @Volatile private var displayId: Int = -1
    /** VA-20: why the last display could not be made. */
    @Volatile private var displayError: String? = null
    /** VA-17: the last start's outcome (see [status]). */
    @Volatile private var lastStatus: String = ""
    @Volatile private var displayWakeLock: PowerManager.WakeLock? = null
    private var screenOffReceiver: BroadcastReceiver? = null
    private var receiverContext: Context? = null

    constructor() : this(null)

    /** Shizuku passes a Context when the service has this constructor; the su helper passes null. */
    constructor(context: Context?) {
        this.injectedContext = context
        exemptHiddenApis()
    }

    override fun start(
        surface: Surface,
        width: Int,
        height: Int,
        dpi: Int,
        packageName: String,
        keepDisplayAwake: Boolean,
    ): Int {
        stop()
        val ctx = injectedContext ?: systemContext() ?: return (-1).also { lastStatus = LaunchStatus.with(LaunchStatus.NO_CONTEXT, method(), "no system context") }
        val vd = createDisplay(ctx, surface, width, height, dpi)
            ?: return (-1).also { lastStatus = LaunchStatus.with(LaunchStatus.NO_DISPLAY, method(), displayError ?: "createVirtualDisplay returned nothing") }
        display = vd
        displayId = vd.display.displayId
        if (keepDisplayAwake) enableKeepAwakeFallback(ctx, displayId)
        // VA-7: the car screen is landscape; a portrait-locked app (YouTube's player, most phone
        // apps) would be pillarboxed in the middle of it. Like a tablet or a desktop window, this
        // display ignores the app's orientation request, so the app fills it.
        ignoreOrientationRequest(displayId)
        val failed = launch(ctx, packageName, displayId)
        if (failed != null) {
            Log.w(TAG, "created display ${displayId} but could not launch $packageName on it: $failed")
            lastStatus = LaunchStatus.with(LaunchStatus.LAUNCH_FAILED, method(), failed)
        } else {
            lastStatus = LaunchStatus.LAUNCHED
            // VA-17: an app that refuses other screens opens on the phone instead; say so, don't let it pass.
            val id = displayId
            Thread {
                Thread.sleep(PLACEMENT_CHECK_MS)
                if (displayId != id) return@Thread
                lastStatus = when (onDisplay(ctx, packageName, id)) {
                    true -> LaunchStatus.ON_CAR
                    false -> LaunchStatus.with(LaunchStatus.ON_PHONE, method(), "activity refused display $id")
                    null -> lastStatus
                }
            }.apply { name = "AARE-placement"; isDaemon = true }.start()
        }
        return displayId
    }

    override fun resize(surface: Surface, width: Int, height: Int, dpi: Int) {
        val vd = display ?: return
        runCatching { vd.resize(width, height, dpi) }
        runCatching { vd.surface = surface }
    }

    override fun touch(downTime: Long, eventTime: Long, action: Int, x: Float, y: Float, pointerId: Int) {
        val ev = MotionEvent.obtain(downTime, eventTime, action, x, y, 0)
        ev.source = SOURCE_TOUCHSCREEN
        setDisplayId(ev, displayId)
        inject(ev)
        ev.recycle()
    }

    override fun key(eventTime: Long, action: Int, keyCode: Int) {
        val ev = KeyEvent(eventTime, eventTime, action, keyCode, 0, 0, -1, 0, 0, SOURCE_KEYBOARD)
        setDisplayId(ev, displayId)
        inject(ev)
    }

    override fun open(packageName: String, uri: String): Boolean {
        val id = displayId
        if (id < 0) return false
        val ctx = injectedContext ?: systemContext() ?: return false
        val direct = runCatching {
            // The app's own handler of the link, as the phone would pick it (never a fixed activity:
            // patched builds name theirs differently).
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri)).setPackage(packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent, fullScreen(ActivityOptions.makeBasic().setLaunchDisplayId(id)).toBundle())
        }
        if (direct.isSuccess) return true
        // VA-20: as the launch, through the activity manager's own command when the call is refused.
        val am = amStart(id, listOf("-a", Intent.ACTION_VIEW, "-d", uri, "-p", packageName))
        if (am != null) Log.w(TAG, "open($packageName) failed: ${direct.exceptionOrNull()?.message}; am: $am")
        return am == null
    }

    override fun status(): String = lastStatus

    /**
     * VA-17: whether [pkg]'s task is on display [id] (true), on another display (false), or unknown
     * (null: no task seen, or the hidden TaskInfo.displayId could not be read).
     */
    @Suppress("DEPRECATION")
    private fun onDisplay(ctx: Context, pkg: String, id: Int): Boolean? = runCatching {
        val am = ctx.getSystemService(android.app.ActivityManager::class.java)
        val tasks = am.getRunningTasks(20).filter { (it.topActivity ?: it.baseActivity)?.packageName == pkg }
        if (tasks.isEmpty()) return@runCatching null
        val field = android.app.TaskInfo::class.java.getField("displayId")
        tasks.any { field.getInt(it) == id }
    }.onFailure { Log.w(TAG, "placement check: ${it.message}") }.getOrNull()

    override fun detach() {
        runCatching { display?.surface = null }
    }

    override fun stop() {
        screenOffReceiver?.let { receiver ->
            receiverContext?.let { context -> runCatching { context.unregisterReceiver(receiver) } }
        }
        screenOffReceiver = null
        receiverContext = null
        displayWakeLock?.let { lock -> runCatching { if (lock.isHeld) lock.release() } }
        displayWakeLock = null
        display?.let { runCatching { it.release() } }
        display = null
        displayId = -1
    }

    override fun destroy() {
        stop()
        exitProcess(0)
    }

    // ---------------------------------------------------------------- privileged plumbing (reflection)

    /**
     * Hold the hosted display awake. Some Android 17 devices still suspend it with the physical
     * display, so wake the phone after a power-off event or when starting while it is already off.
     */
    // BlockedPrivateApi: this runs in the privileged helper, which exempts PowerManager from the
    // hidden API checks when it starts (exemptHiddenApis); lint only sees the app's target SDK.
    @SuppressLint("BlockedPrivateApi")
    @Suppress("DEPRECATION")
    private fun enableKeepAwakeFallback(ctx: Context, id: Int) {
        runCatching {
            val pm = ctx.getSystemService(PowerManager::class.java)
            val method = PowerManager::class.java.getDeclaredMethod(
                "newWakeLock",
                Int::class.javaPrimitiveType,
                String::class.java,
                Int::class.javaPrimitiveType,
            ).apply { isAccessible = true }
            val lock = method.invoke(pm, PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "aare:standalone-display", id)
                as PowerManager.WakeLock
            lock.setReferenceCounted(false)
            lock.acquire()
            displayWakeLock = lock
            Log.i(TAG, "holding display wake lock for display $id")
        }.onFailure { Log.w(TAG, "display wake lock failed for display $id", it) }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) wakePhysicalDisplay(id)
            }
        }
        runCatching {
            val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
            if (Build.VERSION.SDK_INT >= 33) ctx.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            else @Suppress("UnspecifiedRegisterReceiverFlag") ctx.registerReceiver(receiver, filter)
            screenOffReceiver = receiver
            receiverContext = ctx
        }.onFailure { Log.w(TAG, "could not watch physical screen state", it) }

        val phoneIsAwake = runCatching {
            ctx.getSystemService(PowerManager::class.java).isInteractive
        }.getOrDefault(true)
        if (!phoneIsAwake) wakePhysicalDisplay(id)
    }

    private fun wakePhysicalDisplay(virtualDisplayId: Int) {
        Thread {
            runCatching {
                Thread.sleep(500)
                val process = ProcessBuilder("/system/bin/cmd", "power", "wakeup")
                    .redirectErrorStream(true)
                    .start()
                val output = process.inputStream.bufferedReader().use { it.readText().trim() }
                val exit = process.waitFor()
                if (exit == 0) Log.i(TAG, "woke phone to keep display $virtualDisplayId active")
                else Log.w(TAG, "phone wake failed for display $virtualDisplayId (exit=$exit): $output")
            }.onFailure { Log.w(TAG, "phone wake failed for display $virtualDisplayId", it) }
        }.apply { name = "AARE-display-wakeup"; isDaemon = true }.start()
    }

    /**
     * Build the virtual display. Trusted + its own display group lets an arbitrary app run and focus
     * on it. VA-17: never a mirror of the phone (own content only): a public display with nothing of
     * its own on it shows the phone's screen, which is how an app that refused this display looked
     * like the phone mirrored.
     */
    private fun createDisplay(ctx: Context, surface: Surface, w: Int, h: Int, dpi: Int): VirtualDisplay? {
        val dm = ctx.getSystemService(DisplayManager::class.java) ?: return null
        // Preferred flags for hosting a foreign app interactively; fall back to the plain public ones
        // if the framework rejects the hidden bits for this uid/version.
        for (flags in intArrayOf(FULL_FLAGS, FLAG_PUBLIC or FLAG_PRESENTATION or FLAG_OWN_CONTENT_ONLY)) {
            val vd = runCatching { dm.createVirtualDisplay("AARE app", w, h, dpi.coerceAtLeast(120), surface, flags) }
                .onFailure { Log.w(TAG, "createVirtualDisplay(flags=$flags) failed: ${it.message}"); displayError = "${it.javaClass.simpleName}: ${it.message}" }
                .getOrNull()
            if (vd != null) return vd
        }
        return null
    }

    /** Launch [pkg]'s main activity onto [onDisplay]. */
    @SuppressLint("QueryPermissionsNeeded")
    /**
     * Launch [pkg]'s main activity onto [onDisplay]: null when launched, else why not (VA-20: the
     * concrete reason, for the car). The call from this process can be refused where the activity
     * manager's own `am start` is not (as root, the system context's package is not root's), so
     * that is tried next.
     */
    private fun launch(ctx: Context, pkg: String, onDisplay: Int): String? {
        val pm = ctx.packageManager
        val intent = runCatching { pm.getLaunchIntentForPackage(pkg) }.getOrNull()
        if (intent == null && runCatching { pm.getApplicationInfo(pkg, 0) }.isFailure) return "package not found"
        val direct = runCatching {
            val i = (intent ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(pkg))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            ctx.startActivity(i, fullScreen(ActivityOptions.makeBasic().setLaunchDisplayId(onDisplay)).toBundle())
        }
        if (direct.isSuccess) return null
        val first = direct.exceptionOrNull().let { "${it?.javaClass?.simpleName}: ${it?.message}" }
        val target = intent?.component?.flattenToShortString()
        val args = if (target != null) listOf("-n", target)
            else listOf("-a", Intent.ACTION_MAIN, "-c", Intent.CATEGORY_LAUNCHER, "-p", pkg)
        val flags = (Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK).toString()
        val am = amStart(onDisplay, listOf("-f", flags) + args) ?: return null
        return "$first; am start: $am"
    }

    /**
     * VA-20: `am start --display <id> --windowingMode 1 <args>`, the arguments passed as they are (no
     * shell). Null when it started the activity, else its error (or a timeout).
     */
    private fun amStart(display: Int, args: List<String>): String? = runCatching {
        val cmd = listOf("/system/bin/am", "start", "--display", display.toString(), "--windowingMode", WINDOWING_MODE_FULLSCREEN.toString()) + args
        val p = ProcessBuilder(cmd).redirectErrorStream(true).start()
        if (!p.waitFor(AM_TIMEOUT_S, java.util.concurrent.TimeUnit.SECONDS)) { p.destroy(); return@runCatching "launch timeout" }
        val out = p.inputStream.bufferedReader().use { it.readText() }
        val error = out.lineSequence().firstOrNull { it.startsWith("Error") || it.contains("Exception") || it.startsWith("Warning: Activity not started") }
        if (p.exitValue() == 0 && error == null) null else (error ?: "exit ${p.exitValue()}").trim()
    }.getOrElse { "${it.javaClass.simpleName}: ${it.message}" }

    /** VA-20: how this helper runs, for the car's message. */
    private fun method(): String = if (android.os.Process.myUid() == 0) "root" else "Shizuku"

    /**
     * VA-7: `wm set-ignore-orientation-request -d <id> true` (Android 12L and newer): the display
     * keeps its own (landscape) orientation whatever the app asks. Logged and skipped where the
     * command is missing or refused.
     */
    private fun ignoreOrientationRequest(id: Int) {
        if (id < 0 || Build.VERSION.SDK_INT < 32) return
        Thread {
            runCatching {
                val p = ProcessBuilder("/system/bin/cmd", "window", "set-ignore-orientation-request", "-d", id.toString(), "true")
                    .redirectErrorStream(true).start()
                val out = p.inputStream.bufferedReader().use { it.readText().trim() }
                if (p.waitFor() != 0) Log.w(TAG, "set-ignore-orientation-request on $id: $out")
            }.onFailure { Log.w(TAG, "set-ignore-orientation-request on $id failed: ${it.message}") }
        }.apply { name = "AARE-orientation"; isDaemon = true }.start()
    }

    /** VA-7: launch full screen on the display (hidden ActivityOptions.setLaunchWindowingMode), not as a freeform window. */
    private fun fullScreen(opts: ActivityOptions): ActivityOptions {
        runCatching {
            opts.javaClass.getMethod("setLaunchWindowingMode", Int::class.javaPrimitiveType).invoke(opts, WINDOWING_MODE_FULLSCREEN)
        }.onFailure { Log.w(TAG, "setLaunchWindowingMode: ${it.message}") }
        return opts
    }

    /** Inject one event into the launched app's display via InputManager's hidden injectInputEvent. */
    private fun inject(event: InputEvent) {
        runCatching {
            val im = inputManager ?: return
            val method = im.javaClass.getMethod("injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType)
            method.invoke(im, event, INJECT_INPUT_EVENT_MODE_ASYNC)
        }.onFailure { Log.w(TAG, "injectInputEvent failed: ${it.message}") }
    }

    /** InputManager.getInstance() (hidden; still the simplest handle that exposes injectInputEvent). */
    private val inputManager: Any? by lazy {
        runCatching {
            val cls = Class.forName("android.hardware.input.InputManager")
            cls.getMethod("getInstance").invoke(null)
        }.onFailure { Log.w(TAG, "InputManager.getInstance failed: ${it.message}") }.getOrNull()
    }

    private fun setDisplayId(event: InputEvent, id: Int) {
        if (id < 0) return
        runCatching { event.javaClass.getMethod("setDisplayId", Int::class.javaPrimitiveType).invoke(event, id) }
    }

    /** The system context of this (app_process) process, when Shizuku did not inject one. */
    @SuppressLint("PrivateApi")
    private fun systemContext(): Context? = runCatching {
        val at = Class.forName("android.app.ActivityThread")
        val thread = at.getMethod("currentActivityThread").invoke(null) ?: at.getMethod("systemMain").invoke(null)
        at.getMethod("getSystemContext").invoke(thread) as Context
    }.onFailure { Log.w(TAG, "no system context: ${it.message}") }.getOrNull()

    private fun exemptHiddenApis() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        runCatching {
            HiddenApiBypass.addHiddenApiExemptions(
                "Landroid/app/ActivityThread;", "Landroid/hardware/input/InputManager;",
                "Landroid/hardware/display/DisplayManager;", "Landroid/os/PowerManager;",
                "Landroid/view/MotionEvent;", "Landroid/view/KeyEvent;", "Landroid/app/ActivityOptions;",
            )
        }
    }

    private companion object {
        const val TAG = "AareLaunchHost"
        const val SOURCE_TOUCHSCREEN = 0x00001002 // InputDevice.SOURCE_TOUCHSCREEN
        const val SOURCE_KEYBOARD = 0x00000101 // InputDevice.SOURCE_KEYBOARD
        const val INJECT_INPUT_EVENT_MODE_ASYNC = 0
        const val WINDOWING_MODE_FULLSCREEN = 1 // WindowConfiguration.WINDOWING_MODE_FULLSCREEN (hidden)

        // VirtualDisplay flags (public + hidden bits). Hidden ones let a foreign app run and take focus.
        const val FLAG_PUBLIC = 1 shl 0
        const val FLAG_PRESENTATION = 1 shl 1
        const val FLAG_OWN_CONTENT_ONLY = 1 shl 3 // VA-17: never mirror the phone's screen
        const val FLAG_TRUSTED = 1 shl 10 // hidden, API 29+
        const val FLAG_OWN_DISPLAY_GROUP = 1 shl 11 // hidden, API 31+
        const val FLAG_ALWAYS_UNLOCKED = 1 shl 12 // hidden, API 30+
        const val FULL_FLAGS = FLAG_PUBLIC or FLAG_PRESENTATION or FLAG_OWN_CONTENT_ONLY or FLAG_TRUSTED or FLAG_OWN_DISPLAY_GROUP or FLAG_ALWAYS_UNLOCKED
        const val PLACEMENT_CHECK_MS = 1_500L
        const val AM_TIMEOUT_S = 8L
    }
}
