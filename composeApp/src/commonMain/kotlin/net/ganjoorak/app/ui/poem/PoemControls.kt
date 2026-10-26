package net.ganjoorak.app.ui.poem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.data.Poem
import net.ganjoorak.app.ui.components.LocalAppActions
import net.ganjoorak.app.ui.components.PoetAvatar

/** Height the floating bar takes at the bottom; poem lists pad their end by this much. */
val PoemControlsHeight = 96.dp

private val BarHeight = 56.dp

/**
 * The poem screen's controls as a Material 3 Expressive floating bar: a pill with the poet, like,
 * share, play (a capsule that spells out «در حال پخش» while playing, like a selected tab) and the
 * menu, and search in its own round button beside it.
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
    extra: (@Composable RowScope.() -> Unit)? = null,
) {
    val app = LocalAppActions.current
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = CircleShape, color = colors.surfaceContainerHigh, shadowElevation = 6.dp) {
            Row(
                Modifier.height(BarHeight).padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val key = poem?.poetKey
                if (poem != null && key != null && onOpenPoet != null) {
                    BarItem(onClick = { onOpenPoet(key) }, description = poem.poetName) {
                        PoetAvatar(poem.poetName, poem.poetImageUrl, 32.dp)
                    }
                }
                if (poem != null) {
                    BarIcon(
                        if (isSaved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        if (isSaved) "حذف از پسندیده‌ها" else "پسندیدن",
                        onToggleSave,
                        tint = if (isSaved) colors.error else null,
                    )
                    BarIcon(Icons.Filled.Share, "هم‌رسانی", onShare)
                    extra?.invoke(this)
                    if (poem.recitations.isNotEmpty()) {
                        PlayCapsule(isPlaying, onPlay)
                    }
                }
                BarIcon(Icons.Filled.Menu, "منو", app.openMenu)
            }
        }
        Surface(
            onClick = app.openSearch,
            shape = CircleShape,
            color = colors.surfaceContainerHigh,
            shadowElevation = 6.dp,
            modifier = Modifier.size(BarHeight),
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Search, contentDescription = "جست‌وجو") }
        }
    }
}

@Composable
private fun BarItem(onClick: () -> Unit, description: String, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .selectable(selected = false, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** An icon-only item of the pill; [description] is what screen readers announce. */
@Composable
fun BarIcon(icon: ImageVector, description: String, onClick: () -> Unit, tint: Color? = null) {
    BarItem(onClick, description) {
        Icon(icon, contentDescription = description, tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Play/pause as the pill's highlighted capsule, the way the selected tab shows its label. */
@Composable
private fun PlayCapsule(isPlaying: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(if (isPlaying) colors.primary else colors.primaryContainer)
    val content by animateColorAsState(if (isPlaying) colors.onPrimary else colors.onPrimaryContainer)
    Row(
        Modifier
            .height(44.dp)
            .clip(CircleShape)
            .background(container)
            .selectable(selected = isPlaying, role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "توقف خوانش" else "پخش خوانش",
            tint = content,
        )
        AnimatedVisibility(visible = isPlaying, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
            Text(
                "در حال پخش",
                color = content,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}
