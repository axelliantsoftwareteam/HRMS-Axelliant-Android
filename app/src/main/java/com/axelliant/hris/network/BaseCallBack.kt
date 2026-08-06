package com.axelliant.hris.network

import com.axelliant.hris.core.events.AppSessionEvents
import com.axelliant.hris.core.contracts.network.ApiErrorMapperContract
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.network.DefaultApiErrorMapper

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

abstract class BaseCallBack<T>(
    private val call: Call<T>,
    private val apiErrorMapper: ApiErrorMapperContract = DefaultApiErrorMapper()
) : Callback<T> {
    abstract fun onFinalSuccess(call: Call<T>, response: Response<T>)

    abstract fun onFinalFailure(
        errorString: String? = null
    )

    override fun onResponse(call: Call<T>, response: Response<T>) {
        onResponseValidator(call, response)

    }

    override fun onFailure(call: Call<T>, t: Throwable) {
        onFinalFailure(apiErrorMapper.messageFor(apiErrorMapper.mapThrowable(t)))
    }


    private fun onResponseValidator(call: Call<T>, response: Response<T>) {
        AppSessionEvents.sessionExpired.set(response.code())
        when (val result = apiErrorMapper.mapResponse(response)) {
            is ApiResult.Success,
            ApiResult.Empty -> onFinalSuccess(call, response)

            is ApiResult.HttpError,
            is ApiResult.NetworkError,
            is ApiResult.UnknownError,
            ApiResult.Unauthorized -> onFinalFailure(apiErrorMapper.messageFor(result))
        }
    }
}
