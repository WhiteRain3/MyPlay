package net.prototux.aare.ui.phone.next

import net.prototux.aare.aap.PinRefusal
import net.prototux.aare.service.ProjectionService.State
import org.junit.Assert.assertEquals
import org.junit.Test

/** Home's hero and cards (plan §10, design frames 01–07): one test per state. */
class NextHomeModelTest {
    private val radios = Radios(wireless = true, bluetoothOn = true, wifiOn = true)
    private val usb = "usb Audi MMI 1.0"

    @Test fun connected() = assertEquals(
        Hero.Connected("Sedan", Link.USB, "1080p · 60 fps", "MyPlay's"),
        heroOf(State(connected = true, headUnit = "Sedan", transport = usb, video = "1080p · 60 fps", certificate = "MyPlay's"), radios, false),
    )

    @Test fun connectingBeforeTheCarSaysItsName() =
        assertEquals(Hero.Connecting(null, Link.WIRELESS), heroOf(State(connected = true, transport = "tcp /10.0.0.2:5277"), radios, false))

    @Test fun connectingWirelessly() = assertEquals(Hero.Connecting(null, null), heroOf(State(running = true), radios, true))

    @Test fun trustANewCar() {
        val s = State(transport = usb, pendingTrust = PinRefusal("key", "4f2a9c1e", changed = false), lastError = "untrusted")
        assertEquals(Hero.Trust("key", "4f2a9c1e", false, Link.USB), heroOf(s, radios, false))
        // "Not now" hides it; the error under it shows.
        assertEquals(Hero.Failed("untrusted"), heroOf(s, radios, false, trustDismissed = "4f2a9c1e"))
        // A trusted, named session no longer asks.
        assertEquals(Hero.Connected("Sedan", Link.USB, "", ""),
            heroOf(s.copy(connected = true, headUnit = "Sedan", lastError = null), radios, false))
    }

    @Test fun notConnectedSaysWhy() {
        assertEquals(Hero.NotConnected(Why.BLUETOOTH_OFF), heroOf(State(), radios.copy(bluetoothOn = false), false))
        assertEquals(Hero.NotConnected(Why.WIFI_OFF), heroOf(State(), radios.copy(wifiOn = false), false))
        assertEquals(Hero.NotConnected(Why.WIRELESS_OFF), heroOf(State(), radios.copy(wireless = false, bluetoothOn = false), false))
        assertEquals(Hero.NotConnected(Why.WAITING), heroOf(State(), radios, false))
    }

    @Test fun anErrorShowsUnlessARadioIsOff() {
        assertEquals(Hero.Failed("The car stopped the video"), heroOf(State(lastError = "The car stopped the video"), radios, false))
        assertEquals(Hero.NotConnected(Why.BLUETOOTH_OFF), heroOf(State(lastError = "x"), radios.copy(bluetoothOn = false), false))
        // With wireless off, an error still shows: USB failed.
        assertEquals(Hero.Failed("x"), heroOf(State(lastError = "x"), radios.copy(wireless = false), false))
    }

    @Test fun anErrorInOneLine() {
        assertEquals("The car stopped the video", oneLine("The car stopped the video. It said: code 4\nmore"))
        assertEquals(60, oneLine("x".repeat(100)).length)
    }

    @Test fun links() {
        assertEquals(Link.USB, linkOf(usb))
        assertEquals(Link.WIRELESS, linkOf("tcp /192.168.1.5:5277"))
        assertEquals(null, linkOf("adb"))
    }

    @Test fun cardsInTheirOrderAtMostThree() {
        val all = Attention.entries.toSet()
        val c = attentionCards(all)
        assertEquals(listOf(Attention.CERT_EXPIRED, Attention.FINISH_SETUP, Attention.UPDATE), c.shown)
        assertEquals(listOf(Attention.CERT_EXPIRING, Attention.CAR_CHECK, Attention.ARCHIVED), c.more)
        assertEquals(Attention.entries, attentionCards(all, expanded = true).shown)
        assertEquals(listOf(Attention.UPDATE, Attention.ARCHIVED), attentionCards(setOf(Attention.ARCHIVED, Attention.UPDATE)).shown)
    }

    @Test fun theDesignInUse() {
        assertEquals(DesignInUse.ORBIT, designInUse(true, "orbit"))
        assertEquals(DesignInUse.DECK, designInUse(true, "deck"))
        assertEquals(DesignInUse.OLDER, designInUse(false, "orbit"))
    }
}
