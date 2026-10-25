package net.ganjoorak.app.ui.poets

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.Poet
import net.ganjoorak.app.data.PoetGroup
import net.ganjoorak.app.ui.components.AppPage
import net.ganjoorak.app.ui.components.AppRow
import net.ganjoorak.app.ui.components.AppTabs
import net.ganjoorak.app.ui.components.BorderedCard
import net.ganjoorak.app.ui.components.ErrorBox
import net.ganjoorak.app.ui.components.Loadable
import net.ganjoorak.app.ui.components.LoadingBox
import net.ganjoorak.app.ui.components.MessageBox
import net.ganjoorak.app.ui.components.PoetAvatar
import net.ganjoorak.app.ui.components.SegmentedSwitch
import net.ganjoorak.app.ui.components.TopicChip
import net.ganjoorak.app.ui.components.rememberLoader
import net.ganjoorak.app.util.hijriLifespan
import net.ganjoorak.app.util.normalizeForSearch
import net.ganjoorak.app.util.toPersianDigits

private enum class PoetsTab(val label: String) { Featured("محبوب"), Classic("کهن"), Modern("نو و جهان"), Following("دنبال‌شده") }

private enum class PoetsView { Grid, List }

/** Shown in «محبوب» when Ganjoor doesn't say which poets to pin, as on the site. */
private val FeaturedSlugs = listOf("hafez", "saadi", "moulavi", "ferdousi", "khayyam", "attar", "nezami", "sanaee")

private class PoetEntry(val poet: Poet, val group: String)

/**
 * The site's poets page: search above everything, then «محبوب / کهن / نو و جهان / دنبال‌شده» tabs,
 * group chips for the classic and modern tabs, and a grid or list where every poet can be followed.
 */
