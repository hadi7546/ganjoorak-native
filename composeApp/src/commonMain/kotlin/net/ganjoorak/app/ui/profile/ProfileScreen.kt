package net.ganjoorak.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.interestByKey
import net.ganjoorak.app.ui.components.AppPage
import net.ganjoorak.app.ui.components.AppRow
import net.ganjoorak.app.ui.components.BorderedCard
import net.ganjoorak.app.ui.components.SectionLabel
import net.ganjoorak.app.ui.components.TopicChip
import net.ganjoorak.app.util.toPersianDigits

/**
 * The site's «حساب من» without the sign-in: counts that link to the library, recent saved poems,
 * the chosen topics, and settings. Everything stays on this device.
 */
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onOpenPoem: (PoemRef) -> Unit,
    onOpenSaved: () -> Unit,
    onOpenFollowing: () -> Unit,
    onEditInterests: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val graph = AppGraph.instance
    val saved by graph.library.savedPoems.collectAsState()
    val settings by graph.settings.settings.collectAsState()

    AppPage(
        title = "حساب من",
        subtitle = "نشان‌شده‌ها، شاعران دنبال‌شده و علاقه‌مندی‌ها روی همین دستگاه نگه داشته می‌شوند.",
        onBack = onBack,
        actions = {
            IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, contentDescription = "تنظیمات") }
        },
        headerExtra = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("نشان‌شده", saved.size, onOpenSaved, Modifier.weight(1f))
                Stat("دنبال‌شده", settings.followedPoets.size, onOpenFollowing, Modifier.weight(1f))
                Stat("علاقه‌مندی", settings.interestKeys.size, onEditInterests, Modifier.weight(1f))
            }
        },
    ) {
        item { SectionLabel("نشان‌شده‌های اخیر") }
        if (saved.isEmpty()) {
            item {
                Text(
                    "هنوز شعری نشان نکرده‌اید.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 16.dp),
                )
            }
        } else {
            items(saved.take(5), key = { it.ref.key }) { poem ->
                AppRow(title = poem.title, meta = poem.poetName, onClick = { onOpenPoem(poem.ref) }, modifier = Modifier.fillMaxWidth())
            }
            if (saved.size > 5) {
                item {
                    AppRow(
                        title = "همهٔ نشان‌شده‌ها",
                        trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null) },
                        onClick = onOpenSaved,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        item { SectionLabel("علاقه‌مندی‌ها") }
        item {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (settings.interestKeys.isEmpty()) {
                    Text(
                        "هنوز علاقه‌مندی‌ای انتخاب نکرده‌اید.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        settings.interestKeys.forEach { key ->
                            TopicChip(interestByKey(key)?.label ?: key, active = false, onClick = onEditInterests)
                        }
                    }
                }
            }
        }
        item {
            AppRow(
                title = "ویرایش علاقه‌مندی‌ها",
                leading = { Icon(Icons.Filled.Tune, contentDescription = null) },
                trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null) },
                onClick = onEditInterests,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            AppRow(
                title = "تنظیمات",
                meta = "پوسته، قلم و اندازهٔ متن شعر",
                leading = { Icon(Icons.Filled.Settings, contentDescription = null) },
                trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null) },
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    BorderedCard(modifier, onClick = onClick) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toPersianDigits(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
