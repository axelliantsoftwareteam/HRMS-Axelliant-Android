package com.axelliant.hris.model.expense
import com.axelliant.hris.model.base.Meta
import com.axelliant.hris.model.leave.SpinnerType

data class GetExpenseResponse(
        var expenses: ArrayList<SpinnerType>?= arrayListOf(),
        val meta: Meta
)