package net.ganjoorak.app.ui.poets

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.ui.components.AppPage
import net.ganjoorak.app.ui.components.ErrorBox
import net.ganjoorak.app.ui.components.Loadable
import net.ganjoorak.app.ui.components.LoadingBox
import net.ganjoorak.app.ui.components.TopicChip
import net.ganjoorak.app.ui.components.rememberLoader

@Composable
fun CategoryScreen(
    categoryId: Int,
    onBack: () -> Unit,
    onOpenPoem: (PoemRef) -> Unit,
    onOpenPoet: (String) -> Unit,
    onOpenCategory: (Int) -> Unit,
) {
    val graph = AppGraph.instance
    val loader = rememberLoader("cat-$categoryId") { graph.repository.category(categoryId) }
    val state by loader.state.collectAsState()
    val category = (state as? Loadable.Ready)?.value

    AppPage(
        title = category?.title ?: "",
        subtitle = category?.poet?.name,
        onBack = onBack,
        headerExtra = category?.takeIf { it.ancestors.isNotEmpty() }?.let { c ->
            @Composable {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    c.ancestors.forEach { ancestor ->
                        val poet = c.poet
                        TopicChip(ancestor.title, active = false, onClick = {
                            if (poet != null && ancestor.id == poet.rootCatId) onOpenPoet(poet.key) else onOpenCategory(ancestor.id)
                        })
                    }
                }
            }
        },
    ) {
        when (val s = state) {
            is Loadable.Loading -> item { LoadingBox(Modifier.height(240.dp)) }
            is Loadable.Error -> item { ErrorBox(s.message, onRetry = loader::reload, modifier = Modifier.height(320.dp)) }
            is Loadable.Ready -> {
                s.value.description?.let {
                    item(key = "description") {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
                categoryItems(s.value.children, onOpenCategory)
                poemItems(s.value.poems, onOpenPoem)
            }
        }
    }
}