@Composable
fun PoetsScreen(onBack: () -> Unit, onOpenPoet: (String) -> Unit) {
    val repository = AppGraph.instance.repository
    val settings by AppGraph.instance.settings.settings.collectAsState()
    val classicLoader = rememberLoader("poet-groups") { repository.poetGroups() }
    val modernLoader = rememberLoader("contemporary-groups") { repository.contemporaryGroups() }
    val classicState by classicLoader.state.collectAsState()
    val modernState by modernLoader.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableStateOf(PoetsTab.Featured) }
    var view by rememberSaveable { mutableStateOf(PoetsView.Grid) }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }

    val classicGroups = (classicState as? Loadable.Ready)?.value.orEmpty()
    val modernGroups = (modernState as? Loadable.Ready)?.value.orEmpty()
    val classic = remember(classicGroups) { classicGroups.entries() }
    val modern = remember(modernGroups) { modernGroups.entries().sortedBy { it.poet.name } }
    val all = remember(classic, modern) { classic + modern }
    val featured = remember(classic) {
        FeaturedSlugs.mapNotNull { slug -> classic.firstOrNull { it.poet.slug == slug } }
    }
    val followedKeys = settings.followedPoets.map { it.key }.toSet()
    val followed = all.filter { it.poet.key in followedKeys }

    val tabEntries = when (tab) {
        PoetsTab.Featured -> featured
        PoetsTab.Classic -> classic
        PoetsTab.Modern -> modern
        PoetsTab.Following -> followed
    }
    val groupFilters = when (tab) {
        PoetsTab.Classic -> classicGroups.map { it.title }
        PoetsTab.Modern -> modernGroups.map { it.title }.distinct()
        else -> emptyList()
    }
    val activeFilter = filter?.takeIf { it in groupFilters }
    val needle = query.normalizeForSearch()
    val visible = when {
        needle.isNotEmpty() -> all.filter {
            needle in it.poet.name.normalizeForSearch() || needle in it.poet.nickname.orEmpty().normalizeForSearch()
        }
        activeFilter != null -> tabEntries.filter { it.group == activeFilter }
        else -> tabEntries
    }

    AppPage(
        title = "شاعران",
        subtitle = "${all.size.toPersianDigits()} شاعر از گنجور و اکولالیا. دنبالشان کنید تا در فید «دنبال‌شده‌ها» بیایند.",
        onBack = onBack,
        actions = {
            IconButton(onClick = { view = if (view == PoetsView.Grid) PoetsView.List else PoetsView.Grid }) {
                Icon(
                    if (view == PoetsView.Grid) Icons.AutoMirrored.Filled.List else Icons.Filled.GridView,
                    contentDescription = if (view == PoetsView.Grid) "نمایش فهرستی" else "نمایش شبکه‌ای",
                )
            }
        },
        headerExtra = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("جست‌وجوی نام شاعر") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, contentDescription = "پاک کردن") }
                        }
                    },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (needle.isEmpty()) {
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        AppTabs(
                            options = PoetsTab.entries,
                            selected = tab,
                            label = { it.label },
                            onSelect = {
                                tab = it
                                filter = null
                            },
                            count = { option ->
                                when (option) {
                                    PoetsTab.Featured -> featured.size
                                    PoetsTab.Classic -> classic.size
                                    PoetsTab.Modern -> modern.size
                                    PoetsTab.Following -> followed.size
                                }
                            },
                        )
                    }
                    if (groupFilters.size > 1) {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TopicChip("همه", active = activeFilter == null, onClick = { filter = null })
                            groupFilters.forEach { group ->
                                TopicChip(group, active = activeFilter == group, onClick = { filter = group })
                            }
                        }
                    }
                }
            }
        },
    ) {
        val anyLoading = classicState is Loadable.Loading || modernState is Loadable.Loading
        val error = (classicState as? Loadable.Error) ?: (modernState as? Loadable.Error)
        when {
            visible.isEmpty() && anyLoading -> item { LoadingBox(Modifier.height(240.dp)) }
            visible.isEmpty() && error != null -> item {
                ErrorBox(error.message, onRetry = { classicLoader.reload(); modernLoader.reload() }, modifier = Modifier.height(320.dp))
            }
            visible.isEmpty() -> item {
                MessageBox(
                    Icons.Filled.SearchOff,
                    if (tab == PoetsTab.Following && needle.isEmpty()) "هنوز شاعری را دنبال نمی‌کنید" else "شاعری پیدا نشد",
                    Modifier.height(240.dp),
                )
            }
            view == PoetsView.List || needle.isNotEmpty() -> items(visible, key = { "l-${it.poet.key}" }) { entry ->
                AppRow(
                    title = entry.poet.name,
                    meta = listOfNotNull(hijriLifespan(entry.poet.birthYear, entry.poet.deathYear), entry.group).joinToString(" · "),
                    leading = { PoetAvatar(entry.poet.name, entry.poet.imageUrl, 48.dp) },
                    trailing = { FollowToggle(entry.poet, entry.poet.key in followedKeys) },
                    onClick = { onOpenPoet(entry.poet.key) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            else -> items(visible.chunked(2), key = { row -> "g-${row.first().poet.key}" }) { row ->
                Row(
                    Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { entry ->
                        PoetTile(entry, entry.poet.key in followedKeys, { onOpenPoet(entry.poet.key) }, Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private fun List<PoetGroup>.entries(): List<PoetEntry> {
    val seen = HashSet<String>()
    return flatMap { group -> group.poets.map { PoetEntry(it, group.title) } }.filter { seen.add(it.poet.key) }
}

@Composable
private fun PoetTile(entry: PoetEntry, followed: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    BorderedCard(modifier, onClick = onClick) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PoetAvatar(entry.poet.name, entry.poet.imageUrl, 72.dp)
            Text(
                entry.poet.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                entry.group,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FollowToggle(entry.poet, followed)
        }
    }
}

/** «دنبال کردن» / «دنبال‌شده», outline until followed. */
@Composable
fun FollowToggle(poet: Poet, followed: Boolean) {
    val store = AppGraph.instance.settings
    val padding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
    if (followed) {
        Button(
            onClick = { store.unfollow(poet.key) },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
            contentPadding = padding,
            modifier = Modifier.height(32.dp),
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.size(4.dp))
            Text("دنبال‌شده", style = MaterialTheme.typography.labelMedium)
        }
    } else {
        OutlinedButton(onClick = { store.follow(poet.key, poet.name) }, contentPadding = padding, modifier = Modifier.height(32.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.size(4.dp))
            Text("دنبال کردن", style = MaterialTheme.typography.labelMedium)
        }
    }
}
