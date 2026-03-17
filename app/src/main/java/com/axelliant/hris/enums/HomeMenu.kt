package com.axelliant.hris.enums


enum class HomeMenu(val gridName:String,val description:String) {

    Attendance("Attendance","Present of this month"),
    Request("Request","View all the requests"),
    Leaves("Leaves","Leaves you have"),
    CheckIN("Check IN","View all the check-in requests"),
    Expense("Expense","View all the expense requests"),
    DocumentManagement("Document Vault","View your all document"),
    ResourceManagement("Resource Management","View your all resource management"),
    Approval("Approval","View all requests")

}