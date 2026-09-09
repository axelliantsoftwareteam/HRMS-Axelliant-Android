package com.axelliant.hris.features.subscriptions.data.remote

import com.axelliant.hris.core.network.BaseApiModel
import com.axelliant.hris.features.subscriptions.data.remote.dto.GetSubscriptionsRequest
import com.axelliant.hris.features.subscriptions.data.remote.dto.GetSubscriptionPlansRequest
import com.axelliant.hris.features.subscriptions.data.remote.dto.SubscriptionPlanListItemDto
import com.axelliant.hris.features.subscriptions.data.remote.dto.SubscriptionListItemDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface SubscriptionApiService {
    @POST("SubSubscription/GetAll")
    suspend fun getAllSubscriptions(
        @Body request: GetSubscriptionsRequest
    ): Response<BaseApiModel<List<SubscriptionListItemDto>>>

    @POST("SubPlan/GetAll")
    suspend fun getAllSubscriptionPlans(
        @Body request: GetSubscriptionPlansRequest
    ): Response<BaseApiModel<List<SubscriptionPlanListItemDto>>>
}
