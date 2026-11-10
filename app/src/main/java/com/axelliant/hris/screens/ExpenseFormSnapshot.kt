package com.axelliant.hris.screens

/** Values only: editing a draft in place must not mutate the comparison baseline. */
internal data class ExpenseFormSnapshot(
    val lines: List<Line>,
    val attachments: List<List<String>>
) {
    data class Line(val type: String, val date: String, val amount: String, val description: String)
}
