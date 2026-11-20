package net.prototux.aare.ui.phone

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.prototux.aare.R

/**
 * The start screen (plan E2): what needs doing first ([top]: banners, the connection and "Finish
 * setting up"), then the picture of the car screen to tap, then everything that cannot be seen
 * on the car screen ([behind]: the tiles).
 */
@Composable
fun CustomiseScreen(
    onPicture: () -> Unit,
    behind: @Composable () -> Unit,
    top: LazyListScope.() -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize().systemBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 24.dp),
    ) {
        item { PhoneTopBar(stringResource(R.string.phone_customise_title)) }
        item { Spacer(Modifier.height(8.dp)) }
        top()
        item {
            CarPicture(stringResource(R.string.phone_customise_picture_description), Modifier.padding(top = 8.dp), onTap = onPicture)
        }
        item { HintLine(Icons.Rounded.TouchApp, stringResource(R.string.phone_customise_hint)) }
        item { SectionHeading(stringResource(R.string.phone_customise_behind)) }
        item { behind() }
    }
}
