package com.reto01.flood.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reto01.flood.ui.theme.CardBorder
import com.reto01.flood.ui.theme.RiskCritical
import com.reto01.flood.ui.theme.RiskWarning
import com.reto01.flood.ui.theme.TextMain
import com.reto01.flood.ui.theme.TextSoft
import com.reto01.flood.ui.theme.WaterBlue
import com.reto01.flood.ui.theme.WaterFill

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = TextSoft, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(title, fontWeight = FontWeight.SemiBold, color = TextMain, fontSize = 15.sp,
                    modifier = Modifier.weight(1f))
                trailing?.invoke()
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun KpiTile(label: String, value: String, modifier: Modifier = Modifier, sub: String? = null,
            accent: Color = TextMain) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, color = TextSoft, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (sub != null) Text(sub, color = TextSoft, fontSize = 11.sp)
        }
    }
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier, filled: Boolean = true) {
    Box(
        modifier
            .background(if (filled) color else color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = if (filled) Color.White else color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Dot(color: Color) {
    Box(Modifier.size(10.dp).background(color, CircleShape))
}

@Composable
fun Banner(text: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Dot(color)
        Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * Gráfico de nivel (últimos N puntos) estilo HydroSnap: área azul + líneas de umbral.
 */
@Composable
fun LevelChart(
    levels: List<Double>,
    warning: Double?,
    critical: Double?,
    modifier: Modifier = Modifier,
    suspectIdx: Set<Int> = emptySet(),
) {
    if (levels.isEmpty()) {
        Box(modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
            Text("Sin historial todavía", color = TextSoft)
        }
        return
    }
    val all = levels + listOfNotNull(warning, critical)
    val minY = (all.minOrNull() ?: 0.0) - 10
    val maxY = (all.maxOrNull() ?: 1.0) + 10
    val labelPaint = android.graphics.Paint().apply {
        isAntiAlias = true; textSize = 26f; color = TextSoft.toArgb()
    }
    Canvas(modifier.fillMaxWidth().height(180.dp)) {
        val left = 70f
        val w = size.width - left
        val h = size.height - 10f
        fun y(v: Double) = (h - ((v - minY) / (maxY - minY) * h)).toFloat() + 5f
        fun x(i: Int) = left + if (levels.size == 1) w / 2 else i * w / (levels.size - 1)

        // ejes Y
        listOf(minY + 10, (minY + maxY) / 2, maxY - 10).forEach { v ->
            drawContext.canvas.nativeCanvas.drawText("%.0f".format(v), 0f, y(v) + 8f, labelPaint)
            drawLine(CardBorder, Offset(left, y(v)), Offset(size.width, y(v)), 1f)
        }
        // área + línea
        val area = Path().apply {
            moveTo(x(0), h + 5f)
            levels.forEachIndexed { i, v -> lineTo(x(i), y(v)) }
            lineTo(x(levels.lastIndex), h + 5f); close()
        }
        drawPath(area, WaterFill)
        val line = Path().apply {
            levels.forEachIndexed { i, v -> if (i == 0) moveTo(x(i), y(v)) else lineTo(x(i), y(v)) }
        }
        drawPath(line, WaterBlue, style = Stroke(width = 5f, cap = StrokeCap.Round))
        // umbrales
        val dash = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
        warning?.let { drawLine(RiskWarning, Offset(left, y(it)), Offset(size.width, y(it)), 3f, pathEffect = dash) }
        critical?.let { drawLine(RiskCritical, Offset(left, y(it)), Offset(size.width, y(it)), 3f, pathEffect = dash) }
        // último punto + sospechosos
        suspectIdx.forEach { i -> if (i in levels.indices) drawCircle(RiskWarning, 7f, Offset(x(i), y(levels[i]))) }
        drawCircle(WaterBlue, 9f, Offset(x(levels.lastIndex), y(levels.last())))
        drawCircle(Color.White, 4f, Offset(x(levels.lastIndex), y(levels.last())))
    }
}
