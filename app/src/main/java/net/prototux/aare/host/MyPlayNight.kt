package net.prototux.aare.host

/** MyPlay (K11/F2): day or night for map apps. */
internal object MyPlayNight {
    /**
     * Night for a map app: the car's own signal when [followCar] is on and the car has reported
     * one ([carNight] not null); otherwise the same as MyPlay's own colours ([screenNight]).
     */
    fun forMaps(screenNight: Boolean, carNight: Boolean?, followCar: Boolean): Boolean =
        if (followCar && carNight != null) carNight else screenNight
}
