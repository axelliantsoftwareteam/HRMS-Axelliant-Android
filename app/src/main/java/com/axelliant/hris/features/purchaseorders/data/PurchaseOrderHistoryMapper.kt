package com.axelliant.hris.features.purchaseorders.data

import com.axelliant.hris.features.purchaseorders.data.remote.dto.PurchaseOrderCommentDto
import com.axelliant.hris.features.purchaseorders.domain.model.PurchaseOrderHistoryItemUiModel
import java.text.SimpleDateFormat
import java.util.Locale

object PurchaseOrderHistoryMapper {
    fun map(items: List<PurchaseOrderCommentDto>): List<PurchaseOrderHistoryItemUiModel> {
        return items
            .sortedWith(
                compareByDescending<PurchaseOrderCommentDto> { it.commentedOn ?: 0L }
                    .thenByDescending { it.processNo ?: 0 }
            )
            .map { item ->
                val formattedDateTime = item.commentedDate.orEmpty().toDisplayDateTime()
                PurchaseOrderHistoryItemUiModel(
                    comment = item.comment.orEmpty(),
                    commentedBy = item.commentedBy.orEmpty(),
                    commentedDate = item.commentedDate.orEmpty(),
                    dateText = formattedDateTime.first,
                    timeText = formattedDateTime.second,
                    roleName = item.roleName.orEmpty(),
                    processName = item.processName.orEmpty(),
                    processNo = item.processNo ?: 0,
                    commentedOn = item.commentedOn ?: 0L,
                    status = item.status ?: false
                )
            }
    }

    private fun String.toDisplayDateTime(): Pair<String, String> {
        val parsed = runCatching { SOURCE_DATE_FORMAT.parse(this) }.getOrNull()
        return if (parsed != null) {
            DISPLAY_DATE_FORMAT.format(parsed) to DISPLAY_TIME_FORMAT.format(parsed)
        } else {
            substringBeforeLast(", ").ifBlank { this } to substringAfterLast(
                delimiter = ", ",
                missingDelimiterValue = ""
            )
        }
    }

    private val SOURCE_DATE_FORMAT = SimpleDateFormat("MMM dd, yyyy, hh:mm:ss a", Locale.US)
    private val DISPLAY_DATE_FORMAT = SimpleDateFormat("MM-dd-yyyy", Locale.US)
    private val DISPLAY_TIME_FORMAT = SimpleDateFormat("hh:mm a", Locale.US)
}
