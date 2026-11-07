package com.alertspot.ui.fires

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alertspot.data.model.FireOverview
import com.alertspot.data.repository.FireRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive

import com.alertspot.data.model.PhysicalNode

sealed class FireUiState {
    data object Loading : FireUiState()
    data class Success(val overview: FireOverview, val healthOk: Boolean) : FireUiState()
    data class Error(val message: String) : FireUiState()
}

sealed class PhysicalUiState {
    data object Loading : PhysicalUiState()
    data class Success(val nodes: List<PhysicalNode>) : PhysicalUiState()
    data object Empty : PhysicalUiState()
    data class Error(val message: String) : PhysicalUiState()
    data class Stale(val nodes: List<PhysicalNode>) : PhysicalUiState()
}

class FireViewModel : ViewModel() {
    private val repository = FireRepository()

    private val _uiState = MutableStateFlow<FireUiState>(FireUiState.Loading)
    val uiState: StateFlow<FireUiState> = _uiState.asStateFlow()

    private val _physicalUiState = MutableStateFlow<PhysicalUiState>(PhysicalUiState.Loading)
    val physicalUiState: StateFlow<PhysicalUiState> = _physicalUiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        startPolling()
    }

    fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                refreshAll()
                delay(15000)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
    }

    fun refreshAll() {
        fetchOverview()
        fetchPhysicalNodes()
    }

    private fun fetchPhysicalNodes() {
        viewModelScope.launch {
            // No cambiamos el estado a Loading si ya tenemos datos, 
            // a menos que este sea el primer fetch o explícitamente queramos Loading.
            // Para ser discretos, solo ponemos loading al principio.
            if (_physicalUiState.value !is PhysicalUiState.Success && _physicalUiState.value !is PhysicalUiState.Stale) {
                _physicalUiState.value = PhysicalUiState.Loading
            }
            
            repository.getPhysicalNodes().fold(
                onSuccess = { nodes -> 
                    if (nodes.isEmpty()) {
                        _physicalUiState.value = PhysicalUiState.Empty
                    } else {
                        // Comprobar si está desactualizado (Stale)
                        // Para simplificar, consideramos "Stale" si la fecha es muy vieja, pero como no sabemos la fecha actual del servidor,
                        // el requirement dice "Stale, si la lectura está desactualizada".
                        // Por el momento, simplemente pasamos a Success, el UI o la lógica de negocio puede decidir si es stale.
                        // O podemos poner Stale si el request falla pero teníamos datos viejos.
                        _physicalUiState.value = PhysicalUiState.Success(nodes)
                    }
                },
                onFailure = { error ->
                    val currentState = _physicalUiState.value
                    if (currentState is PhysicalUiState.Success) {
                        _physicalUiState.value = PhysicalUiState.Stale(currentState.nodes)
                    } else if (currentState is PhysicalUiState.Stale) {
                        // Mantener stale
                    } else {
                        _physicalUiState.value = PhysicalUiState.Error(error.message ?: "Error al cargar nodos físicos")
                    }
                }
            )
        }
    }

    fun fetchOverview() {
        viewModelScope.launch {
            _uiState.value = FireUiState.Loading
            val healthResult = repository.getHealth()
            val isHealthOk = healthResult.isSuccess
            
            repository.getOverview().fold(
                onSuccess = { _uiState.value = FireUiState.Success(it, isHealthOk) },
                onFailure = { _uiState.value = FireUiState.Error(it.message ?: "Error de red al conectar al backend real") }
            )
        }
    }
}