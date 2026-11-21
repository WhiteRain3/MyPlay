package net.prototux.aare.ui.phone

import net.prototux.aare.ui.car.CarLayoutSpec
import net.prototux.aare.ui.car.CarPart
import net.prototux.aare.ui.car.DashCard
import org.junit.Assert.assertEquals
import org.junit.Test

class PicturePartsTest {
    /** The parts meant to open the whole car screen settings (no page of their own). */
    private val toWhole = setOf<CarPart>(CarPart.OrbitTurn)

    /** One of each CarPart the car side defines (all are nested in CarPart), so a new one is never forgotten. */
    private fun everyPart(): List<CarPart> = CarPart::class.java.declaredClasses
        .filter { CarPart::class.java.isAssignableFrom(it) }
        .map { c ->
            when (c) {
                CarPart.DashboardCard::class.java -> CarPart.DashboardCard(DashCard.entries.first())
                CarPart.MapBehindCard::class.java -> CarPart.MapBehindCard(CarLayoutSpec.SpecCard.entries.first())
                else -> runCatching { c.getField("INSTANCE").get(null) as CarPart }.getOrNull()
                    ?: throw AssertionError("${c.simpleName}: a new part with data; add it to this test and to picturePartOf")
            }
        }

    @Test
    fun everyPartTheCarDrawsOpensAPage() {
        val parts = everyPart()
        assertEquals(true, parts.size >= 11)
        parts.forEach { part ->
            val page = picturePartOf(part)
            if (part in toWhole) assertEquals(PicturePart.WHOLE, page)
            else assertEquals("$part opens no page of its own: map it in picturePartOf", true, page != PicturePart.WHOLE)
        }
    }

    @Test
    fun orbitsPartsOpenTheirPages() {
        assertEquals(PicturePart.HUB, picturePartOf(CarPart.OrbitHub))
        assertEquals(PicturePart.CARDS, picturePartOf(CarPart.OrbitSong))
        assertEquals(PicturePart.SPEED, picturePartOf(CarPart.OrbitSpeed))
        assertEquals(PicturePart.WHOLE, picturePartOf(null))
    }
}
