package com.reto01.flood.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reto01.flood.AlertNotifier
import com.reto01.flood.data.AlertDto
import com.reto01.flood.data.ApiResult
import com.reto01.flood.data.FirmsDto
import com.reto01.flood.data.FloodRepository
import com.reto01.flood.data.FloodStationDto
import com.reto01.flood.data.ReadingDto
import com.reto01.flood.data.Settings
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

enum class Connection { CONNECTING, ONLINE, OFFLINE }

data class UiState(
    val station: FloodStationDto? = null,
    val history: List<ReadingDto> = emptyList(),
    val alerts: List<AlertDto> = emptyList(),
    val firms: FirmsDto? = null,
    val connection: Connection = Connection.CONNECTING,
    val lastSuccessAt: Instant? = null,
    val lastError: String? = null,
    val ackInProgress: Int? = null,
    val message: String? = null,
    val baseUrl: String = "",
    val operator: String = "",
    val mockMode: Boolean = false,
    val backendMode: String? = null,
)

class FloodViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = Settings(app)
    private val repo = FloodRepository(settings.baseUrl, settings.mockMode)
    private val notifier = AlertNotifier(app)

    private val _state = MutableStateFlow(
        UiState(baseUrl = settings.baseUrl, operator = settings.operator, mockMode = settings.mockMode)
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var tick = 0

    /** Bucle de refresco cada 5 s; se ejecuta solo mientras la pantalla está visible. */
    suspend fun pollLoop() {
        while (true) {
            refresh()
            delay(5_000)
        }
    }

    fun refreshNow() = viewModelScope.launch { refresh() }

    private suspend fun refresh() {
        val st = viewModelScope.async { repo.station() }
        val hi = viewModelScope.async { repo.history(30) }
        val al = viewModelScope.async { repo.alerts() }
        val stationRes = st.await()
        val historyRes = hi.await()
        val alertsRes = al.await()
        // FIRMS cambia cada horas: se consulta cada ~60 s.
        val firmsRes = if (tick++ % 12 == 0 || _state.value.firms == null) repo.firms() else null

        when (stationRes) {
            is ApiResult.Ok -> _state.update {
                it.copy(station = stationRes.value, connection = Connection.ONLINE,
                    lastSuccessAt = Instant.now(), lastError = null)
            }
            is ApiResult.HttpError -> _state.update {
                it.copy(connection = Connection.OFFLINE, lastError = "HTTP ${stationRes.code}: ${stationRes.message}")
            }
            is ApiResult.NetworkError -> _state.update {
                it.copy(connection = Connection.OFFLINE, lastError = stationRes.message)
            }
        }
        if (historyRes is ApiResult.Ok) _state.update { it.copy(history = historyRes.value.items.orEmpty()) }
        if (alertsRes is ApiResult.Ok) {
            val items = alertsRes.value.items.orEmpty()
            _state.update { it.copy(alerts = items) }
            maybeNotify(items)
        }
        if (firmsRes is ApiResult.Ok) _state.update { it.copy(firms = firmsRes.value) }
    }

    private fun maybeNotify(alerts: List<AlertDto>) {
        val newest = alerts.filter { it.status == "PENDING" }.maxByOrNull { it.id ?: -1 } ?: return
        val id = newest.id ?: return
        if (id > settings.lastNotifiedAlertId) {
            settings.lastNotifiedAlertId = id
            notifier.notifyAlert(newest, _state.value.station?.name ?: "RIO-01")
        }
    }

    fun acknowledge(alertId: Int) {
        if (_state.value.ackInProgress != null) return
        viewModelScope.launch {
            _state.update { it.copy(ackInProgress = alertId, message = null) }
            val msg = when (val r = repo.ack(alertId, settings.operator)) {
                is ApiResult.Ok -> "✅ Alerta #$alertId confirmada por ${r.value.acknowledgedBy} (${r.value.acknowledgedAt})"
                is ApiResult.HttpError -> when (r.code) {
                    409 -> "Alerta #$alertId ya confirmada o resuelta: ${r.message}"
                    404 -> "Alerta #$alertId no existe en el servidor"
                    else -> "Error ${r.code}: ${r.message}"
                }
                // Nunca se muestra éxito sin respuesta 200 del servidor.
                is ApiResult.NetworkError -> "❌ Sin conexión: la confirmación NO se registró. Reintente."
            }
            _state.update { it.copy(ackInProgress = null, message = msg) }
            refresh()
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun checkHealth() = viewModelScope.launch {
        val msg = when (val r = repo.health()) {
            is ApiResult.Ok -> "Backend OK · modo ${r.value.mode}".also {
                _state.update { s -> s.copy(backendMode = r.value.mode) }
            }
            is ApiResult.HttpError -> "Backend respondió ${r.code}: ${r.message}"
            is ApiResult.NetworkError -> "No se alcanza ${repo.baseUrl}: ${r.message}"
        }
        _state.update { it.copy(message = msg) }
    }

    fun saveSettings(baseUrl: String, operator: String, mock: Boolean) {
        val url = baseUrl.trim().let { if (it.endsWith("/")) it else "$it/" }
        settings.baseUrl = url
        settings.operator = operator.trim().ifEmpty { "equipo-android" }
        settings.mockMode = mock
        repo.updateBaseUrl(url)
        repo.mockMode = mock
        _state.update {
            it.copy(baseUrl = url, operator = settings.operator, mockMode = mock,
                station = null, history = emptyList(), alerts = emptyList(), firms = null,
                connection = Connection.CONNECTING, message = "Ajustes guardados")
        }
        tick = 0
        refreshNow()
    }
}
