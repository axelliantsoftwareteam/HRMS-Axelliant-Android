package com.axelliant.hris.features.warehouse.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.features.warehouse.data.WarehouseRepository
import com.axelliant.hris.features.warehouse.domain.model.WarehouseModel
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
class WarehousesViewModel @Inject constructor(
    private val repository: WarehouseRepository
) : ViewModel() {

    private val _warehousesState = MutableStateFlow<UiState<WarehouseListUiModel>>(UiState.Idle)
    val warehousesState = _warehousesState.asStateFlow()

    private val searchInput = MutableStateFlow("")
    private val loadedWarehouses = mutableListOf<WarehouseModel>()
    private var activeSearchQuery = ""
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
                if (trimmed == activeSearchQuery) return@onEach
                activeSearchQuery = trimmed
                loadWarehouses()
            }
            .launchIn(viewModelScope)
    }

    fun loadWarehousesIfNeeded() {
        if (_warehousesState.value !is UiState.Idle) return
        loadWarehouses()
    }

    fun loadWarehouses() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isPageLoading = false
            loadedWarehouses.clear()
            totalCount = 0
            nextStart = 0
            _warehousesState.value = UiState.Loading
            loadPage(start = 0, append = false)
        }
    }

    fun loadNextPage() {
        if (
            isPageLoading ||
            loadedWarehouses.isEmpty() ||
            loadedWarehouses.size >= totalCount ||
            _warehousesState.value is UiState.Loading
        ) {
            return
        }

        loadJob = viewModelScope.launch {
            _warehousesState.value = UiState.Success(currentUiModel(isLoadingNextPage = true))
            loadPage(start = nextStart, append = true)
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchInput.value = query
    }

    fun submitSearch(query: String) {
        val trimmed = query.trim()
        activeSearchQuery = trimmed
        searchInput.value = trimmed
        loadWarehouses()
    }

    fun clearSearchQuery() {
        activeSearchQuery = ""
        searchInput.value = ""
    }

    private suspend fun loadPage(start: Int, append: Boolean) {
        isPageLoading = true
        _warehousesState.value = when (
            val result = repository.getWarehouses(
                start = start,
                limit = WarehouseRepository.PAGE_SIZE,
                search = activeSearchQuery
            )
        ) {
            is ApiResult.Success -> {
                if (!append) loadedWarehouses.clear()
                totalCount = result.data.totalCount
                loadedWarehouses.addAll(result.data.warehouses)
                nextStart = loadedWarehouses.size
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

    private fun errorOrKeep(append: Boolean, message: String): UiState<WarehouseListUiModel> {
        return if (append && loadedWarehouses.isNotEmpty()) {
            UiState.Success(currentUiModel(isLoadingNextPage = false))
        } else {
            UiState.Error(message.ifBlank { "Unable to load warehouses." })
        }
    }

    private fun currentUiModel(isLoadingNextPage: Boolean): WarehouseListUiModel {
        return WarehouseListUiModel(
            warehouses = loadedWarehouses.toList(),
            totalCount = totalCount,
            isLoadingNextPage = isLoadingNextPage,
            isLastPage = loadedWarehouses.size >= totalCount
        )
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 400L
    }
}

data class WarehouseListUiModel(
    val warehouses: List<WarehouseModel>,
    val totalCount: Int,
    val isLoadingNextPage: Boolean,
    val isLastPage: Boolean
)
