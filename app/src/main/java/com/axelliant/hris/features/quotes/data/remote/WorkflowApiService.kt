package com.axelliant.hris.features.quotes.data.remote

import com.axelliant.hris.core.network.BaseApiModel
import com.axelliant.hris.features.quotes.data.remote.dto.WorkflowDecisionRequest
import com.axelliant.hris.features.quotes.data.remote.dto.WorkflowGraphInstanceDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface WorkflowApiService {

    @GET("Workflow/Graph/Instance")
    suspend fun getWorkflowGraphInstance(
        @Query("relationId") relationId: String
    ): Response<BaseApiModel<WorkflowGraphInstanceDto>>

    @POST("Workflow/Graph/Decide")
    suspend fun decideWorkflowNode(
        @Body request: WorkflowDecisionRequest
    ): Response<BaseApiModel<Any>>
}
