package com.axelliant.hris.features.dashboard.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UserMenuPermissionGroupDto(
    @SerializedName("groupName")
    val groupName: String? = null,
    @SerializedName("rolePermissions")
    val rolePermissions: List<UserRolePermissionDto>? = null
)

data class UserRolePermissionDto(
    @SerializedName("roleId")
    val roleId: String? = null,
    @SerializedName("roleName")
    val roleName: String? = null,
    @SerializedName("entities")
    val entities: List<UserPermissionEntityDto>? = null
)

data class UserPermissionEntityDto(
    @SerializedName("url")
    val url: String? = null,
    @SerializedName("entity")
    val entity: String? = null,
    @SerializedName("entityName")
    val entityName: String? = null,
    @SerializedName("actions")
    val actions: List<UserPermissionActionDto>? = null
)

data class UserPermissionActionDto(
    @SerializedName("action")
    val action: String? = null,
    @SerializedName("permissionId")
    val permissionId: String? = null,
    @SerializedName("status")
    val status: Int? = null
)
