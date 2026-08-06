package com.axelliant.hris.features.saleorders.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.features.saleorders.data.SaleOrdersRepository
import com.axelliant.hris.features.saleorders.domain.model.SaleOrderDetailModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ViewSaleOrderViewModel @Inject constructor(
    private val repository: SaleOrdersRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val orderId: String = savedStateHandle.get<String>(ARG_SALE_ORDER_ID).orEmpty()

    private val _detailState = MutableStateFlow<UiState<SaleOrderDetailModel>>(UiState.Idle)
    val detailState = _detailState.asStateFlow()

    private val _submitState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val submitState = _submitState.asStateFlow()

    init {
        loadDetail()
    }

    fun loadDetail() {
        if (orderId.isBlank()) {
            _detailState.value = UiState.Error("Sale order not found.")
            return
        }
        viewModelScope.launch {
            _detailState.value = UiState.Loading
            _detailState.value = when (val result = repository.getSaleOrderDetail(orderId)) {
                is ApiResult.Success -> UiState.Success(result.data)
                is ApiResult.Empty -> UiState.Error("Sale order not found.")
                is ApiResult.HttpError -> UiState.Error(result.message)
                is ApiResult.NetworkError -> UiState.Error(result.message)
                is ApiResult.UnknownError -> UiState.Error(result.message)
                is ApiResult.Unauthorized -> UiState.Unauthorized
            }
        }
    }

    fun submitOrder() {
        if (orderId.isBlank() || _submitState.value is UiState.Loading) return
        viewModelScope.launch {
            _submitState.value = UiState.Loading
            _submitState.value = when (val result = repository.submitSaleOrder(orderId)) {
                is ApiResult.Success -> UiState.Success(result.data.message)
                is ApiResult.Empty -> UiState.Error("Unable to submit sale order.")
                is ApiResult.HttpError -> UiState.Error(result.message)
                is ApiResult.NetworkError -> UiState.Error(result.message)
                is ApiResult.UnknownError -> UiState.Error(result.message)
                ApiResult.Unauthorized -> UiState.Unauthorized
            }
        }
    }

    fun onSubmitHandled() {
        _submitState.value = UiState.Idle
    }

    companion object {
        const val ARG_SALE_ORDER_ID = "saleOrderId"
    }
}
