package com.axelliant.hris.model.leave
import com.axelliant.hris.model.base.Meta
import com.axelliant.hris.model.expense.FileDetail

data class PostExpenseImageResponse(
    val status_message:String?=null,
    val url:String?=null,
    val file_detail: FileDetail?=null,
    val meta: Meta
)