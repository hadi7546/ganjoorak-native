package net.ganjoorak.app.ui.poem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import net.ganjoorak.app.ui.components.FloatingIconButton
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.CategoryRef
import net.ganjoorak.app.data.Poem
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.PoetSource
import net.ganjoorak.app.data.Recitation
import net.ganjoorak.app.data.Verse
import net.ganjoorak.app.platform.copyText
import net.ganjoorak.app.player.PlayerController
import net.ganjoorak.app.player.PlayerState
import net.ganjoorak.app.ui.components.ErrorBox
import net.ganjoorak.app.ui.components.Loadable
import net.ganjoorak.app.ui.components.LoadingBox
import net.ganjoorak.app.ui.components.rememberLoader
import net.ganjoorak.app.ui.share.ShareSheet
import net.ganjoorak.app.ui.theme.LocalPoemStyle
import net.ganjoorak.app.util.toPersianDigits

@Composable
fun PoemScreen(
    ref: PoemRef,
    onBack: () -> Unit,
    onOpenPoem: (PoemRef) -> Unit,
    onOpenPoet: (String) -> Unit,
    onOpenCategory: (Int) -> Unit,
) {
    val graph = AppGraph.instance
    val loader = rememberLoader("poem-${ref.key}") { graph.repository.poem(ref) }
    var sharing by remember { mutableStateOf(false) }
    val state by loader.state.collectAsState()
    val saved by graph.library.savedPoems.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val poem = (state as? Loadable.Ready)?.value

    val player by graph.player.state.collectAsState()
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (val s = state) {
            is Loadable.Loading -> LoadingBox()
            is Loadable.Error -> ErrorBox(s.message, onRetry = loader::reload)
            is Loadable.Ready -> PoemReader(
                poem = s.value,
                player = graph.player,
                onOpenPoem = onOpenPoem,
                onOpenPoet = onOpenPoet,
                onOpenCategory = onOpenCategory,
            )
        }

        FloatingIconButton(
            Icons.AutoMirrored.Filled.ArrowBack,
            "بازگشت",
            onBack,
            modifier = Modifier.align(Alignment.TopStart).windowInsetsPadding(WindowInsets.statusBars).padding(12.dp),
        )

        PoemFloatingControls(
            poem = poem,
            isSaved = poem != null && saved.any { it.ref == poem.ref },
            isPlaying = poem != null && player.poemRef == poem.ref && player.isPlaying,
            onToggleSave = { poem?.let(graph.library::toggleSaved) },
            onShare = { sharing = true },
            onPlay = { poem?.let { p -> if (player.poemRef == p.ref) graph.player.toggle() else graph.player.play(p) } },
            onOpenPoet = onOpenPoet,
            extra = {
                if (poem != null) {
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        FloatingIconButton(Icons.Filled.MoreVert, "بیشتر", { menu = true })
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("کپی متن شعر") },
                                leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                                onClick = {
                                    menu = false
                                    copyText(poem.shareText())
                                    scope.launch { snackbar.showSnackbar("متن شعر کپی شد") }
                                },
                            )
                            if (poem.link.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text(if (poem.source == PoetSource.Ganjoor) "دیدن در گنجور" else "مشاهدهٔ منبع") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                                    onClick = {
                                        menu = false
                                        uriHandler.openUri(poem.link)
                                    },
                                )
                            }
                        }
                    }
                }
            },
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = PoemControlsHeight))
    }
    if (sharing && poem != null) ShareSheet(poem, onDismiss = { sharing = false })
}

