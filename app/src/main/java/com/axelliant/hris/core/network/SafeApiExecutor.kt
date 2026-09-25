package com.axelliant.hris.core.network

import com.axelliant.hris.core.contracts.navigation.WorkspaceKey
import com.axelliant.hris.core.contracts.network.ApiErrorMapperContract
import com.axelliant.hris.core.contracts.session.SessionExpiryContract
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SafeApiExecutor @Inject constructor(
    private val networkMonitor: NetworkMonitor,
    private val apiErrorMapper: ApiErrorMapperContract,
    private val sessionExpiryContract: SessionExpiryContract
) {
    suspend fun <T> execute(
        handleUnauthorized: Boolean = true,
        workspace: WorkspaceKey = WorkspaceKey.INTERNAL_APPS,
        apiCall: suspend () -> Response<T>
    ): ApiResult<T> {
        if (!networkMonitor.isOnline.value) {
            return ApiResult.NetworkError(ApiErrorMessages.NO_INTERNET)
        }
        return try {
            val result = apiErrorMapper.mapResponse(apiCall())
            if (result is ApiResult.Unauthorized && handleUnauthorized) {
                sessionExpiryContract.handleSessionExpired(
                    workspace = workspace,
                    message = apiErrorMapper.messageFor(result)
                )
            }
            result
        } catch (throwable: Throwable) {
            apiErrorMapper.mapThrowable(throwable)
        }
    }
}
