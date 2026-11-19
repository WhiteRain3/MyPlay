package net.prototux.aare.car

import aap_protobuf.service.ServiceOuterClass.Service
import aap_protobuf.service.inputsource.InputSourceServiceOuterClass.InputSourceService
import aap_protobuf.service.inputsource.message.AbsoluteEventOuterClass.AbsoluteEvent
import aap_protobuf.service.inputsource.message.InputReportOuterClass.InputReport
import aap_protobuf.service.inputsource.message.KeyEventOuterClass.KeyEvent
import aap_protobuf.service.inputsource.message.RelativeEventOuterClass.RelativeEvent
import aap_protobuf.service.inputsource.message.TouchEventOuterClass.TouchEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CarInputLogTest {
    private val lines = ArrayList<String>()
    private val timers = ArrayList<() -> Unit>()
    private var clock = 0L
    private var on = true
    private fun log(max: Int = CarInputLog.MAX_LINES) = CarInputLog({ on }, { lines += it }, { _, block -> timers += block }, { clock }, max)

    private fun key(code: Int, down: Boolean, long: Boolean = false, meta: Int = 0): InputReport =
        InputReport.newBuilder().setKeyEvent(KeyEvent.newBuilder().addKeys(
            KeyEvent.Key.newBuilder().setKeycode(code).setDown(down).setLongpress(long).setMetastate(meta))).build()

    private fun turn(delta: Int, code: Int = 65536): InputReport =
        InputReport.newBuilder().setRelativeEvent(RelativeEvent.newBuilder().addData(
            RelativeEvent.Rel.newBuilder().setKeycode(code).setDelta(delta))).build()

    @Test fun namesShowTheNumberToo() {
        assertEquals("BACK (4)", CarInputLog.keyName(4))
        assertEquals("DPAD_CENTER (23)", CarInputLog.keyName(23))
        assertEquals("ROTARY_CONTROLLER (65536)", CarInputLog.keyName(65536))
        assertEquals("key 999", CarInputLog.keyName(999))
    }

    @Test fun keyLinesSayDownUpHeldAndChannel() {
        assertEquals("car key BACK (4) down, input channel 6", CarInputLog.keyLine(6, 4, true, false, 0))
        assertEquals("car key BACK (4) up, input channel 6", CarInputLog.keyLine(6, 4, false, false, 0))
        assertEquals("car key SEARCH (84) down, long press, input channel 2", CarInputLog.keyLine(2, 84, true, true, 0))
        assertEquals("car key key 300 up, meta 0x41, input channel 6", CarInputLog.keyLine(6, 300, false, false, 0x41))
    }

    /** The knob's press and the D-pad are logged like any key: they are what the design needs to see. */
    @Test fun knobPressAndDpadAreLogged() {
        val l = log()
        l.report(6, key(23, true)); l.report(6, key(23, false)); l.report(6, key(21, true))
        assertEquals(listOf(
            "car key DPAD_CENTER (23) down, input channel 6",
            "car key DPAD_CENTER (23) up, input channel 6",
            "car key DPAD_LEFT (21) down, input channel 6"), lines)
    }

    @Test fun turnsInOneDirectionBecomeOneLine() {
        val l = log()
        l.report(6, turn(1)); clock += 200
        l.report(6, turn(1)); clock += 200
        l.report(6, turn(2))
        assertTrue("nothing yet: the run is still open", lines.isEmpty())
        clock += CarInputLog.RUN_GAP_MS
        timers.toList().forEach { it() }
        assertEquals(listOf("car knob turned forward 4 steps in 3 events (ROTARY_CONTROLLER (65536)), input channel 6"), lines)
    }

    @Test fun changeOfDirectionStartsANewLine() {
        val l = log()
        l.report(6, turn(1)); clock += 100
        l.report(6, turn(-1)); clock += 100
        l.end()
        assertEquals(listOf(
            "car knob turned forward 1 step in 1 event (ROTARY_CONTROLLER (65536)), input channel 6",
            "car knob turned back 1 step in 1 event (ROTARY_CONTROLLER (65536)), input channel 6"), lines)
    }

    @Test fun aKeyClosesTheRunSoTheOrderIsKept() {
        val l = log()
        l.report(6, turn(-3)); clock += 100
        l.report(6, key(23, true))
        assertEquals(listOf(
            "car knob turned back 3 steps in 1 event (ROTARY_CONTROLLER (65536)), input channel 6",
            "car key DPAD_CENTER (23) down, input channel 6"), lines)
    }

    @Test fun aPauseSplitsTurnsIntoTwoRuns() {
        val l = log()
        l.report(6, turn(1)); clock += CarInputLog.RUN_GAP_MS + 1
        l.report(6, turn(1)); l.end()
        assertEquals(2, lines.size)
    }

    /** An early timer must not close a run that is still going. */
    @Test fun aTimerThatFiresEarlyChangesNothing() {
        val l = log()
        l.report(6, turn(1)); clock += 100
        timers.toList().forEach { it() }
        assertTrue(lines.isEmpty())
        l.end()
        assertEquals(1, lines.size)
    }

    @Test fun absoluteAndTouchpadAreNoted() {
        val l = log()
        l.report(6, InputReport.newBuilder().setAbsoluteEvent(AbsoluteEvent.newBuilder().addData(
            AbsoluteEvent.Abs.newBuilder().setKeycode(7).setValue(42))).build())
        val pad = InputReport.newBuilder().setTouchpadEvent(TouchEvent.getDefaultInstance()).build()
        l.report(6, pad); l.report(6, pad)
        assertEquals(listOf(
            "car absolute input key 7 value 42 (MyPlay does not use it), input channel 6",
            "car touchpad is sending events (MyPlay does not use them), input channel 6"), lines)
    }

    @Test fun touchesAreNotLogged() {
        log().report(6, InputReport.newBuilder().setTouchEvent(TouchEvent.getDefaultInstance()).build())
        assertTrue(lines.isEmpty())
    }

    @Test fun stopsAfterTheLimitAndSaysSo() {
        val l = log(max = 3)
        repeat(10) { l.report(6, key(4, true)) }
        assertEquals(4, lines.size)
        assertEquals("car input: 3 lines written, no more in this session", lines.last())
    }

    @Test fun nothingWhenSwitchedOff() {
        on = false
        val l = log()
        l.report(6, key(4, true)); l.report(6, turn(1)); l.end()
        assertTrue(lines.isEmpty())
    }

    @Test fun serviceLineListsWhatTheCarSaysItSends() {
        val knobOnly = Service.newBuilder().setId(6).setInputSourceService(InputSourceService.newBuilder()
            .addKeycodesSupported(4).addKeycodesSupported(23).addKeycodesSupported(65536).setDisplayId(0)).build()
        assertEquals("car input channel 6: display 0, touch screen none, touchpad none, " +
            "keys it says it sends: BACK (4), DPAD_CENTER (23), ROTARY_CONTROLLER (65536)", CarInputLog.serviceLine(knobOnly, used = true))
        val touch = Service.newBuilder().setId(9).setInputSourceService(InputSourceService.newBuilder()
            .addTouchscreen(InputSourceService.TouchScreen.newBuilder().setWidth(800).setHeight(480))).build()
        assertEquals("car input channel 9 (not used by MyPlay): display 0, touch screen 800x480, touchpad none, " +
            "keys it says it sends: none listed", CarInputLog.serviceLine(touch, used = false))
    }
}
