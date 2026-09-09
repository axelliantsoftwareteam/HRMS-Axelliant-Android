package com.axelliant.hris.features.subscriptions.data.remote.dto

data class GetSubscriptionsRequest(
    val start: Int,
    val limit: Int,
    val sort: String = "",
    val order: String = "",
    val isPaginated: Boolean = true,
    val search: String = "",
    val accountId: String? = null,
    val planId: String? = null,
    val subscriptionStatus: Int? = null,
    val salesPersonId: String? = null,
    val currencyCode: String = "",
    val startDateFrom: String? = null,
    val startDateTo: String? = null
)

data class SubscriptionListItemDto(
    val id: String? = null,
    val subscriptionNumber: String? = null,
    val accountId: String? = null,
    val accountName: String? = null,
    val planId: String? = null,
    val planName: String? = null,
    val planCode: String? = null,
    val billingModelType: Int? = null,
    val billingModelTypeName: String? = null,
    val billingFrequency: Int? = null,
    val billingFrequencyName: String? = null,
    val currencyCode: String? = null,
    val unitPrice: Double? = null,
    val quantity: Int? = null,
    val subTotal: Double? = null,
    val grandTotal: Double? = null,
    val itemsTotal: Double? = null,
    val startDate: String? = null,
    val nextBillingDate: String? = null,
    val autoRenew: Boolean? = null,
    val subscriptionStatusName: String? = null,
    val itemCount: Int? = null,
    val totalCount: Int? = null,
    val serialNo: Int? = null,
    val status: Int? = null,
    val createdOn: Long? = null,
    val createdDate: String? = null
)

data class GetSubscriptionPlansRequest(
    val start: Int,
    val limit: Int,
    val sort: String = "",
    val order: String = "",
    val isPaginated: Boolean = true,
    val search: String = "",
    val billingModelType: Int? = null,
    val planStatus: Int? = null,
    val currencyCode: String = ""
)

data class SubscriptionPlanListItemDto(
    val id: String? = null,
    val name: String? = null,
    val code: String? = null,
    val description: String? = null,
    val billingModelType: Int? = null,
    val billingModelTypeName: String? = null,
    val billingFrequency: Int? = null,
    val billingFrequencyName: String? = null,
    val pricingStrategy: Int? = null,
    val basePrice: Double? = null,
    val currencyCode: String? = null,
    val allowTrial: Boolean? = null,
    val autoRenew: Boolean? = null,
    val tierCount: Int? = null,
    val featureCount: Int? = null,
    val addonCount: Int? = null,
    val vendorMappingCount: Int? = null,
    val activeSubscriptionCount: Int? = null,
    val productIds: List<String>? = null,
    val productsName: List<String>? = null,
    val totalCount: Int? = null,
    val serialNo: Int? = null,
    val status: Int? = null,
    val createdOn: Long? = null,
    val createdDate: String? = null
)
