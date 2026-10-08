package com.axelliant.hris.model.payslip

import com.axelliant.hris.network.ErrorMessages

/** Reads a number HRIS may send as 3, 3.0, "3" or true/false. */
fun Any?.asLenientInt(): Int? = when (this) {
    null -> null
    is Number -> toInt()
    is Boolean -> if (this) 1 else 0
    is String -> trim().toDoubleOrNull()?.toInt()
    else -> null
}

/** HRIS check fields: true only for `true`, 1 and "1". */
fun Any?.isTicked(): Boolean = when (this) {
    is Boolean -> this
    is Number -> toDouble() == 1.0
    is String -> trim() == "1"
    else -> false
}

/** Trimmed text, or null when blank. */
fun String?.cleaned(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

/**
 * Message for a toast: the server's own words when it sent some, [fallback] when the text is one of
 * the generic transport errors (or blank).
 */
fun userMessage(message: String?, fallback: String): String {
    val text = message.cleaned() ?: return fallback
    return if (ErrorMessages.values().any { it.errorString == text }) fallback else text
}
