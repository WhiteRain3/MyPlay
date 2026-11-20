package net.prototux.aare.car

import net.prototux.aare.ui.car.CarCall

/**
 * MyPlay (plan R5): the car's buttons while a call rings. The knob's push answers (the ring starts
 * on Answer, ui/car/CallCard.kt); Back declines, wherever the ring is, when Android lets MyPlay end
 * a call ([canEnd]); otherwise Back does what it always does.
 */
internal object CallKeys {
    fun backDeclines(call: CarCall?, canEnd: Boolean): Boolean = call?.ringing == true && canEnd
}
