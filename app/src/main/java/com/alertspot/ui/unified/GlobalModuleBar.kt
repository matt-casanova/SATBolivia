package com.alertspot.ui.unified

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Barra GLOBAL (nivel 1) de la app unificada: INCENDIOS | INUNDACIONES.
 *
 * Es una implementación compacta equivalente a NavigationBar (60 dp + inset del sistema)
 * para que, apilada bajo la barra interna de cada módulo (la barra iOS de Incendios o la
 * NavigationBar M3 de Inundaciones), no robe demasiado alto de pantalla. Usa los colores
 * de identidad de cada app: naranja de AlertSpot e índigo #667EEA de Alerta Río.
 * Es la única pieza que aplica el inset de la barra de navegación del sistema; el
 * contenedor de módulos lo marca como consumido para que no se aplique dos veces.
 */

private val FireAccent = Color(0xFFFF9500)       // Orange de com.alertspot.ui.theme
private val FireLabel = Color(0xFFB45309)        // naranja oscuro legible sobre blanco
private val FireAccentDark = Color(0xFFFF9F0A)   // naranja para modo oscuro de AlertSpot
private val FloodAccent = Color(0xFF667EEA)      // Indigo de com.reto01.flood.ui.theme
private val FloodLabel = Color(0xFF4C51BF)       // índigo oscuro legible sobre blanco
private val FloodLabelDark = Color(0xFF9AA8F5)   // índigo claro para modo oscuro

@Composable
fun GlobalModuleBar(
    selected: SatModule,
    onSelect: (SatModule) -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val container = if (darkTheme) Color(0xFF1C1C1E) else Color.White
    val divider = if (darkTheme) Color.White.copy(alpha = 0.10f) else Color(0xFFE5E7EB)
    val inactive = if (darkTheme) Color.White.copy(alpha = 0.60f) else Color(0xFF6B7280)

    // MaterialTheme neutro solo para que las pestañas tengan ripple (la barra vive fuera
    // de AlertSpotTheme y de Reto01Theme); los colores visibles son los de arriba.
    MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
        Column(modifier.fillMaxWidth().background(container)) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(divider))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(60.dp)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ModuleTab(
                    module = SatModule.FIRES,
                    icon = FlameIcon,
                    selected = selected == SatModule.FIRES,
                    accent = if (darkTheme) FireAccentDark else FireAccent,
                    labelColor = if (darkTheme) FireAccentDark else FireLabel,
                    inactive = inactive,
                    onClick = { onSelect(SatModule.FIRES) },
                )
                ModuleTab(
                    module = SatModule.FLOODS,
                    icon = WaterDropIcon,
                    selected = selected == SatModule.FLOODS,
                    accent = FloodAccent,
                    labelColor = if (darkTheme) FloodLabelDark else FloodLabel,
                    inactive = inactive,
                    onClick = { onSelect(SatModule.FLOODS) },
                )
            }
        }
    }
}

@Composable
private fun RowScope.ModuleTab(
    module: SatModule,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    labelColor: Color,
    inactive: Color,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(
        if (selected) accent.copy(alpha = 0.14f) else Color.Transparent, label = "moduleTabBackground"
    )
    val iconTint by animateColorAsState(if (selected) accent else inactive, label = "moduleTabIcon")
    val textColor by animateColorAsState(if (selected) labelColor else inactive, label = "moduleTabText")

    Row(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = module.label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = 0.6.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/* Iconos vectoriales propios (24×24): no dependen de material-icons-extended. */

private val FlameIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "SatFlame", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f,
    ).path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
        // llama exterior
        moveTo(13.0f, 2.0f)
        curveTo(13.6f, 5.2f, 12.0f, 7.4f, 10.0f, 9.4f)
        curveTo(7.9f, 11.5f, 6.0f, 13.4f, 6.0f, 16.0f)
        curveTo(6.0f, 19.3f, 8.7f, 22.0f, 12.0f, 22.0f)
        curveTo(15.3f, 22.0f, 18.0f, 19.3f, 18.0f, 16.0f)
        curveTo(18.0f, 12.6f, 16.2f, 10.2f, 15.0f, 8.6f)
        curveTo(14.8f, 10.3f, 14.1f, 11.4f, 13.0f, 11.8f)
        curveTo(13.8f, 8.6f, 13.7f, 5.0f, 13.0f, 2.0f)
        close()
        // núcleo (hueco por EvenOdd)
        moveTo(12.0f, 20.0f)
        curveTo(10.3f, 20.0f, 9.0f, 18.7f, 9.0f, 17.1f)
        curveTo(9.0f, 15.6f, 10.0f, 14.6f, 11.0f, 13.6f)
        curveTo(11.3f, 14.6f, 12.0f, 15.2f, 12.8f, 15.3f)
        curveTo(13.3f, 14.6f, 13.4f, 13.8f, 13.3f, 13.0f)
        curveTo(14.3f, 14.0f, 15.0f, 15.4f, 15.0f, 17.0f)
        curveTo(15.0f, 18.7f, 13.7f, 20.0f, 12.0f, 20.0f)
        close()
    }.build()
}

private val WaterDropIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "SatWaterDrop", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f,
    ).path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
        // gota
        moveTo(12.0f, 2.5f)
        curveTo(12.0f, 2.5f, 5.0f, 10.4f, 5.0f, 15.0f)
        curveTo(5.0f, 18.9f, 8.1f, 22.0f, 12.0f, 22.0f)
        curveTo(15.9f, 22.0f, 19.0f, 18.9f, 19.0f, 15.0f)
        curveTo(19.0f, 10.4f, 12.0f, 2.5f, 12.0f, 2.5f)
        close()
        // ola (hueco por EvenOdd), guiño al ícono original de Alerta Río
        moveTo(7.4f, 15.6f)
        curveTo(8.9f, 14.6f, 10.4f, 14.6f, 12.0f, 15.6f)
        curveTo(13.6f, 16.6f, 15.1f, 16.6f, 16.6f, 15.6f)
        lineTo(16.6f, 17.2f)
        curveTo(15.1f, 18.2f, 13.6f, 18.2f, 12.0f, 17.2f)
        curveTo(10.4f, 16.2f, 8.9f, 16.2f, 7.4f, 17.2f)
        close()
    }.build()
}
