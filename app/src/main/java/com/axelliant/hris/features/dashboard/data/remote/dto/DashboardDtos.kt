package com.axelliant.hris.features.dashboard.data.remote.dto

data class DashboardSummaryResponse(
    val totalAssets: Int,
    val activeAssets: Int,
    val expiredAssets: Int,
    val expiringSoon: Int,
    val assetDistribution: List<AssetDistributionItemResponse> = emptyList(),
    val monthlyOverview: List<MonthlyAssetOverviewResponse> = emptyList()
)

data class AssetDistributionItemResponse(
    val label: String,
    val percentage: Double
)

data class MonthlyAssetOverviewResponse(
    val month: String,
    val added: Int,
    val removed: Int
)
