package com.reto01.flood.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reto01.flood.data.FirmsDto
import com.reto01.flood.data.HotspotDto
import com.reto01.flood.ui.UiState
import com.reto01.flood.ui.ago
import com.reto01.flood.ui.components.Pill
import com.reto01.flood.ui.components.SectionCard
import com.reto01.flood.ui.localDateTime
import com.reto01.flood.ui.theme.CardBorder
import com.reto01.flood.ui.theme.Indigo
import com.reto01.flood.ui.theme.RiskCritical
import com.reto01.flood.ui.theme.RiskNormal
import com.reto01.flood.ui.theme.RiskUnknown
import com.reto01.flood.ui.theme.RiskWarning
import com.reto01.flood.ui.theme.TextMain
import com.reto01.flood.ui.theme.TextSoft

@Composable
fun SatelliteScreen(state: UiState) {
    val f = state.firms
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Spacer(Modifier.height(16.dp))
            Text("Satélite · NASA FIRMS", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Text("Detecciones térmicas MODIS/VIIRS (NRT) alrededor de la estación. Evidencia fechada " +
                "de contexto: no mide agua ni entra al score de riesgo.", color = TextSoft, fontSize = 12.sp)
        }
        item {
            SectionCard("Estado de la fuente", icon = Icons.Filled.Info,
                trailing = { Pill(f?.status ?: "…", if (f?.status == "AVAILABLE") RiskNormal else RiskUnknown) }) {
                SourceRow("Proveedor", f?.provider ?: "NASA_FIRMS")
                SourceRow("Sensores", f?.sources?.joinToString(", ") ?: "—")
                SourceRow("Ventana", "${f?.dayRange ?: "—"} día(s)")
                SourceRow("Consultado", f?.fetchedAt?.let { "${localDateTime(it)} (${ago(it)})" } ?: "—")
                SourceRow("Detecciones", "${f?.count ?: 0}")
                if (f?.status != "AVAILABLE") Text(
                    "Sin evidencia satelital reciente · ${f?.reason ?: "fuente no disponible"}",
                    color = TextSoft, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
        item {
            SectionCard("Mapa del área (bbox)", icon = Icons.Filled.Place) {
                MiniMap(f, state.station?.lat, state.station?.lon)
                Text("● estación   ● detección (color = FRP)", color = TextSoft, fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp))
            }
        }
        items(f?.items.orEmpty().take(30)) { h -> HotspotRow(h) }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

private fun frpColor(frp: Double?): Color = when {
    frp == null -> RiskUnknown
    frp >= 20 -> RiskCritical
    frp >= 5 -> RiskWarning
    else -> Color(0xFFFBBF24)
}

@Composable
private fun MiniMap(f: FirmsDto?, lat: Double?, lon: Double?) {
    val bbox = f?.bbox
    Canvas(
        Modifier.fillMaxWidth().aspectRatio(1.4f)
            .background(Color(0xFFEFF6EE), RoundedCornerShape(12.dp))
    ) {
        if (bbox == null || bbox.size != 4) return@Canvas
        val (w, s, e, n) = bbox
        fun px(lo: Double) = ((lo - w) / (e - w) * size.width).toFloat()
        fun py(la: Double) = ((n - la) / (n - s) * size.height).toFloat()
        val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        for (i in 1..3) {
            drawLine(CardBorder, Offset(size.width * i / 4, 0f), Offset(size.width * i / 4, size.height), 2f, pathEffect = dash)
            drawLine(CardBorder, Offset(0f, size.height * i / 4), Offset(size.width, size.height * i / 4), 2f, pathEffect = dash)
        }
        f?.items.orEmpty().forEach { h ->
            if (h.lat != null && h.lon != null) drawCircle(frpColor(h.frpMw), 9f, Offset(px(h.lon), py(h.lat)))
        }
        if (lat != null && lon != null) {
            drawCircle(Color.White, 16f, Offset(px(lon), py(lat)))
            drawCircle(Indigo, 11f, Offset(px(lon), py(lat)))
        }
    }
}

@Composable
private fun HotspotRow(h: HotspotDto) {
    SectionCard("${h.instrument ?: h.source ?: "FIRMS"} · ${h.satellite ?: ""}", icon = Icons.Filled.Star,
        trailing = { Pill(h.frpMw?.let { "%.1f MW".format(it) } ?: "FRP —", frpColor(h.frpMw)) }) {
        SourceRow("Adquisición", "${localDateTime(h.observedAt)} (${ago(h.observedAt)})")
        SourceRow("Coordenadas", "%.4f, %.4f".format(h.lat ?: 0.0, h.lon ?: 0.0))
        SourceRow("Confianza", h.confidence ?: "—")
        SourceRow("Brillo", h.brightnessK?.let { "%.1f K".format(it) } ?: "—")
    }
}
