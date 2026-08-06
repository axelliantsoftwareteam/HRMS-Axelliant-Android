package com.axelliant.hris.features.saleorders.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.R
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.features.saleorders.data.SaleOrdersRepository
import com.axelliant.hris.features.saleorders.domain.model.SaleOrderFilterChip
import com.axelliant.hris.features.saleorders.domain.model.SaleOrderListUiModel
import com.axelliant.hris.features.saleorders.domain.model.SaleOrderModel
import com.axelliant.hris.features.saleorders.domain.model.SaleOrderStatusFilterType
import com.axelliant.hris.features.saleorders.domain.model.SaleOrdersEmptyStateUi
import com.axelliant.hris.features.quotes.domain.model.QuoteWorkflowUiModel
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
class SaleOrdersViewModel @Inject constructor(
    private val repository: SaleOrdersRepository
) : ViewModel() {

    private val _ordersState = MutableStateFlow<UiState<SaleOrderListUiModel>>(UiState.Idle)
    val ordersState = _ordersState.asStateFlow()

    private val _workflowState = MutableStateFlow<UiState<QuoteWorkflowUiModel>>(UiState.Idle)
    val workflowState = _workflowState.asStateFlow()

    private val searchInput = MutableStateFlow("")
    private var activeSearchQuery = ""
    private var selectedFilterType = SaleOrderStatusFilterType.ALL
    private val loadedOrders = mutableListOf<SaleOrderModel>()
    private var filterChips = emptyList<SaleOrderFilterChip>()
    private var listTotalCount = 0
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

    fun onFilterChipSelected(chip: SaleOrderFilterChip) {
        if (selectedFilterType == chip.filterType) return
        selectedFilterType = chip.filterType
        clearSearchQuery()
        loadOrders()
    }

    fun getSelectedFilterId(): String = selectedFilterType.filterId

    fun loadOrdersIfNeeded() {
        if (_ordersState.value !is UiState.Idle) return
        loadOrders()
    }

    fun loadOrders() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isPageLoading = false
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
            val result = repository.getSaleOrders(
                start = start,
                limit = SaleOrdersRepository.PAGE_SIZE,
                search = activeSearchQuery,
                statusFilter = selectedFilterType
            )
        ) {
            is ApiResult.Success -> {
                val page = result.data
                listTotalCount = page.totalCount
                filterChips = SaleOrderStatusFilterType.entries.map { type ->
                    SaleOrderFilterChip(
                        count = page.statusCounts[type] ?: 0,
                        filterType = type
                    )
                }
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

    private fun errorOrKeep(append: Boolean, message: String): UiState<SaleOrderListUiModel> {
        return if (append && loadedOrders.isNotEmpty()) {
            UiState.Success(currentUiModel(isLoadingNextPage = false))
        } else {
            UiState.Error(message.ifBlank { "Unable to load sale orders." })
        }
    }

    private fun currentUiModel(isLoadingNextPage: Boolean): SaleOrderListUiModel {
        val isEmpty = loadedOrders.isEmpty()
        return SaleOrderListUiModel(
            orders = loadedOrders.toList(),
            filterChips = filterChips,
            totalCount = listTotalCount,
            isLoadingNextPage = isLoadingNextPage,
            isLastPage = loadedOrders.size >= listTotalCount,
            emptyState = if (isEmpty) {
                SaleOrdersEmptyStateUi(
                    titleRes = R.string.sale_orders_empty_title,
                    descriptionRes = R.string.sale_orders_empty_description
                )
            } else {
                null
            }
        )
    }

    fun loadSaleOrderWorkflow(order: SaleOrderModel) {
        if (_workflowState.value is UiState.Loading) return

        viewModelScope.launch {
            _workflowState.value = UiState.Loading
            _workflowState.value = when (
                val result = repository.getSaleOrderWorkflow(
                    relationId = order.id,
                    orderNumber = order.orderNumber
                )
            ) {
                is ApiResult.Success -> UiState.Success(result.data)
                ApiResult.Empty -> UiState.Error(message = "Unable to load sale order workflow.")
                is ApiResult.HttpError -> UiState.Error(result.message)
                is ApiResult.NetworkError -> UiState.Error(result.message)
                is ApiResult.UnknownError -> UiState.Error(result.message)
                ApiResult.Unauthorized -> UiState.Unauthorized
            }
        }
    }

    fun resetWorkflowState() {
        _workflowState.value = UiState.Idle
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 400L
    }
}
