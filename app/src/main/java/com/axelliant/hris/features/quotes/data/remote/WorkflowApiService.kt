package com.axelliant.hris.features.quotes.data.remote

import com.axelliant.hris.core.network.BaseApiModel
import com.axelliant.hris.features.quotes.data.remote.dto.WorkflowProcessDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WorkflowApiService {
    @GET("Workflow/GetApproveProcess")
    suspend fun getApproveProcess(
        @Query("RelationId") relationId: String
    ): Response<BaseApiModel<List<WorkflowProcessDto>>>
}
