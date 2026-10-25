package net.ganjoorak.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import net.ganjoorak.app.ui.components.LocalAppActions
import net.ganjoorak.app.ui.components.SegmentedSwitch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.filled.Menu
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
fun SearchScreen(onBack: () -> Unit, onOpenPoem: (PoemRef) -> Unit) {
    val graph = AppGraph.instance
    val vm = viewModel { SearchViewModel(graph.repository, graph.library) }
    val state by vm.state.collectAsState()
    val recent by graph.library.recentSearches.collectAsState()
    val focus = LocalFocusManager.current

    val app = LocalAppActions.current
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).windowInsetsPadding(WindowInsets.statusBars)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت") }
                Text(
                    "جست‌وجو",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                IconButton(onClick = app.openMenu) { Icon(Icons.Filled.Menu, contentDescription = "منو") }
            }
            Column(Modifier.widthIn(max = 840.dp).padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = vm::onQueryChange,
                    singleLine = true,
                    placeholder = { Text(if (state.mode == SearchMode.Text) "واژه یا مصراعی از شعر" else "مضمون، مثلاً «غم غربت»") },
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
                SegmentedSwitch(
                    options = SearchMode.entries,
                    selected = state.mode,
                    label = { if (it == SearchMode.Text) "متنی" else "معنایی" },
                    onSelect = vm::onModeChange,
                    modifier = Modifier.padding(vertical = 10.dp),
                )
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
                        state = state,
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
