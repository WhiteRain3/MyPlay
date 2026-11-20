package net.prototux.aare.ui.phone

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.ui.car.CarLayout
import net.prototux.aare.ui.car.DashCard
import net.prototux.aare.ui.car.changeLayout
import net.prototux.aare.ui.car.myplayLayoutSpec
import net.prototux.aare.ui.car.moved
import net.prototux.aare.ui.car.withCard

/** The rows of the Cards page: the shown cards in their order, then the hidden ones. */
fun cardRows(order: List<DashCard>): List<DashCard> = order + DashCard.entries.filter { it !in order }

/** Whether [card] can be switched: the last shown card cannot be hidden, an empty home screen is no use. */
fun canSwitchCard(order: List<DashCard>, card: DashCard): Boolean = !(card in order && order.size == 1)

private val DashCard.symbol: ImageVector
    get() = when (this) {
        DashCard.MAP -> Icons.Rounded.Map
        DashCard.MEDIA -> Icons.Rounded.MusicNote
        DashCard.CALENDAR -> Icons.Rounded.CalendarMonth
        DashCard.NOTIFICATION -> Icons.Rounded.Notifications
        DashCard.WEATHER -> Icons.Rounded.WbSunny
    }

private val DashCard.tone: TileTone
    get() = when (this) {
        DashCard.MAP -> TileTone.GREEN
        DashCard.MEDIA -> TileTone.PINK
        DashCard.CALENDAR -> TileTone.BLUE
        DashCard.NOTIFICATION -> TileTone.VIOLET
        DashCard.WEATHER -> TileTone.AMBER
    }

private val DashCard.title: Int
    get() = when (this) {
        DashCard.MAP -> R.string.dash_card_map
        DashCard.MEDIA -> R.string.dash_card_media
        DashCard.CALENDAR -> R.string.dash_card_calendar
        DashCard.NOTIFICATION -> R.string.dash_card_notification
        DashCard.WEATHER -> R.string.dash_card_weather
    }

/**
 * The cards on the car's home screen (plan E5): with "map behind" that layout's own cards and
 * their place; otherwise the taskbar's dashboard cards (the dock and the app grid have none), with
 * a note saying which layout is in use.
 */
@Composable
fun CardsPage(openLayout: () -> Unit) {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    val version = rememberSettingsChanges()
    val layout = remember(version) { CarLayout.byId(settings.carLayoutId) }
    val spec = remember(version) { settings.myplayLayoutSpec }
    var order by remember { mutableStateOf(DashCard.parse(settings.dashboardCards)) }
    fun change(new: List<DashCard>) { order = new; settings.dashboardCards = new.map(DashCard::id) }
    Column {
        PartPicture()
        LayoutNote(layout, stringResource(when (layout) {
            CarLayout.TASKBAR -> R.string.phone_cards_note_taskbar
            CarLayout.MAP_BEHIND -> R.string.phone_cards_note_map_behind
            else -> R.string.phone_cards_note_none
        }), openLayout)
        // The taskbar's weather card still makes the car fetch the weather (a rough position to
        // Open-Meteo) whatever layout is in use, so say so here and offer to switch it off.
        if (layout != CarLayout.TASKBAR && DashCard.WEATHER in order) {
            Spacer(Modifier.height(12.dp))
            PartCard(padding = 14.dp) {
                Column {
                    Text(stringResource(R.string.phone_cards_weather_still_on), color = PhoneColors.Warning, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.phone_cards_weather_note), color = PhoneColors.Muted, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    PillButton(stringResource(R.string.phone_cards_weather_off), main = false) {
                        if (canSwitchCard(order, DashCard.WEATHER)) change(order.withCard(DashCard.WEATHER, false))
                        else change(DashCard.DEFAULT)
                    }
                }
            }
        }
        if (layout == CarLayout.MAP_BEHIND) {
            SectionHeading(stringResource(R.string.phone_cards_order))
            MapBehindCardRows(spec) { f -> settings.changeLayout(f) }
            return@Column
        }
        SectionHeading(stringResource(R.string.phone_cards_order))
        RowGroup(rows = cardRows(order).map { card ->
            {
                val i = order.indexOf(card)
                val shown = i >= 0
                val note = if (card == DashCard.WEATHER) stringResource(R.string.phone_cards_weather_note) else null
                SimpleRow(card.symbol, stringResource(card.title), card.tone, note) {
                    if (shown) {
                        RoundIconButton(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.common_up), enabled = i > 0, size = 36.dp) { change(order.moved(i, -1)) }
                        RoundIconButton(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.common_down), enabled = i < order.lastIndex, size = 36.dp) { change(order.moved(i, 1)) }
                    }
                    PhoneSwitch(shown, if (canSwitchCard(order, card)) { on -> change(order.withCard(card, on)) } else null)
                }
            }
        })
        HintLine(Icons.Rounded.Map, stringResource(R.string.dash_cards_hint))
    }
}
