package com.axelliant.hris.features.quotes.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.core.ui.toUiState
import com.axelliant.hris.features.quotes.data.QuotesRepository
import com.axelliant.hris.features.quotes.domain.model.QuoteReportUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class QuoteReportViewModel @Inject constructor(
    private val repository: QuotesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val quoteId: String = checkNotNull(savedStateHandle.get<String>(ARG_QUOTE_ID))

    private val _reportState = MutableStateFlow<UiState<QuoteReportUiModel>>(UiState.Idle)
    val reportState = _reportState.asStateFlow()

    init {
        loadReport()
    }

    fun loadReport() {
        if (_reportState.value is UiState.Loading) return

        viewModelScope.launch {
            _reportState.value = UiState.Loading
            _reportState.value = repository.getQuoteReport(quoteId).toUiState()
        }
    }

    companion object {
        const val ARG_QUOTE_ID = "quoteId"
    }
}
