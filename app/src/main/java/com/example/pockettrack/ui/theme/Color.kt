package com.example.pockettrack.ui.theme

import androidx.compose.ui.graphics.Color

// ── Emerald Green Palette ──────────────────────────────────────────────────
val Emerald50   = Color(0xFFECFDF5)
val Emerald100  = Color(0xFFD1FAE5)
val Emerald200  = Color(0xFFA7F3D0)
val Emerald400  = Color(0xFF34D399)
val Emerald500  = Color(0xFF10B981)
val Emerald600  = Color(0xFF059669)
val Emerald700  = Color(0xFF047857)
val Emerald900  = Color(0xFF064E3B)

// ── Semantic colors (chosen to be readable on both light and dark surfaces) ─
val IncomeGreen  = Color(0xFF059669)   // Emerald600 – rich green, visible on white & dark
val ExpenseRed   = Color(0xFFDC2626)   // Red-600 – strong red, visible on white & dark

// ── Light theme surfaces ───────────────────────────────────────────────────
val LightBackground = Color(0xFFF0FDF9)
val LightSurface    = Color(0xFFFFFFFF)
val LightOnBg       = Color(0xFF0A1F17)

// ── Dark theme surfaces ────────────────────────────────────────────────────
val DarkBackground  = Color(0xFF050E0A)
val DarkSurface     = Color(0xFF0D1F17)
val DarkSurface2    = Color(0xFF122A1E)
val DarkOnBg        = Color(0xFFE0F2EC)
val DarkOnSurface   = Color(0xFFCCEDE3)

// ── Legacy aliases (referenced by other screens) ───────────────────────────
val SagePrimary   = Emerald500
val SageLight     = Emerald400
val SageDark      = Emerald700
val SageContainer = Emerald100
val Background    = LightBackground
val CardBg        = LightSurface
