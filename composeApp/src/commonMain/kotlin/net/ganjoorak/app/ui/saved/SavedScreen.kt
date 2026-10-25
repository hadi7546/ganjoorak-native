package net.ganjoorak.app.ui.saved

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.ui.components.AppPage
import net.ganjoorak.app.ui.components.AppRow
import net.ganjoorak.app.ui.components.MessageBox
import net.ganjoorak.app.util.toPersianDigits

@Composable
fun SavedScreen(onBack: () -> Unit, onOpenPoem: (PoemRef) -> Unit) {
    val library = AppGraph.instance.library
    val saved by library.savedPoems.collectAsState()

    AppPage(
        title = "نشان‌شده‌ها",
        subtitle = if (saved.isEmpty()) null else "${saved.size.toPersianDigits()} شعر",
        onBack = onBack,
    ) {
        if (saved.isEmpty()) {
            item {
                MessageBox(
                    Icons.Filled.BookmarkBorder,
                    "هنوز شعری نشان نکرده‌اید",
                    Modifier.height(320.dp),
                    body = "با زدن نشانک روی هر شعر، آن را این‌جا نگه دارید تا بدون جست‌وجو به آن برگردید",
                )
            }
        } else {
            items(saved, key = { it.ref.key }) { poem ->
                AppRow(
                    title = poem.title,
                    meta = listOf(poem.poetName, poem.excerpt).filter { it.isNotBlank() }.joinToString(" · "),
                    trailing = {
                        IconButton(onClick = { library.remove(poem.ref) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "حذف")
                        }
                    },
                    onClick = { onOpenPoem(poem.ref) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
