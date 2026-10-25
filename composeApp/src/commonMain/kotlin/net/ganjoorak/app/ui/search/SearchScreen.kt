package net.ganjoorak.app.ui.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.width
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import net.ganjoorak.app.data.Poet
import net.ganjoorak.app.data.PoetSource
import net.ganjoorak.app.ui.components.AppPageBar
import net.ganjoorak.app.ui.components.AppPageHeader
import net.ganjoorak.app.ui.components.PoetAvatar
import net.ganjoorak.app.ui.components.TopicChip
import net.ganjoorak.app.util.normalizeForSearch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import net.ganjoorak.app.ui.components.SegmentedSwitch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.SearchHit
import net.ganjoorak.app.ui.components.ErrorBox
import net.ganjoorak.app.ui.components.MessageBox
import net.ganjoorak.app.ui.theme.LocalPoemStyle
import net.ganjoorak.app.util.toPersianDigits

@Composable
fun SearchScreen(onBack: () -> Unit, onOpenPoem: (PoemRef) -> Unit, onOpenPoet: (String) -> Unit) {
    val graph = AppGraph.instance
    val vm = viewModel { SearchViewModel(graph.repository, graph.library) }
    val state by vm.state.collectAsState()
    val recent by graph.library.recentSearches.collectAsState()
    val focus = LocalFocusManager.current

    var source by rememberSaveable { mutableStateOf<PoetSource?>(null) }
    // Poets whose names match, shown above the poems as on the site.
    val poets by produceState(emptyList<Poet>()) {
        value = runCatching {
            graph.repository.poetGroups().flatMap { it.poets } + graph.repository.contemporaryGroups().flatMap { it.poets }
        }.getOrDefault(emptyList())
    }
    val needle = state.query.normalizeForSearch()
    val matchingPoets = if (needle.length < 2) emptyList() else poets.filter {
        (source == null || it.source == source) &&
            (needle in it.name.normalizeForSearch() || needle in it.nickname.orEmpty().normalizeForSearch())
    }.distinctBy { it.key }.take(12)

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            AppPageBar("جست‌وجو", scrolled = false, onBack = onBack)
            AppPageHeader("جست‌وجو", "شاعر، مصرع یا یک واژه؛ در گنجور، اکولالیا و شاعران محلی.")
            Column(Modifier.widthIn(max = 840.dp).padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = vm::onQueryChange,
                    singleLine = true,
                    placeholder = { Text(if (state.mode == SearchMode.Text) "نام شاعر، مصرع یا واژه…" else "موضوع یا حال‌وهوای شعر…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { vm.onQueryChange("") }) { Icon(Icons.Filled.Clear, contentDescription = "پاک کردن") }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        vm.saveRecent()
                        focus.clearFocus()
                    }),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp).horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SegmentedSwitch(
                        options = SearchMode.entries,
                        selected = state.mode,
                        label = { if (it == SearchMode.Text) "واژه" else "معنا" },
                        onSelect = vm::onModeChange,
                    )
                    VerticalDivider(Modifier.height(24.dp).padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    listOf<Pair<PoetSource?, String>>(
                        null to "همه",
                        PoetSource.Ganjoor to "گنجور",
                        PoetSource.Echolalia to "اکولالیا",
                        PoetSource.Local to "محلی",
                    ).forEach { (value, label) ->
                        TopicChip(label, active = source == value, onClick = { source = value })
                    }
                }
                if (matchingPoets.isNotEmpty()) {
                    Text(
                        "شاعران",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        matchingPoets.forEach { poet ->
                            Column(
                                Modifier.width(72.dp).clip(MaterialTheme.shapes.medium).clickable { onOpenPoet(poet.key) }.padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                PoetAvatar(poet.name, poet.imageUrl, 52.dp)
                                Text(poet.name, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())

            Box(Modifier.fillMaxSize()) {
                when {
                    state.error != null -> ErrorBox(state.error!!, onRetry = vm::retry)
                    !state.searched && state.query.trim().length < 2 -> RecentSearches(
                        recent = recent,
                        onPick = {
                            vm.onQueryChange(it)
                            vm.saveRecent(it)
                        },
                        onClear = graph.library::clearRecentSearches,
                    )
                    state.searched && state.hits.isEmpty() && !state.loading ->
                        MessageBox(Icons.Filled.SearchOff, "نتیجه‌ای پیدا نشد", body = "واژهٔ دیگری را امتحان کنید")
                    else -> Results(
                        state = if (source == null) state else state.copy(hits = state.hits.filter { it.ref.source == source }),
                        onLoadMore = vm::loadMore,
                        onOpen = { hit ->
                            vm.saveRecent()
                            onOpenPoem(hit.ref)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentSearches(recent: List<String>, onPick: (String) -> Unit, onClear: () -> Unit) {
    if (recent.isEmpty()) {
        MessageBox(
            Icons.Filled.Search,
            "در میان شعرها بگردید",
            body = "جست‌وجوی «متنی» واژه‌ها را پیدا می‌کند و جست‌وجوی «معنایی» شعرهای هم‌مضمون را",
        )
        return
    }
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("جست‌وجوهای اخیر", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onClear) { Text("پاک کردن") }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            recent.forEach { term ->
                AssistChip(
                    onClick = { onPick(term) },
                    label = { Text(term) },
                    leadingIcon = { Icon(Icons.Filled.History, contentDescription = null) },
                )
            }
        }
    }
}

@Composable
private fun Results(state: SearchState, onLoadMore: () -> Unit, onOpen: (SearchHit) -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState, state.hits.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { last -> if (last >= state.hits.size - 4) onLoadMore() }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(state = listState, modifier = Modifier.widthIn(max = 840.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            state.total?.takeIf { it > 0 && state.mode == SearchMode.Text }?.let { total ->
                item(key = "total") {
                    Text(
                        "${total.toPersianDigits()} نتیجه",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
            items(state.hits, key = { it.ref.key }) { hit ->
                SearchHitRow(hit, if (state.mode == SearchMode.Text) state.query.trim() else "", onClick = { onOpen(hit) })
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
            }
            if (state.loadingMore) {
                item(key = "more") {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
            }
        }
    }
}

@Composable
private fun SearchHitRow(hit: SearchHit, term: String, onClick: () -> Unit) {
    val poemStyle = LocalPoemStyle.current
    val accent = MaterialTheme.colorScheme.primary
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            listOfNotNull(hit.poetName.takeIf { it.isNotBlank() }, hit.breadcrumb).joinToString(" » "),
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(hit.title, style = MaterialTheme.typography.titleMedium)
        if (hit.snippet.isNotBlank()) {
            Text(
                highlight(hit.snippet, term, SpanStyle(color = accent, fontWeight = FontWeight.Bold)),
                style = TextStyle(fontFamily = poemStyle.fontFamily, fontSize = poemStyle.fontSize * 0.9f, lineHeight = poemStyle.fontSize * 1.6f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun highlight(text: String, term: String, style: SpanStyle): AnnotatedString = buildAnnotatedString {
    if (term.isEmpty()) {
        append(text)
        return@buildAnnotatedString
    }
    var start = 0
    while (true) {
        val index = text.indexOf(term, start, ignoreCase = true)
        if (index < 0) break
        append(text.substring(start, index))
        withStyle(style) { append(text.substring(index, index + term.length)) }
        start = index + term.length
    }
    append(text.substring(start))
}
