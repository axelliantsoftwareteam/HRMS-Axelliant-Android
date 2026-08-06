package com.axelliant.hris.features.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.core.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor() : ViewModel() {
    private val _dashboardState = MutableStateFlow<UiState<DashboardUiModel>>(UiState.Idle)
    val dashboardState = _dashboardState.asStateFlow()

    fun loadSummary() {
        viewModelScope.launch {
            _dashboardState.value = UiState.Success(
                DashboardUiModel(
                    totalProducts = 1248,
                    activeQuotes = 42,
                    assetHealthLabel = "100",
                    openOrders = 8,
                    totalProfiles = 72,
                    activeModules = 14
                )
            )
        }
    }
}

data class DashboardUiModel(
    val totalProducts: Int,
    val activeQuotes: Int,
    val assetHealthLabel: String,
    val openOrders: Int,
    val totalProfiles: Int,
    val activeModules: Int
)
