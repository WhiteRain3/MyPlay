package net.prototux.aare.ui.car

/**
 * MyPlay (M1): a car screen layout as data, the eight options of the Design reviewer's sheet
 * (docs/plans/myplay.md, phase M). One screen file will draw any allowed combination (M3); the
 * presets A to F are named combinations. Nothing here draws anything.
 *
 * Choosing a preset sets all eight options; each one can then be changed on its own. [allowed]
 * turns any combination into one that keeps the sheet's rules.
 */
data class CarLayoutSpec(
    /** 1. The maps app's picture fills the screen behind everything (off: plain dark). */
    val mapBackground: Boolean,
    /** 2. The map goes on under the bar (off: it stops at the bar's edge). */
    val mapUnderBar: Boolean,
    /** 3. How the bar looks. */
    val barStyle: BarStyle,
    /** 4. The screen edge the bar is on. */
    val barSide: BarSide,
    /** 5. Where the cards sit. */
    val cardPlace: CardPlace,
    /** 6. How see-through the cards are, 0 to 100 %, always over a dark base. */
    val cardSeeThrough: Int,
    /** 7. Which cards, in this order. */
    val cards: List<SpecCard>,
    /** 7. For [CardPlace.STAGE]: one card at a time besides the next turn. */
    val oneExtraAtATime: Boolean,
    /** 8. Hide the music card this many seconds after it last changed; null: never. */
    val hideMusicAfterS: Int?,
    /** 8. Hide the arrival time card this many seconds after it last changed; null: never. */
    val hideArrivalAfterS: Int?,
) {
    enum class BarStyle(val key: String) {
        SOLID_RAIL("solid_rail"), SEE_THROUGH_RAIL("see_through_rail"), FLOATING_PILLS("pills"),
        BOTTOM_DOCK("bottom_dock"), TOP_RIBBON("top_ribbon"),
    }

    enum class BarSide(val key: String) { LEFT("left"), RIGHT("right"), BOTTOM("bottom"), TOP("top") }

    enum class CardPlace(val key: String) {
        COLUMN_BY_BAR("column"), CORNERS("corners"), STAGE("stage"), TOP_BANNER_AND_CORNER("banner_corner"),
    }

    enum class SpecCard(val key: String) { NEXT_TURN("next_turn"), ARRIVAL("arrival"), MUSIC("music"), CLOCK_STATUS("clock_status") }

    /**
     * The same layout with the sheet's rules kept, changing as little as possible:
     * - a bottom dock only at the bottom, a top ribbon only at the top (the side follows the style);
     * - "map under the bar" only with the map as background;
     * - "one extra at a time" only with the stage card;
     * - see-through 0 to 100 %; hiding after 5 to 60 s, or never;
     * - each card at most once; at least the next turn when the list is empty.
     */
    fun allowed(): CarLayoutSpec = copy(
        barSide = when (barStyle) {
            BarStyle.BOTTOM_DOCK -> BarSide.BOTTOM
            BarStyle.TOP_RIBBON -> BarSide.TOP
            else -> barSide
        },
        mapUnderBar = mapUnderBar && mapBackground,
        oneExtraAtATime = oneExtraAtATime && cardPlace == CardPlace.STAGE,
        cardSeeThrough = cardSeeThrough.coerceIn(0, 100),
        cards = cards.distinct().ifEmpty { listOf(SpecCard.NEXT_TURN) },
        hideMusicAfterS = hideMusicAfterS?.coerceIn(HIDE_MIN_S, HIDE_MAX_S),
        hideArrivalAfterS = hideArrivalAfterS?.coerceIn(HIDE_MIN_S, HIDE_MAX_S),
    )

    companion object {
        const val HIDE_MIN_S = 5
        const val HIDE_MAX_S = 60
        private val ALL_FOUR = listOf(SpecCard.NEXT_TURN, SpecCard.ARRIVAL, SpecCard.MUSIC, SpecCard.CLOCK_STATUS)

        /**
         * The six presets of the sheet. The 14 % and 40 % amounts and the 15 s hide in E are the
         * Design reviewer's proposals, not the owner's decisions.
         */
        val PRESETS: Map<String, CarLayoutSpec> = linkedMapOf(
            "A" to CarLayoutSpec(true, false, BarStyle.SOLID_RAIL, BarSide.LEFT, CardPlace.COLUMN_BY_BAR, 14, ALL_FOUR, false, null, null),
            "B" to CarLayoutSpec(true, false, BarStyle.BOTTOM_DOCK, BarSide.BOTTOM, CardPlace.CORNERS, 14, ALL_FOUR, false, null, null),
            "C" to CarLayoutSpec(true, false, BarStyle.TOP_RIBBON, BarSide.TOP, CardPlace.STAGE, 14, ALL_FOUR, true, null, null),
            "D" to CarLayoutSpec(true, true, BarStyle.SEE_THROUGH_RAIL, BarSide.LEFT, CardPlace.CORNERS, 40, ALL_FOUR, false, null, null),
            "E" to CarLayoutSpec(true, true, BarStyle.FLOATING_PILLS, BarSide.BOTTOM, CardPlace.CORNERS, 40, ALL_FOUR, false, 15, null),
            "F" to CarLayoutSpec(true, true, BarStyle.FLOATING_PILLS, BarSide.RIGHT, CardPlace.TOP_BANNER_AND_CORNER, 40, ALL_FOUR, false, null, null),
        )

        /**
         * Until the owner picks one: D, the see-through rail (the dispatcher's instruction,
         * 2026-11-20). Not a decision.
         */
        const val DEFAULT_PRESET = "D"
        val DEFAULT: CarLayoutSpec get() = PRESETS.getValue(DEFAULT_PRESET)

        /** One line of text for the settings, every option named, so a later version can add options. */
        fun encode(spec: CarLayoutSpec): String = listOf(
            "map" to spec.mapBackground.flag(),
            "under" to spec.mapUnderBar.flag(),
            "bar" to spec.barStyle.key,
            "side" to spec.barSide.key,
            "place" to spec.cardPlace.key,
            "see" to spec.cardSeeThrough.toString(),
            "cards" to spec.cards.joinToString(",") { it.key },
            "one" to spec.oneExtraAtATime.flag(),
            "hide_music" to (spec.hideMusicAfterS?.toString() ?: ""),
            "hide_arrival" to (spec.hideArrivalAfterS?.toString() ?: ""),
        ).joinToString(";") { (k, v) -> "$k=$v" }

        /**
         * The layout written by [encode]. An option that is missing or not understood takes
         * [fallback]'s value; the result keeps the rules ([allowed]).
         */
        fun decode(text: String?, fallback: CarLayoutSpec = DEFAULT): CarLayoutSpec {
            if (text.isNullOrBlank()) return fallback
            val m = text.split(';').mapNotNull { part ->
                val i = part.indexOf('=')
                if (i <= 0) null else part.substring(0, i).trim() to part.substring(i + 1).trim()
            }.toMap()
            fun bool(k: String, d: Boolean) = when (m[k]) { "1" -> true; "0" -> false; else -> d }
            fun int(k: String, d: Int) = m[k]?.toIntOrNull() ?: d
            fun seconds(k: String, d: Int?): Int? = when (val v = m[k]) { null -> d; "" -> null; else -> v.toIntOrNull() ?: d }
            return CarLayoutSpec(
                mapBackground = bool("map", fallback.mapBackground),
                mapUnderBar = bool("under", fallback.mapUnderBar),
                barStyle = BarStyle.entries.firstOrNull { it.key == m["bar"] } ?: fallback.barStyle,
                barSide = BarSide.entries.firstOrNull { it.key == m["side"] } ?: fallback.barSide,
                cardPlace = CardPlace.entries.firstOrNull { it.key == m["place"] } ?: fallback.cardPlace,
                cardSeeThrough = int("see", fallback.cardSeeThrough),
                cards = m["cards"]?.let { list -> list.split(',').mapNotNull { k -> SpecCard.entries.firstOrNull { it.key == k.trim() } } } ?: fallback.cards,
                oneExtraAtATime = bool("one", fallback.oneExtraAtATime),
                hideMusicAfterS = seconds("hide_music", fallback.hideMusicAfterS),
                hideArrivalAfterS = seconds("hide_arrival", fallback.hideArrivalAfterS),
            ).allowed()
        }

        private fun Boolean.flag() = if (this) "1" else "0"
    }
}
