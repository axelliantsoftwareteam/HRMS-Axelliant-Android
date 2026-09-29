package com.axelliant.hris.features.quotes.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartQuoteProductSearchHistory @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun record(query: String) {
        val normalized = query.trim()
        if (normalized.isBlank()) return
        val current = getFrequent().filterNot { it.equals(normalized, ignoreCase = true) }
        val updated = listOf(normalized) + current
        prefs.edit()
            .putString(KEY_QUERIES, updated.take(MAX_ENTRIES).joinToString(SEPARATOR))
            .apply()
    }

    fun getFrequent(limit: Int = MAX_ENTRIES): List<String> {
        val raw = prefs.getString(KEY_QUERIES, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split(SEPARATOR).filter { it.isNotBlank() }.take(limit)
    }

    private companion object {
        const val PREFS_NAME = "smart_quote_product_search_history"
        const val KEY_QUERIES = "queries"
        const val SEPARATOR = "\u001E"
        const val MAX_ENTRIES = 6
    }
}
