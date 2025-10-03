package com.costura.pro.ui.worker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.costura.pro.data.repository.OperationRepository
import com.costura.pro.data.repository.ProductionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class WorkerDashboardViewModel(
    private val operationRepository: OperationRepository,
    private val productionRepository: ProductionRepository,
    private val workerId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkerDashboardState())
    val uiState: StateFlow<WorkerDashboardState> = _uiState

    init {
        loadInitialData()
        observeOperations()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Sincronizar operaciones
            operationRepository.syncOperationsFromFirebase()

            // Cargar ganancias iniciales
            val totalEarnings = productionRepository.getTotalEarnings(workerId) ?: 0.0
            _uiState.value = _uiState.value.copy(totalEarnings = totalEarnings)
        }
    }

    private fun observeOperations() {
        viewModelScope.launch {
            operationRepository.getActiveOperations().collect { operations ->
                _uiState.value = _uiState.value.copy(
                    operations = operations,
                    isLoading = false
                )
            }
        }
    }

    fun refreshEarnings() {
        viewModelScope.launch {
            val totalEarnings = productionRepository.getTotalEarnings(workerId) ?: 0.0
            _uiState.value = _uiState.value.copy(totalEarnings = totalEarnings)
        }
    }

    fun registerProduction(operationId: String, operationName: String, paymentPerUnit: Double, quantity: Int) {
        viewModelScope.launch {
            val success = productionRepository.registerProduction(
                workerId = workerId,
                operationId = operationId,
                operationName = operationName,
                paymentPerUnit = paymentPerUnit,
                quantity = quantity
            )

            if (success) {
                // Actualizar ganancias después de registrar producción
                refreshEarnings()
            }
        }
    }
}

data class WorkerDashboardState(
    val operations: List<com.costura.pro.data.model.Operation> = emptyList(),
    val totalEarnings: Double = 0.0,
    val isLoading: Boolean = true
)