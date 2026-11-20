package net.prototux.aare.ui.phone

import net.prototux.aare.audio.CaptureMethod
import org.junit.Assert.assertEquals
import org.junit.Test

class BehindTheScenesTest {
    @Test
    fun soundIsOffWhenSoundToTheCarIsSwitchedOff() {
        assertEquals(SoundValue.OFF, soundValue(carAudio = false, best = CaptureMethod.AUDIO_POLICY))
        assertEquals(SoundValue.OFF, soundValue(carAudio = false, best = null))
    }

    @Test
    fun soundNeedsSettingUpWithoutAnyWayToCaptureIt() {
        assertEquals(SoundValue.NOT_SET_UP, soundValue(carAudio = true, best = null))
    }

    @Test
    fun soundNamesTheWayItIsCaptured() {
        assertEquals(SoundValue.BEST, soundValue(true, CaptureMethod.AUDIO_POLICY))
        assertEquals(SoundValue.BEST, soundValue(true, CaptureMethod.SHIZUKU_POLICY))
        assertEquals(SoundValue.ALL_SOUND, soundValue(true, CaptureMethod.SHIZUKU))
        assertEquals(SoundValue.MUSIC_ONLY, soundValue(true, CaptureMethod.PLAYBACK_CAPTURE))
    }

    @Test
    fun theBestCaptureMethodFollowsTheSoundPagesOrder() {
        assertEquals(CaptureMethod.AUDIO_POLICY, bestCaptureMethod(policy = true, shizuku = true, root = true, playbackCapture = true))
        assertEquals(CaptureMethod.SHIZUKU_POLICY, bestCaptureMethod(false, true, true, true))
        assertEquals(CaptureMethod.SHIZUKU, bestCaptureMethod(false, false, true, true))
        assertEquals(CaptureMethod.PLAYBACK_CAPTURE, bestCaptureMethod(false, false, false, true))
        assertEquals(null, bestCaptureMethod(false, false, false, false))
    }

    @Test
    fun messagesNeedAccessBeforeAnythingElse() {
        assertEquals(MessagesValue.NO_ACCESS, messagesValue(access = false, banners = true))
        assertEquals(MessagesValue.ON, messagesValue(access = true, banners = true))
        assertEquals(MessagesValue.OFF, messagesValue(access = true, banners = false))
    }

    @Test
    fun advancedShowsTheLogFirstThenTheServer() {
        assertEquals(AdvancedValue.LOG_ON, advancedValue(verboseLog = true, headUnitServer = true))
        assertEquals(AdvancedValue.SERVER_ON, advancedValue(false, true))
        assertEquals(AdvancedValue.NORMAL, advancedValue(false, false))
    }

    @Test
    fun anAppIsCountedOnceAndHiddenOnesAreLeftOut() {
        val car = setOf("maps", "music")
        val media = setOf("music", "podcasts", "chat")
        val messaging = setOf("chat", "sms", "maps")
        val messagingOnly = setOf("sms")
        // maps, music (car), podcasts and chat (media), sms (messaging): chat declares media so it is listed once.
        assertEquals(5, carScreenAppCount(car, media, messaging, messagingOnly, hidden = emptySet()))
        assertEquals(3, carScreenAppCount(car, media, messaging, messagingOnly, hidden = setOf("podcasts", "sms")))
    }

    @Test
    fun aMessagingOnlyAppIsCountedAsMessagingNotMedia() {
        assertEquals(1, carScreenAppCount(emptySet(), setOf("sms"), setOf("sms"), setOf("sms"), emptySet()))
    }

    @Test
    fun googlesAndroidAutoIsNotCountedAsTheCarLeavesItOut() {
        val gearhead = "com.google.android.projection.gearhead"
        assertEquals(1, carScreenAppCount(setOf(gearhead, "maps"), emptySet(), setOf(gearhead), emptySet(), emptySet()))
    }
}
