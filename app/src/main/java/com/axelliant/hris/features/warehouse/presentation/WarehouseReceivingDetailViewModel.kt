package com.axelliant.hris.features.warehouse.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.features.purchaseorders.data.PurchaseOrdersRepository
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderProductLine
import com.axelliant.hris.features.warehouse.data.WarehouseRepository
import com.axelliant.hris.features.warehouse.domain.model.WarehouseReceiptDetailModel
import com.axelliant.hris.features.warehouse.domain.model.WarehouseReceiptLineModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class WarehouseReceivingDetailViewModel @Inject constructor(
    private val warehouseRepository: WarehouseRepository,
    private val purchaseOrdersRepository: PurchaseOrdersRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val receiptId: String = savedStateHandle.get<String>(ARG_RECEIPT_ID).orEmpty()

    private val _detailState =
        MutableStateFlow<UiState<WarehouseReceiptDetailModel>>(UiState.Idle)
    val detailState = _detailState.asStateFlow()

    init {
        loadReceipt()
    }

    fun loadReceipt() {
        if (receiptId.isBlank()) {
            _detailState.value = UiState.Error("Receipt id is missing.")
            return
        }
        viewModelScope.launch {
            _detailState.value = UiState.Loading
            _detailState.value = when (
                val result = warehouseRepository.getWarehouseReceiptDetail(receiptId)
            ) {
                is ApiResult.Success -> UiState.Success(enrichWithPurchaseOrder(result.data))
                ApiResult.Empty -> UiState.Error("Receipt not found.")
                is ApiResult.HttpError -> UiState.Error(result.message)
                is ApiResult.NetworkError -> UiState.Error(result.message)
                is ApiResult.UnknownError -> UiState.Error(result.message)
                ApiResult.Unauthorized -> UiState.Unauthorized
            }
        }
    }

    private suspend fun enrichWithPurchaseOrder(
        detail: WarehouseReceiptDetailModel
    ): WarehouseReceiptDetailModel {
        val purchaseOrderId = detail.receipt.purchaseOrderId.takeIf { it.isNotBlank() }
            ?: return detail
        val purchaseOrder = when (
            val result = purchaseOrdersRepository.getPurchaseOrderDetail(purchaseOrderId)
        ) {
            is ApiResult.Success -> result.data
            else -> return detail
        }
        val byProductId = purchaseOrder.products
            .filter { it.productId.isNotBlank() }
            .associateBy { it.productId }
        val byName = purchaseOrder.products.associateBy { it.name.normalizeKey() }
        return detail.copy(
            lines = detail.lines.map { line ->
                val purchaseOrderLine = byProductId[line.productId]
                    ?: byName[line.productName.normalizeKey()]
                line.withPurchaseOrderLine(purchaseOrderLine)
            }
        )
    }

    private fun WarehouseReceiptLineModel.withPurchaseOrderLine(
        purchaseOrderLine: PurchaseOrderProductLine?
    ): WarehouseReceiptLineModel {
        if (purchaseOrderLine == null) return this
        return copy(
            productCode = purchaseOrderLine.partNumber.ifBlank { productCode },
            uom = purchaseOrderLine.uom.ifBlank { uom }
        )
    }

    private fun String.normalizeKey(): String = trim().lowercase()

    companion object {
        const val ARG_RECEIPT_ID = "receiptId"
    }
}
