package net.prototux.aare.car

import aap_protobuf.service.ServiceOuterClass.Service
import aap_protobuf.service.inputsource.InputSourceServiceOuterClass.InputSourceService
import aap_protobuf.service.inputsource.message.AbsoluteEventOuterClass.AbsoluteEvent
import aap_protobuf.service.inputsource.message.InputReportOuterClass.InputReport
import aap_protobuf.service.inputsource.message.KeyEventOuterClass.KeyEvent
import aap_protobuf.service.inputsource.message.RelativeEventOuterClass.RelativeEvent
import aap_protobuf.service.inputsource.message.TouchEventOuterClass.TouchEvent
import aap_protobuf.service.media.shared.message.InsetsOuterClass.Insets
import aap_protobuf.service.media.shared.message.UiConfigOuterClass.UiConfig
import aap_protobuf.service.media.sink.message.VideoCodecResolutionTypeOuterClass.VideoCodecResolutionType
import aap_protobuf.service.media.sink.message.VideoConfigurationOuterClass.VideoConfiguration
import aap_protobuf.service.media.sink.message.VideoFrameRateTypeOuterClass.VideoFrameRateType
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** MyPlay (I4). */
class CarInputEventsTest {
    private fun key(code: Int, down: Boolean, long: Boolean = false): InputReport =
        InputReport.newBuilder().setKeyEvent(KeyEvent.newBuilder().addKeys(
            KeyEvent.Key.newBuilder().setKeycode(code).setDown(down).setLongpress(long))).build()

    @Test fun everyKeyAndTurnIsAnEventAsItCame() {
        assertEquals(listOf(CarInputEvent.Key(3, 4, true, false, 10)), CarInputEvents.eventsOf(3, key(4, true), 10, false))
        assertEquals(listOf(CarInputEvent.Key(3, 4, false, true, 11)), CarInputEvents.eventsOf(3, key(4, false, long = true), 11, false))
        val turns = InputReport.newBuilder().setRelativeEvent(RelativeEvent.newBuilder()
            .addData(RelativeEvent.Rel.newBuilder().setKeycode(65536).setDelta(1))
            .addData(RelativeEvent.Rel.newBuilder().setKeycode(65536).setDelta(-2))
            .addData(RelativeEvent.Rel.newBuilder().setKeycode(65536).setDelta(0))).build()
        assertEquals(
            listOf(CarInputEvent.Turn(3, 65536, 1, 5), CarInputEvent.Turn(3, 65536, -2, 5), CarInputEvent.Turn(3, 65536, 0, 5)),
            CarInputEvents.eventsOf(3, turns, 5, false),
        )
    }

    @Test fun absoluteAndTouchpad() {
        val abs = InputReport.newBuilder().setAbsoluteEvent(AbsoluteEvent.newBuilder().addData(
            AbsoluteEvent.Abs.newBuilder().setKeycode(7).setValue(42))).build()
        assertEquals(listOf(CarInputEvent.Absolute(6, 7, 42, 1)), CarInputEvents.eventsOf(6, abs, 1, false))
        val pad = InputReport.newBuilder().setTouchpadEvent(TouchEvent.getDefaultInstance()).build()
        assertEquals(listOf(CarInputEvent.Touchpad(6, 1)), CarInputEvents.eventsOf(6, pad, 1, false))
        assertTrue(CarInputEvents.eventsOf(6, pad, 1, true).isEmpty())
    }

    @Test fun eventsReachACollectorWithNoCap() = runBlocking {
        CarInputEvents.start(emptyList())
        val n = 300
        val got = async(start = CoroutineStart.UNDISPATCHED) { CarInputEvents.events.take(n).toList() }
        // A collector that keeps up gets every event: no cap, unlike the connection log.
        repeat(n) { CarInputEvents.report(1, key(66, it % 2 == 0), it.toLong()); yield() }
        assertEquals((0 until n).map { CarInputEvent.Key(1, 66, it % 2 == 0, false, it.toLong()) }, withTimeout(5_000) { got.await() })
    }

    @Test fun theReportHasTheCarsScreensAndInputServicesAndOneChosenLine() {
        val ui = UiConfig.newBuilder().setContentInsets(Insets.newBuilder().setTop(1).setBottom(2).setLeft(3).setRight(4)).build()
        val c0 = VideoConfiguration.newBuilder().setCodecResolution(VideoCodecResolutionType.VIDEO_1920x1080)
            .setFrameRate(VideoFrameRateType.VIDEO_FPS_60).setHeightMargin(360).setDensity(200).setUiConfig(ui).build()
        val c1 = VideoConfiguration.newBuilder().setCodecResolution(VideoCodecResolutionType.VIDEO_800x480)
            .setFrameRate(VideoFrameRateType.VIDEO_FPS_30).build()
        val input = Service.newBuilder().setId(5).setInputSourceService(InputSourceService.newBuilder().addKeycodesSupported(4)).build()
        CarInputEvents.start(CarInputEvents.sessionLines(listOf(c0, c1), listOf(input), input))
        val lines = CarInputEvents.report.value
        assertEquals(3, lines.size)
        assertEquals("car offers screen #0: 1920x1080 60 fps H264_BP, margins 0x360, density 200, content insets top 1 bottom 2 left 3 right 4", lines[0])
        assertEquals("car offers screen #1: 800x480 30 fps H264_BP, margins 0x0, density not given", lines[1])
        assertEquals(CarInputLog.serviceLine(input, used = true), lines[2])

        CarInputEvents.chosen(0, c0)
        CarInputEvents.chosen(1, c1)
        assertEquals(4, CarInputEvents.report.value.size)
        assertEquals("chosen: screen #1, 800x480 30 fps H264_BP, margins 0x0, density not given", CarInputEvents.report.value.last())

        CarInputEvents.end()
        assertTrue(CarInputEvents.report.value.isEmpty())
    }
}
