package com.reto01.flood.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reto01.flood.ui.UiState
import com.reto01.flood.ui.components.SectionCard
import com.reto01.flood.ui.theme.TextMain
import com.reto01.flood.ui.theme.TextSoft

@Composable
fun SettingsScreen(state: UiState, onSave: (String, String, Boolean) -> Unit, onHealth: () -> Unit) {
    var url by remember(state.baseUrl) { mutableStateOf(state.baseUrl) }
    var op by remember(state.operator) { mutableStateOf(state.operator) }
    var mock by remember(state.mockMode) { mutableStateOf(state.mockMode) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Ajustes", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextMain)
        SectionCard("Conexión con el backend", icon = Icons.Filled.Settings) {
            OutlinedTextField(url, { url = it }, label = { Text("URL base") }, singleLine = true,
                modifier = Modifier.fillMaxWidth())
            Text("Emulador: http://10.0.2.2:5000/ · Teléfono: http://IP_DE_TU_PC:5000/ (misma Wi-Fi)",
                color = TextSoft, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(op, { op = it }, label = { Text("Operador (aparece en el ACK)") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Modo MOCK (sin servidor)", fontWeight = FontWeight.Medium)
                    Text("Usa el JSON del contrato para diseñar la UI. Se marca como MOCK.",
                        color = TextSoft, fontSize = 12.sp)
                }
                Switch(mock, { mock = it })
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onSave(url, op, mock) }) { Text("Guardar") }
                OutlinedButton(onClick = onHealth) { Text("Probar /health") }
            }
        }
        SectionCard("Acerca del prototipo", icon = Icons.Filled.Build) {
            Text("Estación y sensor simulados (DEMO). Pronóstico heurístico HEURISTIC_TREND_V1 no " +
                "calibrado; umbrales ilustrativos, no oficiales. Lluvia: Open-Meteo cuando está " +
                "disponible. Satélite: NASA FIRMS con fecha y disponibilidad. Los tokens (Telegram, " +
                "MAP_KEY) viven solo en el servidor.", color = TextSoft, fontSize = 13.sp)
        }
    }
}
