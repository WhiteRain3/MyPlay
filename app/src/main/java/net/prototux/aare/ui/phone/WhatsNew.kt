package net.prototux.aare.ui.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.prototux.aare.R

/** The release notes drawn in MyPlay's style: headings, bullets with their levels, paragraphs. */
@Composable
fun ReleaseNotesText(blocks: List<NoteBlock>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { b ->
            when (b) {
                is NoteBlock.Heading -> Text(b.text.uppercase(), color = PhoneColors.Muted, fontSize = 13.sp, letterSpacing = 1.sp,
                    fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 10.dp))
                is NoteBlock.Paragraph -> Text(ReleaseNotes.inline(b.text), color = PhoneColors.Text, fontSize = 15.sp)
                is NoteBlock.Bullet -> Row(Modifier.padding(start = (b.level * 18).dp)) {
                    Box(Modifier.padding(top = 8.dp).size(6.dp).clip(CircleShape).background(if (b.level == 0) PhoneColors.Accent else PhoneColors.Muted))
                    Spacer(Modifier.width(10.dp))
                    Text(ReleaseNotes.inline(b.text), color = PhoneColors.Text, fontSize = 15.sp)
                }
            }
        }
    }
}

/** "What's new in <version>": the notes in a dark card over the app, closed with its button or Back. */
@Composable
fun WhatsNewDialog(version: String, notes: String, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.padding(horizontal = 18.dp, vertical = 40.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp))
                .background(PhoneColors.Page).padding(20.dp),
        ) {
            Text(stringResource(R.string.phone_whats_new_title, version), color = PhoneColors.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.weight(1f, fill = false).heightIn(max = 520.dp).verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                ReleaseNotesText(ReleaseNotes.parse(notes))
            }
            Spacer(Modifier.size(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                PillButton(stringResource(R.string.phone_whats_new_close), onClick = onClose)
            }
        }
    }
}
