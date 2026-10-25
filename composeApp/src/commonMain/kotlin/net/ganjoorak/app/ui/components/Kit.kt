package net.ganjoorak.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.util.toPersianDigits

/** What every screen can ask of the shell: the navigation drawer and the search page. */
class AppActions(val openMenu: () -> Unit, val openSearch: () -> Unit)

val LocalAppActions = staticCompositionLocalOf { AppActions(openMenu = {}, openSearch = {}) }

/** The site's outline icon button: a round card with a 1px border, used for the floating controls. */
@Composable
fun FloatingIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        color = if (active) colors.primary else colors.surfaceContainer.copy(alpha = 0.92f),
        contentColor = if (active) colors.onPrimary else colors.onSurface,
        border = BorderStroke(1.dp, colors.outlineVariant),
        shadowElevation = 2.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(20.dp))
        }
    }
}

/** A pill-shaped segmented control, like the site's «همه / دنبال‌شده‌ها» switch. */
@Composable
fun <T> SegmentedSwitch(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier
            .clip(CircleShape)
            .border(1.dp, colors.outlineVariant, CircleShape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { option ->
            val active = option == selected
            val background by animateColorAsState(if (active) colors.primary else Color.Transparent, tween(240))
            val content by animateColorAsState(if (active) colors.onPrimary else colors.onSurfaceVariant, tween(240))
            Box(
                Modifier
                    .height(30.dp)
                    .defaultMinSize(minWidth = 56.dp)
                    .clip(CircleShape)
                    .background(background)
                    .selectable(selected = active, role = Role.Tab, onClick = { if (!active) onSelect(option) })
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label(option), color = content, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
    }
}

/** A rounded topic chip; the active one is filled with the foreground colour. */
@Composable
fun TopicChip(label: String, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(if (active) colors.primary else Color.Transparent, tween(200))
    val content by animateColorAsState(if (active) colors.onPrimary else colors.onSurface.copy(alpha = 0.8f), tween(200))
    Box(
        modifier
            .height(32.dp)
            .clip(CircleShape)
            .border(1.dp, if (active) colors.primary else colors.outlineVariant, CircleShape)
            .background(background)
            .selectable(selected = active, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = content, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** A bordered card like shadcn's `Card`. */
@Composable
fun BorderedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large
    val base = modifier
        .clip(shape)
        .border(1.dp, colors.outlineVariant, shape)
        .background(colors.surfaceContainer)
    Box(if (onClick != null) base.clickable(onClick = onClick) else base) { content() }
}

/** A list row in the site's `app-row` style: leading visual, title with a muted line, trailing slot. */
@Composable
fun AppRow(
    title: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier
            .widthIn(max = 720.dp)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
            if (meta != null) {
                Text(
                    meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
        if (trailing != null) {
            androidx.compose.runtime.CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                trailing()
            }
        }
    }
}

/**
 * shadcn `Tabs`: a muted rounded track where the active tab is a raised card. Each tab may carry a
 * small count, like the site's library and poets tabs.
 */
@Composable
fun <T> AppTabs(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    count: (T) -> Int? = { null },
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier
            .clip(shape)
            .background(colors.surfaceContainerHigh)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { option ->
            val active = option == selected
            val background by animateColorAsState(if (active) colors.background else Color.Transparent, tween(220))
            val content by animateColorAsState(if (active) colors.onSurface else colors.onSurfaceVariant, tween(220))
            Row(
                Modifier
                    .height(34.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(background)
                    .selectable(selected = active, role = Role.Tab, onClick = { if (!active) onSelect(option) })
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(label(option), color = content, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                count(option)?.takeIf { it > 0 }?.let {
                    Text(
                        it.toPersianDigits(),
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}
