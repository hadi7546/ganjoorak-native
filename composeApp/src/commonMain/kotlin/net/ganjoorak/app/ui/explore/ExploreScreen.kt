package net.ganjoorak.app.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.GanjoorException
import net.ganjoorak.app.data.Poem
import net.ganjoorak.app.data.PoemBlock
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.Poet
import net.ganjoorak.app.ui.components.AppPageBar
import net.ganjoorak.app.ui.components.AppPageHeader
import net.ganjoorak.app.ui.components.PoetAvatar
import net.ganjoorak.app.ui.theme.LocalPoemStyle
import net.ganjoorak.app.util.toPersianDigits

private sealed interface ExploreItem {
    val key: String

    class PoemCard(val poem: Poem) : ExploreItem {
        override val key = "poem-${poem.ref.key}"
    }

    class PoetCard(val poet: Poet, val caption: String, val tall: Boolean) : ExploreItem {
        override val key = "poet-${poet.key}"
    }
}

private const val PAGE_SIZE = 8
private const val CONCURRENCY = 4
private const val POEMS_PER_POET = 3

/**
 * The site's «کاوش»: a masonry of random poem cards (a few lines, the poet and the title) with a
 * poet card every few poems; it keeps loading as you scroll and «مجموعهٔ تازه» starts over.
 */
@Composable
fun ExploreScreen(onBack: () -> Unit, onOpenPoem: (PoemRef) -> Unit, onOpenPoet: (String) -> Unit) {
    val repository = AppGraph.instance.repository
    val cards = remember { mutableStateListOf<ExploreItem>() }
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var generation by remember { mutableIntStateOf(0) }
    var request by remember { mutableIntStateOf(0) }
    val gridState = rememberLazyStaggeredGridState()
    val scrolled by remember { derivedStateOf { gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 120 } }

    // Loads one page whenever asked; [generation] restarts the set.
    LaunchedEffect(generation, request) {
        loading = true
        failed = false
        val poets = runCatching { repository.poetGroups().flatMap { group -> group.poets.map { it to group.title } }.shuffled() }
            .getOrDefault(emptyList())
        val loaded = coroutineScope {
            List(CONCURRENCY) {
                async {
                    List(PAGE_SIZE / CONCURRENCY) {
                        try {
                            repository.randomPoem(null)
                        } catch (e: GanjoorException) {
                            null
                        }
                    }.filterNotNull()
                }
            }.awaitAll().flatten()
        }
        var poemCount = cards.count { it is ExploreItem.PoemCard }
        loaded.forEach { poem ->
            if (cards.none { it.key == "poem-${poem.ref.key}" }) {
                cards += ExploreItem.PoemCard(poem)
                poemCount++
                if (poemCount % POEMS_PER_POET == 0) {
                    poets.firstOrNull { (poet, _) -> cards.none { it.key == "poet-${poet.key}" } }
                        ?.let { (poet, century) -> cards += ExploreItem.PoetCard(poet, century, tall = poemCount % (POEMS_PER_POET * 2) == 0) }
                }
            }
        }
        failed = loaded.isEmpty()
        loading = false
    }

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { last -> if (!loading && !failed && last >= cards.size - 4) request++ }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppPageBar("کاوش", scrolled, onBack) {
            IconButton(onClick = {
                cards.clear()
                generation++
            }) { Icon(Icons.Filled.Refresh, contentDescription = "مجموعهٔ تازه") }
        }
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(160.dp),
            state = gridState,
            modifier = Modifier.weight(1f).fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
        ) {
            item(key = "header", span = StaggeredGridItemSpan.FullLine) {
                AppPageHeader("کاوش", "شعرها و شاعرانی برای گشت‌وگذار؛ هر بار مجموعه‌ای تازه.")
            }
            items(cards, key = { it.key }) { item ->
                when (item) {
                    is ExploreItem.PoemCard -> PoemCard(item.poem, onClick = { onOpenPoem(item.poem.ref) })
                    is ExploreItem.PoetCard -> PoetCard(item.poet, item.caption, item.tall, onClick = { onOpenPoet(item.poet.key) })
                }
            }
            if (loading) {
                items(4, key = { "skeleton-$it" }) { index ->
                    Box(
                        Modifier.fillMaxWidth().height(if (index % 2 == 0) 180.dp else 140.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    )
                }
            }
            if (failed && !loading) {
                item(key = "retry", span = StaggeredGridItemSpan.FullLine) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("شعری دریافت نشد", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { request++ }) { Text("تلاش دوباره") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardBox(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick),
        content = content,
    )
}

/** Like a video thumbnail's duration chip, in the top corner. */
@Composable
private fun BoxScope.Badge(content: @Composable () -> Unit) {
    Row(
        Modifier
            .align(Alignment.TopEnd)
            .padding(7.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f))
            .padding(horizontal = 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.surface,
        ) { content() }
    }
}

@Composable
private fun PoemCard(poem: Poem, onClick: () -> Unit) {
    val prose = poem.blocks.any { it is PoemBlock.Prose }
    val lines = poem.blocks.flatMap { it.verses }.map { it.text }.filter { it.isNotBlank() }.take(if (prose) 1 else 4)
    CardBox(onClick) {
        Column {
            Column(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 26.dp, bottom = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                lines.forEachIndexed { index, line ->
                    Text(
                        line,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = LocalPoemStyle.current.fontFamily),
                        fontWeight = FontWeight.Medium,
                        textAlign = if (prose) TextAlign.Justify else TextAlign.Center,
                        maxLines = if (prose) 6 else 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = if (index % 2 == 1) 8.dp else 0.dp),
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PoetAvatar(poem.poetName, poem.poetImageUrl, 30.dp)
                Column(Modifier.weight(1f)) {
                    Text(poem.poetName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        poem.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (poem.recitations.isNotEmpty()) {
            Badge {
                Icon(Icons.Filled.Headphones, contentDescription = null, modifier = Modifier.size(11.dp))
                Text(poem.recitations.size.toPersianDigits(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PoetCard(poet: Poet, caption: String, tall: Boolean, onClick: () -> Unit) {
    CardBox(onClick, Modifier.aspectRatio(if (tall) 0.62f else 0.8f)) {
        if (poet.imageUrl.isNotBlank()) {
            AsyncImage(poet.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { PoetAvatar(poet.name, null, 72.dp) }
        }
        Badge {
            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(11.dp))
            Text("شاعر", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                .padding(horizontal = 10.dp, vertical = 10.dp),
        ) {
            Text(poet.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
            Text(caption, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f), maxLines = 1)
        }
    }
}
