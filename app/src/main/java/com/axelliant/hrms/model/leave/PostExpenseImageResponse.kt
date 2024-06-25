package com.axelliant.hrms.model.leave
import com.axelliant.hrms.model.base.Meta
import com.axelliant.hrms.model.expense.FileDetail

data class PostExpenseImageResponse(
    val status_message:String?=null,
    val url:String?=null,
    val file_detail: FileDetail?=null,
    val meta: Meta
)