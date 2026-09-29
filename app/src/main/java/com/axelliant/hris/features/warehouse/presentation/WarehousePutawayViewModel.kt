package com.axelliant.hris.features.warehouse.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.features.warehouse.data.WarehouseRepository
import com.axelliant.hris.features.warehouse.domain.model.WarehouseLocationOptionModel
import com.axelliant.hris.features.warehouse.domain.model.WarehouseOptionModel
import com.axelliant.hris.features.warehouse.domain.model.WarehousePutawayModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class WarehousePutawayViewModel @Inject constructor(
    private val repository: WarehouseRepository
) : ViewModel() {

    private val _putawayState = MutableStateFlow<UiState<WarehousePutawayUiModel>>(UiState.Idle)
    val putawayState = _putawayState.asStateFlow()

    private val _warehouseOptionsState =
        MutableStateFlow<UiState<List<WarehouseOptionModel>>>(UiState.Idle)
    val warehouseOptionsState = _warehouseOptionsState.asStateFlow()

    private val _locationOptionsState =
        MutableStateFlow<UiState<List<WarehouseLocationOptionModel>>>(UiState.Idle)
    val locationOptionsState = _locationOptionsState.asStateFlow()

    private val loadedPutaways = mutableListOf<WarehousePutawayModel>()
    private var filters = WarehousePutawayFilters()
    private var totalCount = 0
    private var nextStart = 0
    private var isPageLoading = false
    private var loadJob: Job? = null
    private var selectedLocationWarehouseId: String? = null

    fun loadPutawaysIfNeeded() {
        if (_putawayState.value !is UiState.Idle) return
        loadPutaways()
    }

    fun loadPutaways() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isPageLoading = false
            loadedPutaways.clear()
            totalCount = 0
            nextStart = 0
            _putawayState.value = UiState.Loading
            loadPage(start = 0, append = false)
        }
    }

    fun loadNextPage() {
        if (
            isPageLoading ||
            loadedPutaways.isEmpty() ||
            loadedPutaways.size >= totalCount ||
            _putawayState.value is UiState.Loading
        ) {
            return
        }
        loadJob = viewModelScope.launch {
            _putawayState.value = UiState.Success(currentUiModel(isLoadingNextPage = true))
            loadPage(start = nextStart, append = true)
        }
    }

    fun applyFilters(newFilters: WarehousePutawayFilters) {
        filters = newFilters
        loadPutaways()
    }

    fun loadWarehouseOptionsIfNeeded() {
        if (_warehouseOptionsState.value is UiState.Success ||
            _warehouseOptionsState.value is UiState.Loading
        ) return
        viewModelScope.launch {
            _warehouseOptionsState.value = UiState.Loading
            _warehouseOptionsState.value = when (val result = repository.getWarehouseDdl()) {
                is ApiResult.Success -> UiState.Success(result.data)
                ApiResult.Empty -> UiState.Success(emptyList())
                is ApiResult.HttpError -> UiState.Error(result.message)
                is ApiResult.NetworkError -> UiState.Error(result.message)
                is ApiResult.UnknownError -> UiState.Error(result.message)
                ApiResult.Unauthorized -> UiState.Unauthorized
            }
        }
    }

    fun loadLocationOptionsForWarehouse(warehouseId: String) {
        if (warehouseId.isBlank()) {
            _locationOptionsState.value = UiState.Error(LOCATION_REQUIRES_WAREHOUSE)
            return
        }
        if (
            selectedLocationWarehouseId == warehouseId &&
            (_locationOptionsState.value is UiState.Success ||
                _locationOptionsState.value is UiState.Loading)
        ) return
        selectedLocationWarehouseId = warehouseId
        viewModelScope.launch {
            _locationOptionsState.value = UiState.Loading
            _locationOptionsState.value = when (val result = repository.getLocationDdl(warehouseId)) {
                is ApiResult.Success -> UiState.Success(result.data)
                ApiResult.Empty -> UiState.Success(emptyList())
                is ApiResult.HttpError -> UiState.Error(result.message)
                is ApiResult.NetworkError -> UiState.Error(result.message)
                is ApiResult.UnknownError -> UiState.Error(result.message)
                ApiResult.Unauthorized -> UiState.Unauthorized
            }
        }
    }

    private suspend fun loadPage(start: Int, append: Boolean) {
        isPageLoading = true
        _putawayState.value = when (
            val result = repository.getPutaways(
                start = start,
                limit = WarehouseRepository.PAGE_SIZE,
                warehouseId = filters.warehouseId,
                locationId = filters.locationId
            )
        ) {
            is ApiResult.Success -> {
                if (!append) loadedPutaways.clear()
                totalCount = result.data.totalCount
                loadedPutaways.addAll(result.data.putaways)
                nextStart = loadedPutaways.size
                UiState.Success(currentUiModel(isLoadingNextPage = false))
            }
            ApiResult.Empty -> UiState.Success(currentUiModel(isLoadingNextPage = false))
            is ApiResult.HttpError -> errorOrKeep(append, result.message)
            is ApiResult.NetworkError -> errorOrKeep(append, result.message)
            is ApiResult.UnknownError -> errorOrKeep(append, result.message)
            ApiResult.Unauthorized -> UiState.Unauthorized
        }
        isPageLoading = false
    }

    private fun errorOrKeep(append: Boolean, message: String): UiState<WarehousePutawayUiModel> {
        return if (append && loadedPutaways.isNotEmpty()) {
            UiState.Success(currentUiModel(isLoadingNextPage = false))
        } else {
            UiState.Error(message.ifBlank { "Unable to load putaway history." })
        }
    }

    private fun currentUiModel(isLoadingNextPage: Boolean): WarehousePutawayUiModel {
        return WarehousePutawayUiModel(
            putaways = loadedPutaways.toList(),
            totalCount = totalCount,
            filters = filters,
            isLoadingNextPage = isLoadingNextPage
        )
    }

    companion object {
        const val LOCATION_REQUIRES_WAREHOUSE = "location_requires_warehouse"
    }
}

data class WarehousePutawayUiModel(
    val putaways: List<WarehousePutawayModel>,
    val totalCount: Int,
    val filters: WarehousePutawayFilters,
    val isLoadingNextPage: Boolean
)

data class WarehousePutawayFilters(
    val warehouseId: String? = null,
    val warehouseLabel: String? = null,
    val locationId: String? = null,
    val locationLabel: String? = null
)
