package com.axelliant.hris.features.quotes.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.features.quotes.data.QuotesRepository
import com.axelliant.hris.features.quotes.domain.model.QuoteModel
import com.axelliant.hris.features.quotes.domain.model.QuoteStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class EditQuoteViewModel @Inject constructor(
    private val repository: QuotesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val quoteId: String = checkNotNull(savedStateHandle.get<String>(ARG_QUOTE_ID))

    private val _quoteState = MutableStateFlow<UiState<QuoteModel>>(UiState.Idle)
    val quoteState = _quoteState.asStateFlow()

    private val _saveState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val saveState = _saveState.asStateFlow()

    init {
        loadQuote()
    }

    fun loadQuote() {
        viewModelScope.launch {
            _quoteState.value = UiState.Loading
            val quote = repository.getQuoteById(quoteId)
            _quoteState.value = when (quote) {
                null -> UiState.Error(message = "Quote not found.")
                else -> UiState.Success(quote)
            }
        }
    }

    fun saveQuote(
        customerName: String,
        statusLabel: String,
        createdBy: String,
        date: String,
        totalAmount: String,
        notes: String
    ) {
        val currentQuote = (_quoteState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _saveState.value = UiState.Loading
            runCatching {
                repository.saveQuote(
                    currentQuote.copy(
                        customerName = customerName,
                        approvalStatus = parseStatus(statusLabel, currentQuote).apiValue,
                        createdBy = createdBy,
                        date = date,
                        totalAmount = totalAmount,
                        notes = notes
                    )
                )
            }.onSuccess {
                _saveState.value = UiState.Success(Unit)
            }.onFailure {
                _saveState.value = UiState.Error(message = "Unable to save quote.")
            }
        }
    }

    private fun parseStatus(label: String, quote: QuoteModel): QuoteStatus {
        val trimmed = label.trim()
        trimmed.toIntOrNull()?.let { apiValue ->
            QuoteStatus.fromApiValue(apiValue)?.let { return it }
        }
        return QuoteStatus.entries.firstOrNull { status ->
            status.name.equals(trimmed, ignoreCase = true) ||
                trimmed.contains(status.name, ignoreCase = true)
        } ?: quote.status ?: QuoteStatus.Draft
    }

    companion object {
        const val ARG_QUOTE_ID = "quoteId"
    }
}
