package com.axelliant.hris.features.dashboard.data.remote

import com.axelliant.hris.core.network.BaseApiModel
import com.axelliant.hris.features.dashboard.data.remote.dto.UserMenuPermissionGroupDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface UserPermissionApiService {
    @GET("UserPermission/GetUserMenuPermissions")
    suspend fun getUserMenuPermissions(
        @Query("id") userId: String
    ): Response<BaseApiModel<List<UserMenuPermissionGroupDto>>>
}
