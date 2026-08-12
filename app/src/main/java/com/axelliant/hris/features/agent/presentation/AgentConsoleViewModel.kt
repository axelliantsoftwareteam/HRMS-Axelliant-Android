package com.axelliant.hris.features.agent.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.features.agent.data.AgentChatRepository
import com.axelliant.hris.features.agent.data.AgentChatSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AgentConsoleViewModel @Inject constructor(
    private val repository: AgentChatRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AgentConsoleUiState(session = repository.getSavedSession())
    )
    val uiState: StateFlow<AgentConsoleUiState> = _uiState.asStateFlow()

    fun prepareSession() {
        ensureSession()
    }

    fun sendMessage(message: String) {
        val trimmedMessage = message.trim()
        if (trimmedMessage.isBlank()) return

        val state = _uiState.value
        if (state.session != null) {
            _uiState.value = state.copy(
                pendingMessage = trimmedMessage,
                message = null,
                error = null
            )
            return
        }

        ensureSession(pendingMessage = trimmedMessage)
    }

    fun clearSession() {
        repository.clearSession()
        _uiState.value = AgentConsoleUiState(message = "Agent chat cleared.")
    }

    fun consumePendingMessage() {
        _uiState.value = _uiState.value.copy(pendingMessage = null)
    }

    private fun ensureSession(pendingMessage: String? = null) {
        val state = _uiState.value
        if (state.isLoading) return
        if (state.session != null) {
            _uiState.value = state.copy(
                pendingMessage = pendingMessage,
                message = null,
                error = null
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            _uiState.value = when (val result = repository.ensureSession()) {
                is ApiResult.Success -> AgentConsoleUiState(
                    session = result.data,
                    pendingMessage = pendingMessage,
                    message = null
                )
                ApiResult.Empty -> AgentConsoleUiState(error = "Agent session response is empty.")
                is ApiResult.HttpError -> AgentConsoleUiState(error = result.message)
                is ApiResult.NetworkError -> AgentConsoleUiState(error = result.message)
                is ApiResult.UnknownError -> AgentConsoleUiState(error = result.message)
                ApiResult.Unauthorized -> AgentConsoleUiState(error = "Session expired.")
            }
        }
    }
}

data class AgentConsoleUiState(
    val isLoading: Boolean = false,
    val session: AgentChatSession? = null,
    val pendingMessage: String? = null,
    val message: String? = null,
    val error: String? = null
)
