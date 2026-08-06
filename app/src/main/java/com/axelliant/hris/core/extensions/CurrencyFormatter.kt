package com.axelliant.hris.core.extensions

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    fun format(amount: Double?): String {
        if (amount == null) return "$0.00"
        return currencyFormat.format(amount)
    }
}
