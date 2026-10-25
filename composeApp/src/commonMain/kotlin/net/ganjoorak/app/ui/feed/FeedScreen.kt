package net.ganjoorak.app.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.CategoryRef
import net.ganjoorak.app.data.Poem
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.PoetSource
import net.ganjoorak.app.data.interestByKey
import net.ganjoorak.app.data.local.FeedMode
import net.ganjoorak.app.ui.components.ErrorBox
import net.ganjoorak.app.ui.components.LoadingBox
import net.ganjoorak.app.ui.components.SegmentedSwitch
import net.ganjoorak.app.ui.components.TopicChip
import net.ganjoorak.app.ui.poem.PoemControlsHeight
import net.ganjoorak.app.ui.poem.PoemFloatingControls
import net.ganjoorak.app.ui.poem.PoemHeader
import net.ganjoorak.app.ui.poem.coupletNumbers
import net.ganjoorak.app.ui.poem.poemBlocks
import net.ganjoorak.app.ui.poem.sideBySide
import net.ganjoorak.app.ui.share.ShareSheet
import net.ganjoorak.app.ui.theme.LocalPoemStyle

/**
 * The home feed, as on the site: one poem per page, swipe up for the next. Above each title sit the
 * «همه / دنبال‌شده‌ها» switch and the topic chips; the controls float over the poem.
 */
@Composable
fun FeedScreen(
    onOpenPoem: (PoemRef) -> Unit,
    onOpenPoet: (String) -> Unit,
    onOpenCategory: (Int) -> Unit,
    onOpenPoets: () -> Unit,
    onEditInterests: () -> Unit,
) {
    val graph = AppGraph.instance
    val vm = viewModel { FeedViewModel(graph.repository, graph.settings) }
    val state by vm.state.collectAsState()
    val settings by graph.settings.settings.collectAsState()
    val saved by graph.library.savedPoems.collectAsState()
    val player by graph.player.state.collectAsState()
    var sharing by remember { mutableStateOf<Poem?>(null) }

    val eyebrow: @Composable () -> Unit = {
        FeedEyebrow(
            mode = settings.feedMode,
            onMode = graph.settings::setFeedMode,
            interestKeys = settings.interestKeys.takeIf { settings.feedMode == FeedMode.All }.orEmpty(),
            activeKeys = settings.activeInterestKeys,
            onToggle = graph.settings::toggleActiveInterest,
            onClear = graph.settings::clearActiveInterests,
            onEdit = onEditInterests,
        )
    }

    val pager = rememberPagerState(pageCount = { state.poems.size + 1 })
    LaunchedEffect(state.generation) { pager.scrollToPage(0) }
    LaunchedEffect(pager) { snapshotFlow { pager.currentPage }.collect { vm.onPageVisible(it) } }
    val current = state.poems.getOrNull(pager.currentPage)

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            state.isFollowingEmpty -> FeedEmpty(
                eyebrow = eyebrow,
                title = "هنوز شاعری را دنبال نمی‌کنید",
                body = "شاعران را دنبال کنید تا این فید فقط از شعرهای آن‌ها پر شود. فید «همه» همچنان از همهٔ شاعران است.",
                primary = "دیدن شاعران" to onOpenPoets,
                secondary = "بازگشت به همه" to { graph.settings.setFeedMode(FeedMode.All) },
            )
            state.poems.isEmpty() && state.error != null -> Column(Modifier.fillMaxSize()) {
                Box(Modifier.windowInsetsPadding(WindowInsets.statusBars).fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                    eyebrow()
                }
                ErrorBox(state.error!!, onRetry = vm::retry)
            }
            else -> VerticalPager(
                state = pager,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                // The loader's key must change with its position: with a fixed key the pager keeps the
                // loader in view as poems are inserted before it, so the first poem never shows.
                key = { index -> state.poems.getOrNull(index)?.ref?.key ?: "loader-$index" },
            ) { index ->
                val poem = state.poems.getOrNull(index)
                when {
                    poem != null -> FeedPage(
                        poem = poem,
                        eyebrow = eyebrow,
                        activeVerse = player.activeVerseOrder.takeIf { player.poemRef == poem.ref },
                        onOpenPoet = onOpenPoet,
                        onOpenCategory = { crumb: CategoryRef -> onOpenCategory(crumb.id) }.takeIf { poem.source == PoetSource.Ganjoor },
                        onOpen = { onOpenPoem(poem.ref) },
                    )
                    state.error != null -> ErrorBox(state.error!!, onRetry = vm::retry)
                    else -> LoadingBox()
                }
            }
        }

        PoemFloatingControls(
            poem = current,
            isSaved = current != null && saved.any { it.ref == current.ref },
            isPlaying = current != null && player.poemRef == current.ref && player.isPlaying,
            onToggleSave = { current?.let(graph.library::toggleSaved) },
            onShare = { sharing = current },
            onPlay = {
                current?.let { poem -> if (player.poemRef == poem.ref) graph.player.toggle() else graph.player.play(poem) }
            },
            onOpenPoet = onOpenPoet,
        )
    }

    sharing?.let { poem -> ShareSheet(poem, onDismiss = { sharing = null }) }
}

