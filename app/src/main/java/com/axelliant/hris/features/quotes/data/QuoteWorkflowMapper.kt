package com.axelliant.hris.features.quotes.data

import com.axelliant.hris.features.quotes.data.remote.dto.WorkflowProcessDto
import com.axelliant.hris.features.profiles.data.local.ProfileLocalSettings
import com.axelliant.hris.features.profiles.data.local.ProfileSettingsStore
import com.axelliant.hris.features.quotes.domain.model.QuoteWorkflowDisplayMode
import com.axelliant.hris.features.quotes.domain.model.QuoteWorkflowStepState
import com.axelliant.hris.features.quotes.domain.model.QuoteWorkflowStepUiModel
import com.axelliant.hris.features.quotes.domain.model.QuoteWorkflowUiModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object QuoteWorkflowMapper {

    private const val FALLBACK = "N/A"
    private const val EPOCH_MILLIS_THRESHOLD = 1_000_000_000_000L

    fun toUiModel(
        quoteNumber: String,
        steps: List<WorkflowProcessDto>,
        settings: ProfileLocalSettings
    ): QuoteWorkflowUiModel {

        val mappedSteps = steps
            .sortedBy { it.processNo ?: Int.MAX_VALUE }
            .map { dto ->
                toStepUiModel(
                    dto = dto,
                    settings = settings
                )
            }

        val displayMode = if (
            mappedSteps.isNotEmpty() &&
            mappedSteps.all {
                it.stepState == QuoteWorkflowStepState.Approved ||
                        it.stepState == QuoteWorkflowStepState.Skipped
            }
        ) {
            QuoteWorkflowDisplayMode.Completed
        } else {
            QuoteWorkflowDisplayMode.InProgress
        }

        return QuoteWorkflowUiModel(
            quoteNumber = quoteNumber,
            displayMode = displayMode,
            steps = mappedSteps
        )
    }

    private fun toStepUiModel(
        dto: WorkflowProcessDto,
        settings: ProfileLocalSettings
    ): QuoteWorkflowStepUiModel {

        val state = when (dto.state?.lowercase()) {
            "approved" -> QuoteWorkflowStepState.Approved
            "active" -> QuoteWorkflowStepState.Active
            "pending" -> QuoteWorkflowStepState.Pending
            "rejected" -> QuoteWorkflowStepState.Rejected
            "skipped" -> QuoteWorkflowStepState.Skipped
            else -> QuoteWorkflowStepState.Pending
        }

        val statusLabel = when (state) {
            QuoteWorkflowStepState.Approved -> "Approved"
            QuoteWorkflowStepState.Active -> "In Progress"
            QuoteWorkflowStepState.Pending -> "Pending"
            QuoteWorkflowStepState.Rejected ->
                dto.rejectText.orFallback().ifBlank { "Rejected" }

            QuoteWorkflowStepState.Skipped -> "Skipped"
        }

        return QuoteWorkflowStepUiModel(
            processNo = dto.processNo ?: 0,

            roleName = dto.roleName.orFallback(),

            processName = dto.processName.orFallback(),

            processScreenName = dto.processScreenName.orFallback(),

            timestampDate = formatTimestamp(
                dto.timeStamp,
                settings
            ) ?: dto.timestampDate.orEmpty().trim(),

            approveText = dto.approveText
                .orFallback()
                .ifBlank { "Approve" },

            rejectText = dto.rejectText
                .orFallback()
                .ifBlank { "Reject" },

            statusLabel = statusLabel,

            stepState = state
        )
    }

    private fun formatTimestamp(
        timestamp: Long?,
        settings: ProfileLocalSettings
    ): String? {
        val safeTimestamp = timestamp?.takeIf { it > 0L } ?: return null
        val epochMillis = if (safeTimestamp >= EPOCH_MILLIS_THRESHOLD) {
            safeTimestamp
        } else {
            safeTimestamp * 1000L
        }
        val pattern =
            "${settings.dateFormat.toDatePattern()}, ${settings.timeFormat.toTimePattern()}"
        return runCatching {
            SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone(settings.timeZoneId)
            }.format(Date(epochMillis))
        }.getOrNull()
    }

    private fun String.toDatePattern(): String {
        return when (this) {
            ProfileSettingsStore.DATE_FORMAT_OPTIONS[1] -> "dd/MM/yyyy"
            ProfileSettingsStore.DATE_FORMAT_OPTIONS[2] -> "yyyy-MM-dd"
            else -> "MM/dd/yyyy"
        }
    }

    private fun String.toTimePattern(): String {
        return if (this == ProfileSettingsStore.TIME_FORMAT_OPTIONS[1]) {
            "HH:mm"
        } else {
            "hh:mm a"
        }
    }

    private fun String?.orFallback(): String = this?.trim()?.takeIf { it.isNotEmpty() } ?: FALLBACK
}
