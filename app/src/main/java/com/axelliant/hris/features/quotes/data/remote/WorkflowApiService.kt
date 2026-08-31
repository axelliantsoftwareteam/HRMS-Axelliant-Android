package com.axelliant.hris.features.quotes.data.remote

import com.axelliant.hris.core.network.BaseApiModel
import com.axelliant.hris.features.quotes.data.remote.dto.WorkflowGraphInstanceDto
import com.axelliant.hris.features.quotes.data.remote.dto.WorkflowProcessDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WorkflowApiService {

    @GET("Workflow/Graph/Instance")
    suspend fun getWorkflowGraphInstance(
        @Query("relationId") relationId: String
    ): Response<BaseApiModel<WorkflowGraphInstanceDto>>

    @GET("Workflow/GetApproveProcess")
    suspend fun getApproveProcess(
        @Query("RelationId") relationId: String
    ): Response<BaseApiModel<List<WorkflowProcessDto>>>
}
