package com.axelliant.hris.core.ui

import com.axelliant.hris.core.network.ApiErrorMessages
import com.axelliant.hris.core.network.ApiResult

sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
    data object Unauthorized : UiState<Nothing>
}

inline fun <T> ApiResult<T>.toUiState(
    isEmpty: (T) -> Boolean = { false }
): UiState<T> {
    return when (this) {
        ApiResult.Empty -> UiState.Empty
        is ApiResult.Success -> if (isEmpty(data)) UiState.Empty else UiState.Success(data)
        is ApiResult.HttpError -> UiState.Error(message.ifBlank { ApiErrorMessages.UNKNOWN })
        is ApiResult.NetworkError -> UiState.Error(message)
        is ApiResult.UnknownError -> UiState.Error(message)
        ApiResult.Unauthorized -> UiState.Unauthorized
    }
}
