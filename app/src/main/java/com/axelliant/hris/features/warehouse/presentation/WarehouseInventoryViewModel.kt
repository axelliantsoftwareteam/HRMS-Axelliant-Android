package com.axelliant.hris.features.warehouse.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.features.warehouse.data.WarehouseRepository
import com.axelliant.hris.features.warehouse.domain.model.InventoryModel
import com.axelliant.hris.features.warehouse.domain.model.InventoryTransactionModel
import com.axelliant.hris.features.warehouse.domain.model.PurchaseOrderOptionModel
import com.axelliant.hris.features.warehouse.domain.model.WarehouseLocationOptionModel
import com.axelliant.hris.features.warehouse.domain.model.WarehouseOptionModel
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
class WarehouseInventoryViewModel @Inject constructor(
    private val repository: WarehouseRepository
) : ViewModel() {

    private val _inventoryState = MutableStateFlow<UiState<WarehouseInventoryUiModel>>(UiState.Idle)
    val inventoryState = _inventoryState.asStateFlow()

    private val _warehouseOptionsState =
        MutableStateFlow<UiState<List<WarehouseOptionModel>>>(UiState.Idle)
    val warehouseOptionsState = _warehouseOptionsState.asStateFlow()

    private val _locationOptionsState =
        MutableStateFlow<UiState<List<WarehouseLocationOptionModel>>>(UiState.Idle)
    val locationOptionsState = _locationOptionsState.asStateFlow()

    private val _purchaseOrderOptionsState =
        MutableStateFlow<UiState<List<PurchaseOrderOptionModel>>>(UiState.Idle)
    val purchaseOrderOptionsState = _purchaseOrderOptionsState.asStateFlow()

    private val _transactionsState =
        MutableStateFlow<UiState<InventoryTransactionsUiModel>>(UiState.Idle)
    val transactionsState = _transactionsState.asStateFlow()

    private val searchInput = MutableStateFlow("")
    private val loadedItems = mutableListOf<InventoryModel>()
    private var filters = WarehouseInventoryFilters()
    private var totalCount = 0
    private var nextStart = 0
    private var isPageLoading = false
    private var loadJob: Job? = null
    private var selectedWarehouseIdForLocations: String? = null

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
                loadInventory()
            }
            .launchIn(viewModelScope)
    }

    fun loadInventoryIfNeeded() {
        if (_inventoryState.value !is UiState.Idle) return
        loadInventory()
    }

    fun loadInventory() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isPageLoading = false
            loadedItems.clear()
            totalCount = 0
            nextStart = 0
            _inventoryState.value = UiState.Loading
            loadPage(start = 0, append = false)
        }
    }

    fun loadNextPage() {
        if (
            isPageLoading ||
            loadedItems.isEmpty() ||
            loadedItems.size >= totalCount ||
            _inventoryState.value is UiState.Loading
        ) {
            return
        }
        loadJob = viewModelScope.launch {
            _inventoryState.value = UiState.Success(currentUiModel(isLoadingNextPage = true))
            loadPage(start = nextStart, append = true)
        }
    }

    fun onSearchChanged(query: String) {
        searchInput.value = query
    }

    fun applyFilters(newFilters: WarehouseInventoryFilters) {
        val warehouseChanged = filters.warehouseId != newFilters.warehouseId
        filters = newFilters
        if (warehouseChanged) {
            selectedWarehouseIdForLocations = null
            _locationOptionsState.value = UiState.Idle
        }
        if (searchInput.value != newFilters.search) {
            searchInput.value = newFilters.search
        }
        loadInventory()
    }

    fun selectWarehouse(option: WarehouseOptionModel?) {
        filters = filters.copy(
            warehouseId = option?.id,
            warehouseLabel = option?.selectorLabel(),
            locationId = null,
            locationLabel = null
        )
        selectedWarehouseIdForLocations = null
        _locationOptionsState.value = UiState.Idle
        loadInventory()
    }

    fun selectLocation(option: WarehouseLocationOptionModel?) {
        filters = filters.copy(
            locationId = option?.id,
            locationLabel = option?.selectorLabel()
        )
        loadInventory()
    }

    fun selectPurchaseOrder(option: PurchaseOrderOptionModel?) {
        filters = filters.copy(
            purchaseOrderId = option?.id,
            purchaseOrderLabel = option?.number?.takeIf { it.isNotBlank() && it != "-" }
        )
        loadInventory()
    }

    fun selectOwnership(option: InventoryStaticFilterOption?) {
        filters = filters.copy(ownershipType = option?.value, ownershipLabel = option?.label)
        loadInventory()
    }

    fun selectPurpose(option: InventoryStaticFilterOption?) {
        filters = filters.copy(purpose = option?.value, purposeLabel = option?.label)
        loadInventory()
    }

    fun selectStatus(option: InventoryStaticFilterOption?) {
        filters = filters.copy(inventoryStatus = option?.value, statusLabel = option?.label)
        loadInventory()
    }

    fun setHideDepleted(hide: Boolean) {
        filters = filters.copy(includeDepleted = !hide, hideDepleted = hide)
        loadInventory()
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

    fun loadLocationOptionsIfNeeded() {
        val warehouseId = filters.warehouseId.orEmpty()
        loadLocationOptionsForWarehouse(warehouseId)
    }

    fun areLocationOptionsLoadedForWarehouse(warehouseId: String): Boolean {
        return selectedWarehouseIdForLocations == warehouseId &&
            _locationOptionsState.value is UiState.Success
    }

    fun loadLocationOptionsForWarehouse(warehouseId: String) {
        if (warehouseId.isBlank()) {
            _locationOptionsState.value = UiState.Idle
            _locationOptionsState.value = UiState.Error(LOCATION_REQUIRES_WAREHOUSE)
            return
        }
        if (selectedWarehouseIdForLocations == warehouseId &&
            _locationOptionsState.value is UiState.Success
        ) return
        if (_locationOptionsState.value is UiState.Loading) return
        viewModelScope.launch {
            _locationOptionsState.value = UiState.Loading
            _locationOptionsState.value = when (val result = repository.getLocationDdl(warehouseId)) {
                is ApiResult.Success -> {
                    selectedWarehouseIdForLocations = warehouseId
                    UiState.Success(result.data)
                }
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

    fun loadTransactions(inventory: InventoryModel) {
        viewModelScope.launch {
            _transactionsState.value = UiState.Loading
            _transactionsState.value = when (
                val result = repository.getInventoryTransactions(inventory.id)
            ) {
                is ApiResult.Success -> UiState.Success(
                    InventoryTransactionsUiModel(
                        inventory = inventory,
                        transactions = result.data.transactions,
                        totalCount = result.data.totalCount
                    )
                )
                ApiResult.Empty -> UiState.Success(
                    InventoryTransactionsUiModel(
                        inventory = inventory,
                        transactions = emptyList(),
                        totalCount = 0
                    )
                )
                is ApiResult.HttpError -> UiState.Error(result.message)
                is ApiResult.NetworkError -> UiState.Error(result.message)
                is ApiResult.UnknownError -> UiState.Error(result.message)
                ApiResult.Unauthorized -> UiState.Unauthorized
            }
        }
    }

    private suspend fun loadPage(start: Int, append: Boolean) {
        isPageLoading = true
        _inventoryState.value = when (
            val result = repository.getInventory(
                start = start,
                limit = WarehouseRepository.PAGE_SIZE,
                search = filters.search,
                warehouseId = filters.warehouseId,
                locationId = filters.locationId,
                purchaseOrderId = filters.purchaseOrderId,
                ownershipType = filters.ownershipType,
                purpose = filters.purpose,
                inventoryStatus = filters.inventoryStatus,
                includeDepleted = filters.includeDepleted
            )
        ) {
            is ApiResult.Success -> {
                if (!append) loadedItems.clear()
                totalCount = result.data.totalCount
                loadedItems.addAll(result.data.items)
                nextStart = loadedItems.size
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

    private fun errorOrKeep(append: Boolean, message: String): UiState<WarehouseInventoryUiModel> {
        return if (append && loadedItems.isNotEmpty()) {
            UiState.Success(currentUiModel(isLoadingNextPage = false))
        } else {
            UiState.Error(message.ifBlank { "Unable to load inventory." })
        }
    }

    private fun currentUiModel(isLoadingNextPage: Boolean): WarehouseInventoryUiModel {
        return WarehouseInventoryUiModel(
            items = loadedItems.toList(),
            totalCount = totalCount,
            filters = filters,
            isLoadingNextPage = isLoadingNextPage
        )
    }

    private fun WarehouseOptionModel.selectorLabel(): String {
        val nameValue = name.takeIf { it.isNotBlank() && it != "-" }
        val codeValue = code.takeIf { it.isNotBlank() && it != "-" }
        return listOfNotNull(nameValue, codeValue?.let { "($it)" }).joinToString(" ")
            .ifBlank { "-" }
    }

    private fun WarehouseLocationOptionModel.selectorLabel(): String {
        val nameValue = name.takeIf { it.isNotBlank() && it != "-" }
        val pathValue = path.takeIf { it.isNotBlank() && it != "-" && it != nameValue }
        return listOfNotNull(nameValue, pathValue).joinToString(" - ")
            .ifBlank { "-" }
    }

    companion object {
        const val LOCATION_REQUIRES_WAREHOUSE = "location-requires-warehouse"
        const val SEARCH_DEBOUNCE_MS = 400L
    }
}

data class WarehouseInventoryUiModel(
    val items: List<InventoryModel>,
    val totalCount: Int,
    val filters: WarehouseInventoryFilters,
    val isLoadingNextPage: Boolean
)

data class InventoryTransactionsUiModel(
    val inventory: InventoryModel,
    val transactions: List<InventoryTransactionModel>,
    val totalCount: Int
)

data class WarehouseInventoryFilters(
    val search: String = "",
    val warehouseId: String? = null,
    val warehouseLabel: String? = null,
    val locationId: String? = null,
    val locationLabel: String? = null,
    val purchaseOrderId: String? = null,
    val purchaseOrderLabel: String? = null,
    val ownershipType: Int? = null,
    val ownershipLabel: String? = null,
    val purpose: Int? = null,
    val purposeLabel: String? = null,
    val inventoryStatus: Int? = null,
    val statusLabel: String? = null,
    val includeDepleted: Boolean = true,
    val hideDepleted: Boolean = false
)

data class InventoryStaticFilterOption(
    val value: Int,
    val label: String
)
