package com.axelliant.hris.features.warehouse.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.features.warehouse.data.WarehouseRepository
import com.axelliant.hris.features.warehouse.domain.model.WarehouseLocationModel
import com.axelliant.hris.features.warehouse.domain.model.WarehouseModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class WarehouseLocationsViewModel @Inject constructor(
    private val repository: WarehouseRepository
) : ViewModel() {

    private val _locationsState =
        MutableStateFlow<UiState<WarehouseLocationsUiModel>>(UiState.Idle)
    val locationsState = _locationsState.asStateFlow()

    private var warehouseOptions = emptyList<WarehouseModel>()
    private var selectedWarehouse: WarehouseModel? = null

    fun loadIfNeeded() {
        if (_locationsState.value !is UiState.Idle) return
        loadWarehousesAndLocations()
    }

    fun refresh() {
        selectedWarehouse?.let { loadLocations(it) } ?: loadWarehousesAndLocations()
    }

    fun selectWarehouse(warehouse: WarehouseModel) {
        if (warehouse.id == selectedWarehouse?.id) return
        selectedWarehouse = warehouse
        loadLocations(warehouse)
    }

    private fun loadWarehousesAndLocations() {
        viewModelScope.launch {
            _locationsState.value = UiState.Loading
            when (
                val result = repository.getWarehouses(
                    start = 0,
                    limit = WAREHOUSE_SELECTOR_LIMIT
                )
            ) {
                is ApiResult.Success -> {
                    warehouseOptions = result.data.warehouses
                    val warehouse = warehouseOptions.firstOrNull()
                    selectedWarehouse = warehouse
                    if (warehouse == null) {
                        _locationsState.value = UiState.Empty
                    } else {
                        loadLocationsInternal(warehouse)
                    }
                }
                ApiResult.Empty -> _locationsState.value = UiState.Empty
                is ApiResult.HttpError -> showError(result.message)
                is ApiResult.NetworkError -> showError(result.message)
                is ApiResult.UnknownError -> showError(result.message)
                ApiResult.Unauthorized -> _locationsState.value = UiState.Unauthorized
            }
        }
    }

    private fun loadLocations(warehouse: WarehouseModel) {
        viewModelScope.launch {
            _locationsState.value = UiState.Loading
            loadLocationsInternal(warehouse)
        }
    }

    private suspend fun loadLocationsInternal(warehouse: WarehouseModel) {
        _locationsState.value = when (val result = repository.getLocationTree(warehouse.id)) {
            is ApiResult.Success -> {
                UiState.Success(
                    WarehouseLocationsUiModel(
                        warehouseOptions = warehouseOptions,
                        selectedWarehouse = warehouse,
                        locations = result.data
                    )
                )
            }
            ApiResult.Empty -> UiState.Success(
                WarehouseLocationsUiModel(
                    warehouseOptions = warehouseOptions,
                    selectedWarehouse = warehouse,
                    locations = emptyList()
                )
            )
            is ApiResult.HttpError -> UiState.Error(result.message)
            is ApiResult.NetworkError -> UiState.Error(result.message)
            is ApiResult.UnknownError -> UiState.Error(result.message)
            ApiResult.Unauthorized -> UiState.Unauthorized
        }
    }

    private fun showError(message: String) {
        _locationsState.value = UiState.Error(message.ifBlank { "Unable to load locations." })
    }

    private companion object {
        const val WAREHOUSE_SELECTOR_LIMIT = 100
    }
}

data class WarehouseLocationsUiModel(
    val warehouseOptions: List<WarehouseModel>,
    val selectedWarehouse: WarehouseModel,
    val locations: List<WarehouseLocationModel>
)
