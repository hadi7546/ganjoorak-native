package net.ganjoorak.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ganjoorak.app.data.local.AppSettings
import net.ganjoorak.app.data.local.CoupletLayout
import net.ganjoorak.app.data.local.PoemFont
import net.ganjoorak.app.data.local.ThemeMode
import net.ganjoorak.app.resources.Res
import net.ganjoorak.app.resources.gandom
import net.ganjoorak.app.resources.nahid
import net.ganjoorak.app.resources.parastoo
import net.ganjoorak.app.resources.sahel
import net.ganjoorak.app.resources.samim
import net.ganjoorak.app.resources.shabnam
import net.ganjoorak.app.resources.tanha
import net.ganjoorak.app.resources.vazirmatn
import org.jetbrains.compose.resources.Font

// The site's shadcn skin: a neutral palette where `card` is the raised surface, `border` the 1px
// outline, `muted` the quiet fill and `primary` the foreground colour itself.
private val LightColors = lightColorScheme(
    primary = Color(0xFF171717),
    onPrimary = Color(0xFFFAFAFA),
    primaryContainer = Color(0xFFF5F5F5),
    onPrimaryContainer = Color(0xFF171717),
    secondary = Color(0xFF171717),
    onSecondary = Color(0xFFFAFAFA),
    secondaryContainer = Color(0xFFF5F5F5),
    onSecondaryContainer = Color(0xFF171717),
    tertiary = Color(0xFF737373),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0A0A0A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF737373),
    surfaceTint = Color(0xFF171717),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF5F5F5),
    surfaceContainerHighest = Color(0xFFEFEFEF),
    outline = Color(0xFFA1A1A1),
    outlineVariant = Color(0xFFE5E5E5),
    error = Color(0xFFE7000B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE5E5E5),
    onPrimary = Color(0xFF171717),
    primaryContainer = Color(0xFF262626),
    onPrimaryContainer = Color(0xFFFAFAFA),
    secondary = Color(0xFFE5E5E5),
    onSecondary = Color(0xFF171717),
    secondaryContainer = Color(0xFF262626),
    onSecondaryContainer = Color(0xFFFAFAFA),
    tertiary = Color(0xFFA1A1A1),
    background = Color(0xFF0A0A0A),
    onBackground = Color(0xFFFAFAFA),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFF262626),
    onSurfaceVariant = Color(0xFFA1A1A1),
    surfaceTint = Color(0xFFE5E5E5),
    surfaceContainerLowest = Color(0xFF0A0A0A),
    surfaceContainerLow = Color(0xFF111111),
    surfaceContainer = Color(0xFF171717),
    surfaceContainerHigh = Color(0xFF262626),
    surfaceContainerHighest = Color(0xFF2E2E2E),
    outline = Color(0xFF737373),
    outlineVariant = Color(0xFF262626),
    error = Color(0xFFFF6467),
)

private val PaperColors = lightColorScheme(
    primary = Color(0xFF785A32),
    onPrimary = Color(0xFFFAF7ED),
    primaryContainer = Color(0xFFEEE8D8),
    onPrimaryContainer = Color(0xFF403428),
    secondary = Color(0xFF785A32),
    onSecondary = Color(0xFFFAF7ED),
    secondaryContainer = Color(0xFFEEE8D8),
    onSecondaryContainer = Color(0xFF403428),
    tertiary = Color(0xFF6E604E),
    background = Color(0xFFF4F0E5),
    onBackground = Color(0xFF403428),
    surface = Color(0xFFF4F0E5),
    onSurface = Color(0xFF403428),
    surfaceVariant = Color(0xFFEEE8D8),
    onSurfaceVariant = Color(0xFF6E604E),
    surfaceTint = Color(0xFF785A32),
    surfaceContainerLowest = Color(0xFFFAF7ED),
    surfaceContainerLow = Color(0xFFFAF7ED),
    surfaceContainer = Color(0xFFFAF7ED),
    surfaceContainerHigh = Color(0xFFEEE8D8),
    surfaceContainerHighest = Color(0xFFE1D2B4),
    outline = Color(0xFFB4A688),
    outlineVariant = Color(0xFFD6CCB7),
    error = Color(0xFFB04428),
)

