package com.axelliant.hris.features.warehouse.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.features.warehouse.data.WarehouseRepository
import com.axelliant.hris.features.warehouse.domain.model.PurchaseOrderOptionModel
import com.axelliant.hris.features.warehouse.domain.model.WarehouseOptionModel
import com.axelliant.hris.features.warehouse.domain.model.WarehouseReceiptModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@HiltViewModel
class WarehouseReceivingViewModel @Inject constructor(
    private val repository: WarehouseRepository
) : ViewModel() {

    private val _receivingState = MutableStateFlow<UiState<WarehouseReceivingUiModel>>(UiState.Idle)
    val receivingState = _receivingState.asStateFlow()

    private val _warehouseOptionsState =
        MutableStateFlow<UiState<List<WarehouseOptionModel>>>(UiState.Idle)
    val warehouseOptionsState = _warehouseOptionsState.asStateFlow()

    private val _purchaseOrderOptionsState =
        MutableStateFlow<UiState<List<PurchaseOrderOptionModel>>>(UiState.Idle)
    val purchaseOrderOptionsState = _purchaseOrderOptionsState.asStateFlow()

    private val searchInput = MutableStateFlow("")
    private val loadedReceipts = mutableListOf<WarehouseReceiptModel>()
    private var filters = WarehouseReceivingFilters()
    private var totalCount = 0
    private var nextStart = 0
    private var isPageLoading = false
    private var loadJob: Job? = null

    init {
        observeSearchQuery()
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        searchInput
            .drop(1)
            .debounce(SEARCH_DEBOUNCE_MS)
            .distinctUntilChanged()
            .onEach { query ->
                val trimmed = query.trim()
                if (trimmed == filters.search) return@onEach
                filters = filters.copy(search = trimmed)
                loadReceipts()
            }
            .launchIn(viewModelScope)
    }

    fun loadReceiptsIfNeeded() {
        if (_receivingState.value !is UiState.Idle) return
        loadReceipts()
    }

    fun loadReceipts() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isPageLoading = false
            loadedReceipts.clear()
            totalCount = 0
            nextStart = 0
            _receivingState.value = UiState.Loading
            loadPage(start = 0, append = false)
        }
    }

    fun loadNextPage() {
        if (
            isPageLoading ||
            loadedReceipts.isEmpty() ||
            loadedReceipts.size >= totalCount ||
            _receivingState.value is UiState.Loading
        ) {
            return
        }
        loadJob = viewModelScope.launch {
            _receivingState.value = UiState.Success(currentUiModel(isLoadingNextPage = true))
            loadPage(start = nextStart, append = true)
        }
    }

    fun onSearchChanged(query: String) {
        searchInput.value = query
    }

    fun applyFilters(newFilters: WarehouseReceivingFilters) {
        filters = newFilters
        if (searchInput.value != newFilters.search) {
            searchInput.value = newFilters.search
        }
        loadReceipts()
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

    fun loadPurchaseOrderOptionsIfNeeded() {
        if (_purchaseOrderOptionsState.value is UiState.Success ||
            _purchaseOrderOptionsState.value is UiState.Loading
        ) return
        viewModelScope.launch {
            _purchaseOrderOptionsState.value = UiState.Loading
            _purchaseOrderOptionsState.value = when (val result = repository.getPurchaseOrderDdl()) {
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
        _receivingState.value = when (
            val result = repository.getWarehouseReceipts(
                start = start,
                limit = WarehouseRepository.PAGE_SIZE,
                search = filters.search,
                warehouseId = filters.warehouseId,
                purchaseOrderId = filters.purchaseOrderId,
                receiptType = filters.receiptType,
                receiptStatus = filters.receiptStatus
            )
        ) {
            is ApiResult.Success -> {
                if (!append) loadedReceipts.clear()
                totalCount = result.data.totalCount
                loadedReceipts.addAll(result.data.receipts)
                nextStart = loadedReceipts.size
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

    private fun errorOrKeep(append: Boolean, message: String): UiState<WarehouseReceivingUiModel> {
        return if (append && loadedReceipts.isNotEmpty()) {
            UiState.Success(currentUiModel(isLoadingNextPage = false))
        } else {
            UiState.Error(message.ifBlank { "Unable to load warehouse receipts." })
        }
    }

    private fun currentUiModel(isLoadingNextPage: Boolean): WarehouseReceivingUiModel {
        return WarehouseReceivingUiModel(
            receipts = loadedReceipts.toList(),
            totalCount = totalCount,
            filters = filters,
            isLoadingNextPage = isLoadingNextPage
        )
    }

    companion object {
        const val SEARCH_DEBOUNCE_MS = 400L
    }
}

data class WarehouseReceivingUiModel(
    val receipts: List<WarehouseReceiptModel>,
    val totalCount: Int,
    val filters: WarehouseReceivingFilters,
    val isLoadingNextPage: Boolean
)

data class WarehouseReceivingFilters(
    val search: String = "",
    val warehouseId: String? = null,
    val warehouseLabel: String? = null,
    val purchaseOrderId: String? = null,
    val purchaseOrderLabel: String? = null,
    val receiptType: Int? = null,
    val receiptTypeLabel: String? = null,
    val receiptStatus: Int? = null,
    val receiptStatusLabel: String? = null
)
