package net.ganjoorak.app.ui.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.local.ThemeMode
import net.ganjoorak.app.resources.Res
import net.ganjoorak.app.resources.app_icon
import net.ganjoorak.app.ui.components.SegmentedSwitch
import net.ganjoorak.app.util.toPersianDigits
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

enum class MenuTarget { Home, Explore, Poets, Saved, Following, Profile, Settings }

/** The site's menu, shown in a bottom sheet: destinations in two groups, then theme and text size. */
@Composable
fun AppMenu(current: MenuTarget?, onSelect: (MenuTarget) -> Unit, onClose: () -> Unit) {
    val store = AppGraph.instance.settings
    val settings by store.settings.collectAsState()
    val colors = MaterialTheme.colorScheme

    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Column(Modifier.padding(vertical = 8.dp)) {
            MenuGroup("کشف")
            MenuItem(Icons.Filled.Home, "صفحهٔ اصلی", current == MenuTarget.Home) { onSelect(MenuTarget.Home) }
            MenuItem(Icons.Filled.Explore, "کاوش", current == MenuTarget.Explore) { onSelect(MenuTarget.Explore) }
            MenuItem(Icons.Filled.People, "شاعران", current == MenuTarget.Poets) { onSelect(MenuTarget.Poets) }
            MenuGroup("کتابخانهٔ من")
            MenuItem(Icons.Filled.FavoriteBorder, "نشان‌شده‌ها", current == MenuTarget.Saved) { onSelect(MenuTarget.Saved) }
            MenuItem(
                Icons.Filled.HowToReg,
                "شاعران دنبال‌شده",
                current == MenuTarget.Following,
                count = settings.followedPoets.size.takeIf { it > 0 },
            ) { onSelect(MenuTarget.Following) }
            MenuItem(Icons.Filled.AccountCircle, "حساب کاربری", current == MenuTarget.Profile) { onSelect(MenuTarget.Profile) }
        }

        HorizontalDivider(color = colors.outlineVariant)
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SegmentedSwitch(
                options = listOf(ThemeMode.Dark, ThemeMode.Light, ThemeMode.Paper),
                selected = if (settings.theme == ThemeMode.System) ThemeMode.Dark else settings.theme,
                label = {
                    when (it) {
                        ThemeMode.Dark -> "تاریک"
                        ThemeMode.Light -> "روشن"
                        else -> "کاغذی"
                    }
                },
                onSelect = { mode -> store.update { it.copy(theme = mode) } },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("اندازهٔ متن شعر", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { store.update { it.copy(fontScale = (it.fontScale - 0.05f).coerceAtLeast(0.8f)) } }) {
                    Icon(Icons.Filled.Remove, contentDescription = "کوچک‌تر کردن متن")
                }
                Text(
                    "${(settings.fontScale * 100).roundToInt().toPersianDigits()}٪",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.widthIn(min = 40.dp),
                )
                IconButton(onClick = { store.update { it.copy(fontScale = (it.fontScale + 0.05f).coerceAtMost(1.6f)) } }) {
                    Icon(Icons.Filled.Add, contentDescription = "بزرگ‌تر کردن متن")
                }
            }
        }
        MenuItem(Icons.Filled.Settings, "تنظیمات", current == MenuTarget.Settings) { onSelect(MenuTarget.Settings) }
        Box(Modifier.size(8.dp))
    }
}

@Composable
private fun MenuGroup(title: String) {
    Text(
        title,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun MenuItem(icon: ImageVector, label: String, selected: Boolean, count: Int? = null, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp)
            .clip(MaterialTheme.shapes.medium)
            .let { if (selected) it.background(colors.secondaryContainer) else it }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) colors.onSurface else colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        if (count != null) {
            Text(count.toPersianDigits(), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        }
    }
}