/** The switch, a hairline and the scrollable topic chips, in one row above the title. */
@Composable
private fun FeedEyebrow(
    mode: FeedMode,
    onMode: (FeedMode) -> Unit,
    interestKeys: List<String>,
    activeKeys: List<String>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
    onEdit: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (interestKeys.isEmpty()) Arrangement.Center else Arrangement.Start,
    ) {
        SegmentedSwitch(
            options = listOf(FeedMode.All, FeedMode.Following),
            selected = mode,
            label = { if (it == FeedMode.All) "همه" else "دنبال‌شده‌ها" },
            onSelect = onMode,
        )
        if (interestKeys.isNotEmpty()) {
            VerticalDivider(Modifier.height(24.dp).padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
            LazyRow(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                item(key = "all") { TopicChip("همهٔ موضوع‌ها", active = activeKeys.isEmpty(), onClick = onClear) }
                items(interestKeys, key = { it }) { key ->
                    TopicChip(interestByKey(key)?.label ?: key, active = key in activeKeys, onClick = { onToggle(key) })
                }
                item(key = "edit") {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .clickable(onClick = onEdit),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Tune, contentDescription = "ویرایش علاقه‌مندی‌ها", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedPage(
    poem: Poem,
    eyebrow: @Composable () -> Unit,
    activeVerse: Int?,
    onOpenPoet: (String) -> Unit,
    onOpenCategory: ((CategoryRef) -> Unit)?,
    onOpen: () -> Unit,
) {
    val style = LocalPoemStyle.current
    val numbers = remember(poem) { coupletNumbers(poem.blocks) }
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val sideBySide = style.sideBySide(minOf(maxWidth, 900.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 900.dp).windowInsetsPadding(WindowInsets.statusBars),
            contentPadding = PaddingValues(bottom = PoemControlsHeight),
        ) {
            item(key = "header") {
                PoemHeader(poem, onPoetClick = onOpenPoet, onCategoryClick = onOpenCategory, eyebrow = eyebrow)
            }
            poemBlocks(poem, numbers, sideBySide, activeVerse, onVerseClick = null)
            item(key = "next") {
                Column(
                    Modifier.fillMaxWidth().padding(top = 24.dp).clickable(onClick = onOpen).padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "برای شعر بعدی بکشید",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                    )
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

/** The site's FeedModeEmpty: the switch on top, then an icon, a message and two actions. */
@Composable
private fun FeedEmpty(
    eyebrow: @Composable () -> Unit,
    title: String,
    body: String,
    primary: Pair<String, () -> Unit>,
    secondary: Pair<String, () -> Unit>,
) {
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        eyebrow()
        Spacer(Modifier.height(64.dp))
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Groups, contentDescription = null)
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 420.dp),
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = primary.second) {
            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(primary.first)
        }
        TextButton(onClick = secondary.second) { Text(secondary.first) }
    }
}
