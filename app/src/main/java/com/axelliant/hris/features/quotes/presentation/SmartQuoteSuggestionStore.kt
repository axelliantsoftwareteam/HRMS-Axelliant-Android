package com.axelliant.hris.features.quotes.presentation

import android.content.Context
import com.axelliant.hris.features.quotes.domain.model.QuoteCustomerUi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class SmartQuoteSuggestionStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun quoteTitles(): List<String> = readStrings(KEY_QUOTE_TITLES)

    fun products(): List<String> = readStrings(KEY_PRODUCTS)

    fun customers(): List<QuoteCustomerUi> {
        val raw = prefs.getString(KEY_CUSTOMERS, null).orEmpty()
        if (raw.isBlank()) return emptyList()

        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString(CUSTOMER_ID).trim()
                    val name = item.optString(CUSTOMER_NAME).trim()
                    if (id.isNotBlank() && name.isNotBlank()) {
                        add(
                            QuoteCustomerUi(
                                id = id,
                                name = name,
                                email = item.optString(CUSTOMER_EMAIL).trim()
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun rememberQuoteTitle(title: String) {
        rememberString(KEY_QUOTE_TITLES, title)
    }

    fun rememberCustomer(customer: QuoteCustomerUi) {
        if (customer.id.isBlank() || customer.name.isBlank()) return
        val updated = (listOf(customer) + customers())
            .distinctBy { it.id.ifBlank { it.name }.lowercase() }
            .take(MAX_SAVED_SUGGESTIONS)

        prefs.edit()
            .putString(
                KEY_CUSTOMERS,
                JSONArray().apply {
                    updated.forEach { item ->
                        put(
                            JSONObject()
                                .put(CUSTOMER_ID, item.id)
                                .put(CUSTOMER_NAME, item.name)
                                .put(CUSTOMER_EMAIL, item.email)
                        )
                    }
                }.toString()
            )
            .apply()
    }

    fun rememberProduct(product: String) {
        rememberString(KEY_PRODUCTS, product.toSmartQuoteProductName())
    }

    private fun rememberString(key: String, value: String) {
        val cleaned = value.trim()
        if (cleaned.isBlank()) return
        val updated = (listOf(cleaned) + readStrings(key))
            .distinctBy { it.lowercase() }
            .take(MAX_SAVED_SUGGESTIONS)
        prefs.edit()
            .putString(key, JSONArray(updated).toString())
            .apply()
    }

    private fun readStrings(key: String): List<String> {
        val raw = prefs.getString(key, null).orEmpty()
        if (raw.isBlank()) return emptyList()

        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val value = array.optString(index).trim()
                    if (value.isNotBlank()) add(value)
                }
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val PREF_NAME = "smart_quote_suggestions"
        const val KEY_QUOTE_TITLES = "quote_titles"
        const val KEY_CUSTOMERS = "customers"
        const val KEY_PRODUCTS = "products"
        const val CUSTOMER_ID = "id"
        const val CUSTOMER_NAME = "name"
        const val CUSTOMER_EMAIL = "email"
        const val MAX_SAVED_SUGGESTIONS = 1000
    }
}

internal fun String.toSmartQuoteProductName(): String {
    return substringBefore(" - ")
        .replace(Regex("\\$[0-9,]+\\.?[0-9]*"), "")
        .trim()
}
