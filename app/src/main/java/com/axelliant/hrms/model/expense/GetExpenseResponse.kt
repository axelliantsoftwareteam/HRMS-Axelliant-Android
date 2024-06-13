package com.axelliant.hrms.model.expense
import com.axelliant.hrms.model.base.Meta
import com.axelliant.hrms.model.leave.SpinnerType

data class GetExpenseResponse(
        var expenses: ArrayList<SpinnerType>?= arrayListOf(),
        val meta: Meta
)