package com.axelliant.hris.features.quotes.presentation

import com.axelliant.hris.features.quotes.data.remote.dto.ApprovalStatusCountsDto
import com.axelliant.hris.features.quotes.domain.model.QuoteFilterChip
import com.axelliant.hris.features.quotes.domain.model.QuoteModel
import com.axelliant.hris.features.quotes.domain.model.QuoteStatusFilterType
import com.axelliant.hris.features.quotes.domain.model.QuoteType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuoteListMemoryCache @Inject constructor() {
    private val snapshots = LinkedHashMap<QuoteListCacheKey, QuoteListSnapshot>(
        MAX_ENTRIES,
        LOAD_FACTOR,
        true
    )

    @Synchronized
    fun get(key: QuoteListCacheKey): QuoteListSnapshot? = snapshots[key]

    @Synchronized
    fun put(snapshot: QuoteListSnapshot) {
        snapshots[snapshot.key] = snapshot
        while (snapshots.size > MAX_ENTRIES) {
            val oldestKey = snapshots.keys.firstOrNull() ?: return
            snapshots.remove(oldestKey)
        }
    }

    private companion object {
        const val MAX_ENTRIES = 4
        const val LOAD_FACTOR = 0.75f
    }
}

data class QuoteListCacheKey(
    val searchQuery: String,
    val quoteType: QuoteType,
    val filterType: QuoteStatusFilterType
)

data class QuoteListSnapshot(
    val key: QuoteListCacheKey,
    val quotes: List<QuoteModel>,
    val filterChips: List<QuoteFilterChip>,
    val globalApprovalStatusCounts: ApprovalStatusCountsDto?,
    val globalTotalCount: Int,
    val listTotalCount: Int,
    val nextStart: Int
)
