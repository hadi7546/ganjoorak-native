package net.ganjoorak.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import net.ganjoorak.app.APP_VERSION
import net.ganjoorak.app.AppGraph
import net.ganjoorak.app.data.PoemBlock
import net.ganjoorak.app.data.Verse
import net.ganjoorak.app.data.VersePosition
import net.ganjoorak.app.data.local.CoupletLayout
import net.ganjoorak.app.data.local.PoemFont
import net.ganjoorak.app.data.local.ThemeMode
import net.ganjoorak.app.ui.components.SectionTitle
import net.ganjoorak.app.ui.poem.PoemBlockView
import net.ganjoorak.app.ui.theme.fontFamilyFor
import net.ganjoorak.app.ui.theme.fontLabel
import net.ganjoorak.app.util.toPersianDigits
import kotlin.math.roundToInt

private val SamplePreview = PoemBlock.Couplet(
    Verse(1, VersePosition.Right, "بشنو این نی چون شکایت می‌کند"),
    Verse(2, VersePosition.Left, "از جدایی‌ها حکایت می‌کند"),
)

/** Reading and appearance settings plus the about section; shown on the profile page. */
@Composable
fun SettingsContent() {
    val store = AppGraph.instance.settings
    val settings by store.settings.collectAsState()
    val uriHandler = LocalUriHandler.current

    Column {
        Card(
            Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Box(Modifier.padding(vertical = 16.dp)) {
                PoemBlockView(SamplePreview, number = 1, sideBySide = false, activeVerse = null, onVerseClick = null)
            }
        }

        SectionTitle("پوسته")
        ChipRow(
            options = listOf(ThemeMode.Dark, ThemeMode.Light, ThemeMode.Paper, ThemeMode.System),
            selected = settings.theme,
            label = {
                when (it) {
                    ThemeMode.Dark -> "تاریک"
                    ThemeMode.Light -> "روشن"
                    ThemeMode.Paper -> "کاغذی"
                    ThemeMode.System -> "هماهنگ با دستگاه"
                }
            },
            onSelect = { mode -> store.update { it.copy(theme = mode) } },
        )

        SectionTitle("قلم شعر")
        FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PoemFont.entries.forEach { font ->
                FilterChip(
                    selected = settings.poemFont == font,
                    onClick = { store.update { it.copy(poemFont = font) } },
                    label = { Text(fontLabel(font), style = TextStyle(fontFamily = fontFamilyFor(font))) },
                )
            }
        }

        SectionTitle("اندازهٔ متن شعر: ${(settings.fontScale * 100).roundToInt().toPersianDigits()}٪")
        Slider(
            value = settings.fontScale,
            onValueChange = { value -> store.update { it.copy(fontScale = (value * 20).roundToInt() / 20f) } },
            valueRange = 0.8f..1.6f,
            steps = 15,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        SectionTitle("چیدمان بیت‌ها")
        ChipRow(
            options = CoupletLayout.entries,
            selected = settings.coupletLayout,
            label = {
                when (it) {
                    CoupletLayout.Auto -> "خودکار"
                    CoupletLayout.Stacked -> "زیر هم"
                    CoupletLayout.SideBySide -> "دو ستونی"
                }
            },
            onSelect = { layout -> store.update { it.copy(coupletLayout = layout) } },
        )

        SwitchRow("شمارهٔ بیت‌ها", settings.showVerseNumbers) { on -> store.update { it.copy(showVerseNumbers = on) } }
        SwitchRow("دنبال کردن بیت‌ها هنگام خوانش", settings.autoScrollWithAudio) { on ->
            store.update { it.copy(autoScrollWithAudio = on) }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        SectionTitle("درباره")
        Text(
            "گنجورک ${APP_VERSION.toPersianDigits()} — خوانش شعر فارسی از گنجینهٔ گنجور",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LinkRow("وب‌سایت گنجورک", "ganjoorak.ir") { uriHandler.openUri("https://ganjoorak.ir") }
        LinkRow("گنجور", "اشعار و خوانش‌ها از ganjoor.net") { uriHandler.openUri("https://ganjoor.net") }
        LinkRow("کد منبع", "github.com/hadi7546/ganjoorak") { uriHandler.openUri("https://github.com/hadi7546/ganjoorak") }
    }
}

@Composable
private fun <T> ChipRow(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(label(option)) })
        }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun LinkRow(title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
