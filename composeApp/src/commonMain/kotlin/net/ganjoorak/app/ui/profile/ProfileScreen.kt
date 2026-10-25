package net.ganjoorak.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.SavedPoem
import net.ganjoorak.app.ui.components.AppPage
import net.ganjoorak.app.ui.components.AppRow
import net.ganjoorak.app.ui.components.BorderedCard
import net.ganjoorak.app.util.toPersianDigits

private const val FAVORITES_SHOWN = 5

/**
 * The site's «حساب من» (its ProfileView) for this device: a hero card, the last poem read and
 * the liked poems. There is no sign-in in the app, so everything here lives on the device.
 */
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onOpenPoem: (PoemRef) -> Unit,
    onOpenSaved: () -> Unit,
    onOpenFollowing: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val graph = AppGraph.instance
    val saved by graph.library.savedPoems.collectAsState()
    val history by graph.library.history.collectAsState()
    val settings by graph.settings.settings.collectAsState()

    AppPage(
        title = "حساب من",
        onBack = onBack,
        actions = {
            IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, contentDescription = "تنظیمات") }
        },
        headerExtra = {
            BorderedCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(28.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("خوانندهٔ گنجورک", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "${saved.size.toPersianDigits()} پسندیده · ${settings.followedPoets.size.toPersianDigits()} شاعر دنبال‌شده",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
    ) {
        section("آخرین شعر")
        val last = history.firstOrNull()
        if (last == null) {
            empty("هنوز شعری نخوانده یا نشنیده‌اید.")
        } else {
            item(key = "last") { PoemRow(last) { onOpenPoem(last.ref) } }
        }

        section(
            if (saved.isEmpty()) "پسندیده‌ها" else "پسندیده‌ها (${saved.size.toPersianDigits()})",
            action = if (saved.size > FAVORITES_SHOWN) ("همه" to onOpenSaved) else null,
        )
        if (saved.isEmpty()) {
            empty("هنوز شعری نپسندیده‌اید. روی آیکن قلب در صفحهٔ شعر بزنید.")
        } else {
            items(saved.take(FAVORITES_SHOWN), key = { "fav-${it.ref.key}" }) { poem -> PoemRow(poem) { onOpenPoem(poem.ref) } }
        }

        section("شاعران")
        item(key = "following") {
            AppRow(
                title = "شاعران دنبال‌شده",
                meta = settings.followedPoets.takeIf { it.isNotEmpty() }?.joinToString("، ") { it.name } ?: "هنوز شاعری را دنبال نمی‌کنید",
                leading = { Icon(Icons.Filled.HowToReg, contentDescription = null) },
                trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null) },
                onClick = onOpenFollowing,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun LazyListScope.section(title: String, action: Pair<String, () -> Unit>? = null) {
    item(key = "section-$title") {
        Row(
            Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (action != null) {
                TextButton(onClick = action.second) {
                    Text(action.first)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

private fun LazyListScope.empty(message: String) {
    item(key = "empty-$message") {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun PoemRow(poem: SavedPoem, onClick: () -> Unit) {
    AppRow(title = poem.title, meta = poem.poetName, onClick = onClick, modifier = Modifier.fillMaxWidth())
}
