package net.ganjoorak.app.ui.poets

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.PoetKey
import net.ganjoorak.app.data.PoetSource
import net.ganjoorak.app.ui.components.AppPage
import net.ganjoorak.app.ui.components.AppRow
import net.ganjoorak.app.ui.components.MessageBox
import net.ganjoorak.app.ui.components.PoetAvatar
import net.ganjoorak.app.ui.components.SectionLabel
import net.ganjoorak.app.util.toPersianDigits

/** The site's «شاعران دنبال‌شده»: the followed poets, a way to read from them, and to add more. */
@Composable
fun FollowingScreen(
    onBack: () -> Unit,
    onOpenPoet: (String) -> Unit,
    onOpenPoets: () -> Unit,
    onEditFollowing: () -> Unit,
    onReadFollowing: () -> Unit,
) {
    val store = AppGraph.instance.settings
    val settings by store.settings.collectAsState()
    val poets = settings.followedPoets

    AppPage(
        title = "شاعران دنبال‌شده",
        subtitle = if (poets.isNotEmpty()) {
            "${poets.size.toPersianDigits()} شاعر. حالت «دنبال‌شده‌ها» در صفحهٔ اصلی فقط از همین شاعران شعر می‌آورد."
        } else {
            "شاعرانی که دنبال می‌کنید اینجا جمع می‌شوند و حالت «دنبال‌شده‌ها» در صفحهٔ اصلی از آن‌ها شعر می‌آورد."
        },
        onBack = onBack,
        actions = {
            if (poets.isNotEmpty()) {
                IconButton(onClick = onOpenPoets) { Icon(Icons.Filled.PersonAdd, contentDescription = "افزودن شاعر") }
            }
        },
        headerExtra = if (poets.isNotEmpty()) {
            @Composable {
                Button(onClick = onReadFollowing, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("خواندن از دنبال‌شده‌ها")
                }
            }
        } else {
            null
        },
    ) {
        if (poets.isEmpty()) {
            item {
                MessageBox(
                    Icons.Filled.Groups,
                    "هنوز شاعری را دنبال نمی‌کنید",
                    Modifier.height(320.dp),
                    body = "در فهرست شاعران یا صفحهٔ هر شاعر «دنبال کردن» را بزنید.",
                    action = {
                        Button(onClick = onOpenPoets) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("دیدن شاعران")
                        }
                    },
                )
            }
            return@AppPage
        }
        item { SectionLabel("شاعران") }
        items(poets, key = { it.key }) { poet ->
            val source = PoetKey.parse(poet.key)?.source
            AppRow(
                title = poet.name,
                meta = when (source) {
                    PoetSource.Echolalia -> "اکولالیا"
                    PoetSource.Local -> "گنجورک"
                    else -> "گنجور"
                },
                leading = { PoetAvatar(poet.name, null, 40.dp) },
                trailing = {
                    OutlinedButton(onClick = { store.unfollow(poet.key) }) { Text("لغو دنبال") }
                },
                onClick = { onOpenPoet(poet.key) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { SectionLabel("بیشتر") }
        item {
            AppRow(
                title = "پیدا کردن شاعر تازه",
                meta = "فهرست همهٔ شاعران با جست‌وجو",
                leading = { Icon(Icons.Filled.PersonAdd, contentDescription = null) },
                trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null) },
                onClick = onOpenPoets,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            AppRow(
                title = "ویرایش گروهی",
                meta = "انتخاب چند شاعر یکجا",
                leading = { Icon(Icons.Filled.Tune, contentDescription = null) },
                trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null) },
                onClick = onEditFollowing,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
