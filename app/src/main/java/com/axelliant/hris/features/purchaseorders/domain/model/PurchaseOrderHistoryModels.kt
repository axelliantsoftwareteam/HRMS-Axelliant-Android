package com.axelliant.hris.features.purchaseorders.domain.model

data class PurchaseOrderHistoryItemUiModel(
    val comment: String,
    val commentedBy: String,
    val commentedDate: String,
    val dateText: String,
    val timeText: String,
    val roleName: String,
    val processName: String,
    val processNo: Int,
    val commentedOn: Long,
    val status: Boolean
)
