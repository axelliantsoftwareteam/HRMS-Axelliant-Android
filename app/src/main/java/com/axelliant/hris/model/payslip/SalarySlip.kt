package com.axelliant.hris.model.payslip

import com.axelliant.hris.ui.WsFormat

/** One row of `get_salary_slips`. Amounts are returned by HRIS but are not shown in the app. */
data class SalarySlip(
    val name: String? = null,
    val start_date: String? = null,
    val end_date: String? = null,
    val posting_date: String? = null,
    val status: String? = null
)

/** `message.data` of `get_salary_slips`. */
data class SalarySlipPage(
    val items: List<SalarySlip?>? = null,
    val total: Any? = null
) {
    /** Rows that carry a document name (the PDF call needs it). */
    fun slips(): List<SalarySlip> = items.orEmpty().filterNotNull().filter { !it.name.isNullOrBlank() }

    fun totalCount(): Int = total.asLenientInt() ?: slips().size
}

/** A downloaded salary slip PDF waiting to be shown. */
data class PayslipPreview(val title: String, val path: String)

object PayslipFormat {
    /** "1 Sep 2026 – 30 Sep 2026", a single date when both ends match, else the posting date or the slip name. */
    fun period(slip: SalarySlip): String {
        val start = WsFormat.date(slip.start_date)
        val end = WsFormat.date(slip.end_date)
        if (start.isNotEmpty() && end.isNotEmpty()) return if (start == end) start else "$start – $end"
        val posted = WsFormat.date(slip.posting_date)
        return posted.ifEmpty { slip.name.orEmpty() }
    }

    /** HRIS submits a slip before it lists it, so an empty status reads "Submitted". */
    fun status(slip: SalarySlip, default: String): String = slip.status.cleaned() ?: default

    /** Letters, digits and "-" stay; everything else becomes "_" (used in the cached file name). */
    fun safeFileName(name: String): String =
        name.map { if (it.isLetterOrDigit() || it == '-') it else '_' }.joinToString("")
}
