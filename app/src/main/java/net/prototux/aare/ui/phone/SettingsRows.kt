package net.prototux.aare.ui.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * The plain parts the older settings pages are made of (sections, switches, choices), drawn in
 * the new design (plan E7). The pages keep their own logic; only the look of these parts changes.
 */

/** A section: a heading with a line, then its contents in a dark card. */
@Composable
fun PhoneSection(title: String, content: @Composable () -> Unit) {
    Column {
        SectionHeading(title, Modifier.padding(top = 0.dp))
        PartCard(padding = 16.dp) { Column { content() } }
    }
}

/** A switch with a title and a line under it; tapping anywhere on the row switches it. */
@Composable
fun PhoneToggle(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(role = Role.Switch) { onChange(!checked) }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = PhoneColors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (subtitle.isNotEmpty()) Text(subtitle, color = PhoneColors.Muted, fontSize = 13.sp)
        }
        Spacer(Modifier.width(12.dp))
        PhoneSwitch(checked, onChange)
    }
}

/** One of several values, as pills: the chosen one green. Labels are never cut, the pills wrap instead. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhoneChoices(title: String, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Text(title, color = PhoneColors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            val on = value == selected
            Text(
                label,
                Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (on) PhoneColors.Accent else PhoneColors.QuietButton)
                    .clickable(role = Role.RadioButton) { onSelect(value) }
                    .semantics { this.selected = on }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                color = if (on) PhoneColors.OnAccent else PhoneColors.QuietButtonText,
                fontSize = 14.sp,
                fontWeight = if (on) FontWeight.ExtraBold else FontWeight.SemiBold,
            )
        }
    }
    Spacer(Modifier.height(6.dp))
}
