package com.axelliant.hris.enums


enum class HomeMenu(val gridName:String,val description:String) {

    Attendance("Attendance","Present of this month"),
    Request("Request","View all the requests"),
    Leaves("Leaves","Leaves you have"),
    Expense("Expense","View all the expense requests"),
    Approval("Approval","View all requests")

}
