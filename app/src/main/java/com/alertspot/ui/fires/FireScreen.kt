package com.alertspot.ui.fires

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alertspot.data.model.FireOverview
import com.alertspot.ui.theme.*

@Composable
fun FireScreen(
    viewModel: FireViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val physicalUiState by viewModel.physicalUiState.collectAsState()

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("SAT Bolivia — Incendios") },
                actions = {
                    IconButton(onClick = { viewModel.refreshAll() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is FireUiState.Loading -> CircularProgressIndicator()
                is FireUiState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.refreshAll() }) {
                            Text("Reintentar")
                        }
                    }
                }
                is FireUiState.Success -> {
                    FireOverviewContent(state.overview, state.healthOk, physicalUiState)
                }
            }
        }
    }
}

@Composable
fun FireOverviewContent(overview: FireOverview, healthOk: Boolean, physicalUiState: PhysicalUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (overview.riskLevel) {
                    "NORMAL" -> BlueLight
                    "WARNING" -> Orange
                    "CRITICAL" -> Red
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Región: ${overview.region}",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Riesgo: ${overview.riskScore} (${overview.riskLevel})",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Temp: ${overview.temperatureC}°C")
            Text("Humedad: ${overview.humidityPct}%")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Viento: ${overview.windKmh} km/h")
            Text("Humo: ${overview.smokeIndex}")
        }

        Text("Focos activos: ${overview.activeHotspots}")
        Text("Actualizado: ${overview.updatedAt}")

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (overview.sourceMode == "DEMO") {
                Badge(containerColor = MaterialTheme.colorScheme.tertiary) {
                    Text("MODO DEMO", modifier = Modifier.padding(8.dp))
                }
            }
            if (healthOk) {
                Badge(containerColor = androidx.compose.ui.graphics.Color(0xFF34C759)) {
                    Text("API REAL CONECTADA", modifier = Modifier.padding(8.dp))
                }
            } else {
                Badge(containerColor = MaterialTheme.colorScheme.error) {
                    Text("ERROR API REAL", modifier = Modifier.padding(8.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        when (physicalUiState) {
            is PhysicalUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            is PhysicalUiState.Empty -> {
                Text(
                    text = "No hay sensores físicos conectados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is PhysicalUiState.Error -> {
                Text(
                    text = "Error sensor: ${physicalUiState.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            is PhysicalUiState.Success -> {
                val node = physicalUiState.nodes.firstOrNull { it.id == "I-01-PHYSICAL" } ?: physicalUiState.nodes.firstOrNull()
                node?.let { PhysicalNodeCard(it) }
            }
            is PhysicalUiState.Stale -> {
                val node = physicalUiState.nodes.firstOrNull { it.id == "I-01-PHYSICAL" } ?: physicalUiState.nodes.firstOrNull()
                node?.let { PhysicalNodeCard(it, isStale = true) }
            }
        }
    }
}

@Composable
fun PhysicalNodeCard(node: com.alertspot.data.model.PhysicalNode, isStale: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "SENSOR FÍSICO EXPERIMENTAL — ${node.id.replace("-PHYSICAL", "")}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            node.temperatureCEstimated?.let { temp ->
                Text(
                    text = "Temperatura estimada: $temp °C",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Text(
                text = "Valor ADC: ${node.adcRaw}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Calidad: ${node.quality}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Calibración: ${if (node.calibrated) "Sí" else "No"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Procedencia: ${node.sourceMode}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Actualizado: ${node.updatedAt}",
                style = MaterialTheme.typography.bodySmall
            )
            
            if (isStale) {
                Text(
                    text = "(Desactualizado)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            if (!node.calibrated) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Temperatura estimada — sensor no calibrado",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}