@Composable
private fun PoemReader(
    poem: Poem,
    player: PlayerController,
    onOpenPoem: (PoemRef) -> Unit,
    onOpenPoet: (String) -> Unit,
    onOpenCategory: (Int) -> Unit,
) {
    val style = LocalPoemStyle.current
    val playerState by player.state.collectAsState()
    val isCurrent = playerState.poemRef == poem.ref
    val activeVerse = playerState.activeVerseOrder.takeIf { isCurrent }
    val numbers = remember(poem) { coupletNumbers(poem.blocks) }
    val listState = rememberLazyListState()
    val blocksStart = if (poem.recitations.isNotEmpty()) 2 else 1
    val seekOnClick: ((Verse) -> Unit)? = if (isCurrent) {
        { verse -> player.seekToVerse(verse.order) }
    } else {
        null
    }

    if (style.autoScroll) FollowActiveVerse(poem, activeVerse, listState, blocksStart)

    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val sideBySide = style.sideBySide(minOf(maxWidth, 900.dp))
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().widthIn(max = 900.dp).windowInsetsPadding(WindowInsets.statusBars),
            contentPadding = PaddingValues(top = 48.dp, bottom = PoemControlsHeight),
        ) {
            item(key = "header") {
                PoemHeader(
                    poem,
                    onPoetClick = onOpenPoet,
                    onCategoryClick = { crumb: CategoryRef -> onOpenCategory(crumb.id) }.takeIf { poem.source == PoetSource.Ganjoor },
                )
            }
            if (poem.recitations.isNotEmpty()) {
                item(key = "recitations") {
                    RecitationsCard(poem, playerState.takeIf { isCurrent }, player)
                }
            }
            poemBlocks(
                poem,
                numbers,
                sideBySide,
                activeVerse,
                onVerseClick = seekOnClick,
            )
            if (poem.previousId != null || poem.nextId != null) item(key = "nav") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val previous = poem.previousId
                    val next = poem.nextId
                    OutlinedButton(onClick = { previous?.let { onOpenPoem(PoemRef(PoetSource.Ganjoor, it)) } }, enabled = previous != null) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("شعر پیشین", modifier = Modifier.padding(start = 6.dp))
                    }
                    OutlinedButton(onClick = { next?.let { onOpenPoem(PoemRef(PoetSource.Ganjoor, it)) } }, enabled = next != null) {
                        Text("شعر بعدی", modifier = Modifier.padding(end = 6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowActiveVerse(poem: Poem, activeVerse: Int?, listState: LazyListState, blocksStart: Int) {
    LaunchedEffect(activeVerse) {
        val order = activeVerse ?: return@LaunchedEffect
        val blockIndex = poem.blocks.indexOfFirst { block -> block.verses.any { it.order == order } }
        if (blockIndex < 0) return@LaunchedEffect
        val target = blocksStart + blockIndex
        val layout = listState.layoutInfo
        val visible = layout.visibleItemsInfo.firstOrNull { it.index == target }
        val viewport = layout.viewportEndOffset - layout.viewportStartOffset
        val comfortable = visible != null && visible.offset > viewport / 6 && visible.offset + visible.size < viewport * 5 / 6
        if (!comfortable) listState.animateScrollToItem(target, scrollOffset = -viewport / 3)
    }
}

@Composable
private fun RecitationsCard(poem: Poem, state: PlayerState?, player: PlayerController) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val current = state?.recitation
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Headphones, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("خوانش‌ها (${poem.recitations.size.toPersianDigits()})", style = MaterialTheme.typography.titleSmall)
                Text(
                    current?.artist ?: poem.recitations.first().artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = {
                when {
                    current == null -> player.play(poem)
                    else -> player.toggle()
                }
            }) {
                Icon(
                    if (state?.isPlaying == true) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state?.isPlaying == true) "توقف" else "پخش",
                )
            }
            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
        }
        if (state != null && state.durationMs > 0) {
            SeekBar(state, player)
        }
        if (!player.isSupported) {
            Text(
                "در این نسخه، خوانش در مرورگر پخش می‌شود",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        if (expanded) {
            HorizontalDivider()
            poem.recitations.forEach { recitation ->
                RecitationRow(recitation, selected = recitation.id == current?.id) { player.play(poem, recitation) }
            }
            Spacer(Modifier.padding(bottom = 4.dp))
        }
    }
}

@Composable
private fun RecitationRow(recitation: Recitation, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        )
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                recitation.artist.ifBlank { "خوانش" },
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            if (recitation.title.isNotBlank()) {
                Text(recitation.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (recitation.inSync) {
            Text("همگام", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SeekBar(state: PlayerState, player: PlayerController) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    val fraction = dragging ?: (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
    Column(Modifier.padding(horizontal = 16.dp)) {
        Slider(
            value = fraction,
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { player.seekTo((it * state.durationMs).toLong()) }
                dragging = null
            },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime((fraction * state.durationMs).toLong()), style = MaterialTheme.typography.labelSmall)
            Text(formatTime(state.durationMs), style = MaterialTheme.typography.labelSmall)
        }
    }
}

fun formatTime(ms: Long): String {
    val total = ms / 1000
    val minutes = total / 60
    val seconds = total % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}".toPersianDigits()
}
