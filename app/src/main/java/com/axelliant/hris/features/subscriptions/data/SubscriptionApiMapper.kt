package com.axelliant.hris.features.subscriptions.data

import com.axelliant.hris.core.extensions.CurrencyFormatter
import com.axelliant.hris.features.subscriptions.data.remote.dto.SubscriptionListItemDto
import com.axelliant.hris.features.subscriptions.data.remote.dto.SubscriptionPlanListItemDto
import com.axelliant.hris.features.subscriptions.domain.model.SubscriptionModel
import com.axelliant.hris.features.subscriptions.domain.model.SubscriptionPageResult
import com.axelliant.hris.features.subscriptions.domain.model.SubscriptionPlanModel
import com.axelliant.hris.features.subscriptions.domain.model.SubscriptionPlanPageResult
import com.axelliant.hris.features.subscriptions.domain.model.SubscriptionPlanStatus
import com.axelliant.hris.features.subscriptions.domain.model.SubscriptionStatus
import java.text.SimpleDateFormat
import java.util.Locale

object SubscriptionApiMapper {
    private const val FALLBACK = "N/A"

    fun mapPage(items: List<SubscriptionListItemDto>): SubscriptionPageResult {
        val subscriptions = items.map(::toListModel)
        return SubscriptionPageResult(
            subscriptions = subscriptions,
            totalCount = items.firstOrNull()?.totalCount ?: subscriptions.size
        )
    }

    fun mapPlanPage(items: List<SubscriptionPlanListItemDto>): SubscriptionPlanPageResult {
        val plans = items.map(::toPlanModel)
        return SubscriptionPlanPageResult(
            plans = plans,
            totalCount = items.firstOrNull()?.totalCount ?: plans.size
        )
    }

    private fun toListModel(dto: SubscriptionListItemDto): SubscriptionModel {
        return SubscriptionModel(
            id = dto.id.orEmpty(),
            subscriptionNumber = dto.subscriptionNumber.orFallback(),
            accountName = dto.accountName.orFallback(),
            planName = dto.planName.orFallback(),
            price = CurrencyFormatter.format(dto.grandTotal ?: dto.unitPrice),
            nextBillingDate = formatDateForUi(dto.nextBillingDate ?: dto.startDate),
            autoRenew = dto.autoRenew == true,
            status = dto.toStatus()
        )
    }

    private fun toPlanModel(dto: SubscriptionPlanListItemDto): SubscriptionPlanModel {
        return SubscriptionPlanModel(
            id = dto.id.orEmpty(),
            name = dto.name.orFallback(),
            code = dto.code.orFallback(),
            price = CurrencyFormatter.format(dto.basePrice),
            billingModel = dto.billingModelTypeName.orFallback(),
            billingFrequency = dto.billingFrequencyName.orFallback(),
            allowTrial = dto.allowTrial == true,
            tierCount = dto.tierCount ?: 0,
            featureCount = dto.featureCount ?: 0,
            activeSubscriptionCount = dto.activeSubscriptionCount ?: 0,
            status = dto.status.toPlanStatus()
        )
    }

    private fun SubscriptionListItemDto.toStatus(): SubscriptionStatus {
        val label = subscriptionStatusName.orEmpty()
        return when {
            label.equals("Active", ignoreCase = true) -> SubscriptionStatus.ACTIVE
            label.equals("Trial", ignoreCase = true) -> SubscriptionStatus.TRIAL
            label.equals("Past due", ignoreCase = true) ||
                label.equals("PastDue", ignoreCase = true) -> SubscriptionStatus.PAST_DUE
            label.equals("Draft", ignoreCase = true) -> SubscriptionStatus.DRAFT
            status == 3 -> SubscriptionStatus.ACTIVE
            else -> SubscriptionStatus.UNKNOWN
        }
    }

    private fun Int?.toPlanStatus(): SubscriptionPlanStatus {
        return when (this) {
            1 -> SubscriptionPlanStatus.ACTIVE
            0 -> SubscriptionPlanStatus.DRAFT
            2 -> SubscriptionPlanStatus.ARCHIVED
            else -> SubscriptionPlanStatus.UNKNOWN
        }
    }

    private fun formatDateForUi(raw: String?): String {
        if (raw.isNullOrBlank()) return FALLBACK
        parseDate(raw)?.let {
            return SimpleDateFormat("MMM d, yyyy", Locale.US).format(it)
        }
        return raw
    }

    private fun parseDate(raw: String): java.util.Date? {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd",
            "MM-dd-yyyy",
            "MM/dd/yyyy",
            "MMM d, yyyy, h:mm:ss a",
            "MMM dd, yyyy, hh:mm:ss a"
        )
        return formats.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.US).parse(raw)
            }.getOrNull()
        }
    }

    private fun String?.orFallback(): String = this?.trim()?.takeIf { it.isNotEmpty() } ?: FALLBACK
}
