package com.axelliant.hris.features.inventory.products.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProductComparisonViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProductComparisonUiState())
    val uiState = _uiState.asStateFlow()
    private var compareRunId = 0

    fun setProduct(slot: Int, product: ProductListItemUi) {
        compareRunId++
        val current = _uiState.value
        _uiState.value = when (slot) {
            ProductComparisonFlow.SLOT_TWO -> current.copy(
                productTwo = product,
                isComparing = false,
                showRecommendation = false
            )
            else -> current.copy(
                productOne = product,
                isComparing = false,
                showRecommendation = false
            )
        }
    }

    fun removeProduct(slot: Int) {
        compareRunId++
        val current = _uiState.value
        _uiState.value = when (slot) {
            ProductComparisonFlow.SLOT_TWO -> current.copy(
                productTwo = null,
                isComparing = false,
                showRecommendation = false
            )
            else -> current.copy(
                productOne = null,
                isComparing = false,
                showRecommendation = false
            )
        }
    }

    fun resetComparison() {
        compareRunId++
        _uiState.value = ProductComparisonUiState()
    }

    fun selectTab(tab: ProductComparisonTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun compareProducts() {
        val current = _uiState.value
        if (!current.canCompare || current.isComparing) return
        val runId = ++compareRunId
        _uiState.value = current.copy(
            isComparing = true,
            showRecommendation = false
        )
        viewModelScope.launch {
            delay(COMPARE_PROGRESS_MS)
            val latest = _uiState.value
            if (runId != compareRunId) return@launch
            _uiState.value = latest.copy(
                isComparing = false,
                showRecommendation = latest.canCompare
            )
        }
    }

    private companion object {
        const val COMPARE_PROGRESS_MS = 1100L
    }
}

data class ProductComparisonUiState(
    val productOne: ProductListItemUi? = null,
    val productTwo: ProductListItemUi? = null,
    val selectedTab: ProductComparisonTab = ProductComparisonTab.Overview,
    val isComparing: Boolean = false,
    val showRecommendation: Boolean = false
) {
    val selectedCount: Int
        get() = listOfNotNull(productOne, productTwo).size

    val canCompare: Boolean
        get() = productOne != null && productTwo != null
}

enum class ProductComparisonTab(val title: String) {
    Overview("Overview"),
    PriceStock("Price & Stock"),
    MoreDetails("More Details")
}
