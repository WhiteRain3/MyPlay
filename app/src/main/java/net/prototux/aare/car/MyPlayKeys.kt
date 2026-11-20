package net.prototux.aare.car

import android.view.KeyEvent

/**
 * MyPlay (IN-1): the keys that take their long press from the car only, never from KeyInjector's
 * synthetic one. HOME, as upstream; and BACK, whose hold MyPlay measures itself (HeldKey, 0.7 s):
 * the system's long-press timeout (0.4 s) would turn a slightly long Back, or a late key-up, into
 * "home".
 */
internal val CAR_LONG_PRESS_ONLY: Set<Int> = setOf(KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_BACK)
