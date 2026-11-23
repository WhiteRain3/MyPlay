package net.prototux.aare.car

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.ViewConfiguration
import net.prototux.aare.DiagLog
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.host.CarRuntime
import net.prototux.aare.media.MediaHub

/**
 * MyPlay (Preview, the dispatcher 2026-11-23): the car's input path, taken out of CarSession so it
 * runs the same on a [CarRuntime] without a car (the phone's interactive Preview): the car's keys
 * through the key injector (long presses), learned buttons, the owner's remaps, the button table
 * and its gestures (double, triple, hold, hold and turn), the knob's turns, Home, the media and
 * call keys. Mirrored phone apps get their keys from the screen as before (MirroredKeyStop), so
 * they follow from [Host.dispatchKey].
 *
 * CarSession keeps the protocol (input reports, the touchpad, the car's feedback) and hands its
 * keys and turns here; the code below is CarSession's own, moved, so a session behaves as before.
 * Main thread only, as before.
 */
class CarInputs(
    private val context: Context,
    private val settings: Settings,
    private val runtime: CarRuntime,
    private val media: MediaHub,
    private val main: Handler,
    private val host: Host,
) {
    /** For the phone's Preview: [runtime]'s own context and media, on the main thread. */
    constructor(runtime: CarRuntime, settings: Settings, host: Host) :
        this(runtime.context, settings, runtime, runtime.media, Handler(Looper.getMainLooper()), host)

    /** What the input path needs from where it runs: a car session, or the Preview. */
    interface Host {
        /** Whether there is a car screen to give keys to (a session: once its display is up). */
        val hasScreen: Boolean
        /** Whether this is over (a gesture's timer then does nothing). */
        val closed: Boolean
        /** A key for the car screen itself (the D-pad, Enter, letters...). */
        fun dispatchKey(event: KeyEvent)
        /** The screen's own Back (a Compose BackHandler); true when it took it. */
        fun screenBack(): Boolean
        /** HOME held, or the Radio key: the screen back to the car's own. */
        fun exitToCar()
        /** The knob's select was pressed on the screen (a session with a touchpad asks the car for its feedback). */
        fun selectFeedback() {}
    }

    /**
     * The Preview's [Host]: [activity]'s own window takes the keys, and its Back dispatcher the
     * screen's Back (only when something on the screen handles it: the activity itself never
     * closes from a car key). [exitToCar] is what HOME held and the Radio key do there.
     */
    class ActivityHost(private val activity: androidx.activity.ComponentActivity, private val onExitToCar: () -> Unit = {}) : Host {
        override val hasScreen get() = !activity.isFinishing
        override val closed get() = activity.isDestroyed
        override fun dispatchKey(event: KeyEvent) { activity.window?.decorView?.dispatchKeyEvent(event) }
        override fun screenBack(): Boolean {
            val d = activity.onBackPressedDispatcher
            if (!d.hasEnabledCallbacks()) return false
            d.onBackPressed()
            return true
        }
        override fun exitToCar() = onExitToCar()
    }

    /** Start listening to the settings the input path reads (the Buttons page, learned buttons, remaps). */
    fun start() {
        settings.registerListener(carKeyPrefsListener)
        settings.registerListener(buttonPrefsListener) // MyPlay (R3)
    }

    /** Stop listening to those settings. */
    fun stop() {
        settings.unregisterListener(carKeyPrefsListener)
        settings.unregisterListener(buttonPrefsListener) // MyPlay (R3)
    }

    /**
     * The code each control of the button table sends in this car (the Buttons page's choice, else
     * the defaults; the lowest when it has several), for a Preview's controls to send the same.
     */
    internal fun controlCodes(): Map<ButtonTable.Control, Int> =
        ButtonTable.Control.entries.mapNotNull { c -> buttonTable.codes(c).minOrNull()?.let { c to it } }.toMap()

    /** The buttons taught on the phone: their codes and what each does (O11). */
    fun learnedCodes(): Map<Int, ButtonAction> = learned

    /** MyPlay (G-3): the button table in use, for the connection log. */
    fun describeButtons(): String = buttonTable.describe()

    /** Pending long presses dropped (the session ended). */
    fun clear() = keys.clear()

    /** A key from the car: [code] (Android's KeyEvent codes, the car's own above 65536), pressed or released. */
    fun key(code: Int, down: Boolean, metaState: Int = 0, longPress: Boolean = false) = keys.onKey(code, down, metaState, longPress)

    /**
     * One turn of the knob by [delta] detents (negative: back): a hold that takes the turn gets it,
     * else focus moves on the screen, Tab (Shift+Tab back) per detent.
     */
    fun rotary(delta: Int) {
        // MyPlay (R3): turned while a button whose hold takes the turn is down: the hold's.
        if (gestureTurn(delta)) return
        val meta = if (delta < 0) KeyEvent.META_SHIFT_ON else 0
        repeat(kotlin.math.abs(delta)) {
            val t = SystemClock.uptimeMillis()
            host.dispatchKey(KeyEvent(t, t, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TAB, 0, meta))
            host.dispatchKey(KeyEvent(t, t, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_TAB, 0, meta))
        }
    }

    /** The car's keys, with metastate and long presses injected on the main thread. */
    private val keys = KeyInjector(ViewConfiguration.getLongPressTimeout().toLong(), object : KeyInjector.Scheduler {
        override fun postDelayed(task: Runnable, delayMs: Long) { main.postDelayed(task, delayMs) }
        override fun cancel(task: Runnable) = main.removeCallbacks(task)
        override fun uptimeMillis() = SystemClock.uptimeMillis()
        // MyPlay (IN-1, R2, R3): HOME, BACK and the button table's keys take their long press only
        // from the car; the gesture engine measures their holds, with the owner's timing.
    }, carLongPressOnly = LiveCodes { CAR_LONG_PRESS_ONLY + buttonCodes }, emit = ::onKey)

    /** HOME goes to the dashboard; held, it hands the screen back to the car. */
    private val homeKey = LongPressKey(onPress = { runtime.actions.goHome() }, onLongPress = host::exitToCar)

    /**
     * MyPlay (R2, R3): the keys of the button table (knob push, wheel click, Menu, Back) as
     * gestures, each doing what the table says. Main thread.
     */
    private var buttonTable = settings.buttonTable
    /** [buttonTable]'s codes, read for every key from the car (the injector's set below). */
    @Volatile private var buttonCodes: Set<Int> = buttonTable.allCodes
    private val buttonPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (isButtonTableKey(key)) main.post {
            buttonTable = settings.buttonTable; buttonCodes = buttonTable.allCodes
            // G-3: what the car side now uses, in the connection log.
            DiagLog.add(buttonTable.describe())
        }
        // MyPlay (O11): the phone arms a button to teach, or changes the learned ones.
        if (isLearnKey(key)) main.post {
            learned = settings.myplayLearnedButtons
            runtime.ui.learnedActions = learnedActions(learned)
            val wasAt = learnArmedAt
            learnArmed = settings.myplayLearnButton
            learnArmedAt = settings.myplayLearnButtonAtMs
            // O11-1: say on the car screen that the next button is being taught. O11-4: on every new
            // arm (its time), the same action armed again included, with the action's own name.
            learnArmed?.takeIf { learnArmedAt != wasAt }?.let { a ->
                runtime.notice(context.getString(R.string.myplay_teach_title), context.getString(R.string.myplay_teach_text, context.getString(actionNameRes(a))),
                    durationMs = LearnedButtons.ARM_MS)
            }
        }
    }

    /** MyPlay (O11): the learned buttons, and the action the next key will be taught (main thread). */
    private var learned: Map<Int, ButtonAction> = settings.myplayLearnedButtons.also { runtime.ui.learnedActions = learnedActions(it) }
    private var learnArmed: ButtonAction? = settings.myplayLearnButton
    private var learnArmedAt: Long = settings.myplayLearnButtonAtMs

    /** O11-4: an action's name, as the phone's Buttons page shows it. */
    private fun actionNameRes(a: ButtonAction): Int = when (a) {
        ButtonAction.SELECT -> R.string.phone_action_select
        ButtonAction.BACK -> R.string.phone_action_back
        ButtonAction.HOME -> R.string.phone_action_home
        ButtonAction.HOME_OR_APPS -> R.string.phone_action_home_or_apps
        ButtonAction.ASSISTANT -> R.string.phone_action_assistant
        ButtonAction.PLAY_PAUSE -> R.string.phone_action_play_pause
        ButtonAction.NEXT -> R.string.phone_action_next
        ButtonAction.PREVIOUS -> R.string.phone_action_previous
        ButtonAction.SKIP_SONGS -> R.string.phone_action_skip_songs
        ButtonAction.OPEN_NAVIGATION -> R.string.phone_action_open_navigation
        ButtonAction.OPEN_MEDIA -> R.string.phone_action_open_media
        ButtonAction.OPEN_PHONE -> R.string.phone_action_open_phone
        ButtonAction.NAVIGATE_HOME -> R.string.phone_action_navigate_home
        ButtonAction.CALL_BACK -> R.string.phone_action_call_back
        ButtonAction.NOTHING -> R.string.phone_action_nothing
    }

    /** O11-1: the arm is over ([why]: not taught, for the log; null: a key was learned). */
    private fun disarm(why: String?) {
        if (why != null) DiagLog.add("teaching a button stopped: $why")
        learnArmed = null
        settings.myplayLearnButton = null
    }

    private val gestures = GestureEngine(
        timings = { settings.gestureTimings },
        doubleCapable = { !plainPushes() && buttonTable.doubleCapable(it) },
        clock = object : GestureEngine.Clock {
            override fun now() = SystemClock.uptimeMillis()
            override fun after(delayMs: Long, task: () -> Unit): () -> Unit {
                val r = Runnable { if (!host.closed) task() }
                main.postDelayed(r, delayMs)
                return { main.removeCallbacks(r) }
            }
        },
        emit = ::onGesture,
        tripleCapable = { !plainPushes() && buttonTable.tripleCapable(it) },
    )

    /**
     * MyPlay (G-4, the Reviewer): while a call rings or is on, or a notice with buttons, a map alert
     * or the music panel holds the knob, a push must act at once: no double or triple, no wait.
     */
    private fun plainPushes(): Boolean {
        val ui = runtime.ui
        return ui.call != null || ui.notices.any { it.actions.isNotEmpty() } || ui.knobBusy
    }

    private fun gestureTurn(steps: Int) = gestures.onTurn(steps, buttonTable::turnCapable)

    private fun onGesture(g: GestureEngine.Gesture) {
        val gesture = when (g) {
            is GestureEngine.Gesture.Press -> ButtonTable.Gesture.PRESS
            is GestureEngine.Gesture.Hold -> ButtonTable.Gesture.HOLD
            is GestureEngine.Gesture.Double -> ButtonTable.Gesture.DOUBLE
            is GestureEngine.Gesture.Triple -> ButtonTable.Gesture.TRIPLE
            is GestureEngine.Gesture.HoldTurn -> ButtonTable.Gesture.HOLD_TURN
        }
        val action = buttonTable.resolve(g.code, gesture) ?: return
        DiagLog.add("button ${KeyEvent.keyCodeToString(g.code)} ${gesture.id}: ${action.id}")
        // MyPlay (R6): a short line on the car screen for what happens away from the ring.
        if (settings.myplayGestureLine && net.prototux.aare.ui.car.GestureLines.shows(action)) {
            val np = runtime.ui.nowPlaying
            runtime.ui.gestureLine = net.prototux.aare.ui.car.GestureLine(action,
                forward = ((g as? GestureEngine.Gesture.HoldTurn)?.steps ?: 1) > 0, atMs = SystemClock.uptimeMillis(),
                titleBefore = np?.title, playingBefore = np?.playing)
        }
        perform(action, g.code, (g as? GestureEngine.Gesture.HoldTurn)?.steps ?: 0)
    }

    /** MyPlay (R3, O11): what a button's [action] does, for a gesture on [keyCode] (a hold and turn's [steps]). */
    private fun perform(action: ButtonAction, keyCode: Int, steps: Int = 0) {
        when (action) {
            ButtonAction.SELECT -> {
                // B1-1: a press of the ring's item, whatever button it was given to (Back or Menu
                // sent as themselves would act as Back or Menu on the screen).
                val code = ButtonAction.selectKeyFor(keyCode)
                val t = SystemClock.uptimeMillis()
                dispatchCarKey(CarKey(code, true, downTime = t))
                dispatchCarKey(CarKey(code, false, downTime = t))
            }
            ButtonAction.BACK -> back()
            ButtonAction.HOME -> runtime.assistantClosedThenHome()
            ButtonAction.HOME_OR_APPS -> if (runtime.ui.screen == net.prototux.aare.ui.car.CarScreen.Dashboard) runtime.actions.openLauncher() else runtime.assistantClosedThenHome()
            ButtonAction.ASSISTANT -> runtime.assistant()
            ButtonAction.PLAY_PAUSE -> media.playPause()
            ButtonAction.NEXT -> media.next()
            ButtonAction.PREVIOUS -> media.previous()
            ButtonAction.SKIP_SONGS -> {
                repeat(kotlin.math.abs(steps)) { if (steps > 0) media.next() else media.previous() }
            }
            ButtonAction.OPEN_NAVIGATION -> performCarKeyAction(CarKeyAction.OPEN_NAVIGATION)
            ButtonAction.OPEN_MEDIA -> performCarKeyAction(CarKeyAction.OPEN_MEDIA)
            ButtonAction.OPEN_PHONE -> performCarKeyAction(CarKeyAction.OPEN_PHONE)
            ButtonAction.NAVIGATE_HOME -> runtime.navigateHome()
            ButtonAction.CALL_BACK -> runtime.callBackLastMissed()
            ButtonAction.NOTHING -> Unit
        }
    }

    /**
     * `Settings.carKeyMap` and the codes of `Settings.carKeysSeen`, kept here: [onKey] runs on
     * every press and every release, and decoding both string sets each time is waste. Refreshed
     * when the "Car buttons" chooser changes them (main thread only).
     */
    private var carKeyMap: Map<Int, String> = settings.carKeyMap
    private var carKeysSeen: Set<Int> = settings.carKeysSeen.keys
    private val carKeyPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "car_key_map" -> main.post { carKeyMap = settings.carKeyMap }
            "car_keys_seen" -> main.post { carKeysSeen = settings.carKeysSeen.keys }
        }
    }

    private fun onKey(key: CarKey) {
        val code = key.code
        val down = key.down
        // For the "Car buttons" chooser: every key the car has ever sent, by a readable name.
        if (code !in carKeysSeen) {
            carKeysSeen = carKeysSeen + code
            settings.rememberCarKey(code, KeyEvent.keyCodeToString(code))
        }
        // Some head units send a media key while they set up, before AARE has a car screen (a
        // play as the session starts): acting on it would start the music the driver didn't ask
        // for, and over whatever the car was playing. The driver's own presses come once the
        // screen is up.
        if (!host.hasScreen && code in MEDIA_KEYS) {
            if (!down) DiagLog.add("ignoring ${KeyEvent.keyCodeToString(code)} from the car: no car screen yet")
            return
        }
        // MyPlay (O11): a button being taught: the next key that is not Back (or the knob's) is
        // bound to the armed action, whatever its code, and does nothing else this time.
        // O11-1: the arm lasts LearnedButtons.ARM_MS, and only for one key: a key that may not be
        // taught (Back, the knob, the pad, the call keys) cancels it and then does its usual thing.
        val teaching = learnArmed?.takeIf { LearnedButtons.stillArmed(learnArmedAt, System.currentTimeMillis()) }
        if (learnArmed != null && teaching == null) disarm("too late")
        val knobOrBack = buttonTable.control(code).let { it == ButtonTable.Control.KNOB_PUSH || it == ButtonTable.Control.BACK }
        if (teaching != null && !down) {
            if (code !in LearnedButtons.NEVER && !knobOrBack) {
                learned = LearnedButtons.learn(learned, code, teaching)
                settings.myplayLearnedButtons = learned
                disarm(null)
                DiagLog.add("learned button ${KeyEvent.keyCodeToString(code)} ($code): ${teaching.id}")
                return
            }
            disarm("${KeyEvent.keyCodeToString(code)} cannot be taught")
        } else if (teaching != null && code !in LearnedButtons.NEVER && !knobOrBack) return
        // O11: a learned button does its action on release, and nothing else (never a call key or
        // the pad, even from an old backup).
        learned[code]?.takeIf { code !in LearnedButtons.NEVER && !knobOrBack }?.let { action ->
            if (!down) {
                DiagLog.add("learned button ${KeyEvent.keyCodeToString(code)}: ${action.id}")
                perform(action, code)
            }
            return
        }
        // A user remap wins over the key's normal behaviour below (never over the guard above:
        // that's about not acting before there's a screen at all, remapped or not).
        resolveCarKeyAction(carKeyMap, code)?.let { action ->
            if (!down) performCarKeyAction(action)
            return
        }
        // MyPlay (R3): the button table's keys (Back among them: held, home; twice on a page that
        // does not change, see BackEscape) go through the gesture engine.
        if (code in buttonCodes) {
            gestures.onKey(code, down, key.longPress)
            return
        }
        when (code) {
            KeyEvent.KEYCODE_HOME -> homeKey.onKey(down, key.longPress)
            KeyEvent.KEYCODE_SEARCH, KeyEvent.KEYCODE_VOICE_ASSIST, KeyEvent.KEYCODE_ASSIST -> if (!down) runtime.assistant()
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> if (!down) media.playPause()
            KeyEvent.KEYCODE_MEDIA_PLAY -> if (!down && runtime.ui.nowPlaying?.playing != true) media.playPause()
            KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_STOP -> if (!down) media.pause()
            KeyEvent.KEYCODE_MEDIA_NEXT -> if (!down) media.next()
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> if (!down) media.previous()
            KEYCODE_MEDIA -> if (!down) runtime.ui.app(runtime.ui.activeMediaAppId)?.let(runtime.actions::openApp)
            KEYCODE_NAVIGATION, KEYCODE_TURN_CARD -> if (!down) runtime.ui.app(runtime.ui.activeNavAppId)?.let(runtime.actions::openApp)
            // The car's dedicated Phone button opens the phone app; the Call/Endcall keys answer and hang up.
            KEYCODE_TEL -> if (!down) runtime.ui.app(CarRuntime.PHONE_ID)?.let(runtime.actions::openApp)
            KeyEvent.KEYCODE_CALL -> if (!down) { if (runtime.ui.call?.ringing == true) runtime.actions.answerCall() else runtime.ui.app(CarRuntime.PHONE_ID)?.let(runtime.actions::openApp) }
            KeyEvent.KEYCODE_ENDCALL -> if (!down) runtime.actions.endCall()
            // The Radio button hands the screen back to the car's own source (radio, media…), like "exit".
            KEYCODE_RADIO -> if (!down) host.exitToCar()
            else -> {
                // MyPlay (O5): a push-like key the button table does not know goes to the screen as
                // itself, whatever the Buttons page says: say so once in the connection log, so the
                // owner's log shows which code the knob really sends.
                if (!down && code in PUSH_LIKE_KEYS && unknownPushLogged.add(code)) {
                    DiagLog.add("key ${KeyEvent.keyCodeToString(code)} ($code) is not in the button table: it pressed the screen as itself (knob push codes: ${buttonTable.codes(ButtonTable.Control.KNOB_PUSH).sorted().joinToString()})")
                }
                dispatchCarKey(key)
            }
        }
    }

    /** MyPlay (O5): push-like codes already logged as missing from the button table, once per connection. */
    private val unknownPushLogged = HashSet<Int>()

    /** A key for the car screen itself: the D-pad, Enter, letters... */
    private fun dispatchCarKey(key: CarKey) {
        if (!host.hasScreen) return
        val flags = if (key.down && key.longPress) KeyEvent.FLAG_LONG_PRESS else 0
        host.dispatchKey(KeyEvent(key.downTime, SystemClock.uptimeMillis(),
            if (key.down) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP, key.code,
            key.repeat, key.metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, flags))
        // With a touchpad for navigation, the car is asked for its select feedback.
        if (key.code == KeyEvent.KEYCODE_DPAD_CENTER && !key.down) host.selectFeedback()
    }

    /** A remapped key's action (see [resolveCarKeyAction]), the same ones the keys above reach by default. */
    private fun performCarKeyAction(action: CarKeyAction) {
        when (action) {
            CarKeyAction.HOME -> runtime.actions.goHome()
            CarKeyAction.BACK -> back()
            CarKeyAction.ASSISTANT -> runtime.assistant()
            CarKeyAction.PLAY_PAUSE -> media.playPause()
            CarKeyAction.NEXT -> media.next()
            CarKeyAction.PREVIOUS -> media.previous()
            CarKeyAction.OPEN_NAVIGATION -> runtime.ui.app(runtime.ui.activeNavAppId)?.let(runtime.actions::openApp)
            CarKeyAction.OPEN_MEDIA -> runtime.ui.app(runtime.ui.activeMediaAppId)?.let(runtime.actions::openApp)
            CarKeyAction.OPEN_PHONE -> runtime.ui.app(CarRuntime.PHONE_ID)?.let(runtime.actions::openApp)
            CarKeyAction.NOTHING -> Unit
        }
    }

    /**
     * The car's Back key. A Compose BackHandler on the car screen (the media browser going up a
     * folder) comes first, as it would for the phone's own Back; then the runtime's back stack.
     * The assistant overlay closes before anything under it handles Back.
     */
    private fun back() {
        // MyPlay (R5): a ringing call is declined by Back.
        if (CallKeys.backDeclines(runtime.ui.call, Calls.canEnd(context))) { runtime.actions.endCall(); return }
        if (runtime.ui.assistantAppId == null && host.screenBack()) return
        runtime.back()
    }

    companion object {
        /** The keys that start, stop or skip media, which [onKey] ignores until there is a car screen. */
        private val MEDIA_KEYS = setOf(
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_STOP, KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_PREVIOUS,
        )
        /** MyPlay (O5): keys that press the ring's item when they reach the screen as themselves. */
        private val PUSH_LIKE_KEYS = setOf(
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_SELECT,
        )
        const val KEYCODE_MEDIA = 65537
        const val KEYCODE_NAVIGATION = 65538
        const val KEYCODE_RADIO = 65539
        const val KEYCODE_TEL = 65540
        const val KEYCODE_TURN_CARD = 65544
    }
}
