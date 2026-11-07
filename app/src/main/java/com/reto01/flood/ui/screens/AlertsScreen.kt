package com.reto01.flood.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reto01.flood.data.AlertDto
import com.reto01.flood.ui.Connection
import com.reto01.flood.ui.UiState
import com.reto01.flood.ui.alertStatusLabel
import com.reto01.flood.ui.cm
import com.reto01.flood.ui.components.Banner
import com.reto01.flood.ui.components.Pill
import com.reto01.flood.ui.components.SectionCard
import com.reto01.flood.ui.localDateTime
import com.reto01.flood.ui.localTime
import com.reto01.flood.ui.theme.RiskCritical
import com.reto01.flood.ui.theme.RiskNormal
import com.reto01.flood.ui.theme.RiskUnknown
import com.reto01.flood.ui.theme.TextMain
import com.reto01.flood.ui.theme.TextSoft
import com.reto01.flood.ui.theme.riskColor

@Composable
fun AlertsScreen(state: UiState, onAck: (Int) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text("Alertas", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Text("La confirmación registra recepción humana (actor y hora) en el servidor. " +
                "No certifica que la inundación sea real.", color = TextSoft, fontSize = 12.sp)
            if (state.connection == Connection.OFFLINE) {
                Spacer(Modifier.height(8.dp))
                Banner("Sin conexión: la lista puede estar desactualizada y no se puede confirmar", RiskCritical)
            }
        }
        if (state.alerts.isEmpty()) {
            item {
                SectionCard("Sin alertas", icon = Icons.Filled.CheckCircle) {
                    Text("No hay alertas registradas para RIO-01.", color = TextSoft)
                }
            }
        }
        items(state.alerts, key = { it.id ?: 0 }) { a -> AlertCard(a, state, onAck) }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun AlertCard(a: AlertDto, state: UiState, onAck: (Int) -> Unit) {
    val statusColor = when (a.status) {
        "PENDING" -> RiskCritical
        "ACKNOWLEDGED" -> RiskNormal
        else -> RiskUnknown
    }
    SectionCard(
        "Alerta #${a.id} · ${a.stationId}",
        icon = Icons.Filled.Warning,
        trailing = { Pill(alertStatusLabel(a.status), statusColor) },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill(a.riskLevel ?: "—", riskColor(a.riskLevel), filled = false)
            Text("score ${a.riskScore ?: "—"} · nivel ${cm(a.levelCm)}", fontSize = 13.sp)
        }
        Spacer(Modifier.height(6.dp))
        SourceRow("Emitida", localDateTime(a.createdAt))
        if (a.acknowledgedAt != null) {
            SourceRow("Confirmada por", a.acknowledgedBy ?: "—")
            SourceRow("Hora de confirmación", localTime(a.acknowledgedAt))
        }
        if (a.resolvedAt != null) SourceRow("Resuelta", localDateTime(a.resolvedAt))

        val id = a.id
        if (a.status == "PENDING" && id != null) {
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { onAck(id) },
                enabled = state.ackInProgress == null && state.connection == Connection.ONLINE,
                colors = ButtonDefaults.buttonColors(containerColor = RiskCritical),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.ackInProgress == id) {
                    CircularProgressIndicator(Modifier.height(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.CheckCircle, null)
                    Text("  Confirmar recepción", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
