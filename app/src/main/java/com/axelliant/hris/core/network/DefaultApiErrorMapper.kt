package com.axelliant.hris.core.network

import com.axelliant.hris.core.contracts.network.ApiErrorMapperContract
import retrofit2.Response
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

class DefaultApiErrorMapper @Inject constructor() : ApiErrorMapperContract {
    override fun <T> mapResponse(response: Response<T>): ApiResult<T> {
        if (response.isSuccessful) {
            return response.body()?.let { body -> ApiResult.Success(body) } ?: ApiResult.Empty
        }

        if (response.code() == HTTP_UNAUTHORIZED) {
            return ApiResult.Unauthorized
        }

        return ApiResult.HttpError(
            code = response.code(),
            message = httpMessage(response.code(), response.message()),
            errorBody = response.errorBody()?.string()
        )
    }

    override fun mapThrowable(throwable: Throwable): ApiResult<Nothing> {
        return when (throwable) {
            is UnknownHostException,
            is SocketException -> ApiResult.NetworkError(ApiErrorMessages.NO_INTERNET)

            is SocketTimeoutException -> ApiResult.NetworkError(ApiErrorMessages.TIMEOUT)
            else -> ApiResult.UnknownError(throwable.message ?: ApiErrorMessages.UNKNOWN)
        }
    }

    override fun messageFor(result: ApiResult<*>): String {
        return when (result) {
            ApiResult.Empty -> ApiErrorMessages.EMPTY_BODY
            is ApiResult.Success -> ""
            is ApiResult.HttpError -> result.message.ifBlank { ApiErrorMessages.UNKNOWN }
            is ApiResult.NetworkError -> result.message
            is ApiResult.UnknownError -> result.message
            ApiResult.Unauthorized -> ApiErrorMessages.UNAUTHORIZED
        }
    }

    private fun httpMessage(code: Int, fallback: String): String {
        return when (code) {
            HTTP_BAD_REQUEST -> "Bad request"
            HTTP_NOT_FOUND -> "Api not found"
            HTTP_UNPROCESSABLE_ENTITY -> fallback.ifBlank { ApiErrorMessages.UNKNOWN }
            HTTP_INTERNAL_SERVER_ERROR -> "Internal server error"
            else -> fallback.ifBlank { ApiErrorMessages.UNKNOWN }
        }
    }

    private companion object {
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_NOT_FOUND = 404
        const val HTTP_UNPROCESSABLE_ENTITY = 422
        const val HTTP_INTERNAL_SERVER_ERROR = 500
    }
}
