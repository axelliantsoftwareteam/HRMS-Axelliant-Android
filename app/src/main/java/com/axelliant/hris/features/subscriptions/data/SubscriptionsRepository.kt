package com.axelliant.hris.features.subscriptions.data

import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.network.BaseApiModel
import com.axelliant.hris.core.network.SafeApiExecutor
import com.axelliant.hris.features.subscriptions.data.remote.SubscriptionApiService
import com.axelliant.hris.features.subscriptions.data.remote.dto.GetSubscriptionsRequest
import com.axelliant.hris.features.subscriptions.data.remote.dto.GetSubscriptionPlansRequest
import com.axelliant.hris.features.subscriptions.domain.model.SubscriptionPlanPageResult
import com.axelliant.hris.features.subscriptions.domain.model.SubscriptionPageResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionsRepository @Inject constructor(
    private val apiService: SubscriptionApiService,
    private val safeApiExecutor: SafeApiExecutor
) {
    suspend fun getSubscriptions(
        start: Int,
        limit: Int = PAGE_SIZE,
        search: String = ""
    ): ApiResult<SubscriptionPageResult> {
        val request = GetSubscriptionsRequest(
            start = start,
            limit = limit,
            search = search.trim()
        )
        return when (val result = safeApiExecutor.execute {
            apiService.getAllSubscriptions(request)
        }) {
            is ApiResult.Success -> {
                val payload = result.data
                val items = payload.data?.data
                if (payload.data?.success == true && items != null) {
                    ApiResult.Success(SubscriptionApiMapper.mapPage(items))
                } else {
                    ApiResult.UnknownError(apiMessage(payload) ?: "Unable to load subscriptions.")
                }
            }
            ApiResult.Empty -> ApiResult.Success(SubscriptionPageResult(emptyList(), 0))
            is ApiResult.HttpError -> ApiResult.HttpError(
                code = result.code,
                message = result.message.ifBlank { "Unable to load subscriptions." },
                errorBody = result.errorBody
            )
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
            ApiResult.Unauthorized -> ApiResult.Unauthorized
        }
    }

    suspend fun getSubscriptionPlans(
        start: Int,
        limit: Int = PAGE_SIZE,
        search: String = ""
    ): ApiResult<SubscriptionPlanPageResult> {
        val request = GetSubscriptionPlansRequest(
            start = start,
            limit = limit,
            search = search.trim()
        )
        return when (val result = safeApiExecutor.execute {
            apiService.getAllSubscriptionPlans(request)
        }) {
            is ApiResult.Success -> {
                val payload = result.data
                val items = payload.data?.data
                if (payload.data?.success == true && items != null) {
                    ApiResult.Success(SubscriptionApiMapper.mapPlanPage(items))
                } else {
                    ApiResult.UnknownError(apiMessage(payload) ?: "Unable to load plans.")
                }
            }
            ApiResult.Empty -> ApiResult.Success(SubscriptionPlanPageResult(emptyList(), 0))
            is ApiResult.HttpError -> ApiResult.HttpError(
                code = result.code,
                message = result.message.ifBlank { "Unable to load plans." },
                errorBody = result.errorBody
            )
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
            ApiResult.Unauthorized -> ApiResult.Unauthorized
        }
    }

    private fun <T> apiMessage(payload: BaseApiModel<T>): String? {
        return payload.data?.message?.takeIf { it.isNotBlank() }
            ?: payload.message?.text?.takeIf { it.isNotBlank() }
    }

    companion object {
        const val PAGE_SIZE = 10
    }
}
