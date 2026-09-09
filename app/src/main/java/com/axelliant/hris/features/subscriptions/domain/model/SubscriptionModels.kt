package com.axelliant.hris.features.subscriptions.domain.model

enum class SubscriptionStatus {
    ACTIVE,
    TRIAL,
    PAST_DUE,
    DRAFT,
    UNKNOWN
}

data class SubscriptionModel(
    val id: String,
    val subscriptionNumber: String,
    val accountName: String,
    val planName: String,
    val price: String,
    val nextBillingDate: String,
    val autoRenew: Boolean,
    val status: SubscriptionStatus
)

data class SubscriptionPageResult(
    val subscriptions: List<SubscriptionModel>,
    val totalCount: Int
)

data class SubscriptionListUiModel(
    val subscriptions: List<SubscriptionModel>,
    val totalCount: Int = 0,
    val isLoadingNextPage: Boolean = false,
    val isLastPage: Boolean = false
)

enum class SubscriptionPlanStatus {
    ACTIVE,
    DRAFT,
    ARCHIVED,
    UNKNOWN
}

data class SubscriptionPlanModel(
    val id: String,
    val name: String,
    val code: String,
    val price: String,
    val billingModel: String,
    val billingFrequency: String,
    val allowTrial: Boolean,
    val tierCount: Int,
    val featureCount: Int,
    val activeSubscriptionCount: Int,
    val status: SubscriptionPlanStatus
)

data class SubscriptionPlanPageResult(
    val plans: List<SubscriptionPlanModel>,
    val totalCount: Int
)

data class SubscriptionPlanListUiModel(
    val plans: List<SubscriptionPlanModel>,
    val totalCount: Int = 0,
    val isLoadingNextPage: Boolean = false,
    val isLastPage: Boolean = false
)
