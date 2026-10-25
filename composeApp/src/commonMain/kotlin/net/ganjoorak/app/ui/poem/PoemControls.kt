package net.ganjoorak.app.ui.poem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.data.Poem
import net.ganjoorak.app.ui.components.FloatingIconButton
import net.ganjoorak.app.ui.components.LocalAppActions
import net.ganjoorak.app.ui.components.PoetAvatar

/** Height the floating controls take at the bottom; poem lists pad their end by this much. */
val PoemControlsHeight = 132.dp

/**
 * The site's mobile controls over a poem: on the right (start) the menu and search, on the left
 * (end) share, save and the poet's avatar, and the play button alone at the bottom centre.
 */
@Composable
fun BoxScope.PoemFloatingControls(
    poem: Poem?,
    isSaved: Boolean,
    isPlaying: Boolean,
    onToggleSave: () -> Unit,
    onShare: () -> Unit,
    onPlay: () -> Unit,
    onOpenPoet: ((String) -> Unit)?,
    extra: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val app = LocalAppActions.current
    val edge = Modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(12.dp)

    Column(edge.align(Alignment.BottomStart), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FloatingIconButton(Icons.Filled.Search, "جست‌وجو", app.openSearch)
        FloatingIconButton(Icons.Filled.Menu, "منو", app.openMenu)
    }

    if (poem == null) return

    Column(
        edge.align(Alignment.BottomEnd),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        extra?.invoke(this)
        FloatingIconButton(Icons.Filled.Share, "هم‌رسانی", onShare)
        FloatingIconButton(
            if (isSaved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            if (isSaved) "حذف از پسندیده‌ها" else "پسندیدن",
            onToggleSave,
            active = isSaved,
        )
        val key = poem.poetKey
        if (key != null && onOpenPoet != null) {
            Surface(
                onClick = { onOpenPoet(key) },
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 2.dp,
                modifier = Modifier.size(44.dp),
            ) {
                PoetAvatar(poem.poetName, poem.poetImageUrl, 44.dp)
            }
        }
    }

    if (poem.recitations.isNotEmpty()) {
        Box(edge.align(Alignment.BottomCenter)) {
            FloatingIconButton(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                if (isPlaying) "توقف خوانش" else "پخش خوانش",
                onPlay,
                modifier = Modifier.size(52.dp),
                active = true,
            )
        }
    }
}
