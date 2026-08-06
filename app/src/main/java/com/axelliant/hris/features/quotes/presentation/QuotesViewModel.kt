package com.axelliant.hris.features.quotes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.features.quotes.data.QuoteListMapper
import com.axelliant.hris.features.quotes.data.QuotesRepository
import com.axelliant.hris.features.quotes.data.remote.dto.ApprovalStatusCountsDto
import com.axelliant.hris.features.quotes.data.remote.dto.GetQuotationRequest
import com.axelliant.hris.features.quotes.domain.model.CancelQuoteResult
import com.axelliant.hris.features.quotes.domain.model.QuoteFilterChip
import com.axelliant.hris.features.quotes.domain.model.QuoteListUiModel
import com.axelliant.hris.features.quotes.domain.model.QuoteModel
import com.axelliant.hris.features.quotes.domain.model.QuoteStatusFilterType
import com.axelliant.hris.features.quotes.domain.model.QuoteType
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@HiltViewModel
class QuotesViewModel @Inject constructor(
    private val repository: QuotesRepository,
    private val quoteListMemoryCache: QuoteListMemoryCache
) : ViewModel() {

    private val _quotesState = MutableStateFlow<UiState<QuoteListUiModel>>(UiState.Idle)
    val quotesState = _quotesState.asStateFlow()

    private val _workflowState = MutableStateFlow<UiState<QuoteWorkflowUiModel>>(UiState.Idle)
    val workflowState = _workflowState.asStateFlow()

    private val _cancelState = MutableStateFlow<UiState<CancelQuoteResult>>(UiState.Idle)
    val cancelState = _cancelState.asStateFlow()

    val quotesResponse = quotesState.map { state ->
        (state as? UiState.Success)?.data
    }

    val isLoading = quotesState.map { it is UiState.Loading }

    val isPaginationLoading = quotesState.map { state ->
        (state as? UiState.Success)?.data?.isLoadingNextPage == true
    }

    val errorMessage = quotesState.map { state ->
        (state as? UiState.Error)?.message
    }

    private val searchInput = MutableStateFlow("")
    private var activeSearchQuery = ""
    private var selectedQuoteType = QuoteType.Standard
    private var selectedFilterType = QuoteStatusFilterType.ALL
    private val loadedQuotes = mutableListOf<QuoteModel>()
    private var filterChips = emptyList<QuoteFilterChip>()
    private var globalApprovalStatusCounts: ApprovalStatusCountsDto? = null
    private var globalTotalCount = 0
    private var listTotalCount = 0
    private var nextStart = 0
    private var isPageLoading = false
    private var quotesLoadJob: Job? = null

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
                val trimmedQuery = query.trim()
                if (trimmedQuery == activeSearchQuery) return@onEach
                activeSearchQuery = trimmedQuery
                loadQuotes()
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
        loadQuotes()
    }

    fun onQuoteTypeSelected(type: QuoteType) {
        if (selectedQuoteType == type) return
        selectedQuoteType = type
        selectedFilterType = QuoteStatusFilterType.ALL
        clearGlobalFilterCounts()
        loadQuotes()
    }

    fun onFilterChipSelected(chip: QuoteFilterChip) {
        selectedFilterType = chip.status
        clearSearchQuery()
        loadQuotes()
    }

    fun clearSearchQuery() {
        activeSearchQuery = ""
        searchInput.value = ""
    }

    fun getSelectedFilterId(): String = selectedFilterType.filterId

    fun getSelectedQuoteType(): QuoteType = selectedQuoteType

    fun loadQuotes() {
        quotesLoadJob?.cancel()
        quotesLoadJob = viewModelScope.launch {
            isPageLoading = false
            loadedQuotes.clear()
            listTotalCount = 0
            nextStart = 0
            _quotesState.value = UiState.Loading
            loadPage(start = 0, append = false)
        }
    }

    fun loadQuotesIfNeeded() {
        if (_quotesState.value !is UiState.Idle) return

        val cacheKey = currentCacheKey()
        val cachedSnapshot = quoteListMemoryCache.get(cacheKey)
        if (cachedSnapshot == null) {
            loadQuotes()
            return
        }

        hydrateQuotes(cachedSnapshot)
        refreshCachedQuotes(cachedSnapshot)
    }

    fun loadNextPage() {
        if (
            isPageLoading ||
            loadedQuotes.isEmpty() ||
            loadedQuotes.size >= listTotalCount ||
            _quotesState.value is UiState.Loading
        ) {
            return
        }

        quotesLoadJob = viewModelScope.launch {
            _quotesState.value = UiState.Success(currentUiModel(isLoadingNextPage = true))
            loadPage(start = nextStart, append = true)
        }
    }

    private suspend fun loadPage(
        start: Int,
        append: Boolean,
        limit: Int = QuotesRepository.PAGE_SIZE,
        keepExistingOnError: Boolean = false
    ) {
        isPageLoading = true
        _quotesState.value = when (
            val result = repository.getAllQuotes(
                GetQuotationRequest(
                    start = start,
                    limit = limit,
                    quoteType = selectedQuoteType.toApiValue(),
                    search = activeSearchQuery,
                    approvalStatus = selectedFilterType.apiApprovalStatus
                )
            )
        ) {
            is ApiResult.Success -> {
                val page = result.data
                if (QuoteListMapper.shouldRefreshGlobalFilterCounts(
                        approvalStatus = selectedFilterType.apiApprovalStatus,
                        search = activeSearchQuery,
                        append = append
                    )
                ) {
                    globalApprovalStatusCounts = page.approvalStatusCounts
                    globalTotalCount = page.totalCount
                }
                listTotalCount = page.totalCount
                refreshFilterChipsFromGlobalSource()
                if (!append) {
                    loadedQuotes.clear()
                }
                loadedQuotes.addAll(page.quotes)
                nextStart = loadedQuotes.size

                saveCurrentSnapshot()
                UiState.Success(currentUiModel(isLoadingNextPage = false))
            }

            ApiResult.Empty -> {
                if (!append) {
                    loadedQuotes.clear()
                    listTotalCount = 0
                }
                refreshFilterChipsFromGlobalSource()
                saveCurrentSnapshot()
                UiState.Success(currentUiModel(isLoadingNextPage = false))
            }

            is ApiResult.HttpError -> loadErrorState(result.message, keepExistingOnError)
            is ApiResult.NetworkError -> loadErrorState(result.message, keepExistingOnError)
            is ApiResult.UnknownError -> loadErrorState(result.message, keepExistingOnError)
            ApiResult.Unauthorized -> UiState.Unauthorized
        }
        isPageLoading = false
    }

    private fun loadErrorState(
        message: String,
        keepExistingOnError: Boolean
    ): UiState<QuoteListUiModel> {
        return if (keepExistingOnError) {
            UiState.Success(currentUiModel(isLoadingNextPage = false))
        } else {
            UiState.Error(message)
        }
    }

    private fun hydrateQuotes(snapshot: QuoteListSnapshot) {
        activeSearchQuery = snapshot.key.searchQuery
        selectedQuoteType = snapshot.key.quoteType
        selectedFilterType = snapshot.key.filterType
        loadedQuotes.clear()
        loadedQuotes.addAll(snapshot.quotes)
        filterChips = snapshot.filterChips
        globalApprovalStatusCounts = snapshot.globalApprovalStatusCounts
        globalTotalCount = snapshot.globalTotalCount
        listTotalCount = snapshot.listTotalCount
        nextStart = snapshot.nextStart
        _quotesState.value = UiState.Success(currentUiModel(isLoadingNextPage = false))
    }

    private fun refreshCachedQuotes(snapshot: QuoteListSnapshot) {
        quotesLoadJob?.cancel()
        quotesLoadJob = viewModelScope.launch {
            isPageLoading = false
            val refreshLimit = snapshot.quotes.size
                .coerceAtLeast(QuotesRepository.PAGE_SIZE)
                .coerceAtMost(BACKGROUND_REFRESH_MAX_SIZE)
            loadPage(
                start = 0,
                append = false,
                limit = refreshLimit,
                keepExistingOnError = true
            )
        }
    }

    private fun saveCurrentSnapshot() {
        quoteListMemoryCache.put(
            QuoteListSnapshot(
                key = currentCacheKey(),
                quotes = loadedQuotes.toList(),
                filterChips = filterChips,
                globalApprovalStatusCounts = globalApprovalStatusCounts,
                globalTotalCount = globalTotalCount,
                listTotalCount = listTotalCount,
                nextStart = nextStart
            )
        )
    }

    private fun currentCacheKey(): QuoteListCacheKey {
        return QuoteListCacheKey(
            searchQuery = activeSearchQuery.trim(),
            quoteType = selectedQuoteType,
            filterType = selectedFilterType
        )
    }

    private fun refreshFilterChipsFromGlobalSource() {
        filterChips = QuoteListMapper.buildFilterChips(
            totalCount = globalTotalCount,
            counts = globalApprovalStatusCounts
        )
    }

    private fun clearGlobalFilterCounts() {
        globalApprovalStatusCounts = null
        globalTotalCount = 0
        filterChips = emptyList()
    }

    private fun currentUiModel(isLoadingNextPage: Boolean): QuoteListUiModel {
        val isEmpty = loadedQuotes.isEmpty()
        return QuoteListUiModel(
            quotes = loadedQuotes.toList(),
            filterChips = filterChips,
            totalCount = listTotalCount,
            isLoadingNextPage = isLoadingNextPage,
            isLastPage = loadedQuotes.size >= listTotalCount,
            emptyState = if (isEmpty) {
                QuotesEmptyStateResolver.resolve(activeSearchQuery, selectedFilterType)
            } else {
                null
            }
        )
    }

    fun loadQuoteWorkflow(quote: QuoteModel) {
        if (_workflowState.value is UiState.Loading) return

        viewModelScope.launch {
            _workflowState.value = UiState.Loading
            _workflowState.value = when (
                val result = repository.getQuoteWorkflow(
                    relationId = quote.id,
                    quoteNumber = quote.quoteId
                )
            ) {
                is ApiResult.Success -> UiState.Success(result.data)
                ApiResult.Empty -> UiState.Error(message = "Unable to load quote workflow.")
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

    fun cancelQuote(quoteId: String) {
        if (_cancelState.value is UiState.Loading) return

        viewModelScope.launch {
            _cancelState.value = UiState.Loading
            _cancelState.value = when (val result = repository.cancelQuote(quoteId)) {
                is ApiResult.Success -> UiState.Success(result.data)
                ApiResult.Empty -> UiState.Error(message = "Unable to cancel quote.")
                is ApiResult.HttpError -> UiState.Error(result.message)
                is ApiResult.NetworkError -> UiState.Error(result.message)
                is ApiResult.UnknownError -> UiState.Error(result.message)
                ApiResult.Unauthorized -> UiState.Unauthorized
            }
        }
    }

    fun resetCancelState() {
        _cancelState.value = UiState.Idle
    }

    private fun QuoteType.toApiValue(): Int {
        return when (this) {
            QuoteType.Standard -> QuotesRepository.QUOTE_TYPE_STANDARD
            QuoteType.Quick -> QuotesRepository.QUOTE_TYPE_QUICK
        }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 400L
        private const val BACKGROUND_REFRESH_MAX_SIZE = 50
    }
}
