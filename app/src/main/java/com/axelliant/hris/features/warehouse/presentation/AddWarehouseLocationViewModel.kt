package com.axelliant.hris.features.warehouse.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.features.warehouse.data.WarehouseRepository
import com.axelliant.hris.features.warehouse.data.remote.dto.AddWarehouseLocationRequest
import com.axelliant.hris.features.warehouse.domain.model.WarehouseLocationOptionModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AddWarehouseLocationViewModel @Inject constructor(
    private val repository: WarehouseRepository
) : ViewModel() {

    private val _parentLocationsState =
        MutableStateFlow<UiState<List<WarehouseLocationOptionModel>>>(UiState.Idle)
    val parentLocationsState = _parentLocationsState.asStateFlow()

    private val _saveState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val saveState = _saveState.asStateFlow()

    fun loadParentLocations(warehouseId: String) {
        if (warehouseId.isBlank() || _parentLocationsState.value is UiState.Loading) return
        viewModelScope.launch {
            _parentLocationsState.value = UiState.Loading
            _parentLocationsState.value = when (val result = repository.getLocationDdl(warehouseId)) {
                is ApiResult.Success -> UiState.Success(result.data)
                ApiResult.Empty -> UiState.Success(emptyList())
                is ApiResult.HttpError -> UiState.Error(result.message)
                is ApiResult.NetworkError -> UiState.Error(result.message)
                is ApiResult.UnknownError -> UiState.Error(result.message)
                ApiResult.Unauthorized -> UiState.Unauthorized
            }
        }
    }

    fun saveLocation(input: AddWarehouseLocationInput) {
        if (_saveState.value is UiState.Loading) return

        val warehouseId = input.warehouseId.trim()
        val code = input.code.trim()
        val name = input.name.trim()
        when {
            warehouseId.isBlank() -> {
                _saveState.value = UiState.Error(VALIDATION_WAREHOUSE)
                return
            }
            code.isBlank() -> {
                _saveState.value = UiState.Error(VALIDATION_CODE)
                return
            }
            name.isBlank() -> {
                _saveState.value = UiState.Error(VALIDATION_NAME)
                return
            }
        }

        viewModelScope.launch {
            _saveState.value = UiState.Loading
            _saveState.value = when (
                val result = repository.addLocation(
                    AddWarehouseLocationRequest(
                        id = input.id?.trim()?.takeIf { it.isNotBlank() },
                        warehouseId = warehouseId,
                        parentId = input.parentId?.trim()?.takeIf { it.isNotBlank() },
                        levelType = input.levelType,
                        code = code,
                        name = name,
                        isActive = input.isActive
                    )
                )
            ) {
                is ApiResult.Success -> UiState.Success(result.data.id)
                ApiResult.Empty -> UiState.Error("Unable to save location.")
                is ApiResult.HttpError -> UiState.Error(result.message)
                is ApiResult.NetworkError -> UiState.Error(result.message)
                is ApiResult.UnknownError -> UiState.Error(result.message)
                ApiResult.Unauthorized -> UiState.Unauthorized
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = UiState.Idle
    }

    companion object {
        const val ARG_MODE = "locationMode"
        const val ARG_WAREHOUSE_ID = "warehouseId"
        const val ARG_WAREHOUSE_NAME = "warehouseName"
        const val ARG_WAREHOUSE_CODE = "warehouseCode"
        const val ARG_LOCATION_ID = "locationId"
        const val ARG_LOCATION_CODE = "locationCode"
        const val ARG_LOCATION_NAME = "locationName"
        const val ARG_LOCATION_LEVEL_TYPE = "locationLevelType"
        const val ARG_LOCATION_PARENT_ID = "locationParentId"
        const val ARG_LOCATION_IS_ACTIVE = "locationIsActive"
        const val MODE_ADD = "add"
        const val MODE_EDIT = "edit"
        const val MODE_VIEW = "view"
        const val RESULT_LOCATION_SAVED = "locationSaved"
        const val VALIDATION_WAREHOUSE = "validation-location-warehouse"
        const val VALIDATION_CODE = "validation-location-code"
        const val VALIDATION_NAME = "validation-location-name"
    }
}

data class AddWarehouseLocationInput(
    val id: String? = null,
    val warehouseId: String,
    val parentId: String?,
    val levelType: Int,
    val code: String,
    val name: String,
    val isActive: Boolean
)

enum class WarehouseLocationLevel(
    val apiValue: Int,
    val label: String
) {
    Area(1, "Area"),
    Zone(2, "Zone"),
    Location(3, "Location"),
    Rack(4, "Rack"),
    Bin(5, "Bin");

    companion object {
        fun from(value: Int): WarehouseLocationLevel {
            return entries.firstOrNull { it.apiValue == value } ?: Area
        }
    }
}