// 0.625rem radius, as on the site.
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

/** How poem text is rendered; derived from [AppSettings]. */
@Immutable
data class PoemStyle(
    val fontFamily: FontFamily,
    val fontSize: TextUnit,
    val layout: CoupletLayout,
    val showVerseNumbers: Boolean,
    val autoScroll: Boolean,
)

val LocalPoemStyle = staticCompositionLocalOf {
    PoemStyle(FontFamily.Default, 18.sp, CoupletLayout.Auto, showVerseNumbers = false, autoScroll = true)
}

fun fontLabel(font: PoemFont): String = when (font) {
    PoemFont.Vazirmatn -> "وزیرمتن"
    PoemFont.Samim -> "صمیم"
    PoemFont.Tanha -> "تنها"
    PoemFont.Shabnam -> "شبنم"
    PoemFont.Gandom -> "گندم"
    PoemFont.Parastoo -> "پرستو"
    PoemFont.Sahel -> "ساحل"
    PoemFont.Nahid -> "ناهید"
}

@Composable
fun fontFamilyFor(font: PoemFont): FontFamily = FontFamily(
    Font(
        when (font) {
            PoemFont.Vazirmatn -> Res.font.vazirmatn
            PoemFont.Samim -> Res.font.samim
            PoemFont.Tanha -> Res.font.tanha
            PoemFont.Shabnam -> Res.font.shabnam
            PoemFont.Gandom -> Res.font.gandom
            PoemFont.Parastoo -> Res.font.parastoo
            PoemFont.Sahel -> Res.font.sahel
            PoemFont.Nahid -> Res.font.nahid
        },
    ),
)

private fun Typography.withFont(font: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = font),
    displayMedium = displayMedium.copy(fontFamily = font),
    displaySmall = displaySmall.copy(fontFamily = font),
    headlineLarge = headlineLarge.copy(fontFamily = font),
    headlineMedium = headlineMedium.copy(fontFamily = font),
    headlineSmall = headlineSmall.copy(fontFamily = font),
    titleLarge = titleLarge.copy(fontFamily = font),
    titleMedium = titleMedium.copy(fontFamily = font),
    titleSmall = titleSmall.copy(fontFamily = font),
    bodyLarge = bodyLarge.copy(fontFamily = font),
    bodyMedium = bodyMedium.copy(fontFamily = font),
    bodySmall = bodySmall.copy(fontFamily = font),
    labelLarge = labelLarge.copy(fontFamily = font),
    labelMedium = labelMedium.copy(fontFamily = font),
    labelSmall = labelSmall.copy(fontFamily = font),
)

@Composable
fun GanjoorakTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.theme) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Dark -> true
        ThemeMode.Light, ThemeMode.Paper -> false
    }
    val colors: ColorScheme = when {
        settings.theme == ThemeMode.Paper -> PaperColors
        dark -> DarkColors
        else -> LightColors
    }
    val uiFont = fontFamilyFor(PoemFont.Vazirmatn)
    val poemStyle = PoemStyle(
        fontFamily = fontFamilyFor(settings.poemFont),
        fontSize = 18.sp * settings.fontScale,
        layout = settings.coupletLayout,
        showVerseNumbers = settings.showVerseNumbers,
        autoScroll = settings.autoScrollWithAudio,
    )

    SystemBarsEffect(lightBackground = !dark)
    MaterialTheme(colorScheme = colors, typography = Typography().withFont(uiFont), shapes = AppShapes) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl,
            LocalPoemStyle provides poemStyle,
        ) {
            content()
        }
    }
}

/** Keeps status/navigation bar icons legible against the app background. */
@Composable
expect fun SystemBarsEffect(lightBackground: Boolean)
