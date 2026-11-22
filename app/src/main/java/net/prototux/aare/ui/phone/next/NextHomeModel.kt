package net.prototux.aare.ui.phone.next

import net.prototux.aare.service.ProjectionService

/** What the Home hero shows (plan §4, §10; design frames 01–07). One state at a time. */
sealed interface Hero {
    /** Talking to [car] over [how]. */
    data class Connected(val car: String, val how: Link?, val video: String, val certificate: String) : Hero

    /** A session is starting; [car] once the car has said its name. */
    data class Connecting(val car: String?, val how: Link?) : Hero

    /** A new or changed car certificate waits for the owner's Trust. */
    data class Trust(val peerKey: String, val fingerprint: String, val changed: Boolean, val how: Link?) : Hero

    /** The last attempt failed: [reason] in one line, the whole text on request. */
    data class Failed(val reason: String) : Hero

    /** Nothing is connected, and [why] says what the owner can do about it. */
    data class NotConnected(val why: Why) : Hero
}

/** How the phone reaches the car, in the owner's words (the service's transport names are technical). */
enum class Link { USB, WIRELESS }

/** "usb Audi MMI 1.0" → USB, "tcp /192.168.…" → wireless; anything else (adb, the bench) → none. */
fun linkOf(transport: String): Link? = when {
    transport.startsWith("usb") -> Link.USB
    transport.startsWith("tcp") || transport.startsWith("wireless") -> Link.WIRELESS
    else -> null
}

/** Why nothing is connected, in the order Home looks for it (plan §10). */
enum class Why { BLUETOOTH_OFF, WIFI_OFF, WIRELESS_OFF, WAITING }

/** What Home knows about the phone besides the service: the wireless setting and the radios. */
data class Radios(val wireless: Boolean, val bluetoothOn: Boolean, val wifiOn: Boolean)

/**
 * The hero for [state]. [wirelessConnecting] is the service's "connecting wirelessly" status (it has no
 * flag of its own); [trustDismissed] is the fingerprint the owner answered "Not now" to.
 */
fun heroOf(state: ProjectionService.State, radios: Radios, wirelessConnecting: Boolean, trustDismissed: String? = null): Hero {
    val how = linkOf(state.transport)
    val named = state.headUnit.isNotBlank()
    val trust = state.pendingTrust
    return when {
        trust != null && !(state.connected && named) && trust.fingerprint != trustDismissed ->
            Hero.Trust(trust.peerKey, trust.fingerprint, trust.changed, how)
        state.connected && named -> Hero.Connected(state.headUnit, how, state.video, state.certificate)
        state.connected || wirelessConnecting -> Hero.Connecting(state.headUnit.ifBlank { null }, how)
        // A car can't come back over the air while a radio is off: say that before an old error.
        radios.wireless && !radios.bluetoothOn -> Hero.NotConnected(Why.BLUETOOTH_OFF)
        radios.wireless && !radios.wifiOn -> Hero.NotConnected(Why.WIFI_OFF)
        state.lastError != null -> Hero.Failed(state.lastError!!)
        !radios.wireless -> Hero.NotConnected(Why.WIRELESS_OFF)
        else -> Hero.NotConnected(Why.WAITING)
    }
}

/** The first line of an error, for the hero; the whole text is under "What happened". */
fun oneLine(error: String, max: Int = 60): String {
    val first = error.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
    val sentence = first.substringBefore(". ").trimEnd('.')
    return if (sentence.length <= max) sentence else sentence.take(max - 1).trimEnd() + "…"
}

/**
 * Home's attention cards (plan §10), in their order. Trust is the hero itself, not a card.
 */
enum class Attention { CERT_EXPIRED, FINISH_SETUP, UPDATE, CERT_EXPIRING, CAR_CHECK, ARCHIVED }

/** At most three cards; the rest fold under "N more". */
data class AttentionCards(val shown: List<Attention>, val more: List<Attention>)

fun attentionCards(present: Set<Attention>, expanded: Boolean = false): AttentionCards {
    val ordered = Attention.entries.filter { it in present }
    return if (expanded || ordered.size <= 3) AttentionCards(ordered, emptyList())
    else AttentionCards(ordered.take(3), ordered.drop(3))
}

/** Which design the car screen uses, for the picture's caption and the archived card. */
enum class DesignInUse { ORBIT, DECK, OLDER }

fun designInUse(enginePreview: Boolean, language: String): DesignInUse = when {
    !enginePreview -> DesignInUse.OLDER
    language == "deck" -> DesignInUse.DECK
    else -> DesignInUse.ORBIT
}
