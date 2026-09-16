package com.axelliant.hris.core.contracts.network

import com.axelliant.hris.core.network.ApiResult
import retrofit2.Response

interface ApiErrorMapperContract {
    fun <T> mapResponse(response: Response<T>): ApiResult<T>
    fun mapThrowable(throwable: Throwable): ApiResult<Nothing>
    fun messageFor(result: ApiResult<*>): String
}
