package com.axelliant.hris.features.quotes.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.features.quotes.data.QuotesRepository
import com.axelliant.hris.features.quotes.domain.model.QuotePreviewUiModel
import com.axelliant.hris.features.quotes.domain.model.QuoteStatus
import com.axelliant.hris.features.quotes.domain.model.QuoteWorkflowUiModel
import com.axelliant.hris.features.quotes.domain.model.SubmitQuoteResult
import com.axelliant.hris.features.quotes.domain.model.WorkflowDecisionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ViewQuoteViewModel @Inject constructor(
    private val repository: QuotesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val quoteId: String = checkNotNull(savedStateHandle.get<String>(ARG_QUOTE_ID))

    private val _previewState = MutableStateFlow<UiState<QuotePreviewUiModel>>(UiState.Idle)
    val previewState = _previewState.asStateFlow()

    private val _submitState = MutableStateFlow<UiState<SubmitQuoteResult>>(UiState.Idle)
    val submitState = _submitState.asStateFlow()

    private val _workflowState = MutableStateFlow<UiState<QuoteWorkflowUiModel>>(UiState.Idle)
    val workflowState = _workflowState.asStateFlow()

    private var quoteNumber: String = ""

    init {
        loadPreview()
    }

    fun loadPreview() {
        viewModelScope.launch {
            _previewState.value = UiState.Loading
            _previewState.value = when (val result = repository.getQuotePreview(quoteId)) {
                is ApiResult.Success -> {
                    quoteNumber = result.data.quoteNumber
                    UiState.Success(result.data)
                }
                ApiResult.Empty -> UiState.Error(message = "Quote preview not found.")
                is ApiResult.HttpError -> UiState.Error(result.message)
                is ApiResult.NetworkError -> UiState.Error(result.message)
                is ApiResult.UnknownError -> UiState.Error(result.message)
                ApiResult.Unauthorized -> UiState.Unauthorized
            }
        }
    }

    fun showsWorkflowAction(): Boolean {
        val status = (_previewState.value as? UiState.Success)?.data?.status
        return status == QuoteStatus.Approved || status == QuoteStatus.Submitted
    }

    fun onPrimaryAction() {
        if (showsWorkflowAction()) {
            loadWorkflow()
        } else {
            submitQuote()
        }
    }

    fun loadWorkflow() {
        if (_workflowState.value is UiState.Loading) return

        viewModelScope.launch {
            _workflowState.value = UiState.Loading
            _workflowState.value = when (
                val result = repository.getQuoteWorkflow(
                    relationId = quoteId,
                    quoteNumber = quoteNumber
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

    fun submitQuote() {
        if (_submitState.value is UiState.Loading) return

        viewModelScope.launch {
            _submitState.value = UiState.Loading
            _submitState.value = when (val result = repository.submitQuote(quoteId)) {
                is ApiResult.Success -> UiState.Success(result.data)
                ApiResult.Empty -> UiState.Error(message = "Quote submission failed.")
                is ApiResult.HttpError -> UiState.Error(result.message)
                is ApiResult.NetworkError -> UiState.Error(result.message)
                is ApiResult.UnknownError -> UiState.Error(result.message)
                ApiResult.Unauthorized -> UiState.Unauthorized
            }
        }
    }

    fun resetSubmitState() {
        _submitState.value = UiState.Idle
    }

    fun resetWorkflowState() {
        _workflowState.value = UiState.Idle
    }

    companion object {
        const val ARG_QUOTE_ID = "quoteId"
        const val RESULT_QUOTE_SUBMITTED = "quoteSubmitted"
    }
}
