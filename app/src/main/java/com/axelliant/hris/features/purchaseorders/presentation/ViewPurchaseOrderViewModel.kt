package com.axelliant.hris.features.purchaseorders.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.features.purchaseorders.data.PurchaseOrdersRepository
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderDetailModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ViewPurchaseOrderViewModel @Inject constructor(
    private val repository: PurchaseOrdersRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val orderId: String = savedStateHandle.get<String>(ARG_PURCHASE_ORDER_ID).orEmpty()

    private val _detailState = MutableStateFlow<UiState<PurchaseOrderDetailModel>>(UiState.Idle)
    val detailState = _detailState.asStateFlow()

    init {
        loadDetail()
    }

    fun loadDetail() {
        if (orderId.isBlank()) {
            _detailState.value = UiState.Error("Purchase order not found.")
            return
        }
        viewModelScope.launch {
            _detailState.value = UiState.Loading
            _detailState.value = when (val result = repository.getPurchaseOrderDetail(orderId)) {
                is ApiResult.Success -> UiState.Success(result.data)
                is ApiResult.Empty -> UiState.Error("Purchase order not found.")
                is ApiResult.HttpError -> UiState.Error(result.message)
                is ApiResult.NetworkError -> UiState.Error(result.message)
                is ApiResult.UnknownError -> UiState.Error(result.message)
                is ApiResult.Unauthorized -> UiState.Unauthorized
            }
        }
    }

    companion object {
        const val ARG_PURCHASE_ORDER_ID = "purchaseOrderId"
    }
}
