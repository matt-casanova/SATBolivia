package com.reto01.flood.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reto01.flood.ui.Connection
import com.reto01.flood.ui.UiState
import com.reto01.flood.ui.ago
import com.reto01.flood.ui.alertStatusLabel
import com.reto01.flood.ui.cm
import com.reto01.flood.ui.components.Banner
import com.reto01.flood.ui.components.KpiTile
import com.reto01.flood.ui.components.LevelChart
import com.reto01.flood.ui.components.Pill
import com.reto01.flood.ui.components.SectionCard
import com.reto01.flood.ui.dataStatusLabel
import com.reto01.flood.ui.localDateTime
import com.reto01.flood.ui.localTime
import com.reto01.flood.ui.mm
import com.reto01.flood.ui.signedRate
import com.reto01.flood.ui.theme.HeaderGradient
import com.reto01.flood.ui.theme.RiskCritical
import com.reto01.flood.ui.theme.RiskNormal
import com.reto01.flood.ui.theme.RiskUnknown
import com.reto01.flood.ui.theme.RiskWarning
import com.reto01.flood.ui.theme.TextSoft
import com.reto01.flood.ui.theme.riskColor
import com.reto01.flood.ui.theme.riskLabel

@Composable
fun HomeScreen(state: UiState, onAck: (Int) -> Unit, onOpenAlerts: () -> Unit, onOpenSatellite: () -> Unit) {
    val s = state.station
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header(state)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ConnectionBanner(state)
            if (s == null) {
                Row(Modifier.fillMaxWidth().padding(32.dp), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            } else {
            RiskCard(state, onAck, onOpenAlerts)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiTile("Nivel del río", cm(s.levelCm), Modifier.weight(1f),
                    sub = "montaje ${cm(s.mountHeightCm)}", accent = riskColor(s.riskLevel))
                KpiTile("Tendencia", signedRate(s.riseCmPerMin), Modifier.weight(1f),
                    sub = if ((s.riseCmPerMin ?: 0.0) > 0.5) "subiendo" else if ((s.riseCmPerMin ?: 0.0) < -0.5) "bajando" else "estable",
                    accent = if ((s.riseCmPerMin ?: 0.0) > 0.5) RiskWarning else RiskNormal)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiTile("Pronóstico 30 min", cm(s.forecastLevel30mCm), Modifier.weight(1f),
                    sub = "heurístico, no oficial")
                KpiTile("Lluvia próx. 3 h", mm(s.rainForecast3hMm), Modifier.weight(1f),
                    sub = if (s.rainSource == "OPEN_METEO") "Open-Meteo" else "fuente no disponible",
                    accent = if (s.rainForecast3hMm == null) RiskUnknown else Color(0xFF1F2937))
            }

            SectionCard("Nivel · últimas ${state.history.size} lecturas", icon = Icons.Filled.LocationOn) {
                val ok = state.history.filter { it.quality != "SUSPECT" }
                LevelChart(
                    levels = ok.mapNotNull { it.levelCm },
                    warning = s.thresholds?.levelWarningCm,
                    critical = s.thresholds?.levelCriticalCm,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Legend("umbral alerta", RiskWarning); Legend("umbral crítico", RiskCritical)
                }
                val suspects = state.history.count { it.quality == "SUSPECT" }
                if (suspects > 0) Text("$suspects lectura(s) sospechosa(s) excluida(s) del cálculo",
                    color = TextSoft, fontSize = 12.sp)
            }

            SatelliteSummary(state, onOpenSatellite)

            SectionCard("Fuentes y modelo", icon = Icons.Filled.Info) {
                SourceRow("Sensor ultrasónico", "${s.stationId} · ${s.sourceMode} · medido ${localTime(s.observedAt)}")
                SourceRow("Estado de datos", dataStatusLabel(s.dataStatus))
                SourceRow("Clima", if (s.rainSource == "OPEN_METEO") "Open-Meteo (caché 30 min)" else "UNAVAILABLE")
                SourceRow("Satélite", "${s.satellite?.source ?: "—"} · ${s.satellite?.status ?: "UNAVAILABLE"}")
                SourceRow("Modelo", "${s.modelType} · pronóstico heurístico no calibrado")
            }
            Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun Header(state: UiState) {
    val s = state.station
    Column(
        Modifier.fillMaxWidth().background(HeaderGradient)
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Alerta Río · Inundaciones", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                Text(s?.name ?: "Estación RIO-01", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("${s?.stationId ?: "RIO-01"} · ${s?.lat ?: "—"}, ${s?.lon ?: "—"}",
                    color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
            // Bandera DEMO grande (plan H7–H8): no confundir con estación real.
            val mode = if (state.mockMode) "MOCK" else (s?.sourceMode ?: state.backendMode ?: "DEMO")
            Pill(mode, if (mode == "LIVE") RiskNormal else Color(0xFFF97316))
        }
    }
}

@Composable
private fun ConnectionBanner(state: UiState) {
    when (state.connection) {
        Connection.CONNECTING -> Banner("Conectando con ${state.baseUrl}…", TextSoft)
        Connection.OFFLINE -> Banner(
            "Sin conexión con el backend · mostrando último dato (${localTime(state.lastSuccessAt)}) · reintentando cada 5 s",
            RiskCritical)
        Connection.ONLINE -> when (state.station?.dataStatus) {
            "STALE" -> Banner("Estación sin lecturas recientes · último dato ${ago(state.station?.observedAt)}", RiskWarning)
            "SUSPECT_READING" -> Banner("Última lectura sospechosa descartada (posible eco o basura flotante)", RiskWarning)
            "INSUFFICIENT_HISTORY" -> Banner("Acumulando historial: se necesitan 3 lecturas para la tendencia", TextSoft)
            else -> Unit
        }
    }
}

@Composable
private fun RiskCard(state: UiState, onAck: (Int) -> Unit, onOpenAlerts: () -> Unit) {
    val s = state.station ?: return
    val color = riskColor(s.riskLevel)
    SectionCard("Riesgo estimado", icon = Icons.Filled.Notifications,
        trailing = { Pill(riskLabel(s.riskLevel), color) }) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${s.riskScore ?: "—"}", fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(" / 100", fontSize = 16.sp, color = TextSoft, modifier = Modifier.padding(bottom = 8.dp))
        }
        LinearProgressIndicator(
            progress = { ((s.riskScore ?: 0) / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = color, trackColor = color.copy(alpha = 0.15f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
        Spacer(Modifier.height(8.dp))
        Text("Observado ${localDateTime(s.observedAt)} · ${dataStatusLabel(s.dataStatus)}", color = TextSoft, fontSize = 12.sp)

        val alertId = s.alertId
        if (alertId != null) {
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier.fillMaxWidth().background(color.copy(alpha = 0.08f), RoundedCornerShape(12.dp)).padding(12.dp)
            ) {
                Text("Alerta #$alertId · ${alertStatusLabel(s.alertStatus)}", fontWeight = FontWeight.Bold, color = color)
                if (s.alertStatus == "PENDING") {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { onAck(alertId) },
                        enabled = state.ackInProgress == null && state.connection == Connection.ONLINE,
                        colors = ButtonDefaults.buttonColors(containerColor = RiskCritical),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.ackInProgress == alertId) CircularProgressIndicator(
                            Modifier.height(18.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Confirmar recepción", fontWeight = FontWeight.Bold)
                    }
                    if (state.connection != Connection.ONLINE) Text("Sin conexión: no se puede confirmar",
                        color = RiskCritical, fontSize = 12.sp)
                } else {
                    Text("Ver detalle en Alertas", color = TextSoft, fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }
        } else if (s.riskLevel == "CRITICAL") {
            Spacer(Modifier.height(8.dp))
            Text("Confirmando tendencia antes de emitir alerta (antirrebote)…", color = TextSoft, fontSize = 12.sp)
        }
        Spacer(Modifier.height(4.dp))
        androidx.compose.material3.TextButton(onClick = onOpenAlerts) { Text("Historial de alertas") }
    }
}

@Composable
private fun SatelliteSummary(state: UiState, onOpenSatellite: () -> Unit) {
    val sat = state.station?.satellite
    SectionCard("Satélite · NASA FIRMS", icon = Icons.Filled.Star,
        trailing = { Pill(sat?.status ?: "UNAVAILABLE", if (sat?.status == "AVAILABLE") RiskNormal else RiskUnknown, filled = false) }) {
        if (sat != null && sat.status == "AVAILABLE") {
            SourceRow("Detecciones térmicas", "${sat.hotspotsCount ?: 0} en ±0.5° (${state.firms?.dayRange ?: 2} días)")
            SourceRow("Último paso con detección", if (sat.observedAt != null) "${localDateTime(sat.observedAt)} (${ago(sat.observedAt)})" else "sin detecciones")
            SourceRow("FRP máx.", sat.maxFrpMw?.let { "%.1f MW".format(it) } ?: "—")
        } else {
            Text("Sin evidencia satelital reciente", fontWeight = FontWeight.Medium)
            Text(sat?.reason ?: "Fuente no disponible", color = TextSoft, fontSize = 12.sp)
        }
        Text("Extensión inundada: ${sat?.floodExtentKm2?.let { "%.1f km²".format(it) } ?: "no medida (FIRMS no mide agua)"}",
            color = TextSoft, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        androidx.compose.material3.TextButton(onClick = onOpenSatellite) {
            androidx.compose.material3.Icon(Icons.Filled.Place, null); Text(" Ver detecciones")
        }
    }
}

@Composable
private fun Legend(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        com.reto01.flood.ui.components.Dot(color); Text(" $text", fontSize = 12.sp, color = TextSoft)
    }
}

@Composable
fun SourceRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = TextSoft, fontSize = 13.sp, modifier = Modifier.weight(0.45f))
        Text(value, fontSize = 13.sp, modifier = Modifier.weight(0.55f))
    }
}
