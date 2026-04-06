package com.example.pockettrack.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SagePrimary   = Color(0xFF5A9467)
val SageLight     = Color(0xFF8FBC96)
val SageDark      = Color(0xFF3D7A52)
val SageContainer = Color(0xFFC8E6C9)
val Background    = Color(0xFFF4F9F5)
val IncomeGreen   = Color(0xFF2E7D32)
val ExpenseRed    = Color(0xFFC62828)
val CardBg        = Color(0xFFFFFFFF)

private val SagePalette = lightColorScheme(
    primary          = SagePrimary,
    onPrimary        = Color.White,
    primaryContainer = SageContainer,
    secondary        = SageLight,
    background       = Background,
    surface          = CardBg,
    onBackground     = Color(0xFF1A1A1A),
    onSurface        = Color(0xFF1A1A1A),
    error            = Color(0xFFC62828)
)

@Composable
fun PocketTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SagePalette, typography = Typography(), content = content)
}