package net.ganjoorak.app.ui.poets

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.CategoryRef
import net.ganjoorak.app.data.GanjoorException
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.PoemSummary
import net.ganjoorak.app.data.PoetDetail
import net.ganjoorak.app.data.PoetSource
import net.ganjoorak.app.platform.shareText
import net.ganjoorak.app.ui.components.AppPage
import net.ganjoorak.app.ui.components.AppRow
import net.ganjoorak.app.ui.components.BorderedCard
import net.ganjoorak.app.ui.components.ErrorBox
import net.ganjoorak.app.ui.components.Loadable
import net.ganjoorak.app.ui.components.LoadingBox
import net.ganjoorak.app.ui.components.PoetAvatar
import net.ganjoorak.app.ui.components.SectionLabel
import net.ganjoorak.app.ui.components.rememberLoader
import net.ganjoorak.app.util.hijriLifespan
import net.ganjoorak.app.util.toPersianDigits

/** The site's poet profile: portrait, facts, «دنبال کردن», then the poet's books and poems. */
@Composable
fun PoetScreen(
    poetKey: String,
    onBack: () -> Unit,
    onOpenPoem: (PoemRef) -> Unit,
    onOpenCategory: (Int) -> Unit,
) {
    val graph = AppGraph.instance
    val loader = rememberLoader("poet-$poetKey") { graph.repository.poet(poetKey) }
    val state by loader.state.collectAsState()
    val settings by graph.settings.settings.collectAsState()
    val scope = rememberCoroutineScope()
    var loadingRandom by remember { mutableStateOf(false) }
    var randomError by remember { mutableStateOf<String?>(null) }
    val detail = (state as? Loadable.Ready)?.value

    AppPage(
        title = detail?.poet?.name ?: "شاعر",
        onBack = onBack,
        actions = {
            if (detail != null) {
                IconButton(onClick = { shareText("${detail.poet.name}\n${poetUrl(detail)}", detail.poet.name) }) {
                    Icon(Icons.Filled.Share, contentDescription = "اشتراک‌گذاری")
                }
            }
        },
        headerExtra = detail?.let { d ->
            @Composable {
                PoetHero(
                    detail = d,
                    followed = settings.followedPoets.any { it.key == d.poet.key },
                    onToggleFollow = { graph.settings.toggleFollow(d.poet.key, d.poet.name) },
                    loadingRandom = loadingRandom,
                    randomError = randomError,
                    onRandom = {
                        loadingRandom = true
                        randomError = null
                        scope.launch {
                            try {
                                onOpenPoem(graph.repository.randomPoem(d.poet.key).ref)
                            } catch (e: GanjoorException) {
                                randomError = e.message
                            } finally {
                                loadingRandom = false
                            }
                        }
                    },
                )
            }
        },
    ) {
        when (val s = state) {
            is Loadable.Loading -> item { LoadingBox(Modifier.height(240.dp)) }
            is Loadable.Error -> item { ErrorBox(s.message, onRetry = loader::reload, modifier = Modifier.height(320.dp)) }
            is Loadable.Ready -> {
                s.value.poet.description?.let { description ->
                    item(key = "description") { ExpandableText(description) }
                }
                categoryItems(s.value.root.children, onOpenCategory)
                poemItems(s.value.root.poems, onOpenPoem)
            }
        }
    }
}

private fun poetUrl(detail: PoetDetail): String = when (detail.poet.source) {
    PoetSource.Ganjoor -> "https://ganjoorak.ir/${detail.poet.slug}"
    PoetSource.Echolalia -> "https://ganjoorak.ir/echolalia/${detail.poet.slug}"
    PoetSource.Local -> "https://ganjoorak.ir/${detail.poet.slug}"
}

@Composable
private fun PoetHero(
    detail: PoetDetail,
    followed: Boolean,
    onToggleFollow: () -> Unit,
    loadingRandom: Boolean,
    randomError: String?,
    onRandom: () -> Unit,
) {
    val poet = detail.poet
    BorderedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                PoetAvatar(poet.name, poet.imageUrl, 72.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    poet.nickname?.takeIf { it != poet.name }?.let {
                        Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    val stats = listOfNotNull(
                        hijriLifespan(poet.birthYear, poet.deathYear),
                        poet.birthPlace?.let { "زادگاه: $it" },
                        poet.deathPlace?.let { "آرامگاه: $it" },
                        detail.root.children.size.takeIf { it > 0 }?.let { "${it.toPersianDigits()} اثر" },
                    )
                    stats.forEach {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (followed) {
                    Button(
                        onClick = onToggleFollow,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("دنبال می‌کنید")
                    }
                } else {
                    Button(onClick = onToggleFollow, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("دنبال کردن")
                    }
                }
                OutlinedButton(onClick = onRandom, enabled = !loadingRandom, modifier = Modifier.weight(1f)) {
                    if (loadingRandom) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("شعر تصادفی")
                }
            }
            if (randomError != null) {
                Text(randomError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ExpandableText(text: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Justify,
        maxLines = if (expanded) Int.MAX_VALUE else 4,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

fun LazyListScope.categoryItems(categories: List<CategoryRef>, onOpenCategory: (Int) -> Unit) {
    if (categories.isEmpty()) return
    item(key = "cats-title") { SectionLabel("آثار") }
    items(categories, key = { "c${it.id}" }) { category ->
        AppRow(
            title = category.title,
            leading = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null) },
            onClick = { onOpenCategory(category.id) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

fun LazyListScope.poemItems(poems: List<PoemSummary>, onOpenPoem: (PoemRef) -> Unit) {
    if (poems.isEmpty()) return
    item(key = "poems-title") { SectionLabel("شعرها") }
    items(poems, key = { "p${it.ref.key}" }) { poem ->
        AppRow(
            title = poem.title,
            meta = poem.excerpt,
            onClick = { onOpenPoem(poem.ref) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
