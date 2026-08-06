package com.axelliant.hris.features.quotes.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GetQuotationRequest(
    @SerializedName("start")
    val start: Int = 0,
    @SerializedName("limit")
    val limit: Int = 10,
    @SerializedName("sort")
    val sort: String = "",
    @SerializedName("order")
    val order: String = "",
    @SerializedName("isPaginated")
    val isPaginated: Boolean = true,
    @SerializedName("search")
    val search: String = "",
    @SerializedName("filter")
    val filter: List<Any> = emptyList(),
    @SerializedName("QuoteType")
    val quoteType: Int,
    @SerializedName("approvalStatus")
    val approvalStatus: Int? = null
)

data class GetQuotationResponse(
    @SerializedName("totalCount")
    val totalCount: Int? = null,
    @SerializedName("dataList")
    val dataList: List<QuotationItemDto>? = null,
    @SerializedName("approvalStatusCounts")
    val approvalStatusCounts: ApprovalStatusCountsDto? = null
)

data class ApprovalStatusCountsDto(
    @SerializedName(value = "approved", alternate = ["Approved"])
    val approved: Int? = null,
    @SerializedName(value = "submitted", alternate = ["Submitted"])
    val submitted: Int? = null,
    @SerializedName(value = "rejected", alternate = ["Rejected"])
    val rejected: Int? = null,
    @SerializedName(value = "saveAsDraft", alternate = ["SaveAsDraft", "draft", "Draft"])
    val saveAsDraft: Int? = null,
    @SerializedName(value = "awaitingApproval", alternate = ["AwaitingApproval", "awaiting"])
    val awaitingApproval: Int? = null,
    @SerializedName(value = "cancelled", alternate = ["Cancelled", "canceled", "Canceled"])
    val cancelled: Int? = null,
    @SerializedName(value = "active", alternate = ["Active"])
    val active: Int? = null,
    @SerializedName(value = "pending", alternate = ["Pending"])
    val pending: Int? = null
)

data class QuotationItemDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("quotationId")
    val quotationId: String? = null,
    @SerializedName("quoteSerialNo")
    val quoteSerialNo: String? = null,
    @SerializedName("accountName")
    val accountName: String? = null,
    @SerializedName("acountName")
    val acountName: String? = null,
    @SerializedName("accountSerialNo")
    val accountSerialNo: String? = null,
    @SerializedName("totalAmount")
    val totalAmount: Double? = null,
    @SerializedName("paymentTerm")
    val paymentTerm: String? = null,
    @SerializedName("validityDays")
    val validityDays: Int? = null,
    @SerializedName("approvalStatus")
    val approvalStatus: Int? = null,
    @SerializedName("createdDate")
    val createdDate: String? = null,
    @SerializedName("createdBy")
    val createdBy: String? = null,
    @SerializedName("utilizingStatus")
    val utilizingStatus: String? = null,
    @SerializedName("quoteType")
    val quoteType: Int? = null,
    @SerializedName("notes")
    val notes: String? = null
)
