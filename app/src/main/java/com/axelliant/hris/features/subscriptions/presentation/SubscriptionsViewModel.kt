package com.axelliant.hris.features.subscriptions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.features.subscriptions.data.SubscriptionsRepository
import com.axelliant.hris.features.subscriptions.domain.model.SubscriptionListUiModel
import com.axelliant.hris.features.subscriptions.domain.model.SubscriptionModel
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
class SubscriptionsViewModel @Inject constructor(
    private val repository: SubscriptionsRepository
) : ViewModel() {
    private val _subscriptionsState =
        MutableStateFlow<UiState<SubscriptionListUiModel>>(UiState.Idle)
    val subscriptionsState = _subscriptionsState.asStateFlow()

    private val searchInput = MutableStateFlow("")
    private val loadedSubscriptions = mutableListOf<SubscriptionModel>()
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
                loadSubscriptions()
            }
            .launchIn(viewModelScope)
    }

    fun loadSubscriptionsIfNeeded() {
        if (_subscriptionsState.value !is UiState.Idle) return
        loadSubscriptions()
    }

    fun onSearchQueryChanged(query: String) {
        searchInput.value = query
    }

    fun clearSearchQuery() {
        activeSearchQuery = ""
        searchInput.value = ""
        loadSubscriptions()
    }

    fun loadSubscriptions() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isPageLoading = false
            loadedSubscriptions.clear()
            totalCount = 0
            nextStart = 0
            _subscriptionsState.value = UiState.Loading
            loadPage(start = 0, append = false)
        }
    }

    fun loadNextPage() {
        if (
            isPageLoading ||
            loadedSubscriptions.isEmpty() ||
            loadedSubscriptions.size >= totalCount ||
            _subscriptionsState.value is UiState.Loading
        ) {
            return
        }

        isPageLoading = true
        loadJob = viewModelScope.launch {
            _subscriptionsState.value = UiState.Success(currentUiModel(isLoadingNextPage = true))
            loadPage(start = nextStart, append = true)
        }
    }

    private suspend fun loadPage(start: Int, append: Boolean) {
        isPageLoading = true
        _subscriptionsState.value = when (
            val result = repository.getSubscriptions(
                start = start,
                limit = SubscriptionsRepository.PAGE_SIZE,
                search = activeSearchQuery
            )
        ) {
            is ApiResult.Success -> {
                val page = result.data
                totalCount = page.totalCount
                if (!append) loadedSubscriptions.clear()
                loadedSubscriptions.addAll(page.subscriptions)
                nextStart = start + SubscriptionsRepository.PAGE_SIZE
                UiState.Success(currentUiModel(isLoadingNextPage = false))
            }
            ApiResult.Empty -> {
                if (!append) {
                    loadedSubscriptions.clear()
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

    private fun errorOrKeep(append: Boolean, message: String): UiState<SubscriptionListUiModel> {
        return if (append && loadedSubscriptions.isNotEmpty()) {
            UiState.Success(currentUiModel(isLoadingNextPage = false))
        } else {
            UiState.Error(message.ifBlank { "Unable to load subscriptions." })
        }
    }

    private fun currentUiModel(isLoadingNextPage: Boolean): SubscriptionListUiModel {
        return SubscriptionListUiModel(
            subscriptions = loadedSubscriptions.toList(),
            totalCount = totalCount,
            isLoadingNextPage = isLoadingNextPage,
            isLastPage = loadedSubscriptions.size >= totalCount
        )
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 400L
    }
}
