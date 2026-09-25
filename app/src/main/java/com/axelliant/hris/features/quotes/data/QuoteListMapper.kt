package com.axelliant.hris.features.quotes.data

import com.axelliant.hris.core.extensions.CurrencyFormatter
import com.axelliant.hris.features.quotes.data.remote.dto.ApprovalStatusCountsDto
import com.axelliant.hris.features.quotes.data.remote.dto.GetQuotationResponse
import com.axelliant.hris.features.quotes.data.remote.dto.QuotationItemDto
import com.axelliant.hris.features.quotes.domain.model.QuoteFilterChip
import com.axelliant.hris.features.quotes.domain.model.QuoteModel
import com.axelliant.hris.features.quotes.domain.model.QuoteStatusFilterType
import com.axelliant.hris.features.quotes.domain.model.QuoteType
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object QuoteListMapper {
    private const val FALLBACK = "N/A"
    private const val UNKNOWN_APPROVAL_STATUS = -1
    private val inputDateFormats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd"
    )
    private val outputDateFormat = SimpleDateFormat("MM-dd-yyyy", Locale.US)

    fun toQuoteModels(items: List<QuotationItemDto>, quoteType: QuoteType): List<QuoteModel> {
        return items.map { item -> item.toQuoteModel(quoteType) }
    }

    fun buildFilterChips(
        totalCount: Int,
        counts: ApprovalStatusCountsDto?
    ): List<QuoteFilterChip> {
        val statusCounts = counts ?: ApprovalStatusCountsDto()
        return listOf(
            QuoteFilterChip(
                count = totalCount,
                status = QuoteStatusFilterType.ALL
            ),
            QuoteFilterChip(
                count = statusCounts.approved ?: 0,
                status = QuoteStatusFilterType.APPROVED
            ),
            QuoteFilterChip(
                count = statusCounts.submitted ?: 0,
                status = QuoteStatusFilterType.SUBMITTED
            ),
            QuoteFilterChip(
                count = statusCounts.rejected ?: 0,
                status = QuoteStatusFilterType.REJECTED
            ),
            QuoteFilterChip(
                count = statusCounts.saveAsDraft ?: 0,
                status = QuoteStatusFilterType.DRAFT
            )
        )
    }

    fun shouldRefreshGlobalFilterCounts(
        approvalStatus: Int?,
        search: String,
        append: Boolean
    ): Boolean {
        return !append && approvalStatus == null && search.isBlank()
    }

    fun mapPage(
        response: GetQuotationResponse,
        quoteType: QuoteType
    ): QuoteListPage {
        val items = response.dataList.orEmpty()
        val total = response.totalCount ?: items.size
        return QuoteListPage(
            quotes = toQuoteModels(items, quoteType),
            totalCount = total,
            approvalStatusCounts = response.approvalStatusCounts
        )
    }

    private fun QuotationItemDto.toQuoteModel(quoteType: QuoteType): QuoteModel {
        return QuoteModel(
            id = id ?: quotationId.orEmpty(),
            quoteId = quoteSerialNo.orFallback(),
            quoteName = quoteName.orFallback(quoteSerialNo),
            customerName = accountName?.takeIf { it.isNotBlank() }
                ?: acountName.orFallback(),
            approvalStatus = approvalStatus ?: UNKNOWN_APPROVAL_STATUS,
            createdBy = createdBy.orFallback(),
            date = formatDate(createdDate),
            totalAmount = CurrencyFormatter.format(totalAmount),
            quoteType = quoteType,
            paymentTerm = paymentTerm.orFallback(),
            validityDays = validityDays?.toString().orFallback(),
            utilizingStatus = utilizingStatus.orFallback(),
            notes = notes.orEmpty()
        )
    }

    private fun formatDate(raw: String?): String {
        if (raw.isNullOrBlank()) return FALLBACK
        inputDateFormats.forEach { pattern ->
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(raw)
            }.getOrNull()
            if (parsed != null) {
                return outputDateFormat.format(parsed)
            }
        }
        return raw
    }

    private fun String?.orFallback(fallback: String? = null): String {
        return this?.trim()?.takeIf { it.isNotEmpty() }
            ?: fallback?.trim()?.takeIf { it.isNotEmpty() }
            ?: FALLBACK
    }
}

data class QuoteListPage(
    val quotes: List<QuoteModel>,
    val totalCount: Int,
    val approvalStatusCounts: ApprovalStatusCountsDto?
)
