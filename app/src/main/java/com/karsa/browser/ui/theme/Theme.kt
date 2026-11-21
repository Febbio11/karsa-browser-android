package com.karsa.browser.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

// --- Palet prototipe (App.tsx / index.css) : gaya Claude AI ---
// Terang: bg #F7F5F0, teks #1F1E1B, aksen #C86442
// Gelap : bg #191816, teks #F2EFE9, aksen #D97757
val KarsaLightBg = Color(0xFFF7F5F0)
val KarsaLightSurface = Color(0xFFFAF8F4)
val KarsaLightCard = Color(0xFFFFFFFF)
val KarsaLightText = Color(0xFF1F1E1B)
val KarsaLightMuted = Color(0xFF6C675E)
val KarsaLightBorder = Color(0xFFE5E0D6)
val KarsaAccent = Color(0xFFC86442)
val KarsaAccentDark = Color(0xFF8E3C21)
val KarsaSuccess = Color(0xFF2E7D4F)

val KarsaDarkBg = Color(0xFF191816)
val KarsaDarkSurface = Color(0xFF21201D)
val KarsaDarkCard = Color(0xFF22201D)
val KarsaDarkText = Color(0xFFF2EFE9)
val KarsaDarkMuted = Color(0xFFA6A198)
val KarsaDarkBorder = Color(0xFF2E2C28)
val KarsaAccentDarkMode = Color(0xFFD97757)

// Tipografi prototipe: serif = Source Serif 4 (editorial),
// sans = Plus Jakarta Sans, mono = JetBrains Mono.
val KarsaSerif: FontFamily = FontFamily.Serif
val KarsaSans: FontFamily = FontFamily.SansSerif
val KarsaMono: FontFamily = FontFamily.Monospace

private val LightScheme = lightColorScheme(
    primary = KarsaAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF5EFE6),
    background = KarsaLightBg,
    onBackground = KarsaLightText,
    surface = KarsaLightSurface,
    onSurface = KarsaLightText,
    surfaceVariant = Color(0xFFEFECE6),
    onSurfaceVariant = KarsaLightMuted,
    outline = KarsaLightBorder,
    secondary = KarsaAccentDark,
    tertiary = KarsaSuccess
)

private val DarkScheme = darkColorScheme(
    primary = KarsaAccentDarkMode,
    onPrimary = Color(0xFF181715),
    primaryContainer = Color(0xFF2E2C28),
    background = KarsaDarkBg,
    onBackground = KarsaDarkText,
    surface = KarsaDarkSurface,
    onSurface = KarsaDarkText,
    surfaceVariant = Color(0xFF23221F),
    onSurfaceVariant = KarsaDarkMuted,
    outline = KarsaDarkBorder,
    secondary = KarsaAccentDarkMode,
    tertiary = Color(0xFF86CFA0)
)

@Composable
fun KarsaBrowserTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        content = content
    )
}
