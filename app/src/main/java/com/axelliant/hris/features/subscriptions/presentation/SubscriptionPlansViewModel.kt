package com.axelliant.hris.features.subscriptions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.features.subscriptions.data.SubscriptionsRepository
import com.axelliant.hris.features.subscriptions.domain.model.SubscriptionPlanListUiModel
import com.axelliant.hris.features.subscriptions.domain.model.SubscriptionPlanModel
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
class SubscriptionPlansViewModel @Inject constructor(
    private val repository: SubscriptionsRepository
) : ViewModel() {
    private val _plansState = MutableStateFlow<UiState<SubscriptionPlanListUiModel>>(UiState.Idle)
    val plansState = _plansState.asStateFlow()

    private val searchInput = MutableStateFlow("")
    private val loadedPlans = mutableListOf<SubscriptionPlanModel>()
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
                loadPlans()
            }
            .launchIn(viewModelScope)
    }

    fun loadPlansIfNeeded() {
        if (_plansState.value !is UiState.Idle) return
        loadPlans()
    }

    fun onSearchQueryChanged(query: String) {
        searchInput.value = query
    }

    fun clearSearchQuery() {
        activeSearchQuery = ""
        searchInput.value = ""
        loadPlans()
    }

    fun loadPlans() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isPageLoading = false
            loadedPlans.clear()
            totalCount = 0
            nextStart = 0
            _plansState.value = UiState.Loading
            loadPage(start = 0, append = false)
        }
    }

    fun loadNextPage() {
        if (
            isPageLoading ||
            loadedPlans.isEmpty() ||
            loadedPlans.size >= totalCount ||
            _plansState.value is UiState.Loading
        ) {
            return
        }

        isPageLoading = true
        loadJob = viewModelScope.launch {
            _plansState.value = UiState.Success(currentUiModel(isLoadingNextPage = true))
            loadPage(start = nextStart, append = true)
        }
    }

    private suspend fun loadPage(start: Int, append: Boolean) {
        isPageLoading = true
        _plansState.value = when (
            val result = repository.getSubscriptionPlans(
                start = start,
                limit = SubscriptionsRepository.PAGE_SIZE,
                search = activeSearchQuery
            )
        ) {
            is ApiResult.Success -> {
                val page = result.data
                totalCount = page.totalCount
                if (!append) loadedPlans.clear()
                loadedPlans.addAll(page.plans)
                nextStart = start + SubscriptionsRepository.PAGE_SIZE
                UiState.Success(currentUiModel(isLoadingNextPage = false))
            }
            ApiResult.Empty -> {
                if (!append) {
                    loadedPlans.clear()
                    totalCount = 0
                }
                UiState.Success(currentUiModel(isLoadingNextPage = false))
            }
            is ApiResult.HttpError -> errorOrKeep(append, result.message)
            is ApiResult.NetworkError -> errorOrKeep(append, result.message)
            is ApiResult.UnknownError -> errorOrKeep(append, result.message)
            ApiResult.Unauthorized -> UiState.Unauthorized
        }
        isPageLoading = false
    }

    private fun errorOrKeep(append: Boolean, message: String): UiState<SubscriptionPlanListUiModel> {
        return if (append && loadedPlans.isNotEmpty()) {
            UiState.Success(currentUiModel(isLoadingNextPage = false))
        } else {
            UiState.Error(message.ifBlank { "Unable to load plans." })
        }
    }

    private fun currentUiModel(isLoadingNextPage: Boolean): SubscriptionPlanListUiModel {
        return SubscriptionPlanListUiModel(
            plans = loadedPlans.toList(),
            totalCount = totalCount,
            isLoadingNextPage = isLoadingNextPage,
            isLastPage = loadedPlans.size >= totalCount
        )
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 400L
    }
}
