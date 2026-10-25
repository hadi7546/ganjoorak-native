package net.ganjoorak.app.ui.poets

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.Poet
import net.ganjoorak.app.data.PoetGroup
import net.ganjoorak.app.ui.components.AppPage
import net.ganjoorak.app.ui.components.AppRow
import net.ganjoorak.app.ui.components.ErrorBox
import net.ganjoorak.app.ui.components.Loadable
import net.ganjoorak.app.ui.components.LoadingBox
import net.ganjoorak.app.ui.components.MessageBox
import net.ganjoorak.app.ui.components.PoetAvatar
import net.ganjoorak.app.ui.components.SectionLabel
import net.ganjoorak.app.ui.components.TopicChip
import net.ganjoorak.app.ui.components.rememberLoader
import net.ganjoorak.app.util.hijriLifespan
import net.ganjoorak.app.util.normalizeForSearch
import net.ganjoorak.app.util.toPersianDigits

private enum class PoetFilter(val label: String) { All("همه"), Classic("کهن (گنجور)"), Contemporary("معاصر و جهان") }

@Composable
fun PoetsScreen(onBack: () -> Unit, onOpenPoet: (String) -> Unit, onOpenFollowing: () -> Unit) {
    val repository = AppGraph.instance.repository
    val classic = rememberLoader("poet-groups") { repository.poetGroups() }
    val contemporary = rememberLoader("contemporary-groups") { repository.contemporaryGroups() }
    val classicState by classic.state.collectAsState()
    val contemporaryState by contemporary.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(PoetFilter.All) }

    val sources = buildList {
        if (filter != PoetFilter.Classic) add(contemporaryState to contemporary::reload)
        if (filter != PoetFilter.Contemporary) add(classicState to classic::reload)
    }
    val firstError = sources.firstOrNull { it.first is Loadable.Error }
    val needle = query.normalizeForSearch()
    val groups = sources.flatMap { (state, _) -> (state as? Loadable.Ready)?.value.orEmpty() }
    val filtered = remember(groups, needle) { filterGroups(groups, needle) }

    AppPage(
        title = "شاعران",
        subtitle = "شاعران کهن گنجور، شاعران معاصر و شعر جهان",
        onBack = onBack,
        actions = {
            IconButton(onClick = onOpenFollowing) { Icon(Icons.Filled.HowToReg, contentDescription = "شاعران دنبال‌شده") }
        },
        headerExtra = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("جست‌وجوی شاعر") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, contentDescription = "پاک کردن") }
                        }
                    },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PoetFilter.entries.forEach {
                        TopicChip(it.label, active = filter == it, onClick = { filter = it })
                    }
                }
            }
        },
    ) {
        when {
            sources.all { it.first is Loadable.Loading } -> item { LoadingBox(Modifier.height(240.dp)) }
            sources.none { it.first is Loadable.Ready } && firstError != null -> item {
                ErrorBox((firstError.first as Loadable.Error).message, onRetry = firstError.second, modifier = Modifier.height(320.dp))
            }
            filtered.isEmpty() -> item { MessageBox(Icons.Filled.SearchOff, "شاعری با این نام پیدا نشد", Modifier.height(240.dp)) }
            else -> filtered.forEach { group ->
                item(key = "h-${group.title}") { SectionLabel("${group.title} · ${group.poets.size.toPersianDigits()} شاعر") }
                items(group.poets, key = { "${group.title}-${it.key}" }) { poet ->
                    PoetRow(poet, onClick = { onOpenPoet(poet.key) })
                }
            }
        }
    }
}

private fun filterGroups(groups: List<PoetGroup>, needle: String): List<PoetGroup> {
    if (needle.isEmpty()) return groups
    return groups.mapNotNull { group ->
        val poets = group.poets.filter {
            needle in it.name.normalizeForSearch() || needle in it.nickname.orEmpty().normalizeForSearch()
        }
        if (poets.isEmpty()) null else group.copy(poets = poets)
    }
}

@Composable
fun PoetRow(poet: Poet, onClick: () -> Unit) {
    val followed by AppGraph.instance.settings.settings.collectAsState()
    AppRow(
        title = poet.name,
        meta = hijriLifespan(poet.birthYear, poet.deathYear) ?: poet.group?.takeIf { poet.birthYear == null },
        leading = { PoetAvatar(poet.name, poet.imageUrl, 44.dp) },
        trailing = {
            if (followed.followedPoets.any { it.key == poet.key }) {
                Icon(Icons.Filled.HowToReg, contentDescription = "دنبال می‌کنید", modifier = Modifier.size(18.dp))
            }
        },
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
internal fun SupportingText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
