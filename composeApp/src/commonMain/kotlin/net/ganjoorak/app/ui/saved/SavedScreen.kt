package net.ganjoorak.app.ui.saved

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.SavedPoem
import net.ganjoorak.app.ui.components.AppPage
import net.ganjoorak.app.ui.components.AppRow
import net.ganjoorak.app.ui.components.AppTabs
import net.ganjoorak.app.ui.components.MessageBox

private enum class LibraryTab(val label: String) { Favorites("ذخیره‌شده‌ها"), History("اخیراً دیده‌شده") }

/** The site's «نشان‌شده‌ها»: saved poems and recently read ones, as two tabs. */
@Composable
fun SavedScreen(onBack: () -> Unit, onOpenPoem: (PoemRef) -> Unit, onOpenFeed: () -> Unit = onBack) {
    val library = AppGraph.instance.library
    val saved by library.savedPoems.collectAsState()
    val history by library.history.collectAsState()
    var tab by rememberSaveable { mutableStateOf(LibraryTab.Favorites) }

    AppPage(
        title = "نشان‌شده‌ها",
        subtitle = "شعرهایی که ذخیره کرده‌اید و آن‌هایی که اخیراً خوانده‌اید.",
        onBack = onBack,
        actions = {
            if (tab == LibraryTab.History && history.isNotEmpty()) {
                TextButton(onClick = library::clearHistory) { Text("پاک کردن") }
            }
        },
        headerExtra = {
            AppTabs(
                options = LibraryTab.entries,
                selected = tab,
                label = { it.label },
                onSelect = { tab = it },
                count = { if (it == LibraryTab.Favorites) saved.size else history.size },
            )
        },
    ) {
        val entries = if (tab == LibraryTab.Favorites) saved else history
        if (entries.isEmpty()) {
            item(key = "empty-$tab") {
                if (tab == LibraryTab.Favorites) {
                    MessageBox(
                        Icons.Filled.FavoriteBorder,
                        "هنوز شعری ذخیره نکرده‌اید",
                        Modifier.height(320.dp),
                        body = "روی آیکن قلب در صفحهٔ شعر بزنید تا اینجا بماند.",
                        action = { Button(onClick = onOpenFeed) { Text("رفتن به فید") } },
                    )
                } else {
                    MessageBox(Icons.Filled.History, "هنوز شعری نخوانده‌اید", Modifier.height(320.dp))
                }
            }
        } else {
            items(entries, key = { "$tab-${it.ref.key}" }) { poem ->
                PoemRow(
                    poem,
                    onOpen = { onOpenPoem(poem.ref) },
                    onRemove = if (tab == LibraryTab.Favorites) ({ library.remove(poem.ref) }) else null,
                )
            }
        }
    }
}

@Composable
private fun PoemRow(poem: SavedPoem, onOpen: () -> Unit, onRemove: (() -> Unit)?) {
    AppRow(
        title = poem.title,
        meta = listOf(poem.poetName, poem.excerpt).filter { it.isNotBlank() }.joinToString(" · "),
        trailing = onRemove?.let { remove ->
            @Composable { IconButton(onClick = remove) { Icon(Icons.Outlined.Delete, contentDescription = "حذف") } }
        },
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
    )
}
