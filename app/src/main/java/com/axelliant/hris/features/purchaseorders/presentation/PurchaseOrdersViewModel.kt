package com.axelliant.hris.features.purchaseorders.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.R
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.features.purchaseorders.data.PurchaseOrdersRepository
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderHistoryItemUiModel
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderListUiModel
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderModel
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderUtilization
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderUtilizationFilterChip
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrdersEmptyStateUi
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
class PurchaseOrdersViewModel @Inject constructor(
    private val repository: PurchaseOrdersRepository
) : ViewModel() {

    private val _ordersState = MutableStateFlow<UiState<PurchaseOrderListUiModel>>(UiState.Idle)
    val ordersState = _ordersState.asStateFlow()

    private val _historyState =
        MutableStateFlow<UiState<List<PurchaseOrderHistoryItemUiModel>>>(UiState.Idle)
    val historyState = _historyState.asStateFlow()

    private val searchInput = MutableStateFlow("")
    private var activeSearchQuery = ""
    private val loadedOrders = mutableListOf<PurchaseOrderModel>()
    private var listTotalCount = 0
    private var nextStart = 0
    private var isPageLoading = false
    private var loadJob: Job? = null
    private var selectedUtilization: PurchaseOrderUtilization? = null

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
                if (trimmed == activeSearchQuery) return@onEach
                activeSearchQuery = trimmed
                loadOrders()
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        searchInput.value = query
    }

    fun submitSearch(query: String) {
        val trimmed = query.trim()
        activeSearchQuery = trimmed
        searchInput.value = trimmed
        loadOrders()
    }

    fun clearSearchQuery() {
        activeSearchQuery = ""
        searchInput.value = ""
    }

    fun selectUtilizationFilter(chip: PurchaseOrderUtilizationFilterChip) {
        if (selectedUtilization == chip.utilization) return
        selectedUtilization = chip.utilization
        _ordersState.value = UiState.Success(currentUiModel(isLoadingNextPage = false))
    }

    fun loadOrdersIfNeeded() {
        if (_ordersState.value !is UiState.Idle) return
        loadOrders()
    }

    fun loadPurchaseOrderHistory(order: PurchaseOrderModel) {
        viewModelScope.launch {
            _historyState.value = UiState.Loading
            _historyState.value = when (
                val result = repository.getPurchaseOrderHistory(order.id)
            ) {
                is ApiResult.Success -> {
                    if (result.data.isEmpty()) UiState.Empty else UiState.Success(result.data)
                }
                ApiResult.Empty -> UiState.Empty
                is ApiResult.HttpError -> UiState.Error(result.message)
                is ApiResult.NetworkError -> UiState.Error(result.message)
                is ApiResult.UnknownError -> UiState.Error(result.message)
                ApiResult.Unauthorized -> UiState.Unauthorized
            }
        }
    }

    fun resetHistoryState() {
        _historyState.value = UiState.Idle
    }

    fun loadOrders() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isPageLoading = false
            selectedUtilization = null
            loadedOrders.clear()
            listTotalCount = 0
            nextStart = 0
            _ordersState.value = UiState.Loading
            loadPage(start = 0, append = false)
        }
    }

    fun loadNextPage() {
        if (
            isPageLoading ||
            loadedOrders.isEmpty() ||
            loadedOrders.size >= listTotalCount ||
            _ordersState.value is UiState.Loading
        ) {
            return
        }

        loadJob = viewModelScope.launch {
            _ordersState.value = UiState.Success(currentUiModel(isLoadingNextPage = true))
            loadPage(start = nextStart, append = true)
        }
    }

    private suspend fun loadPage(start: Int, append: Boolean) {
        isPageLoading = true
        _ordersState.value = when (
            val result = repository.getPurchaseOrders(
                start = start,
                limit = PurchaseOrdersRepository.PAGE_SIZE,
                search = activeSearchQuery
            )
        ) {
            is ApiResult.Success -> {
                val page = result.data
                listTotalCount = page.totalCount
                if (!append) {
                    loadedOrders.clear()
                }
                loadedOrders.addAll(page.orders)
                nextStart = loadedOrders.size
                UiState.Success(currentUiModel(isLoadingNextPage = false))
            }
            is ApiResult.Empty -> {
                if (append && loadedOrders.isNotEmpty()) {
                    UiState.Success(currentUiModel(isLoadingNextPage = false))
                } else {
                    listTotalCount = 0
                    loadedOrders.clear()
                    UiState.Success(currentUiModel(isLoadingNextPage = false))
                }
            }
            is ApiResult.HttpError -> errorOrKeep(append, result.message)
            is ApiResult.NetworkError -> errorOrKeep(append, result.message)
            is ApiResult.UnknownError -> errorOrKeep(append, result.message)
            is ApiResult.Unauthorized -> UiState.Unauthorized
        }
        isPageLoading = false
    }

    private fun errorOrKeep(append: Boolean, message: String): UiState<PurchaseOrderListUiModel> {
        return if (append && loadedOrders.isNotEmpty()) {
            UiState.Success(currentUiModel(isLoadingNextPage = false))
        } else {
            UiState.Error(message.ifBlank { "Unable to load purchase orders." })
        }
    }

    private fun currentUiModel(isLoadingNextPage: Boolean): PurchaseOrderListUiModel {
        val displayedOrders = selectedUtilization?.let { utilization ->
            loadedOrders.filter { it.utilization == utilization }
        } ?: loadedOrders.toList()
        val isEmpty = displayedOrders.isEmpty()
        return PurchaseOrderListUiModel(
            orders = displayedOrders,
            filterChips = buildFilterChips(),
            selectedFilterId = selectedUtilization?.name
                ?: PurchaseOrderFilterChipAdapter.FILTER_ALL_ID,
            totalCount = listTotalCount,
            isLoadingNextPage = isLoadingNextPage,
            isLastPage = loadedOrders.size >= listTotalCount,
            emptyState = if (isEmpty) {
                PurchaseOrdersEmptyStateUi(
                    titleRes = R.string.purchase_orders_empty_title,
                    descriptionRes = R.string.purchase_orders_empty_description
                )
            } else {
                null
            }
        )
    }

    private fun buildFilterChips(): List<PurchaseOrderUtilizationFilterChip> {
        val orders = loadedOrders.toList()
        val chips = mutableListOf(
            PurchaseOrderUtilizationFilterChip(
                id = PurchaseOrderFilterChipAdapter.FILTER_ALL_ID,
                labelRes = R.string.purchase_orders_filter_all,
                utilization = null,
                count = listTotalCount.coerceAtLeast(orders.size)
            )
        )
        val utilizationOrder = listOf(
            PurchaseOrderUtilization.PENDING,
            PurchaseOrderUtilization.PARTIALLY_UTILIZED,
            PurchaseOrderUtilization.FULLY_UTILIZED
        )
        utilizationOrder.forEach { utilization ->
            val count = orders.count { it.utilization == utilization }
            if (count > 0) {
                chips += PurchaseOrderUtilizationFilterChip(
                    id = utilization.name,
                    labelRes = utilization.toLabelRes(),
                    utilization = utilization,
                    count = count
                )
            }
        }
        return chips
    }

    private fun PurchaseOrderUtilization.toLabelRes(): Int {
        return when (this) {
            PurchaseOrderUtilization.PENDING -> R.string.purchase_orders_filter_pending
            PurchaseOrderUtilization.PARTIALLY_UTILIZED ->
                R.string.purchase_orders_filter_partially_utilized
            PurchaseOrderUtilization.FULLY_UTILIZED ->
                R.string.purchase_orders_filter_fully_utilized
        }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 400L
    }
}
