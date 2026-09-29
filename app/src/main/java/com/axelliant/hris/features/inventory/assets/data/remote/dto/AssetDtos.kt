package com.axelliant.hris.features.inventory.assets.data.remote.dto

data class AssetListResponse(
    val items: List<AssetSummaryResponse>,
    val page: Int,
    val totalPages: Int,
    val totalItems: Int
)

data class AssetSummaryResponse(
    val id: String,
    val assetTag: String,
    val productName: String,
    val serialNumber: String? = null,
    val status: String,
    val assignedTo: String? = null
)

data class AssetDetailResponse(
    val id: String,
    val assetTag: String,
    val productId: String,
    val productName: String,
    val serialNumber: String? = null,
    val status: String,
    val assignedTo: String? = null,
    val purchaseDate: String? = null,
    val warrantyExpiry: String? = null,
    val location: String? = null
)
