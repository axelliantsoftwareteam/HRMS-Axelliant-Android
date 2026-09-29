package com.axelliant.hris.core.network

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data object Empty : ApiResult<Nothing>
    data class HttpError(
        val code: Int,
        val message: String,
        val errorBody: String? = null
    ) : ApiResult<Nothing>

    data class NetworkError(val message: String) : ApiResult<Nothing>
    data class UnknownError(val message: String) : ApiResult<Nothing>
    data object Unauthorized : ApiResult<Nothing>
}
