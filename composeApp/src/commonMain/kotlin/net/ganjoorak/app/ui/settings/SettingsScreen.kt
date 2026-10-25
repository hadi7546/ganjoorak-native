package net.ganjoorak.app.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.ganjoorak.app.ui.components.AppPage

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    AppPage(title = "تنظیمات", onBack = onBack) {
        item { Box(Modifier.widthIn(max = 720.dp).fillMaxWidth()) { SettingsContent() } }
    }
}
