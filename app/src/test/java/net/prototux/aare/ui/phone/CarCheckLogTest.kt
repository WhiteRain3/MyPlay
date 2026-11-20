package net.prototux.aare.ui.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarCheckLogTest {
    @Test
    fun privateDetailsAreTakenOut() {
        assertEquals("trusted head unit certificate for [address]: SHA-256 ab12",
            CarCheckLog.withoutPrivate("trusted head unit certificate for 00:1A:7D:DA:71:13: SHA-256 ab12"))
        assertEquals("tcp from [address] port 5277", CarCheckLog.withoutPrivate("tcp from 192.168.1.20 port 5277"))
        assertEquals("ip [address]", CarCheckLog.withoutPrivate("ip fe80:0:0:0:1c2b:3dff:fe4e:5f60"))
        assertEquals("mail [e-mail]", CarCheckLog.withoutPrivate("mail someone.else@example.org"))
        assertEquals("call [number]", CarCheckLog.withoutPrivate("call +44 20 7946 0958"))
    }

    @Test
    fun namesFromTheLogsCallSitesAreTakenOut() {
        // DrivingMode, WirelessReceiver, ProjectionService and HeadUnitNetwork, as they write them.
        assertEquals("driving: auto-replied to [name]", CarCheckLog.withoutPrivate("driving: auto-replied to Anna Schmidt"))
        assertEquals("wireless: [device] offers Android Auto wireless, connecting",
            CarCheckLog.withoutPrivate("wireless: Tom's Pixel offers Android Auto wireless, connecting"))
        assertEquals("wireless: [device] connected over Bluetooth but doesn't offer Android Auto wireless",
            CarCheckLog.withoutPrivate("wireless: Anna's headphones connected over Bluetooth but doesn't offer Android Auto wireless"))
        assertEquals("wireless: [device] already connected over Bluetooth, connecting",
            CarCheckLog.withoutPrivate("wireless: MMI 3G already connected over Bluetooth, connecting"))
        assertEquals("wireless: connecting to [device]", CarCheckLog.withoutPrivate("wireless: connecting to Audi MMI 1234"))
        assertEquals("wireless: requesting network \"[network]\" (WPA2); Android may ask the user to approve",
            CarCheckLog.withoutPrivate("wireless: requesting network \"Smith family car\" (WPA2); Android may ask the user to approve"))
        // A certificate's name stays: it names the car, not a person.
        assertEquals("connecting USB with certificate \"Audi 2019\"", CarCheckLog.withoutPrivate("connecting USB with certificate \"Audi 2019\""))
    }

    @Test
    fun phoneNumbersWrittenInGroupsAreTakenOut() {
        assertEquals("from [number]", CarCheckLog.withoutPrivate("from 06 12 34 56 78"))
        assertEquals("from [number]", CarCheckLog.withoutPrivate("from 555-123-4567"))
        assertEquals("from [number]", CarCheckLog.withoutPrivate("from (030) 123.4567"))
    }

    @Test
    fun aVinAndShortIpv6AreTakenOut() {
        assertEquals("car [VIN]", CarCheckLog.withoutPrivate("car WAUZZZ8V5KA123456"))
        assertEquals("ip [address] up", CarCheckLog.withoutPrivate("ip fe80::1c2b:3dff:fe4e:5f60 up"))
    }

    @Test
    fun theCarsCodesAndTimesStay() {
        val lines = listOf(
            "20/11 16:04:03  car key ROTARY_CONTROLLER (65536) down, input channel 3",
            "car knob turned forward 3 steps in 3 events (key 65536), input channel 3",
            "video 1280x720 at 160 dpi, margins 0,0,0,0",
            "car key DPAD_CENTER (23) up, held 1200 ms",
            "certificate \"Google Automotive Link\" expires on 2027-03-01",
            "20/11 16:04:03  video codec: H.264 (car offers H264_BP, VP9)",
            "car input channel 3: display 0, touch screen 1280x720, touchpad none, keys it says it sends: BACK (4), key 65536",
            "USB: head unit back after 1500 ms",
            "version 0.1.21, sha256 3f2a9c0e4b1d7f8a6c5e0b9d2a4f7e1c3b8d6a0f5e2c9b7a4d1f8e6c3a0b5d9e",
        )
        lines.forEach { assertEquals(it, CarCheckLog.withoutPrivate(it)) }
    }

    @Test
    fun theNewestLinesAreKeptWhenTheLogIsTooLong() {
        val log = (1..100).map { "line $it" }
        val text = CarCheckLog.shareText(listOf("Knob push: key 23"), listOf("screen 1280x720"), log, maxChars = 200)
        assertTrue(text.startsWith("MyPlay car check\nKnob push: key 23\n"))
        assertTrue(text.contains("screen 1280x720"))
        assertTrue(text.endsWith("line 100"))
        assertFalse(text.contains("line 1\n"))
        assertTrue(text.contains("older lines left out"))
        assertTrue(text.length <= 200 + 40)
    }

    @Test
    fun notConnectedIsSaid() {
        assertTrue(CarCheckLog.shareText(emptyList(), emptyList(), listOf("a")).contains("(nothing: not connected)"))
    }
}
