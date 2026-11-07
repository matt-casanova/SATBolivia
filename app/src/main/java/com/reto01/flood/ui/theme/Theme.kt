package com.reto01.flood.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Paleta inspirada en HydroSnap (degradado índigo→violeta, tarjetas blancas).
val Indigo = Color(0xFF667EEA)
val Violet = Color(0xFF764BA2)
val Bg = Color(0xFFF5F7FB)
val CardBorder = Color(0xFFE5E7EB)
val TextMain = Color(0xFF1F2937)
val TextSoft = Color(0xFF6B7280)
val WaterBlue = Color(0xFF3B82F6)
val WaterFill = Color(0x553B82F6)

val RiskNormal = Color(0xFF10B981)
val RiskWarning = Color(0xFFF59E0B)
val RiskCritical = Color(0xFFE53935)
val RiskUnknown = Color(0xFF9CA3AF)

val HeaderGradient = Brush.horizontalGradient(listOf(Indigo, Violet))

fun riskColor(level: String?): Color = when (level) {
    "CRITICAL" -> RiskCritical
    "WARNING" -> RiskWarning
    "NORMAL" -> RiskNormal
    else -> RiskUnknown
}

fun riskLabel(level: String?): String = when (level) {
    "CRITICAL" -> "CRÍTICO"
    "WARNING" -> "ALERTA"
    "NORMAL" -> "NORMAL"
    else -> "SIN DATOS"
}

@Composable
fun Reto01Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Indigo, secondary = Violet, background = Bg, surface = Color.White,
            onPrimary = Color.White, onBackground = TextMain, onSurface = TextMain, error = RiskCritical,
        ),
        content = content,
    )
}
