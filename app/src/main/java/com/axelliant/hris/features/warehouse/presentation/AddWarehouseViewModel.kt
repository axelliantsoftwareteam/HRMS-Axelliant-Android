package com.axelliant.hris.features.warehouse.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.features.warehouse.data.WarehouseRepository
import com.axelliant.hris.features.warehouse.data.remote.dto.AddWarehouseRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AddWarehouseViewModel @Inject constructor(
    private val repository: WarehouseRepository
) : ViewModel() {

    private val _saveState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val saveState = _saveState.asStateFlow()

    fun saveWarehouse(input: AddWarehouseInput) {
        if (_saveState.value is UiState.Loading) return

        val code = input.code.trim()
        val name = input.name.trim()
        if (code.isBlank()) {
            _saveState.value = UiState.Error(VALIDATION_CODE)
            return
        }
        if (name.isBlank()) {
            _saveState.value = UiState.Error(VALIDATION_NAME)
            return
        }

        viewModelScope.launch {
            _saveState.value = UiState.Loading
            _saveState.value = when (
                val result = repository.addWarehouse(
                    AddWarehouseRequest(
                        id = input.id?.trim()?.takeIf { it.isNotBlank() },
                        code = code,
                        name = name,
                        addressLine1 = input.addressLine1.trim(),
                        addressLine2 = input.addressLine2.trim(),
                        city = input.city.trim(),
                        state = input.state.trim(),
                        postalCode = input.postalCode.trim(),
                        country = input.country.trim(),
                        isActive = input.isActive
                    )
                )
            ) {
                is ApiResult.Success -> UiState.Success(result.data.id)
                ApiResult.Empty -> UiState.Error("Unable to save warehouse.")
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
        const val ARG_MODE = "warehouseMode"
        const val ARG_WAREHOUSE_ID = "warehouseId"
        const val ARG_WAREHOUSE_CODE = "warehouseCode"
        const val ARG_WAREHOUSE_NAME = "warehouseName"
        const val ARG_WAREHOUSE_ADDRESS_LINE_1 = "warehouseAddressLine1"
        const val ARG_WAREHOUSE_ADDRESS_LINE_2 = "warehouseAddressLine2"
        const val ARG_WAREHOUSE_CITY = "warehouseCity"
        const val ARG_WAREHOUSE_STATE = "warehouseState"
        const val ARG_WAREHOUSE_POSTAL_CODE = "warehousePostalCode"
        const val ARG_WAREHOUSE_COUNTRY = "warehouseCountry"
        const val ARG_WAREHOUSE_IS_ACTIVE = "warehouseIsActive"
        const val MODE_ADD = "add"
        const val MODE_EDIT = "edit"
        const val MODE_VIEW = "view"
        const val RESULT_WAREHOUSE_CREATED = "warehouseCreated"
        const val VALIDATION_CODE = "validation-warehouse-code"
        const val VALIDATION_NAME = "validation-warehouse-name"
    }
}

data class AddWarehouseInput(
    val id: String? = null,
    val code: String,
    val name: String,
    val addressLine1: String,
    val addressLine2: String,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String,
    val isActive: Boolean
)
