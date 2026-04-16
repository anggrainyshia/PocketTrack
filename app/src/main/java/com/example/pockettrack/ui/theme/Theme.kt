package com.example.pockettrack.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Light: clean emerald on near-white ────────────────────────────────────
private val EmeraldLight = lightColorScheme(
    primary             = Emerald600,
    onPrimary           = Color.White,
    primaryContainer    = Emerald100,
    onPrimaryContainer  = Emerald900,
    secondary           = Emerald400,
    onSecondary         = Color.White,
    secondaryContainer  = Emerald50,
    onSecondaryContainer= Emerald700,
    background          = LightBackground,
    onBackground        = LightOnBg,
    surface             = LightSurface,
    onSurface           = LightOnBg,
    surfaceVariant      = Emerald50,
    onSurfaceVariant    = Emerald700,
    error               = ExpenseRed,
    outline             = Emerald200
)

// ── Dark: futuristic deep-green with glowing emerald primaries ─────────────
private val EmeraldDark = darkColorScheme(
    primary             = Emerald400,
    onPrimary           = Emerald900,
    primaryContainer    = Emerald700,
    onPrimaryContainer  = Emerald100,
    secondary           = Emerald500,
    onSecondary         = Color.Black,
    secondaryContainer  = DarkSurface2,
    onSecondaryContainer= Emerald200,
    background          = DarkBackground,
    onBackground        = DarkOnBg,
    surface             = DarkSurface,
    onSurface           = DarkOnSurface,
    surfaceVariant      = DarkSurface2,
    onSurfaceVariant    = Emerald200,
    error               = Color(0xFFFF6B6B),
    outline             = Emerald700
)

@Composable
fun PocketTrackTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) EmeraldDark else EmeraldLight,
        typography  = Typography(),
        content     = content
    )
}
