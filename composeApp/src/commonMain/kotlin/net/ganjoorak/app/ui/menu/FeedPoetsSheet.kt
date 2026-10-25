package net.ganjoorak.app.ui.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.GanjoorException
import net.ganjoorak.app.data.Poet
import net.ganjoorak.app.data.PoetGroup
import net.ganjoorak.app.data.local.FollowedPoet
import net.ganjoorak.app.ui.components.ErrorBox
import net.ganjoorak.app.ui.components.Loadable
import net.ganjoorak.app.ui.components.LoadingBox
import net.ganjoorak.app.ui.components.PoetAvatar
import net.ganjoorak.app.ui.components.SectionTitle
import net.ganjoorak.app.util.normalizeForSearch
import net.ganjoorak.app.util.toPersianDigits

@Composable
fun FeedPoetsSheet(onDismiss: () -> Unit) {
    val store = AppGraph.instance.settings
    val settings by store.settings.collectAsState()
    var selection by remember { mutableStateOf(settings.followedPoets) }
    var query by rememberSaveable { mutableStateOf("") }
    var attempt by remember { mutableIntStateOf(0) }
    // Shown from the app shell, outside any navigation entry, so it loads without a ViewModel.
    val state by produceState<Loadable<List<PoetGroup>>>(Loadable.Loading, attempt) {
        value = Loadable.Loading
        val repository = AppGraph.instance.repository
        value = try {
            Loadable.Ready(repository.contemporaryGroups() + repository.poetGroups())
        } catch (e: GanjoorException) {
            Loadable.Error(e.message ?: "خطا")
        }
    }

    val save = {
        store.update { it.copy(followedPoets = selection) }
        onDismiss()
    }

    ModalBottomSheet(onDismissRequest = save, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("شاعران دنبال‌شده", style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (selection.isEmpty()) {
                            "حالت «دنبال‌شده‌ها» در صفحهٔ اصلی فقط از این شاعران شعر می‌آورد"
                        } else {
                            "${selection.size.toPersianDigits()} شاعر انتخاب شده"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (selection.isNotEmpty()) TextButton(onClick = { selection = emptyList() }) { Text("پاک کردن") }
                TextButton(onClick = save) { Text("تأیید") }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("نام شاعر") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
        }
        when (val s = state) {
            is Loadable.Loading -> LoadingBox(Modifier.padding(32.dp))
            is Loadable.Error -> ErrorBox(s.message, onRetry = { attempt++ })
            is Loadable.Ready -> {
                val needle = query.normalizeForSearch()
                val selectedKeys = selection.map { it.key }.toSet()
                LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                    s.value.forEach { group ->
                        val poets = group.poets.filter { needle.isEmpty() || needle in it.name.normalizeForSearch() }
                        if (poets.isEmpty()) return@forEach
                        item(key = "g-${group.title}") { SectionTitle(group.title) }
                        items(poets, key = { "${group.title}-${it.key}" }) { poet ->
                            PoetCheckRow(poet, checked = poet.key in selectedKeys) { checked ->
                                selection = if (checked) selection + FollowedPoet(poet.key, poet.name) else selection.filterNot { it.key == poet.key }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PoetCheckRow(poet: Poet, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(poet.name) },
        leadingContent = { PoetAvatar(poet.name, poet.imageUrl, 40.dp) },
        trailingContent = { Checkbox(checked = checked, onCheckedChange = onChange) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onChange(!checked) },
    )
}
