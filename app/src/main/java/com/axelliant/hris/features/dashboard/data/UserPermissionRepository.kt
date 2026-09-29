package com.axelliant.hris.features.dashboard.data

import com.axelliant.hris.core.AppDrawerAction
import com.axelliant.hris.core.network.ApiResult
import com.axelliant.hris.core.network.SafeApiExecutor
import com.axelliant.hris.core.session.SessionManager
import com.axelliant.hris.features.dashboard.data.remote.UserPermissionApiService
import com.axelliant.hris.features.dashboard.data.remote.dto.UserMenuPermissionGroupDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPermissionRepository @Inject constructor(
    private val apiService: UserPermissionApiService,
    private val safeApiExecutor: SafeApiExecutor,
    private val sessionManager: SessionManager
) {
    suspend fun getAllowedDrawerActions(): ApiResult<Set<AppDrawerAction>> {
        val userId = sessionManager.getUserId()?.takeIf { it.isNotBlank() }
            ?: return ApiResult.UnknownError("User id was missing from the current session.")

        return when (val result = safeApiExecutor.execute { apiService.getUserMenuPermissions(userId) }) {
            is ApiResult.Success -> {
                val groups = result.data.data?.data.orEmpty()
                ApiResult.Success(groups.toAllowedDrawerActions())
            }
            ApiResult.Empty -> ApiResult.Empty
            is ApiResult.HttpError -> result
            is ApiResult.NetworkError -> result
            is ApiResult.UnknownError -> result
            ApiResult.Unauthorized -> ApiResult.Unauthorized
        }
    }

    private fun List<UserMenuPermissionGroupDto>.toAllowedDrawerActions(): Set<AppDrawerAction> {
        val visibleEntities = flatMap { group -> group.rolePermissions.orEmpty() }
            .flatMap { role -> role.entities.orEmpty() }
            .filter { entity ->
                entity.actions.orEmpty().any { action ->
                    action.action.equals(VIEW_ACTION, ignoreCase = true) && action.status == ENABLED_STATUS
                }
            }
            .mapNotNull { it.entity?.trim()?.lowercase() }
            .toSet()

        return AppDrawerAction.entries
            .filter { action ->
                action.permissionEntities.isEmpty() || action.permissionEntities.any { it in visibleEntities }
            }
            .toSet()
    }

    private val AppDrawerAction.permissionEntities: Set<String>
        get() = when (this) {
            AppDrawerAction.Products -> setOf("product")
            AppDrawerAction.Quotes -> setOf("quote")
            AppDrawerAction.SaleOrders -> setOf("salesorder")
            AppDrawerAction.PurchaseOrders -> setOf("purchaseorder")
            AppDrawerAction.Subscriptions -> setOf("subscription", "managedsubscription")
            AppDrawerAction.Warehouse,
            AppDrawerAction.Warehouses,
            AppDrawerAction.WarehouseLocations,
            AppDrawerAction.WarehouseReceiving,
            AppDrawerAction.WarehouseInventory,
            AppDrawerAction.WarehousePutaway,
            AppDrawerAction.InventoryReservations,
            AppDrawerAction.WarehousePicking,
            AppDrawerAction.CustomerSuppliedInventory -> setOf("warehouse")
            AppDrawerAction.Profiles -> setOf("user")
            AppDrawerAction.Home,
            AppDrawerAction.AgentConsole,
            AppDrawerAction.Calendar,
            AppDrawerAction.Attendance,
            AppDrawerAction.Requests,
            AppDrawerAction.Leaves,
            AppDrawerAction.CheckInRequests,
            AppDrawerAction.Expenses,
            AppDrawerAction.DocumentVault,
            AppDrawerAction.ResourceManagement,
            AppDrawerAction.Settings,
            AppDrawerAction.Logout -> emptySet()
        }

    private companion object {
        const val VIEW_ACTION = "view"
        const val ENABLED_STATUS = 1
    }
}
