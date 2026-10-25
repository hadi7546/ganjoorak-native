package net.ganjoorak.app.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.DefaultSuggestedInterestKeys
import net.ganjoorak.app.data.PoemInterests
import net.ganjoorak.app.ui.components.TopicChip

/**
 * The site's interest picker: topics as chips; the chosen ones become the chips above the feed.
 * The first time it pre-selects a suggested set.
 */
@Composable
fun InterestPickerSheet(onDismiss: () -> Unit) {
    val store = AppGraph.instance.settings
    val settings by store.settings.collectAsState()
    var selection by remember {
        mutableStateOf(if (settings.hasChosenInterests) settings.interestKeys.toSet() else DefaultSuggestedInterestKeys.toSet())
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("چه شعرهایی دوست دارید؟", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "موضوع‌هایی که انتخاب کنید بالای صفحهٔ اصلی می‌آیند و با زدن هرکدام، فید فقط از همان موضوع می‌شود.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PoemInterests.forEach { interest ->
                    val on = interest.key in selection
                    TopicChip(interest.label, active = on, onClick = {
                        selection = if (on) selection - interest.key else selection + interest.key
                    })
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        store.setInterests(selection.toList())
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("ذخیره") }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("بعداً") }
            }
        }
    }
}
